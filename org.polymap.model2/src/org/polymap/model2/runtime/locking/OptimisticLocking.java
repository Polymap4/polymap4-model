/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.runtime.locking;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.ConcurrentEntityModificationException;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.store.CloneCompositeStateSupport;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.CompositeStateReference;
import org.polymap.model2.store.StoreDecorator;
import org.polymap.model2.store.StoreResultSet;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreSPI;
import org.polymap.model2.store.StoreUnitOfWork;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * This {@link StoreDecorator} provides a simple check for concurrent modifications
 * from different UnitOfWork instances in this JVM. The check fails on
 * {@link StoreUnitOfWork#submit(Iterable)}.
 * <p/>
 * This implementation holds all versions in memory and never checks the underlying
 * store for concurrent modifications. So the check is fast but the table of versions
 * grows with the number of modified entities. This implementation does not detect
 * modification of the underlying store by a second party.
 * <p/>
 * The implementation relies on globally unique ids across all Entity types in the
 * repository.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class OptimisticLocking
        extends StoreDecorator
        implements StoreSPI {

    private static Log log = LogFactory.getLog( OptimisticLocking.class );
    
    private StoreRuntimeContext             context;
    
    private ConcurrentMap<Object,Integer>   storeVersions = new ConcurrentHashMap( 256, 0.75f, 4 );

    
    public OptimisticLocking( StoreSPI store ) {
        super( store );
    }

    @Override
    public void init( @SuppressWarnings("hiding") StoreRuntimeContext context ) {
        store.init( context );
        this.context = context;
    }

    @Override
    public StoreUnitOfWork createUnitOfWork() {
        StoreUnitOfWork suow = store.createUnitOfWork();
        return suow instanceof CloneCompositeStateSupport
                ? new OptimisticLockingSuow2( suow )
                : new OptimisticLockingSuow( suow );
    }


    /**
     * 
     */
    class OptimisticLockingSuow
            extends UnitOfWorkDecorator
            implements StoreUnitOfWork {
    
        protected ConcurrentMap<Object,Integer> loadedVersions = new ConcurrentHashMap( 256, 0.75f, 2 );
        
        private List<Entity>                    prepared;

        
        public OptimisticLockingSuow( StoreUnitOfWork suow ) {
            super( suow );
        }

        
        @Override
        public void prepareCommit( Iterable<Entity> modified ) throws Exception {
            // check only
            prepared = new ArrayList( loadedVersions.size() );
            for (Entity entity : modified) {
                if (entity.status() == EntityStatus.MODIFIED || entity.status() == EntityStatus.REMOVED) {
                    Integer loadedVersion = loadedVersions.get( entity.id() );
                    Integer storeVersion = storeVersions.get( entity.id() );
                    if (!Objects.equals( storeVersion, loadedVersion )) {
                        throw new ConcurrentEntityModificationException( 
                                "Entity has been modified be another UnitOfWork: " + entity +
                                "\r\n\t(loadedVersion=" + loadedVersion + ", storedVersion=" + storeVersion + ")", 
                                singletonList( entity ) );
                    }
                    prepared.add( entity );
                }
            }

            // delegate
            suow.submit( modified );
        }

        
        @Override
        public void commit() {
            assert prepared != null : "no prepareCommit() before commit()!";
            
            // check and set versions
            for (Entity entity : prepared) {
                Integer loadedVersion = loadedVersions.get( entity.id() );
                Integer newVersion = loadedVersion == null 
                        ? new Integer( 1 ) 
                        : new Integer( loadedVersion.intValue() + 1 );

                Integer storeVersion = storeVersions.put( entity.id(), newVersion );
                if (!Objects.equals( storeVersion, loadedVersion )) {
                    storeVersions.put( entity.id(), storeVersion );
                    throw new ConcurrentEntityModificationException( 
                            "Entity has been modified AFTER prepare(): " + entity.id(), 
                            singletonList( entity ) );
                }
                // update also loadedVersions for subsequent commits
                loadedVersions.put( entity.id(), newVersion );                
            }
            prepared = null;
            
            // delegate
            suow.commit();
        }

        
        @Override
        public void rollback( Iterable<Entity> modified ) {
            super.rollback( modified );
            
            // reset versions
            for (Entity entity : modified) {
                Integer storeVersion = storeVersions.get( entity.id() );
                // newly created entities do not have storeVersion
                if (storeVersion != null) {
                    loadedVersions.put( entity.id(), storeVersion );
                }
                else {
                    loadedVersions.remove( entity.id() );
                }
            }
            prepared = null;
        }


        protected void registerLoadedVersion( Object id ) {
            Integer version = storeVersions.get( id );
            if (version != null) {
                loadedVersions.put( id, version );
            }
        }
        
        
        @Override
        public <T extends Entity> CompositeState loadEntityState( Object id, Class<T> entityClass ) {
            CompositeState result = suow.loadEntityState( id, entityClass );
            registerLoadedVersion( id );
            return result;
        }

        
        @Override
        public <T extends Entity> CompositeState adoptEntityState( Object state, Class<T> entityClass ) {
            CompositeState result = suow.adoptEntityState( state, entityClass );
            registerLoadedVersion( result.id() );
            return result;
        }


        @Override
        public StoreResultSet executeQuery( Query query ) {
            StoreResultSet delegate = suow.executeQuery( query );
            return new StoreResultSet() {
                @Override
                public CompositeStateReference next() {
                    CompositeStateReference result = delegate.next();
                    CompositeState preloaded = result.get();
                    if (preloaded != null) {
                        registerLoadedVersion( preloaded.id() );
                    }
                    return result;
                }
                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }
                @Override
                public int size() {
                    return delegate.size();
                }
                @Override
                public void close() {
                    delegate.close();
                }
            };
        }
    }
        
    
    /**
     * 
     */
    class OptimisticLockingSuow2
            extends OptimisticLockingSuow
            implements CloneCompositeStateSupport {

        public OptimisticLockingSuow2( StoreUnitOfWork suow ) {
            super( suow );
        }

        protected CloneCompositeStateSupport suow() {
            return (CloneCompositeStateSupport)suow;
        }
        
        @Override
        public CompositeState cloneEntityState( CompositeState state ) {
            return suow().cloneEntityState( state ); 
        }

        @Override
        public void reincorparateEntityState( CompositeState state, CompositeState clonedState ) {
            suow().reincorparateEntityState( state, clonedState );
        }
        
    }
    
}
