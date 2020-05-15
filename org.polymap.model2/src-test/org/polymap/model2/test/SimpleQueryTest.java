/* 
 * polymap.org
 * Copyright (C) 2012-2014, Falko Bräutigam. All rights reserved.
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

import static org.polymap.model2.query.Expressions.and;
import static org.polymap.model2.query.Expressions.eq;
import static org.polymap.model2.query.Expressions.matches;
import static org.polymap.model2.query.Expressions.not;
import static org.polymap.model2.query.Expressions.notEq;
import static org.polymap.model2.query.Expressions.or;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.test.Employee.Rating;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import junit.framework.TestCase;

/**
 * Test for simple model queries.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class SimpleQueryTest
        extends TestCase {

    private static final Log log = LogFactory.getLog( SimpleQueryTest.class );

    protected EntityRepository      repo;

    protected UnitOfWork            uow;

    private Employee                ulli;

    private Employee                az;
    

    public SimpleQueryTest( String name ) {
        super( name );
    }

    protected void setUp() throws Exception {
        log.info( " --------------------------------------- " + getClass().getSimpleName() + " : " + getName() );
    }

    protected void tearDown() throws Exception {
        uow.close();
        repo.close();
    }


    public void testCommitted() throws Exception {
        createEntities();
        uow.commit();
        doQueries();
        doMultipleRuns();
        doPartialRun();
    }

    
    /**
     * Query uncommitted changes which used the {@link BooleanExpression} implementations.
     */
    public void testUncommitted() throws Exception {
        createEntities();
        doQueries();
        doMultipleRuns();
        doPartialRun();
    }

    
    public void testEqual() throws Exception {
        createEntities();

        ResultSet<Employee> rs = uow.query( Employee.class ).execute();
        assertEquals( rs, rs );
        
//        ArrayList<Employee> copy = Lists.newArrayList( rs );
//        assertEquals( copy, rs );
//        assertEquals( rs, copy );
    }

    
    protected void createEntities() {
        ulli = uow.createEntity( Employee.class, null, new ValueInitializer<Employee>() {
            public Employee initialize( Employee proto ) throws Exception {
                proto.firstname.set( "Ulli" );
                proto.name.set( "Philipp" );
                proto.rating.set( Rating.good );
                return proto;
            }
        });
        az = uow.createEntity( Employee.class, null, new ValueInitializer<Employee>() {
            public Employee initialize( Employee proto ) throws Exception {
                proto.firstname.set( "AZ (Andreas)" );
                proto.name.set( "Zimmermann" );
                return proto;
            }
        });        
    }
    
    
    protected void doMultipleRuns() {
        ResultSet<Employee> rs = uow.query( Employee.class ).execute();

        // first run of Iterator
        List<Entity> results = new ArrayList();
        results.addAll( rs.stream().collect( Collectors.toList() ) );
        
        // second run
        List<Entity> results2 = new ArrayList();
        results2.addAll( rs.stream().collect( Collectors.toList() ) );
  
        for (int i=0; i<results.size(); i++) {
            assertSame( results.get( i ), results2.get( i ) );
            assertSame( results.get( i ).state(), results2.get( i ).state() );
        }
        
        // 3rd and 4th run
        assertEquals( 2, Iterables.size( rs ) );
        assertEquals( 2, Iterables.size( rs ) );
    }
    
    
    protected void doPartialRun() {
        ResultSet<Employee> rs = uow.query( Employee.class ).execute();

        // build 2 iterators on same ResultSet
        Employee first = Iterators.get( rs.iterator(), 0 );
        Employee second = Iterators.get( rs.iterator(), 1 );
 
        assertNotSame( first, second );
    }
    
    
    protected void doQueries() {
        // all
        ResultSet<Employee> rs = uow.query( Employee.class ).execute();
        assertEquals( 2, rs.size() );
        assertEquals( 2, Iterables.size( rs ) );

        Employee wanted = Expressions.template( Employee.class, repo );
        
        // String property
        rs = uow.query( Employee.class ).where( eq( wanted.firstname, "Ulli" ) ).execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );

        rs = uow.query( Employee.class ).where( eq( wanted.firstname, "AZ (Andreas)" ) ).execute();
        assertEquals( 1, rs.size() );
        
        // id
        rs = uow.query( Employee.class ).where( Expressions.id( ulli ) ).execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );
        assertEquals( "Ulli", Iterables.getOnlyElement( rs ).firstname.get() );
        
        // matches
        rs = uow.query( Employee.class ).where( matches( wanted.firstname, "Ul*" ) ).execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );

        // matches
        rs = uow.query( Employee.class ).where( matches( wanted.firstname, "Ull?" ) ).execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );

        // Enum property
        rs = uow.query( Employee.class ).where( eq( wanted.rating, Rating.good ) ).execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );

        // and
        rs = uow.query( Employee.class )
                .where( and( eq( wanted.firstname, "Ulli" ), eq( wanted.name, "Philipp" ) ) )
                .execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );

        // or
        rs = uow.query( Employee.class )
                .where( or( eq( wanted.firstname, "Ulli" ), eq( wanted.name, "Zimmermann" ) ) )
                .execute();
        assertEquals( 2, rs.size() );
        assertEquals( 2, Iterables.size( rs ) );

        // and notEq
        rs = uow.query( Employee.class )
                .where( and( eq( wanted.firstname, "Ulli" ), notEq( wanted.name, "Zimmermann" ) ) )
                .execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );
        
        // and not eq
        rs = uow.query( Employee.class )
                .where( and( eq( wanted.firstname, "Ulli" ), not( eq( wanted.name, "Zimmermann" ) ) ) )
                .execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );
    }
    
}
