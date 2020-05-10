/* 
 * polymap.org
 * Copyright (C) 2020, the @authors. All rights reserved.
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
import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class JSStateObject 
        implements JSObject {

    @NoSideEffects
    @JSBody(params = "object", script = "return typeof object === 'undefined';")
    public static native boolean isUndefined( JSObject object );

    
    @JSBody( script = "return {};" )
    public static native JSStateObject create();


    @JSIndexer
    public abstract JSObject get( String key );


    @JSIndexer
    public abstract void set( String key, JSObject object );
    
}
