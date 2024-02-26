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
package org.polymap.model2.store.no2;

import static org.dizitart.no2.index.IndexOptions.indexOptions;

import java.util.HashMap;
import java.util.Map;

import java.io.File;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.index.IndexType;
import org.dizitart.no2.mvstore.MVStoreModule;
import org.dizitart.no2.store.memory.InMemoryStoreModule;
import org.dizitart.no2.transaction.Session;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.base.Function;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;

/**
 *
 * @author Falko
 */
public class No2Store
        implements StoreSPI {

    private static final Log LOG = LogFactory.getLog( No2Store.class );
    
    private File file;

    private Nitrite db;
    
    protected Session session;
    
    protected Map<String, NitriteCollection> collections;

    protected StoreRuntimeContext context;
    
   // private WorkerThread worker = new WorkerThread();

    /**
     * Creates a store with the given file backend. 
     */
    public No2Store( File file ) {
        this.file = file;
    }

    /**
     * Creates a store with in-memory backend. 
     */
    public No2Store() {
    }

    
    protected NitriteCollection collection( CompositeInfo<? extends Entity> entityInfo ) {
        return collections.get( entityInfo.getNameInStore() );    
    }
    
    
    @Override
    public Promise<Void> init( @SuppressWarnings("hiding") StoreRuntimeContext context ) {
        this.context = Assert.notNull( context );
        return async( __ -> { 
            db = Nitrite.builder()
                    .loadModule( file != null
                            ? MVStoreModule.withConfig().filePath( file ).build()
                            : InMemoryStoreModule.withConfig().build() )
                    .openOrCreate();
            
            session = db.createSession();

            // check/init collections + indices
            EntityRepository repo = context.getRepository();
            collections = new HashMap<>();
            for (var entityClassInfo : repo.getConfig().entities.get()) {
                var entityInfo = repo.infoOf( entityClassInfo );
                LOG.debug( "Init: %s", entityClassInfo.name() );
                var coll = db.getCollection( entityInfo.getNameInStore() );
                LOG.debug( "    collection: %s (%s)", coll.getName(), coll.size() );

                checkCompositeIndexes( entityInfo, coll, "" );

                collections.put( entityInfo.getNameInStore(), coll );
            }
            return null;
        });
    }

    
    protected void checkCompositeIndexes( CompositeInfo<?> info, NitriteCollection coll, String fieldNameBase ) {
        for (var prop : info.getProperties()) {
            var indexName = fieldNameBase + prop.getNameInStore();
            if (prop.isQueryable() && !coll.hasIndex( indexName )) {
                coll.createIndex( indexOptions( IndexType.NON_UNIQUE ), indexName );
                LOG.debug( "    index: %s", indexName );
                Assert.that( coll.hasIndex( indexName ) );
            }
            if (Composite.class.isAssignableFrom( prop.getType() )) {
                @SuppressWarnings( "unchecked" )
                var type = (Class<Composite>)prop.getType();
                checkCompositeIndexes( infoOf( type ), coll, fieldNameBase + prop.getNameInStore() + "." );
            }
        }        
    }
    
            
    @Override
    public void close() {
        collections.values().forEach( coll -> coll.close() );
        collections.clear();
        session.close();
        db.close();
    }

    
    @Override
    public StoreUnitOfWork createUnitOfWork() {
        return new No2UnitOfWork( this );
    }


    @Override
    public Object stateId( Object state ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    
    public <T extends Composite> CompositeInfo<T> infoOf( ClassInfo<T> compositeClassInfo ) {
        return context.getRepository().infoOf( compositeClassInfo );
    }

    public <T extends Composite> CompositeInfo<T> infoOf( Class<T> compositeClass ) {
        return infoOf( ClassInfo.of( compositeClass ) );  // XXX optimize this!?
    }

    
    /**
     * Internal use: execute the given (database) task asynchronously. 
     */
    <R> Promise<R> async( Function<Completable<R>,R,Exception> task ) {
        return Platform.async( () -> task.apply( null ) );
    }
    
    
//    /**
//     * Internal use: execute the given (database) task asynchronously. 
//     */
//    <R> Promise<R> async( Function<Completable<R>,R,Exception> task ) {
//        //return Platform.async( task );
//        
//        // wenn tatsächlich mal im Thread, dann müssen die Ergebnisse des Promise
//        // im EventLoop konsumiert werden
//        var eventLoop = areca.common.Session.instanceOf( EventLoop.class );
//        var promise = new Promise.Completable<R>() {
//            @Override
//            public void complete( R value ) {
//                eventLoop.enqueue( "No2Store", () -> {
//                    super.complete( value );
//                }, 0 );                
//            }
//            @Override
//            public void consumeResult( R value ) {
//                // XXX Auto-generated method stub
//                throw new RuntimeException( "not yet implemented." );
//            }
//            @Override
//            public void completeWithError( Throwable e ) {
//                // XXX Auto-generated method stub
//                throw new RuntimeException( "not yet implemented." );
//            }
//        };
//        try {
//            worker.queue.offer( () -> {
//                eventLoop.enqueue( "No2Store", () -> {
//                    try {
//                        var result = task.call();
//                        if (result != null) {
//                            promise.complete( task.call() );
//                        }
//                    }
//                    catch (Exception e) {
//                        promise.completeWithError( e );
//                    }
//                }, 0 );
//            }, 10, TimeUnit.SECONDS );
//        }
//        catch (InterruptedException e) {
//            throw new RuntimeException( e );
//        }
//        return promise;
//    }
//
//    /**
//     * 
//     */
//    private static class WorkerThread extends Thread {
//
//        boolean stop;
//        
//        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( 10, true );
//        
//        public WorkerThread() {
//            super( "No2Store WorkerThread" );
//            start();
//        }
//
//        @Override
//        public void run() {
//            while (!stop) {
//                try {
//                    var task = queue.take();
//                    task.run();
//                }
//                catch (InterruptedException e) {
//                }
//            }
//        }
//    }
}
