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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public final class TemplateInstanceBuilder {

    private static Log log = LogFactory.getLog( TemplateInstanceBuilder.class );

    private EntityRepository        repo;

    private CompositeInfo           compositeInfo;
    
    
    public TemplateInstanceBuilder( EntityRepository repo ) {
        this.repo = repo;
    }


    public <T extends Composite> T newComposite( Class<T> entityClass ) { 
        try {
            // composite info
            compositeInfo = repo.infoOf( entityClass );
            if (compositeInfo == null) {
                log.info( "Mixin type not declared on Entity type: " + entityClass.getName() );
                compositeInfo = new CompositeInfoImpl( entityClass );
            }
            assert compositeInfo != null : "No info for Composite type: " + entityClass.getName();

            // create instance
            Constructor<?> ctor = entityClass.getConstructor( new Class[] {} );
            T instance = (T)ctor.newInstance( new Object[] {} );
            
            // set context
            InstanceBuilder.contextField.set( instance, new TemplateEntityRuntimeContext() );
            
            // properties
            initProperties( instance );
            
            return instance;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ModelRuntimeException( "Error while instantiation of: " + entityClass, e );
        }
    }
    
    
    /**
     * Initializes all properties of the given Composite, including all super classes.
     * Composite properties are init with {@link CompositePropertyImpl} which comes back to 
     * {@link TemplateInstanceBuilder} when the value is accessed.
     */
    protected void initProperties( Composite instance ) throws Exception {
        Class superClass = instance.getClass();
        while (superClass != null) {
            // XXX cache fields
            for (Field field : superClass.getDeclaredFields()) {
                if (PropertyBase.class.isAssignableFrom( field.getType() )) {
                    field.setAccessible( true );

                    PropertyInfo info = compositeInfo.getProperty( field.getName() );
                    PropertyBase prop = null;

                    // single property
                    if (Property.class.isAssignableFrom( field.getType() )) {
                        // Computed
                        if (info.isComputed()) {
                            prop = new NotQueryableProperty( info );
                        }
                        // primitive or Composite
                        else {
                            prop = new PropertyImpl( info );
                        }
                    }

                    // Collection
                    else if (CollectionProperty.class.isAssignableFrom( field.getType() )) {
                        // primitive or Composite
                        prop = new CollectionPropertyImpl( info );
                    }

                    // Association
                    else if (Association.class.isAssignableFrom( field.getType() )) {
                        prop = new AssociationImpl( info );
                    }

                    // ManyAssociation
                    else if (ManyAssociation.class.isAssignableFrom( field.getType() )) {
                        prop = new ManyAssociationImpl( info );
                    }

                    // set field
                    //assert prop != null : "Unable to build property instance for: " + field;
                    field.set( instance, prop );                    
                }
            }
            superClass = superClass.getSuperclass();
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
        public T createValue( ValueInitializer<T> initializer ) {
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
            return new TemplateInstanceBuilder( repo ).newComposite( type );
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
        public T createElement( ValueInitializer<T> initializer ) {
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
        public T createValue( ValueInitializer<T> initializer ) {
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
        public CompositeInfo getInfo() {
            return compositeInfo;
        }

        @Override
        public <T extends Composite> T getCompositePart( Class<T> type ) {
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
        public EntityRepository getRepository() {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }

        @Override
        public void methodProlog( String methodName, Object[] args ) {
            throw new UnsupportedOperationException( "Method is not allowed for template Composite instance." );
        }
        
    }
    
}
