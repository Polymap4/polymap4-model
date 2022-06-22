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

import org.teavm.jso.JSMethod;
import org.teavm.jso.indexeddb.IDBCursorRequest;
import org.teavm.jso.indexeddb.IDBIndex;
import org.teavm.jso.indexeddb.IDBKeyRange;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class IDBIndex2 extends IDBIndex {

    @JSMethod
    public abstract IDBCursorRequest openCursor(IDBKeyRange range, String order);

    @JSMethod
    public abstract IDBCursorRequest openKeyCursor(IDBKeyRange range, String order);

    
}
