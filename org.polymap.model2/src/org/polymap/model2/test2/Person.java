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
package org.polymap.model2.test2;

import java.util.Date;

import org.polymap.model2.Concerns;
import org.polymap.model2.DefaultValue;
import org.polymap.model2.Description;
import org.polymap.model2.Entity;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@Description( "Beschreibung dieses Datentyps" )
//@Concerns( LogConcern.class )
//@Mixins( {TrackableMixin.class} )
public /*abstract*/ class Person
        extends Entity {
    
    public static final PersonClassInfo info = PersonClassInfo.INFO;
    
    //public static Person TYPE;
    
    @Nullable
    @Concerns( InvocationCountConcern.class )
    public Property<String>         name;

    /** Defaults to "Ulli". Not Nullable. */
    @DefaultValue( "Ulli" )
    protected Property<String>      firstname;

    @Nullable
    protected Property<Date>        birthday;
 
}
