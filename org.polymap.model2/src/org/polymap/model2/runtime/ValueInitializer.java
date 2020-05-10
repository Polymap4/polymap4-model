/* 
 * polymap.org
 * Copyright 2012-2018, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.runtime;

import java.util.Optional;

import areca.common.Assert;
import areca.common.reflect.GenericType;
import areca.common.reflect.GenericType.ClassType;
import areca.common.reflect.GenericType.ParameterizedType;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@FunctionalInterface
public interface ValueInitializer<T> {

//    /**
//     * XXX Lambda declaration does not seem to deliver the type parameter, a fallback
//     * needs to be given for this case.
//     */
//    public default Optional<Class<T>> rawResultType() {
//        return rawTypeParameter( getClass() );
//    }

    
    public default ValueInitializer<T> and( ValueInitializer<T> other ) {
        ValueInitializer<T> self = this;
        return new ValueInitializer<T>() {
            @Override
            public T initialize( T prototype ) throws Exception {
                prototype = self.initialize( prototype );
                return other.initialize( prototype );
            }
        };
    }
    
    
    public abstract T initialize( T prototype ) throws Exception;


    /**
     * Returns the raw type (Class) of the first type argument of the given
     * <code>parameterized</code> type.
     *
     * @param cl The parameterized type (Class, interface or
     *        {@link ParameterizedType}).
     * @return {@link Optional#empty()} if the given <b>Class</b> has no type param
     *         (maybe lambda declaration).
     * @throws AssertionError If argument is not a parameterized.
     * @throws RuntimeException
     */
    public static <R> Optional<Class<R>> rawTypeParameter( GenericType genericType ) {
        ParameterizedType parameterized = null;
        if (genericType instanceof ParameterizedType) {
            parameterized = (ParameterizedType)genericType;
        }
        else if (genericType instanceof ClassType) {
            throw new UnsupportedOperationException( "Type parameter in superclass is not yet supported." );
//            // class
//            Type generic = ((Class)genericType).getGenericSuperclass();
//            // interface
//            Type[] genericInterfaces = ((Class)genericType).getGenericInterfaces();
//            if (generic.equals( Object.class )) {
//                assert genericInterfaces.length == 1;
//                generic = genericInterfaces[0];
//            }
//            if (!(generic instanceof ParameterizedType)) {
//                return Optional.empty();
//            }
//            parameterized = (ParameterizedType)generic;
        }
        else {
            throw new RuntimeException( "Unknown type: " + genericType );            
        }
        
        Assert.that( parameterized instanceof ParameterizedType, "Argument is no a ParameterizedType: " + parameterized );
        GenericType result = ((ParameterizedType)parameterized).getActualTypeArguments()[0];
        if (result instanceof ClassType) {
            return Optional.of( ((Class<R>)((ClassType)result).getRawType()) );
        }
        else if (result instanceof ParameterizedType) {
            return Optional.of( (Class<R>)((ParameterizedType)result).getRawType() );
        }
//        else if (result instanceof TypeVariable) {
//            // The type parameter is something like <T>. So the compiler does not
//            // know what it actually is. This was first used in Styling plugin. I'm
//            // not quite sure what to return. Object seems to be ok. The store backend
//            // stores the actual runtime class. Maybe lower bound of the TypeVariable?
//            return Optional.of( (Class<R>)Object.class );
//        }
        else {
            throw new RuntimeException( "Unknown type argument: " + result + " of type: " + genericType );
        }
    }

}
