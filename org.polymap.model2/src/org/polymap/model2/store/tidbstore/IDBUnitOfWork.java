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

import org.apache.commons.lang3.tuple.MutablePair;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.UnitOfWork.Submitted;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.CompositeStateReference;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreUnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore.TxMode;
import org.polymap.model2.store.tidbstore.indexeddb.IDBCursor;
import org.polymap.model2.store.tidbstore.indexeddb.IDBCursorRequest;
import org.polymap.model2.store.tidbstore.indexeddb.IDBObjectStore;
import org.polymap.model2.store.tidbstore.indexeddb.IDBRequest;
import org.polymap.model2.store.tidbstore.indexeddb.IDBTransaction;

import areca.common.MutableInt;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.base.Consumer.RConsumer;
import areca.common.base.Function.RFunction;
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
    
    protected StoreRuntimeContext   context;
    
    protected IDBStore              store;

    protected Priority              priority;
    

    public IDBUnitOfWork( IDBStore store, StoreRuntimeContext context ) {
        this.store = store;
        this.context = context;
    }


//    protected <E extends Exception> void doRequest( TxMode mode, List<String> storeNames, Consumer<Map<String,IDBObjectStore>,E> action ) throws E {
//        IDBTransaction tx = store.transaction( mode, storeNames.toArray( String[]::new ) );
//        Map<String,IDBObjectStore> objectStores = storeNames.stream()
//                .map( storeName -> tx.objectStore( storeName ) )
//                .collect( Collectors.toMap( os -> os.getName(), os -> os ) );
//        action.accept( objectStores );
//    }


    @Override
    public void setPriority( Priority priority ) {
        this.priority = priority;
    }


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
    protected <RE extends IDBRequest> void doRequest( TxMode mode, String storeName, 
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
    
    
    /**
     * Must be followed by {@link Promise#map(areca.common.base.BiConsumer)} spread
     * operation.
     *
     * @param <RE>
     * @param <T>
     */
    protected <RE extends IDBCursorRequest, T extends Entity> Promise<IDBCursor> doRequest( TxMode mode, Class<T> entityType, 
            RFunction<IDBObjectStore,RE> createRequest ) {
        
        CompositeInfo<T> entityInfo = store.infoOf( entityType );        
        IDBTransaction tx = store.transaction( mode, entityInfo.getNameInStore() );
        IDBObjectStore os = tx.objectStore( entityInfo.getNameInStore() ).cast();
        
        RE request = createRequest.apply( os );
        
        var result = new Promise.Completable<IDBCursor>();
        request.setOnError( ev -> {
            result.completeWithError( new IOException( "Event: " + ev.getType() + ", Error: " + request.getError().getName() ) );
        });
        request.setOnSuccess( ev -> {
            var cursor = request.getResult().<IDBCursor>cast();
            result.consumeResult( cursor );
        });
        return result;
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
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public <T extends Entity> Promise<CompositeStateReference> executeQuery( Query<T> query ) {
        CompositeInfo<T> entityInfo = store.infoOf( query.resultType() );
        LOG.debug( "executeQuery(): %s where %s", entityInfo.getNameInStore(), query.expression );
        
        return new QueryExecutor( query, this ).execute()
                .map( (ids, next) -> {
                    for (var id : ids) {
                        next.consumeResult( CompositeStateReference.create( id, null ) );
                    }
                    next.complete( null );
                });
    }


    @Override
    public Promise<Submitted> submit( Collection<Entity> modified ) {
        if (modified.isEmpty()) {
            return Promise.completed( new Submitted() {}, priority );
        }
        var promise = new Promise.Completable<Entity>();
        var count = new MutableInt( modified.size() );
        var submitted = new Submitted();
        for (Entity entity : modified) {
            // FIXME separated transactions!
            doRequest( TxMode.READWRITE, entity.info().getNameInStore(),
                    os -> {
                        LOG.debug( "submit(): " + entity );
                        switch (entity.status()) {
                            case CREATED:
                                submitted.createdIds.add( entity.id() );
                                return os.add( (JSObject)entity.state(), IDBStore.id( entity.id() ) );
                            case MODIFIED:
                                submitted.modifiedIds.add( entity.id() );
                                return os.put( (JSObject)entity.state(), IDBStore.id( entity.id() ) );
                            case REMOVED:
                                submitted.removedIds.add( entity.id() );
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
        return promise.reduce( submitted, (r,entity) -> {} );
    }


    /**
     * XXX This is a *refresh* impl. - change name!
     */
    @Override
    public Promise<Submitted> rollback( Iterable<Entity> entities ) {
        var l = Sequence.of( entities ).toList();
        if (l.isEmpty()) {
            return Promise.completed( new Submitted() {}, priority );
        }
        
        var submitted = new Submitted();
        return Promise.joined( l.size(), i -> {
            var entity = l.get( i );
            var state = (IDBCompositeState)context.contextOfEntity( entity ).getState();
            return loadEntityState( state.id(), entity.getClass() )
                    .map( newState -> MutablePair.of( state, (IDBCompositeState)newState ) );
        })
        .map( loaded -> {
            if (loaded.right == null) {
                submitted.removedIds.add( loaded.left.id() );
            }
            else {
                submitted.modifiedIds.add( loaded.left.id() );
                loaded.left.state = loaded.right.state;
                LOG.debug( "ROLLED BACK: " + loaded.left.id() );
            }
            return loaded;
        })
        .reduce( submitted, (result, next) -> {} );
    }


    @Override
    public void close() {
//        if (tx != null) {
//            tx.abort();
//            tx = null;
//        }
    }
    
}
