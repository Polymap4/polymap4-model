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

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSDate;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;

import org.polymap.model2.Composite;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreCollectionProperty;
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
        // Assert.that( !info.isAssociation() && info.getMaxOccurs() == 1 );
        if (info.getMaxOccurs() > 1) {
            return new StoreCollectionPropertyImpl() {
                @Override public PropertyInfo info() { return info; }
                @Override public Object get() { throw new RuntimeException( "..." ); }
                @Override public void set( Object value ) { throw new RuntimeException( "..." ); }
            };             
        }
        else {
            return new StorePropertyImpl() {
                @Override public PropertyInfo info() { return info; }
            };
        }
    }


    @Override
    public JSStateObject getUnderlying() {
        return jsObject;
    }
    
    
    protected JSObject jsValueOf( Object value ) {
        if (value instanceof String) {
            return JSString.valueOf( (String)value );
        }
        else if (value instanceof Integer) {
            return JSNumber.valueOf( ((Integer)value).intValue() );
        }
        else if (value instanceof Date) {
            return JSDate.create( ((Date)value).getTime() );
        }
        else {
            throw new UnsupportedOperationException( "Unhandled: " + value );
        }
    }
    
    
    protected Object javaValueOf( JSObject value, PropertyInfo<?> info ) {
        if (JSStateObject.isUndefined( value )) {
            return null;
        }
        else if (JSString.isInstance( value )) {
            return ((JSString)value).stringValue();
        }
        else {
            throw new UnsupportedOperationException( "Unhandled type: " + info.getName() );
        }
        
    }
    
    
    /**
     * 
     */
    protected abstract class StorePropertyImpl
            implements StoreProperty<Object> {
        
        @Override
        public Object get() {
            return javaValueOf( jsObject.get( info().getNameInStore() ), info() );
        }
        
        @Override
        public void set( Object value ) {
            jsObject.set( info().getNameInStore(), jsValueOf( value ) );
        }
        
        @Override
        public Object createValue( Class actualType ) {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }
    };

    
    /**
     * 
     */
    protected abstract class StoreCollectionPropertyImpl
            implements StoreCollectionProperty<Object>, StoreProperty<Object> {

        @Override
        public int size() {
            JSArray<?> array = (JSArray<?>)jsObject.get( info().getNameInStore() );
            return JSObjects.isUndefined( array ) ? 0 : array.getLength();
        }

        @Override
        public Iterator<Object> iterator() {
            JSArray<?> array = (JSArray<?>)jsObject.get( info().getNameInStore() );
            if (!JSObjects.isUndefined( array )) {
                return new Iterator<Object>() {
                    int index = 0;
                    @Override
                    public boolean hasNext() {
                        return index < array.getLength();
                    }
                    @Override
                    public Object next() {
                        return javaValueOf( array.get( index++ ), info() );
                    }
                };
            }
            else {
                return Collections.emptyIterator();
            }
        }

        @Override
        public boolean add( Object elm ) {
            @SuppressWarnings("unchecked")
            JSArray<JSObject> array = (JSArray<JSObject>)jsObject.get( info().getNameInStore() );
            if (JSObjects.isUndefined( array )) {
                array = JSArray.create();                
                jsObject.set( info().getNameInStore(), array );
            }
            array.push( jsValueOf( elm ) );
            return true;
        }

        @Override
        public Object createValue( Class actualType ) {
            throw new RuntimeException( "not yet implemented." );
        }

        @Override
        public void clear() {
            throw new RuntimeException( "not yet implemented." );
        }
    }
}
