/* 
 * polymap.org
 * Copyright (C) 2016, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Property;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class CopyCompositeState<C extends Composite>
        extends CompositeStateVisitor<RuntimeException> {

    private static Log log = LogFactory.getLog( CopyCompositeState.class );
    
    public static <T extends Composite> CopyCompositeState<T> from( T from ) {
        return new CopyCompositeState( from );
    }

    // instance *******************************************
    
    protected C             from;
    
    protected C             to;
    
    public CopyCompositeState( C from ) {
        this.from = from;
    }
    
    public void to( @SuppressWarnings("hiding") C to ) {
        this.to = to;
        process( from );
    }

    @Override
    protected void visitProperty( Property prop ) {
        ((Property)prop.info().get( to )).set( prop.get() );
    }

    @Override
    protected void visitAssociation( Association prop ) {
        ((Association)prop.info().get( to )).set( prop.get() );
    }

    @Override
    protected boolean visitCompositeProperty( Property prop ) {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    protected void visitCollectionProperty( CollectionProperty prop ) {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    protected boolean visitCompositeCollectionProperty( CollectionProperty prop ) {
        throw new RuntimeException( "not yet implemented." );
    }
    
}
