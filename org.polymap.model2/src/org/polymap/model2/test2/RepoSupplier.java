/* 
 * polymap.org
 * Copyright (C) 2024, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.test2;

import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.no2.No2Store;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.common.Assert;
import areca.common.base.Function.RFunction;

/**
 * Different {@link StoreSPI} implementations for the tests of this package.
 * 
 * @author Falko
 */
public abstract class RepoSupplier {

    /** The implementation used by this tests */
    private static SPI current = null;
    
    public static StoreSPI newStore( String testName ) {
        return Assert.notNull( current, "Set RuntimeTest#repoSupplier before test!" ).apply( testName );
    }
    
    @FunctionalInterface
    public interface SPI extends RFunction<String, StoreSPI> { }
    
    /**
     * Activate {@link IDBStore} for subsequent tests. 
     */
    public static SPI teavm() {
        return current = testName -> new IDBStore( testName, IDBStore.nextDbVersion(), true );
    };

    /**
     * Activate {@link No2Store} for subsequent tests. 
     */
    public static SPI no2() {
        return current = testName -> new No2Store();
    };
    
}
