/* 
 * polymap.org
 * Copyright 2012, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.store.geotools;

import static java.util.Collections.singleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import java.io.IOException;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;

import org.polymap.core.data.FeatureFactory;
import org.polymap.core.runtime.Closer;

import org.polymap.model2.Entity;
import org.polymap.model2.NameInStore;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.ConcurrentEntityModificationException;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.store.CompositeState;
import org.polymap.model2.store.CompositeStateReference;
import org.polymap.model2.store.StoreResultSet;
import org.polymap.model2.store.StoreRuntimeContext;
import org.polymap.model2.store.StoreUnitOfWork;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class FeatureStoreUnitOfWork
        implements StoreUnitOfWork {

    private static Log log = LogFactory.getLog( FeatureStoreUnitOfWork.class );
    
    public static final FilterFactory   ff = CommonFactoryFinder.getFilterFactory( null );
    
    private static final Transaction    TX_FAILED = new DefaultTransaction( "__failed__" );
    
    private FeatureStoreAdapter         store;

    private ConcurrentMap<FeatureId,FeatureModifications> 
                                        modifications = new ConcurrentHashMap( 1024, 0.75f, 4 );
    
    private Transaction                 tx;
    
    /** Never evicting cache of used {@link FeatureSource} instances. */
    private LoadingCache<Class<? extends Entity>,FeatureSource> featureSources;
    
    
    protected FeatureStoreUnitOfWork( StoreRuntimeContext context, FeatureStoreAdapter store ) {
        this.store = store;

        // XXX why use Guave cache here anyway?
        this.featureSources = CacheBuilder.newBuilder().build( new CacheLoader<Class<?>,FeatureSource>() {
            public FeatureSource load( Class<?> entityClass ) throws Exception {
                // name in store
                String typeName = entityClass.getAnnotation( NameInStore.class ) != null
                        ? entityClass.getAnnotation( NameInStore.class ).value()
                        : entityClass.getSimpleName();
                        
                NameImpl name = new NameImpl( typeName );
                return FeatureStoreUnitOfWork.this.store.getStore().getFeatureSource( name );
            }
        });
    }

    
    public FeatureSource featureSource( Class<? extends Entity> entityClass ) {
        try {
            // why use Guave cache here anyway?
            return featureSources.get( entityClass );
        }
        catch (ExecutionException e) {
            throw new RuntimeException( e );
        }
    }


    @Override
    public <T extends Entity> CompositeState loadEntityState( Object id, Class<T> entityClass ) {
        FeatureSource fs = featureSource( entityClass );
        FeatureIterator it = null;
        try {
            FeatureCollection features = fs.getFeatures(
                    ff.id( Collections.singleton( ff.featureId( (String)id ) ) ) );
            it = features.features();
            Feature feature = it.hasNext() ? it.next() : null;
            //assert feature != null : "Possible BUG: No feature found for id: " + id;
            return feature != null ? new FeatureCompositeState( feature, this ) : null;
        }
        catch (Exception e) {
            throw new ModelRuntimeException( e );
        }
        finally {
            if (it != null) { it.close(); }
        }
    }


    @Override
    public <T extends Entity> CompositeState adoptEntityState( Object state, Class<T> entityClass ) {
        return new FeatureCompositeState( (Feature)state, this );
    }


    @Override
    public <T extends Entity> CompositeState newEntityState( Object id, Class<T> entityClass ) {
        // find schema for entity
        FeatureSource fs = featureSource( entityClass );
        FeatureType schema = fs.getSchema();
        
        // create feature
        Feature feature = null;
        if (fs instanceof FeatureFactory) {
            feature = ((FeatureFactory)fs).newFeature( (String)id );
            assert !(schema instanceof SimpleFeatureType) || feature instanceof SimpleFeature; 
        }
        else if (schema instanceof SimpleFeatureType) {
            feature = SimpleFeatureBuilder.build( (SimpleFeatureType)schema, 
                    Collections.EMPTY_LIST, (String)id );
        }
        else {
            throw new UnsupportedOperationException( "Unable to build feature for schema: " + schema );
        }
        feature.getUserData().put( "__created__", Boolean.TRUE );
        return new FeatureCompositeState( feature, this );
    }


    @Override
    public StoreResultSet executeQuery( Query query ) {
        assert query.expression == null 
                || query.expression == Expressions.TRUE 
                || query.expression instanceof FilterWrapper : "Wrong query expression type: " + query.expression;
        try {
            // schema
            FeatureSource fs = featureSource( query.resultType() );
            FeatureType schema = fs.getSchema();

            // features
            org.geotools.data.Query featureQuery = new org.geotools.data.Query( schema.getName().getLocalPart() );
            featureQuery.setFilter( query.expression != null && query.expression != Expressions.TRUE 
                    ? ((FilterWrapper)query.expression).filter 
                    : Filter.INCLUDE );
            // load all properties as we actually use the features via the #found buffer
            //featureQuery.setPropertyNames( new String[] {} );
            featureQuery.setStartIndex( query.firstResult );
            featureQuery.setMaxFeatures( query.maxResults );

            return new StoreResultSet() {
                private FeatureCollection   features = fs.getFeatures( featureQuery );
                private FeatureIterator     it = features.features();

                @Override
                public boolean hasNext() {
                    if (it == null) {
                        return false;
                    }
                    boolean result = it.hasNext();
                    if (result == false) {
                        // fast close connection
                        close();
                    }
                    return result;
                }
                @Override
                public CompositeStateReference next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return new CompositeStateReference() {
                        private Feature feature = it.next();
                        @Override
                        public Object id() {
                            return feature.getIdentifier().getID();
                        }
                        @Override
                        public CompositeState get() {
                            return new FeatureCompositeState( feature, FeatureStoreUnitOfWork.this );
                        }
                    };
                }
                @Override
                public int size() {
                    return features.size();
                }
                @Override
                public void close() {
                    it = Closer.create().closeAndNull( it );
                }
                @Override
                protected void finalize() throws Throwable {
                    close();
                }
            };
        }
        catch (IOException e) {
            throw new ModelRuntimeException( e );
        }
    }


    @Override
    public void prepareCommit( Iterable<Entity> modified )
    throws IOException, ConcurrentEntityModificationException {
        assert tx == null;
        
        tx = new DefaultTransaction( getClass().getName() + " Transaction" );
        try {
            apply( modified );
        }
        catch (IOException e) {
            Transaction tx2 = tx;
            tx = TX_FAILED;
            tx2.rollback();
            tx2.close();
            throw e;
        }
    }


    public void commit() throws ModelRuntimeException {
        assert tx != null && tx != TX_FAILED;
        try {
            tx.commit();
            tx.close();
            tx = null;

            for (FeatureSource fs : featureSources.asMap().values()) {
//                log.debug( "Checking features: " + fs );
                ((FeatureStore)fs).setTransaction( Transaction.AUTO_COMMIT );
//                fs.getFeatures().accepts( new FeatureVisitor() {
//                    public void visit( Feature feature ) {
//                        log.debug( "FeatureId: " + feature.getIdentifier() );
//                    }
//                }, null );
            }

            modifications.clear();            
            featureSources.asMap().clear();
        }
        catch (Exception e) {
            throw new ModelRuntimeException( e );
        }
    }


    @Override
    public void rollback( Iterable<Entity> modified ) {
        throw new RuntimeException( "Rolling back entity states is not yet supported." );
        
//        if (tx != null && tx != TX_FAILED) {
//            try {
//                tx.rollback();
//                tx.close();
//                tx = null;
//
//                for (FeatureSource fs : featureSources.asMap().values()) {
//                    //                log.debug( "Checking features: " + fs );
//                    ((FeatureStore)fs).setTransaction( Transaction.AUTO_COMMIT );
//                    //                fs.getFeatures().accepts( new FeatureVisitor() {
//                    //                    public void visit( Feature feature ) {
//                    //                        log.debug( "FeatureId: " + feature.getIdentifier() );
//                    //                    }
//                    //                }, null );
//                }
//
//                modifications.clear();            
//                featureSources.asMap().clear();
//            }
//            catch (Exception e) {
//                throw new ModelRuntimeException( e );
//            }
//        }
    }


    public void close() {
        if (tx != null && tx != TX_FAILED) {
            try {
                tx.rollback();
                tx = null;
            }
            catch (IOException e) {
                throw new RuntimeException( e );
            }
        }
        store = null;
        featureSources = null;
        modifications = null;
    }


    protected void apply( Iterable<Entity> loaded ) throws IOException {  //, ConcurrentEntityModificationException {
        assert tx != null;
        // find created, modified, removed
        Map<Class,MemoryFeatureCollection> created = new HashMap();
        Map<Class,Set<FeatureId>> removed = new HashMap();

        for (Entity entity : loaded) {
            Feature feature = (Feature)entity.state();

            // created
            if (entity.status() == EntityStatus.CREATED) {
                // it in case of exception while prepare the mark is removed to early; but
                // it should not cause trouble as potential subsequent modifications are
                // just send twice to the store, one in create and the equal modification
                feature.getUserData().remove( "__created__" );
                
                MemoryFeatureCollection coll = created.get( entity.getClass() );
                if (coll == null) {
                    coll = new MemoryFeatureCollection( null, null );
                    created.put( entity.getClass(), coll );
                }
                coll.add( feature );
            }
            // removed
            else if (entity.status() == EntityStatus.REMOVED) {
                //assert feature.getUserData().get( "__created__" ) == null;
                Set<FeatureId> fids = removed.get( entity.getClass() );
                if (fids == null) {
                    fids = new HashSet( 1024 );
                    removed.put( entity.getClass(), fids );
                }
                fids.add( feature.getIdentifier() );
            }
        }

        // write created
        for (Entry<Class,MemoryFeatureCollection> entry : created.entrySet()) {
            log.debug( "    Adding feature(s) of " + entry.getKey().getSimpleName() + " : " + entry.getValue().size() );
            FeatureStore fs = (FeatureStore)featureSource( entry.getKey() );
            if (tx != fs.getTransaction()) {
                fs.setTransaction( tx );
            }
            fs.addFeatures( entry.getValue() );
            
//            fs.getFeatures().accepts( new FeatureVisitor() {
//                public void visit( Feature feature ) {
//                    log.debug( "        fid: " + feature.getIdentifier() );
//                }
//            }, null );
        }

        // write removed
        for (Entry<Class,Set<FeatureId>> entry : removed.entrySet()) {
            log.debug( "    Removing feature(s) of " + entry.getKey().getSimpleName() + " : " + entry.getValue().size() );
            FeatureStore fs = (FeatureStore)featureSource( entry.getKey() );
            if (tx != fs.getTransaction()) {
                fs.setTransaction( tx );
            }
            fs.removeFeatures( ff.id( entry.getValue() ) );
        }
        
        // write modified
        log.debug( "    Modified feature(s): " + modifications.size() );
        for (Entry<FeatureId,FeatureModifications> entry : modifications.entrySet()) {
            FeatureModifications mods = entry.getValue();
            assert mods.feature.getUserData().get( "__created__" ) == null;
            
            FeatureStore fs = (FeatureStore)store.getStore().getFeatureSource( 
                    mods.feature.getType().getName() );

            // any other than no or my tx is an error
            assert fs.getTransaction() == Transaction.AUTO_COMMIT || tx == fs.getTransaction();

            if (tx != fs.getTransaction()) {
                fs.setTransaction( tx );
            }
            AttributeDescriptor[] atts = mods.types();
            Object values[] = mods.values2();
            
            log.trace( "    Modifying feature: " + mods.feature.getIdentifier() ); 
            for (int i=0; i<atts.length; i++) {
                log.trace( "        attribute: " + atts[i].getLocalName() + " = " + values[i] );
            }
            fs.modifyFeatures( mods.types(), mods.values2(), 
                    ff.id( singleton( mods.feature.getIdentifier() ) ) );
        }
    }


    protected void markPropertyModified( Feature feature, AttributeDescriptor att, Object value) {
        if (feature.getUserData().get( "__created__" ) == null) {
            FeatureModifications fm = modifications.get( feature.getIdentifier() );
            if (fm == null) {
                fm = new FeatureModifications( feature );
                FeatureModifications other = modifications.putIfAbsent( feature.getIdentifier(), fm );
                fm = other != null ? other : fm;
            }
            fm.put( att, value );
        }
    }

    
    /**
     * 
     */
    class FeatureModifications
            extends HashMap<AttributeDescriptor,Object> {

        Feature     feature;
        
        public FeatureModifications( Feature feature ) {
            this.feature = feature;
        }
        
        public AttributeDescriptor[] types() {
            return Iterables.toArray( keySet(), AttributeDescriptor.class );
        }
        
        public Object[] values2() {
            return Iterables.toArray( values(), Object.class );            
        }
    }
    
}
