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

import static org.polymap.model2.query.Expressions.eq;
import static org.polymap.model2.store.tidbstore.IDBStore.nextDbVersion;
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.After;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 * Test for simple models: no associations, no Composite properties
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@Test
public class SimpleModelTest {

    private static final Log LOG = LogFactory.getLog( SimpleModelTest.class );

    public static final ClassInfo<SimpleModelTest> info = SimpleModelTestClassInfo.instance();

    protected EntityRepository   _repo;

    protected UnitOfWork         uow; 
    

    protected Promise<EntityRepository> initRepo( String testName ) {
        return EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Person.info ) )
                .store.set( new IDBStore( "SimpleModelTest-" + testName, nextDbVersion(), true ) )
                .create()
                .onSuccess( newRepo -> {
                    LOG.debug( "Repo created." );    
                    _repo = newRepo;
                    uow = newRepo.newUnitOfWork();
                });
    }

    @After
    protected void tearDown() throws Exception {
        if (_repo != null) {
            uow.close();
            _repo.close();
        }
    }

    
    @Test
    public Promise<?> testEntityInfo() throws Exception {
        return initRepo( "entityInfo" ).map( repo -> {
            CompositeInfo<Person> personInfo = repo.infoOf( Person.info );
            Person person = uow.createEntity( Person.class );
            Assert.isSame( personInfo, person.info() );

            Assert.isEqual( "Person", personInfo.getName() );
            Assert.isEqual( "Person", personInfo.getNameInStore() );
            Assert.notNull( Person.TYPE );
            return null;
        });
    }

    
    @Test
    public Promise<?> testProperties() throws Exception {
        return initRepo( "properties" )
                .then( repo -> {
                    Person person = uow.createEntity( Person.class );
                    LOG.debug( "Person: id=" + person.id() );
                    Assert.notNull( person.id() );
                    Assert.isNull( person.name.get() );
                    Assert.isEqual( person.firstname.get(), "Ulli" );

                    person.name.set( "Philipp" );
                    Assert.isEqual( person.name.get(), "Philipp" );
 
                    UnitOfWork uow2 = repo.newUnitOfWork();
                    uow2.entity( Person.class, person.id() ).onSuccess( p -> {
                        LOG.debug( "Uncommited: %s", Assert.isNull( p ) );
                    });

                    // commit
                    return uow.submit().map( submitted -> person );
                })
//                .then( created -> {
//                    return uow.entity( Person.class, created.id() ).onSuccess( p -> {
//                        log.info( "Commited: %s", Assert.notNull( p ) );
//                        Assert.isSame( created, p );
//                        return created;
//                    });
//                })
                .then( created -> {
                    var uow3 = _repo.newUnitOfWork();
                    return uow3.entity( Person.class, created.id() )
                            .onSuccess( p -> {
                                LOG.debug( "Commited re-read: %s", p );
                                LOG.debug( "Person3: " + Assert.notNull( p, "Unable to read commited Person: " + created ) );
                                Assert.isEqual( p.name.get(), "Philipp" );
                                Assert.isEqual( p.firstname.get(), "Ulli" );
                            });

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
                });
    }
    
    
    @Test
    public Promise<?> testQueryIterate() {
        MutableInt count = new MutableInt();
        return initRepo( "queryIterate" )
                // create data
                .then( repo -> {
                    var uow2 = repo.newUnitOfWork();
                    Sequence.ofInts( 0, 9 ).forEach( i -> {
                        uow2.createEntity( Person.class, p -> p.name.set( "name-" + i ) );                        
                    });
                    return uow2.submit();
                })
                // query
                .then( submitted -> {
                    return uow.query( Person.class ).execute();
                })
                // entity
                .onSuccess( person -> {
                    person.ifPresent( p -> {
                        LOG.debug( "RS: " + count + ": " + p.id() + ", name=" + p.name.get() );
                        _repo.newUnitOfWork()
                                .entity( Person.class, p.id() )
                                .onSuccess( loaded -> {
                                    LOG.debug( "loaded: %s", Assert.notNull( loaded ) );
                                });
                        count.increment();
                        LOG.debug( "count = " + count );
                    });
                });                    
                
    }
    

//    @Test
//    @Skip
//    public void testQueryPerformance() {
//        var uow2 = repo.newUnitOfWork();
//        for (int i=0; i<100; i++) {
//            var p = uow2.createEntity( Person.class, null );
//            p.name.set( "name-" + i );
//        }
//        uow2.submit().waitForResult();
//        
//        for (int i=0; i<20; i++) {
//            uow2 = repo.newUnitOfWork();
//            MutableInt count = new MutableInt();
//            uow2.query( Person.class ).execute()
//                    .onSuccess( p -> count.increment() )
//                    .waitForResult();
//            uow2.close();
//        }
//    }
    
    
    @Test
    public Promise<?> testQueryName() {
        return initRepo( "queryName" )
                .then( repo -> {
                    var uow2 = repo.newUnitOfWork();
                    Sequence.ofInts( 0, 9 ).forEach( i -> {
                        uow2.createEntity( Person.class, p -> p.name.set( "name-" + i ) );                        
                    });
                    return uow2.submit();
                })
                .then( submitted -> {
                    return uow.query( Person.class )
                            .where( eq( Person.TYPE.name, "name-0" ) )
                            .executeToList()
                            .onSuccess( results -> {
                                Assert.isEqual( 1, results.size() );
                            });
                });
    }
    
    
    @Test
    @Skip
    public void testDefaults() throws Exception {
        Person person = uow.createEntity( Person.class );
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
    public Promise<?> testConcern() throws Exception {
        return initRepo( "concern" )
                .map( repo -> {
                    Person person = uow.createEntity( Person.class );

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
                    return null;        
                });
    }
    
    
    @Test( expected = ModelRuntimeException.class )
    public Promise<?> testDetached() {
        return initRepo( "detached" ).map( repo -> {
            UnitOfWork uow2 = repo.newUnitOfWork();
            //_testDetached( uow2.newUnitOfWork() );
            _testDetached( uow2 );
            return null;
        });
    }
    

    public void _testDetached( UnitOfWork uow2 ) throws Exception {
        Person person = uow2.createEntity( Person.class );
        person.name.get();
        
        uow2.close();
        person.name.get();
    }
    
}
