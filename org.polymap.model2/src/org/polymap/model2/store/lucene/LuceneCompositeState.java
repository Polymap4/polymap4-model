/* 
 * polymap.org
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package org.polymap.model2.store.lucene;

import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;

import org.polymap.model2.Composite;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreCollectionProperty;
import org.polymap.model2.store.StoreProperty;

import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 * @author Falko
 */
public class LuceneCompositeState
        implements CompositeState {

    private static final Log LOG = LogFactory.getLog( LuceneCompositeState.class );

    public static final String  FIELD_ID = "id";

    private Class<? extends Composite>  entityClass;

    private Document                    doc;

    
    public LuceneCompositeState( Object id, Class<? extends Composite> entityClass ) {
        this.entityClass = Assert.notNull( entityClass );
        this.doc = new Document();
        this.doc.add( new StringField( FIELD_ID, (String)id, Store.YES ) );
    }
    
    
    public LuceneCompositeState( Class<? extends Composite> entityClass, Document state ) {
        this.entityClass = Assert.notNull( entityClass );
        this.doc = Assert.notNull( state );
    }

    @Override
    public Object id() {
        return doc.get( FIELD_ID );
    }

    @Override
    public Document getUnderlying() {
        return doc;
    }

    @Override
    public Class<? extends Composite> compositeInstanceType( Class declaredType ) {
        return entityClass;
    }

    @Override
    public StoreProperty loadProperty( PropertyInfo info ) {
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

    
    /**
     * 
     */
    protected abstract class StorePropertyImpl
            implements StoreProperty<Object> {
        
        @Override
        public Object get() {
            var field = doc.getField( info().getNameInStore() );
            if (field == null) {
                return null;
            }
            else if (Composite.class.isAssignableFrom( info().getType() )
                    && !info().isAssociation()) { // XXX separate impl for Composite
//                Class<? extends Composite> compositeType = info().getType();
//                JSStateObject compositeState = jsvalue.cast();
//                return new IDBCompositeState( compositeType, compositeState );
                throw new RuntimeException( "not yet implemented." );
            }
            else {
                return ValueCoder.decode( info(), field );
            }
        }
        
        @Override
        public void set( Object value ) {
            Assert.that( !Composite.class.isInstance( value ), "Composite value is not yet supported." );
            doc.removeField( info().getNameInStore() );
            if (value != null) {
                doc.add( ValueCoder.encode( info(), value ) );
            }
        }
        
        @Override
        public Object createValue( Class actualType ) {
            if (Composite.class.isAssignableFrom( actualType )) {
//                var compositeState = JSStateObject.create();
//                state.set( info().getNameInStore(), compositeState );
//                return new IDBCompositeState( actualType, compositeState );
                throw new RuntimeException( "not yet implemented." );
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
            return doc.getFields( info().getNameInStore() ).length;
        }

        @Override
        public Iterator<Object> iterator() {
            return Sequence.of( doc.getFields( info().getNameInStore() ) )
                    .map( field -> {
                        return ValueCoder.decode( info(), field );
                    })
                    .asIterable().iterator();
        }

        @Override
        public boolean add( Object elm ) {
            doc.add( ValueCoder.encode( info(), elm ) );
            return true;
        }

        @Override
        public boolean remove( Object elm ) {
            throw new RuntimeException( "not yet implemented" );
        }

        @Override
        public Object createValue( Class actualType ) {
            if (Composite.class.isAssignableFrom( actualType ) 
                    && !info().isAssociation()) { // XXX separate impl for Composite
                throw new RuntimeException( "not yet implemented: " + actualType );
            }
            else {
                throw new RuntimeException( "not yet implemented: " + actualType );
            }
        }

        @Override
        public void clear() {
            doc.removeFields( info().getNameInStore() );
        }
    }

}
