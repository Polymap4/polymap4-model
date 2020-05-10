/* 
 * polymap.org
 * Copyright (C) 2016, Falko Bräutigam. All rights reserved.
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

import org.polymap.model2.runtime.PropertyInfo;

/**
 * Bases class a computed properties. See {@link Computed} annotation. 
 *
 * @see Computed
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class ComputedPropertyBase<T>
        implements PropertyBase<T> {

    protected PropertyInfo<T>   info;
    
    protected Composite         composite;
    
    
    public void init( PropertyInfo<T> _info, Composite _composite ) {
        this.composite = _composite;
        this.info = _info;
    }

    @Override
    public PropertyInfo info() {
        return info;
    }

}
