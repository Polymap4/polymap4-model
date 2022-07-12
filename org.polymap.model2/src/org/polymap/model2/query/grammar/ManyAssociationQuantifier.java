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
package org.polymap.model2.query.grammar;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.runtime.PropertyInfo;

import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class ManyAssociationQuantifier<T extends Entity>
        extends Quantifier<ManyAssociation<T>,T> {

    private static final Log LOG = LogFactory.getLog( ManyAssociationQuantifier.class );

    public ManyAssociationQuantifier( Type type, ManyAssociation<T> prop, BooleanExpression subExp ) {
        super( type, prop, subExp );
    }

    
    @Override
    public boolean evaluate( Composite target ) {
        throw new RuntimeException( "must not be called" );
    }


    @Override
    @SuppressWarnings("rawtypes")
    public Promise<Boolean> evaluate2( Composite target ) {
        String propName = prop.info().getName();
        PropertyInfo propInfo = target.info().getProperty( propName );
        ManyAssociation<T> association = (ManyAssociation<T>)propInfo.get( target );
        LOG.info( "%s : %s", association.info().getName(), subExp() );
        
        return association.fetchCollect().map( associateds -> {
            LOG.info( "Fetched: %s", associateds.size() );
            for (T associated : associateds) {
                boolean subResult = subExp().evaluate( associated );
                if (type == Type.ANY && subResult) {
                    return true;
                }
                else if (type == Type.ALL && !subResult) {
                    return false;
                }
            }
            return type == Type.ANY ? false : true;
        }).onSuccess( result -> LOG.info( "Result: %s", result ) );
    }

}
