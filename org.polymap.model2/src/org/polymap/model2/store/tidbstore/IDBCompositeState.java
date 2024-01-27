/* 
 * polymap.org
 * Copyright (C) 2020-2022, the @authors. All rights reserved.
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

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSBoolean;
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
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 * @author Falko
 */
public class IDBCompositeState
        implements CompositeState {

    private static final Log LOG = LogFactory.getLog( IDBCompositeState.class );

    protected Class<? extends Composite>    entityClass;
    
    protected JSStateObject                 state;

    
    public IDBCompositeState( Object id, Class<? extends Composite> entityClass ) {
        this.entityClass = Assert.notNull( entityClass );
        this.state = JSStateObject.create();
        this.state.set( "id", IDBStore.id( Assert.notNull( id ) ) );
    }
    
    
    public IDBCompositeState( Class<? extends Composite> entityClass, JSStateObject jsObject ) {
        this.entityClass = Assert.notNull( entityClass );
        this.state = Assert.notNull( jsObject );
    }


    @Override
    public Object id() {
        return ((JSString)state.get( "id" )).stringValue();
    }


    @Override
    public Class<? extends Composite> compositeInstanceType( Class declaredType ) {
        // XXX is this correct?
        return entityClass;
    }


    @Override
    public StoreProperty<?> loadProperty( PropertyInfo info ) {
        if (info.getMaxOccurs() > 1) { // Many, Collection, CompositeCollection
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
        return state;
    }
    
    
    public static JSObject jsValueOf( Object value ) {
        if (value == null) {
            return null;
        }
        else if (value instanceof String) {
            return JSString.valueOf( (String)value );
        }
        else if (value instanceof Integer) {
            return JSNumber.valueOf( ((Integer)value).intValue() );
        }
        else if (value instanceof Long) {
            return JSNumber.valueOf( (double)((Long)value).longValue() );
        }
        else if (value instanceof Date) {
            return JSDate.create( ((Date)value).getTime() );
        }
        else if (value instanceof Boolean) {
            return JSBoolean.valueOf( ((Boolean)value).booleanValue() );
        }
        else if (value instanceof Enum) {
            return JSString.valueOf( ((Enum<?>)value).name() );
        }
        else {
            throw new UnsupportedOperationException( "Unhandled Entity property type: " + value );
        }
    }
    
    
    public static Object javaValueOf( JSObject value, PropertyInfo<?> info ) {
        if (JSStateObject.isUndefined( value )) {
            return null;
        }
        else if (info.getType().equals( Boolean.class )) {
            return ((JSBoolean)value).booleanValue();
        }
        else if (info.getType().equals( Integer.class )) {
            return ((JSNumber)value).intValue();
        }
        else if (info.getType().equals( Long.class )) {
            return Long.valueOf( (long)((JSNumber)value).doubleValue() );
        }
        else if (info.getType().equals( Date.class )) {
            return new Date( (long)((JSDate)value).getTime() );
        }
        else if (Enum.class.isAssignableFrom( info.getType() )) {
            var s = ((JSString)value).stringValue();
            @SuppressWarnings({"unchecked", "rawtypes"})
            var enumType = (Class<Enum>)info.getType();
            @SuppressWarnings("unchecked")
            var result = Enum.valueOf( enumType, s );
            return result;
        }
        // FIXME for ManyAssociation backed by collections this works just by hazard!
        else if (JSString.isInstance( value )) {
            return ((JSString)value).stringValue();
        }
        else {
            throw new UnsupportedOperationException( "Unhandled Entity property type: " + info.getName() );
        }
    }
    
    
    /**
     * 
     */
    protected abstract class StorePropertyImpl
            implements StoreProperty<Object> {
        
        @Override
        public Object get() {
            var jsvalue = state.get( info().getNameInStore() );
            if (JSStateObject.isUndefined( jsvalue )) {
                return null;
            }
            else if (Composite.class.isAssignableFrom( info().getType() )
                    && !info().isAssociation()) { // XXX separate impl for Composite
                Class<? extends Composite> compositeType = info().getType();
                JSStateObject compositeState = jsvalue.cast();
                return new IDBCompositeState( compositeType, compositeState );
            }
            else {
                return javaValueOf( jsvalue, info() );
            }
        }
        
        @Override
        public void set( Object value ) {
            Assert.that( !Composite.class.isInstance( value ), "Composite value is not yet supported." );
            state.set( info().getNameInStore(), jsValueOf( value ) );
        }
        
        @Override
        public Object createValue( Class actualType ) {
            if (Composite.class.isAssignableFrom( actualType )) {
                var compositeState = JSStateObject.create();
                state.set( info().getNameInStore(), compositeState );
                return new IDBCompositeState( actualType, compositeState );
            }
            else {
                throw new RuntimeException( "not yet implemented: " + actualType );
            }
        }
    };

    
    /**
     * 
     */
    protected abstract class StoreCollectionPropertyImpl
            implements StoreCollectionProperty<Object>, StoreProperty<Object> {

        @Override
        public int size() {
            JSArray<?> array = (JSArray<?>)state.get( info().getNameInStore() );
            return JSObjects.isUndefined( array ) ? 0 : array.getLength();
        }

        protected JSArray<JSObject> ensureArray() {
            @SuppressWarnings("unchecked")
            JSArray<JSObject> array = (JSArray<JSObject>)state.get( info().getNameInStore() );
            if (JSObjects.isUndefined( array )) {
                array = JSArray.create();
                state.set( info().getNameInStore(), array );
            }
            return array;
        }

        @Override
        public Iterator<Object> iterator() {
            JSArray<?> array = (JSArray<?>)state.get( info().getNameInStore() );
            if (!JSObjects.isUndefined( array )) {
                return new Iterator<Object>() {
                    int index = 0;
                    @Override
                    public boolean hasNext() {
                        return index < array.getLength();
                    }
                    @Override
                    public Object next() {
                        var jsvalue = array.get( index++ );
                        if (Composite.class.isAssignableFrom( info().getType() )
                                && !info().isAssociation()) { // XXX separate impl for composite
                            Class<? extends Composite> compositeType = info().getType();
                            JSStateObject compositeState = jsvalue.cast();
                            return new IDBCompositeState( compositeType, compositeState );
                        }
                        else {
                            return javaValueOf( jsvalue, info() );
                        }
                    }
                };
            }
            else {
                return Collections.emptyIterator();
            }
        }

        @Override
        public boolean add( Object elm ) {
            ensureArray().push( jsValueOf( elm ) );
            return true;
        }

        @Override
        public boolean remove( Object elm ) {
            @SuppressWarnings("unchecked")
            JSArray<JSObject> array = (JSArray<JSObject>)state.get( info().getNameInStore() );
            if (JSObjects.isUndefined( array )) {
                return false;
            }
            Assert.that( !Composite.class.isInstance( elm ), "Composite value is not yet supported." );
            var jsValue = jsValueOf( elm );
            for (int i = 0; i < array.getLength(); i++) {
                if (array.get( i ) == jsValue) {
                    LOG.info( "Collection: remove index=" + i );
                    array.splice( i, 1 );
                    return true;
                }
            }
            return false;
        }

        @Override
        public Object createValue( Class actualType ) {
            if (Composite.class.isAssignableFrom( actualType )
                    && !info().isAssociation()) { // XXX separate impl for Composite
                var compositeState = JSStateObject.create();
                ensureArray().push( compositeState );
                return new IDBCompositeState( actualType, compositeState );
            }
            else {
                throw new RuntimeException( "not yet implemented: " + actualType );
            }
        }

        @Override
        public void clear() {
            state.set( info().getNameInStore(), JSArray.create() );
        }
    }
}
