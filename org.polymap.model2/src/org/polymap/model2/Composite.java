/* 
 * polymap.org
 * Copyright (C) 2012-2016, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;

import areca.common.reflect.ClassInfo;

/**
 * A Composite is the base abstraction for defining a domain model. A Composite can
 * be an {@link Entity}, a Mixin or a complex Property. A Composite consists of a
 * number of Properties. Properties can have primitive or Composite values or a
 * Collection thereof.
 * <p/>
 * <b>Properties</b> are declared as:
 * <ul>
 * <li>{@link Property}: primitive or {@link Composite} value</li>
 * <li>{@link CollectionProperty}: collection of primitive values or
 * {@link Composite}s</li>
 * <li>{@link Association}: single association with another {@link Entity}</li>
 * <li>...</li>
 * </ul>
 * members.
 * <p/>
 * <b>Runtime information</b> about an instance of a Composite can be retrieved by
 * calling {@link #info()}.
 * <p/>
 * A <b>template</b> instance of a concrete Composite is automatically
 * <b>injected</b> into a static member with the name "TYPE". Sub-classes may expose
 * such a static member in order to access information about properties without a
 * conrete instance of this Composite. See also
 * {@link Expressions#template(Class, org.polymap.model2.runtime.EntityRepository)}.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class Composite {

    public EntityRuntimeContext      context;
    

    public Object state() {
        return context.getState().getUnderlying();
    }
    
    /**
     * Static type and model information about this Composite.
     */
    public CompositeInfo<?> info() {
        return context.getRepository().infoOf( ClassInfo.of( getClass() ) );
    }

    public String toString() {
        return getClass().getSimpleName() + 
                (context.getStatus() == EntityStatus.DETACHED ? "[detached!]" : "[state=" + state() + "]") ;
    }
    
    /**
     * Not supported by any Composite type other than an Entity.
     */
    @Override
    public boolean equals( Object obj ) {
        throw new UnsupportedOperationException( "Composite (mixin or Composite property value) does not provide a default implementation of equals()." );
    }

}
