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
package org.polymap.model2.runtime;

import java.util.function.Supplier;

import javax.cache.CacheManager;

import org.apache.commons.logging.LogFactory;import org.apache.commons.logging.Log;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.engine.EntityRepositoryImpl;
import org.polymap.model2.engine.cache.SimpleCache;
import org.polymap.model2.engine.cache.SimpleCacheManager;
import org.polymap.model2.runtime.config.ConfigurationFactory;
import org.polymap.model2.runtime.config.Mandatory;
import org.polymap.model2.runtime.config.Property;
import org.polymap.model2.runtime.locking.CommitLockStrategy;
import org.polymap.model2.store.StoreSPI;

/**
 * 
 * <p/>
 * One repository is backed by exactly one underlying store. Client may decide to
 * work with different repositories and their {@link UnitOfWork} instances. It is
 * responsible of synchronizing commit/rollback between those instances.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class EntityRepository
        implements AutoCloseable {

    private static Log log = LogFactory.getLog( EntityRepository.class );

    // config factory *************************************
    
    /**
     * Returns a new Configuration to {@link Configuration#create()} a new
     * {@link EntityRepository} from.
     */
    public static Configuration newConfiguration() {
        return ConfigurationFactory.create( Configuration.class );
    }
    
    /**
     * 
     */
    public static class Configuration {

        @Mandatory
        public Property<Configuration,StoreSPI>     store;
        
        @Mandatory
        public Property<Configuration,Class<Entity>[]> entities;
        
        /**
         * The CacheManager to create internal caches from. Mainly this is used to
         * create the cache for {@link Entity} instances. If not specified then a
         * default Cache ({@link SimpleCache}) implementation is used.
         */
        public Property<Configuration,CacheManager> cacheManager;
        
        /**
         * The strategy to handle concurrent attempts to prepare/commit. Defaults to
         * {@link CommitLockStrategy.Serialize}
         * 
         * @see CommitLockStrategy.Serialize
         */
        public Property<Configuration,Supplier<CommitLockStrategy>> commitLockStrategy;
        
        /**
         * @deprecated Not supported yet.
         */
        public Property<Configuration,NameInStoreMapper> nameInStoreMapper;
        
        public EntityRepository create() {
            if (cacheManager.get() == null) {
                cacheManager.set( new SimpleCacheManager() );
            }
            if (commitLockStrategy.get() == null) {
                commitLockStrategy.set( () -> new CommitLockStrategy.Serialize() );
            }
            if (nameInStoreMapper.get() == null) {
                nameInStoreMapper.set( new DefaultNameInStoreMapper() );
            }
            return new EntityRepositoryImpl( this );
        }
    }
        

    // instance *******************************************
    
    public abstract StoreSPI getStore();
    
    public abstract Configuration getConfig();
    
    public abstract void close();


    /**
     * 
     * 
     * @param <T>
     * @param compositeClass Class of {@link Entity}, Mixin or complex property.
     * @return The info object, or null if the given Class is not an Entity, Mixin or
     *         complex property in this repository.
     */
    public abstract <T extends Composite> CompositeInfo<T> infoOf( Class<T> compositeClass );
    
    
    /**
     * Creates a new {@link UnitOfWork} for this repository.
     */
    public abstract UnitOfWork newUnitOfWork();
    
}
