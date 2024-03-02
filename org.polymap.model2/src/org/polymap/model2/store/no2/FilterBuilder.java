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

import java.util.ArrayList;

import org.dizitart.no2.collection.NitriteId;
import org.dizitart.no2.common.Constants;
import org.dizitart.no2.filters.Filter;
import org.dizitart.no2.filters.FluentFilter;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.query.grammar.ComparisonPredicate;
import org.polymap.model2.query.grammar.Conjunction;
import org.polymap.model2.query.grammar.Disjunction;
import org.polymap.model2.query.grammar.IdPredicate;
import org.polymap.model2.query.grammar.ManyAssociationQuantifier;
import org.polymap.model2.query.grammar.Negation;
import org.polymap.model2.query.grammar.PropertyEquals;
import org.polymap.model2.query.grammar.PropertyEqualsAny;
import org.polymap.model2.query.grammar.PropertyMatches;
import org.polymap.model2.query.grammar.PropertyNotEquals;
import org.polymap.model2.query.grammar.Quantifier.Type;
import org.polymap.model2.query.grammar.TheCompositeQuantifier;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Opt;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * 
 * @author Falko
 */
public class FilterBuilder {

    private static final Log LOG = LogFactory.getLog( FilterBuilder.class );
    
    private Query<?>        query;
    
    private No2UnitOfWork   uow;

    public FilterBuilder( Query<?> query, No2UnitOfWork uow ) {
        this.query = query;
        this.uow = uow;
    }

    public Filter build() {
        return build( query.expression, "" );
    }

    protected Filter build( BooleanExpression expr, String fieldNameBase ) {
        if (expr == Expressions.TRUE) {
            return Filter.ALL;
        }
        else if (expr == Expressions.FALSE) {
            return Filter.ALL.not();
        }
        // AND
        else if (expr instanceof Conjunction) {
            var children = expr.children().map( child -> build( child, fieldNameBase ) ).toArray( Filter[]::new );
            return Filter.and( children );
        }
        // OR
        else if (expr instanceof Disjunction) {
            var children = expr.children().map( child -> build( child, fieldNameBase ) ).toArray( Filter[]::new );
            return Filter.or( children );
        }
        // NOT
        else if (expr instanceof Negation) {
            return build( expr.children[0], fieldNameBase ).not();
        }
        // id
        else if (expr instanceof IdPredicate) {
            Object[] ids = ((IdPredicate<?>)expr).ids;
            if (ids.length == 1) {
                return Filter.byId( NitriteId.createId( (String)ids[0] ) );
            }
            else {
                var _ids = Sequence.of( ids ).map( id -> (String)id ).toArray( String[]::new );
                return FluentFilter.where( Constants.DOC_ID ).in( _ids );
            }
        }
        // comparison
        else if (expr instanceof ComparisonPredicate) {
            var comparison = (ComparisonPredicate<?>)expr;
            var result = FluentFilter.where( fieldNameBase + comparison.prop.info().getNameInStore() );
            Assert.that( comparison.prop.info().isQueryable(), "Property is not @Queryable: " + comparison.prop );
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
                
                // fulltext
                if (matches.prop.info().getAnnotation( Fulltext.class ) != null) {
                    return result.text( (String)matches.value );
                }
                // other: between
                else {
                    var value = (String)matches.value;
                    Assert.that( value.endsWith( String.valueOf( matches.multiWildcard ) ),
                            "Only 'starts-with' is supported for PropertyMatches: " + matches.prop );
                    Assert.that( !value.contains( String.valueOf( matches.singleWildcard ) ),
                            "Single wildcard is not supported for PropertyMatches: " + matches.prop );

                    var base = value.substring( 0, value.length()-1 );
                    LOG.debug( "MATCHES: %s", base );
                    return result.between( base, base + '\uffff', true, false );
                }
            }
            else {
                throw new RuntimeException( "Not yet implemented: " + expr );                
            }
        }
        // many: ANY
        else if (expr instanceof ManyAssociationQuantifier) {
            var quantifier = (ManyAssociationQuantifier<?>)expr;
            Assert.that( quantifier.type == Type.ANY, "ALL quantifier is not yet supported" );
            var subType = (Class<? extends Entity>)quantifier.prop.info().getType();
            
            var subQuery = new SubQuery<>( subType ).where( quantifier.subExp() );
            var ids = uow.executeQuery( subQuery )
                    .reduce( new ArrayList<String>( 128 ), (result,next) -> {
                        if (next != null) {
                            result.add( (String)next.id() );
                        }
                    })
                    .waitForResult().get();

            LOG.debug( "AnyOf: subQuery: %s -> %s", quantifier.subExp(), ids );
            
            return FluentFilter.where( fieldNameBase + quantifier.prop.info().getNameInStore() )
                    .elemMatch( FluentFilter.$.in( ids.toArray( String[]::new ) ) );
        }
        // Composite
        else if (expr instanceof TheCompositeQuantifier) {
            var quantifier = (TheCompositeQuantifier<?>)expr;
            var fieldName = fieldNameBase + quantifier.prop.info().getNameInStore() + ".";
            LOG.debug( "The_Only: subQuery: %s (%s)", quantifier.subExp(), fieldName );
            return build( quantifier.subExp(), fieldName );
        }
        // not yet...
        else {
            throw new RuntimeException( "Not yet implemented: " + expr );
        }
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
