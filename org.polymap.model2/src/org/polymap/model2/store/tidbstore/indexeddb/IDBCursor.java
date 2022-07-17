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

public interface IDBCursor extends JSObject {
    String DIRECTION_NEXT = "next";

    String DIRECTION_NEXT_UNIQUE = "nextunique";

    String DIRECTION_PREVIOUS = "prev";

    String DIRECTION_PREVIOUS_UNIQUE = "prevunique";

    @JSProperty
    IDBCursorSource getSource();

    @JSProperty
    String getDirection();

    @JSProperty
    JSObject getKey();

    @JSProperty
    JSObject getValue();

    @JSProperty
    JSObject getPrimaryKey();

    IDBRequest update(JSObject value);

    void advance(int count);

//    @JSMethod("continue")
//    void doContinue();

    IDBRequest delete();
    
    /**
     * This method is used by success handlers of an {@link IDBCursorRequest} to
     * check if the cursor has another value.
     *
     * @return True signals that the next invocation of {@link #getValue()} returns a
     *         valid value.
     */
    @JSBody(script = "return this === null;")
    boolean isNull();

    
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
