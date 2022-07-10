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

import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBObjectStoreParameters;

import org.polymap.model2.Composite;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.store.tidbstore.IDBObjectStore2.IndexParameters;

import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class ObjectStoreBuilder {

    private static final Log LOG = LogFactory.getLog( ObjectStoreBuilder.class );
    
    private IDBStore        store;

    private IDBDatabase     db;

    private EntityRepository repo;

    
    public ObjectStoreBuilder( IDBStore store ) {
        this.store = store;
    }


    @SuppressWarnings("hiding")
    public void checkSchemas( EntityRepository repo, IDBDatabase db, boolean clear ) {
        this.db = db;
        this.repo = repo;
        
        var objectStoreNames = Sequence.of( db.getObjectStoreNames() ).toSet();
        
        for (var entityClassInfo : repo.getConfig().entities.get()) {
            var info = repo.infoOf( entityClassInfo );
            String name = info.getNameInStore();

            // delete
            if (objectStoreNames.contains( name ) && clear) {
                LOG.debug( "Deleting schema: %s ...", name );
                db.deleteObjectStore( name );                
            }
            
            // create
            if (!objectStoreNames.contains( name ) || clear) {
                LOG.info( "Creating schema: %s ...", name );
                var os = db.createObjectStore( name, IDBObjectStoreParameters.create() ).<IDBObjectStore2>cast();
                
                // indexes
                for (var prop : info.getProperties()) {
                    checkCreateIndexes( prop, os, prop.getNameInStore(), true );
                }
            }
        }
    }
    
    
    @SuppressWarnings("rawtypes")
    protected void checkCreateIndexes( PropertyInfo prop, IDBObjectStore2 os, String name, boolean valid ) {
        if (prop.isQueryable()) {
            Assert.that( !prop.getType().equals( Boolean.class ), "Boolean is not a valid index key in IndexedDB: " + prop );
            if (!valid) {
                LOG.warn( "Indexing CompositeCollections is not yet supported: " + prop );
            }
            LOG.info( "    Index: %s", name );
            var params = IndexParameters.create();
            params.setMultiEntry( true );
            os.createIndex( name, name, params );
        }
        
        if (Composite.class.isAssignableFrom( prop.getType() ) && !prop.isAssociation()) {
            Class<? extends Composite> compositeType = prop.getType();
            var compositeInfo = repo.infoOf( ClassInfo.of( compositeType ) );
            for (var compositeProp : compositeInfo.getProperties()) {
                var nextValid = prop.getMaxOccurs() == 1 || !Composite.class.isAssignableFrom( prop.getType() );
                checkCreateIndexes( compositeProp, os, name + "." + compositeProp.getNameInStore(), nextValid );
            }
        }
    }
}
