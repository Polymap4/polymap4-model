/* 
 * polymap.org
 * Copyright 2012, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.store.geotools;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.runtime.config.Config2;
import org.polymap.core.runtime.config.Configurable;
import org.polymap.core.runtime.config.Defaults;

import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.StoreUnitOfWork;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class FeatureStoreAdapter
        extends Configurable
        implements StoreSPI {

    private static Log log = LogFactory.getLog( FeatureStoreAdapter.class );

    private StoreRuntimeContext         context;
    
    private DataAccess                  store;
    
    @Defaults
    public Config2<FeatureStoreAdapter,Boolean>  createOrUpdateSchemas;


    public FeatureStoreAdapter( DataAccess store ) {
        assert store != null;
        this.store = store;
    }


    public void init( StoreRuntimeContext _context ) {
        this.context = _context;
        EntityRepository repo = context.getRepository();
    
        // check/create/update schemas
        if (createOrUpdateSchemas.get()) {
            //        FeatureStoreUnitOfWork uow = (FeatureStoreUnitOfWork)createUnitOfWork();
            for (Class<? extends Entity> entityClass : repo.getConfig().entities.get()) {

                // is entityClass complex?
                boolean isComplex = false;
                Class superClass = entityClass; 
                for (;superClass != null; superClass = superClass.getSuperclass()) {
                    for (Field field : superClass.getDeclaredFields()) {
                        if (CollectionProperty.class.isAssignableFrom( field.getType() )) {
                            isComplex = true; 
                            break;
                        }
                        if (Property.class.isAssignableFrom( field.getType() )) {
                            Class binding = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                            if (Composite.class.isAssignableFrom( binding )) {
                                isComplex = true; 
                                break;
                            }
                        }
                    }
                }

                // check/update schema            
                FeatureType entitySchema = isComplex ? featureType( entityClass ) : simpleFeatureType( entityClass );
                try {
                    log.info( "Checking FeatureSource: " + entitySchema.getName().getLocalPart() + " ..." ); 
                    FeatureSource fs = store.getFeatureSource( entitySchema.getName() );
                    // update
                    if (fs != null && !entitySchema.equals( fs.getSchema() )) {
                        try {
                            log.warn( "FeatureType has been changed: " + entitySchema.getName() + " !!!" );
                            store.updateSchema( entitySchema.getName(), entitySchema );
                        }
                        catch (UnsupportedOperationException e) {
                            log.warn( "", e );
                        }
                    }
                }
                // create schema
                // fs.getSchema() throws RuntimeException for ShapefileDataSource
                catch (Exception e) {
                    try {
                        log.info( "No feature store found: " + e.getLocalizedMessage() + ". Creating schema: " + entitySchema ); 
                        store.createSchema( entitySchema );
                    }
                    catch (IOException e1) {
                        throw new ModelRuntimeException( e1 );
                    }
                }
            }
        }
    }


    public void close() {
    }

    
    public Object stateId( Object state ) {
        return ((Feature)state).getIdentifier().getID();
    }


    public DataAccess getStore() {
        return store;
    }

    
    public StoreUnitOfWork createUnitOfWork() {
        return new FeatureStoreUnitOfWork( context, this );
    }


    /**
     * Creates a new {@link FeatureType} instance for the given {@link Entity} class.
     * The returned instance does not depend on the actually type in the store.
     * 
     * @param <T>
     * @param entityClass
     * @return Newly created {@link FeatureType} instance.
     */
    public <T extends Entity> FeatureType featureType( Class<T> entityClass ) {
        try {
            return new FeatureTypeBuilder( entityClass ).build();
        }
        catch (Exception e) {
            throw new ModelRuntimeException( e );
        }
    }

    
    public <T extends Entity> SimpleFeatureType simpleFeatureType( Class<T> entityClass ) {
        try {
            return new SimpleFeatureTypeBuilder( entityClass ).build();
        }
        catch (Exception e) {
            throw new ModelRuntimeException( e );
        }
    }
    
}
