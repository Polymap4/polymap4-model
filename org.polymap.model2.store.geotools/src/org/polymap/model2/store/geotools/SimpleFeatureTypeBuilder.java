/* 
 * polymap.org
 * Copyright (C) 2012-2014, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.store.geotools;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;import org.apache.commons.logging.Log;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.ComplexType;

import org.polymap.model2.Entity;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class SimpleFeatureTypeBuilder
        extends FeatureTypeBuilder {

    private static Log log = LogFactory.getLog( SimpleFeatureTypeBuilder.class );
    
    
    public SimpleFeatureTypeBuilder( Class<? extends Entity> entityClass) throws Exception {
        super( entityClass );
    }


    public SimpleFeatureType build() throws Exception {
        log.debug( "Entity: " + entityClass );
        ComplexType complexType = buildComplexType( entityClass, "    " );
        
        List<AttributeDescriptor> attributes = new ArrayList( 128 );
        GeometryDescriptor geom = null;
        for (PropertyDescriptor prop : complexType.getDescriptors()) {
            if (prop instanceof GeometryDescriptor) {
                geom = (GeometryDescriptor)prop;
            }
            if (prop instanceof AttributeDescriptor) {
                attributes.add( (AttributeDescriptor)prop );
            }
        }
        
        return factory.createSimpleFeatureType( complexType.getName(), attributes,
                geom, false, complexType.getRestrictions(),
                complexType.getSuper(), complexType.getDescription() );
    }
    
}