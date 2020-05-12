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

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventTarget;

public interface IDBTransaction extends JSObject, EventTarget {
    @JSProperty
    String getMode();

    @JSProperty
    IDBDatabase getDb();

    @JSProperty
    IDBError getError();

    IDBObjectStore objectStore(String name);

    void abort();
    
    /**
     * For an active transaction, commits the transaction. Note that this doesn't
     * normally have to be called — a transaction will automatically commit when all
     * outstanding requests have been satisfied and no new requests have been made.
     * commit() can be used to start the commit process without waiting for events
     * from outstanding requests to be dispatched.
     */
    void commit();

    @JSProperty("onabort")
    void setOnAbort(EventHandler handler);

    @JSProperty("oncomplete")
    void setOnComplete(EventHandler handler);

    @JSProperty("onerror")
    void setOnError(EventHandler handler);
}
