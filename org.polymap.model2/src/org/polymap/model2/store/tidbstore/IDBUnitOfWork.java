/* 
 * polymap.org
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package org.polymap.model2.store.tidbstore;

import java.util.logging.Logger;

import org.teavm.jso.JSObject;
import org.teavm.jso.indexeddb.IDBGetRequest;
import org.teavm.jso.indexeddb.IDBObjectStore;
import org.teavm.jso.indexeddb.IDBTransaction;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreResultSet;
import org.polymap.model2.store.StoreUnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore.TxMode;

import areca.common.Assert;
import areca.common.base.Sequence;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class IDBUnitOfWork
        implements StoreUnitOfWork {
    
    private static final Logger LOG = Logger.getLogger( IDBUnitOfWork.class.getName() );
    
    protected IDBStore          store;
    
    protected IDBTransaction    tx;
    

    public IDBUnitOfWork( IDBStore store ) {
        this.store = store;
    }


    @Override
    public <T extends Entity> CompositeState newEntityState( Object id, Class<T> entityClass ) {
        LOG.info( "newEntityState() ..." );
        return new IDBCompositeState( id, entityClass );
    }


    @Override
    public <T extends Entity> CompositeState loadEntityState( Object id, Class<T> entityClass ) {
        CompositeInfo<T> entityInfo = store.infoOf( entityClass );
        IDBTransaction local = store.transaction( TxMode.READONLY, entityInfo.getNameInStore() );
        IDBObjectStore os = local.objectStore( entityInfo.getNameInStore() );
        IDBGetRequest request = os.get( IDBStore.id( id ) );
        JSStateObject jsObject = (JSStateObject)request.getResult();
        return new IDBCompositeState( id, entityClass, jsObject );
    }

    
    @Override
    public <T extends Entity> CompositeState adoptEntityState( Object state, Class<T> entityClass ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public StoreResultSet executeQuery( Query query ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public void prepareCommit( Iterable<Entity> modified ) throws Exception {
        Assert.isNull( tx );
        String[] storeNames = Sequence.of( modified )
                .transform( entity -> entity.info().getNameInStore() )
                .toArray( String[]::new );
        
        tx = store.transaction( TxMode.READWRITE, storeNames );
        
        for (Entity entity : modified) {
            IDBObjectStore os = tx.objectStore( entity.info().getNameInStore() );
            if (entity.status() == EntityStatus.CREATED) {
                os.add( (JSObject)entity.state() );
            }
            else if (entity.status() == EntityStatus.MODIFIED) {
                os.put( (JSObject)entity.state() );
            }
            else if (entity.status() == EntityStatus.REMOVED) {
                os.delete( IDBStore.id( entity.id() ) );
            }
            else {
                throw new IllegalStateException( "Status: " + entity.status() );
            }
        }
    }


    @Override
    public void commit() {
        Assert.notNull( tx );
        
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public void rollback( Iterable<Entity> modified ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public void close() {
        if (tx != null) {
            tx.abort();
            tx = null;
        }
    }
    
}
