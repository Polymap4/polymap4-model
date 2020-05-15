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
package org.polymap.model2.engine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.Configuration;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.FieldInfo;

/**
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class EntityRepositoryImpl
        extends EntityRepository {

    private static final Log log = LogFactory.getLog( EntityRepositoryImpl.class );

    private Configuration               config;
    
    /** Infos of Entities, Mixins, Composite properties. */
    private Map<ClassInfo<? extends Composite>,CompositeInfo<?>> infos = new HashMap<>();
    
    
    public EntityRepositoryImpl( final Configuration config ) {
        this.config = config;
        
        // init infos
        log.debug( "Initialializing Composite types:" );
        Queue<ClassInfo<? extends Composite>> queue = new LinkedList<>();
        queue.addAll( config.entities.get() );
        
        while (!queue.isEmpty()) {
            ClassInfo<? extends Composite> typeInfo = queue.poll();
            if (!infos.containsKey( typeInfo )) {
                log.debug( "    Composite type: " + typeInfo );
                CompositeInfoImpl<?> info = new CompositeInfoImpl<>( typeInfo );
                if (infos.put( typeInfo, info ) != null) {
                    throw new ModelRuntimeException( "CompositeInfo already registered for: " + typeInfo );
                }

                // init static TYPE variable
                try {
                    FieldInfo field = typeInfo.fields().stream().filter( f -> f.name().equals( "TYPE" ) )
                            .findAny().orElse( null );
                    
                    if (field != null) {
                        //assert field.get( null ) != null : "Entity class is already connected to an other repository.";
                        Composite current = (Composite)field.get( null );
                        if (current != null) {
                            log.warn( "Entity class is already connected to an other repository: " + typeInfo.name() );
                        }
                        field.set( null, Expressions.template( typeInfo.type(), this ) );
                    }
                }
                catch (SecurityException e) {
                    throw new ModelRuntimeException( e );
                }
                
                // mixins
                info.getMixins().stream().map( m -> ClassInfo.of( m ) ).forEach( m -> queue.offer( m ) );

                // Composite properties
                for (PropertyInfo<?> propInfo : info.getProperties()) {
                    if (Composite.class.isAssignableFrom( propInfo.getType() )) {
                        queue.offer( ClassInfo.of( (Class<Composite>)propInfo.getType() ) );
                    }
                }
            }
        }
//        infos.entrySet().forEach( entry -> System.out.println( "   " + entry.getKey() + " -> ..." ) );
//        log.debug( "done" );
        
        // init store
        getStore().init( new StoreRuntimeContextImpl() );
    }

    
    public StoreSPI getStore() {
        checkOpen();
        return config.store.get();
    }

    
    public Configuration getConfig() {
        checkOpen();
        return config;
    }

    public boolean isOpen() {
        return config != null;
    }
    
    protected void checkOpen() {
        if (!isOpen()) {
            throw new RuntimeException( "EntityRepository is closed." );
        }
    }

    public void close() {
        if (isOpen()) {
            try {
                getStore().close();
            }
            finally {
                config = null;
            }
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends Composite> CompositeInfo<T> infoOf( ClassInfo<T> compositeClassInfo ) {
        CompositeInfo<T> result = (CompositeInfo<T>)infos.get( compositeClassInfo );
        
        // for Composite properties the actual type might be a sub-class of the declared type
        // see TypedValueInitializer
        if (result == null) {
            for (Map.Entry<ClassInfo<? extends Composite>,CompositeInfo<?>> entry : infos.entrySet()) {
                if (entry.getKey().type().isAssignableFrom( compositeClassInfo.type() )) {
                    return (CompositeInfo<T>)entry.getValue();
                }
            }
        }
        return Assert.notNull( result );
    }
    
    @Override    
    public UnitOfWork newUnitOfWork() {
        return new UnitOfWorkImpl( this, getStore().createUnitOfWork() );
    }
    
    
    protected <T extends Entity> T buildEntity( CompositeState state, Class<T> entityClass, UnitOfWork uow ) {
        //log.info( "buildEntity(): ..." );
        try {
            EntityRuntimeContextImpl entityContext = new EntityRuntimeContextImpl( state, EntityStatus.LOADED, uow );
            InstanceBuilder builder = new InstanceBuilder( entityContext );
            T result = builder.newComposite( state, entityClass );
            entityContext.entity = result;
            return result;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ModelRuntimeException( e );
        }
    }

    
    protected <T extends Composite> T buildMixin( Entity entity, Class<T> mixinClass, UnitOfWork uow ) {
        try {
            EntityRuntimeContextImpl entityContext = contextOf( entity );
            InstanceBuilder builder = new InstanceBuilder( entityContext );
            return builder.newComposite( entityContext.getState(), mixinClass );
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ModelRuntimeException( e );
        }
    }

    
    /**
     * Engine internal method that exposes the context of the given entity.
     */
    protected EntityRuntimeContextImpl contextOf( Entity entity ) {
        return InstanceBuilder.contextOf( entity );
    }
    
    
    /**
     * 
     */
    protected final class StoreRuntimeContextImpl
            implements StoreRuntimeContext {

        public EntityRepositoryImpl getRepository() {
            return EntityRepositoryImpl.this;
        }

        public EntityRuntimeContext contextOfEntity( Entity entity ) {
            return EntityRepositoryImpl.this.contextOf( entity );
        }
    }


    /**
     * 
     */
    protected class EntityRuntimeContextImpl
            implements EntityRuntimeContext {

        private Entity                  entity;
        
        private CompositeState          state;
        
        private EntityStatus            status;
        
        private UnitOfWork              uow;
        
        private Object                  id;

        
        EntityRuntimeContextImpl( CompositeState state, EntityStatus status, UnitOfWork uow ) {
            assert state != null;
            assert uow != null;
            assert status != null;
            
            this.state = state;
            this.status = status;
            this.uow = uow;
        }

        public void detach() {
            id();  // make sure that #id is initialized
            status = EntityStatus.DETACHED;
            
            //entity = null;
            state = null;
            uow = null;
        }
        
        /**
         * For some {@link Cache} implementations used by {@link UnitOfWorkImpl} it
         * is possible that cache entries are evicted while there are still
         * references on it. This may lead to a situation where modifications are not
         * recognized, hence lost updates. This check makes sure that an Exception is
         * thrown at least.
         */
        protected void checkState() {
            assert status != EntityStatus.EVICTED;
            if (status == EntityStatus.DETACHED || uow == null) {
                throw new ModelRuntimeException( "Entity is detached after UnitOfWork has been closed or rolled back: " + entity );
            }
        }
        
        @Override
        public Object id() {
            if (id == null) {
                id = state.id();
            }
            return id;
        }


        @Override
        public UnitOfWork getUnitOfWork() {
            checkState();
            return uow;
        }

        @Override
        public StoreUnitOfWork getStoreUnitOfWork() {
            checkState();
            // XXX :( ???
            return ((UnitOfWorkImpl)uow).storeUow;
        }

        @Override
        public EntityRepository getRepository() {
            checkState();
            return EntityRepositoryImpl.this;
        }
        
        @Override
        public CompositeState getState() {
            checkState();
            return state;
        }

        @Override
        public EntityStatus getStatus() {
            return status;
        }

        @Override
        public void raiseStatus( EntityStatus newStatus ) {
            checkState();
            assert newStatus.status >= status.status;
            // keep created if modified after creation
            if (status != EntityStatus.CREATED) {
                status = newStatus;
            }
            ((UnitOfWorkImpl)uow).raiseStatus( entity );
        }

        @Override
        public void resetStatus( EntityStatus newStatus ) {
            checkState();
            this.status = newStatus;
        }

        @Override
        public <E extends Entity> E getEntity() {
            return (E)entity;
        }


        @Override
        public <T extends Composite> T getCompositePart( Class<T> type ) {
            if (type.isAssignableFrom( entity.getClass() )) {
                return (T)entity;
            }
            else {
                throw new RuntimeException( "Retrieving mixin parts is not yet implemented." );
            }
        }

    }
    
}
