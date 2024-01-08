/* 
 * polymap.org
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package org.polymap.model2.store.no2;

import static org.dizitart.no2.common.SortOrder.Ascending;
import static org.dizitart.no2.common.SortOrder.Descending;

import java.util.Collection;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.FindOptions;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.collection.NitriteId;
import org.apache.commons.lang3.tuple.MutablePair;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.UnitOfWork.Submitted;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.CompositeStateReference;
import org.polymap.model2.store.StoreUnitOfWork;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.Scheduler.Priority;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 * @author Falko
 */
public class No2UnitOfWork
        implements StoreUnitOfWork {

    private static final Log LOG = LogFactory.getLog( No2UnitOfWork.class );
    
    private No2Store store;
    
    
    public No2UnitOfWork( No2Store store ) {
        this.store = store;
    }

    @Override
    public void close() {
    }


    protected NitriteCollection collection( CompositeInfo<?> entityInfo ) {
        return store.db.getCollection( entityInfo.getNameInStore() );
    }
    
    
    @Override
    public <T extends Entity> CompositeState newEntityState( Object id, Class<T> entityClass ) {
        return new No2CompositeState( id, entityClass );
    }

    
    @Override
    public <T extends Entity> Promise<CompositeState> loadEntityState( Object id, Class<T> entityClass ) {
        return store.async( __ -> {
            CompositeInfo<T> entityInfo = store.infoOf( entityClass );
            LOG.debug( "loadEntityState(): " + entityInfo.getNameInStore() + " / " + id );
            
            var coll = collection( entityInfo );
            var doc = coll.getById( NitriteId.createId( (String)id ) );
            return doc != null ? new No2CompositeState( entityClass, doc ) : null;
        });
    }

    
    @Override
    public <T extends Entity> CompositeState adoptEntityState( Object state, Class<T> entityClass ) {
        throw new RuntimeException( "not yet implemented." );
    }

    
    @Override
    public <T extends Entity> Promise<CompositeStateReference> executeQuery( Query<T> query ) {
        var promise = new Promise.Completable<CompositeStateReference>();
        
        store.async( __ -> {
            Class<T> entityClass = query.resultType();
            var coll = collection( store.infoOf( entityClass ) );
            LOG.debug( "executeQuery(): %s - where: %s", entityClass.getSimpleName(), query.expression );
            
            var options = new FindOptions();
            options.skip( query.firstResult ).limit( query.maxResults );
            if (query.orderBy != null) {
                options.thenOrderBy( query.orderBy.prop.info().getNameInStore(), 
                        query.orderBy.order == Query.Order.ASC ? Ascending : Descending );
            }
            var cursor = coll.find( new FilterBuilder( query ).build(), options );
            for (var doc : cursor) {
                //Platform.async( () -> {
                    var state = new No2CompositeState( entityClass, doc );
                    promise.consumeResult( CompositeStateReference.create( doc.getId().getIdValue(), state ) );
                //});
            }
            //Platform.async( () -> promise.complete( null ) );
            promise.complete( null );
            
            return null;
        });

        return promise;
    }

    
    @Override
    public Promise<Submitted> submit( Collection<Entity> modified ) {
        return store.async( __ -> {
            var tx = store.session.beginTransaction();
            try {
                var submitted = new Submitted();
                for (Entity entity : modified) {
                    LOG.debug( "submit(): %s", entity );
                    var coll = tx.getCollection( entity.info().getNameInStore() );
                    switch (entity.status()) {
                        case CREATED: {
                            var affected = coll.insert( new Document[] { entity.state() } ).getAffectedCount();
                            Assert.isEqual( 1, affected );
                            submitted.createdIds.add( entity.id() );
                            break;
                        }
                        case MODIFIED: {
                            var affected = coll.update( entity.state(), false ).getAffectedCount();
                            Assert.isEqual( 1, affected );
                            submitted.modifiedIds.add( entity.id() );
                            break;
                        }
                        case REMOVED: {
                            var affected = coll.remove( (Document)entity.state() ).getAffectedCount();
                            Assert.isEqual( 1, affected );
                            submitted.removedIds.add( entity.id() );
                            break;
                        }
                        default: 
                            throw new IllegalStateException( "Status: " + entity.status() );
                    }
                }
                tx.commit();
                return submitted;
            }
            catch (Exception e) {
                tx.rollback();
                throw e;
            }
            finally {
                tx.close();
            }
        });
    }

    
    /**
     * XXX This is a *refresh* impl - change name!
     */
    @Override
    public Promise<Submitted> rollback( Iterable<Entity> entities ) {
//        for (var entity : entities) {
//            var state = (No2CompositeState)entity.context.getState();
//            return loadEntityState( state.id(), entity.getClass() );
//            
//        }
              
        var l = Sequence.of( entities ).toList();
        if (l.isEmpty()) {
            return Promise.completed( new Submitted() {}, Priority.BACKGROUND );
        }
        
        var submitted = new Submitted();
        return Promise.joined( l.size(), i -> {
            var entity = l.get( i );
            var state = (No2CompositeState)entity.context.getState();
            return loadEntityState( state.id(), entity.getClass() )
                    .map( newState -> MutablePair.of( state, (No2CompositeState)newState ) );
        })
        .map( loaded -> {
            if (loaded.right == null) {
                submitted.removedIds.add( loaded.left.id() );
            }
            else {
                submitted.modifiedIds.add( loaded.left.id() );
                loaded.left.setUnderlying( loaded.right.getUnderlying() );
                LOG.debug( "ROLLED BACK: " + loaded.left.id() );
            }
            return loaded;
        })
        .reduce( submitted, (result, next) -> {} );
    }

    
    @Override
    public void setPriority( Priority priority ) {
        LOG.warn( "No setPriority()");
    }
}
