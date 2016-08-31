/* 
 * polymap.org
 * Copyright (C) 2012-2016, Falko Bräutigam. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.test.Employee.Rating;

/**
 * Test for complex models: associations, Composite properties, collections
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class ComplexModelTest
        extends TestCase {

    private static final Log log = LogFactory.getLog( ComplexModelTest.class );
    
    protected EntityRepository      repo;

    protected UnitOfWork            uow;
    

    public ComplexModelTest( String name ) {
        super( name );
    }

    protected void setUp() throws Exception {
        log.info( " --------------------------------------- " + getClass().getSimpleName() + " : " + getName() );
    }

    protected void tearDown() throws Exception {
        uow.close();
        repo.close();
    }


    public void testAssociation() {
        Company company = uow.createEntity( Company.class, null );
        Employee employee = uow.createEntity( Employee.class, null );
        company.chief.set( employee );
        
        Employee chief = company.chief.get();
        log.info( "chief: " + chief );
        assertNotNull( chief );
        
        uow.commit();

        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( Company.class, company.id() );
        Employee chief2 = company2.chief.get();
        log.info( "chief2: " + chief2 );
        assertNotNull( chief2 );
    }
    

    public void testManyAssociationAdd() {
        // create entity
        Company company = uow.createEntity( Company.class, null );
        Employee employee = uow.createEntity( Employee.class, null );
        company.employees.add( employee );
        
        // check uncommitted
        assertEquals( 1, company.employees.size() );
        assertEquals( employee, Iterables.getOnlyElement( company.employees ) );
        assertSame( employee, Iterables.getOnlyElement( company.employees ) );
        
        uow.commit();

        // fetch
        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( Company.class, company.id() );
        log.info( "Company: " + company2 );

        // check committed
        assertEquals( 1, company2.employees.size() );
        assertNotSame( employee, Iterables.getOnlyElement( company2.employees ) );
        assertEquals( employee.id(), company.employees.stream().findFirst().get().id() );
    }

    
    public void testManyAssociationModify() {
        // create entity
        Company company = uow.createEntity( Company.class, null );
        Employee employee = uow.createEntity( Employee.class, null );
        company.employees.add( employee );
        
        uow.commit();

        // add another
        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( company );
        Employee employee2 = uow2.createEntity( Employee.class, null );
        company2.employees.add( employee2 );

        // check uncommitted
        assertEquals( 2, company2.employees.size() );
        
        uow2.commit();

        // check committed
        UnitOfWork uow3 = repo.newUnitOfWork();
        Company company3 = uow3.entity( company );
        assertEquals( 2, company3.employees.size() );
    }

    
    public void testManyAssociationRemove() {
        // create entity
        Company company = uow.createEntity( Company.class, null );
        Employee employee = uow.createEntity( Employee.class, null );
        company.employees.add( employee );        
        uow.commit();

        // remove
        company.employees.remove( employee );
        
        // check uncommitted
        assertEquals( 0, company.employees.size() );
        
        uow.commit();
        
        // check committed
        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( company );
        assertEquals( 0, company2.employees.size() );
    }

    
//    public void testBidiAssociation() {
//        // create entity
//        Company company = uow.createEntity( Company.class, null );
//        Employee employee = uow.createEntity( Employee.class, null );
//
//        // set
//        employee.company.set( company );
//        assertSame( company, employee.company.get() );
//
//        // check back reference
//        assertEquals( 1, company.employees.size() );
//        assertEquals( employee, Iterables.getOnlyElement( company.employees ) );
//        assertSame( employee, Iterables.getOnlyElement( company.employees ) );
//
//        // remove
//        employee.company.set( null );
//
//        // check back reference
//        assertEquals( 0, company.employees.size() );
//    }
//
//    
//    public void testBidiManyAssociation() {
//        // create entity
//        Male man = uow.createEntity( Male.class, null );
//        Male man2 = uow.createEntity( Male.class, null );
//        Female woman = uow.createEntity( Female.class, null );
//
//        // set
//        man.friends.add( woman );
//
//        // check back references
//        assertEquals( 1, man.friends.size() );
//        assertEquals( 1, woman.friends.size() );
//        assertSame( woman, Iterables.getOnlyElement( man.friends ) );
//        assertSame( man, Iterables.getOnlyElement( woman.friends ) );
//        
//        // remove
//        woman.friends.remove( man );
//        assertEquals( 0, man.friends.size() );
//        assertEquals( 0, woman.friends.size() );
//        
//        // man2
//        man.friends.add( woman );
//        woman.friends.add( man2 );
//        assertEquals( 1, man.friends.size() );
//        assertEquals( 1, man2.friends.size() );
//        assertEquals( 2, woman.friends.size() );
//    }
//
//    
//    public void testComputedBidiAssociation() {
//        // create entity
//        Company company = uow.createEntity( Company.class, null );
//        Employee employee = uow.createEntity( Employee.class, null );
//
//        // set
//        company.employees.add( employee );
//
//        // check back reference; uncommitted
//        assertSame( company, employee.computedCompany.get() );
//        uow.commit();
//
//        // check back reference; committed
//        assertSame( company, employee.computedCompany.get() );
//    }

    
    public void testCompositeProperty() {
        Company company = uow.createEntity( Company.class, null );
        
        Address address = company.address.get();
        assertNull( address );
        
        address = company.address.createValue( new ValueInitializer<Address>() {
            public Address initialize( Address value ) throws Exception {
                value.street.set( "Jump" );
                value.nr.set( 1 );
                return value;
            }
        } );
        assertNotNull( address );
        log.info( "Address: " + address );
        assertEquals( "Jump", company.address.get().street.get() );
        assertEquals( 1, (int)company.address.get().nr.get() );

        uow.commit();

        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( Company.class, company.id() );
        Address address2 = company2.address.get();
        assertNotNull( address2 );
        log.info( "Address: " + address2 );
        assertEquals( "Jump", company2.address.get().street.get() );
        assertEquals( 1, (int)company2.address.get().nr.get() );
    }

    
    public void testPrimitiveCollection() {
        Company company = uow.createEntity( Company.class, null );

        company.docs.add( "doc1" );
        log.info( "Company: " + company );
        assertEquals( 1, company.docs.size() );
        assertEquals( "doc1", Iterables.get( company.docs, 0 ) );
        
        // check equal()
        assertTrue( company.docs.equals( company.docs ) );
        ArrayList<String> copy = Lists.newArrayList( company.docs );
        assertTrue( company.docs.equals( copy ) );
        //assertTrue( CollectionUtils.isEqualCollection( company.docs, copy ) );

        uow.commit();

        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( Company.class, company.id() );
        Collection<String> docs = company2.docs;
        assertEquals( 1, docs.size() );
        assertEquals( "doc1", Iterables.get( docs, 0 ) );

        // equal()
        assertTrue( company.docs.equals( company2.docs ) );
//        assertEquals( copy, company2.docs );
//        assertEquals( company2.docs, copy );
    }
    
    
    public void testCompositeCollection() {
        Company company = uow.createEntity( Company.class, null );

        assertEquals( 0, company.moreAddresses.size() );

        Address address = company.moreAddresses.createElement( new ValueInitializer<Address>() {
            public Address initialize( Address value ) throws Exception {
                value.street.set( "Jump" );
                value.nr.set( 1 );
                return value;
            }
        } );
        log.info( "Company: " + company );
        log.info( "Address: " + address );
        assertEquals( 1, company.moreAddresses.size() );
        Address firstAddress = Iterables.get( company.moreAddresses, 0 );
        assertEquals( "Jump", firstAddress.street.get() );
        assertEquals( 1, (int)firstAddress.nr.get() );

        uow.commit();

        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( Company.class, company.id() );
        assertEquals( 1, company2.moreAddresses.size() );
        Address firstAddress2 = Iterables.get( company2.moreAddresses, 0 );
        assertEquals( "Jump", firstAddress2.street.get() );
        assertEquals( 1, (int)firstAddress2.nr.get() );
    }


    public void testCompositeCollectionTypedCreate() {
        Company company = uow.createEntity( Company.class, null );

        assertEquals( 0, company.fellows.size() );

        Employee employee = company.fellows.createElement( new ValueInitializer<Employee>() {
            public Employee initialize( Employee proto ) throws Exception {
                proto.jap.set( 100 );
                return proto;
            }
        } );
        log.info( "Company: " + company );
        log.info( "fellow: " + employee );
        assertEquals( 1, company.fellows.size() );

        uow.commit();

        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( Company.class, company.id() );
        Employee employee2 = (Employee)Iterables.get( company2.fellows, 0 );
        assertEquals( new Integer( 100 ), employee2.jap.get() );
    }

    
    public void testCompositePropertyTypedCreate() {
        Company company = uow.createEntity( Company.class, null );

        Employee employee = company.bigFellow.createValue( new ValueInitializer<Employee>() {
            public Employee initialize( Employee proto ) throws Exception {
                proto.jap.set( 100 );
                return proto;
            }
        } );
        log.info( "Company: " + company );
        log.info( "Fellow: " + employee );

        uow.commit();

        UnitOfWork uow2 = repo.newUnitOfWork();
        Company company2 = uow2.entity( Company.class, company.id() );
        Employee employee2 = (Employee)company2.bigFellow.get();
        assertEquals( new Integer( 100 ), employee2.jap.get() );
    }

    
//    public void testCompositeCollectionElementEqual() {
//        Company company = uow.createEntity( Company.class, null );
//        Address address = company.moreAddresses.createElement( new ValueInitializer<Address>() {
//            public Address initialize( Address value ) throws Exception {
//                return value;
//            }
//        } );
//        Address firstAddress = Iterables.get( company.moreAddresses, 0 );
//        assertEquals( address, firstAddress );
//
//        uow.commit();
//
//        UnitOfWork uow2 = repo.newUnitOfWork();
//        Company company2 = uow2.entity( Company.class, company.id() );
//        Address firstAddress2 = Iterables.get( company2.moreAddresses, 0 );
//        assertEquals( address, firstAddress2 );
//
//        Address firstAddress3 = Iterables.get( company2.moreAddresses, 0 );
//        assertEquals( firstAddress2, firstAddress3 );
//    }

    
    public void testCompositeCollectionClear() {
        Company company = uow.createEntity( Company.class, null );
        company.moreAddresses.createElement( (Address proto) -> {
            proto.street.set( "To be removed" );
            proto.nr.set( 1 );
            return proto;
        });
        company.moreAddresses.createElement( (Address proto) -> {
            proto.street.set( "To be removed" );
            proto.nr.set( 2 );
            return proto;
        });
        
        company.moreAddresses.clear();
        assertEquals( 0, company.moreAddresses.size() );
        assertEquals( 0, Iterators.size( company.moreAddresses.iterator() ) );
    }
    
    
    public void _testCompositeCollectionElementRemove() {
        Company company = uow.createEntity( Company.class, null );
        Address address = company.moreAddresses.createElement( new ValueInitializer<Address>() {
            public Address initialize( Address value ) throws Exception {
                value.street.set( "To be removed" );
                value.nr.set( 11 );
                return value;
            }
        } );
        // check uncommited
        log.info( "Company: " + company );
        assertTrue( company.moreAddresses.remove( address ) );
        log.info( "Company (removed): " + company );
        assertEquals( 0, company.moreAddresses.size() );
        assertEquals( 0, Iterables.size( company.moreAddresses ) );

        // check commited
        address = company.moreAddresses.createElement( new ValueInitializer<Address>() {
            public Address initialize( Address value ) throws Exception {
                value.street.set( "To be removed (commited)" );
                value.nr.set( 12 );
                return value;
            }
        } );
        company.moreAddresses.createElement( new ValueInitializer<Address>() {
            public Address initialize( Address value ) throws Exception {
                value.street.set( "another" );
                value.nr.set( 13 );
                return value;
            }
        } );
        company.moreAddresses.createElement( new ValueInitializer<Address>() {
            public Address initialize( Address value ) throws Exception {
                value.street.set( "third" );
                value.nr.set( 14 );
                return value;
            }
        } );

        uow.commit();
        
        log.info( "Company: " + company );
        company.moreAddresses.remove( address );
        log.info( "Company (removed, commited): " + company );
        assertEquals( 2, company.moreAddresses.size() );
        assertEquals( 2, Iterables.size( company.moreAddresses ) );
        
        company.moreAddresses.forEach( a -> {
            assertTrue( a.nr.get() == 13 || a.nr.get() == 14 );
            assertTrue( !a.street.get().startsWith( "To" ) );
        });
    }
    
    
    public void testRollbackCreated() throws Exception {
        Employee employee2 = uow.createEntity( Employee.class, null );
        uow.prepare();
        uow.rollback();

        assertTrue( employee2.id() == employee2.id() );
        try {
            employee2.name.get();
            assertTrue( "Entity should be detached.", false );
        }
        catch (ModelRuntimeException e) {
            // ok
        }
    }

    
    public void testRollbackRemoved() throws Exception {
        Employee employee = uow.createEntity( Employee.class, null, (Employee proto) -> {
            proto.name.set( "employee" );
            return proto;
        });
        uow.commit();
        
        uow.removeEntity( employee );
        uow.prepare();
        uow.rollback();

        assertEquals( "employee", employee.name.get() );
        
        // check status
        Employee e2 = uow.entity( employee );
        assertEquals( "employee", e2.name.get() );
    }

    
    public void testRollbackModified() throws Exception {
        Employee employee = uow.createEntity( Employee.class, null );
        Company company = uow.createEntity( Company.class, null );
        uow.commit();
        
        // modify
        employee.name.set( "modified" );
        employee.birthday.set( new Date() );
        employee.company.set( company );
        employee.rating.set( Rating.topNotch );
        
        company.chief.set( employee );
        company.employees.add( employee );
        company.address.createValue( (Address proto) -> {
            proto.street.set( "Industriestr" );
            return proto;
        });
        company.moreAddresses.createElement( (Address proto) -> {
            proto.street.set( "Industriestr" );
            return proto;
        });
        
        uow.rollback();

        // check
        assertNull( employee.name.get() );
        assertNull( employee.rating.get() );
        assertNull( employee.birthday.get() );
        assertNull( employee.company.get() );

        assertNull( company.chief.get() );
        assertNull( company.address.get() );
        assertTrue( company.moreAddresses.isEmpty() );
        assertTrue( company.employees.isEmpty() );
    }


    public void testRollbackModified2() throws Exception {
        Employee employee = uow.createEntity( Employee.class, null, (Employee proto) -> {
            proto.name.set( "modified" );
            proto.birthday.set( new Date() );
            proto.rating.set( Rating.topNotch );
            proto.jap.set( 1 );
            return proto;
        });
        Company company = uow.createEntity( Company.class, null, (Company proto) -> {
            proto.chief.set( employee );
            proto.employees.add( employee );
            proto.address.createValue( (Address a) -> {
                a.street.set( "Industriestr" );
                return a;
            });
            proto.moreAddresses.createElement( (Address a) -> {
                a.street.set( "Industriestr" );
                return a;
            });
            return proto;
        });
        employee.company.set( company );
        uow.commit();

        // modify
        employee.name.set( null );
        employee.birthday.set( null );
        employee.company.set( null );
        employee.rating.set( null );
//        employee.jap.set( null );
        
        company.chief.set( null );
        company.employees.clear();
//        company.address.set( null );
//        company.moreAddresses.clear();
        
        uow.prepare();
        uow.rollback();

        // check
        assertEquals( "modified", employee.name.get() );
        assertEquals( Rating.topNotch, employee.rating.get() );
        assertNotNull( employee.birthday.get() );
        assertNotNull( employee.company.get() );

        assertNotNull( company.chief.get() );
//        assertNull( company.address.get() );
//        assertEquals( 1, company.moreAddresses.size() );
        assertEquals( 1, company.employees.size() );
    }

}
