/* 
 * polymap.org
 * Copyright (C) 2022, Falko Bräutigam. All rights reserved.
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

import areca.common.Assert;

/**
 * 
 * @author Falko
 */
public class CollectionQuantifier<T>
        extends Quantifier<CollectionProperty<T>,T> {

    public T value;

    public CollectionQuantifier( Type type, CollectionProperty<T> prop, T value ) {
        super( type, prop );
        Assert.that( !Composite.class.isInstance( value ) );
        this.value = value;
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public boolean evaluate( Composite target ) {
        String propName = prop.info().getName();
        PropertyInfo<?> propInfo = target.info().getProperty( propName );
        CollectionProperty<T> targetProp = (CollectionProperty<T>)propInfo.get( target );

        for (T propValue : targetProp) {
            boolean subResult = propValue.equals( value );
            if (type == Type.ANY && subResult) {
                return true;
            }
            if (type == Type.ALL && !subResult) {
                return false;
            }
        }
        return type == Type.ANY ? false : true;
    }


    @Override
    public BooleanExpression subExp() {
        throw new RuntimeException( "There is no sub-expression for collection of primitive types." );
    }
    
}
