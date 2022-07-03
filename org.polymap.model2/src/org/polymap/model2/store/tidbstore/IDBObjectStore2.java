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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.indexeddb.IDBCursorRequest;
import org.teavm.jso.indexeddb.IDBIndex;
import org.teavm.jso.indexeddb.IDBKeyRange;
import org.teavm.jso.indexeddb.IDBObjectStore;

/**
 * 
 */
public abstract class IDBObjectStore2
        extends IDBObjectStore {

    @JSBody(params = {"obj"}, script = "console.log( obj );")
    public static native void console( JSObject obj );

    public abstract IDBIndex createIndex(String name, String key, IndexParameters params );

    public abstract IDBCursorRequest openKeyCursor();

    public abstract IDBCursorRequest openKeyCursor(IDBKeyRange range);

    public abstract IDBCursorRequest openKeyCursor(IDBKeyRange range, String direction);

    
    public static abstract class IndexParameters
            implements JSObject {

        @JSBody(params = {"obj"}, script = "console.log( obj );")
        public static native void console( JSObject obj );
        
        @JSBody( script = "return {};" )
        public static native IndexParameters create();
        
        @JSProperty
        public abstract void setMultiEntry( boolean value );
    }
}
