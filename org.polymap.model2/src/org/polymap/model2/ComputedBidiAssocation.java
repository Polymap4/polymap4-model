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
package org.polymap.model2;

import static org.polymap.model2.BidiBackAssociationFinder.findBackAssociation;
import static org.polymap.model2.query.Expressions.anyOf;
import static org.polymap.model2.query.Expressions.id;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Provides a computed back reference of a bidirectional {@link ManyAssociation}.
 * <p/>
 * Not cached. Every call of {@link #get()} executes a {@link Query}.
 * 
 * @see BidiAssociationName
 * @see BidiAssociationConcern
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class ComputedBidiAssocation<T extends Entity>
        extends ComputedAssociation<T> {

    private static Log log = LogFactory.getLog( ComputedBidiAssocation.class );

    
    @Override
    public T get() {
        EntityRepository repo = composite.context.getRepository();
        Entity template = (Entity)Expressions.template( info.getType(), repo );
        PropertyBase<Entity> backAssoc = findBackAssociation( composite.context, info, template );
        
        UnitOfWork uow = composite.context.getUnitOfWork();
        // ManyAssociation
        if (backAssoc instanceof ManyAssociation) {
            ResultSet<T> results = uow.query( (Class<T>)info.getType() )
                    .where( anyOf( ((ManyAssociation)backAssoc), id( (Entity)composite ) ) )
                    .execute();
            assert results.size() <= 1;
            return results.stream().findAny().orElse( null );
        }
        else {
            throw new IllegalStateException( "Unknown backAssoc type: " + backAssoc.getClass().getSimpleName() );
        }
    }
    
}
