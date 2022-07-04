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

import java.util.Arrays;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;

import areca.common.Assert;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class IdPredicate<T extends Entity>
        extends Predicate {

    public Object[]             ids;

    
    public IdPredicate( Object... ids ) {
        Assert.that( ids != null /*&& ids.length > 0*/, "IdPredicate: ids must not be null" );
//        assert !Arrays.stream( ids ).filter( id -> id instanceof Composite ).findAny().isPresent() 
//                : "Composite is not allowed as key. Did you cast parameter of Expressions.id() correctly?";
        this.ids = ids;
    }

    
    @Override
    public boolean evaluate( Composite target ) {
        for (Object id : ids) {
            if (((Entity)target).id().equals( id )) {
                return true;
            }
        }
        return false;
    }


    @Override
    public String toString() {
        return String.format( "id IN %s", Arrays.toString( ids ) );
    }
}
