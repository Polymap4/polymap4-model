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
package org.polymap.model2.engine;

import org.polymap.model2.Property;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.store.StoreProperty;

import areca.common.Assert;

/**
 * Property implementation for simple (non-Composite) values.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class PropertyImpl<T>
        implements Property<T> {

    private StoreProperty<T>        storeProp;

    
    protected PropertyImpl( StoreProperty<T> storeProp ) {
        this.storeProp = Assert.notNull( storeProp );
    }

    protected StoreProperty<T> delegate() {
        return storeProp;
    }
    
    @Override
    public T get() {
        // no cache here; the store should decide when and what to cache.
        return storeProp.get();
    }

    @Override
    public <U extends T> U createValue( ValueInitializer<U> initializer ) {
        U result = (U)storeProp.createValue( null );
        if (initializer != null) {
            try {
                //assert !(initializer instanceof TypedValueInitializer) : "TypeValueInitializer not allowed for simple type.";
                result = initializer.initialize( result );
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new ModelRuntimeException( e );
            }
        }
        return result;
    }

    @Override
    public void set( T value ) {
        storeProp.set( value );
    }

    @Override
    public PropertyInfo info() {
        return storeProp.info();
    }

    @Override
    public String toString() {
        T value = get();
        return "Property[name:" + info().getName() + ",value=" + (value != null ? value.toString() : "null") + "]";
    }
    
}
