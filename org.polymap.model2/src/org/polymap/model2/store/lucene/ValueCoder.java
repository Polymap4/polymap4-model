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

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;

import org.polymap.model2.runtime.PropertyInfo;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 *
 * @author Falko
 */
public class ValueCoder {

    private static final Log LOG = LogFactory.getLog( ValueCoder.class );


    /**
     * Encode the given value into a Lucene field. 
     */
    public static IndexableField encode( PropertyInfo<?> info, Object value ) {
        Assert.notNull( value );
        Assert.isType( info.getType(), value );

        // String
        if (value instanceof String) {
            return new StringField( info.getNameInStore(), (String)value, Store.YES );
        }
        // not handled
        else {
            throw new RuntimeException( "not yet implemented: " + info );            
        }
    }


    /**
     * 
     */
    public static Object decode( PropertyInfo<?> info, IndexableField field ) {
        var type = info.getType();
        
        // String
        if (type.isAssignableFrom( String.class )) {
            return ((StringField)field).stringValue();
        }
        // not handled
        else {
            throw new RuntimeException( "not yet implemented: " + info );            
        }
    }
}
