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
package org.polymap.model2.runtime;

import java.util.List;
import java.util.function.Supplier;

import org.polymap.model2.Entity;
import org.polymap.model2.engine.EntityRepositoryImpl;
import org.polymap.model2.runtime.config.Mandatory;
import org.polymap.model2.runtime.config.Property;
import org.polymap.model2.runtime.locking.CommitLockStrategy;
import org.polymap.model2.store.StoreSPI;

import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;

/**
 * 
 */
@RuntimeInfo
public class Configuration {

    @Mandatory
    public Property<Configuration,StoreSPI>     store;
    
    @Mandatory
    public Property<Configuration,List<ClassInfo<? extends Entity>>> entities;
    
//    /**
//     * The CacheManager to create internal caches from. Mainly this is used to
//     * create the cache for {@link Entity} instances. If not specified then a
//     * default Cache ({@link SimpleCache}) implementation is used.
//     */
//    public Property<Configuration,CacheManager> cacheManager;
    
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
//        if (cacheManager.get() == null) {
//            cacheManager.set( new SimpleCacheManager() );
//        }
        if (commitLockStrategy.get() == null) {
            commitLockStrategy.set( () -> new CommitLockStrategy.Ignore() );
        }
        if (nameInStoreMapper.get() == null) {
            nameInStoreMapper.set( new DefaultNameInStoreMapper() );
        }
        return new EntityRepositoryImpl( this );
    }
}