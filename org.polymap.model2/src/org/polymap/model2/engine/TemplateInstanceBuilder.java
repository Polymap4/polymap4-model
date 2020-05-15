/* 
 * polymap.org
 * Copyright (C) 2012-2015, Falko Bräutigam. All rights reserved.
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

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Assert;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.FieldInfo;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public final class TemplateInstanceBuilder {

    private static final Logger LOG = Logger.getLogger( TemplateInstanceBuilder.class.getName() );

    private EntityRepository        repo;

    private CompositeInfo<?>        compositeInfo;
    
    
    public TemplateInstanceBuilder( EntityRepository repo ) {
        this.repo = repo;
    }


    public <T extends Composite> T newComposite( ClassInfo<T> entityClassInfo ) { 
        try {
            // composite info
            compositeInfo = repo.infoOf( entityClassInfo );
            if (compositeInfo == null) {
                LOG.info( "Mixin type not declared on Entity type: " + entityClassInfo.name() );
                compositeInfo = new CompositeInfoImpl<>( entityClassInfo );
            }
            Assert.notNull( compositeInfo, "No info for Composite type: " + entityClassInfo.name() );

            // create instance
            T instance = (T)entityClassInfo.newInstance();
            
            // set context
            // XXX
//            FieldInfo contextField = entityClassInfo.fields().stream().filter( f -> f.name().equals( "context" ) ).findAny().get();
//            contextField.set( instance, new TemplateEntityRuntimeContext() );
            instance.context = new TemplateEntityRuntimeContext();
            
            // properties
            initProperties( instance, entityClassInfo );
            
            return instance;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ModelRuntimeException( "Error while instantiation of: " + entityClassInfo.name(), e );
        }
    }
    
    
    /**
     * Initializes all properties of the given Composite, including all super classes.
     * Composite properties are init with {@link CompositePropertyImpl} which comes back to 
     * {@link TemplateInstanceBuilder} when the value is accessed.
     */
    protected void initProperties( Composite instance, ClassInfo<?> classInfo ) throws Exception {
        // XXX cache fields
        for (FieldInfo field : classInfo.fields()) {
            if (PropertyBase.class.isAssignableFrom( field.type() )) {

                PropertyInfo<?> info = compositeInfo.getProperty( field.name() );
                PropertyBase<?> prop = null;

                // single property
                if (Property.class.isAssignableFrom( field.type() )) {
                    // Computed
                    if (info.isComputed()) {
                        prop = new NotQueryableProperty<>( info );
                    }
                    // primitive or Composite
                    else {
                        prop = new PropertyImpl<>( info );
                    }
                }

                // Collection
                else if (CollectionProperty.class.isAssignableFrom( field.type() )) {
                    // primitive or Composite
                    prop = new CollectionPropertyImpl( info );
                }

                // Association
                else if (Association.class.isAssignableFrom( field.type() )) {
                    prop = new AssociationImpl( info );
                }

                // ManyAssociation
                else if (ManyAssociation.class.isAssignableFrom( field.type() )) {
                    prop = new ManyAssociationImpl( info );
                }

                // set field
                //assert prop != null : "Unable to build property instance for: " + field;
                field.set( instance, prop );                    
            }
        }
    }

    
    /**
     * 
     */
    public class PropertyImpl<T>
            implements Property<T>, TemplateProperty<T> {

        protected PropertyInfo<T>       info;
        
        protected PropertyImpl( PropertyInfo<T> info ) {
            this.info = info;
        }

        @Override
        public PropertyInfo info() {
            return info;
        }

        @Override
        public String toString() {
            return "TemplateProperty[name=" + info.getName() + "]"; 
        }

        @Override
        public T get() {
//            if (Composite.class.isAssignableFrom( info.getType() )) {
//                Class<Composite> type = (Class<Composite>)info.getType();
//                return (T)new TemplateInstanceBuilder( repo, this ).newComposite( type );
//            }
//            else {
                throw new ModelRuntimeException( "Calling get() on a query template is not allowed. Use Expressions.the() quantifier to query a Composite property." );
//            }
        }

        @Override
        public void set( T value ) {
            throw new ModelRuntimeException( "Calling set() on a query template is not allowed." );
        }        

        @Override
        public <U extends T> U createValue( ValueInitializer<U> initializer ) {
            throw new ModelRuntimeException( "Calling createValue() on a query template is not allowed." );
        }
    }

   
    /**
     * 
     */
    public class AssociationImpl<T extends Entity>
            extends PropertyImpl<T>
            implements Association<T>, TemplateProperty<T> {
        

        protected AssociationImpl( PropertyInfo<T> info ) {
            super( info );
        }

        @Override
        public T get() {
            Class<T> type = info.getType();
            return new TemplateInstanceBuilder( repo ).newComposite( ClassInfo.of( type ) );
        }
    }


    /**
     * 
     */
    public class CollectionPropertyImpl<T extends Entity>
            extends AbstractCollection<T>
            implements CollectionProperty<T>, TemplateProperty<T> {
        
        protected PropertyInfo<T>       info;
        
        
        protected CollectionPropertyImpl( PropertyInfo<T> info ) {
            this.info = info;
        }

        @Override
        public PropertyInfo info() {
            return info;
        }

        @Override
        public <U extends T> U createElement( ValueInitializer<U> initializer ) {
            throw new ModelRuntimeException( "Method is not allowed on query template" );
        }

        @Override
        public Iterator<T> iterator() {
            throw new ModelRuntimeException( "Method is not allowed on query template" );
        }

        @Override
        public int size() {
            throw new ModelRuntimeException( "Method is not allowed on query template" );
        }
    }


    /**
     * 
     */
    public class ManyAssociationImpl<T extends Entity>
            extends AbstractCollection<T>
            implements ManyAssociation<T>, TemplateProperty<T> {
        
        protected PropertyInfo<T>       info;
        
        
        protected ManyAssociationImpl( PropertyInfo<T> info ) {
            this.info = info;
        }

        @Override
        public PropertyInfo info() {
            return info;
        }

        @Override
        public boolean add( T e ) {
            throw new ModelRuntimeException( "Method is not allowed on query template" );
        }

        @Override
        public Iterator<T> iterator() {
            throw new ModelRuntimeException( "Method is not allowed on query template" );
        }

        @Override
        public int size() {
            throw new ModelRuntimeException( "Method is not allowed on query template" );
        }
    }


    /**
     * 
     */
    public class NotQueryableProperty<T>
            extends PropertyImpl<T>
            implements Property<T>, TemplateProperty<T> {

        public NotQueryableProperty( PropertyInfo<T> info ) {
            super( info );
        }

        @Override
        public T get() {
            throw new ModelRuntimeException( "This Property is not @Queryable: " + info.getName() );
        }

        @Override
        public <U extends T> U createValue( ValueInitializer<U> initializer ) {
            throw new ModelRuntimeException( "Calling createValue() on a query template is not allowed." );
        }

        @Override
        public void set( T value ) {
            throw new ModelRuntimeException( "This Property is not @Queryable: " + info.getName() );
        }        
    }

    
    /**
     * 
     */
    protected class TemplateEntityRuntimeContext
            implements EntityRuntimeContext {

        @Override
        public EntityRepository getRepository() {
            return TemplateInstanceBuilder.this.repo;
        }

        @Override
        public <T extends Composite> T getCompositePart( Class<T> type ) {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public <E extends Entity> E getEntity() {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public CompositeState getState() {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public EntityStatus getStatus() {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public void raiseStatus( EntityStatus newStatus ) {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public void resetStatus( EntityStatus loaded ) {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public UnitOfWork getUnitOfWork() {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public StoreUnitOfWork getStoreUnitOfWork() {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public Object id() {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

    }
    
}
