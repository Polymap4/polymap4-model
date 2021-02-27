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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.teavm.jso.JSObject;
import org.teavm.jso.indexeddb.IDBCursor;
import org.teavm.jso.indexeddb.IDBObjectStore;
import org.teavm.jso.indexeddb.IDBRequest;
import org.teavm.jso.indexeddb.IDBTransaction;

import org.apache.commons.lang3.mutable.MutableObject;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.CompositeStateReference;
import org.polymap.model2.store.StoreResultSet;
import org.polymap.model2.store.StoreUnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore.TxMode;

import areca.common.base.Function;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class IDBUnitOfWork
        implements StoreUnitOfWork {
    
    private static final Log LOG = LogFactory.getLog( IDBUnitOfWork.class );
    
    protected IDBStore          store;
    

    public IDBUnitOfWork( IDBStore store ) {
        this.store = store;
    }


//    protected <E extends Exception> void doRequest( TxMode mode, List<String> storeNames, Consumer<Map<String,IDBObjectStore>,E> action ) throws E {
//        IDBTransaction tx = store.transaction( mode, storeNames.toArray( String[]::new ) );
//        Map<String,IDBObjectStore> objectStores = storeNames.stream()
//                .map( storeName -> tx.objectStore( storeName ) )
//                .collect( Collectors.toMap( os -> os.getName(), os -> os ) );
//        action.accept( objectStores );
//    }

    protected <RE extends IDBRequest,R> R doRequest( TxMode mode, String storeName, 
            Function<IDBObjectStore,RE,RuntimeException> doRequest, 
            Function<RE,R,RuntimeException> doAction ) { 
        
        IDBTransaction tx = store.transaction( mode, storeName );
        IDBObjectStore os = tx.objectStore( storeName );
        RE request = doRequest.apply( os );
        
        MutableObject<R> result = new MutableObject<>();
        request.setOnError( store.onErrorHandler );
        request.setOnSuccess( ev -> {
            result.setValue( doAction.apply( request ) );
        });
        store.waitFor( request );
        return result.getValue();
    }
    
    
    @Override
    public <T extends Entity> CompositeState newEntityState( Object id, Class<T> entityClass ) {
        LOG.info( "newEntityState() ..." );
        return new IDBCompositeState( id, entityClass );
    }


    @Override
    public <T extends Entity> CompositeState loadEntityState( Object id, Class<T> entityClass ) {
        CompositeInfo<T> entityInfo = store.infoOf( entityClass );
        LOG.info( "loadEntityState(): " + entityInfo.getNameInStore() + " / " + id );
        return doRequest( TxMode.READONLY, entityInfo.getNameInStore(), 
                os -> {
                    return os.get( IDBStore.id( id ) );
                },
                request -> {
                    JSStateObject jsObject = (JSStateObject)request.getResult();
                    return jsObject.isUndefined() ? null : new IDBCompositeState( entityClass, jsObject );
                }
        );
    }

    
    @Override
    public <T extends Entity> CompositeState adoptEntityState( Object state, Class<T> entityClass ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public <T extends Entity> StoreResultSet executeQuery( Query<T> query ) {
        CompositeInfo<T> entityInfo = store.infoOf( query.resultType() );
        LOG.info( "executeQuery(): " + entityInfo.getNameInStore() + " where " + query.expression );
        
        return new StoreResultSet() {
            String                              storeName = entityInfo.getNameInStore();
            List<CompositeStateReference>       results = new ArrayList<>( 128 );
            Iterator<CompositeStateReference>   resultsIt; 
            {
                LOG.info( "StoreResultSet init..." );
                doRequest( TxMode.READONLY, storeName, 
                        os -> os.openCursor(), 
                        request -> {
                            IDBCursor cursor = request.getResult();
                            if (!cursor.isNull()) {
                                JSObject jsObject = cursor.getValue();
                                IDBCompositeState state = new IDBCompositeState( query.resultType(), (JSStateObject)jsObject );
                                //if (query.expression.evaluate( state )
                                results.add( CompositeStateReference.create( state.id(), state ) );
                                cursor.doContinue();
                            }
                            return null;
                        });
                resultsIt = results.iterator();
            }
            @Override 
            public boolean hasNext() {
                return resultsIt.hasNext();
            }
            @Override 
            public CompositeStateReference next() {
                return resultsIt.next();
            }
            @Override 
            public int size() {
                return results.size();
                //return doRequest( TxMode.READONLY, storeName, os -> os.count(), request -> request.getResult() );
            }
            @Override 
            public void close() {
            }
        };
    }


    @Override
    public void prepareCommit( Iterable<Entity> modified ) throws Exception {
        List<String> storeNames = Sequence.of( modified )
                .transform( entity -> entity.info().getNameInStore() )
                .collect( Collectors.toList() );

        for (Entity entity : modified) {
            // FIXME separated transactions!
            doRequest( TxMode.READWRITE, storeNames.get( 0 ),
                    os -> {
                        LOG.info( entity.status() + " entity: " + entity );
                        if (entity.status() == EntityStatus.CREATED) {
                            return os.add( (JSObject)entity.state() );
                        }
                        else if (entity.status() == EntityStatus.MODIFIED) {
                            return os.put( (JSObject)entity.state() );
                        }
                        else if (entity.status() == EntityStatus.REMOVED) {
                            return os.delete( IDBStore.id( entity.id() ) );
                        }
                        else {
                            throw new IllegalStateException( "Status: " + entity.status() );
                        }
                    },
                    request -> null );
        }
        
//        tx = store.transaction( TxMode.READWRITE, storeNames );
//        
//        for (Entity entity : modified) {
//            IDBObjectStore os = tx.objectStore( entity.info().getNameInStore() );
//            if (entity.status() == EntityStatus.CREATED) {
//                LOG.info( "IDB: ADDING entity: " + entity );
//                store.waitFor( os.add( (JSObject)entity.state() ) );
//            }
//            else if (entity.status() == EntityStatus.MODIFIED) {
//                os.put( (JSObject)entity.state() );
//            }
//            else if (entity.status() == EntityStatus.REMOVED) {
//                os.delete( IDBStore.id( entity.id() ) );
//            }
//            else {
//                throw new IllegalStateException( "Status: " + entity.status() );
//            }
//        }
    }


    @Override
    public void commit() {
        // FIXME
        //tx.commit();
    }


    @Override
    public void rollback( Iterable<Entity> modified ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public void close() {
//        if (tx != null) {
//            tx.abort();
//            tx = null;
//        }
    }
    
}
