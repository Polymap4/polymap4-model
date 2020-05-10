/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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

import static org.polymap.model2.BidiBackAssociationFinder.findBackAssociation;

import org.polymap.model2.runtime.ValueInitializer;

import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;

/**
 * This {@link ManyAssociation} concern maintains the back reference of a
 * bidirectional association. The back reference can be an {@link Association} or a
 * {@link ManyAssociation}. If multiple possible back references exists then
 * {@link BidiAssociationName} annotation can be used to choose the one to use.
 *
 * @see BidiAssociationConcern
 * @see BidiAssociationName
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class BidiManyAssociationConcern<T extends Entity>
        extends PropertyConcernBase<T> 
        implements PropertyConcern<T>, ManyAssociation<T> {

    private static Log log = LogFactory.getLog( BidiManyAssociationConcern.class );

    
    @Override
    public T get() {
        throw new RuntimeException( "not implemented." );
    }
    
    @Override
    public void set( T value ) {
        throw new RuntimeException( "not implemented." );
    }

    @Override
    public <U extends T> U createValue( ValueInitializer<U> initializer ) {
        throw new RuntimeException( "not implemented." );
    }

    protected ManyAssociation _delegate() {
        return (ManyAssociation)delegate();
    }

    @Override
    public boolean add( T element ) {
        // avoid ping-pong between double-sided bidi associations
        if (_delegate().contains( element )) {
            return false;
        }
        else {
            _delegate().add( element );
            updateBackReference( element, true );
            return true;
        }
    }


    @Override
    public boolean remove( Object element ) {
        // avoid ping-pong between double-sided bidi associations
        if (!_delegate().contains( element )) {
            return false;
        }
        else {
            _delegate().remove( element );
            updateBackReference( (T)element, false );
            return true;
        }
    }


    protected void updateBackReference( T target, boolean add ) {
        // find back association
        PropertyBase backAssoc = findBackAssociation( context, info(), target );

        Class hostType = context.getEntity().info().getType();
        Entity hostEntity = (Entity)context.getCompositePart( hostType );

        // Association
        if (backAssoc instanceof Association) {
            ((Association)backAssoc).set( add ? hostEntity : null );
        }
        // ManyAssocation
        else if (backAssoc instanceof ManyAssociation) {
            if (add) {
                ((ManyAssociation)backAssoc).add( hostEntity );
            }
            else {
                ((ManyAssociation)backAssoc).remove( hostEntity );                
            }
        }
        else {
            throw new IllegalStateException( "Unknown association type: " + backAssoc.getClass().getSimpleName() );            
        }
    }
    
}
