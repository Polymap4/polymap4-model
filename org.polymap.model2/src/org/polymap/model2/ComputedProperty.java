/* 
 * polymap.org
 * Copyright (C) 2014-2016, Falko Bräutigam. All rights reserved.
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

import org.polymap.model2.runtime.ValueInitializer;

/**
 * Bases class of computed {@link Property} implementations. See {@link Computed}
 * annotation.
 *
 * @see Computed
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class ComputedProperty<T>
        extends ComputedPropertyBase<T>
        implements Property<T> {

    @Override
    public <U extends T> U createValue( ValueInitializer<U> initializer ) {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public void set( T value ) {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public String toString() {
        T value = get();
        return "ComputedProperty[name:" + info().getName() + ",value=" + (value != null ? value.toString() : "null") + "]";
    }

}
