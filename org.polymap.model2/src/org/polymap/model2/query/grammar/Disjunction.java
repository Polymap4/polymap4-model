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

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Disjunction
        extends BooleanExpression {

    
    public Disjunction( BooleanExpression... expressions ) {
        super( expressions );
        assert expressions.length >= 2;
        assert expressions[0] != null;
        assert expressions[1] != null;
    }
    

    @Override
    public boolean evaluate( Composite target ) {
        for (BooleanExpression child : children) {
            if (child.evaluate( target )) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "(" + String.join( " " + opName() + " ", childrenToString() ) + ")";
    }

    @Override
    protected String opName() {
        return "OR";
    }

}
