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

import java.util.Collection;
import java.io.IOException;

import org.teavm.jso.JSObject;
import org.teavm.jso.indexeddb.IDBCursor;
import org.teavm.jso.indexeddb.IDBObjectStore;
import org.teavm.jso.indexeddb.IDBRequest;
import org.teavm.jso.indexeddb.IDBTransaction;

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.UnitOfWork.Submitted;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.CompositeStateReference;
import org.polymap.model2.store.StoreUnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore.TxMode;

import areca.common.Promise;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Function.RFunction;
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

    /**
     * 
     *
     * @param <RE> The type of the {@link IDBRequest}.
     * @param <R> The type of the result value.
     * @param mode
     * @param storeName
     * @param createRequest
     * @param handleResult
     */
    protected <RE extends IDBRequest,R> void doRequest( TxMode mode, String storeName, 
            RFunction<IDBObjectStore,RE> createRequest, 
            RConsumer<RE> handleResult,
            RConsumer<Throwable> handleError) { 
        
        IDBTransaction tx = store.transaction( mode, storeName );
        IDBObjectStore os = tx.objectStore( storeName );
        RE request = createRequest.apply( os );
        
        request.setOnError( ev -> {
            handleError.accept( new IOException( "Event: " + ev.getType() + ", Error: " + request.getError().getName() ) );
        });
        request.setOnSuccess( ev -> {
            handleResult.accept( request );
        });
    }
    
    
    @Override
    public <T extends Entity> CompositeState newEntityState( Object id, Class<T> entityClass ) {
        LOG.debug( "newEntityState() ..." );
        return new IDBCompositeState( id, entityClass );
    }


    @Override
    public <T extends Entity> Promise<CompositeState> loadEntityState( Object id, Class<T> entityClass ) {
        CompositeInfo<T> entityInfo = store.infoOf( entityClass );
        LOG.debug( "loadEntityState(): " + entityInfo.getNameInStore() + " / " + id );
        
        var promise = new Promise.Completable<CompositeState>();
        doRequest( TxMode.READONLY, entityInfo.getNameInStore(), 
                os -> os.get( IDBStore.id( id ) ),
                request -> {
                    JSStateObject jso = (JSStateObject)request.getResult();
                    LOG.debug( "    : %s", jso.isUndefined() );
                    var result = jso.isUndefined() ? null : new IDBCompositeState( entityClass, jso );
                    promise.complete( result );
                },
                error -> promise.completeWithError( error ) );
        return promise;
    }

    
    @Override
    public <T extends Entity> CompositeState adoptEntityState( Object state, Class<T> entityClass ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public <T extends Entity> Promise<CompositeStateReference> executeQuery( Query<T> query ) {
        CompositeInfo<T> entityInfo = store.infoOf( query.resultType() );
        LOG.debug( "executeQuery(): %s where %s", entityInfo.getNameInStore(), query.expression );
        
        var promise = new Promise.Completable<CompositeStateReference>();
        doRequest( TxMode.READONLY, entityInfo.getNameInStore(), 
                os -> os.openCursor(), 
                request -> {
                    if (!promise.isCanceled()) {
                        IDBCursor cursor = request.getResult();
                        if (!cursor.isNull()) {
                            JSObject jsObject = cursor.getValue();
                            IDBCompositeState state = new IDBCompositeState( query.resultType(), (JSStateObject)jsObject );
                            promise.consumeResult( CompositeStateReference.create( state.id(), state ) );
                            cursor.doContinue();
                        }
                        else {
                            // FIXME signal end
                            promise.complete( null );
                        }
                    }
                },
                error -> promise.completeWithError( error ) );
        return promise;
    }


    @Override
    public Promise<Submitted> submit( Collection<Entity> modified ) {
        var promise = new Promise.Completable<Entity>();
        var count = new MutableInt( modified.size() );
        for (Entity entity : modified) {
            // FIXME separated transactions!
            doRequest( TxMode.READWRITE, entity.info().getNameInStore(),
                    os -> {
                        LOG.debug( "submit(): " + entity );
                        switch (entity.status()) {
                            case CREATED: 
                                return os.add( (JSObject)entity.state(), IDBStore.id( entity.id() ) );
                            case MODIFIED:
                                return os.put( (JSObject)entity.state(), IDBStore.id( entity.id() ) );
                            case REMOVED:
                                return os.delete( IDBStore.id( entity.id() ) );
                            default: 
                                throw new IllegalStateException( "Status: " + entity.status() );
                        }
                    },
                    request -> {
                        if (count.decrementAndGet() == 0) {
                            promise.complete( entity );
                        } else {
                            promise.consumeResult( entity );
                        }
                    },
                    error -> promise.completeWithError( error ) );
        }
        return promise.reduce( new Submitted() {}, (r,entity) -> {} );
    }


//    @Override
//    public void commit() {
//        // FIXME
//        //tx.commit();
//    }


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
