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
import java.util.logging.Logger;

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
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreCollectionProperty;
import org.polymap.model2.store.StoreProperty;

import areca.common.Assert;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.FieldInfo;

/**
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public final class InstanceBuilder {

    private static final Logger LOG = Logger.getLogger( InstanceBuilder.class.getName() );

//    private static FieldInfo                        concernContextField;
//
//    private static FieldInfo                        concernDelegateField;
//    
//
//    static {
//        try {
//            concernContextField = PropertyConcernBase.class.getDeclaredField( "context" );
//            concernContextField.setAccessible( true );
//            
//            concernDelegateField = PropertyConcernBase.class.getDeclaredField( "delegate" );
//            concernDelegateField.setAccessible( true );
//        }
//        catch (Exception e) {
//            LOG.error( "", e );
//            throw new RuntimeException( e );
//        }
//    }

    
    /**
     * Engine internal method that exposes the context of the given entity.
     */
    protected static EntityRuntimeContextImpl contextOf( Entity entity ) {
        return (EntityRuntimeContextImpl)entity.context;
//        try {
//            FieldInfo contextField = ClassInfo.of( entity )
//                    .fields().stream().filter( f -> f.name().equals( "context" ) ).findAny().get();
//            return (EntityRuntimeContextImpl)contextField.get( entity );
//        }
//        catch (RuntimeException e) {
//            throw e;
//        }
//        catch (Exception e) {
//            throw new ModelRuntimeException( e );
//        }
    }
    

    
    // instance *******************************************
    
    private EntityRuntimeContext    context;
    
    
    public InstanceBuilder( EntityRuntimeContext context ) {
        this.context = context;
    }
    
    
    public <T extends Composite> T newComposite( CompositeState state, Class<T> entityClass ) {
        LOG.info( "newComposite(): " );
        try {
            ClassInfo<T> entityClassInfo = ClassInfo.of( entityClass );
            
            // new instance
            T instance = entityClassInfo.newInstance();
            
            // set context
            // XXX
//            FieldInfo contextField = entityClassInfo.fields().stream().filter( f -> f.name().equals( "context" ) ).findAny().get();
//            LOG.info( "newComposite(): " + contextField );
//            contextField.set( instance, context );
            instance.context = context;
            
//            // init concerns
//            List<PropertyConcern> concerns = new ArrayList();
//            Concerns concernsAnnotation = entityClass.getAnnotation( Concerns.class );
//            if (concernsAnnotation != null) {
//                for (Class<? extends PropertyConcern> concernClass : concernsAnnotation.value()) {
//                    concerns.add( concernClass.newInstance() );
//                }
//            }

            // init properties
            initProperties( instance, entityClassInfo, state );
            
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
    protected <T extends Composite> void initProperties( T instance, ClassInfo<T> classInfo, CompositeState state ) throws Exception {
        CompositeInfo<T> compositeInfo = context.getRepository().infoOf( classInfo  );
        if (compositeInfo == null) {
            LOG.info( "Mixin type not declared on Entity type: " + instance.getClass().getName() );
            compositeInfo = new CompositeInfoImpl<>( classInfo );
        }
        assert compositeInfo != null : "No info for Composite type: " + instance.getClass().getName();
        
        // XXX cache fields
        for (FieldInfo field : classInfo.fields()) {
            if (PropertyBase.class.isAssignableFrom( field.type() )) {

                if (field.get( instance ) != null) {
                    LOG.info( "Property already inistialized, skipping: " + field.name() );
                    continue;
                }

                PropertyInfo<?> info = compositeInfo.getProperty( field.name() );
                Assert.notNull( info, "No property info for field: " + classInfo.name() + "." + field.name() + " ! - Entity type correctly declared in EntityRepository?" );
                PropertyBase<?> prop = null;

                // Property
                if (Property.class.isAssignableFrom( field.type() )) {
                    // Computed
                    if (info.isComputed()) {
                        Computed a = ((PropertyInfoImpl<?>)info).getField().annotation( Computed.class ).get();
                        prop = ClassInfo.of( a.value() ).newInstance();
                        ((ComputedPropertyBase)prop).init( info, instance );
                    }
                    // Composite or primitive
                    else {
                        StoreProperty<?> storeProp = state.loadProperty( info );
                        prop = Composite.class.isAssignableFrom( info.getType() )
                                ? new CompositePropertyImpl( context, storeProp )
                                : new PropertyImpl<>( storeProp );
                    }
                    // always check modifications, default value, immutable, nullable
                    prop = new ConstraintsPropertyInterceptor<>( (Property<?>)prop, (EntityRuntimeContextImpl)context );
                    prop = fieldConcerns( field, prop );
                }

                // Association
                else if (Association.class.isAssignableFrom( field.type() )) {
                    Assert.that( info.isAssociation() );
                    // Computed
                    if (info.isComputed()) {
                        Computed a = ((PropertyInfoImpl<?>)info).getField().annotation( Computed.class ).get();
                        prop = ClassInfo.of( a.value() ).newInstance();
                        ((ComputedPropertyBase)prop).init( info, instance );
                    }
                    //
                    else {
                        StoreProperty<?> storeProp = state.loadProperty( info );
                        prop = new AssociationImpl<>( context, storeProp );
                    }
                    // always check modifications, default value, immutable, nullable
                    prop = new ConstraintsAssociationInterceptor<>( (Association<?>)prop, (EntityRuntimeContextImpl)context );
                    prop = fieldConcerns( field, prop );
                }

                // ManyAssociation
                else if (ManyAssociation.class.isAssignableFrom( field.type() )) {
                    Assert.that( info.isAssociation() );
                    Assert.that( info.getMaxOccurs() > 1, "Field has improper @MaxOccurs: " + propName( field ) );
                    // check Computed
                    if (info.isComputed()) {
                        Computed a = ((PropertyInfoImpl<?>)info).getField().annotation( Computed.class ).get();
                        prop = ClassInfo.of( a.value() ).newInstance();
                        ((ComputedPropertyBase)prop).init( info, instance );
                    }
                    else {
                        StoreCollectionProperty<?> storeProp = (StoreCollectionProperty<?>)state.loadProperty( info );
                        prop = new ManyAssociationImpl<>( context, storeProp );
                    }
                    prop = new ConstraintsManyAssociationInterceptor<>( (ManyAssociation<?>)prop, (EntityRuntimeContextImpl)context );
                    prop = fieldConcerns( field, prop );
                }

                // Collection
                else if (CollectionProperty.class.isAssignableFrom( field.type() )) {
                    Assert.that( info.getMaxOccurs() > 1, "Field has improper @MaxOccurs: " + propName( field ) );
                    StoreCollectionProperty<?> storeProp = (StoreCollectionProperty<?>)state.loadProperty( info );
                    // Computed
                    if (info.isComputed()) {
                        throw new UnsupportedOperationException( "Computed CollectionProperty is not supported yet: " + propName( field ));
                    }
                    // Composite
                    else if (Composite.class.isAssignableFrom( info.getType() )) {
                        prop = new CompositeCollectionPropertyImpl<>( context, storeProp );                            
                    }
                    // primitive type
                    else {
                        prop = new CollectionPropertyImpl<>( context, storeProp );
                    }
                    if (info.isNullable()) {
                        throw new ModelRuntimeException( "CollectionProperty cannot be @Nullable." );
                    }
                    prop = new ConstraintsCollectionInterceptor<>( (CollectionProperty<?>)prop, (EntityRuntimeContextImpl)context );
                    // concerns
                    prop = fieldConcerns( field, prop );
                }

                // set field
                assert prop != null : "Unable to build property instance for: " + field;
                field.set( instance, prop );                    
            }
        }
    }


    protected String propName( FieldInfo field ) {
        return field.declaringClass().getSimpleName() + "#" + field.name();
    }

    
    protected PropertyBase<?> fieldConcerns( final FieldInfo field, final PropertyBase<?> prop ) throws Exception {
        List<Class<?>> concernTypes = new ArrayList<>();
        // Class concerns
        field.declaringClassInfo().annotation( Concerns.class ).ifPresent( a -> {
            concernTypes.addAll( Arrays.asList( a.value() ) );
        });
        // Field concerns
        field.annotation( Concerns.class ).ifPresent( a -> {
            concernTypes.addAll( Arrays.asList( a.value() ) );
        });
        
        PropertyBase result = prop;
        for (Class<?> concernType : concernTypes) {
            try {
                // early check concern type
                if (Property.class.isAssignableFrom( field.type() )
                        && !PropertyConcern.class.isAssignableFrom( concernType )) {
                    throw new ModelRuntimeException( "Concerns of Property have to extend PropertyConcern: " + concernType.getName() + " @ " + field.name() );
                }
                else if (CollectionProperty.class.isAssignableFrom( field.type() )
                        && !CollectionPropertyConcern.class.isAssignableFrom( concernType )) {
                    throw new ModelRuntimeException( "Concerns of CollectionProperty have to extend CollectionPropertyConcern: " + concernType.getName() + " @ " + field.name() );
                }
                else if (Association.class.isAssignableFrom( field.type() )
                        && !AssociationConcern.class.isAssignableFrom( concernType )) {
                    throw new ModelRuntimeException( "Concerns of Association have to extend AssociationConcern: " + concernType.getName() + " @ " + field.name() );
                }

                // create concern
                PropertyConcernBase<?> concern = (PropertyConcernBase<?>)ClassInfo.of( concernType ).newInstance();
                concern.context = context;
                concern.delegate = result;

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
