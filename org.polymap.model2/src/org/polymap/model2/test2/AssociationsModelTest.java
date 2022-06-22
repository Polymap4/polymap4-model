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

import static org.polymap.model2.store.tidbstore.IDBStore.nextDbVersion;

import java.util.Arrays;

import org.polymap.model2.query.Query.Order;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.tidbstore.IDBStore;

import areca.common.Assert;
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

    protected EntityRepository   repo;

    protected UnitOfWork         uow;
    

    protected Promise<EntityRepository> initRepo( String name ) {
        return EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Person.info, Company.info ) )
                .store.set( new IDBStore( "AssociationsModelTest-" + name, nextDbVersion(), true ) )
                .create()
                .onSuccess( newRepo -> {
                    LOG.debug( "Repo created." );    
                    repo = newRepo;
                    uow = newRepo.newUnitOfWork();
                });
    }

    @After
    protected void tearDown() throws Exception {
        if (repo != null) {
            uow.close();
            repo.close();
        }
    }

    
    protected Promise<Company> createCompany() {
        var _uow = repo.newUnitOfWork();
        var company = _uow.createEntity( Company.class, c -> { 
            Sequence.ofInts( 0, 9 )
                    .map( i -> _uow.createEntity( Person.class, p -> p.name.set( "" + i) ) ) 
                    .forEach( p -> c.employees.add( p ) );
            _uow.createEntity( Person.class, p -> p.name.set( "extra" ) ); 
        });
        return _uow.submit().map( submitted -> company );
    }

    
    @Test
    public Promise<?> manyTest() throws Exception {
        return initRepo( "manyTest" )
                .then( __ -> createCompany() )
                .then( created -> uow.entity( Company.class, created.id() ) )
                .then( company -> company.employees.fetchCollect() )
                .onSuccess( rs -> {
                    LOG.info( "%s", Sequence.of( rs ).map( p -> p.name.get() ) );
                    Assert.isEqual( 10, rs.size() );
                });
    }

    
    @Test
    public Promise<?> manyQueryTest() throws Exception {
        return initRepo( "manyQueryTest" )
                .then( __ -> createCompany() )
                .then( created -> uow.entity( Company.class, created.id() ) )
                .then( company -> company.employees.query()
                        .orderBy( Person.TYPE.name, Order.DESC )
                        .firstResult( 1 )
                        .maxResults( 5 )
                        .executeCollect() )
                .onSuccess( rs -> {
                    LOG.info( "%s", Sequence.of( rs ).map( p -> p.name.get() ) );
                    Assert.isEqual( 5, rs.size() );
                    Assert.isEqual( "8", rs.get( 0 ).name.get() );
                });
    }

    
    @Test
    public Promise<?> oneTest() throws Exception {
        return initRepo( "oneTest" )
                .then( __ -> {
                    var first = uow.createEntity( Person.class, p -> p.name.set( "first" ) );
                    var company = uow.createEntity( Company.class, c -> c.chief.set( first ) );
                    return uow.submit().map( submitted -> company );
                })
                .then( created -> {
                    var uow3 = repo.newUnitOfWork();
                    return uow3.entity( Company.class, created.id() );
                })
                .then( fetched -> {
                    return fetched.chief.fetch().onSuccess( p -> {
                        LOG.debug( "Chief: %s", p ); 
                    });
                });
    }
        
}
