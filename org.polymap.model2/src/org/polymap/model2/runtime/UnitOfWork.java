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
package org.polymap.model2.runtime;

import java.io.IOException;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.EntityRepository.Configuration;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.StoreSPI;

/**
 * A UnitOfWork is the only way to actually <b>access</b> Entities and to work with
 * them. A UnitOfWork tracks all <b>modifications</b>. It is used to logically group
 * operations that modify a set of entities. These modifications can then be written
 * down to the underlying store in one atomic transaction.
 * <p/>
 * Before {@link #prepare()}/{@link #commit()} all modifications are local to the
 * UnitOfWork. After {@link #commit()} all modifications are persitently stored. By
 * calling {@link #close()} the UnitOfWork can be *discarded* at any point in time.
 * <p/>
 * UnitOfWork can be safely accessed from <b>multiple threads</b>. However,
 * concurrent modifications of <b>one entity</b> results in undefined behaviour
 * and/or errors. This depends on the store implementation. The strategy to handle
 * attempts to concurrently {@link #prepare()}/ {@link #commit()} can be specified
 * via {@link Configuration#commitLockStrategy}.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface UnitOfWork
        extends AutoCloseable {

    /**
     * Builds an {@link Entity} instance for the given state and assigns it
     * to this {@link UnitOfWork}.
     * 
     * @param entityClass The type of the entity to build.
     * @param state The state of the entity
     * @param <T> The type of the entity to build.
     * @return A newly created {@link Entity} instance or a previously created instance.
     */
    public <T extends Entity> T entityForState( Class<T> entityClass, Object state );


    /**
     * Finds the {@link Entity} with the given identifier and type.
     * 
     * @param entityClass The type of the entity to find.
     * @param id The identifier of the entity to find.
     * @param <T> The type of the entity to build.
     * @return A newly loaded {@link Entity} instance or a previously created,
     *         cached instance. Returns null if no Entity exists for the given id.
     *         Also returns null if the Entity was {@link #removeEntity(Entity)
     *         removed} for this UnitOfWork.
     */
    public <T extends Entity> T entity( Class<T> entityClass, Object id );

    /**
     * 
     * 
     * @param entity An entity loaded from another {@link UnitOfWork}.
     * @param <T> The type of the entity to build.
     * @return A newly loaded {@link Entity} instance or a previously created,
     *         cached instance. Returns null if no Entity exists for the given id.
     *         Also returns null if the Entity was {@link #removeEntity(Entity)
     *         removed} for this UnitOfWork.
     */
    public <T extends Entity> T entity( T entity );

//    public <T extends Composite> T mixin( Class<T> entityClass, Entity entity );


    /**
     * Creates a new state in the underlying store and builds an {@link Entity}
     * representation for it.
     * 
     * @param <T>
     * @param entityClass
     * @param id The identifier of the newly created entity, or null if a new
     *        identifier is to be created by the store automatically. Avoid using this
     *        as most backend stores do not support this!
     * @param initializers Allows to init properties, especially properties that must
     *        have a value because they are non-{@link Nullable} and does not a
     *        default value.
     * @return Newly created {@link Entity}.
     */
    public <T extends Entity> T createEntity( Class<T> entityClass, Object id, ValueInitializer<T>... initializers );


    /**
     * Removes the given {@link Entity} from the underlying store. The
     * {@link Entity#status()} is set to {@link EntityStatus#REMOVED}.
     * 
     * @param entity The entity to remove.
     */
    public void removeEntity( Entity entity );


    /**
     * This methods allows a UnitOfWork to take part on a 2-phase commit protocol.
     * Calling this method sends all changes down to the underlying store but does
     * not commit the transaction. Client code does not have to use this method but
     * can call {@link #commit()} directly.
     * <p/>
     * Client code must not call any other method than {@link #commit()} or
     * {@link #close()} after {@link #prepare()}.
     * <p/>
     * Backend stores probably open an external transaction between
     * {@link #prepare()} and {@link #commit()}. This probably aquires resources and
     * locks. It is important that {@link #commit()} and/or {@link #close()} is
     * called with a reasonable timeframe. Consider using some kind of transaction
     * monitor for this.
     * <p/>
     * If {@link #prepare()} fails and throws an exception then it has to clean any
     * transaction specific resources and locks before returning. Client code may use
     * this UnitOfWork after prepare failed to cure problems and try prepare/commit
     * again.
     * 
     * @throws IOException If the underlying store was not able to store all changes
     *         properly.
     * @throws ConcurrentEntityModificationException
     */
    public void prepare() throws IOException, ConcurrentEntityModificationException;


    /**
     * Persistently stores all modifications that were made within this UnitOfWork.
     * If {@link #prepare()} has not been called yet then it is done by this method.
     * <p/>
     * This does not close this {@link UnitOfWork} but may flush internal caches.
     * 
     * @throws ModelRuntimeException If {@link #prepare()} was called by this method
     *         and a exception occured.
     */
    public void commit() throws ModelRuntimeException;

    
    /**
     * Discards any uncommitted modifications but does not close this UnitOfWork.
     * This method can be called before or after {@link #prepare()}. The states of
     * all modified entities are reloaded from backend store. Newly created entities
     * are in {@link EntityStatus#DETACHED} state afterwards.
     * <p/>
     * This may flush internal caches.
     * 
     * @throws ModelRuntimeException
     */
    public void rollback() throws ModelRuntimeException;


    /**
     * Closes this UnitOfWork by releasing all resources associated with this
     * instance. All uncommitted modifications are discarded.
     * <p/>
     * No method should be called after closing the UnitOfWork except for
     * {@link #isOpen()}.
     */
    public void close();
    
    public boolean isOpen();

    
    /**
     * Creates a new query for the given {@link Entity} type. By default the returned
     * {@link Query} returns all entities of the given type with no order. That is:
     * <ul>
     * <li>{@link Query#where(Object)} == null</li>
     * <li>{@link Query#firstResult(int)} == 0</li>
     * <li>{@link Query#maxResults(int)} == {@link Integer#MAX_VALUE}</li>
     * </ul>
     * <p>
     * The {@link ResultSet} reflects all Entity/Property modifications of the
     * corresponding {@link UnitOfWork}. That is, the query works against the states
     * of the Entities inside the UnitOfWork - not the states in the backend store.
     * There is no need to commit changes before executing a query. However, checking
     * the modified Entities in-memory might be not as fast as let the backend store
     * do an (indexed) seach.
     * 
     * @param entityClass
     * @return Newly created {@link Query} instance. It allows to set pagination and
     *         ordering and then {@link Query#execute()} the query against the data
     *         store.
     */
    public <T extends Entity> Query<T> query( Class<T> entityClass );

    
    /**
     * Creates a new, <b>nested</b> {@link UnitOfWork}. The nested UnitOfWork
     * reflects the Entity states of the parent UnitOfWork. Committing the nested
     * UnitOfWork writes down the modifications to the parent without changing the
     * underlying store. Until prepare/commit the parent UnitOfWork does not see any
     * modification done in the nested instance.
     * <p/>
     * {@link #rollback()} resets the states of the entities of the nested UnitOfWork
     * to the state of the parent. Care must be taken after {@link #prepare()} has
     * been called (and it maybe failed). After {@link #prepare()} the parent
     * contains (some) modifications. So the parent <b>MUST</b> be
     * {@link #rollback()}ed <b>before</b> the nested UnitOfWork can be rolled back.
     * <p/>
     * There is <b>no check for concurrent modifications</b> between the nested
     * instances! Client code has to make sure that the parent UnitOfWork is not
     * modified as long as there are nested instances in use. Otherwise modifications
     * in the parent would get <b>lost silently</b>!
     * <p/>
     * The availability of nested UnitOfWork depends on the capability of the
     * {@link StoreSPI store implementation} to clone {@link CompositeState Entity
     * states}.
     */
    public UnitOfWork newUnitOfWork();
    
}
