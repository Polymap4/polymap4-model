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

import org.dizitart.no2.collection.NitriteId;
import org.dizitart.no2.filters.Filter;
import org.dizitart.no2.filters.FluentFilter;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.query.grammar.ComparisonPredicate;
import org.polymap.model2.query.grammar.Conjunction;
import org.polymap.model2.query.grammar.Disjunction;
import org.polymap.model2.query.grammar.IdPredicate;
import org.polymap.model2.query.grammar.Negation;
import org.polymap.model2.query.grammar.PropertyEquals;
import org.polymap.model2.query.grammar.PropertyEqualsAny;
import org.polymap.model2.query.grammar.PropertyMatches;
import org.polymap.model2.query.grammar.PropertyNotEquals;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 * @author Falko
 */
public class FilterBuilder {

    private static final Log LOG = LogFactory.getLog( FilterBuilder.class );
    
    private Query<?> query;

    public FilterBuilder( Query<?> query ) {
        this.query = query;
    }

    public Filter build() {
        return build( query.expression );
    }

    protected Filter build( BooleanExpression expr ) {
        if (expr == Expressions.TRUE) {
            return Filter.ALL;
        }
        else if (expr == Expressions.FALSE) {
            return Filter.ALL.not();
        }
        // AND
        else if (expr instanceof Conjunction) {
            var children = expr.children().map( child -> build( child ) ).toArray( Filter[]::new );
            return Filter.and( children );
        }
        // OR
        else if (expr instanceof Disjunction) {
            var children = expr.children().map( child -> build( child ) ).toArray( Filter[]::new );
            return Filter.or( children );
        }
        // NOT
        else if (expr instanceof Negation) {
            return build( expr.children[0] ).not();
        }
        // id
        else if (expr instanceof IdPredicate) {
            Object[] ids = ((IdPredicate<?>)expr).ids;
            Assert.isEqual( 1, ids.length, "Multiple IDs are not yet implemented" );
            return Filter.byId( NitriteId.createId( (String)ids[0] ) );
        }
        // comparison
        else if (expr instanceof ComparisonPredicate) {
            var comparison = (ComparisonPredicate<?>)expr;
            var result = FluentFilter.where( comparison.prop.info().getNameInStore() );
            // eq
            if (expr instanceof PropertyEquals) {
                return result.eq( comparison.value );
            }
            // not eq
            else if (expr instanceof PropertyNotEquals) {
                return result.notEq( comparison.value );
            }
            // in
            else if (expr instanceof PropertyEqualsAny) {
                return result.in( ((PropertyEqualsAny<?>)comparison).values.toArray( Comparable[]::new ) );
            }
            // matches
            else if (expr instanceof PropertyMatches) {
                var matches = (PropertyMatches<?>)expr;
                Assert.that( matches.value instanceof String,
                        "Only String values are supported for PropertyMatches: " + matches.prop );
                
                var value = (String)matches.value;
                Assert.that( value.endsWith( String.valueOf( matches.multiWildcard ) ),
                        "Only 'starts-with' is supported for PropertyMatches: " + matches.prop );
                Assert.that( !value.contains( String.valueOf( matches.singleWildcard ) ),
                        "Single wildcard is not supported for PropertyMatches: " + matches.prop );
                
                var base = value.substring( 0, value.length()-1 );
                LOG.debug( "MATCHES: %s", base );
                return result.between( base, base + '\uffff', true, false );
            }
            else {
                throw new RuntimeException( "Not yet implemented: " + expr );                
            }
        }
        // not yet
        else {
            throw new RuntimeException( "Not yet implemented: " + expr );
        }
        
    }
}
