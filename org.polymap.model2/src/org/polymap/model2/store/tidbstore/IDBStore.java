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

import org.teavm.jso.core.JSString;
import org.teavm.jso.indexeddb.EventHandler;
import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBOpenDBRequest;
import org.teavm.jso.indexeddb.IDBTransaction;

import org.polymap.model2.Composite;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Assert;
import areca.common.reflect.ClassInfo;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class IDBStore
        implements StoreSPI {

    private static final Logger LOG = Logger.getLogger( IDBStore.class.getName() );
    
    public enum TxMode {
        READWRITE, READONLY
    }
    
    // instance *******************************************
    
    private String                  dbName;
    
    private StoreRuntimeContext     context;

    IDBDatabase                     db;

    private EventHandler            onErrorHandler; // = () -> throw new RuntimeException( "Error during IDB request." );
    
    private EventHandler            onSuccessHandler;
    
    private EventHandler            onBlockedHandler;
    
    
    public IDBStore( String dbName ) {
        this.dbName = Assert.notNull( dbName );
        
        onErrorHandler = new EventHandler() {
            @Override public void handleEvent() {
                throw new RuntimeException( "Error during IndexedDB request." );
            }
        };
        onSuccessHandler = new EventHandler() {
            @Override public void handleEvent() {
                LOG.info( "IDB: request successfully completed" );
            }
        };
        onBlockedHandler = new EventHandler() {
            @Override public void handleEvent() {
                LOG.info( "IDB: request blocked" );
            }
        };
    }


    @Override
    public void init( @SuppressWarnings( "hiding" ) StoreRuntimeContext context ) {
        this.context = context;
        LOG.info( "IDB: " + FixedIDBFactory.isSupported() );
        FixedIDBFactory factory = FixedIDBFactory.getInstance();
        IDBOpenDBRequest request = factory.open( dbName, 2 );
        request.setOnError( onErrorHandler );
        request.setOnSuccess( onSuccessHandler );
        request.setOnBlocked( onBlockedHandler );
        request.setOnUpgradeNeeded( ev -> {
            new ObjectStoreBuilder( this ).checkSchemas( context.getRepository(), request.getResult() );            
        });
        
        while (request.getReadyState().equals( "pending" )) {
            try {
                LOG.info( "IDB: " + request.getReadyState() );
                Thread.sleep( 100 );
            }
            catch (InterruptedException e) {
            }
        }
        LOG.info( "IDB: " + request.getReadyState() );
        db = request.getResult();
    }

    
    public <T extends Composite> CompositeInfo<T> infoOf( ClassInfo<T> compositeClassInfo ) {
        return context.getRepository().infoOf( compositeClassInfo );
    }

    
    public <T extends Composite> CompositeInfo<T> infoOf( Class<T> compositeClass ) {
        return infoOf( ClassInfo.of( compositeClass ) );  // XXX optimize this!?
    }

    
    @Override
    public StoreUnitOfWork createUnitOfWork() {
        return new IDBUnitOfWork( this );
    }


    IDBTransaction transaction( TxMode mode, String... storeNames ) {
        LOG.info( "STORE: creating " + mode + " TX for " +  storeNames );
        IDBTransaction tx = db.transaction( storeNames, mode.name().toLowerCase() );
        tx.setOnError( onErrorHandler );
        tx.setOnComplete( new EventHandler() {
            @Override
            public void handleEvent() {
                LOG.info( "STORE: TX completed" );
            }
        });
        return tx;
    }


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

}
