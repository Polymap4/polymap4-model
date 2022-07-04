/* 
 * polymap.org
 * Copyright 2012-2022, Falko Bräutigam. All rights reserved.
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
import java.util.List;

import org.polymap.model2.Concerns;
import org.polymap.model2.DefaultValue;
import org.polymap.model2.Description;
import org.polymap.model2.Entity;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
import org.polymap.model2.query.Expressions;

import areca.common.Promise;

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
    
    public static final PersonClassInfo info = PersonClassInfo.instance();
    
    public static Person TYPE; 
    
    @Nullable
    @Queryable
    @Concerns( {InvocationCountConcern.class} )
    public Property<String>         name;

    @Queryable
    @DefaultValue( "Ulli" )
    protected Property<String>      firstname;

    @Nullable
    protected Property<Date>        birthday;
 
//    @Computed( ComputedBidiManyAssociation.class )
//    public ManyAssociation<Company> company;
    
    /**
     * Computed bidi association to {@link Company#employees}.
     */
    public Promise<List<Company>> companies() {
        return context.getUnitOfWork().query( Company.class )
                .where( Expressions.anyOf( Company.TYPE.employees, Expressions.id( id() ) ) )
                .executeCollect();
    }
}
