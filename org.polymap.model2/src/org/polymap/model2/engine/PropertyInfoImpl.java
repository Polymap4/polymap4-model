/* 
 * polymap.org
 * Copyright (C) 2012-2015, Falko Bräutigam. All rights reserved.
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

import java.util.Optional;

import java.lang.annotation.Annotation;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Computed;
import org.polymap.model2.Description;
import org.polymap.model2.Immutable;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.MaxOccurs;
import org.polymap.model2.NameInStore;
import org.polymap.model2.Nullable;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.Queryable;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.ValueInitializer;

import areca.common.Assert;
import areca.common.reflect.FieldInfo;
import areca.common.reflect.GenericType;
import areca.common.reflect.GenericType.ParameterizedType;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PropertyInfoImpl<T>
        implements PropertyInfo<T> {

    private FieldInfo           field;

    
    public PropertyInfoImpl( FieldInfo field ) {
        Assert.that( PropertyBase.class.isAssignableFrom( field.type() ) );
        this.field = field;
    }

    FieldInfo getField() {
        return field;
    }

    @Override
    public Class<T> getType() {
        Optional<Class<T>> opt = ValueInitializer.rawTypeParameter( field.genericType() );
        return opt.orElseThrow( () -> new ModelRuntimeException( "Type param missing: " + toString() ) );
    }
    
    @Override
    public Optional<ParameterizedType> getParameterizedType() {
        ParameterizedType declaredType = (ParameterizedType)field.genericType();
        GenericType result = declaredType.getActualTypeArguments()[0];
        return result instanceof ParameterizedType 
                ? Optional.of( (ParameterizedType)result ) : Optional.empty();
    }
    
    @Override
    public String getName() {
        return field.name();
    }

    @Override
    public Optional<String> getDescription() {
        Description a = getAnnotation( Description.class );
        return a != null ? Optional.of( a.value() ) : Optional.empty();
    }

    @Override
    public String getNameInStore() {
        return field.annotation( NameInStore.class ).transform( a -> a.value() ).orElse( field.name() );
    }

    @Override
    public boolean isAssociation() {
        return Association.class.isAssignableFrom( field.type() )
                || ManyAssociation.class.isAssignableFrom( field.type() );
    }

    @Override
    public boolean isNullable() {
        return field.annotation( Nullable.class ).isPresent();
    }

    @Override
    public boolean isImmutable() {
        return field.annotation( Immutable.class ).isPresent();
    }

    @Override
    public boolean isComputed() {
        return field.annotation( Computed.class ).isPresent();
    }

    @Override
    public boolean isQueryable() {
        return field.annotation( Queryable.class ).isPresent();
    }

    @Override
    public int getMaxOccurs() {
        if (CollectionProperty.class.isAssignableFrom( field.type() )
                || ManyAssociation.class.isAssignableFrom( field.type() )) {
            return field.annotation( MaxOccurs.class ).transform( a -> a.value() ).orElse( Integer.MAX_VALUE );
        }
        else {
            Assert.that( field.annotation( MaxOccurs.class ).isAbsent(), "@MaxOccurs is not allowed on single value properties." );
            return 1;
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public T getDefaultValue() {
        return (T)DefaultValues.valueOf( this );
    }

    @Override    
    @SuppressWarnings( "unchecked" )
    public <P extends PropertyBase<T>> P get( Composite composite ) {
        return (P)field.get( composite );
    }

    @Override
    public <A extends Annotation> A getAnnotation( Class<A> type ) {
        return field.annotation( type ).orElse( null );
    }

    @Override
    public String toString() {
        return "PropertyInfoImpl[" + field + "]";
    }
    
}
