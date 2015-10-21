/* 
 * polymap.org
 * Copyright (C) 2012-2014, Falko Bräutigam. All rights reserved.
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

import java.beans.PropertyChangeEvent;

import org.apache.commons.logging.LogFactory;import org.apache.commons.logging.Log;

import org.polymap.core.runtime.event.EventManager;

import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * Fires {@link PropertyChangeEvent}s via {@link EventManager} when a
 * {@link Property} changes.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PropertyChangeSupport
        extends PropertyConcernBase
        implements PropertyConcern, ManyAssociation {

    private static final Log log = LogFactory.getLog( PropertyChangeSupport.class );

    
    @Override
    public Object get() {
        return ((Property)delegate).get();
    }

    @Override
    public Object createValue( ValueInitializer initializer ) {
        return ((Property)delegate).createValue( initializer );
    }

    @Override
    public Object createValue( Class clazz, ValueInitializer initializer ) {
        return ((Property)delegate).createValue( clazz, initializer );
    }

    @Override
    public void set( Object value ) {
        ((Property)delegate).set( value );
        fireEvent( null, value );
    }

    @Override
    public boolean add( Object e ) {
       boolean result = ((ManyAssociation)delegate).add( e );
       fireEvent( null, e );
       return result;
    }

    protected void fireEvent( Object oldValue, Object newValue ) {
        PropertyInfo info = delegate.info();
        Entity entity = context.getCompositePart( Entity.class );
        PropertyChangeEvent event = new PropertyChangeEvent( entity, info.getName(), oldValue, newValue );
        EventManager.instance().publish( event );
    }

}
