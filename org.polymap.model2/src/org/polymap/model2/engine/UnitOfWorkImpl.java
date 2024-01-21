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

import static java.util.Collections.singleton;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.CREATED;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.LOADED;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.MODIFIED;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.REMOVED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.engine.EntityRepositoryImpl.EntityRuntimeContextImpl;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.Lifecycle;
import org.polymap.model2.runtime.Lifecycle.State;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.runtime.locking.CommitLockStrategy;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.CompositeStateReference;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.Assert;
import areca.common.MutableInt;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.base.Consumer;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class UnitOfWorkImpl
        implements UnitOfWork {

    private static final Log LOG = LogFactory.getLog( UnitOfWorkImpl.class );

//    protected static final Exception        PREPARED = new Exception( "Successfully prepared for commit." );
    
    private static MutableInt               idCount = new MutableInt/*AtomicInteger*/( (int)Math.abs( System.currentTimeMillis() ) );
    
    protected EntityRepositoryImpl          repo;
    
    /** Only set if this is the root UnitOfwork, or null if this is a nested instance. */
    protected StoreUnitOfWork               storeUow;
    
    /** Cache of all loaded entities: id -> entity */
    protected Map<Object,Entity>            loaded;
    
    protected Map<String,Composite>         loadedMixins;
    
    /** Strong reference to Entities that must not be GCed from {@link #loaded} cache. */
    protected Map<Object,Entity>            modified;
    
    protected CommitLockStrategy            commitLock;

    protected Priority                      priority;

    
    protected UnitOfWorkImpl( EntityRepositoryImpl repo, StoreUnitOfWork suow ) {
        this.repo = repo;
        this.storeUow = suow;
        assert repo != null : "repo must not be null.";
        assert suow != null : "suow must not be null.";

        this.loaded = new /*Concurrent*/HashMap<>( 128 );  //LoadingCache.create( cacheManager, cacheConfig );
        this.loadedMixins = new /*Concurrent*/HashMap<>( 128 );  //LoadingCache.create( cacheManager, cacheConfig );
        this.modified = new /*Concurrent*/HashMap<>( 128/*, 0.75f, SimpleCache.CONCURRENCY*/ );

        commitLock = repo.getConfig().commitLockStrategy.get().get();
    }

    
    /**
     * Raises the status of the given Entity. Called by {@link ConstraintsPropertyInterceptor}.
     */
    protected void raiseStatus( Entity entity, EntityStatus old) {
        checkOpen();
        if (entity.status() == MODIFIED || entity.status() == REMOVED) {
            modified.putIfAbsent( entity.id(), entity );
        }
        if (old.status < MODIFIED.status || entity.status() == MODIFIED) {
            lifecycle( singleton( entity ), State.AFTER_MODIFIED );
        }
        if (old.status < REMOVED.status || entity.status() == REMOVED) {
            lifecycle( singleton( entity ), State.AFTER_REMOVED );
        }
    }


    @Override
    public UnitOfWork setPriority( Priority priority ) {
        this.priority = priority;
        storeUow.setPriority( priority );
        return this;
    }


    @Override
    public Priority priority() {
        return priority;
    }


    @Override
    public <T extends Entity> T createEntity( Class<T> entityClass, ValueInitializer<T> initializer ) {
        checkOpen();
        
        // build id; don't depend on store's ability to deliver id for newly created state
        var id = entityClass.getSimpleName() + "." + idCount.getAndIncrement();
        LOG.debug( "id = " + id );

        CompositeState state = storeUow.newEntityState( id, entityClass );
        id = Assert.notNull( (String)state.id() );  // store can veto the id
        
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
        lifecycle( singleton( result ), State.AFTER_CREATED );        
        return result;
    }

    
    @Override
    public <T extends Entity,E extends Exception> Promise<T> ensureEntity( 
            Class<T> type, BooleanExpression cond, Consumer<T,E> initializer ) {
        
        return query( type ).where( cond ).executeCollect().map( rs -> {
            if (rs.isEmpty()) {
                // there is a race cond between query and create, so we have
                // to make sure that the entity was not created someone else for this UoW
                // XXX this needs some kind of lock in preempt environments
                T foundOrCreated = Sequence.of( modified.values() )
                        .<T,RuntimeException>filter( type::isInstance )
                        .first( cond::evaluate )  // FIXME evaluate is now async
                        .orElse( createEntity( type, initializer ) );
                Assert.that( cond.evaluate( foundOrCreated ), "Newly created Entity does not match search condition: " + cond );
                return foundOrCreated;
            }
            else {
                Assert.isEqual( 1, rs.size(), "UnitOfWork: ensureEntity() has multiple matches! (" + cond + ")" );
                return rs.get( 0 );
            }
        });
    }

    
    @Override
    public <T extends Entity> Promise<T> entity( final Class<T> entityClass, final Object id ) {
        Assert.notNull( entityClass, "Given entity Class is null." );
        Assert.notNull( id, "Given Id is null." );
        checkOpen();

        T cached = entityClass.cast( loaded.get( id ) );
        if (cached != null) {
            LOG.debug( "entity(): CACHED: %s", id );
            cached = cached.status() != EntityStatus.REMOVED ? cached : null;
            return Promise.completed( cached, priority );
        }
        else {
            return storeUow.loadEntityState( id, entityClass )
                    .priority( priority )
                    .map( state -> {
                        LOG.debug( "entity(): LOADED: %s", id );
                        if (state == null) {
                            return null;
                        }
                        else {
                            var entity = entityClass.cast( loaded.computeIfAbsent( id, __ -> { 
                                var result = repo.buildEntity( state, entityClass, UnitOfWorkImpl.this );
                                lifecycle( singleton( result ), State.AFTER_LOADED );
                                return result;
                            }));
                            Assert.that( entity.status() != EntityStatus.REMOVED );
                            return entity;
                        }
                    });
        }
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
        
        String key = String.join( "_", entity.id().toString(), mixinClass.getName() );
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
        return new Query<T>( entityClass ) {
            @Override
            public Promise<Opt<T>> execute() {
                // the preloaded entity from the CompositeStateReference is used to build the
                // entity; but we are not keeping a strong ref to it in order to allow the cache to
                // evict the entity state; 
                
                // we are either not keeping a strong ref to the CompositeStateReferences as they
                // may contain refs to the states which would kept in memory for the lifetime of
                // the ResultSet otherwise

                var _modified = Sequence.of( modified.values() )
                        .filter( it -> entityClass.isInstance( it ) && it.status() != REMOVED )
                        .map( it -> entityClass.cast( it ) )
                        .toList();

                // query store
                var queried = storeUow.executeQuery( this )
                        .priority( priority )
                        // check/load Entity
                        .then( (CompositeStateReference ref) -> {
                            if (ref != null) {
                                if (ref.get() != null) {
                                    // query has CompositeState already loaded
                                    var entity = loaded.computeIfAbsent( ref.id(), __ -> { 
                                        var result = repo.buildEntity( ref.get(), entityClass, UnitOfWorkImpl.this );
                                        lifecycle( singleton( result ), State.AFTER_LOADED );
                                        return result;
                                    });
                                    return Promise.completed( entityClass.cast( entity ), priority );
                                }
                                else {
                                    // query just returned id
                                    return entity( entityClass, ref.id() );
                                }
                            } 
                            else {
                                return Promise.completed( null, priority );
                            }
                        })
                        // filter modified
                        .filter( (T entity) -> {
                            if (entity != null) {
                                Assert.that( entity.status() != CREATED );
                                return entity.status() == LOADED;
                            } else {
                                return true;
                            }
                        });

                // unsubmitted changes
                LOG.debug( "query(): modified: %s (%s)", _modified.size(), modified.size() );
                Assert.that( orderBy == null || _modified.isEmpty(), "OrderBy for modified results is not yet supported." );
                var unsubmitted = Promise.joined( _modified.size(), null, i -> {
                    var check = _modified.get( i );                    
                    Assert.that( check.status().status > LOADED.status );
                    return expression.evaluate2( check ).map( match -> match ? check : null );
                });
                
//                var unsubmitted = new Promise.Completable<T>();
//                LOG.debug( "query(): modified: %s", modified.size() );
//                var count= new MutableInt( 0 );
//                for (var check : modified.values()) {
//                    Assert.that( check.status().status > LOADED.status );
//                    if (entityClass.isInstance( check ) && check.status() != REMOVED) {
//                        expression.evaluate2( check ).onSuccess( match -> { 
//                            if (match) {
//                                unsubmitted.consumeResult( entityClass.cast( check ) );
//                            }
//                            if (count.incrementAndGet() == modified.size()
//                        });
//                    }
//                }
//                unsubmitted.complete( null );
                
                return queried.join( unsubmitted ).map( entity -> Opt.of( entity ) );
            }
        };
    }

    
    @Override
    public UnitOfWork newUnitOfWork() {
        checkOpen();
        throw new UnsupportedOperationException( "Not implemented: nested UnitOfWork" );
//        if (storeUow instanceof CloneCompositeStateSupport) {
//            return new UnitOfWorkNested( repo, (CloneCompositeStateSupport)storeUow, this );
//        }
//        else {
//            throw new UnsupportedOperationException( "The current store backend does not support cloning states (nested UnitOfWork): " + storeUow );
//        }
    }


    @Override
    public Optional<UnitOfWork> parent() {
        return Optional.empty();
    }


    protected void lifecycle( Collection<Entity> entities, State state ) {
        for (Entity entity : entities) {
            if (entity instanceof Lifecycle) {
                try {
                    ((Lifecycle)entity).onLifecycleChange( state );
                }
                catch (Throwable e) {
                    LOG.warn( "Error while calling onLifecycleChange()", e );
                    throw (RuntimeException)e;
                }
            }
        }
    }
    
    
    @Override
    public Promise<Submitted> submit() {
        checkOpen();
        //commitLock.lock();

        lifecycle( modified.values(), State.BEFORE_SUBMIT );
        return storeUow
                .submit( modified.values() )
                .priority( priority )
                .onSuccess( submitted -> {
                    // commit store
                    resetStatusLoaded();
                    var _modified = new ArrayList<>( modified.values() );
                    modified.clear();
                    lifecycle( _modified, State.AFTER_SUBMIT );
                    //commitLock.unlock( true );
                });
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
        Assert.that( Sequence.of( loaded.values() ).allMatch( e -> e.status() == EntityStatus.LOADED ) );
    }

    
    @Override
    public Promise<Submitted> discard() throws ModelRuntimeException {
        checkOpen();
        lifecycle( modified.values(), State.BEFORE_DISCARD );

        // reset Entity internal caches
        for (Entity entity : modified.values()) {
            new ResetCachesVisitor().process( entity );            
        }
        
        // reset status of modified entities
        for (Map.Entry<Object,Entity> entry : modified.entrySet()) {
            if (entry.getValue().status() == CREATED) {
                InstanceBuilder.contextOf( entry.getValue() ).detach();
                loaded.remove( entry.getKey() );
            }
            else {
                repo.contextOf( entry.getValue() ).resetStatus( LOADED );
            }
        }
        
        // give entities a new state
        var notCreated = Sequence.of( loaded.values() ).filter( e -> e.status() != CREATED ).asIterable();
        return storeUow.rollback( notCreated )
                .onSuccess( __ -> {
                    lifecycle( modified.values(), State.AFTER_DISCARD );
                    modified.clear();        
                    //commitLock.unlock( true );
                });
    }


    @Override
    public Promise<Submitted> refresh() throws ModelRuntimeException {
        checkOpen();
        var _loaded = Sequence.of( loaded.values() )
                .filter( entity -> entity.status() == LOADED )
                .toList();
        return storeUow.rollback( _loaded )
                .map( submitted -> {
                    for (var removedId : submitted.removedIds) {
                        var removed = loaded.remove( removedId );
                        removed.context.resetStatus( EntityStatus.REMOVED );
                        LOG.info( "Removed: " + removed );
                    }
                    lifecycle( _loaded, State.AFTER_REFRESH );
                    return submitted;
                });
    }


    @Override
    public Promise<Submitted> refresh( Set<?> ids ) throws ModelRuntimeException {
        checkOpen();
        // XXX check if entity is modified? 
        var entities = Sequence.of( loaded.values() )
                .filter( entity -> ids.contains( entity.id() ) )
                .toList();
        return storeUow.rollback( entities )
                .map( submitted -> {
                    for (var removedId : submitted.removedIds) {
                        var removed = loaded.remove( removedId );
                        removed.context.resetStatus( EntityStatus.REMOVED );
                        LOG.info( "Removed: " + removed );
                    }
                    lifecycle( entities, State.AFTER_REFRESH );
                    return submitted;
                });
    }


    //@Override
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