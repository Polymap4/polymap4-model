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
package org.polymap.model2.test2;

import java.util.concurrent.atomic.AtomicInteger;

import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernAdapter;

import areca.common.reflect.RuntimeInfo;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@RuntimeInfo
public class InvocationCountConcern<T>
        extends PropertyConcernAdapter<T>
        implements PropertyConcern<T> {

    public static final InvocationCountConcernClassInfo info = InvocationCountConcernClassInfo.instance();
    
    public static AtomicInteger     getCount = new AtomicInteger();
    
    public static AtomicInteger     setCount = new AtomicInteger();

    @Override
    public T get() {
        getCount.incrementAndGet();
        return _delegate().get();
    }

    @Override
    public void set( T value ) {
        setCount.incrementAndGet();
        _delegate().set( value );
    }

}
