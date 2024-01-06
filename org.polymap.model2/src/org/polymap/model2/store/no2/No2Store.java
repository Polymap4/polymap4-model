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

import java.util.concurrent.Callable;

import java.io.File;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.index.IndexType;
import org.dizitart.no2.mvstore.MVStoreModule;
import org.dizitart.no2.store.memory.InMemoryStoreModule;
import org.dizitart.no2.transaction.Session;
import org.polymap.model2.Composite;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Promise;
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

    protected Nitrite db;
    
    protected Session session;

    protected StoreRuntimeContext context;

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

    
    @Override
    public Promise<Void> init( @SuppressWarnings("hiding") StoreRuntimeContext context ) {
        this.context = Assert.notNull( context );
        return async( () -> { 
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
                LOG.info( "Init: %s", entityClassInfo.name() );
                var coll = db.getCollection( entityInfo.getNameInStore() );
                LOG.info( "    collection: %s (%s)", coll.getName(), coll.size() );
                for (var prop : entityInfo.getProperties()) {
                    coll.createIndex( indexOptions( IndexType.NON_UNIQUE ), prop.getNameInStore() );
                    LOG.info( "    index: %s", prop.getNameInStore() );
                    Assert.that( coll.hasIndex( prop.getNameInStore() ) );
                }
            }
            return null;
        });
    }

    
    @Override
    public void close() {
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
    <R> Promise<R> async( Callable<R> task ) {
        return Platform.async( task );
        
        // wenn tatsächlich mal im Thread, dann müssen die Ergebnisse des Promise
        // im EventLoop konsumiert werden
    }

}
