/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
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
import org.polymap.model2.engine.EntityRepositoryImpl.EntityRuntimeContextImpl;
import org.polymap.model2.runtime.ImmutableException;
import org.polymap.model2.runtime.NotNullableException;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;

/**
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
final class ConstraintsPropertyInterceptor<T>
        extends ConstraintsInterceptor<T>
        implements Property<T> {

    /**
     * Lazily init variable for fast access when frequently used. Don't use cool
     * {@link LazyInit} in order to save memory (one more Object per property
     * instance).
     */
    protected Object                    defaultValue = UNINITIALIZED;
    

    public ConstraintsPropertyInterceptor( Property<T> delegate, EntityRuntimeContextImpl context ) {
        super( delegate, context );
    }

    
    public Property<T> delegate() {
        return (Property<T>)delegate;
    }
    
    
    @Override
    public T get() {
        context.checkState();
        
        T value = delegate().get();
        
        // check/init default value
        if (value == null) {
            if (defaultValue == UNINITIALIZED) {
                // not synchronized; concurrent inits are ok here 
                defaultValue = delegate.info().getDefaultValue();
            }
            value = (T)defaultValue;
        }
        // check Nullable
        if (value == null && !isNullable) {
            throw new NotNullableException( "Property is not @Nullable: " + fullPropName() );
        }
        return value;
    }

    
    @Override
    public void set( T value ) {
        context.checkState();
        
        // XXX this should always fail outside a ValueInitializer
        if (isImmutable && delegate().get() != null) {
            throw new ImmutableException( "Property is @Immutable: " + fullPropName() );
        }
        if (!isNullable && value == null) {
            throw new NotNullableException( "Property is not @Nullable: " + fullPropName() );
        }
        delegate().set( value );
        
        context.raiseStatus( EntityStatus.MODIFIED );
    }

    
    @Override
    public <U extends T> U createValue( ValueInitializer<U> initializer ) {
        context.checkState();
        return delegate().createValue( initializer );
    }


    @Override
    public String toString() {
        return delegate().toString();
    }

}
