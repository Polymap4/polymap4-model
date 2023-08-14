/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.runtime.config;

import areca.common.reflect.ClassInfo;
import areca.common.reflect.FieldInfo;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class ConfigurationFactory {

    /**
     * Creates a new configuration of the given type.
     */
    public static <T> T create( ClassInfo<T> cl ) throws ConfigurationException {
        try {
            // create instance
            T instance = cl.newInstance();
            
            // init properties
            for (FieldInfo f : cl.fields()) {
                // TODO static fields
                if (f.name().equals( "info" )) {
                    continue;
                }
                Property prop = new Property() {

                    private Object      value;
                    
                    @Override
                    public Object set( Object newValue ) {
                        this.value = newValue;
                        return instance;
                    }

                    @Override
                    public Object setAndGet( Object newValue ) {
                        throw new RuntimeException( "not yet implemented." );
                    }

                    @Override
                    public Object get() {
                        if (value == null && f.annotation( Mandatory.class ).isPresent()) {
                            throw new ConfigurationException( "Configuration property is @Mandatory: " + f.name() );
                        }
                        DefaultValue defaultValue = f.annotation( DefaultValue.class ).orNull();
                        if (value == null && defaultValue != null) {
                            return defaultValue.value();
                        }
                        DefaultDouble defaultDouble = f.annotation( DefaultDouble.class ).orNull();
                        if (value == null && defaultDouble != null) {
                            return defaultDouble.value();
                        }
                        DefaultInt defaultInt = f.annotation( DefaultInt.class ).orNull();
                        if (value == null && defaultInt != null) {
                            return defaultInt.value();
                        }
                        DefaultBoolean defaultBoolean = f.annotation( DefaultBoolean.class ).orNull();
                        if (value == null && defaultBoolean != null) {
                            return defaultBoolean.value();
                        }
                        return value;
                    }
                };
                f.set( instance, prop );
            }
            return instance;
        }
        catch (Exception e) {
            throw new ConfigurationException( e );
        }
    }
    
}
