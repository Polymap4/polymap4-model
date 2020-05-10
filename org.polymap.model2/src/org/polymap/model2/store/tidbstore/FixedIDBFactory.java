package org.polymap.model2.store.tidbstore;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.indexeddb.IDBFactory;
import org.teavm.jso.indexeddb.IDBOpenDBRequest;

/**
 * {@link IDBFactory} has a bug in {@link #getInstance()} 
 */
public abstract class FixedIDBFactory implements JSObject {
    
    public static boolean isSupported() {
        return !getInstanceImpl().isUndefined();
    }

    @JSBody(script = "return typeof this === 'undefined';")
    private native boolean isUndefined();

    public static FixedIDBFactory getInstance() {
        FixedIDBFactory factory = getInstanceImpl();
        if (factory.isUndefined()) {
            throw new IllegalStateException("IndexedDB is not supported in this browser");
        }
        return factory;
    }

    @JSBody(script = "return window.indexedDB || window.mozIndexedDB || window.webkitIndexedDB || "
            + "window.msIndexedDB;")
    static native FixedIDBFactory getInstanceImpl();

    public abstract IDBOpenDBRequest open(String name, int version);

    public abstract IDBOpenDBRequest deleteDatabase(String name);

    public abstract int cmp(JSObject a, JSObject b);
}
