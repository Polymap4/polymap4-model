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

import com.sun.istack.internal.Nullable;

import org.polymap.model2.Defaults;
import org.polymap.model2.Mixins;
import org.polymap.model2.NameInStore;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.store.feature.SRS;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@NameInStore("Employee")
@Mixins( {TrackableMixin.class} )
@SRS( "EPSG:31468" )
public class Employee
        extends Person {
    
    public static CompositeInfo     INFO;

    public enum Rating {
        good, topNotch;
    }
    
    @Defaults
    public Property<Integer>        jap;
    
    @Defaults
    public Property<String>         defaultString;
    
    @Defaults
    public Property<Date>           defaultDate;

    public Property<String>         nonNullable;

    @Nullable
    public Property<Rating>         rating;
    
    /**
     * Computed property: back reference of {@link Company#employees()}.
     */
    public Company company() {
        throw new RuntimeException();
    }
    
}
