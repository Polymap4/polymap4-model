/* 
 * polymap.org
 * Copyright (C) 2015-2016, Falko Bräutigam. All rights reserved.
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
import static org.polymap.model2.query.Expressions.is;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * Provides a computed back reference of a bidirectional {@link Association}.
 * <p/>
 * Not cached. Every call of {@link #iterator()} or {@link #size()} executes a
 * {@link Query}. However, client code should not rely on this behaviour.
 * 
 * @see BidiAssociationName
 * @see BidiManyAssociationConcern
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class ComputedBidiManyAssocation<T extends Entity>
        extends ComputedManyAssociation<T> {

    protected ResultSet<T> results() {
        EntityRepository repo = composite.context.getRepository();
        Entity template = (Entity)Expressions.template( info.getType(), repo );
        PropertyBase<Entity> backAssoc = findBackAssociation( composite.context, info, template );
        assert backAssoc instanceof Association;
        
        UnitOfWork uow = composite.context.getUnitOfWork();
        return uow.query( (Class<T>)info.getType() )
                .where( is( ((Association)backAssoc), (Entity)composite ) )
                .execute();        
    }


    @Override
    public Iterator<T> iterator() {
         ResultSet<T> results = results();          
         // log.info( ":: " + Joiner.on(", ").join( results ) );

         // auto closing Iterator
         return new Iterator<T>() {
             Iterator<T> delegate = results.iterator();
             @Override
             public boolean hasNext() {
                 boolean result = delegate.hasNext();
                 if (result == false) {
                     results.close();
                 }
                 return result;
             }
             @Override
             public T next() {
                 return delegate.next();
             }
         };
    }


    @Override
    public int size() {
        try (ResultSet<T> results = results()) {        
            return results.size();
        }
    }


    @Override
    public boolean isEmpty() {
        return size() == 0;
    }


    /**
     * {@inheritDoc}
     * <p/>
     * Note that this uses {@link Entity#equals(Object)}. That is, entities from
     * different {@link UnitOfWork} instances are not equal!
     */
    @Override
    public boolean contains( Object o ) {
        assert o != null;
        for (T elm : this) {
            if (elm.equals( o )) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean containsAll( Collection<?> c ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public Object[] toArray() {
        try (
            ResultSet<T> results = results()
        ){
            return results.stream().collect( Collectors.toList() ).toArray();
        }
    }


    @Override
    public <U> U[] toArray( U[] a ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }
    
}
