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
package org.polymap.model2.store.lucene;

import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko
 */
public class LuceneStore
        implements StoreSPI {

    private static Log log = LogFactory.getLog( LuceneStore.class );

    @Override
    public Promise<Void> init( StoreRuntimeContext context ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void close() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public Object stateId( Object state ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public StoreUnitOfWork createUnitOfWork() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }
}
