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

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static java.util.Collections.singletonList;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.CREATED;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.MODIFIED;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import java.io.IOException;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.runtime.ConcurrentEntityModificationException;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.Lifecycle.State;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.CloneCompositeStateSupport;
import org.polymap.model2.store.CompositeState;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class UnitOfWorkNested
        extends UnitOfWorkImpl {

    /** The parent UnitOfWork in case of nested instances, or null for the root UnitOfWork. */
    protected UnitOfWorkImpl        parent;

    
    protected UnitOfWorkNested( EntityRepositoryImpl repo, CloneCompositeStateSupport storeUow, UnitOfWorkImpl parent ) {
        super( repo, storeUow );
        this.parent = parent;
        assert parent != null : "parent must not be null.";
    }
    
    
    CloneCompositeStateSupport storeUow() {
        return (CloneCompositeStateSupport)storeUow;    
    }

    
    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends Entity> T entity( final Class<T> entityClass, final Object id ) {
        assert entityClass != null;
        assert id != null;
        checkOpen();
        T result = (T)loaded.computeIfAbsent( id, key -> {
            // just clone the entire Entity and its state; copy-on-write would probably
            // be faster and less memory consuming but also would introduce a lot more complexity;
            // maybe I will later investigate a global copy-on-write cache for Entities
            T parentEntity = parent.entity( entityClass, id );

            if (parentEntity == null) {
                return null;
            }
            else {
                CompositeState parentState = repo.contextOf( parentEntity ).getState();
                CompositeState state = storeUow().cloneEntityState( parentState );
                return repo.buildEntity( state, entityClass, UnitOfWorkNested.this );
            }
        });
        return result != null && result.status() != EntityStatus.REMOVED ? result : null;
    }


    @Override
    public <T extends Entity> T entityForState( final Class<T> entityClass, Object state ) {
        throw new RuntimeException( "not yet implemented." );
//        checkOpen();
//        
//        final CompositeState compositeState = storeUow.adoptEntityState( state, entityClass );
//        final Object id = compositeState.id();
//        
//        return (T)loaded.get( id, new EntityCacheLoader() {
//            public Entity load( Object key ) throws RuntimeException {
//                return repo.buildEntity( compositeState, entityClass, UnitOfWorkNested.this );
//            }
//        });
    }

    
    @Override
    public void removeEntity( Entity entity ) {
        assert entity != null : "entity must not be null.";
        checkOpen();
        repo.contextOf( entity ).raiseStatus( EntityStatus.REMOVED );
    }


    @Override
    public <T extends Entity> Query<T> query( final Class<T> entityClass ) {
        return new Query<T>( entityClass ) {
            @Override
            public ResultSet<T> execute() {
                // unmodified ***
                final ResultSet<T> parentRs = parent.query( entityClass )
                        .where( expression )
                        .maxResults( maxResults )
                        .firstResult( firstResult )
                        .execute();
                // adopt entities
                Iterator<T> results = transform( parentRs.iterator(),
                        entity -> entity( entityClass, entity.id() ) );
                // filter
                Iterator<T> unmodifiedResults = filter( results,
                        entity -> {
                            EntityStatus status = entity != null ? entity.status() : EntityStatus.REMOVED;
                            assert status != EntityStatus.CREATED; 
                            return status == EntityStatus.LOADED;                            
                        });

                // modified ***
                assert expression instanceof BooleanExpression;
                Iterator<T> modifiedResults = (Iterator<T>)filter( modified.values().iterator(),
                        entity -> entity.getClass().equals( entityClass ) 
                                    && (entity.status() == CREATED || entity.status() == MODIFIED )
                                    && expression.evaluate( entity ) );

                // ResultSet, caching the ids for subsequent runs
                return new CachingResultSet<T>( concat( unmodifiedResults, modifiedResults ) ) {
                    @Override
                    protected T entity( Object id ) {
                        return UnitOfWorkNested.this.entity( entityClass, id, null );
                    }
                    @Override
                    public int size() {
                        if (cachedSize == -1) {
                            cachedSize = delegate == null
                                    ? cachedIds.size()
                                    : modified.isEmpty() 
                                            ? parentRs.size()
                                            : Iterators.size( iterator() );
                        }
                        return cachedSize;
                    }
                    @Override
                    public void close() {
                        parentRs.close();
                        super.close();
                    }
                };
            }
        };
    }


    @Override
    public UnitOfWork newUnitOfWork() {
        return new UnitOfWorkNested( repo, storeUow(), this );
    }

    
    @Override
    public Optional<UnitOfWork> parent() {
        return Optional.of( parent );
    }


    @Override
    public void prepare() throws IOException, ConcurrentEntityModificationException {
        checkOpen();
        commitLock.lock();
        prepareResult = null;
        lifecycle( State.BEFORE_PREPARE );
        for (Entity entity : modified.values()) {
            // created
            if (entity.status() == EntityStatus.CREATED) {
                // create a separate instance in the parent; same as for modified instances
                // this allows to manage distinct status for parent and nested entity
                CompositeState entityState = repo.contextOf( entity ).getState();
                CompositeState parentState = storeUow().cloneEntityState( entityState );
                
                Entity parentEntity = parent.entityForState( entity.getClass(), parentState.getUnderlying() );
                repo.contextOf( parentEntity ).raiseStatus( EntityStatus.CREATED );
                parent.modified.putIfAbsent( parentEntity.id(), parentEntity );
            }
            // modified
            if (entity.status() == EntityStatus.MODIFIED
                    || entity.status() == EntityStatus.REMOVED) {
                Entity parentEntity = parent.entity( entity.getClass(), entity.id() );
                
                if (parentEntity == null || parentEntity.status() == EntityStatus.REMOVED) {
                    throw new ConcurrentEntityModificationException( "Entity was removed in parent UnitOfWork.", singletonList( entity ) );
                }

                repo.contextOf( parentEntity ).raiseStatus( entity.status() );

                CompositeState parentState = repo.contextOf( parentEntity ).getState();
                CompositeState clonedState = repo.contextOf( entity ).getState();
                storeUow().reincorparateEntityState( parentState, clonedState );
                
                new ResetCachesVisitor().process( parentEntity );
            }
        }
        lifecycle( State.AFTER_PREPARE );
        prepareResult = PREPARED;
    }


    @Override
    public void commit() throws ModelRuntimeException {
        // prepare if not yet done
        if (prepareResult == null) {
            try {
                prepare();
            }
            catch (Exception e) {
                Throwables.propagateIfPossible( e, ModelRuntimeException.class );
            }
        }
        if (prepareResult != PREPARED) {
            throw new ModelRuntimeException( "UnitOfWork was not successfully prepared for commit." );
        }
        prepareResult = null;
        
        // reset Entity status
        lifecycle( State.BEFORE_COMMIT );
        resetStatusLoaded();
        lifecycle( State.AFTER_COMMIT );
        
        modified.clear();
        commitLock.unlock( true );
    }


    @Override
    public void rollback() throws ModelRuntimeException {
        checkOpen();
        lifecycle( State.BEFORE_ROLLBACK );

        // reset status of modified entities
        for (Map.Entry<Object,Entity> entry : modified.entrySet()) {
            Entity entity = entry.getValue();
            if (entity.status() == EntityStatus.CREATED) {
                InstanceBuilder.contextOf( entity ).detach();
                loaded.remove( entry.getKey() );
            }
            else {
                CompositeState state = repo.contextOf( entity ).getState();
                Entity parentEntity = parent.entity( entity.getClass(), entity.id() );
                CompositeState parentState = repo.contextOf( parentEntity ).getState();
                storeUow().reincorparateEntityState( state, parentState );

                new ResetCachesVisitor().process( entity );            
                repo.contextOf( entity ).resetStatus( EntityStatus.LOADED );
            }
        }
        modified.clear();
        
        lifecycle( State.AFTER_ROLLBACK );
        prepareResult = null;
        commitLock.unlock( false );
    }


    @Override
    public void reload( Entity entity ) throws ModelRuntimeException {
        throw new RuntimeException( "not yet implemented." );
    }


    public void close() {
        if (isOpen()) {
            // detach loaded Entities in order to avoid leaks and improper state access
            for (Entity entity : loaded.values()) {
                InstanceBuilder.contextOf( entity ).detach();
            }
            commitLock.unlock( false );
            repo = null;
            loaded = null;
            modified = null;
            
            // keep parent for parent()
            //parent = null;
        }
    }

}