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

import java.util.logging.Logger;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.polymap.model2.Composite;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreProperty;

import areca.common.Assert;

/**
 * 
 *
 * @author git_user_name
 */
public class IDBCompositeState
        implements CompositeState {

    private static final Logger LOG = Logger.getLogger( IDBCompositeState.class.getName() );
    
    private Class<? extends Composite>  entityClass;
    
    private JSStateObject               jsObject;

    
    public IDBCompositeState( Object id, Class<? extends Composite> entityClass ) {
        this.entityClass = Assert.notNull( entityClass );
        this.jsObject = JSStateObject.create();
        this.jsObject.set( "id", IDBStore.id( Assert.notNull( id ) ) );
        LOG.info( "jsObject created: " + id() );
    }
    
    
    public IDBCompositeState( Class<? extends Composite> entityClass, JSStateObject jsObject ) {
        this.entityClass = Assert.notNull( entityClass );
        this.jsObject = Assert.notNull( jsObject );
    }


    @Override
    public Object id() {
        return ((JSString)jsObject.get( "id" )).stringValue();
    }


    @Override
    public Class<? extends Composite> compositeInstanceType( Class declaredType ) {
        // XXX is this correct?
        return entityClass;
    }


    @Override
    public StoreProperty<?> loadProperty( PropertyInfo info ) {
        Assert.that( !info.isAssociation() && info.getMaxOccurs() == 1 );
        return new StoreProperty<Object>() {
            @Override
            public Object get() {
                JSObject result = jsObject.get( info().getNameInStore() );
                if (JSStateObject.isUndefined( result )) {
                    return null;
                }
                else if (JSString.isInstance( result )) {
                    return ((JSString)result).stringValue();
                }
                else {
                    throw new UnsupportedOperationException( "Unhandled type: " + info.getName() );
                }
            }
            @Override
            public void set( Object value ) {
                jsObject.set( info().getNameInStore(), JSString.valueOf( (String)value ) );
            }
            @Override
            public Object createValue( Class actualType ) {
                // XXX Auto-generated method stub
                throw new RuntimeException( "not yet implemented." );
            }
            @Override
            public PropertyInfo info() {
                return info;
            }
        };
    }


    @Override
    public JSStateObject getUnderlying() {
        return jsObject;
    }
    
}
