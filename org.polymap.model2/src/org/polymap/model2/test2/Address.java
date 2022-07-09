/* 
 * polymap.org
 * Copyright (C) 2022, the @authors. All rights reserved.
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

import org.polymap.model2.Composite;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;

import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko
 */
@RuntimeInfo
public class Address
        extends Composite {

    public static final ClassInfo<Address> info = AddressClassInfo.instance();
    
    public static Address       TYPE;

    @Queryable
    public Property<String>     street;
    
    public Property<Integer>    number;
    
    public Property<String>     city;
}
