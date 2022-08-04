/* 
 * polymap.org
 * Copyright (C) 2012-2022, Falko Bräutigam. All rights reserved.
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

import static org.polymap.model2.store.tidbstore.IDBStore.nextDbVersion;

import java.util.Arrays;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.common.Assert;
import areca.common.Platform;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.After;
import areca.common.testrunner.Test;

/**
 * Test for simple models: no associations, no Composite properties
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@Test
public class RuntimeTest {

    private static final Log LOG = LogFactory.getLog( RuntimeTest.class );

    public static final ClassInfo<RuntimeTest> info = RuntimeTestClassInfo.instance();

    protected EntityRepository   _repo;

    protected UnitOfWork         uow; 
    
    protected Priority          priority = Priority.BACKGROUND;


    protected Promise<EntityRepository> initRepo( String testName ) {
        return EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Person.info ) )
                .store.set( new IDBStore( "RuntimeTest-" + testName, nextDbVersion(), true ) )
                .create()
                .onSuccess( newRepo -> {
                    LOG.debug( "Repo created." );    
                    _repo = newRepo;
                    uow = newRepo.newUnitOfWork().setPriority( priority );
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
    public Promise<?> discardTest() {
        return initRepo( "discardTest" )
                .then( repo  -> {
                    var entity = uow.createEntity( Person.class, proto -> proto.name.set( "init" ) );
                    return uow.submit().map( __ -> entity );
                })
                .then( entity -> { 
                    Assert.isEqual( "init", entity.name.get() );
                    entity.name.set( "modified" );
                    Assert.isEqual( "modified", entity.name.get() );
                    return uow.discard().map( __ -> entity );
                })
                .onSuccess( entity -> {
                    Assert.isEqual( "init", entity.name.get() );
                }); 
    }

    
    @Test
    public Promise<?> refreshTest() {
        return initRepo( "refreshTest" )
                .then( repo  -> {
                    var entity = uow.createEntity( Person.class, proto -> proto.name.set( "init" ) );
                    return uow.submit().map( __ -> entity );
                })
                .then( entity -> { 
                    Assert.isEqual( "init", entity.name.get() );
                    
                    var check = Platform.schedule( 200, () -> Assert.isEqual( "init", entity.name.get() ) )
                            .then( __ -> uow.refresh() )
                            .onSuccess( __ -> Assert.isEqual( "modified", entity.name.get() ) );
                    
                    var uow2 = _repo.newUnitOfWork().setPriority( priority );
                    var submit = uow2.entity( entity )
                            .onSuccess( modified -> modified.name.set( "modified" ) )
                            .then( __ -> uow2.submit() );
                    
                    return check.join( submit );
                });
    }
    
    
    private Person removed;
    
    @Test
    public Promise<?> removeTest() {
        return initRepo( "removeTest" )
                .then( repo -> {
                    Sequence.ofInts( 0, 9 ).forEach( i -> uow.createEntity( Person.class, p -> p.name.set( "" + i ) ) );
                    return uow.submit();
                })
                .then( __ -> {
                    return uow.query( Person.class ).executeCollect();
                })
                // remove and check Status
                .then( rs -> {
                    uow.removeEntity( removed = rs.get( 0 ) );
                    Assert.isEqual( EntityStatus.REMOVED, removed.status() );
                    return uow.query( Person.class ).executeCollect();
                })
                // check uncommited changes
                .then( rs -> {
                    Assert.isEqual( 9, rs.size() );
                    return uow.submit().onSuccess( __ -> {
                        Assert.isEqual( EntityStatus.REMOVED, removed.status() );
                    });
                })
                // query commited
                .then( __ -> {
                    return uow.query( Person.class ).executeCollect();
                })
                .onSuccess( rs -> {
                    Assert.isEqual( 9, rs.size() );
                });

    }
}
