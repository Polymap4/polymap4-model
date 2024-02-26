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

import static org.polymap.model2.query.Expressions.eq;
import static org.polymap.model2.query.Expressions.the;
import java.util.Arrays;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.After;
import areca.common.testrunner.Skip;
import areca.common.testrunner.Test;

/**
 * Test of complex models
 *
 * @author Falko
 */
@Test
public class ComplexModelTest {

    private static final Log LOG = LogFactory.getLog( ComplexModelTest.class );

    public static final ClassInfo<ComplexModelTest> info = ComplexModelTestClassInfo.instance();

    protected EntityRepository  _repo;

    protected UnitOfWork        uow; 
    
    protected Priority          priority = Priority.BACKGROUND;


    protected Promise<EntityRepository> initRepo( String testName ) {
        return EntityRepository.newConfiguration()
                .entities.set( Arrays.asList( Contact.info ) )
                .store.set( RepoSupplier.newStore( "ComplexModelTest-" + testName ) )
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
    public Promise<?> simpleCompositeValueTest() throws Exception {
        return initRepo( "address" )
                .then( repo -> {
                    var _uow = repo.newUnitOfWork().setPriority( priority );
                    var contact = _uow.createEntity( Contact.class );
                    contact.name.set( "test" );
                    contact.address.createValue( proto -> {
                        proto.street.set( "street1" );
                        proto.number.set( 100 );
                    });
                    Assert.isEqual( "street1", contact.address.get().street.get() );
                    Assert.isEqual( 100, contact.address.get().number.get() );
                    return _uow.submit();
                })
                .then( __ -> {
                    return uow.query( Contact.class ).singleResult();
                })
                .map( contact -> {
                    Assert.isEqual( "test", contact.name.get() );
                    Assert.isEqual( "street1", contact.address.get().street.get() );
                    Assert.isEqual( 100, contact.address.get().number.get() );
                    return null;
                });
    }

    @Test
    public Promise<?> compositeValueTest() throws Exception {
        return initRepo( "address" )
                .then( repo -> {
                    Assert.isEqual( 1, Contact.TYPE.address.info().getMaxOccurs() );
                    Assert.isEqual( "address", Contact.TYPE.address.info().getNameInStore() );
                    Assert.isEqual( "address", Contact.TYPE.address.info().getName() );
                    Assert.isEqual( Address.class, Contact.TYPE.address.info().getType() );
                    
                    var _uow = repo.newUnitOfWork().setPriority( priority );
                    var contact = _uow.createEntity( Contact.class );
                    Assert.isNull( contact.address.get() );

                    contact.address.createValue( proto -> {
                        proto.street.set( "street1" );
                        proto.number.set( 100 );
                    });
                    return _uow.submit();
                })
                .then( __ -> {
                    return uow.query( Contact.class )
                            .where( the( Contact.TYPE.address, eq( Address.TYPE.street, "street1" ) ))
                            .singleResult();
                })
                .map( contact -> {
                    Assert.isEqual( "street1", contact.address.get().street.get() );
                    Assert.isEqual( 100, contact.address.get().number.get() );
                    return null;
                });
    }
    

    @Test
    @Skip
    public Promise<?> collectionTest() throws Exception {
        return initRepo( "emails" )
                .then( repo -> {
                    Assert.isEqual( Integer.MAX_VALUE, Contact.TYPE.emails.info().getMaxOccurs() );
                    Assert.isEqual( "emails", Contact.TYPE.emails.info().getNameInStore() );
                    Assert.isEqual( "emails", Contact.TYPE.emails.info().getName() );
                    Assert.isEqual( String.class, Contact.TYPE.emails.info().getType() );
                    
                    var _uow = repo.newUnitOfWork().setPriority( priority );
                    var contact = _uow.createEntity( Contact.class );
                    Assert.isEqual( 0, contact.emails.size() );

                    contact.emails.add( "first" );
                    contact.emails.add( "second" );
                    return _uow.submit();
                })
                .then( __ -> {
                    return uow.query( Contact.class )
                            .where( Expressions.anyEq( Contact.TYPE.emails, "first" ) )
                            .executeCollect();
                })
                .map( rs -> {
                    var contact = rs.get( 0 );
                    Assert.isEqual( 2, contact.emails.size() );
                    Assert.that( contact.emails.contains( "first" ) );
                    return null;
                });
    }

    
    @Test
    public Promise<?> compositeCollectionTest() throws Exception {
        return initRepo( "others" )
                .then( repo -> {
                    Assert.isEqual( Integer.MAX_VALUE, Contact.TYPE.others.info().getMaxOccurs() );
                    Assert.isEqual( "others", Contact.TYPE.others.info().getNameInStore() );
                    Assert.isEqual( "others", Contact.TYPE.others.info().getName() );
                    Assert.isEqual( false, Contact.TYPE.emails.info().isAssociation() );
                    Assert.isEqual( Address.class, Contact.TYPE.others.info().getType() );
                    
                    var _uow = repo.newUnitOfWork().setPriority( priority );
                    var contact = _uow.createEntity( Contact.class );
                    Assert.isEqual( 0, contact.others.size() );

                    contact.others.createElement( proto -> {
                        proto.street.set( "street1" );
                        proto.number.set( 100 );
                    });
                    contact.others.createElement( proto -> {
                        proto.street.set( "street2" );
                        proto.number.set( 200 );
                    });
                    //contact.name.set( "name" );
                    return _uow.submit();
                })
                .then( __ -> {
                    return uow.query( Contact.class )
                            //.where( Expressions.anyOf( Contact.TYPE.others, eq( Address.TYPE.street, "street1" ) ) )
                            .executeCollect();
                })
                .map( rs -> {
                    var contact = rs.get( 0 );
                    Assert.isEqual( 2, contact.others.size() );
                    Assert.isEqual( Arrays.asList( "street1", "street2" ), 
                            contact.others.seq().map( a -> a.street.get() ).toList() );
                    return null;
                });
    }
    
}
