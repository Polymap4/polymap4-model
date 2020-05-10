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

import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;

/**
 * This {@link Association} concern maintains the back reference of a bidirectional
 * association. The back reference can be an {@link Association} or a
 * {@link ManyAssociation}. If multiple possible back references exists then
 * {@link BidiAssociationName} annotation can be used to choose the one to use.
 *
 * @see BidiManyAssociationConcern
 * @see BidiAssociationName
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class BidiAssociationConcern<T extends Entity>
        extends PropertyConcernBase<T> 
        implements AssociationConcern<T> {

    private static Log log = LogFactory.getLog( BidiAssociationConcern.class );

    
    protected Association<T> _delegate() {
        return (Association<T>)super.delegate();        
    }
    
    @Override
    public T get() {
        return _delegate().get();
    }

    @Override
    public void set( T value ) {
        // value == null signals that association is removed, so we use current value as target
        T target = value != null ? value : get();

        Entity currentValue = _delegate().get();
        // avoid ping-pong between double-sided bidi associations
        if (currentValue == value) {
            return;
        }
        // check reset without prior remove old association
        if (value != null && currentValue != null) {
            throw new IllegalStateException( "Association is not null currently. Call set( null) before setting another association." );
        }
        
        // delegate
        _delegate().set( value );
        
        // find back association
        PropertyBase backAssoc = findBackAssociation( context, info(), target );

        // find my host entity
        Class hostType = context.getEntity().info().getType();
        Entity hostEntity = (Entity)context.getCompositePart( hostType );

        // set back reference
        // Association
        if (backAssoc instanceof Association) {
            ((Association)backAssoc).set( value != null ? hostEntity : null );
        }
        // ManyAssocation
        else if (backAssoc instanceof ManyAssociation) {
            if (value != null) {
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
