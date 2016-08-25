/* 
 * polymap.org
 * Copyright (C) 2016, the @authors. All rights reserved.
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
package org.polymap.model2.engine;

import org.polymap.model2.PropertyBase;

/**
 * Base of all interceptor implementations. Interceptors add behaviour to a
 * {@link #delegate()}.
 * <p/>
 * Note: It should be named "decorator" but its one of the first concepts and code of
 * Model2 and so I stay with it. :)
 *
 * @author Falko Bräutigam
 */
public interface PropertyInterceptorBase<T>
        extends PropertyBase<T> {

    @FunctionalInterface
    public static interface Visitor<E extends Exception> {
        /**
         * Visit the given property.
         * @return False stops execution.
         */
        public boolean visit( PropertyBase prop ) throws E;
    }
    
    
    /**
     * Visit all {@link PropertyInterceptorBase#delegate()}s in the delegation chain
     * of the given property, including this start property.
     * 
     * @return True if all delegates were visited (execution was not stopped by vsitor).
     */
    public static <E extends Exception> boolean visitDelegates( PropertyBase prop, Visitor<E> visitor ) throws E {
        assert prop != null;
        while (prop != null && prop instanceof PropertyInterceptorBase) {
            boolean success = visitor.visit( prop );
            if (!success) {
                return false;
            }
            prop = ((PropertyInterceptorBase)prop).delegate();
        }
        visitor.visit( prop );
        return true;
    }

    
    /**
     * Finds the root of the delegation chain.
     */
    public static PropertyBase rootDelegate( PropertyInterceptorBase prop ) {
        assert prop != null;
        PropertyBase result = prop;
        while (result != null && result instanceof PropertyInterceptorBase) {
            result = ((PropertyInterceptorBase)result).delegate();
        }
        return result;
    }
    
    
    // interface ******************************************
    
    public PropertyBase<T> delegate();

}
