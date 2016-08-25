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
package org.polymap.model2.runtime.locking;

import java.util.function.Predicate;
import java.util.function.Supplier;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * Implements One-Reader {@link PessimisticLocking}.
 * <p>
 * <b>Beware</b>: Not thoroughly tested yet. See {@link PessimisticLocking} for
 * general limitations.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class OneReaderPessimisticLocking
        extends PessimisticLocking {

    private static final Log log = LogFactory.getLog( OneReaderPessimisticLocking.class );
    
    @Override
    protected EntityLock newLock( EntityKey key, Entity entity ) {
        return new OneReaderEntityLock();
    }


    /**
     * 
     */
    protected class OneReaderEntityLock
            extends EntityLock {
        
        /** Aquired by this {@link UnitOfWork}. */
        protected Reference<UnitOfWork>   reader;
        
        protected Supplier<Boolean>       noReader = () -> reader == null || reader.get() == null || !reader.get().isOpen();
        
        protected Predicate<UnitOfWork>   isAquired = uow -> reader != null && reader.get() == uow;
        
        @Override
        public void aquire( UnitOfWork uow, AccessMode accessMode ) {
            // do we have to lock? -> avoid synchronize
            if (!isAquired.test( uow )) {
                synchronized (this) {
                    await( noReader, accessMode );
                    reader = new WeakReference( uow );
                }
            }
        }

        @Override
        public boolean checkRelease( UnitOfWork uow ) {
            if (isAquired.test( uow )) {
                synchronized (this) {
                    log.info( "[" + StringUtils.right( Thread.currentThread().getName(), 2 ) + "]" 
                            + " released: " + context.getEntity().id() );
                    reader = null;
                    notifyAll();
                    return true;
                }
            }
            return false;
        }
    }

//    class Sync 
//            extends AbstractQueuedSynchronizer {
//        
//    }
    
}
