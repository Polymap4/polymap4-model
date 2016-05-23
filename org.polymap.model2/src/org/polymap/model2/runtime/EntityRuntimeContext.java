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
package org.polymap.model2.runtime;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreUnitOfWork;

/**
 * The API to access the engine from within an {@link Entity}. Holds the
 * {@link EntityStatus status} of the entity.
 * <p/>
 * Implementation note: This approach might not be that elegant than any kind of
 * dependency injection but it saves the memory of the references used by dependency
 * injection. Maybe later I will search for a better solution.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface EntityRuntimeContext {

    /**
     * The status an Entity can have.
     */
    public enum EntityStatus {
        LOADED( 0 ), 
        CREATED( 1 ), 
        MODIFIED( 2 ),
        REMOVED( 3 ),
        /**
         * This status indicates that the Entity was evicted from its UnitOfWork. Not
         * currently used.
         */
        EVICTED( 4 ),
        /**
         * After {@link UnitOfWork} is closed all of its entities become detached. A
         * detached entity must not be used anymore. Except for {@link Entity#id()}
         * and {@link Entity#status()} all methods throw an
         * {@link ModelRuntimeException}.
         */
        DETACHED( 5 );
        
        public int         status;
        
        EntityStatus( int status ) {
            this.status = status;    
        }
        
    }
    
    public Object id();

    public <E extends Entity> E getEntity();
    
    /**
     * Returns the {@link Entity} of this context or any of its mixins. 
     *
     * @param type The type of the entity or mixin to find.
     */
    public <T extends Composite> T getCompositePart( Class<T> type );
    
    public CompositeState getState();
    
    public EntityStatus getStatus();
    
    public void raiseStatus( EntityStatus newStatus );
    
    public void resetStatus( EntityStatus loaded );

    public UnitOfWork getUnitOfWork();

    public StoreUnitOfWork getStoreUnitOfWork();

    public EntityRepository getRepository();

}
