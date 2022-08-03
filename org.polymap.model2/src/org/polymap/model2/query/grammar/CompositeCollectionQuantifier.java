/* 
 * polymap.org
 * Copyright (C) 2015-2022, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.query.grammar;

import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.runtime.PropertyInfo;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class CompositeCollectionQuantifier<T extends Composite>
        extends Quantifier<CollectionProperty<T>,T> {

    public CompositeCollectionQuantifier( Type type, CollectionProperty<T> prop, BooleanExpression subExp ) {
        super( type, prop, subExp );
    }

    
    @Override
    @SuppressWarnings("rawtypes")
    public boolean evaluate( Composite target ) {
        String propName = prop.info().getName();
        PropertyInfo propInfo = target.info().getProperty( propName );
        CollectionProperty<T> targetProp = (CollectionProperty<T>)propInfo.get( target );

        for (T composite : targetProp) {

            boolean subResult = subExp().evaluate( composite );
            
            if (type == Type.ANY && subResult) {
                return true;
            }
            else if (type == Type.ALL && !subResult) {
                return false;
            }
        }
        return type == Type.ANY ? false : true;
    }


    @Override
    public String toString() {
        return String.format( "%s %s [%s]", type, prop.info().getName(), subExp() );
    }
    
}
