/* 
 * polymap.org
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package org.polymap.model2.store.tidbstore;

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.indexeddb.IDBCursor;

/**
 * 
 *
 * @author Falko
 */
public interface IDBCursor2 
        extends IDBCursor {

    @NoSideEffects
    @JSMethod("continue")
    void doContinue();

    @NoSideEffects
    @JSMethod("continue")
    void doContinue( JSObject nextKey );

    @NoSideEffects
    @JSMethod("continuePrimaryKey")
    void doContinuePrimaryKey( JSObject nextKey, JSObject nextPrimaryKey );

}