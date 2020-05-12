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

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.runtime.config.ConfigurationFactory;
import org.polymap.model2.store.StoreSPI;

import areca.common.reflect.ClassInfo;

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

    /**
     * Returns a new Configuration to {@link Configuration#create()} a new
     * {@link EntityRepository} from.
     */
    public static Configuration newConfiguration() {
        return ConfigurationFactory.create( Configuration.info );
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
    public abstract <T extends Composite> CompositeInfo<T> infoOf( ClassInfo<T> compositeClassInfo );
    
    
    /**
     * Creates a new {@link UnitOfWork} for this repository.
     */
    public abstract UnitOfWork newUnitOfWork();
    
}
