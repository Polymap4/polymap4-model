/* 
 * polymap.org
 * Copyright 2012, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.test;

import java.util.Date;

import org.polymap.model2.Association;
import org.polymap.model2.Defaults;
import org.polymap.model2.NameInStore;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.CompositeInfo;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@NameInStore("Employee")
public class Employee
        extends Person {
    
    public static CompositeInfo     INFO;

    public enum Rating {
        good, topNotch;
    }
    
    @Defaults
    public Property<Integer>        jap;
    
    public Property<Float>          _float;
    
    @Defaults
    @NameInStore("defaultStr") // shapefile supports 10 chars in name
    public Property<String>         defaultString;
    
    @Defaults
    public Property<Date>           defaultDate;

    @NameInStore("nonNullabl") // shapefile supports 10 chars in name
    public Property<String>         nonNullable;

    @Nullable
    public Property<Rating>         rating;

    @Nullable
    public Association<Company>     company;
    
}
