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
package org.polymap.model2.runtime.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.MapMaker;

import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;

/**
 *
 * @deprecated Just an idea. Don't use yet!
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PessimisticLocking
        extends PropertyConcernBase
        implements PropertyConcern, ManyAssociation {

    private static final Log log = LogFactory.getLog( PessimisticLocking.class );

    private static ConcurrentMap<EntityKey,EntityLock>  locks = new MapMaker().concurrencyLevel( 4 ).initialCapacity( 256 ).makeMap();
    
    private enum AccessType {
        READ, WRITE
    }
    
    @Override
    public Object get() {
        checkWait( AccessType.READ );
        Object result = ((Property)delegate).get();
        return result;
    }

    
    @Override
    public Object createValue( ValueInitializer initializer ) {
        checkWait( AccessType.WRITE );
        return ((Property)delegate).createValue( initializer );
    }

    @Override
    public Object createValue( Class clazz, ValueInitializer initializer ) {
        checkWait( AccessType.WRITE );
        return ((Property)delegate).createValue( clazz, initializer );
    }
    
    @Override
    public void set( Object value ) {
        checkWait( AccessType.WRITE );
        ((Property)delegate).set( value );
    }

    
    @Override
    public boolean add( Object e ) {
       checkWait( AccessType.WRITE );
       return ((ManyAssociation)delegate).add( e );
    }

    
    protected void checkWait( AccessType accessType ) {
        UnitOfWork uow = context.getUnitOfWork();
        Entity entity = context.getCompositePart( Entity.class );
        EntityKey key = new EntityKey( entity );
        
        EntityLock entityLock = locks.computeIfAbsent( key, k -> new EntityLock() );
        entityLock.lock( uow, accessType );
        
//        if (accessType.equals( AccessType.READ )) {
//            entityLock.readLock().lock();
//        }
//        else if (accessType.equals( AccessType.WRITE )) {
//            entityLock.writeLock().lock();
//        }
//        else {
//            throw new IllegalStateException( "Unknown accessType: " + accessType );
//        }
    }


    /**
     * 
     */
    protected class EntityLock {
        
        private List<Reference<UnitOfWork>> uows = new ArrayList();
        
        private ReferenceQueue      queue = new ReferenceQueue();
                
        private ReadWriteLock       lock = new ReentrantReadWriteLock();
    
        public synchronized void lock( UnitOfWork uow, AccessType accessType ) {
            throw new RuntimeException( "not yet implemented" );
//            PhantomReference
//            for (Reference<UnitOfWork> ref : uows) {
//                if (ref.
//            }
        }
    }

    
    protected class UowReference
            extends PhantomReference<UnitOfWork> {
        
        private EntityLock      entityLock;
        
        public UowReference( UnitOfWork referent, ReferenceQueue<? super UnitOfWork> q ) {
            super( referent, q );
        }

    }

    
    /**
     * 
     */
    protected class EntityKey {
        
        private String      key; 
    
        public EntityKey( Entity entity ) {
            key = entity.getClass().getName() + entity.id().toString();
        }
    
        @Override
        public int hashCode() {
            return key.hashCode();
        }
    
        @Override
        public boolean equals( Object obj ) {
            return key.equals( ((EntityKey)obj).key );
        }        
    }

}
