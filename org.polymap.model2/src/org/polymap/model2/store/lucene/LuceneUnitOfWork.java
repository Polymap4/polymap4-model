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

import java.util.Collection;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.UnitOfWork.Submitted;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.CompositeStateReference;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 * @author Falko
 */
public class LuceneUnitOfWork
        implements StoreUnitOfWork {

    private static final Log LOG = LogFactory.getLog( LuceneUnitOfWork.class );
    
    private LuceneStore store;
    
    private StoreRuntimeContext context;

    
    public LuceneUnitOfWork( LuceneStore store, StoreRuntimeContext context ) {
        this.store = store;
        this.context = context;
    }

    @Override
    public void close() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T extends Entity> CompositeState newEntityState( Object id, Class<T> entityClass ) {
        return new LuceneCompositeState( id, entityClass );
    }

    @Override
    public <T extends Entity> Promise<CompositeState> loadEntityState( Object id, Class<T> entityClass ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T extends Entity> CompositeState adoptEntityState( Object state, Class<T> entityClass ) {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T extends Entity> Promise<CompositeStateReference> executeQuery( Query<T> query ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public Promise<Submitted> submit( Collection<Entity> modified ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public Promise<Submitted> rollback( Iterable<Entity> modified ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void setPriority( Priority priority ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }
}
