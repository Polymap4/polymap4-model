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
import static org.polymap.model2.query.Expressions.anyOf;
import static org.polymap.model2.query.Expressions.eq;
import static org.polymap.model2.query.Expressions.id;
import static org.polymap.model2.query.Expressions.is;
import static org.polymap.model2.query.Expressions.matches;
import static org.polymap.model2.query.Expressions.the;

import com.google.common.collect.Iterables;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.test.Employee.Rating;

import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;
import junit.framework.TestCase;

/**
 * Test for simple model queries.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class ComplexQueryTest
        extends TestCase {

    private static final Log log = LogFactory.getLog( ComplexQueryTest.class );

    protected EntityRepository      repo;

    protected UnitOfWork            uow;

    private Employee                ulli, az;
    

    public ComplexQueryTest( String name ) {
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
    }

    
    /**
     * Query uncommitted changes which used the {@link BooleanExpression} implementations.
     */
    public void testUncommitted() throws Exception {
        createEntities();
        doQueries();
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
                proto.firstname.set( "AZ" );
                proto.name.set( "Zimmermann" );
                return proto;
            }
        });        
        uow.createEntity( Company.class, null, new ValueInitializer<Company>() {
            public Company initialize( Company proto ) throws Exception {
                proto.name.set( "ullis" );
                proto.chief.set( ulli );
                proto.employees.add( ulli );
                proto.employees.add( az );
                return proto;
            }
        });        
        uow.createEntity( Company.class, null, new ValueInitializer<Company>() {
            public Company initialize( Company proto ) throws Exception {
                proto.name.set( "Irgendeine" );
                proto.address.<Address>createValue( (Address a) -> {
                        a.street.set( "Südstrasse" );
                        a.nr.set( 6 );
                        return a;
                });
                proto.moreAddresses.createElement( new ValueInitializer<Address>() {
                    public Address initialize( Address a ) throws Exception {
                        a.street.set( "Weststrasse" );
                        a.nr.set( 1 );
                        return a;
                    }
                });
                return proto;
            }
        });        
    }
    
    
    protected void doQueries() {
//        collection();
        composite();
        compositeCollection();
        associationIs();
        associationIs2();
        association();
        manyAssociationContains();
    }
    
    
//    protected void collection() {
//        Company wanted = Expressions.template( Company.class, repo );
//        
//        ResultSet<Company> rs = uow.query( Company.class )
//                .where( Expressions.eq( wanted.docs, "Philipp" ) ) )
//                .execute();
//        assertEquals( 1, rs.size() );
//        assertEquals( 1, Iterables.size( rs ) );
//    }

    
    protected void composite() {
        Company wanted = Expressions.template( Company.class, repo );
        Address address = Expressions.template( Address.class, repo );
        
        ResultSet<Company> rs = uow.query( Company.class )
                .where( the( wanted.address,
                        and( 
                            matches( address.street, "Südstr*" ), 
                            eq( address.nr, 6 ) ) ) )
                .execute();
        
//        ResultSet<Company> rs = uow.query( Company.class )
//                .where( eq( wanted.moreAddresses.any().street, "süd" ) )
//                .execute();

        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );
    }

    
    protected void compositeCollection() {
        Company wanted = Expressions.template( Company.class, repo );
        Address address = Expressions.template( Address.class, repo );
        
        ResultSet<Company> rs = uow.query( Company.class )
                .where( anyOf( wanted.moreAddresses, 
                        and( 
                            eq( address.street, "Weststrasse" ), 
                            eq( address.nr, 1 ) ) ) )
                .execute();
        
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );
    }

    
    protected void associationIs() {
        Company wanted = Expressions.template( Company.class, repo );
        ResultSet<Company> rs = uow.query( Company.class )
                .where( is( wanted.chief, ulli ) )
                .execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );
    }

    
    protected void associationIs2() {
        assertEquals( 1, uow.query( Company.class )
                .where( the( Company.TYPE.chief, id( ulli ) ) )
                .execute().size() );
    }

    
    protected void association() {
        Company wanted = Expressions.template( Company.class, repo );
        Employee employee = Expressions.template( Employee.class, repo );
        
        ResultSet<Company> rs = uow.query( Company.class )
                .where( the( wanted.chief, eq( employee.name, "Philipp" ) ) )
                .execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );
    }

    
    protected void manyAssociationContains() {
        Company wanted = Expressions.template( Company.class, repo );
        Employee employee = Expressions.template( Employee.class, repo );
        
        ResultSet<Company> rs = uow.query( Company.class )
                .where( anyOf( wanted.employees, 
                        and( 
                            eq( employee.name, "Philipp" ),
                            eq( employee.firstname, "Ulli" ) ) ) )
                .execute();
        assertEquals( 1, rs.size() );
        assertEquals( 1, Iterables.size( rs ) );
    }

}
