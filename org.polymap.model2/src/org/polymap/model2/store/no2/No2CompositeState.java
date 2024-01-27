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
package org.polymap.model2.store.no2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.common.Constants;

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
public class No2CompositeState
        implements CompositeState {

    private static final Log LOG = LogFactory.getLog( No2CompositeState.class );

    public static final String  FIELD_ID = Constants.DOC_ID; // "_id";

    public static final String  FIELD_SEP = ".";

    private Class<? extends Composite>  entityClass;

    private Document                    doc;
    
    private String                      fieldNameBase;

    
    public No2CompositeState( Object id, Class<? extends Composite> entityClass ) {
        this.entityClass = Assert.notNull( entityClass );
        this.doc = Document.createDocument();
        //this.doc.put( FIELD_ID, NitriteId.newId().getIdValue() );
    }
    
    
    @SuppressWarnings("unchecked")
    public No2CompositeState( Class<?/* extends Composite*/> entityClass, Document state ) {
        this.entityClass = Assert.notNull( (Class<? extends Composite>)entityClass );
        this.doc = Assert.notNull( state );
    }

    @Override
    public Object id() {
        return doc.getId().getIdValue();
    }

    @Override
    public Document getUnderlying() {
        return doc;
    }

    void setUnderlying( Document doc ) {
        this.doc = doc;
    }
    
    @Override
    public Class<? extends Composite> compositeInstanceType( Class declaredType ) {
        return entityClass;
    }

    @Override
    public StoreProperty loadProperty( PropertyInfo info ) {
        if (info.getMaxOccurs() > 1) { // Many, Collection, CompositeCollection
            return new StoreCollectionPropertyImpl() {
                { fieldName = fieldName( this ); }
                @Override public PropertyInfo info() { return info; }
                @Override public Object get() { throw new RuntimeException( "..." ); }
                @Override public void set( Object value ) { throw new RuntimeException( "..." ); }
            };             
        }
        else {
            return new StorePropertyImpl() {
                { fieldName = fieldName( this ); }
                @Override public PropertyInfo info() { return info; }
            };
        }
    }

    protected String fieldName( StoreProperty<?> prop ) {
        return fieldNameBase == null
                ? prop.info().getNameInStore()
                : String.join( FIELD_SEP, fieldNameBase, prop.info().getNameInStore() ); 
    }
    
    /**
     * 
     */
    protected abstract class StorePropertyImpl
            implements StoreProperty<Object> {
        
        protected String fieldName;
        
        @Override
        public Object get() {
            if (!doc.containsKey( fieldName )) {
                return null;
            }
            else if (Composite.class.isAssignableFrom( info().getType() )
                    && !info().isAssociation()) { // XXX separate impl for Composite
                var compositeDoc = (Document)doc.get( fieldName, Document.class );
                return new No2CompositeState( info().getType(), compositeDoc );
            }
            else {
                return doc.get( fieldName, (Class<?>)info().getType() );
            }
        }
        
        @Override
        public void set( Object value ) {
            Assert.that( !Composite.class.isInstance( value ), "Composite value is not yet supported." );
            if (value != null) {
                doc.put( fieldName, value );
            }
            else {
                doc.remove( fieldName );
            }
        }
        
        @Override
        public Object createValue( Class actualType ) {
            if (Composite.class.isAssignableFrom( actualType )) {
                var newdoc = Document.createDocument();
                doc.put( fieldName, newdoc );
                return new No2CompositeState( actualType, newdoc );
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

        protected String fieldName;
        
        @Override
        public int size() {
            var l = doc.get( fieldName, List.class );
            return l != null ? l.size() : 0;
        }

        @Override
        public Iterator<Object> iterator() {
            var l = doc.get( fieldName, List.class );
            if (l != null) {
                return new Iterator<Object>() {
                    Iterator<Object> it = l.iterator();
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }
                    @Override
                    public Object next() {
                        if (Composite.class.isAssignableFrom( info().getType() )
                                && !info().isAssociation()) { // XXX separate impl for composite
                            var compositeDoc = (Document)it.next();
                            return new No2CompositeState( info().getType(), compositeDoc );
                        }
                        else {
                            return it.next();
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
            var l = doc.get( fieldName, List.class );
            if (l == null) {
                doc.put( fieldName, l = new ArrayList<>() );
            }
            return doc.get( fieldName, List.class ).add( elm );
        }

        @Override
        public boolean remove( Object elm ) {
            var l = doc.get( fieldName, List.class );
            return l != null ? doc.get( fieldName, List.class ).remove( elm ) : false;
        }

        @Override
        public Object createValue( Class actualType ) {
            if (Composite.class.isAssignableFrom( actualType ) 
                    && !info().isAssociation()) { // XXX separate impl for Composite
                List<Object> l = doc.get( fieldName, List.class );
                if (l == null) {
                    doc.put( fieldName, l = new ArrayList<>() );
                }
                var newdoc = Document.createDocument();
                l.add( newdoc );
                return new No2CompositeState( actualType, newdoc );
            }
            else {
                throw new RuntimeException( "not yet implemented: " + actualType );
            }
        }

        @Override
        public void clear() {
            doc.remove( fieldName );
        }
    }

}
