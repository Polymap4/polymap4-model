/* 
 * polymap.org
 * Copyright (C) 2016, the @authors. All rights reserved.
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
package org.polymap.model2;

import java.util.Collection;

/**
 * Bases class of computed {@link ManyAssociation} implementations. See
 * {@link Computed} annotation.
 *
 * @author Falko Bräutigam
 */
public abstract class ComputedManyAssociation<T extends Entity>
        extends ComputedPropertyBase<T>
        implements ManyAssociation<T> {
    
    @Override
    public boolean add( T e ) {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public boolean remove( Object o ) {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public boolean addAll( Collection<? extends T> c ) {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public boolean removeAll( Collection<?> c ) {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public boolean retainAll( Collection<?> c ) {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public String toString() {
        return "ComputedManyAssociation[name:" + info().getName() + ",elms=???" /*+ Joiner.on(", ").join( this )*/ + "]";
    }

}
