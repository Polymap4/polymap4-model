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

import java.io.IOException;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.indexeddb.EventHandler;
import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBFactory;
import org.teavm.jso.indexeddb.IDBOpenDBRequest;
import org.teavm.jso.indexeddb.IDBTransaction;

import org.polymap.model2.Composite;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class IDBStore
        implements StoreSPI {

    private static final Log LOG = LogFactory.getLog( IDBStore.class );

    public static int nextDbVersion() {
        long dbVersion = System.currentTimeMillis();
        // LOG.info( "timeMillis: %s", dbVersion );
        dbVersion = dbVersion >> 10; // roughly secconds
        // LOG.info( ">> 10: %s", dbVersion );
        return (int)dbVersion;
    }
    
    public enum TxMode {
        READWRITE, READONLY
    }
    
    // instance *******************************************
    
    private String                  dbName;
    
    private int                     dbVersion;
    
    private boolean                 deleteOnStartup;

    private StoreRuntimeContext     context;

    IDBDatabase                     db;

    EventHandler            onErrorHandler;
    
    EventHandler            onSuccessHandler;
    
    EventHandler            onBlockedHandler;


    public IDBStore( String dbName, int dbVersion, boolean deleteOnStartup ) {
        this.dbName = Assert.notNull( dbName );
        this.dbVersion = dbVersion;
        this.deleteOnStartup = deleteOnStartup;
        
        onErrorHandler = (Event ev) -> { 
            LOG.warn( "error during request: " + ev.getType() );
            //throw new RuntimeException( "Error during IndexedDB request." );
        };
        onSuccessHandler = (Event ev) -> {
            LOG.debug( "request successfully completed: '" + ev.getType() + "'" );
        };
        onBlockedHandler = (Event ev) -> {
            LOG.info( "request is BLOCKED..." );
        };
    }


    @Override
    public Promise<Void> init( @SuppressWarnings( "hiding" ) StoreRuntimeContext context ) {
        this.context = context;
        IDBFactory factory = IDBFactory.getInstance();

        var promise = new Promise.Completable<Void>();
        IDBOpenDBRequest request = factory.open( dbName, dbVersion );
        request.setOnError( ev -> {
            promise.completeWithError( new IOException( "Unable to init IDBStore: " + ev.getType() ) );
        });
        request.setOnSuccess( ev -> {
            db = request.getResult();
            promise.complete( null );
        });
        request.setOnBlocked( onBlockedHandler );
        request.setOnUpgradeNeeded( ev -> {
            new ObjectStoreBuilder( this ).checkSchemas( context.getRepository(), request.getResult(), deleteOnStartup );            
        });
        return promise;
    }

    
    public <T extends Composite> CompositeInfo<T> infoOf( ClassInfo<T> compositeClassInfo ) {
        return context.getRepository().infoOf( compositeClassInfo );
    }

    
    public <T extends Composite> CompositeInfo<T> infoOf( Class<T> compositeClass ) {
        return infoOf( ClassInfo.of( compositeClass ) );  // XXX optimize this!?
    }

    
    @Override
    public StoreUnitOfWork createUnitOfWork() {
        return new IDBUnitOfWork( this, context );
    }


    IDBTransaction transaction( TxMode mode, String... storeNames ) {
        LOG.debug( "TX: " + mode + " for ???" ); // + Arrays.asList( storeNames ) );
        IDBTransaction tx = db.transaction( storeNames, mode.name().toLowerCase() );
        tx.setOnError( onErrorHandler );
        tx.setOnComplete( (Event ev) -> {
            LOG.debug( "TX: completed" );
        });
        return tx;
    }

    
//    /**
//     * Wait for the given request to become ready.
//     * <p>
//     * XXX This is probably a bad solution. But model2 does not currently have an
//     * async API so I'm going with this to make some progress and learn about IDB.
//     * Later I maybe I will think about async API in model2. 
//     *
//     * @return The completed request.
//     */
//    <R extends IDBRequest> R waitFor( R request ) {
//        if (request.getReadyState().equals( IDBRequest.STATE_PENDING )) {
//            
//            // XXX check if we are inside a javascript callback
//            Long monitor = System.currentTimeMillis();
//            EventListener<Event> listener = (Event ev) -> {
//                synchronized (monitor) {
//                    monitor.notifyAll();
//                }
//            };
//            request.addEventListener( "success", listener );
//            request.addEventListener( "error", listener );
//
//            while (request.getReadyState().equals( IDBRequest.STATE_PENDING )) {
//                try {
//                    synchronized (monitor) {
//                        monitor.wait( 500 );
//                    }
//                } 
//                catch (InterruptedException e) {}
//            }
//            LOG.debug( "request ready. (" + (System.currentTimeMillis()-monitor) + "ms)" );
//        }
//        return request;
//    }

    
    @Override
    public void close() {
        db.close();
        db = null;
    }


    @Override
    public Object stateId( Object state ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    
    public static JSString id( Object id ) {
        Assert.that( id instanceof String );
        return JSString.valueOf( (String)id );
    }

    
    public static String id( JSObject js ) {
        if (JSStateObject.isUndefined( js )) {
            return null;
        }
        else {
            Assert.that( JSString.isInstance( js ) );
            return ((JSString)js).stringValue();
        }
    }

}
