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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBObjectStoreParameters;
import org.polymap.model2.Entity;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRepository;

import areca.common.reflect.ClassInfo;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class ObjectStoreBuilder {

    private static final Logger LOG = Logger.getLogger( IDBStore.class.getName() );
    
    private IDBStore        store;

    private IDBDatabase db;

    
    public ObjectStoreBuilder( IDBStore store ) {
        this.store = store;
    }


    public void checkSchemas( EntityRepository repo, IDBDatabase db ) {
        this.db = db;
        Set<String> objectStoreNames = new HashSet<>( Arrays.asList( db.getObjectStoreNames() ) );
        
        for (ClassInfo<? extends Entity> entityClassInfo : repo.getConfig().entities.get()) {
            CompositeInfo<? extends Entity> info = repo.infoOf( entityClassInfo );
            if (!objectStoreNames.contains( info.getNameInStore() )) {
                createSchema( info ); 
            }
        }
    }


    protected void createSchema( CompositeInfo<? extends Entity> info ) {
        LOG.info( "Creating schema: " + info + " ..." );
        db.createObjectStore( info.getNameInStore(), IDBObjectStoreParameters.create().keyPath( "id" ) );
    }
}
