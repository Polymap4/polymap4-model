/* 
 * polymap.org
 * Copyright (C) 2024, Falko Bräutigam. All rights reserved.
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.Timer;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.After;
import areca.common.testrunner.Test;

/**
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@Test
public class PerformanceTest {

    private static final Log LOG = LogFactory.getLog( PerformanceTest.class );

    public static final ClassInfo<PerformanceTest> info = PerformanceTestClassInfo.instance();
    
    protected EntityRepository  _repo;

    protected UnitOfWork        uow; 
    
    protected Priority          priority = Priority.BACKGROUND;


    protected Promise<EntityRepository> initRepo( String testName ) {
        return EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Person.info ) )
                .store.set( RepoSupplier.newStore( "PerformanceTest-" + testName ) )
                .create()
                .onSuccess( newRepo -> {
                    LOG.info( "Repo created." );
                    _repo = newRepo;
                    uow = newRepo.newUnitOfWork().setPriority( priority );
                } );
    }

    
    @After
    protected void tearDown() throws Exception {
        if (_repo != null) {
            uow.close();
            _repo.close();
        }
    }

    @Test
    public Promise<?> uowTest() {
        var count = 1000;
        var t = Timer.start();
        var complete = new AtomicBoolean();
        var result = initRepo( "uowTest" )
                // create
                .then( repo -> {
                    uow.createEntity( Person.class, p -> { } );
                    return uow.submit().onSuccess( __ -> 
                            LOG.warn( "Create: %s", t.elapsedHumanReadable() ) );
                })
                // query
                .map( submitted -> {
                    t.restart();
                    for (int i = 0; i < count; i++) {
                        var uow2 = _repo.newUnitOfWork();
                        uow2.query( Person.class ).singleResult().waitForResult().get();
                        uow2.close();
                    }
                    LOG.warn( "Sessions: %s in %s", count, t.elapsedHumanReadable() );
                    complete.set( true );
                    return null;
                });
        //Assert.that( complete.get() );
        return result;
    }
    
    @Test
    public Promise<?> createTest() {
        var count = 1000;
        var t = Timer.start();
        return initRepo( "createTest" )
                // create
                .then( repo -> {
                    Sequence.ofInts( 1, count ).forEach( i -> uow.createEntity( Person.class, p -> { 
                        p.name.set( "" + i );
                        p.firstname.set( "" + i );
                    }));
                    return uow.submit().onSuccess( __ -> 
                            LOG.warn( "Create (%s): %s", count, t.elapsedHumanReadable() ) );
                })
                // query warm
                .then( submitted -> {
                    t.restart();
                    return uow.query( Person.class ).executeCollect().onSuccess( rs -> {
                        Assert.isEqual( count, rs.size() );
                        LOG.info( "Iterate (warm UoW): %s", t.elapsedHumanReadable() );
                    });
                })
                // query new uow
                .then( submitted -> {
                    t.restart();
                    return _repo.newUnitOfWork().query( Person.class ).executeCollect().onSuccess( rs -> {
                        Assert.isEqual( count, rs.size() );
                        LOG.info( "Iterate (new UoW): %s", t.elapsedHumanReadable() );
                    });
                })
                // query single
                .then( submitted -> {
                    t.restart();
                    return _repo.newUnitOfWork().query( Person.class )
                            .where( Expressions.eq( Person.TYPE.name, "100" ) )
                            .executeCollect().onSuccess( rs -> {
                                Assert.isEqual( 1, rs.size() );
                                LOG.info( "Query (name=100): %s", t.elapsedHumanReadable() );
                            });
                })
                // query single
                .then( submitted -> {
                    t.restart();
                    return _repo.newUnitOfWork().query( Person.class )
                            .where( Expressions.eq( Person.TYPE.name, "100" ) )
                            .executeCollect().onSuccess( rs -> {
                                Assert.isEqual( 1, rs.size() );
                                LOG.info( "Query (name=100): %s", t.elapsedHumanReadable() );
                            });
                })
                // match
                .then( submitted -> {
                    t.restart();
                    var c = String.valueOf( count );
                    String pattern = c.substring( 0, c.length()-1 ) + "*";
                    LOG.debug( pattern );
                    return _repo.newUnitOfWork().query( Person.class )
                            .where( Expressions.matches( Person.TYPE.name, pattern ) )
                            .executeCollect().onSuccess( rs -> {
                                //rs.forEach( p -> LOG.info( "    %s", p.name.get() ) );
                                Assert.isEqual( 2, rs.size() );
                                LOG.info( "Query (match): %s", t.elapsedHumanReadable() );
                            });
                });

    }
}
