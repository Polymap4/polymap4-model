/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.polymap.model2.store.tidbstore.indexeddb;

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class IDBCursor implements JSObject {
    String DIRECTION_NEXT = "next";

    String DIRECTION_NEXT_UNIQUE = "nextunique";

    String DIRECTION_PREVIOUS = "prev";

    String DIRECTION_PREVIOUS_UNIQUE = "prevunique";

    @JSProperty
    public abstract IDBCursorSource getSource();

    @JSProperty
    public abstract String getDirection();

    @JSProperty
    public abstract JSObject getKey();

    @JSProperty
    public abstract JSObject getValue();

    @JSProperty
    public abstract JSObject getPrimaryKey();

    public abstract IDBRequest update(JSObject value);

    public abstract void advance(int count);

    @JSMethod("continue")
    public abstract void doContinue();

    public abstract IDBRequest delete();
    
    /** XXX falko: check after {@link #doContinue()} */
    @NoSideEffects
    @JSBody(script = "return this === null;")
    public native boolean isNull();

}
