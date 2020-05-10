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

import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.ValueInitializer;

import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;
import areca.common.event.EventManager;

/**
 * Fires a {@link PropertyAccessEvent} via {@link EventManager} when a
 * {@link Property} is accessed.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PropertyAccessSupport
        extends PropertyConcernBase
        implements PropertyConcern, ManyAssociation {

    private static final Log log = LogFactory.getLog( PropertyAccessSupport.class );

    
    @Override
    public Object get() {
        Object result = ((Property)delegate).get();
        fireEvent( result );
        return result;
    }

    @Override
    public Object createValue( ValueInitializer initializer ) {
        return ((Property)delegate).createValue( initializer );
    }

    @Override
    public void set( Object value ) {
        ((Property)delegate).set( value );
    }

    @Override
    public boolean add( Object e ) {
       return ((ManyAssociation)delegate).add( e );
    }

    protected void fireEvent( Object value ) {
        PropertyInfo info = delegate.info();
        Entity entity = context.getCompositePart( Entity.class );
        PropertyAccessEvent ev = new PropertyAccessEvent( entity, info, value );
        throw new RuntimeException( "EventManager.instance().publish( event )" );
    }

}
