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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Iterables;

import org.polymap.model2.engine.UnitOfWorkNested;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * Test of {@link UnitOfWorkNested}.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class NestedUowTest
        extends TestCase {

    private static final Log log = LogFactory.getLog( NestedUowTest.class );

    // instance *******************************************
    
    protected EntityRepository      repo;

    protected UnitOfWork            uow;
    

    public NestedUowTest( String name ) {
        super( name );
    }

    protected void setUp() throws Exception {
        log.info( " --------------------------------------- " + getClass().getSimpleName() + " : " + getName() );
    }

    protected void tearDown() throws Exception {
        uow.close();
        repo.close();
    }

    
    /**
     * 
     */
    public void testModification() throws Exception {
        Company company = uow.createEntity( Company.class, null );
        Employee loadedEmployee = uow.createEntity( Employee.class, null, (Employee proto) -> {
             proto.name.set( "loaded" ); 
             return proto;
        });
        Employee modifiedEmployee = uow.createEntity( Employee.class, null, (Employee proto) -> {
            proto.name.set( "init" );
            proto.company.set( company );
            return proto;
        });
        uow.commit();

        Employee createdEmployee = uow.createEntity( Employee.class, null, (Employee proto) -> {
            proto.name.set( "created" );
            return proto;
        });
        modifiedEmployee.name.set( "modified" );
        
        UnitOfWork nested = uow.newUnitOfWork();
        
        // check nested
        Employee nestedLoaded = nested.entity( Employee.class, loadedEmployee.id() );
        assertEquals( "loaded", nestedLoaded.name.get() );
        assertEquals( EntityStatus.LOADED, nestedLoaded.status() );

        Employee nestedCreated = nested.entity( Employee.class, createdEmployee.id() );
        assertEquals( "created", nestedCreated.name.get() );
        assertEquals( EntityStatus.LOADED, nestedCreated.status() );

        Employee nestedModified = nested.entity( modifiedEmployee );
        assertEquals( modifiedEmployee.id(), nestedModified.id() );
        assertEquals( "modified", nestedModified.name.get() );
        assertEquals( EntityStatus.LOADED, nestedModified.status() );

        // modify nested
        nestedLoaded.name.set( "nestedLoaded" ); assertEquals( "nestedLoaded", nestedLoaded.name.get() );
        nestedCreated.name.set( "nestedCreated" ); assertEquals( "nestedCreated", nestedCreated.name.get() );
        nestedModified.name.set( "nestedModified" ); assertEquals( "nestedModified", nestedModified.name.get() );
        nestedModified.company.set( null );

        Employee innerCreated = nested.createEntity( Employee.class, null, new ValueInitializer<Employee>() {
            public Employee initialize( Employee prototype ) throws Exception {
                prototype.name.set( "innerCreated" );
                return prototype;
            }
        });
        assertEquals( EntityStatus.CREATED, innerCreated.status() );
        
        // check parent
        assertEquals( EntityStatus.LOADED, loadedEmployee.status() );
        assertEquals( EntityStatus.CREATED, createdEmployee.status() );
        assertEquals( EntityStatus.MODIFIED, modifiedEmployee.status() );
        
        assertEquals( "loaded", loadedEmployee.name.get() );
        assertEquals( "created", createdEmployee.name.get() );
        assertEquals( "modified", modifiedEmployee.name.get() );

        assertEquals( "loaded", uow.entity( Employee.class, loadedEmployee.id() ).name.get() );
        assertEquals( "created", uow.entity( Employee.class, createdEmployee.id() ).name.get() );
        assertEquals( "modified", uow.entity( Employee.class, modifiedEmployee.id() ).name.get() );
        assertNull( uow.entity( Employee.class, innerCreated.id() ) );
        
        // commit nested
        nested.commit();

        // check parent again
        assertEquals( EntityStatus.MODIFIED, loadedEmployee.status() );
        assertEquals( EntityStatus.CREATED, createdEmployee.status() );
        assertEquals( EntityStatus.MODIFIED, modifiedEmployee.status() );

        assertEquals( "nestedLoaded", loadedEmployee.name.get() );
        assertEquals( "nestedCreated", createdEmployee.name.get() );
        assertEquals( "nestedModified", modifiedEmployee.name.get() );
        assertFalse( modifiedEmployee.company.isPresent() );

        assertEquals( "nestedLoaded", uow.entity( Employee.class, loadedEmployee.id() ).name.get() );
        assertEquals( "nestedCreated", uow.entity( Employee.class, createdEmployee.id() ).name.get() );
        assertEquals( "nestedModified", uow.entity( Employee.class, modifiedEmployee.id() ).name.get() );
        assertEquals( "innerCreated", uow.entity( Employee.class, innerCreated.id() ).name.get() );
        
        // check isolation
        UnitOfWork uow2 = repo.newUnitOfWork();
        assertEquals( "loaded", uow2.entity( Employee.class, loadedEmployee.id() ).name.get() );
        assertEquals( "init", uow2.entity( Employee.class, modifiedEmployee.id() ).name.get() );
        
        // commit parent
        uow.commit();

        uow2 = repo.newUnitOfWork();
        assertEquals( "nestedLoaded", uow2.entity( Employee.class, loadedEmployee.id() ).name.get() );
        assertEquals( "nestedCreated", uow2.entity( Employee.class, createdEmployee.id() ).name.get() );
        assertEquals( "nestedModified", uow2.entity( Employee.class, modifiedEmployee.id() ).name.get() );
        assertEquals( "innerCreated", uow2.entity( Employee.class, innerCreated.id() ).name.get() );
    }
    

    /**
     * 
     */
    public void testQuery() throws Exception {
        Employee parentEmployee = uow.createEntity( Employee.class, null, (Employee prototype) -> {
                prototype.name.set( "name" ); return prototype;
        });
        
        UnitOfWork nested = uow.newUnitOfWork();
        
        nested.createEntity( Employee.class, null, (Employee prototype) -> {
                prototype.name.set( "name2" ); return prototype;
        });

        // check nested
        ResultSet<Employee> rs = nested.query( Employee.class ).execute();
        assertEquals( 2, rs.size() );
        assertEquals( 2, Iterables.size( rs ) );
        
        // check if entities are adopted
        for (Employee found : rs) {
            assertNotSame( found, parentEmployee );
        }
        
        // check parent
        ResultSet<Employee> rs2 = uow.query( Employee.class ).execute();
        assertEquals( 1, rs2.size() );
        assertEquals( 1, Iterables.size( rs2 ) );        
    }
    
    
    public void testCaching() {
        Employee employee = uow.createEntity( Employee.class, null, (Employee proto) -> {
            proto.name.set( "chief" );
            return proto;
        });
        Company company = uow.createEntity( Company.class, null, (Company proto) -> {
            proto.employees.add( employee );
            proto.chief.set( employee );
            proto.moreAddresses.createElement( a -> {
                a.street.set( "süd" ); return a;                
            });
            proto.address.createValue( a -> {
                a.street.set( "similden" ); return a;
            });
            return proto; 
        });
        uow.commit();
        
        // check nested init state
        UnitOfWork nested = uow.newUnitOfWork();
        Company nestedCompany = nested.entity( company );
        assertEquals( 1, nestedCompany.employees.size() );
        assertEquals( 1, nestedCompany.moreAddresses.size() );
        assertEquals( "süd", nestedCompany.moreAddresses.stream().findAny().get().street.get() );
        assertEquals( "similden", nestedCompany.address.get().street.get() );

        // modify nested
        nestedCompany.employees.add( nested.createEntity( Employee.class, null, (Employee proto) -> {
            return proto;
        }));
        assertEquals( 2, nestedCompany.employees.size() );
        nestedCompany.address.get().street.set( "modified" );
        nestedCompany.moreAddresses.createElement( a -> {
            a.street.set( "modified" ); return a;                
        });
        
        assertEquals( 1, company.employees.size() );
        nested.commit();

        assertEquals( 2, company.employees.size() );
        assertEquals( 2, company.moreAddresses.size() );
        assertEquals( "süd", company.moreAddresses.stream().findAny().get().street.get() );
        assertEquals( "modified", Iterables.get( company.moreAddresses, 1 ).street.get() );
        assertEquals( "modified", company.address.get().street.get() );
    }
    
}
