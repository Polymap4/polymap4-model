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

import org.polymap.model2.DefaultValue;
import org.polymap.model2.Immutable;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.engine.EntityRepositoryImpl.EntityRuntimeContextImpl;
import org.polymap.model2.runtime.PropertyInfo;

/**
 * Handles property contraints: {@link DefaultValue}, {@link DefaultValues},
 * {@link Immutable}, {@link Nullable}. This also raises the status of the
 * Entity when a property is modified.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
abstract class ConstraintsInterceptor<T>
        implements PropertyInterceptorBase<T> {

    protected static final Object       UNINITIALIZED = new Object();
    
    protected EntityRuntimeContextImpl  context;
    
    protected PropertyBase<T>           delegate;

    /** Store in variable for fast access. */
    protected boolean                   isImmutable;
    
    protected boolean                   isNullable;

    /**
     * Lazily init variable for fast access when frequently used. Don't use cool
     * {@link LazyInit} in order to save memory (one more Object per property
     * instance).
     */
    protected Object                    defaultValue = UNINITIALIZED;
    
    
    public ConstraintsInterceptor( PropertyBase<T> delegate, EntityRuntimeContextImpl context ) {
        this.delegate = delegate;
        this.context = context;
        PropertyInfo info = delegate.info();
        this.isImmutable = info.isImmutable();
        this.isNullable = info.isNullable();
    }


    @Override
    public PropertyBase<T> delegate() {
        return delegate;
    }


    protected String fullPropName() {
        return context.getEntity().info().getName() + "." + info().getName();
    }


    public PropertyInfo info() {
        return delegate.info();
    }

}
