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
package org.polymap.model2.runtime.locking;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.MapMaker;

import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * Provides base abstractions of pessimistic locking of {@link Entity}s accessed from
 * different {@link UnitOfWork} (not Thread!) instances.
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
    private static ConcurrentMap<EntityKey,EntityLock>  locks = new MapMaker().concurrencyLevel( 4 ).initialCapacity( 256 ).makeMap();
    
    /**
     * 
     */
    protected enum AccessMode {
        READ, WRITE
    }

    /**
     * 
     *
     * @param uow
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
    public boolean add( Object e ) {
       lock( AccessMode.WRITE );
       return ((ManyAssociation)delegate).add( e );
    }

    
    protected void lock( AccessMode accessMode ) {
        UnitOfWork uow = context.getUnitOfWork();
        Entity entity = context.getEntity();
        
        // XXX check if cached entiyLock is for same type of locking (OneReader, MROW)
        EntityLock entityLock = locks.computeIfAbsent( new EntityKey( entity ), key -> 
                newLock( key, entity ) );
        
        entityLock.aquire( uow, accessMode );
    }

    
    protected abstract EntityLock newLock( EntityKey key, Entity entity );

    
    /**
     * 
     */
    protected abstract class EntityLock {

        public abstract void aquire( UnitOfWork uow, AccessMode accessMode );
        
        public abstract boolean checkRelease( UnitOfWork uow );
        
        protected void await( Supplier<Boolean> condition, AccessMode mode ) {
            // FIXME polling! wait that GC reclaimed readers and writer
            // a writer has read lock too, so we avoid writer check
            boolean firstLoop = true;
            while (!condition.get()) {
                if (firstLoop) {
                    log.info( "[" + StringUtils.right( Thread.currentThread().getName(), 2 ) + "]" 
                            + " await lock: " + mode + " on: " + context.getEntity().id() );
                    firstLoop = false;
                }
                try { 
                    wait( 100 );
                    cleanStaleHolders();    
                } 
                catch (InterruptedException e) {
                }
            }
            if (!firstLoop) {
                log.info( "[" + StringUtils.right( Thread.currentThread().getName(), 2 ) + "]" 
                        + " got lock." );
            }
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
