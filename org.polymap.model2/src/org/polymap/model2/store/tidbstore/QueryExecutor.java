/* 
 * polymap.org
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package org.polymap.model2.store.tidbstore;

import static org.polymap.model2.store.tidbstore.IDBCompositeState.jsValueOf;
import static org.polymap.model2.store.tidbstore.IDBStore.TxMode.READONLY;
import static org.teavm.jso.indexeddb.IDBCursor.DIRECTION_NEXT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.indexeddb.IDBCursor;
import org.teavm.jso.indexeddb.IDBIndex;
import org.teavm.jso.indexeddb.IDBKeyRange;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

import org.polymap.model2.Entity;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.Queryable;
import org.polymap.model2.engine.TemplateProperty;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.Query.Order;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.query.grammar.CollectionQuantifier;
import org.polymap.model2.query.grammar.ComparisonPredicate;
import org.polymap.model2.query.grammar.IdPredicate;
import org.polymap.model2.query.grammar.ManyAssociationQuantifier;
import org.polymap.model2.query.grammar.PropertyEquals;
import org.polymap.model2.query.grammar.PropertyEqualsAny;
import org.polymap.model2.query.grammar.PropertyMatches;
import org.polymap.model2.query.grammar.Quantifier.Type;
import org.polymap.model2.query.grammar.TheCompositeQuantifier;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.Timer;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Query the database according to a given {@link Query}/{@link BooleanExpression}
 * using {@link IDBIndex database indexes} of {@link Queryable} properties.
 *
 * @author Falko Bräutigam
 */
class QueryExecutor {

    private static final Log LOG = LogFactory.getLog( QueryExecutor.class );

    /** Init size of result lists. */
    public static final int DEFAULT_RS_SIZE = 128;

    protected Query<?>          query;
    
    protected IDBUnitOfWork     uow;
    
    protected String            indexBaseName = "";
    
    
    public QueryExecutor( Query<?> query, IDBUnitOfWork uow ) {
        this.query = query;
        this.uow = uow;
    }

    protected QueryExecutor( Query<?> query, IDBUnitOfWork uow, String indexBaseName ) {
        this( query, uow );
        this.indexBaseName = indexBaseName;
    }

    
    protected String indexName( PropertyBase<?> prop ) {
        LOG.info( "Index: %s -> %s", prop.info().getNameInStore(), indexBaseName + prop.info().getNameInStore() ); 
        return indexBaseName + prop.info().getNameInStore();
    }

    
    public Promise<List<Object>> execute() {
        // optimze: orderBy, NO Query
        if (query.expression == Expressions.TRUE && query.orderBy != null) {
            return executeOrderByWithoutQuery();
        }
        
        var result = process( query.expression );
        
        // orderBy
        if (query.orderBy != null) {
            LOG.debug( "orderBy: %s", query.orderBy.prop.info().getName() );
            var ordered = new ArrayList<Object>( DEFAULT_RS_SIZE );
            result = result.then( ids -> uow
                    .doRequest( READONLY, query.resultType, os -> {
                        IDBIndex2 index = os.index( indexName( query.orderBy.prop ) ).cast();
                        return index.openKeyCursor( null, query.orderBy.order == Order.DESC 
                                ? IDBCursor.DIRECTION_PREVIOUS : IDBCursor.DIRECTION_NEXT );                    
                    })
                    .map( (cursor,next) -> {
                        if (!cursor.isNull()) {
                            Object primaryKey = id( cursor.getPrimaryKey() );
                            if (ids.contains( primaryKey )) {
                                ordered.add( primaryKey );
                            }
                            cursor.doContinue();
                        }
                        else {
                            LOG.debug( "Ids: %s", ids );
                            next.complete( ordered );
                        }
                    }));
        }
        // first/max
        if (query.firstResult > 0 || query.maxResults < Integer.MAX_VALUE) {
            LOG.debug( "firstResult=%d, maxResults=%d", query.firstResult, query.maxResults );
            result = result.map( ids -> { 
                var fromIndex = query.firstResult;
                var toIndex = query.maxResults < Integer.MAX_VALUE 
                        ? Math.min( fromIndex + query.maxResults, ids.size() )
                        : ids.size();
                LOG.debug( "fromIndex=%d, toIndex=%d", fromIndex, toIndex );
                return ids.subList( fromIndex, toIndex );
            });
        }
        return result;
    }
    

    /**
     * Optimized handling for orderBy without query.
     */
    protected Promise<List<Object>> executeOrderByWithoutQuery() {
        LOG.debug( "orderBy (NO query): %s, firstResult=%d, maxResults=%d", query.orderBy.prop.info().getName(), query.firstResult, query.maxResults );
        var ordered = new ArrayList<Object>( DEFAULT_RS_SIZE );
        var isFirstRun = new MutableBoolean( true );
        var count = new MutableInt( 0 );
        var timer = Timer.start();
        return uow
                .doRequest( READONLY, query.resultType, os -> {
                    IDBIndex2 index = os.index( indexName( query.orderBy.prop ) ).cast();
                    return index.openKeyCursor( null, query.orderBy.order == Order.DESC 
                            ? IDBCursor.DIRECTION_PREVIOUS : IDBCursor.DIRECTION_NEXT );                    
                })
                .map( (cursor,next) -> {
                    if (!cursor.isNull()) {
                        if (query.firstResult > 0 && isFirstRun.isTrue()) {
                            isFirstRun.setFalse();
                            cursor.advance( query.firstResult );
                        }
                        else if (count.intValue() >= query.maxResults) {
                            LOG.debug( "   Results: %d (%s)", ordered.size(), timer.elapsedHumanReadable() );
                            next.complete( ordered );                            
                        }
                        else {
                            Object primaryKey = id( cursor.getPrimaryKey() );
                            ordered.add( primaryKey );
                            count.increment();
                            cursor.doContinue();
                        }
                    }
                    else {
                        LOG.debug( "   Results: %d (%s)", ordered.size(), timer.elapsedHumanReadable() );
                        next.complete( ordered );
                    }
                });
    }


    protected Promise<List<Object>> process( BooleanExpression exp ) {
        if (exp == Expressions.TRUE) {
            return processTrue();
        }
        else if (exp instanceof IdPredicate) {
            return Promise.completed( Arrays.asList( ((IdPredicate<?>)exp).ids ) );
        }
        else if (exp instanceof PropertyEqualsAny) {
            var equalsAny = (PropertyEqualsAny<?>)exp;
            return processPropertyEqualsAny2( equalsAny.prop, equalsAny.values );
        }
        else if (exp instanceof ComparisonPredicate) {
            return processComparison( (ComparisonPredicate<?>)exp );
        }
        else if (exp instanceof ManyAssociationQuantifier) {
            return processManyAssociationQuantifier( (ManyAssociationQuantifier<?>)exp );
        }
        else if (exp instanceof TheCompositeQuantifier) {
            return processCompositeQuantifier( (TheCompositeQuantifier<?>)exp );
        }
        else if (exp instanceof CollectionQuantifier) {
            return processCollectionQuantifier( (CollectionQuantifier<?>)exp );
        }
        else {
            throw new UnsupportedOperationException( "Unhandled expression: " + exp.getClass().getSimpleName() );
        }
    }


    protected Promise<List<Object>> processComparison( ComparisonPredicate<?> exp ) {
        var result = new ArrayList<Object>( DEFAULT_RS_SIZE );
        return uow
                .doRequest( READONLY, query.resultType, os -> {
                    Assert.that( exp.prop.info().isQueryable(), "Property is not @Queryable: " + exp.prop.info().getName() );
                    IDBIndex2 index = os.index( indexName( exp.prop ) ).cast();
                    IDBKeyRange keyRange = null;
                    // eq
                    if (exp instanceof PropertyEquals) {
                        keyRange = IDBKeyRange.only( jsValueOf( exp.value ) );
                    }
                    // matches
                    else if (exp instanceof PropertyMatches) {
                        var matches = (PropertyMatches<?>)exp;
                        Assert.that( exp.value instanceof String, "Only String values are supported for PropertyMatches: " + exp.prop );
                        var value = (String)exp.value;
                        Assert.that( value.endsWith( String.valueOf( matches.multiWildcard ) ), "Only 'starts-with' is supported for PropertyMatches: " + exp.prop );
                        Assert.that( !value.contains( String.valueOf( matches.singleWildcard ) ), "Single wildcard is not supported for PropertyMatches: " + exp.prop );
                        var base = value.substring( 0, value.length()-1 );
                        LOG.info( "MATCHES: %s", base );
                        keyRange = IDBKeyRange.bound( jsValueOf( base ), jsValueOf( base + '\uffff' ), false, true );
                    }
                    else {
                        throw new UnsupportedOperationException( "Unsupported ComparisonPredicate: " + exp.getClass().getSimpleName() );
                    }
                    return index.openKeyCursor( keyRange, DIRECTION_NEXT );
                })
                .map( (cursor, next) -> {
                    if (!cursor.isNull()) {
                        Object id = id( cursor.getPrimaryKey() );
                        var value = IDBCompositeState.javaValueOf( cursor.getKey(), exp.prop.info() );
                        LOG.debug( "Comparison: value=%s", value );
                        result.add( id );
                        cursor.doContinue();
                    }
                    else {
                        LOG.debug( "Comparison: ids: %s", result );
                        next.complete( result );
                    }
                });
    }

    
    protected Promise<List<Object>> processPropertyEqualsAny( PropertyEqualsAny<?> exp ) {
        if (exp.values.isEmpty()) {
            return Promise.completed( Collections.emptyList() );
        }
        
        var result = new ArrayList<Object>( DEFAULT_RS_SIZE );
        return uow
                .doRequest( READONLY, query.resultType, os -> {
                    Assert.that( exp.prop.info().isQueryable(), "Property is not @Queryable: " + exp.prop.info().getName() );
                    IDBIndex2 index = os.index( indexName( exp.prop ) ).cast();
                    Comparable<Object> lower = null;
                    Comparable<Object> upper = null;
                    for (var v : exp.values) {
                        Comparable<Object> comparable = Comparable.class.cast( v );
                        lower = lower == null || lower.compareTo( v ) > 0 ? comparable : lower;
                        upper = upper == null || upper.compareTo( v ) < 0 ? comparable : upper;
                    }
                    LOG.debug( "EqualsAny: %s <= x <= %s", lower, upper );
                    IDBKeyRange keyRange = IDBKeyRange.bound( jsValueOf( lower ), jsValueOf( upper ), false, false );
                    return index.openKeyCursor( keyRange, DIRECTION_NEXT );
                })
                .map( (cursor, next) -> {
                    if (!cursor.isNull()) {
                        var value = IDBCompositeState.javaValueOf( cursor.getKey(), exp.prop.info() );
                        LOG.debug( "EqualsAny: value=%s", value );
                        if (exp.values.contains( value )) {
                            result.add( id( cursor.getPrimaryKey() ) );
                        }
                        cursor.doContinue();
                    }
                    else {
                        LOG.debug( "EqualsAny: ids: %s", result );
                        next.complete( result );
                    }
                });
    }


    /**
     * Depends on: IDBCursor.doContinue( nextKey ); 
     */
    protected Promise<List<Object>> processPropertyEqualsAny2( TemplateProperty<?> prop, Collection<?> values ) {
        if (values.isEmpty()) {
            return Promise.completed( Collections.emptyList() );
        }
        var result = new ArrayList<Object>( DEFAULT_RS_SIZE );
        var searchValues = new SortedValuesIterator( values );
        return uow
                .doRequest( READONLY, query.resultType, os -> {
                    Assert.that( prop.info().isQueryable(), "Property is not @Queryable: " + prop.info().getName() );
                    IDBIndex2 index = os.index( indexName( prop ) ).cast();
                    return index.openKeyCursor( null, DIRECTION_NEXT );
                })
                .map( (cursor, next) -> {
                    if (!cursor.isNull()) {
                        var value = IDBCompositeState.javaValueOf( cursor.getKey(), prop.info() );
                        LOG.debug( "EqualsAny: value=%s - searchValue=%s", value, searchValues.peek );
                        
                        while (searchValues.peek.compareTo( value ) < 0) {
                            if (searchValues.hasMore()) {
                                LOG.debug( "EqualsAny:   skipping searchValue=%s", searchValues.peek );
                                searchValues.next();
                            }
                            else {
                                // no more values to search for
                                LOG.debug( "EqualsAny: ids: %s", result );
                                next.complete( result );
                                return;                                
                            }
                        }
                        
                        if (value.equals( searchValues.peek )) {
                            LOG.debug( "EqualsAny:   found: %s == %s", value, searchValues.peek );
                            result.add( id( cursor.getPrimaryKey() ) );
                            cursor.doContinue();  // check for more entries for this searchValue
                        }
                        else {
                            LOG.debug( "EqualsAny:   continue: %s", searchValues.peek );
                            cursor.doContinue( IDBCompositeState.jsValueOf( searchValues.peek ) );                            
                        }
                    }
                    else {
                        // no more index entries to check
                        LOG.debug( "EqualsAny: ids: %s", result );
                        next.complete( result );
                    }
                });
    }


    protected class SortedValuesIterator {
        public Comparable<Object> peek;
        private Iterator<Object> it;
        
        public SortedValuesIterator( Collection<?> values ) {
            var sorted = new TreeSet<Object>( values );
            LOG.debug( "EqualsAny: %s", sorted );
            it = sorted.iterator();
            next();
        }
        public boolean hasMore() {
            return it.hasNext();
        }
        @SuppressWarnings("unchecked")
        public void next() {
            peek = it.hasNext() ? (Comparable<Object>)it.next() : null;            
        }
    }


    @SuppressWarnings("unchecked")
    protected Promise<List<Object>> processManyAssociationQuantifier( ManyAssociationQuantifier<?> exp ) {
        Assert.that( exp.type == Type.ANY, "ALL quantifier is not yet supported" );
        LOG.info( "AnyOf: subQuery: %s", exp.subExp() );
        var subType = exp.prop.info().getType();
        var subQuery = new SubQuery<>( subType ).where( exp.subExp() );
        return new QueryExecutor( subQuery, uow ).execute()
                .then( rs -> processPropertyEqualsAny2( (TemplateProperty<?>)exp.prop, rs ) );
    }


    protected Promise<List<Object>> processCompositeQuantifier( TheCompositeQuantifier<?> exp ) {
        Assert.that( exp.type == Type.THE_ONLY, "Wrong quantifier: " + exp.type );
        var subType = exp.prop.info().getType();
        var subQuery = new SubQuery<>( query.resultType() ).where( exp.subExp() );
        LOG.info( "TheComposite: %s: %s", subType, exp.subExp() );
        return new QueryExecutor( subQuery, uow, indexName( exp.prop ) + "." ).execute();
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Promise<List<Object>> processCollectionQuantifier( CollectionQuantifier exp ) {
        Assert.that( exp.type == Type.ANY, "ALL quantifier is not yet supported" );
        return processComparison( new PropertyEquals( (TemplateProperty)exp.prop, exp.value ) );
    }


    protected Promise<List<Object>> processTrue() {
        var result = new ArrayList<Object>( DEFAULT_RS_SIZE );
        return uow
                .doRequest( READONLY, query.resultType, os -> os.openKeyCursor() )
                .map( (cursor, next) -> {
                    if (!cursor.isNull()) {
                        Object id = id( cursor.getKey() );
                        result.add( id );
                        cursor.doContinue();
                    }
                    else {
                        LOG.debug( "ALL: ids: %s", result );
                        next.complete( result );
                    }
                });
    }
    
    
    protected Object id( JSObject js ) {
        return ((JSString)js).stringValue();  // XXX        
    }


    /**
     * 
     */
    protected static class SubQuery<T extends Entity>
            extends Query<T> {
        
        public SubQuery( Class<T> resultType ) {
            super( resultType );
        }
    
        @Override public Promise<Opt<T>> execute() { throw new RuntimeException( "do not call" ); }
    }
    
}
