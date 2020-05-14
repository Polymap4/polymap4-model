/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.query.grammar;

import org.polymap.model2.Composite;
import org.polymap.model2.engine.TemplateProperty;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PropertyMatches<T>
        extends ComparisonPredicate<T> {

    protected char  singleWildcard = '?';

    protected char  multiWildcard = '*';
    
    public PropertyMatches( TemplateProperty<T> prop, T value, char singleWildcard, char multiWildcard ) {
        super( prop, value );
    }

    public PropertyMatches( TemplateProperty<T> prop, T value ) {
        super( prop, value );
    }

    @Override
    public boolean evaluate( Composite target ) {
        throw new UnsupportedOperationException( "wildcard match not supported" );
        
        // removed to get rid of commons.io
//        Object propValue = propValue( target, prop );
//        return propValue != null 
//                ? FilenameUtils.wildcardMatch( propValue.toString(), value.toString() )
//                : false;
    }
    
    protected String name() {
        return "~=";
    }

}
