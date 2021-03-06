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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Iterators;

import org.polymap.model2.Entity;
import org.polymap.model2.query.ResultSet;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class CachingResultSet<T extends Entity>
        implements ResultSet<T> {

    private static Log log = LogFactory.getLog( CachingResultSet.class );

    /** null after one full run */
    protected Iterator<T>   delegate;
    
    protected List<Object>  cachedIds = new ArrayList( 1024 );
    
    /** The cached cachedSize; not synchronized */
    protected int           cachedSize = -1;

    
    public CachingResultSet( Iterator<T> delegate ) {
        this.delegate = delegate;
    }


    /**
     * 
     *
     * @param id
     * @return
     */
    protected abstract T entity( Object id );


    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = -1;
            
            @Override
            public boolean hasNext() {
                if (index+1 < cachedIds.size() || (delegate != null && delegate.hasNext())) {
                    return true;
                }
                else {
                    delegate = null;
                    return false;
                }
            }
            
            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException( "index = " + index );
                }
                if (++index < cachedIds.size()) {
                    return entity( cachedIds.get( index ) );
                }
                else {
                    assert index == cachedIds.size() : "index == cachedIds.size(): " +  index + ", " + cachedIds.size();
                    T result = delegate.next();
                    cachedIds.add( result.id() );
                    return result;
                }
            }
        };
    }

    
    @Override
    public int size() {
        if (cachedSize == -1) {
            cachedSize = delegate == null ? cachedIds.size() : Iterators.size( iterator() );
        }
        return cachedSize;
    }
    
    
    @Override
    public Stream<T> stream() {
        // XXX use cachedIds.size() if available
        return StreamSupport.stream( spliterator(), false );
    }

    
    @Override
    public void close() {
        delegate = null;
        cachedIds = null;
    }

}
