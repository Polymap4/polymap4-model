/* 
 * polymap.org
 * Copyright (C) 2012-2016, Falko Bräutigam. All rights reserved.
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

import com.google.common.collect.Iterables;

import org.polymap.model2.Association;
import org.polymap.model2.BidiAssociationConcern;
import org.polymap.model2.BidiAssociationName;
import org.polymap.model2.BidiManyAssociationConcern;
import org.polymap.model2.Computed;
import org.polymap.model2.ComputedBidiAssocation;
import org.polymap.model2.ComputedBidiManyAssocation;
import org.polymap.model2.Concerns;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Nullable;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import junit.framework.TestCase;

/**
 * Test of bidirectional associations: {@link BidiAssociationConcern},
 * {@link BidiManyAssociationConcern}, {@link ComputedBidiAssocation},
 * {@link ComputedBidiManyAssocation}.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class BidiAssociationTest
        extends TestCase {

    private static final Log log = LogFactory.getLog( BidiAssociationTest.class );

    /**
     * 
     */
    public static class Group
            extends Entity {

        /** Fully bidirectional with {@link Member#group}; implicite assoc name. */
        @Nullable
        @Concerns(BidiManyAssociationConcern.class)
        public ManyAssociation<Member>  members;
        
        /** Computed back association of {@link Member#group}; implicite assoc name. */
        @Computed(ComputedBidiManyAssocation.class)
        public ManyAssociation<Member>  computedMembers;
    }

    /**
     * 
     */
    public static class Member
            extends Entity {
        
        /** Fully bidirectional with {@link Group#members}; exlicite assoc name. */
        @Nullable
        @Concerns(BidiAssociationConcern.class)
        @BidiAssociationName("members")
        public Association<Group>       group;
        
        @Nullable
        @Computed(ComputedBidiAssocation.class)
        @BidiAssociationName("members")
        public Association<Group>       computedGroup;
    }

    
    // instance *******************************************
    
    protected EntityRepository      repo;

    protected UnitOfWork            uow;
    

    public BidiAssociationTest( String name ) {
        super( name );
    }

    protected void setUp() throws Exception {
        log.info( " --------------------------------------- " + getClass().getSimpleName() + " : " + getName() );
    }

    protected void tearDown() throws Exception {
        uow.close();
        repo.close();
    }


    public void testBidiAssociation() {
        // create entity
        Group group = uow.createEntity( Group.class, null );
        Member m1 = uow.createEntity( Member.class, null );
        uow.createEntity( Group.class, null );
        uow.createEntity( Member.class, null );

        // set
        m1.group.set( group );
        assertSame( group, m1.group.get() );

        // check back reference
        assertEquals( 1, group.members.size() );
        assertEquals( m1, Iterables.getOnlyElement( group.members ) );
        assertSame( m1, Iterables.getOnlyElement( group.members ) );
        assertEquals( m1, group.computedMembers.iterator().next() );
        
        // check committed
        uow.commit();
        UnitOfWork uow2 = repo.newUnitOfWork();
        assertEquals( 1, uow2.entity( group ).members.size() );
        assertEquals( uow2.entity(m1), Iterables.getOnlyElement( uow2.entity(group).computedMembers ) );
        
        // remove
        m1.group.set( null );

        // check back associations
        assertEquals( 0, group.members.size() );
        assertEquals( 0, group.computedMembers.size() );
    }

    
    public void testBidiManyAssociation() {
        // create entity
        Group group = uow.createEntity( Group.class, null );
        uow.createEntity( Group.class, null );
        Member m1 = uow.createEntity( Member.class, null );
        uow.createEntity( Member.class, null );

        // set
        group.members.add( m1 );
        assertEquals( 1, group.members.size() );

        // check back associations
        assertSame( group, m1.group.get() );
        assertSame( group, m1.computedGroup.get() );
        
        // check committed
        uow.commit();
        UnitOfWork uow2 = repo.newUnitOfWork();
        assertSame( uow2.entity( group ), uow2.entity( m1 ).computedGroup.get() );
        
        // remove
        group.members.remove( m1 );
        assertEquals( 0, group.members.size() );

        // check back associations
        assertNull( m1.group.get() );
        assertNull( m1.computedGroup.get() );

        // check committed
        uow.commit();
        uow2 = repo.newUnitOfWork();
        assertNull( uow2.entity( m1 ).group.get() );
        assertNull( uow2.entity( m1 ).computedGroup.get() );
    }

}
