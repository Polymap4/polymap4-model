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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.PropertyInfo;

import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class BidiBackAssociationFinder {

    private static Log log = LogFactory.getLog( BidiBackAssociationFinder.class );
    
    /**
     * 
     *
     * @param context The runtime context of the host entity.
     * @param propInfo The property of the host {@link Association}.
     * @param target The target {@link Entity}.
     * @return The {@link Association} or {@link ManyAssociation} that is the back
     *         association in the target.
     */
    public static <T extends Entity> PropertyBase<T> findBackAssociation( 
            EntityRuntimeContext context, PropertyInfo propInfo, T target ) {
        // find my host entity
        Class hostType = context.getEntity().info().getType();

        // find back association
        Collection<PropertyInfo<?>> propInfos = target.info().getProperties();
        List<PropertyInfo> candidates = propInfos.stream()
                .filter( info -> !info.isComputed() && info.isAssociation() && info.getType().isAssignableFrom( hostType ) )
                .collect( Collectors.toList() );

        PropertyInfo backAssocInfo = null;
        // nothing found
        if (candidates.isEmpty()) {
            throw new IllegalStateException( "No back assocation found for: " + hostType.getSimpleName() + " in: " + target.getClass().getSimpleName() );
        }
        // multiple
        else if (candidates.size() > 1) {
            BidiAssociationName assocName = (BidiAssociationName)propInfo.getAnnotation( BidiAssociationName.class );
            if (assocName != null) {
                backAssocInfo = candidates.stream()
                        .filter( i -> i.getName().equals( assocName.value() ) ).findAny()
                        .orElseThrow( () -> new IllegalStateException( "No back assocation found for name: " + assocName.value() ) );
            }
            else {
                throw new IllegalStateException( "Multiple back assocation found for: " + hostType.getSimpleName() 
                        + " in: " + target.getClass().getSimpleName() 
                        + " as: " + candidates.stream().map( i -> i.getName() ).collect( Collectors.toList() ) + "."
                        + "Use @BidiAssocationName to specify which one to use.");
            }
        }
        // just one
        else {
            backAssocInfo = candidates.get( 0 );
        }

        return backAssocInfo.get( target );
    }

}
