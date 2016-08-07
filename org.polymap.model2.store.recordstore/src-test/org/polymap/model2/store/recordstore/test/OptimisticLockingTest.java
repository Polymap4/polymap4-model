/* 
 * polymap.org
 * Copyright 2012, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.store.recordstore.test;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.ConcurrentEntityModificationException;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.runtime.locking.OptimisticLocking;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.model2.test.Employee;
import org.polymap.model2.test.SimpleModelTest;
import org.polymap.recordstore.IRecordStore;
import org.polymap.recordstore.lucene.LuceneRecordStore;

/**
 * The {@link SimpleModelTest} with {@link IRecordStore}/Lucene backend.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class OptimisticLockingTest
        extends TestCase {

    private static final Log log = LogFactory.getLog( OptimisticLockingTest.class );

    protected IRecordStore          store;

    private EntityRepository        repo;

    private UnitOfWork              uow;

    
    public OptimisticLockingTest( String name ) {
        super( name );
    }


    @Override
    protected void setUp() throws Exception {
        store = new LuceneRecordStore();
        repo = EntityRepository.newConfiguration()
                .store.set( new OptimisticLocking( new RecordStoreAdapter( store ) ) )
                .entities.set( new Class[] {Employee.class} )
                .create();
        uow = repo.newUnitOfWork();
    }


    public void testConcurrentModification() throws Exception {
        Employee employee = uow.createEntity( Employee.class, null, (Employee prototype) -> {
                prototype.name.set( "samstag" );
                return prototype;
        });
        uow.commit();
        
        uow = repo.newUnitOfWork();
        Employee employee1 = uow.entity( Employee.class, employee.id() );

        UnitOfWork uow2 = repo.newUnitOfWork();
        Employee employee2 = uow2.entity( Employee.class, employee.id() );
        
        try {
            employee1.name.set( "changed" );
            uow.commit();

            employee2.name.set( "changed too" );
            uow2.commit();
            assertTrue( "No exception :(", false );
        }
        catch (ConcurrentEntityModificationException e) {
            uow2.rollback();
            // ok
        }
    }


    public void testMultipleCommits() throws Exception {
        Employee employee = uow.createEntity( Employee.class, null, new ValueInitializer<Employee>() {
            public Employee initialize( Employee prototype ) throws Exception {
                prototype.name.set( "donnerstag" );
                return prototype;
            }
        });
        uow.commit();
        
        // multiple uows
        uow = repo.newUnitOfWork();
        uow.entity( Employee.class, employee.id() ).name.set( "modified" );
        uow.commit();

        uow = repo.newUnitOfWork();
        uow.entity( Employee.class, employee.id() ).name.set( "modified2" );
        uow.commit();

        uow = repo.newUnitOfWork();
        assertEquals( "modified2", uow.entity( Employee.class, employee.id() ).name.get() );

        // single uow
        uow = repo.newUnitOfWork();
        uow.entity( Employee.class, employee.id() ).name.set( "modified" );
        uow.commit();
        uow.entity( Employee.class, employee.id() ).name.set( "modified2" );
        uow.commit();

        // single uow, one entity
        uow = repo.newUnitOfWork();
        Employee entity = uow.entity( Employee.class, employee.id() );
        entity.name.set( "modified" );
        uow.commit();
        entity.name.set( "modified2" );
        uow.commit();

        uow = repo.newUnitOfWork();
        assertEquals( "modified2", uow.entity( Employee.class, employee.id() ).name.get() );
    }


    public void testNestedUow() throws Exception {
        Employee employee = uow.createEntity( Employee.class, null, new ValueInitializer<Employee>() {
            public Employee initialize( Employee prototype ) throws Exception {
                prototype.name.set( "donnerstag" );
                return prototype;
            }
        });
        uow.commit();
        
        UnitOfWork uow2 = uow.newUnitOfWork();
        uow2.entity( Employee.class, employee.id() ).name.set( "modified" );
        uow2.commit();
        uow.commit();

        uow2 = uow.newUnitOfWork();
        assertEquals( "modified", uow2.entity( Employee.class, employee.id() ).name.get() );
        uow2.entity( Employee.class, employee.id() ).name.set( "modified2" );
        uow2.commit();
        uow.commit();

        assertEquals( "modified2", uow.entity( Employee.class, employee.id() ).name.get() );

        UnitOfWork uow3 = repo.newUnitOfWork();
        assertEquals( "modified2", uow3.entity( Employee.class, employee.id() ).name.get() );
    }


    /**
     * Check if queried/preloaded entities are properly tracked. If not, then second
     * uow fails because entity was not tracked and OptimisticLocking misleadingly
     * claims concurrent modification.
     */
    public void testPreloaded() throws Exception {
        Employee employee = uow.createEntity( Employee.class, null, new ValueInitializer<Employee>() {
            public Employee initialize( Employee prototype ) throws Exception {
                prototype.name.set( "donnerstag" );
                return prototype;
            }
        });
        uow.commit();
        
        UnitOfWork uow2 = repo.newUnitOfWork();
        uow2.query( Employee.class )
                .where( Expressions.id( employee ) )
                .execute()
                .stream().findAny().get().name.set( "modified" );
        uow2.commit();
        uow.commit();

        uow2 = repo.newUnitOfWork();
        Employee employee2 = uow2.query( Employee.class )
                .where( Expressions.id( employee ) )
                .execute()
                .stream().findAny().get();
        assertEquals( "modified", uow2.entity( Employee.class, employee.id() ).name.get() );
        employee2.name.set( "modified2" );
        uow2.commit();

        UnitOfWork uow3 = repo.newUnitOfWork();
        assertEquals( "modified2", uow3.entity( Employee.class, employee.id() ).name.get() );
    }

}
