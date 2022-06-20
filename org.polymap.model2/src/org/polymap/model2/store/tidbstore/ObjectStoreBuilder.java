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

import java.util.Set;

import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBObjectStoreParameters;

import org.polymap.model2.runtime.EntityRepository;

import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class ObjectStoreBuilder {

    private static final Log LOG = LogFactory.getLog( ObjectStoreBuilder.class );
    
    private IDBStore        store;

    private IDBDatabase     db;

    
    public ObjectStoreBuilder( IDBStore store ) {
        this.store = store;
    }


    public void checkSchemas( EntityRepository repo, @SuppressWarnings("hiding") IDBDatabase db, boolean clear ) {
        this.db = db;
        Set<String> objectStoreNames = Sequence.of( db.getObjectStoreNames() ).toSet();
        
        for (var entityClassInfo : repo.getConfig().entities.get()) {
            var info = repo.infoOf( entityClassInfo );
            String name = info.getNameInStore();

            if (objectStoreNames.contains( name ) && clear) {
                LOG.debug( "Deleting schema: %s ...", name );
                db.deleteObjectStore( name );                
            }
            
            if (!objectStoreNames.contains( name ) || clear) {
                LOG.info( "Creating schema: %s ...", name );
                var os = db.createObjectStore( name, IDBObjectStoreParameters.create()/*.keyPath( "id" )*/ );
                
                for (var prop : info.getProperties()) {
                    if (prop.isQueryable()) {
                        //Assert.that( prop.is );
                        LOG.info( "    Index: %s (%s)", prop.getNameInStore(), prop.getNameInStore() );
                        os.createIndex( prop.getNameInStore(), prop.getNameInStore() );
                    }
                }
            }
        }
    }
}
