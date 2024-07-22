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

import org.polymap.model2.Association;
import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.engine.TemplateProperty;
import areca.common.Promise;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 * XXX not a {@link Quantifier}?
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class AssociationEquals<T extends Entity>
        extends Predicate {

    private static final Log LOG = LogFactory.getLog( AssociationEquals.class );

    public TemplateProperty<T>      prop;
    

    public AssociationEquals( TemplateProperty<T> assoc, BooleanExpression sub ) {
        super( sub );
        assert children.length == 1;
        assert children[0] != null;
        this.prop = assoc;
    }

    
    @Override
    protected String opName() {
        return prop.info().getName() + " is ";
    }


    public BooleanExpression subExp() {
        return children[0];
    }

    
    @Override
    public boolean evaluate( Composite target ) {
        throw new RuntimeException( "must not be called" );
    }


    @Override
    @SuppressWarnings({"unchecked"})
    public Promise<Boolean> evaluate2( Composite target ) {
        var propName = prop.info().getName();
        var propInfo = target.info().getProperty( propName );
        var assoc = (Association<T>)propInfo.get( target );
        LOG.debug( "%s : %s", assoc.info().getName(), subExp() );

        return assoc.fetch().map( associated -> {
            return subExp().evaluate( associated );
        });
    }

}
