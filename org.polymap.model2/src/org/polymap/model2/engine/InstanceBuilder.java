/* 
 * polymap.org
 * Copyright (C) 2012-2016, Falko Bräutigam. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Association;
import org.polymap.model2.AssociationConcern;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.CollectionPropertyConcern;
import org.polymap.model2.Composite;
import org.polymap.model2.Computed;
import org.polymap.model2.ComputedPropertyBase;
import org.polymap.model2.Concerns;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.engine.EntityRepositoryImpl.EntityRuntimeContextImpl;
import org.polymap.model2.engine.cache.LoadingCache;
import org.polymap.model2.engine.cache.LoadingCache.Loader;
import org.polymap.model2.engine.cache.SimpleCacheManager;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreCollectionProperty;
import org.polymap.model2.store.StoreProperty;

/**
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public final class InstanceBuilder {

    private static Log log = LogFactory.getLog( InstanceBuilder.class );

    protected static Field                      contextField;
    
    private static Field                        concernContextField;

    private static Field                        concernDelegateField;
    
    private static Method                       computedPropertyInitMethod;
    
    protected static LoadingCache<Field,List<Class>>   concerns;

    static {
        try {
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            MutableConfiguration cacheConfig = new MutableConfiguration()
                    .setExpiryPolicyFactory( AccessedExpiryPolicy.factoryOf( Duration.ONE_MINUTE ) );
            concerns = LoadingCache.create( cacheManager, cacheConfig );
                    
            contextField = Composite.class.getDeclaredField( "context" );
            contextField.setAccessible( true );

            concernContextField = PropertyConcernBase.class.getDeclaredField( "context" );
            concernContextField.setAccessible( true );
            
            concernDelegateField = PropertyConcernBase.class.getDeclaredField( "delegate" );
            concernDelegateField.setAccessible( true );
            
            computedPropertyInitMethod = ComputedPropertyBase.class.getDeclaredMethod( "init", PropertyInfo.class, Composite.class );
            computedPropertyInitMethod.setAccessible( true );
        }
        catch (Exception e) {
            log.error( "", e );
            throw new RuntimeException( e );
        }
    }

    
    /**
     * Engine internal method that exposes the context of the given entity.
     */
    protected static EntityRuntimeContextImpl contextOf( Entity entity ) {
        assert entity != null;
        try {
            return (EntityRuntimeContextImpl)contextField.get( entity );
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ModelRuntimeException( e );
        }
    }
    

    
    // instance *******************************************
    
    private EntityRuntimeContext    context;
    
    
    public InstanceBuilder( EntityRuntimeContext context ) {
        this.context = context;
    }
    
    
    public <T extends Composite> T newComposite( CompositeState state, Class<T> entityClass ) { 
        try {
            // new instance
            Constructor<?> ctor = entityClass.getConstructor( ArrayUtils.EMPTY_CLASS_ARRAY );
            T instance = (T)ctor.newInstance( ArrayUtils.EMPTY_OBJECT_ARRAY );
            
            // set context
            contextField.set( instance, context );
            
//            // init concerns
//            List<PropertyConcern> concerns = new ArrayList();
//            Concerns concernsAnnotation = entityClass.getAnnotation( Concerns.class );
//            if (concernsAnnotation != null) {
//                for (Class<? extends PropertyConcern> concernClass : concernsAnnotation.value()) {
//                    concerns.add( concernClass.newInstance() );
//                }
//            }

            // init properties
            initProperties( instance, state );
            
            return instance;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ModelRuntimeException( "Error while creating an instance of: " + entityClass, e );
        }
    }
    
    
    /**
     * Initializes all properties of the given Composite, including all super classes.
     * Composite properties are init with {@link CompositePropertyImpl} which comes back to 
     * {@link InstanceBuilder} when the value is accessed.
     */
    protected void initProperties( Composite instance, CompositeState state ) throws Exception {
        CompositeInfo compositeInfo = context.getRepository().infoOf( instance.getClass() );
        if (compositeInfo == null) {
            log.info( "Mixin type not declared on Entity type: " + instance.getClass().getName() );
            compositeInfo = new CompositeInfoImpl( instance.getClass() );
        }
        assert compositeInfo != null : "No info for Composite type: " + instance.getClass().getName();
        
        Class superClass = instance.getClass();
        while (superClass != null) {
            // XXX cache fields
            for (Field field : superClass.getDeclaredFields()) {
                if (PropertyBase.class.isAssignableFrom( field.getType() )) {
                    field.setAccessible( true );
                    
                    if (field.get( instance ) != null) {
                        log.info( "Property already inistialized, skipping: " + field.getName() );
                        continue;
                    }

                    PropertyInfo info = compositeInfo.getProperty( field.getName() );
                    assert info != null : "No property info for field: " + superClass.getSimpleName() + "." + field.getName() + " ! - Entity type correctly declared in EntityRepository?";
                    PropertyBase prop = null;

                    // Property
                    if (Property.class.isAssignableFrom( field.getType() )) {
                        // Computed
                        if (info.isComputed()) {
                            Computed a = ((PropertyInfoImpl)info).getField().getAnnotation( Computed.class );
                            prop = a.value().newInstance();
                            computedPropertyInitMethod.invoke( prop, info, instance );
                        }
                        // Composite or primitive
                        else {
                            StoreProperty storeProp = state.loadProperty( info );
                            prop = Composite.class.isAssignableFrom( info.getType() )
                                    ? new CompositePropertyImpl( context, storeProp )
                                    : new PropertyImpl( storeProp );
                        }
                        // always check modifications, default value, immutable, nullable
                        prop = new ConstraintsPropertyInterceptor( (Property)prop, (EntityRuntimeContextImpl)context );
                        prop = fieldConcerns( field, prop );
                    }

                    // Association
                    else if (Association.class.isAssignableFrom( field.getType() )) {
                        assert info.isAssociation();
                        // Computed
                        if (info.isComputed()) {
                            Computed a = ((PropertyInfoImpl)info).getField().getAnnotation( Computed.class );
                            prop = a.value().newInstance();
                            computedPropertyInitMethod.invoke( prop, info, instance );
                        }
                        //
                        else {
                            StoreProperty storeProp = state.loadProperty( info );
                            prop = new AssociationImpl( context, storeProp );
                        }
                        // always check modifications, default value, immutable, nullable
                        prop = new ConstraintsAssociationInterceptor( (Association)prop, (EntityRuntimeContextImpl)context );
                        prop = fieldConcerns( field, prop );
                    }

                    // ManyAssociation
                    else if (ManyAssociation.class.isAssignableFrom( field.getType() )) {
                        assert info.isAssociation();
                        assert info.getMaxOccurs() > 1 : "Field has improper @MaxOccurs: " + propName( field );
                        // check Computed
                        if (info.isComputed()) {
                            Computed a = ((PropertyInfoImpl)info).getField().getAnnotation( Computed.class );
                            prop = a.value().newInstance();
                            computedPropertyInitMethod.invoke( prop, info, instance );
                        }
                        else {
                            StoreCollectionProperty storeProp = (StoreCollectionProperty)state.loadProperty( info );
                            prop = new ManyAssociationImpl( context, storeProp );
                        }
                        prop = new ConstraintsManyAssociationInterceptor( (ManyAssociation)prop, (EntityRuntimeContextImpl)context );
                        prop = fieldConcerns( field, prop );
                    }

                    // Collection
                    else if (CollectionProperty.class.isAssignableFrom( field.getType() )) {
                        assert info.getMaxOccurs() > 1 : "Field has improper @MaxOccurs: " + propName( field );
                        StoreCollectionProperty storeProp = (StoreCollectionProperty)state.loadProperty( info );
                        // Computed
                        if (info.isComputed()) {
                            throw new UnsupportedOperationException( "Computed CollectionProperty is not supported yet: " + propName( field ));
                        }
                        // Composite
                        else if (Composite.class.isAssignableFrom( info.getType() )) {
                            prop = new CompositeCollectionPropertyImpl( context, storeProp );                            
                        }
                        // primitive type
                        else {
                            prop = new CollectionPropertyImpl( context, storeProp );
                        }
                        if (info.isNullable()) {
                            throw new ModelRuntimeException( "CollectionProperty cannot be @Nullable." );
                        }
                        prop = new ConstraintsCollectionInterceptor( (CollectionProperty)prop, (EntityRuntimeContextImpl)context );
                        // concerns
                        prop = fieldConcerns( field, prop );
                    }

                    // set field
                    assert prop != null : "Unable to build property instance for: " + field;
                    field.set( instance, prop );                    
                }
            }
            superClass = superClass.getSuperclass();
        }
    }


    protected String propName( Field field ) {
        return field.getDeclaringClass().getSimpleName() + "#" + field.getName();
    }

    
    protected PropertyBase fieldConcerns( final Field field, final PropertyBase prop ) throws Exception {
        List<Class> concernTypes = concerns.get( field, new Loader<Field,List<Class>>() {
            public List<Class> load( Field key ) {
                List<Class> result = new ArrayList();
                // Class concerns
                Concerns ca = field.getDeclaringClass().getAnnotation( Concerns.class );
                if (ca != null) {
                    result.addAll( Arrays.asList( ca.value() ) );
                }
                // Field concerns
                Concerns fa = field.getAnnotation( Concerns.class );
                if (fa != null) {
                    result.addAll( Arrays.asList( fa.value() ) );
                }
                return result;
            }
        });
        
        PropertyBase result = prop;
        for (Class concernType : concernTypes) {
            try {
                // early check concern type
                if (Property.class.isAssignableFrom( field.getType() )
                        && !PropertyConcern.class.isAssignableFrom( concernType )) {
                    throw new ModelRuntimeException( "Concerns of Property have to extend PropertyConcern: " + concernType.getName() + " @ " + field.getName() );
                }
                else if (CollectionProperty.class.isAssignableFrom( field.getType() )
                        && !CollectionPropertyConcern.class.isAssignableFrom( concernType )) {
                    throw new ModelRuntimeException( "Concerns of CollectionProperty have to extend CollectionPropertyConcern: " + concernType.getName() + " @ " + field.getName() );
                }
                else if (Association.class.isAssignableFrom( field.getType() )
                        && !AssociationConcern.class.isAssignableFrom( concernType )) {
                    throw new ModelRuntimeException( "Concerns of Association have to extend AssociationConcern: " + concernType.getName() + " @ " + field.getName() );
                }

                // create concern
                PropertyConcernBase concern = (PropertyConcernBase)concernType.newInstance();
                concernContextField.set( concern, context );
                concernDelegateField.set( concern, result );

                result = concern;
            } 
            catch (ModelRuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new ModelRuntimeException( "Error while initializing concern: " + concernType + " (" + e.getLocalizedMessage() + ")", e );
            }
        }
        return result;
    }
    
}
