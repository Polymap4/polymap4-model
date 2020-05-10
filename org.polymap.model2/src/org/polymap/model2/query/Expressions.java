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
package org.polymap.model2.query;

import com.google.common.collect.Lists;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.engine.TemplateInstanceBuilder;
import org.polymap.model2.engine.TemplateProperty;
import org.polymap.model2.query.grammar.AssociationEquals;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.query.grammar.CompositeCollectionQuantifier;
import org.polymap.model2.query.grammar.Conjunction;
import org.polymap.model2.query.grammar.Disjunction;
import org.polymap.model2.query.grammar.IdPredicate;
import org.polymap.model2.query.grammar.ManyAssociationQuantifier;
import org.polymap.model2.query.grammar.Negation;
import org.polymap.model2.query.grammar.PropertyEquals;
import org.polymap.model2.query.grammar.PropertyEqualsAny;
import org.polymap.model2.query.grammar.PropertyMatches;
import org.polymap.model2.query.grammar.PropertyNotEquals;
import org.polymap.model2.query.grammar.Quantifier;
import org.polymap.model2.query.grammar.Quantifier.Type;
import org.polymap.model2.query.grammar.TheAssociationQuantifier;
import org.polymap.model2.query.grammar.TheCompositeQuantifier;
import org.polymap.model2.runtime.EntityRepository;

import areca.common.reflect.ClassInfo;

/**
 * Static factory methods to create query expressions. 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Expressions {

    public static final BooleanExpression TRUE = new BooleanExpression() {
        @Override
        public boolean evaluate( Composite target ) {
            return true;
        }
    };
    
    public static final BooleanExpression FALSE = new BooleanExpression() {
        @Override
        public boolean evaluate( Composite target ) {
            return false;
        }
    };
    
    public static Conjunction and( BooleanExpression first, BooleanExpression second, BooleanExpression... more ) {
        return new Conjunction( Lists.asList( first, second, more ).toArray( new BooleanExpression[2+more.length] ) );                                                                               
    }
    
    public static Disjunction or( BooleanExpression first, BooleanExpression second, BooleanExpression... more) {
        return new Disjunction( Lists.asList( first, second, more ).toArray( new BooleanExpression[2+more.length] ) );                                                                               
    }
    
    public static <T> PropertyEquals<T> eq( Property<T> prop, T value ) {
        return new PropertyEquals( (TemplateProperty)prop, value );
    }

    public static <T> PropertyNotEquals<T> notEq( Property<T> prop, T value ) {
        return new PropertyNotEquals( (TemplateProperty)prop, value );
    }
    
    public static <T> Negation not( BooleanExpression expression ) {
        return new Negation( expression );
    }
    
    /**
     * Checks the value to see if it matches the specified wildcard matcher, always
     * testing case-sensitive.
     * <p/>
     * The wildcard matcher uses the characters '?' and '*' to represent a single or
     * multiple wildcard characters. This is the same as often found on Dos/Unix
     * command lines. The check is case-sensitive always.
     */
    public static <T> PropertyMatches<T> matches( Property<T> prop, T value ) {
        return new PropertyMatches( (TemplateProperty)prop, value );
    }
    
    public static <T> PropertyEqualsAny<T> eqAny( Property<T> prop, T... values ) {
        return new PropertyEqualsAny( (TemplateProperty)prop, values );
    }
    
//    public static <T> PropertyEqualsAny<T> eqAny( Property<T> prop, Iterable<T> values ) {
//        ArrayList<T> l = Lists.newArrayList( values );
//        T[] a = l.toArray( new T[ l.size() ] );
//        return new PropertyEqualsAny( (TemplateProperty)prop, a );
//    }
    
    public static <T extends Entity> AssociationEquals<T> is( Association<T> assoc, T entity ) {
        return new AssociationEquals( (TemplateProperty)assoc, id( entity ) );
    }

    public static <T extends Entity> AssociationEquals<T> isAnyOf( Association<T> assoc, T... entities ) {
        return new AssociationEquals( (TemplateProperty)assoc, id( entities ) );
    }

//    public static <T extends Entity> AssociationEquals<T> isAnyOf( Association<T> assoc, Iterable<T> entities ) {
//        ArrayList<T> l = Lists.newArrayList( entities );
//        l.toArray( new T[] );
//        return new AssociationEquals( (TemplateProperty)assoc, id( entities ) );
//    }

    /**
     * True if {@link Entity#id()} of the target Entity of the {@link Query} is in
     * the given array of ids.
     */
    public static <T extends Entity> IdPredicate<T> id( Object... ids ) {
        return new IdPredicate( ids );
    }
    
    /**
     * True if {@link Entity#id()} of the target Entity of the {@link Query} equals
     * an id of one of the given Entities.
     */
    public static <T extends Entity> IdPredicate<T> id( T... entities ) {
        Object[] ids = new Object[entities.length];
        for (int i=0; i<entities.length; i++) {
            ids[i] = entities[i].id();
        }
        return new IdPredicate( ids );
    }

    // Quantifiers ****************************************
    
    /**
     * Queries an {@link Association}. True if the associated {@link Entity} matches
     * the given sub-expression.
     * <p/>
     * Use {@link #is(Association, Entity)} or {@link #isAnyOf(Association, Entity...)}
     * to check if an Association points to an <b>already known Entity</b>.
     */
    public static <T extends Entity> Quantifier the( Association<T> prop, BooleanExpression subExp ) {
        return new TheAssociationQuantifier( prop, subExp );
    }
    
    /**
     * Queries a single Composite property. True if the Composite matches the given
     * sub-expression.
     */
    public static <T extends Composite> Quantifier the( Property<T> prop, BooleanExpression subExp ) {
        return new TheCompositeQuantifier( prop, subExp );
    }
    
    /**
     * True if ANY member of the given Composite collection matches the given
     * sub-expression.
     */
    public static <T extends Composite> Quantifier anyOf( CollectionProperty<T> prop, BooleanExpression subExp ) {
        return new CompositeCollectionQuantifier( Type.ANY, prop, subExp );
    }
    
    /**
     * True if ANY member of the given association matches the given sub-expression.
     */
    public static <T extends Entity> Quantifier anyOf( ManyAssociation<T> prop, BooleanExpression subExp ) {
        return new ManyAssociationQuantifier( Type.ANY, prop, subExp );
    }
    
    // Template *******************************************
    
    /**
     * Returns query template of the given {@link Composite} type.
     *
     * @param type
     * @param repo
     * @return Newly created query template instance.
     */
    public static <T extends Composite> T template( Class<T> type, EntityRepository repo ) {
        return new TemplateInstanceBuilder( repo ).newComposite( ClassInfo.of( type ) );
    }

}
