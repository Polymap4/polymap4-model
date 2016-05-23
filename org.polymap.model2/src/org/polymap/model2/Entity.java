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

import java.util.Optional;

import org.polymap.model2.engine.UnitOfWorkImpl;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;

/**
 * An Entity is a directly instantiable {@link Composite} with an {@link #id()
 * identifier} and {@link #status()}.
 * <p/>
 * The <b>lifecycle</b> of an Entity is:
 * <ol>
 * <li>{@link EntityStatus#CREATED} - after instance has been created within an UnitOfWork; <i>or</i></li>
 * <li>{@link EntityStatus#LOADED} - after instance has been loaded into an UnitOfWork</li>
 * <li>{@link EntityStatus#MODIFIED}/{@link EntityStatus#REMOVED} - if instance was modified within its UnitOfWork</li>
 * <li>back to 2. after UnitOfWork has been committed
 * <li>{@link EntityStatus#DETACHED} - after {@link UnitOfWork} has been closed</li>
 * </ol>
 * 
 * @see Composite
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class Entity
        extends Composite {
    
    public Object id() {
        return context.id();
    }
    
    public EntityStatus status() {
        return context.getStatus();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id() + ",status=" + status() 
                + (status() != EntityStatus.DETACHED ? ",state=" + state() : "") + "]" ;
    }

    /**
     * By default two entities are {@link #equals(Object) equal} only if they are the
     * same object. That is, even two {@link Entity} instances refering the same state
     * are not equal if they were instantiated in differnet {@link UnitOfWork}s.
     */
    @Override
    public boolean equals( Object obj ) {
        return this == obj;
    }

    /**
     * Casts this entity into one of its Mixin types. Mixins are defined via the
     * {@link Mixins} annotation or at runtime. Creating runtime Mixins is not as
     * efficient as Mixins defined via the annotation.
     * 
     * @param <T>
     * @param mixinClass
     * @return A mixin of the given type, or null if no such mixin was defined.
     */
    public <T extends Composite> Optional<T> as( Class<T> mixinClass ) {
        T result = ((UnitOfWorkImpl)context.getUnitOfWork()).mixin( mixinClass, this );
        return Optional.ofNullable( result );
    }

}
