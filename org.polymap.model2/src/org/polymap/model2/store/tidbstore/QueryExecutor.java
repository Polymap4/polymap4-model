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
import java.util.Collections;
import java.util.List;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.indexeddb.IDBCursor;
import org.teavm.jso.indexeddb.IDBIndex;
import org.teavm.jso.indexeddb.IDBKeyRange;

import org.polymap.model2.Queryable;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.Query.Order;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.query.grammar.ComparisonPredicate;
import org.polymap.model2.query.grammar.IdPredicate;
import org.polymap.model2.query.grammar.PropertyEquals;
import org.polymap.model2.query.grammar.PropertyEqualsAny;
import org.polymap.model2.query.grammar.PropertyMatches;

import areca.common.Assert;
import areca.common.Promise;
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
    
    protected Query<?>          query;
    
    protected IDBUnitOfWork     uow;
    
    
    public QueryExecutor( Query<?> query, IDBUnitOfWork uow ) {
        this.query = query;
        this.uow = uow;
    }


    public Promise<List<Object>> execute() {
        var result = process( query.expression );
        
        // orderBy
        if (query.orderBy != null) {
            LOG.debug( "orderBy: %s", query.orderBy.prop.info().getName() );
            var ordered = new ArrayList<Object>( 128 );
            result = result.then( ids -> uow
                    .doRequest( READONLY, query.resultType, os -> {
                        IDBIndex2 index = os.index( query.orderBy.prop.info().getNameInStore() ).cast();
                        return index.openKeyCursor( null, query.orderBy.order == Order.DESC 
                                ? IDBCursor.DIRECTION_PREVIOUS : IDBCursor.DIRECTION_NEXT );                    
                    })
                    .map( (request,next) -> {
                        var cursor = request.getResult();
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
                var toIndex = Math.min( fromIndex + query.maxResults, ids.size() );
                return ids.subList( fromIndex, toIndex );
            });
        }
        return result;
    }
    
    
    protected Promise<List<Object>> process( BooleanExpression exp ) {
        if (exp == Expressions.TRUE) {
            return processTrue();
        }
        else if (exp instanceof IdPredicate) {
            return Promise.completed( Arrays.asList( ((IdPredicate<?>)exp).ids ) );
        }
        else if (exp instanceof PropertyEqualsAny) {
            return processPropertyEqualsAny( (PropertyEqualsAny<?>)exp );
        }
        else if (exp instanceof ComparisonPredicate) {
            return processComparison( (ComparisonPredicate<?>)exp );
        }
        else {
            throw new UnsupportedOperationException( "Unhandled expression: " + exp.getClass().getSimpleName() );
        }
    }


    protected Promise<List<Object>> processComparison( ComparisonPredicate<?> exp ) {
        var result = new ArrayList<Object>( 256 );
        return uow
                .doRequest( READONLY, query.resultType, os -> {
                    Assert.that( exp.prop.info().isQueryable(), "Property is not @Queryable: " + exp.prop.info().getName() );
                    IDBIndex2 index = os.index( exp.prop.info().getNameInStore() ).cast();
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
                .map( (request, next) -> {
                    var cursor = request.getResult();
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
        
        var result = new ArrayList<Object>( 256 );
        return uow
                .doRequest( READONLY, query.resultType, os -> {
                    Assert.that( exp.prop.info().isQueryable(), "Property is not @Queryable: " + exp.prop.info().getName() );
                    IDBIndex2 index = os.index( exp.prop.info().getNameInStore() ).cast();
                    Comparable<Object> lower = null;
                    Comparable<Object> upper = null;
                    for (var v : exp.values) {
                        Comparable<Object> comparable = Comparable.class.cast( v );
                        lower = lower == null || lower.compareTo( v ) > 0 ? comparable : lower;
                        upper = upper == null || upper.compareTo( v ) < 0 ? comparable : upper;
                    }
                    LOG.info( "EqualsAny: %s <= x <= %s", lower, upper );
                    IDBKeyRange keyRange = IDBKeyRange.bound( jsValueOf( lower ), jsValueOf( upper ), false, false );
                    return index.openKeyCursor( keyRange, DIRECTION_NEXT );
                })
                .map( (request, next) -> {
                    var cursor = request.getResult();
                    if (!cursor.isNull()) {
                        var value = IDBCompositeState.javaValueOf( cursor.getKey(), exp.prop.info() );
                        LOG.info( "EqualsAny: value=%s", value );
                        if (exp.values.contains( value )) {
                            result.add( id( cursor.getPrimaryKey() ) );
                        }
                        cursor.doContinue();
                    }
                    else {
                        LOG.info( "EqualsAny: ids: %s", result );
                        next.complete( result );
                    }
                });
    }


    protected Promise<List<Object>> processTrue() {
        var result = new ArrayList<Object>( 256 );
        return uow
                .doRequest( READONLY, query.resultType, os -> os.openCursor() ) // FIXME key cursor
                .map( (request, next) -> {
                    var cursor = request.getResult();
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
    
}
