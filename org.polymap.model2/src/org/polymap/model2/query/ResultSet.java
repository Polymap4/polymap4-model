/* 
 * polymap.org
 * Copyright (C) 2014-2015, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.query;

import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.polymap.model2.Entity;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface ResultSet<T extends Entity>
        extends Iterable<T>, AutoCloseable {

    public static final ResultSet<?> EMPTY = new ResultSet() {
        @Override
        public Iterator iterator() {
            return Collections.emptyIterator();
        }
        @Override
        public int size() {
            return 0;
        }
        @Override
        public void close() {
        }
    };
    
    // ****************************************************
    
    public int size();

    @Override
    public void close();
    
    public default Stream<T> stream() {
        // XXX use cachedIds.size() if available
        return StreamSupport.stream( spliterator(), false );
    }

}
