/* 
 * polymap.org
 * Copyright (C) 2016, the @authors. All rights reserved.
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
package org.polymap.model2.engine;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.runtime.CompositeStateVisitor;

/**
 * Resets all internal caches of a {@link Composite}.
 *
 * @author Falko Bräutigam
 */
public class ResetCachesVisitor
        extends CompositeStateVisitor<RuntimeException> {

    @Override
    protected void visitProperty( Property prop ) {
        resetCache( prop );
    }

    @Override
    protected void visitCollectionProperty( CollectionProperty prop ) {
        resetCache( prop );
    }

    @Override
    protected void visitAssociation( Association prop ) {
        resetCache( prop );
    }

    @Override
    protected void visitManyAssociation( ManyAssociation prop ) {
        resetCache( prop );
    }

    @Override
    protected boolean visitCompositeCollectionProperty( CollectionProperty prop ) {
        resetCache( prop );
        return true;
    }

    @Override
    protected boolean visitCompositeProperty( Property prop ) {
        resetCache( prop );
        return true;
    }

    protected void resetCache( PropertyBase prop ) {
        PropertyInterceptorBase.visitDelegates( prop, delegate -> {
            if (delegate instanceof CachingProperty) {
                ((CachingProperty)delegate).clearCache();
            }
            return true;
        });
    }
    
}
