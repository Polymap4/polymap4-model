/* 
 * polymap.org
 * Copyright (C) 2012-2015, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.store.recordstore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.polymap.model2.Composite;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreCollectionProperty;
import org.polymap.model2.store.StoreProperty;
import org.polymap.recordstore.IRecordState;
import org.polymap.recordstore.lucene.LuceneRecordState;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class RecordCompositeState
        implements CompositeState {
    
//    public static final String      KEY_DELIMITER = "/";

    public static final String      TYPE_KEY = "_type_";
    
//    public static String buildKey( String... parts ) {
//        // Joiner.on( KEY_DELIMITER ).skipNulls().join( baseKey, info.getNameInStore() );
//        StringBuilder result = new StringBuilder( 256 );
//        for (String part : parts) {
//            if (part != null && part.length() > 0) {
//                if (result.length() > 0) {
//                    result.append( KEY_DELIMITER );
//                }
//                result.append( part );
//            }
//        }
//        return result.toString();
//    }
    
    // instance *******************************************
    
    protected IRecordState          state;
    
    protected FieldnameBuilder      basename;

    
    public RecordCompositeState( IRecordState state ) {
        assert state != null;
        this.state = state;
        this.basename = FieldnameBuilder.EMPTY;
    }

    private RecordCompositeState( IRecordState state, FieldnameBuilder basename ) {
        assert state != null;
        assert basename != null;
        this.state = state;
        this.basename = basename;
    }

    @Override
    public Object id() {
        // a non-Entity Composite property does not have an id 
        if (basename != FieldnameBuilder.EMPTY) {
            throw new IllegalStateException( "Composite property does not have an id." );            
        } 
        else {
            return state.id();
        }
    }

    @Override
    public Class<? extends Composite> compositeInstanceType( Class declared ) {
        try {
            // field is written by CompositeCollectionPropertyImpl.createValue() or
            // CompositePropertyImpl.createValue()
            String classname = state.get( basename.composite( TYPE_KEY ).get() );
            return classname != null && !declared.getName().equals( classname )
                    ? (Class<? extends Composite>)declared.getClassLoader().loadClass( classname )
                     : declared;
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Object getUnderlying() {
        // a non-Entity Composite property does not have an underlying representation 
        assert basename == FieldnameBuilder.EMPTY;
        return state;
    }

    @Override
    public StoreProperty loadProperty( PropertyInfo info ) {
        // association
        if (info.isAssociation()) {
            return info.getMaxOccurs() > 1 
                    ? new CollectionPropertyImpl( info, basename )
                    : new PropertyImpl( info, basename );
        }
        // composite
        else if (Composite.class.isAssignableFrom( info.getType() )) {
            return info.getMaxOccurs() > 1 
                    ? new CompositeCollectionPropertyImpl( info, basename )
                    : new CompositePropertyImpl( info, basename );
        }
        // primitive
        else {
            return info.getMaxOccurs() > 1 
                    ? new CollectionPropertyImpl( info, basename )
                    : new PropertyImpl( info, basename );
        }
    }


    /*
     * 
     */
    protected class PropertyImpl
            implements StoreProperty {
        
        protected PropertyInfo          info;
        
        protected FieldnameBuilder      fieldname;
        
        
        protected PropertyImpl( PropertyInfo info, FieldnameBuilder parentname ) {
            this.info = info;
            this.fieldname = parentname.composite( info.getNameInStore() );
        }

        public CompositeState createValue( Class actualType ) {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }

        public Object get() {
            Object value = state.get( fieldname.get() );
            if (value != null && info.getType().isEnum()) {
                value = Enum.valueOf( info.getType(), (String)value );
            }
            return value;
        }

        public void set( Object value ) {
            if (value == null) {
                state.remove( fieldname.get() );
            }
            else if (value instanceof Enum) {
                state.put( fieldname.get(), ((Enum)value).toString() );
            }
            else {
                state.put( fieldname.get(), value );
            }
        }

        public Object createValue() {
            return info().getDefaultValue();
        }

        public PropertyInfo info() {
            return info;
        }
        
    }
    

    /*
     * 
     */
    protected class CompositePropertyImpl
            extends PropertyImpl {

        protected CompositePropertyImpl( PropertyInfo info, FieldnameBuilder parentname ) {
            super( info, parentname );
        }
        
        @Override
        public CompositeState get() {
            Object id = state.get( fieldname.composite( "_id_" ).get() );
            return id != null ? new RecordCompositeState( state, fieldname ) : null;
        }
        
        @Override
        public CompositeState createValue( Class actualType ) {
            state.put( fieldname.composite( "_id_" ).get(), "created" );
            //
            if (!info.getType().getName().equals( actualType.getName() )) {
                state.put( fieldname.composite( TYPE_KEY ).get(), actualType.getName() );
            }
            return new RecordCompositeState( state, fieldname );
        }
        
        @Override
        public void set( Object value ) {
            CompositeState compState = get();
            if(value instanceof Object []) {
                Object[] array = (Object[]) value;
                if(array.length == 2) {
                    Object obj1 = array[0];
                    Object obj2 = array[1];
                    if(obj1 instanceof CompositeInfo && obj2 instanceof LuceneRecordState) {
                        CompositeInfo info = (CompositeInfo) obj1;
                        LuceneRecordState newState = (LuceneRecordState) obj2;
                        List<String> keys = new ArrayList<String>();
                        Iterator<Entry<String,Object>> newIter = newState.iterator();
                        while(newIter.hasNext()) {
                            Entry<String,Object> newNext = newIter.next();
                            if(newNext.getKey().startsWith( fieldname.get() + "/" )) {
                                keys.add( newNext.getKey() );
                            }
                        }
                        for(String key : keys) {
                            if(!key.endsWith( "/_id_" )) {
                                PropertyInfo propInfo = info.getProperty( key.substring( key.lastIndexOf( "/" )+1) );
                                StoreProperty prop = compState.loadProperty( propInfo );
                                prop.set( newState.get( key ) );
                            }
                        }
                    }
                }
            }
        }
    }
    

    /**
     * 
     */
    protected class CollectionPropertyImpl
            extends PropertyImpl
            implements StoreCollectionProperty {

        protected CollectionPropertyImpl( PropertyInfo info, FieldnameBuilder parentname ) {
            super( info, parentname );
        }

        @Override
        public CompositeState createValue( Class actualType ) {
            throw new RuntimeException( "createValue() is not allowed for primitive value properties." );
        }

        @Override
        public int size() {
            Integer result = state.get( fieldname.arraySize().get() );
            return result != null ? result : 0;
        }

        @Override
        public Iterator iterator() {
            return new Iterator() {
                int size = size();
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < size;
                }

                @Override
                public Object next() {
                    return state.get( fieldname.arrayElement( index++ ).get() );
                }

                @Override
                public void remove() {
                    CollectionPropertyImpl.this.remove( index );
                }
            };
        }

        public void remove( int index ) {
            // shift down all fields above index
            for (int i=index; i<size()-1; i++) {
                String targetPrefix = fieldname.arrayElement( i ).get();
                String srcPrefix = fieldname.arrayElement( i+1 ).get();

                Iterator<Entry<String,Object>> it = state.iterator();
                Map<String,Object> newEntries = new HashMap();
                
                // create new keys/values and remove old values (don't modify while iterate)
                while (it.hasNext()) {
                    Entry<String,Object> entry = it.next();
                    if (entry.getKey().startsWith( srcPrefix )) {
                        String newKey = StringUtils.replace( entry.getKey(), srcPrefix, targetPrefix );
                        newEntries.put( newKey, entry.getValue() );
                        it.remove();
                    }
                }
                // add new entries
                for (Entry<String,Object> entry : newEntries.entrySet()) {
                    state.put( entry.getKey(), entry.getValue() );
                }
            }
            // delete last element's/Composite's fields
            String lastPrefix = fieldname.arrayElement( size()-1 ).get();
            Iterator<Entry<String,Object>> it = state.iterator();
            while (it.hasNext()) {
                Entry<String,Object> entry = it.next();
                if (entry.getKey().startsWith( lastPrefix )) {
                    it.remove();
                }
            }
            // adjust size field
            state.put( fieldname.arraySize().get(), size() - 1 );            
        }
        
        @Override
        public boolean add( Object o ) {
            state.put( fieldname.arrayElement( size() ).get(), o );
            state.put( fieldname.arraySize().get(), size() + 1 );
            return true;
        }

    }

    
    /**
     * 
     */
    protected class CompositeCollectionPropertyImpl
            extends CollectionPropertyImpl {

        protected CompositeCollectionPropertyImpl( PropertyInfo info, FieldnameBuilder parentname ) {
            super( info, parentname );
        }

        @Override
        public CompositeState createValue( Class actualType ) {
            FieldnameBuilder elmBasename = fieldname.arrayElement( size() );
            RecordCompositeState result = new RecordCompositeState( state, elmBasename );
            state.put( fieldname.arraySize().get(), size() + 1 );
            //
            if (!info.getType().getName().equals( actualType.getName() )) {
                state.put( elmBasename.composite( TYPE_KEY ).get(), actualType.getName() );
            }
            return result;
        }

        @Override
        public Iterator iterator() {
            return new Iterator() {
                int size = size();
                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < size;
                }

                @Override
                public Object next() {
                    assert Composite.class.isAssignableFrom( info().getType() );
                    return new RecordCompositeState( state, fieldname.arrayElement( index++ ) );
                }

                @Override
                public void remove() {
                    CompositeCollectionPropertyImpl.this.remove( index );
                }
            };
        }
        
    }
    
}
