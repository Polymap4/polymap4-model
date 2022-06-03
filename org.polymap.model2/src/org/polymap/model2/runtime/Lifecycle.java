/* 
 * polymap.org
 * Copyright (C) 2014-2022, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.runtime;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;

/**
 * An {@link Entity} can implement this interface in order to get lifecycle events.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface Lifecycle {

    public enum State {
        /** Fired (just) on created, modified or removed entities. */
        BEFORE_SUBMIT,
        /** Fired (just) on created, modified or removed entities. */
        AFTER_SUBMIT,
        BEFORE_DISCARD,
        AFTER_DISCARD,
        /** Fired when {@link UnitOfWork#refresh()} */
        AFTER_REFRESH,
        /** Fired when the Entity is first loaded into the cache. */
        AFTER_LOADED,
        /**  */
        AFTER_CREATED,
        /** Fired when the {@link Entity#status()} switches from {@link EntityStatus#LOADED} to {@link EntityStatus#MODIFIED}. */
        AFTER_MODIFIED,
        /** @deprecated Yet to be supported by the engine. */
        BEFORE_REMOVED
    }
    
    public void onLifecycleChange( State state );

}
