/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.store;

import java.util.function.Supplier;

import org.polymap.model2.runtime.UnitOfWork;

/**
 * The result of a
 * {@link StoreUnitOfWork#executeQuery(org.polymap.model2.query.Query) store query}.
 * It allows the store to provide a preloaded {@link CompositeState} that might have
 * been loaded as a result of the query. This state is used by the {@link UnitOfWork}
 * to fill the cache instead of requesting the state via id from the store.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface CompositeStateReference
        extends Supplier<CompositeState> {

    public static CompositeStateReference create( Object id, CompositeState state ) {
        return new CompositeStateReference() {
            @Override public Object id() {
                return id;
            }
            @Override public CompositeState get() {
                return state;
            }
        };
    }
    
    // instance *******************************************
    
    public Object id();
    
    
    /**
     * If available, provides a state that was <b>preloaded</b> during execution of
     * the query, or null if no such preloaded state exists.
     */
    @Override
    public CompositeState get();
    
}
