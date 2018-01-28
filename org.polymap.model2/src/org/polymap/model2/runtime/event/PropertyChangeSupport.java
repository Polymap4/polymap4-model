/* 
 * polymap.org
 * Copyright (C) 2012-2018, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.runtime.event;

import java.util.Collection;

import java.beans.PropertyChangeEvent;

import org.polymap.core.runtime.event.EventManager;

import org.polymap.model2.CollectionProperty;
import org.polymap.model2.CollectionPropertyConcern;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * Fires {@link PropertyChangeEvent}s via {@link EventManager} when the value of a
 * property changes. Supports {@link Property}, {@link CollectionProperty} and
 * {@link ManyAssociation}.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PropertyChangeSupport
        extends PropertyConcernBase
        implements PropertyConcern, CollectionPropertyConcern, ManyAssociation {

    @Override
    public Object get() {
        return ((Property)delegate).get();
    }

    @Override
    public Object createValue( ValueInitializer initializer ) {
        return ((Property)delegate).createValue( initializer );
    }

    @Override
    public void set( Object value ) {
        ((Property)delegate).set( value );
        fireEvent( null, value );
    }

    // Collection: ManyAssociation or CollectionProperty
    
    @Override
    public boolean add( Object e ) {
       boolean result = ((Collection)delegate).add( e );
       fireEvent( null, e );
       return result;
    }

    @Override
    public boolean remove( Object e ) {
       boolean result = ((Collection)delegate).remove( e );
       fireEvent( null, e );
       return result;
    }

    @Override
    public boolean addAll( Collection c ) {
        boolean result = ((Collection)delegate).addAll( c );
        fireEvent( null, null );
        return result;
    }

    @Override
    public boolean removeAll( Collection c ) {
        boolean result = ((Collection)delegate).removeAll( c );
        fireEvent( null, null );
        return result;
    }

    @Override
    public boolean retainAll( Collection c ) {
        boolean result = ((Collection)delegate).retainAll( c );
        fireEvent( null, null );
        return result;
    }

    @Override
    public void clear() {
        ((Collection)delegate).clear();
        fireEvent( null, null );
    }
    
    /** {@link CollectionPropertyConcern} */
    @Override
    public Object createElement( ValueInitializer initializer ) {
        Object result = ((CollectionProperty)delegate).createElement( initializer );
        fireEvent( null, null );
        return result;
    }

    protected void fireEvent( Object oldValue, Object newValue ) {
        PropertyInfo info = delegate.info();
        Entity entity = context.getCompositePart( Entity.class );
        PropertyChangeEvent event = new PropertyChangeEvent( entity, info.getName(), oldValue, newValue );
        EventManager.instance().publish( event );
    }

}
