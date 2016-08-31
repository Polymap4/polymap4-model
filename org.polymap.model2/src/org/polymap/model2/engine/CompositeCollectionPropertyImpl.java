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
package org.polymap.model2.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Iterators;

import org.polymap.model2.Composite;
import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreCollectionProperty;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class CompositeCollectionPropertyImpl<T extends Composite>
        extends CollectionPropertyImpl<T>
        implements CachingProperty {

    private static Log log = LogFactory.getLog( CompositeCollectionPropertyImpl.class );

    /**
     * Cache of the Composite value. As building the Composite is an expensive
     * operation the Composite and the corresponding {@link CompositeState} is cached
     * here (in contrast to primitive values). This mimics the cache behaviour of the
     * UnitOfWork.
     */
    // XXX make it a Cache?
    private List<T>                 cache;

    
    public CompositeCollectionPropertyImpl( EntityRuntimeContext entityContext, StoreCollectionProperty storeProp ) {
        super( entityContext, storeProp );
    }

    
    @Override
    public <U extends T> U createElement( ValueInitializer<U> initializer ) {
        Class actualType = initializer.rawResultType().orElse( info().getType() );

        CompositeState state = (CompositeState)storeProp.createValue( actualType );
                
        InstanceBuilder builder = new InstanceBuilder( entityContext );
        Composite value = builder.newComposite( state, (Class<U>)actualType );
        
        if (initializer != null) {
            try {
                value = initializer.initialize( (U)value );
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new ModelRuntimeException( e );
            }
        }
        
        // cache
        checkInitCache().add( (T)value );
        return (U)value;
    }

    
    /**
     * Simple, straight forward: init all elements on first access. This is not very
     * smart but other implementations are ticky; this one works and, as number of
     * elements in a Composite collection is limited in "most cases", performs ok 
     */
    protected List<T> checkInitCache() {
        if (cache == null) {
            cache = new ArrayList();
            // always completely iterating until hasNext()==false 'fixes' this problem that
            // iterator has no close(); hasNext()=false signals the impl to close the connection
            Iterator<CompositeState> it = (Iterator<CompositeState>)storeProp.iterator();
            while (it.hasNext()) {
                CompositeState state = it.next();
                InstanceBuilder builder = new InstanceBuilder( entityContext );
                T instance = (T)builder.newComposite( state, state.compositeInstanceType( info().getType() ) );
                cache.add( instance );
            }
        }
        return cache;
    }

    
    @Override
    public void clearCache() {
        // XXX client code may reference the old instrances; so this produces
        // new Composite instances while another instance for the same state may already exists!
        cache = null;
    }
    
    
    @Override
    public void clear() {
        super.clear();
        clearCache();
    }


    @Override
    public Iterator<T> iterator() {
        // FIXME remove not supported yet
        return Iterators.unmodifiableIterator( checkInitCache().iterator() );
        
//        return new Iterator<T>() {
//            private Iterator        storeIt = storeProp.iterator();
//            
//            private int             i = 0;
//            
//            @Override
//            public boolean hasNext() {
//                return storeIt.hasNext();
//            }
//            
//            @Override
//            public T next() {
//                if (!hasNext()) {
//                    throw new NoSuchElementException( "Index: " + i );
//                }
//                return cache.computeIfAbsent( i++, _i -> {
//                    CompositeState state = (CompositeState)storeIt.next();
//                    InstanceBuilder builder = new InstanceBuilder( entityContext );
//                    T result = (T)builder.newComposite( state, state.compositeInstanceType( info().getType() ) );
//                    return result;
//                });
//            }
//            
//            @Override
//            public void remove() {
//                storeIt.remove();
//                cache.remove( i );
//            }
//        };
    }


    @Override
    public boolean remove( Object o ) {
        throw new UnsupportedOperationException( "Not yet implemented." );
        
//        for (Iterator<T> it=iterator(); it.hasNext(); ) {
//            EntityRepositoryImpl repo = (EntityRepositoryImpl)entityContext.getRepository();
//            repo.contextOfEntity( o );
//            if (o == it.next()) {
//                it.remove();
//                return true;
//            }
//        }
//        return false;
    }


    @Override
    public boolean add( T e ) {
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public boolean addAll( Collection<? extends T> c ) {
        throw new RuntimeException( "not yet implemented." );
    }

}
