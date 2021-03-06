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

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class AssociationEquals<T extends Entity>
        extends Predicate {

    public TemplateProperty<T>      assoc;
    

    public AssociationEquals( TemplateProperty<T> assoc, BooleanExpression sub ) {
        super( sub );
        assert children.length == 1;
        assert children[0] != null;
        this.assoc = assoc;
    }

    @Override
    public boolean evaluate( Composite target ) {
        Association<T> targetProp = targetProp( target, assoc );
        Entity entity = targetProp.get();
        return entity != null && children[0].evaluate( entity );
    }
    
}
