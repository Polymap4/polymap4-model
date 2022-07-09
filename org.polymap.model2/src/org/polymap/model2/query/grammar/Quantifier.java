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

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.PropertyBase;

/**
 * A Quantifier allows to query any Composite property (single {@link Composite},
 * Collection, {@link Association} or {@link ManyAssociation})
 * <p>
 * P is {@link CollectionProperty} or {@link ManyAssociation}.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class Quantifier<P extends PropertyBase<T>, T>
        extends BooleanExpression {

    public enum Type {
        ANY, ALL, THE_ONLY
    }
    
    public Type         type;
    
    public P            prop;
    
    
    public Quantifier( Type type, P prop, BooleanExpression subExp ) {
        super( subExp );
        assert type != null;
        assert prop != null;
        assert subExp != null;
        
        this.type = type;
        this.prop = prop;
    }

    
    public Quantifier( Type type, P prop ) {
        this.type = type;
        this.prop = prop;
    }

    
    public BooleanExpression subExp() {
        return children[0];
    }
    
}
