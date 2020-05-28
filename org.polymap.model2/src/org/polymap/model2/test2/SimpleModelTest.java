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
package org.polymap.model2.test2;

import java.util.Arrays;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.After;
import areca.common.testrunner.Before;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 * Test for simple models: no associations, no Composite properties
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@Test
public class SimpleModelTest {

    private static final Log log = LogFactory.getLog( SimpleModelTest.class );

    public static final ClassInfo<SimpleModelTest> info = SimpleModelTestClassInfo.instance();

    protected static EntityRepository   repo;

    protected UnitOfWork                uow;
    

    static {
        //log.info( "" + InvocationCountConcern.info );
        repo = EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Person.info ) )
                .store.set( new IDBStore( "test2", 3 ) )
                .create();
        log.info( "MAIN: repo created" );    
    }
    
    
    @Before
    protected void setUp() throws Exception {
        uow = repo.newUnitOfWork();
    }

    @After
    protected void tearDown() throws Exception {
        uow.close();
    }

    
    @Test
    public void testEntityInfo() throws Exception {
        CompositeInfo<Person> personInfo = repo.infoOf( Person.info );
        Person person = uow.createEntity( Person.class, null );
        Assert.isSame( personInfo, person.info() );
        
        Assert.isEqual( "Person", personInfo.getName() );
        Assert.isEqual( "Person", personInfo.getNameInStore() );
    }

    
    @Test
    public void testProperties() throws Exception {    
        Person person = uow.createEntity( Person.class, null );
        log.info( "Person: id=" + person.id() );
        Assert.notNull( person.id() );
        Assert.isNull( person.name.get() );
        Assert.isEqual( person.firstname.get(), "Ulli" );
        
        person.name.set( "Philipp" );
        Assert.isEqual( person.name.get(), "Philipp" );

        UnitOfWork uow2 = repo.newUnitOfWork();
        Person p2 = uow2.entity( Person.class, person.id() );
        log.info( "Person2: " + p2 );
        Assert.isNull( p2 );
        
//        // commit
//        uow.commit();

//        uow2 = repo.newUnitOfWork();
//        p2 = uow2.entity( Person.class, person.id() );
//        log.info( "Person2: " + p2 );
//        Assert.notNull( p2 );

//        // re-read
//        log.info( "Employee: id=" + person.id() );
//        Employee employee2 = uow.entityForState( Employee.class, person.state() );
//        assertEquals( "Philipp", employee2.name.get() );
//        assertEquals( "Ulli", employee2.firstname.get() );
//        assertEquals( person._float.get(), 0f );
//
//        Employee employee3 = uow.entity( Employee.class, person.id() );
//        log.info( "Employee: name=" + employee3.name.get() );
//
//        // modify
//        employee2.firstname.set( "Ulrike" );
//        log.info( "Employee: firstname=" + employee2.firstname.get() );
//        assertEquals( "Ulrike", employee2.firstname.get() );
//        employee2.jap.set( 100 );
//        
//        // commit
//        log.info( "### COMMIT ###" );
//        uow.commit();
    }
    
    
    @Test
    public void testQueryIterate() {
        ResultSet<Person> rs = uow.query( Person.class ).execute();
        log.info( "size=" + rs.size() );
        
        int count = 0;
        for (Person person : rs) {
            log.info( "RS: " + ++count + ": " + person.id() + ", name=" + person.name.get() );
        }
        log.info( "count=" + count );
        Assert.isEqual( rs.size(), count );
        Assert.isEqual( rs.size(), Sequence.of( rs ).count() );
    }
    

    @Test
    public void testQueryPerformance() {
        for (int i=0; i<20; i++) {
            UnitOfWork uow2 = repo.newUnitOfWork();
            ResultSet<Person> rs = uow2.query( Person.class ).execute();
            long start = System.currentTimeMillis();
            log.info( "" + Sequence.of( rs ).count() + " " + (System.currentTimeMillis()-start) + "ms" );
            uow.close();
        }
        
//        int count = 0;
//        for (Person person : rs) {
//            count ++;
//        }
    }
    
    
    @Test
    @Skip
    public void testQueryName() {
        ResultSet<Person> rs = uow.query( Person.class )
                .where( Expressions.eq( Expressions.template( Person.class, repo ).name, "Philipp" ) )
                .execute();
        Assert.isEqual( 34, rs.size() );
        Assert.isEqual( 34, Sequence.of( rs ).count() );
    }
    
    
    @Test
    public void testDefaults() throws Exception {
        Person person = uow.createEntity( Person.class, null );
        Assert.isEqual( "Ulli", person.firstname.get() );
    }
    

//    public void testEnum() throws Exception {
//        Employee employee = uow.createEntity( Employee.class, null, (Employee prototype) -> {
//                prototype.rating.set( Rating.good );
//                return prototype;
//            }
//        );
////        Employee employee = uow.createEntity( Employee.class, null, new ValueInitializer<Employee>() {
////            public Employee initialize( Employee proto ) throws Exception {
////                proto.rating.set( Rating.good );
////                return proto;
////            }
////        } );
//        assertEquals( Rating.good, employee.rating.get() );
//        assertSame( Rating.good, employee.rating.get() );
//        uow.commit();
//        
//        UnitOfWork uow2 = repo.newUnitOfWork();
//        Employee employee2 = uow2.entityForState( Employee.class, employee.state() );
//        assertEquals( Rating.good, employee2.rating.get() );
//        assertSame( Rating.good, employee2.rating.get() );
//    }
//    
//
//    public void testNullable() throws Exception {
//        Employee employee = uow.createEntity( Employee.class, null );
//        Exception thrown = null;
//        try { 
//            employee.jap.set( null ); 
//        } 
//        catch (Exception e) { thrown = e; }        
//        assertTrue( thrown instanceof ModelRuntimeException );
//        
//        try { 
//            employee.nonNullable.get(); 
//        } 
//        catch (Exception e) { thrown = e; }        
//        assertTrue( thrown instanceof ModelRuntimeException );
//    }
//    
//    
//    public void testCreateEmployees() throws Exception {
//        // loop count greater than default query size of RecordStore
//        for (int i=0; i<11; i++) {
//            Employee employee = uow.createEntity( Employee.class, null );
//            employee.jap.set( i );
//            employee.as( TrackableMixin.class )
//                    .orElseThrow( () -> new IllegalStateException( "Kein Mixin: Trackable" ) )
//                    .track.set( 100 );
//        }
//        // commit
//        log.info( "### COMMIT ###" );
//        uow.commit();
//        
//        // check
//        UnitOfWork uow2 = repo.newUnitOfWork();
//        ResultSet<Employee> results = uow2.query( Employee.class ).execute();
//        assertEquals( 11, results.size() );
//
//        int previousJap = -1;
//        for (Employee employee : results) {
//            int jap = employee.jap.get();
//            assertTrue( jap >= 0 && jap <= results.size() && previousJap != jap );
//        }
//    }
//
//
//    public void testRemoveEntity() throws Exception {
//        Employee employee = uow.createEntity( Employee.class, null );
//        uow.commit();
//        
//        // uow2
//        UnitOfWork uow2 = repo.newUnitOfWork();
//        Employee employee2 = uow2.entity( employee );
//        uow2.removeEntity( employee2 );
//        assertEquals( employee2.status(), EntityStatus.REMOVED );
//        assertTrue( uow2.entity( employee ) == null );
//        
//        uow2.commit();
//        
//        assertTrue( uow2.entity( employee ) == null );
//    }
//    
//    
//    public void testMixinComputed() throws Exception {
//        Employee employee = uow.createEntity( Employee.class, null );
//        
//        TrackableMixin trackable = employee.as( TrackableMixin.class ).get();
//        assertNotNull( trackable );
//        assertSame( employee.state(), trackable.state() );
//
//        trackable = employee.as( TrackableMixin.class ).get();
//        assertNotNull( trackable );
//        assertSame( employee.state(), trackable.state() );
//        
//        trackable.track.set( 10 );
//        log.info( "Computed property: " + trackable.computed.get() );
//        assertTrue( trackable.computed.get().endsWith( "computed" ) );
//        
//        // commit
//        uow.commit();
//        
//        // check
//        UnitOfWork uow2 = repo.newUnitOfWork();
//        employee = uow2.entityForState( Employee.class, employee.state() );
//        trackable = employee.as( TrackableMixin.class ).get();
//        assertNotNull( trackable );
//        assertEquals( 10, (int)trackable.track.get() );
//    }
    
    
    @Test
    public void testConcern() throws Exception {
        Person person = uow.createEntity( Person.class, null );

        int getCount = InvocationCountConcern.getCount.get();
        int setCount = InvocationCountConcern.setCount.get();
        
        person.name.set( "Mufu" );
        person.name.get();
        Assert.isEqual( getCount+1, InvocationCountConcern.getCount.get() );        
        Assert.isEqual( setCount+1, InvocationCountConcern.setCount.get() );        

        person.firstname.set( "Mufu" );
        person.firstname.get();
        Assert.isEqual( getCount+1, InvocationCountConcern.getCount.get() );        
        Assert.isEqual( setCount+1, InvocationCountConcern.setCount.get() );        

//        // Employee does not have LogConcern
//        person.jap.set( 1 );
//        person.jap.get();
//        Assert.isEqual( getCount+1, InvocationCountConcern.getCount.get() );        
//        Assert.isEqual( setCount+1, InvocationCountConcern.setCount.get() );
//        
//        // computed and concerned
//        person.as( TrackableMixin.class ).get().computed.get();
//        Assert.isEqual( getCount+2, InvocationCountConcern.getCount.get() );        
//        Assert.isEqual( setCount+1, InvocationCountConcern.setCount.get() );        
    }
    
    
    @Test( expected = ModelRuntimeException.class )
    public void testDetached() throws Exception {
        UnitOfWork uow2 = repo.newUnitOfWork();
        //_testDetached( uow2.newUnitOfWork() );
        _testDetached( uow2 );
    }
    

    public void _testDetached( UnitOfWork uow2 ) throws Exception {
        Person person = uow2.createEntity( Person.class, null );
        person.name.get();
        
        uow2.close();
        person.name.get();
    }
    
}
