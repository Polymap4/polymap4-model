/* 
 * polymap.org
 * Copyright 2012, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.engine;

import java.util.Date;

import org.polymap.model2.DefaultValue;
import org.polymap.model2.Defaults;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Provides runtime support for property default values. 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class DefaultValues {

    private static Log log = LogFactory.getLog( DefaultValues.class );

    public static final String      DEFAULT_STRING = "";
    public static final Integer     DEFAULT_INTEGER = Integer.valueOf( 0 );
    public static final Long        DEFAULT_LONG = Long.valueOf( 0 );
    public static final Date        DEFAULT_DATE = new Date( 0 );
    public static final Boolean     DEFAULT_BOOLEAN = Boolean.FALSE;
    public static final Float       DEFAULT_FLOAT = Float.valueOf( 0f );
    public static final Double      DEFAULT_DOUBLE = Double.valueOf( 0d );

    /**
     * Creates a default value for the given field. The default value can be defined
     * via the {@link DefaultValue} annotation. 
     * 
     * @param propInfo
     * @return The default value for the given field, or null if no default value was
     *         defined via {@link DefaultValue} annotation
     */
    public static Object valueOf( PropertyInfoImpl propInfo ) {
        Class<?> type = propInfo.getType();
        
        // @DefaultValue
        DefaultValue defaultValue = (DefaultValue)propInfo.getAnnotation( DefaultValue.class );
        if (defaultValue != null) {
            if (type.equals( String.class )) {
                return defaultValue.value();
            }
            else if (type.equals( Integer.class )) {
                return Integer.parseInt( defaultValue.value() );
            }
            else if (type.equals( Float.class )) {
                return Float.parseFloat( defaultValue.value() );
            }
            else if (type.equals( Double.class )) {
                return Double.parseDouble( defaultValue.value() );
            }
            else if (type.equals( Boolean.class )) {
                return Boolean.parseBoolean( defaultValue.value() );
            }
            // XXX
            else {
                throw new UnsupportedOperationException( "Default values of this type are not supported yet: " + type );
            }
        }
        
        // @Defaults
        Defaults defaults = (Defaults)propInfo.getAnnotation( Defaults.class );
        if (defaults != null) {
            if (type.equals( String.class )) {
                return DEFAULT_STRING;
            }
            else if (type.equals( Integer.class )) {
                return DEFAULT_INTEGER;
            }
            else if (type.equals( Long.class )) {
                return DEFAULT_LONG;
            }
            else if (type.equals( Date.class )) {
                return DEFAULT_DATE;
            }
            else if (type.equals( Boolean.class )) {
                return DEFAULT_BOOLEAN;
            }
            else if (type.equals( Float.class )) {
                return DEFAULT_FLOAT;
            }
            else if (type.equals( Double.class )) {
                return DEFAULT_DOUBLE;
            }
            // XXX
            else {
                throw new UnsupportedOperationException( "Default values of this type are not supported yet: " + type );
            }
        }
        return null;
    }
    
}
