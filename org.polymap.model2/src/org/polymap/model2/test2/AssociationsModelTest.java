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

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.After;
import areca.common.testrunner.Test;

/**
 * Test associations
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@Test
public class AssociationsModelTest {

    private static final Log LOG = LogFactory.getLog( AssociationsModelTest.class );

    public static final ClassInfo<AssociationsModelTest> info = AssociationsModelTestClassInfo.instance();

    public static int nextDbVersion() {
        int dbVersion = (int)System.currentTimeMillis();
        dbVersion = dbVersion << 1 >> 1;
        LOG.info( "Version: %s", dbVersion );
        return dbVersion;
    }

    // instance *******************************************
    
    private static int          dbCount = 0;
    
    protected EntityRepository   _repo;

    protected UnitOfWork         uow;
    

    protected Promise<EntityRepository> initRepo() {
        return EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Person.info, Company.info ) )
                .store.set( new IDBStore( "AssociationsModelTest-" + dbCount++, nextDbVersion(), true ) )
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
    public Promise<?> manyTest() throws Exception {
        return initRepo()
                .then( repo -> {
                    //var first = uow.createEntity( Person.class, p -> p.name.set( "first" ) );
                    var company = uow.createEntity( Company.class, c -> { 
                        Sequence.ofInts( 1, 10 )
                                .map( i -> uow.createEntity( Person.class, p -> p.name.set( "" + i) ) ) 
                                .forEach( p -> c.employees.add( p ) );
                    });
                    return uow.submit().map( submitted -> company );
                })
                .then( created -> {
                    var uow3 = _repo.newUnitOfWork();
                    return uow3.entity( Company.class, created.id() );
                })
                .then( fetched -> {
                    return fetched.employees.fetch().onSuccess( p -> {
                        //LOG.debug( "Employees: %s", p ); 
                    });
                });
    }
        

    @Test
    public Promise<?> oneTest() throws Exception {
        return initRepo()
                .then( repo -> {
                    var first = uow.createEntity( Person.class, p -> p.name.set( "first" ) );
                    var company = uow.createEntity( Company.class, c -> c.chief.set( first ) );
                    return uow.submit().map( submitted -> company );
                })
                .then( created -> {
                    var uow3 = _repo.newUnitOfWork();
                    return uow3.entity( Company.class, created.id() );
                })
                .then( fetched -> {
                    return fetched.chief.fetch().onSuccess( p -> {
                        LOG.debug( "Chief: %s", p ); 
                    });
                });
    }
        
}
