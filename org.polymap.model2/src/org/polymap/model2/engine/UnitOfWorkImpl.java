/* 
 * polymap.org
 * Copyright (C) 2012-2016, Falko Bräutigam. All rights reserved.
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

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.CREATED;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.MODIFIED;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import java.io.IOException;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.engine.EntityRepositoryImpl.EntityRuntimeContextImpl;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.runtime.ConcurrentEntityModificationException;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.Lifecycle;
import org.polymap.model2.runtime.Lifecycle.State;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.runtime.locking.CommitLockStrategy;
import org.polymap.model2.store.CloneCompositeStateSupport;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreResultSet;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Assert;
import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class UnitOfWorkImpl
        implements UnitOfWork {

    private static final Log log = LogFactory.getLog( UnitOfWorkImpl.class );

    protected static final Exception        PREPARED = new Exception( "Successfully prepared for commit." );
    
    private static AtomicInteger            idCount = new AtomicInteger( (int)Math.abs( System.currentTimeMillis() ) );
    
    protected EntityRepositoryImpl          repo;
    
    /** Only set if this is the root UnitOfwork, or null if this is a nested instance. */
    protected StoreUnitOfWork               storeUow;
    
    /** id -> entity */
    protected Map<Object,Entity>   loaded;
    
    protected Map<String,Composite> loadedMixins;
    
    /** Strong reference to Entities that must not be GCed from {@link #loaded} cache. */
    protected Map<Object,Entity>            modified;
    
    protected volatile Exception            prepareResult;
    
    protected CommitLockStrategy            commitLock;

    
    protected UnitOfWorkImpl( EntityRepositoryImpl repo, StoreUnitOfWork suow ) {
        this.repo = repo;
        this.storeUow = suow;
        assert repo != null : "repo must not be null.";
        assert suow != null : "suow must not be null.";

//        MutableConfiguration cacheConfig = new MutableConfiguration()
//                .setExpiryPolicyFactory( AccessedExpiryPolicy.factoryOf( Duration.ONE_MINUTE ) );
//        CacheManager cacheManager = repo.getConfig().cacheManager.get();
        
        this.loaded = new /*Concurrent*/HashMap<>( 128 );  //LoadingCache.create( cacheManager, cacheConfig );
        this.loadedMixins = new /*Concurrent*/HashMap<>( 128 );  //LoadingCache.create( cacheManager, cacheConfig );
        this.modified = new /*Concurrent*/HashMap<>( 128/*, 0.75f, SimpleCache.CONCURRENCY*/ );

        commitLock = repo.getConfig().commitLockStrategy.get().get();
        
//        // check evicted entries and re-insert if modified
//        this.loaded.addEvictionListener( new CacheEvictionListener<Object,Entity>() {
//            public void onEviction( Object key, Entity entity ) {
//                // re-insert if modified
//                if (entity.status() != EntityStatus.LOADED) {
//                    loaded.putIfAbsent( key, entity );
//                }
//                // mark entity as evicted otherwise
//                else {
//                    EntityRuntimeContext entityContext = UnitOfWorkImpl.this.repo.contextOfEntity( entity );
//                    entityContext.raiseStatus( EntityStatus.EVICTED );
//                }
//            }
//        });
    }

    
    /**
     * Raises the status of the given Entity. Called by {@link ConstraintsPropertyInterceptor}.
     */
    protected void raiseStatus( Entity entity) {
        checkOpen();
        if (entity.status() == EntityStatus.MODIFIED
                || entity.status() == EntityStatus.REMOVED) {
            modified.putIfAbsent( entity.id(), entity );
        }        
    }


    @Override
    public <T extends Entity> T createEntity( Class<T> entityClass, Object id, ValueInitializer<T> initializer ) {
        checkOpen();
        
        // build id; don't depend on store's ability to deliver id for newly created state
        id = id != null ? id : entityClass.getSimpleName() + "." + idCount.getAndIncrement();
        log.info( "id = " + id );

        CompositeState state = storeUow.newEntityState( id, entityClass );
        Assert.that( id == null || state.id().equals( id ) );
        log.info( "state = " + state );
        
        T result = repo.buildEntity( state, entityClass, this );
        repo.contextOf( result ).raiseStatus( EntityStatus.CREATED );

        boolean ok = loaded.putIfAbsent( id, result ) == null;
        if (!ok) {
            throw new ModelRuntimeException( "ID of newly created Entity already exists: " + id );
        }
        modified.put( id, result );
        
        // initializer
        try {
            if (initializer != null) {
                initializer.initialize( result );
            }
        }
        catch (Exception e) {
            throw new IllegalStateException( "Error while initializing.", e );
        }
        
        return result;
    }


    @Override
    public <T extends Entity> T entity( final Class<T> entityClass, final Object id ) {
        return entity( entityClass, id, null );
    }


    @Override
    public <T extends Entity> T entity( T entity ) {
        return (T)entity( entity.getClass(), entity.id() );
    }


    /**
     * 
     *
     * @param entityClass
     * @param id
     * @param preloaded Optional supplier of an already loaded CompositeState.
     * @return
     */
    protected <T extends Entity> T entity( 
            final Class<T> entityClass,
            final Object id, 
            final Supplier<CompositeState> preloaded ) {
        
        assert entityClass != null : "Given entity Class is null.";
        assert id != null : "Given Id is null.";
        checkOpen();
        @SuppressWarnings( "unchecked" )
        T result = (T)loaded.computeIfAbsent( id, key -> {
                // get preloaded if provided
                CompositeState state = preloaded != null ? preloaded.get() : null;
                // no preloaded or it returned null?
                state = state != null ? state : storeUow.loadEntityState( id, entityClass );
                return state != null ? repo.buildEntity( state, entityClass, UnitOfWorkImpl.this ) : null;
        });
        return result != null && result.status() != EntityStatus.REMOVED ? result : null;
    }


    @Override
    @SuppressWarnings( "unchecked" )
    public <T extends Entity> T entityForState( final Class<T> entityClass, Object state ) {
        checkOpen();
        
        final CompositeState compositeState = storeUow.adoptEntityState( state, entityClass );
        final Object id = compositeState.id();
        
        // modified
        if (modified.containsKey( id ) && state != compositeState.getUnderlying()) {
            throw new RuntimeException( "Entity is already modified in this UnitOfWork." );
        }
        // build Entity instance
        return (T)loaded.computeIfAbsent( id, key -> {
            return (T)repo.buildEntity( compositeState, entityClass, UnitOfWorkImpl.this );
        });
    }

    
    @SuppressWarnings( "unchecked" )
    public <T extends Composite> T mixin( final Class<T> mixinClass, final Entity entity ) {
        assert mixinClass != null : "mixinClass must not be null.";
        assert entity != null : "entity must not be null.";
        checkOpen();
        
        String key = Joiner.on( '_' ).join( entity.id().toString(), mixinClass.getName() );
        return (T)loadedMixins.computeIfAbsent( key, k -> {
            return repo.buildMixin( entity, mixinClass, UnitOfWorkImpl.this );
        });
    }


    @Override
    public void removeEntity( Entity entity ) {
        assert entity != null : "entity must not be null.";
        checkOpen();
        repo.contextOf( entity ).raiseStatus( EntityStatus.REMOVED );
    }


    @Override
    public <T extends Entity> Query<T> query( final Class<T> entityClass ) {
        checkOpen();
        return new Query( entityClass ) {
            @Override
            public ResultSet<T> execute() {
                // the preloaded entity from the CompositeStateReference is used to build the
                // entity; but we are not keeping a strong ref to it in order to allow the cache to
                // evict the entity state; 
                
                // we are either not keeping a strong ref to the CompositeStateReferences as they
                // may contain refs to the states which would kept in memory for the lifetime of
                // the ResultSet otherwise

                // unmodified
                final StoreResultSet rs = storeUow.executeQuery( this );
                Iterator<T> results = transform( rs,
                        ref -> entity( entityClass, ref.id(), ref ) );
                Iterator<T> unmodifiedResults = filter( results,
                        entity -> {
                            EntityStatus status = entity != null ? entity.status() : EntityStatus.REMOVED;
                            assert status != EntityStatus.CREATED; 
                            return status == EntityStatus.LOADED;                            
                        });

                // modified
                // XXX not cached, done for every call to iterator()
                assert expression instanceof BooleanExpression;
                Iterator<T> modifiedResults = (Iterator<T>)filter( modified.values().iterator(),
                        entity -> entity.getClass().equals( entityClass ) 
                                    && (entity.status() == CREATED || entity.status() == MODIFIED )
                                    && expression.evaluate( entity ) );

                // ResultSet, caching the ids for subsequent runs
                return new CachingResultSet<T>( concat( unmodifiedResults, modifiedResults ) ) {
                    @Override
                    protected T entity( Object id ) {
                        return UnitOfWorkImpl.this.entity( entityClass, id, null );
                    }
                    @Override
                    public int size() {
                        if (cachedSize == -1) {
                            cachedSize = delegate == null
                                    ? cachedIds.size()
                                    : modified.isEmpty() 
                                            ? rs.size()
                                            : Iterators.size( iterator() );
                        }
                        return cachedSize;
                    }
                    @Override
                    public void close() {
                        rs.close();
                        super.close();
                    }
                };
            }
        };
    }

    
    @Override
    public UnitOfWork newUnitOfWork() {
        checkOpen();
        if (storeUow instanceof CloneCompositeStateSupport) {
            return new UnitOfWorkNested( repo, (CloneCompositeStateSupport)storeUow, this );
        }
        else {
            throw new UnsupportedOperationException( "The current store backend does not support cloning states (nested UnitOfWork): " + storeUow );
        }
    }


    @Override
    public Optional<UnitOfWork> parent() {
        return Optional.empty();
    }


    protected void lifecycle( State state ) {
        for (Entity entity : modified.values()) {
            if (entity instanceof Lifecycle) {
                try {
                    ((Lifecycle)entity).onLifecycleChange( state );
                }
                catch (Throwable e) {
                    log.warn( "Error while calling onLifecycleChange()", e );
                }
            }
        }
    }
    
    
    @Override
    public void prepare() throws IOException, ConcurrentEntityModificationException {
        checkOpen();
        commitLock.lock();
        try {
            prepareResult = null;
            lifecycle( State.BEFORE_PREPARE );
            storeUow.prepareCommit( modified.values() );
            lifecycle( State.AFTER_PREPARE );
            prepareResult = PREPARED;
        }
        catch (ModelRuntimeException e) {
            prepareResult = e;
            throw e;
        }
        catch (IOException e) {
            prepareResult = e;
            throw e;
        }
        catch (Exception e) {
            prepareResult = e;
            throw new ModelRuntimeException( e );
        }
    }


    @Override
    public void commit() throws ModelRuntimeException {
        checkOpen();
        // prepare if not yet done
        if (prepareResult == null) {
            try {
                prepare();
            }
            catch (ModelRuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new ModelRuntimeException( e );
            }
            finally {
                if (prepareResult != PREPARED) {
                    rollback();
                }
            }
        }
        if (prepareResult != PREPARED) {
            throw new ModelRuntimeException( "UnitOfWork is not prepared successfully for commit." );
        }
        // commit store
        lifecycle( State.BEFORE_COMMIT );
        storeUow.commit();
        prepareResult = null;
        
        resetStatusLoaded();
        lifecycle( State.AFTER_COMMIT );
        
        modified.clear();
        commitLock.unlock( true );
    }

    
    /** 
     * Reset status of all {@link #modified} entities (after {@link #commit()}).
     */
    protected void resetStatusLoaded() {        
        for (Map.Entry<Object,Entity> entry : modified.entrySet()) {
            if (entry.getValue().status() == EntityStatus.REMOVED) {
                loaded.remove( entry.getKey() );
            }
            else {
                repo.contextOf( entry.getValue() ).resetStatus( EntityStatus.LOADED );
            }
        }
        
        // all Entities in loaded have LOADED state?
        Assert.that( loaded.values().stream().allMatch( e -> e.status() == EntityStatus.LOADED ) );
    }

    
    @Override
    public void rollback() throws ModelRuntimeException {
        checkOpen();
        lifecycle( State.BEFORE_ROLLBACK );
        
        // give all entities a new state
        storeUow.rollback( modified.values() );
        
        // reset Entity internal caches
        for (Entity entity : modified.values()) {
            new ResetCachesVisitor().process( entity );            
        }
        
        // reset status of modified entities
        for (Map.Entry<Object,Entity> entry : modified.entrySet()) {
            if (entry.getValue().status() == EntityStatus.CREATED) {
                InstanceBuilder.contextOf( entry.getValue() ).detach();
                loaded.remove( entry.getKey() );
            }
            else {
                repo.contextOf( entry.getValue() ).resetStatus( EntityStatus.LOADED );
            }
        }
        lifecycle( State.AFTER_ROLLBACK );

        modified.clear();        
        prepareResult = null;        
        commitLock.unlock( true );
    }


    @Override
    public void reload( Entity entity ) throws ModelRuntimeException {
        EntityRuntimeContextImpl context = repo.contextOf( entity );
        assert context.getUnitOfWork() == this;
        assert loaded.containsKey( entity.id() );
        assert entity.status() != EntityStatus.CREATED : "Illegal: reload() of CREATED entity";
  
        // XXX 
        context.resetStatus( EntityStatus.MODIFIED );
        storeUow.rollback( Collections.singleton( entity ) );
        
        new ResetCachesVisitor().process( entity );
        
        context.resetStatus( EntityStatus.LOADED );
        modified.remove( entity.id() );
    }


    @Override
    public void close() {
        if (isOpen()) {
            // detach loaded Entities to avoid leaks and improper state access
            for (Entity entity : loaded.values()) {
                InstanceBuilder.contextOf( entity ).detach();
            }            
            commitLock.unlock( false );
            storeUow.close();
            repo = null;
            loaded.clear();
            loaded = null;
            modified.clear();
            modified = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (isOpen()) {
            close();
        }
    }


    public boolean isOpen() {
        return repo != null;
    }

    
    protected final void checkOpen() throws ModelRuntimeException {
        if (!isOpen()) {
            throw new IllegalStateException( "UnitOfWork is closed." );
        }
    }
    
}