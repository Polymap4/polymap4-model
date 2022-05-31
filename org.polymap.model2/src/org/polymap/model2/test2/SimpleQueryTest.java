/* 
 * polymap.org
 * Copyright (C) 2022, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.test2;

import static org.polymap.model2.query.Expressions.eq;
import static org.polymap.model2.query.Expressions.matches;

import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.After;
import areca.common.testrunner.Test;

/**
 * 
 *
 * @author git_user_name
 */
@RuntimeInfo
public class SimpleQueryTest {

    private static final Log LOG = LogFactory.getLog( SimpleQueryTest.class );

    public static final ClassInfo<SimpleQueryTest> info = SimpleQueryTestClassInfo.instance();
    
    protected EntityRepository   _repo;

    protected UnitOfWork         _uow; 
    

    protected Promise<UnitOfWork> initRepo( String testName ) {
        return EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Person.info ) )
                .store.set( new IDBStore( "SimpleQueryTest-" + testName, IDBStore.nextDbVersion(), true ) )
                .create()
                .then( newRepo -> {
                    LOG.debug( "Repo created." );    
                    _repo = newRepo;
                    _uow = newRepo.newUnitOfWork();
                    return createEntities();
                })
                .map( __ -> _uow );
    }

    @After
    protected void tearDown() throws Exception {
        if (_repo != null) {
            _uow.close();
            _repo.close();
        }
    }

    protected Promise<?> createEntities() {
        var uow = _repo.newUnitOfWork();
        uow.createEntity( Person.class, proto -> {
            proto.firstname.set( "Ulli" );
            proto.name.set( "Philipp" );
            //proto.rating.set( Rating.good );
        });  
        uow.createEntity( Person.class, proto -> {
            proto.firstname.set( "AZ (Andreas)" );
            proto.name.set( "Zimmermann" );
        });
        return uow.submit();
    }
    
    
    @Test
    protected Promise<?> allTest() {
        return initRepo( "all" )
                .then( uow -> uow.query( Person.class ).executeCollect() )
                .onSuccess( rs -> {
                    Assert.isEqual( 2, rs.size() );
                });
    }
        

    @Test
    protected Promise<?> eqTest() {
        return initRepo( "eq" )
                .then( uow -> uow.query( Person.class )
                        .where( eq( Person.TYPE.firstname, "Ulli" ) )
                        .executeCollect() )
                .onSuccess( rs -> {
                    Assert.isEqual( 1, rs.size() );
                });
    }
        

    @Test
    protected Promise<?> matchesTest() {
        return initRepo( "matches" )
                .then( uow -> uow.query( Person.class )
                        .where( matches( Person.TYPE.firstname, "Ul*" ) )
                        .executeCollect() )
                .onSuccess( rs -> {
                    Assert.isEqual( 1, rs.size() );
                })
                .then( rs -> _uow.query( Person.class )
                        .where( matches( Person.TYPE.firstname, "Ull?" ) )
                        .executeCollect() )
                .onSuccess( rs -> {
                    Assert.isEqual( 1, rs.size() );
                });
    }
    
    
    @Test
    protected Promise<?> readCreated() {
        return initRepo( "readCreated" )
                .onSuccess( uow -> {
                    uow.createEntity( Person.class, proto -> {
                        proto.firstname.set( "neu" );
                        proto.name.set( "Philipp" );
                    });
                })
                .then( __ -> _uow.query( Person.class ).executeCollect() )
                .onSuccess( rs -> 
                        Assert.isEqual( 3, rs.size() ) )
                
                .then( __ -> _uow.query( Person.class )
                        .where( eq( Person.TYPE.firstname, "neu" ) )
                        .executeCollect() )
                .onSuccess( rs -> 
                        Assert.isEqual( 1, rs.size() ) )
                
                .then( __ -> _uow.query( Person.class )
                        .where( eq( Person.TYPE.name, "Philipp" ) )
                        .executeCollect() )
                .onSuccess( rs -> 
                        Assert.isEqual( 2, rs.size() ) );
    }
    

    @Test
    protected Promise<?> readModified() {
        return initRepo( "readModified" )
                .then( __ -> _uow.query( Person.class )
                        .where( eq( Person.TYPE.firstname, "Ulli" ) )
                        .executeCollect() )
                .onSuccess( rs -> rs.get( 0 ).firstname.set( "modified" ) )
                
                .then( __ -> _uow.query( Person.class )
                        .where( eq( Person.TYPE.firstname, "modified" ) )
                        .executeCollect() )
                .onSuccess( rs -> 
                        Assert.isEqual( 1, rs.size() ) )
                
                .then( __ -> _uow.query( Person.class )
                        .where( eq( Person.TYPE.name, "Philipp" ) )
                        .executeCollect() )
                .onSuccess( rs -> 
                        Assert.isEqual( 1, rs.size() ) );
    }

    @Test
    public Promise<?> testQueryIterate() {
        MutableInt count = new MutableInt();
        return initRepo( "queryIterate" )
                // create data
                .then( __ -> {
                    var uow2 = _repo.newUnitOfWork();
                    Sequence.ofInts( 0, 9 ).forEach( i -> {
                        uow2.createEntity( Person.class, p -> p.name.set( "name-" + i ) );
                    });
                    return uow2.submit();
                })
                // query
                .then( submitted -> {
                    var uow = _repo.newUnitOfWork();
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
                    .then( __ -> {
                        var uow2 = _repo.newUnitOfWork();
                        Sequence.ofInts( 0, 9 ).forEach( i -> {
                            uow2.createEntity( Person.class, p -> p.name.set( "name-" + i ) );                        
                        });
                        return uow2.submit();
                    })
                    .then( submitted -> {
                        var uow = _repo.newUnitOfWork();
                        return uow.query( Person.class )
                                .where( eq( Person.TYPE.name, "name-0" ) )
                                .executeCollect()
                                .onSuccess( results -> {
                                    Assert.isEqual( 1, results.size() );
                                });
                    });
        }
    
    
//        Employee wanted = Expressions.template( Employee.class, repo );
//        
//        // id
//        rs = uow.query( Employee.class ).where( Expressions.id( ulli ) ).execute();
//        assertEquals( 1, rs.size() );
//        assertEquals( 1, Iterables.size( rs ) );
//        assertEquals( "Ulli", Iterables.getOnlyElement( rs ).firstname.get() );
//        
//        // Enum property
//        rs = uow.query( Employee.class ).where( eq( wanted.rating, Rating.good ) ).execute();
//        assertEquals( 1, rs.size() );
//        assertEquals( 1, Iterables.size( rs ) );
//
//        // and
//        rs = uow.query( Employee.class )
//                .where( and( eq( wanted.firstname, "Ulli" ), eq( wanted.name, "Philipp" ) ) )
//                .execute();
//        assertEquals( 1, rs.size() );
//        assertEquals( 1, Iterables.size( rs ) );
//
//        // or
//        rs = uow.query( Employee.class )
//                .where( or( eq( wanted.firstname, "Ulli" ), eq( wanted.name, "Zimmermann" ) ) )
//                .execute();
//        assertEquals( 2, rs.size() );
//        assertEquals( 2, Iterables.size( rs ) );
//
//        // and notEq
//        rs = uow.query( Employee.class )
//                .where( and( eq( wanted.firstname, "Ulli" ), notEq( wanted.name, "Zimmermann" ) ) )
//                .execute();
//        assertEquals( 1, rs.size() );
//        assertEquals( 1, Iterables.size( rs ) );
//        
//        // and not eq
//        rs = uow.query( Employee.class )
//                .where( and( eq( wanted.firstname, "Ulli" ), not( eq( wanted.name, "Zimmermann" ) ) ) )
//                .execute();
//        assertEquals( 1, rs.size() );
//        assertEquals( 1, Iterables.size( rs ) );

}
