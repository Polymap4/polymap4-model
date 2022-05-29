/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2;

import java.util.ArrayList;
import java.util.List;

import areca.common.Promise;
import areca.common.base.Opt;

/**
 * A multi-value association to an {@link Entity}.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface ManyAssociation<T extends Entity>
        extends PropertyBase<T> {

    public Promise<Opt<T>> fetch();
    
    public default Promise<List<T>> fetchCollect() {
        return fetch().reduce( new ArrayList<>(), (l,entity) -> entity.ifPresent( present -> l.add( present ) ) );
    }
    
    public boolean add( T elm );

    public int size();

}
