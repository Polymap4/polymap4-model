/* 
 * polymap.org
 * Copyright (C) 2012-2018, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.runtime.locking;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import java.lang.ref.WeakReference;

import org.apache.commons.lang3.StringUtils;

import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;

import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;

/**
 * Provides base abstractions for pessimistic locking of {@link Entity}s accessed from
 * different {@link UnitOfWork} (not Thread) instances.
 * <p>
 * <b>Beware</b>: Not thoroughly tested yet. Implementation currently uses polling
 * and {@link WeakReference} to get informed about the end of an {@link UnitOfWork}.
 * <p>
 * Implementation uses one global map for all locks. This map is filled with
 * {@link EntityLock} instances from all {@link EntityRepository} instances of the
 * lifetime of the JVM. Entries are never evicted from this global map.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class PessimisticLocking
        extends PropertyConcernBase
        implements PropertyConcern, ManyAssociation/*, Association*/ {

    private static final Log log = LogFactory.getLog( PessimisticLocking.class );

    // XXX memory sensitive cache?
    private static ConcurrentMap<EntityKey,EntityLock>  locks = new ConcurrentHashMap( 256, 0.75f, 4);
    
    /**
     * 
     */
    protected enum AccessMode {
        READ, WRITE
    }

    /**
     * Release all locks the given {@link UnitOfWork} might held.
     */
    public static void notifyClosed( UnitOfWork uow ) {
        locks.forEach( (key, lock) -> lock.checkRelease( uow ) );
    }
    
    
    // instance *******************************************
    
    @Override
    public Object get() {
        lock( AccessMode.READ );
        return ((Property)delegate).get();
    }

    
    @Override
    public Object createValue( ValueInitializer initializer ) {
        lock( AccessMode.WRITE );
        return ((Property)delegate).createValue( initializer );
    }

    
    @Override
    public void set( Object value ) {
        lock( AccessMode.WRITE );
        ((Property)delegate).set( value );
    }

    
    @Override
    public boolean add( Object elm ) {
       lock( AccessMode.WRITE );
       return ((ManyAssociation)delegate).add( elm );
    }

    
    protected void lock( AccessMode accessMode ) {
        //log.info( "lock: " + info().getName() + "()" + " : " + accessMode );
        UnitOfWork uow = context.getUnitOfWork();
        Entity entity = context.getEntity();
        
        // XXX check if cached entiyLock is for same type of locking (OneReader, MROW)
        EntityLock entityLock = locks.computeIfAbsent( new EntityKey( entity ), key -> 
                newLock( key, entity ) );
        
        entityLock.aquire( uow, entity, accessMode );
    }

    
    protected abstract EntityLock newLock( EntityKey key, Entity entity );

    
    /**
     * 
     */
    protected abstract class EntityLock {

        /**
         * 
         *
         * @param uow The {@link UnitOfWork} from which the entity is accessed.
         * @param entity The actual Entity instance from within the given UnitOfWork.
         * @param accessMode
         */
        public abstract void aquire( UnitOfWork uow, Entity entity, AccessMode accessMode );
        
        /**
         * 
         *
         * @param uow
         * @return True if the lock was aquired and actually got released.
         */
        public abstract boolean checkRelease( UnitOfWork uow );
        
        /**
         * 
         *
         * @param condition
         * @param mode
         * @param uow The {@link UnitOfWork} from which the entity is accessed.
         * @param entity The actual Entity instance from within the given UnitOfWork.
         */
        protected void await( Supplier<Boolean> condition, AccessMode mode, UnitOfWork uow, Entity entity ) {
            // XXX polling! wait that GC reclaimed readers and writer
            // a writer has read lock too, so we avoid writer check
            boolean firstLoop = true;
            while (!condition.get()) {
                if (firstLoop) {
                    log.debug( logPrefix() + "await lock: " + mode + " on: " + context.getEntity().id() );
                    firstLoop = false;
                }
                try { 
                    wait( 100 );
                    cleanStaleHolders();    
                } 
                catch (InterruptedException e) {
                    log.warn( logPrefix() + "Interrupted!" );
                }
            }
            if (!firstLoop) {
                log.debug( logPrefix() + "got lock on: " + context.getEntity().id() );
                // now we have the lock; the other UnitOfWork might have modified
                // the Entity state, so we have to reload; the client code has not seen
                // any properties of the entity yet
                uow.reload( entity );
            }
        }
        
        protected String logPrefix() {
            return "[" + StringUtils.right( Thread.currentThread().getName(), 2 ) + "] ";            
        }
        
        protected void cleanStaleHolders() {
        }
    }

    
    /**
     * 
     */
    protected static class EntityKey
            implements Comparable {
        
        private String      key; 
    
        public EntityKey( Entity entity ) {
            key = entity.getClass().getName() + entity.id().toString();
        }
    
        @Override
        public int hashCode() {
            return key.hashCode();
        }
    
        @Override
        public boolean equals( Object other ) {
            return key.equals( ((EntityKey)other).key );
        }

        @Override
        public int compareTo( Object other ) {
            return key.compareTo( ((EntityKey)other).key );
        }        
    }

}
