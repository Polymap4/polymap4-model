/* 
 * polymap.org
 * Copyright (C) 2012-2013, Falko Bräutigam. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import org.polymap.model2.Composite;
import org.polymap.model2.Description;
import org.polymap.model2.Immutable;
import org.polymap.model2.Mixins;
import org.polymap.model2.NameInStore;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;

import areca.common.Assert;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.FieldInfo;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public final class CompositeInfoImpl<T extends Composite>
        implements CompositeInfo<T> {

    private ClassInfo<T>                    compositeClassInfo;
    
    /** Maps property name into PropertyInfo. */
    private Map<String,PropertyInfo<?>>     propertyInfos = new HashMap<>();
    
    
    public CompositeInfoImpl( ClassInfo<T> compositeClassInfo ) {
        this.compositeClassInfo = Assert.notNull( compositeClassInfo );
        try {
            initPropertyInfos();
        }
        catch (Exception e) {
            throw new ModelRuntimeException( e );
        }
    }

    /**
     * Recursivly init {@link #propertyInfos} of the given instance and all complex
     * propertyInfos.
     */
    protected void initPropertyInfos() throws Exception {
        for (FieldInfo field : compositeClassInfo.fields()) {
            if (PropertyBase.class.isAssignableFrom( field.type() )) {
                PropertyInfoImpl<?> info = new PropertyInfoImpl<>( field );
                propertyInfos.put( info.getName(), info );
            }
        }
    }

    @Override
    public String getName() {
        return StringUtils.substringAfterLast( compositeClassInfo.name(), "." );
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(
                compositeClassInfo.annotation( Description.class ).transform( a -> a.value() ).orElse( null ) );
    }

    @Override
    public String getNameInStore() {
        System.out.println( ":: " ); // + compositeClassInfo );
        return compositeClassInfo.annotation( NameInStore.class ).transform( a -> a.value() ).orElse( getName() );
    }

    @Override
    public Class<T> getType() {
        return compositeClassInfo.type();
    }

//    @Override
//    public Composite getTemplate() {
//        return template.get( new Supplier<Composite>() {
//            public Composite get() {
//                CompositeState templateState = new CompositeState() {
//                    @Override
//                    public Object id() {
//                        throw new UnsupportedOperationException( "Method id() is nout allowed for Composite templates." );
//                    }
//                    @Override
//                    public StoreProperty loadProperty( PropertyInfo info ) {
//                        throw new RuntimeException( "Method loadProperty() is nout allowed for Composite templates." );
//                    }
//                    @Override
//                    public Object getUnderlying() {
//                        throw new RuntimeException( "Method getUnderlying() is nout allowed for Composite templates." );
//                    }
//                };
//                EntityRuntimeContextImpl entityContext = new EntityRuntimeContextImpl( 
//                        templateState, EntityStatus.CREATED );
//                InstanceBuilder builder = new InstanceBuilder( entityContext );
//                return builder.newComposite( templateState, compositeClassInfo );
//
//            }
//        });
//    }

    @Override
    public Collection<Class<? extends Composite>> getMixins() {
        return compositeClassInfo.annotation( Mixins.class )
                .transform( a -> Arrays.asList( a.value() ) )
                .orElse( Collections.EMPTY_LIST );
    }

    @Override
    public Collection<PropertyInfo<?>> getProperties() {
        return Collections.unmodifiableCollection( propertyInfos.values() );
    }

    @Override
    public PropertyInfo<?> getProperty( String name ) {
        return propertyInfos.get( name );
    }

    @Override
    public boolean isImmutable() {
        return compositeClassInfo.annotation( Immutable.class ).isPresent();
    }

}
