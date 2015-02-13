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

import org.polymap.model2.Composite;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreProperty;

/**
 *
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class CompositePropertyImpl<T extends Composite>
        implements Property<T> {

    public static final Object              NULL_VALUE = new Object();
    
    private EntityRuntimeContext            entityContext;
    
    private StoreProperty<CompositeState>   storeProp;

    /**
     * Cache of the Composite value. As building the Composite is an expensive
     * operation the Composite and the corresponding {@link CompositeState} is cached
     * here (in contrast to primitive values).
     * @see CompositeCollectionPropertyImpl
     */
    private Object                          value;


    protected CompositePropertyImpl( EntityRuntimeContext entityContext, 
            StoreProperty<CompositeState> underlying ) {
        this.storeProp = underlying;
        this.entityContext = entityContext;
    }

    
    @Override
    public T get() {
        
        // no synchronization, concurrent init is ok
        if (value == null) {
            CompositeState state = storeProp.get();
            if (state != null) {
                InstanceBuilder builder = new InstanceBuilder( entityContext );
                value = builder.newComposite( state, getInfo().getType() );
            }
            else {
                value = NULL_VALUE;
            }
        }
        return value != NULL_VALUE ? (T)value : null;
    }

    
    @Override
    public void set( T value ) {
        this.value = value;
        storeProp.set( value.state() );
    }

    
    @Override
    public T createValue( ValueInitializer<T> initializer ) {
        T result = get();
        if (result == null) {
            synchronized (this) {
                CompositeState state = storeProp.createValue();
                assert state != null : "Store must not return null as newValue().";
                InstanceBuilder builder = new InstanceBuilder( entityContext );
                result = (T)builder.newComposite( state, getInfo().getType() );

                if (initializer != null) {
                    try {
                        result = initializer.initialize( result );
                    }
                    catch (RuntimeException e) {
                        throw e;
                    }
                    catch (Exception e) {
                        throw new ModelRuntimeException( e );
                    }
                }
                value = result;
            }
        }
        return result;
    }


    @Override
    public PropertyInfo getInfo() {
        return storeProp.getInfo();
    }
    

    @Override
    public String toString() {
        return "Property[name:" + getInfo().getName() + ",value=" + get() + "]";
    }

}