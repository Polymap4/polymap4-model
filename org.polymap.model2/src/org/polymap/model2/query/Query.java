/* 
 * polymap.org
 * Copyright (C) 2013, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.query;

import static org.polymap.model2.query.Expressions.and;

import org.polymap.model2.Entity;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * Represents a query for the given {@link Entity} type.  
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class Query<T extends Entity> {

    public Class<T>             resultType;

    public BooleanExpression    expression = Expressions.TRUE;

    public int                  firstResult = 0;

    public int                  maxResults = Integer.MAX_VALUE;

    
    public Query( Class<T> resultType ) {
        this.resultType = resultType;
    }


    /**
     * Executes the query with its current settings. Once executed the contents of
     * the resulting {@link ResultSet} does not reflect modifications of the
     * underlying store or other {@link UnitOfWork} instances. However, modifications
     * from within the same {@link UnitOfWork} <b>are</b> refelected, <b>until</b> a
     * referenced Entity was first returned from any Iterator of the ResultSet.
     * 
     * @see UnitOfWork#query(Class)
     * @return Newly created {@link ResultSet}.
     */
    public abstract ResultSet<T> execute();
    
    
    /**
     * Set the filter expression. Use the {@link Expressions} static factory to build
     * a {@link BooleanExpression}.
     * <p/>
     * <b>Example:</b>
     * <pre>
     * import static org.polymap.model2.query.Expressions.and;
     * import static org.polymap.model2.query.Expressions.eq;
     *
     * Employee wanted = Expressions.template( Employee.class, repo );
     * rs = uow.query( Employee.class )
     *         .where( and( eq( wanted.firstname, "Ulli" ), eq( wanted.name, "Philipp" ) ) )
     *         .execute();
     * </pre>
     * 
     * @param expression The expression can be {@link BooleanExpression} or store
     *        dependent filter expression.
     * @return this
     */
    public Query<T> where( @SuppressWarnings("hiding") BooleanExpression expression ) {
        assert expression != null : "Null expression is not (no longer) allowed.";
        this.expression = expression;
        return this;
    }
    
    
    /**
     * Add the given expression to this query. The expressions are joined via logical
     * AND expression.
     * 
     * @param expression
     * @return this
     */
    public Query<T> andWhere( @SuppressWarnings("hiding") BooleanExpression expression ) {
        assert this.expression != null : "Set expression via #where() before calling #and().";
        assert this.expression instanceof BooleanExpression : "Adding more expressions is supported for BooleanExpression only.";
        where( and( this.expression, expression ) );
        return this;
    }
    
    
    /**
     * Set the index of the first result. Default is 0 (zero).
     *
     * @return this
     */
    public Query<T> firstResult( @SuppressWarnings("hiding") int firstResult ) {
        this.firstResult = firstResult;
        return this;
    }
    
    
    /**
     * Set how many results should be returned. Default is that there is no limit
     * set.
     * 
     * @return this
     */
    public Query<T> maxResults( @SuppressWarnings("hiding") int maxResults ) {
        this.maxResults = maxResults;
        return this;
    }

    
    public Class<T> resultType() {
        return resultType;
    }

}
