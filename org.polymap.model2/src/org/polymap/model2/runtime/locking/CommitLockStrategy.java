/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.runtime.locking;

import java.util.concurrent.atomic.AtomicBoolean;
import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class CommitLockStrategy {

    private static Log log = LogFactory.getLog( CommitLockStrategy.class );
    
    public abstract void lock();

    public abstract void unlock( boolean check );
    
    
    /**
     * Throw a {@link RuntimeException} when concurrent prepare/commit is detected.
     * 
     * @deprecated Seems to be broken. Throws wrong exception on rollback()!
     */
    public static class FailOnConcurrentCommit
            extends CommitLockStrategy {

        private AtomicBoolean       locked = new AtomicBoolean( false );
        
        @Override
        public void lock() {
            if (!locked.compareAndSet( false, true )) {
                throw new RuntimeException( "Concurrent prepare/commit detected!" );                
            }
        }

        @Override
        public void unlock( boolean check ) {
            if (check) {
                if (!locked.compareAndSet( true, false )) {
                    throw new RuntimeException( "Concurrent prepare/commit detected!" );                
                }                
            }
            else {
                locked.set( false );
            }
        }
    }
    

//    /**
//     * Serialize concurrent attempts to {@link #prepare()}/{@link #commit()}.
//     */
//    public static class Serialize
//            extends CommitLockStrategy {
//        
//        private ReentrantLock       lock = new ReentrantLock();
//        
//        private long                timeout;
//        
//        private TimeUnit            timeUnit;
//
//        
//        public Serialize() {
//        }
//
//        /**
//         * Causes {@link #lock()} to throw an {@link RuntimeException} if the given time
//         * expires before we get the lock.
//         * 
//         * @param timeout
//         * @param timeUnit
//         */
//        public Serialize( long timeout, TimeUnit timeUnit ) {
//            this.timeout = timeout;
//            this.timeUnit = timeUnit;
//        }
//
//        @Override
//        public void lock() {
//            if (timeout == 0) {
//                lock.lock();
//            }
//            else {
//                try {
//                    lock.tryLock( timeout, timeUnit );
//                }
//                catch (InterruptedException e) {
//                    throw new RuntimeException( e );
//                }
//            }
//        }
//
//        @Override
//        public void unlock( boolean check ) {
//            if (lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }
    
    
    /**
     * Do nothing ans ignore locking altogether. Save cycles and memory.
     */
    public static class Ignore
            extends CommitLockStrategy {
        
        @Override
        public void lock() {
        }

        @Override
        public void unlock( boolean check ) {
        }
    }
    
}
