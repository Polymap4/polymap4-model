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

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
import areca.common.Platform.PollingCommand;
import areca.common.Promise;
import areca.common.Promise.Completable;
import areca.common.SessionScoper.ThreadBoundSessionScoper;
import areca.common.Timer;
import areca.common.base.Consumer;
import areca.common.base.Lazy.RLazy;
import areca.common.base.Supplier;
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
    
    protected Map<String, NitriteCollection> collections = new ConcurrentHashMap<>();

    protected StoreRuntimeContext context;
    
    private RLazy<WorkerThread> worker = new RLazy<>( () -> new WorkerThread() ) ;
    
    private boolean closed;


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
        checkOpen();
        return collections.get( entityInfo.getNameInStore() );    
    }
    
    
    @Override
    public Promise<Void> init( @SuppressWarnings("hiding") StoreRuntimeContext context ) {
        this.context = Assert.notNull( context );
        return async( "init()", () -> { 
            db = Nitrite.builder()
                    .loadModule( file != null
                            ? MVStoreModule.withConfig().filePath( file ).build()
                            : InMemoryStoreModule.withConfig().build() )
                    .openOrCreate();
            
            session = db.createSession();

            // check/init collections + indices
            EntityRepository repo = context.getRepository();
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
                if (prop.getAnnotation( Fulltext.class ) != null) {
                    coll.createIndex( indexOptions( IndexType.FULL_TEXT ), indexName );
                    LOG.debug( "    fulltext: %s", indexName );
                }
                else {
                    coll.createIndex( indexOptions( IndexType.NON_UNIQUE ), indexName );
                    LOG.debug( "    index: %s", indexName );
                }
                Assert.that( coll.hasIndex( indexName ) );
            }
            if (Composite.class.isAssignableFrom( prop.getType() )
                    && !prop.isAssociation() && !prop.isComputed()) {
                @SuppressWarnings( "unchecked" )
                var type = (Class<Composite>)prop.getType();
                checkCompositeIndexes( infoOf( type ), coll, fieldNameBase + prop.getNameInStore() + "." );
            }
        }        
    }
    
            
    @Override
    public void close() {
        worker.ifInitialized( self -> self.stop = true );
        collections.values().forEach( coll -> coll.close() );
        collections.clear();
        session.close();
        db.close();
        closed = true;
    }

    
    protected void checkOpen() {
        if (db == null) {
            throw new RuntimeException( "Not yet init()" );
        }
        if (closed) {
            throw new RuntimeException( "No2Store is closed" );            
        }
    }
    
    
    @Override
    public StoreUnitOfWork createUnitOfWork() {
        checkOpen();
        return new No2UnitOfWork( this );
    }


    @Override
    public Object stateId( Object state ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    
    public <T extends Composite> CompositeInfo<T> infoOf( ClassInfo<T> compositeClassInfo ) {
        checkOpen();
        return context.getRepository().infoOf( compositeClassInfo );
    }

    public <T extends Composite> CompositeInfo<T> infoOf( Class<T> compositeClass ) {
        checkOpen();
        return infoOf( ClassInfo.of( compositeClass ) );  // XXX optimize this!?
    }

    
    /**
     * Execute the given (database) task asynchronously. 
     */
    <R> Promise<R> async( String label, Supplier<R,Exception> task ) {
        return async( label, promise -> promise.complete( task.supply() ) );
    }

    
    /**
     * Execute the given (database) task asynchronously. 
     */
    <R> Promise<R> async( String label, Consumer<Completable<R>,Exception> task ) {
        return asyncEnqueue( label, task );
        //return asyncDirect( label, task );
        //return asyncWorker( label, task );
    }
    
    
    private <R> Promise<R> asyncEnqueue( String label, Consumer<Completable<R>,Exception> task ) {
        return Platform.enqueue( "No2Store." + label, 0, task );
    }
    

    /**
     * @deprecated This one is a bit tricky porque no estoy seguro si {@link Promise}
     *             handles already-completed results correctly.
     */
    private <R> Promise<R> asyncDirect( String label, Consumer<Completable<R>,Exception> task ) {
        var result = new Promise.Completable<R>();
        try {
            task.accept( result );
        }
        catch (Exception e) {
            result.completeWithError( e );
        }
        return result;
    }
    
    
    private <R> Promise<R> asyncWorker( String label, Consumer<Completable<R>,Exception> task ) {
        Platform.polling( PollingCommand.START );
        var promise = new Promise.Completable<R>() {
            @Override
            public void complete( R value ) {
                Platform.enqueue( "No2Store." + label, 0, __ -> { 
                    super.complete( value );
                    Platform.polling( PollingCommand.STOP );
                });
            }
            @Override
            public void consumeResult( R value ) {
                Platform.enqueue( "No2Store." + label, 0, __ -> super.consumeResult( value ) );
            }
            @Override
            public void completeWithError( Throwable e ) {
                Platform.enqueue( "No2Store." + label, 0, __ -> { 
                    super.completeWithError( e );
                    Platform.polling( PollingCommand.STOP );
                });
            }
        };
        try {
            worker.$().queue.offer( () -> {
                try {
                    task.accept( promise );
                }
                catch (Exception e) {
                    promise.completeWithError( e );
                }
            }, 10, TimeUnit.SECONDS );
        }
        catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
        return promise;
    }

    /**
     * Jus one thread to ensure that there is just serial, no multi-threaded access
     * to Nitrite.
     */
    private static class WorkerThread
            extends Thread {

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( 25, true );
        
        areca.common.Session session = areca.common.Session.current();
        
        boolean stop;
        
        public WorkerThread() {
            super( "No2Store.Worker" );
            setDaemon( true );
            // help code in main thread to set Promise.onSuccess() before we finish (?)
            setPriority( NORM_PRIORITY - 1 );
            start();
        }

        @Override
        public void run() {
            ThreadBoundSessionScoper.instance().bind( session );
            while (!stop /*|| !queue.isEmpty()*/) {
                try {
                    var task = queue.poll( 10, TimeUnit.SECONDS );
                    if (task != null) {
                        var t = Timer.start();
                        task.run();
                        LOG.info( "%s: queue=%s [%s]", getName(), queue.size(), t );
                    }
                }
                catch (InterruptedException e) {
                }
            }
            ThreadBoundSessionScoper.instance().unbind( session );
            LOG.info( "%s: stopped", getName() );
        }
    }
}
