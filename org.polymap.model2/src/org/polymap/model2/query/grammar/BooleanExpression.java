/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
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

import org.polymap.model2.Composite;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.engine.TemplateProperty;
import org.polymap.model2.runtime.PropertyInfo;

import areca.common.base.Sequence;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class BooleanExpression {

    public BooleanExpression[]  children;

    
    public BooleanExpression( BooleanExpression... children ) {
        this.children = children;
    }
    
    /**
     * Evaluates the boolean expression agains a target object.
     *
     * @param target The target object.
     * @return true If boolean expression evaluates to TRUE for the target object.
     */
    public abstract boolean evaluate( Composite target );

    
    // util methods
    
    protected <P extends PropertyBase<T>,T> P targetProp( Composite target, TemplateProperty<T> prop ) {
        assert target != null;
        assert prop != null;
        String propName = prop.info().getName();
        PropertyInfo propInfo = target.info().getProperty( propName );
        return (P)propInfo.get( target );
    }
    
    protected <T> T propValue( Composite target, TemplateProperty<T> prop ) {
        assert target != null;
        assert prop != null;
        String propName = prop.info().getName();
        PropertyInfo propInfo = target.info().getProperty( propName );
        Property<T> targetProp = (Property<T>)propInfo.get( target );
        return targetProp.get();
    }

    @Override
    public String toString() {
        return  opName() + "(" + String.join( ", ", childrenToString() ) + ")";
    }

    protected Iterable<String> childrenToString() {
        return  Sequence.of( children ).transform( BooleanExpression::toString ).asIterable();
    }
    
    /**
     * The name of this operator. Used by {@link #toString()}
     */
    protected String opName() {
        return getClass().getSimpleName();
    }
    
}
