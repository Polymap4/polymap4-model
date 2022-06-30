/* 
 * polymap.org
 * Copyright (C) 2012-2015, Falko Bräutigam. All rights reserved.
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

import areca.common.Promise;
import areca.common.base.Consumer;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface Association<T extends Entity>
        extends PropertyBase<T> {

    public Promise<T> fetch();

    /** Check/create the associated Entity in an atomar operation. */
    public <E extends Exception> Promise<T> ensure( Consumer<T,E> initializer );

    public void set( T value );

    
    /**
     * If a value is present then (asynchronously) invoke the specified consumer with
     * the value, otherwise do nothing.
     *
     * @param consumer block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code consumer} is null
     */
    public default <E extends Exception> Promise<T> ifPresent( Consumer<? super T,E> consumer ) {
        return fetch().onSuccess( value -> {
            if (value != null) {
                consumer.accept( value );
            }
        });
    }

}
