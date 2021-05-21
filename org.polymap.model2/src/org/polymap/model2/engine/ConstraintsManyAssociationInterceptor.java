/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.engine;

import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.engine.EntityRepositoryImpl.EntityRuntimeContextImpl;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.ModelRuntimeException;

import areca.common.Promise;

/**
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
final class ConstraintsManyAssociationInterceptor<T extends Entity>
        extends ConstraintsInterceptor<T>
        implements ManyAssociation<T> {

    public ConstraintsManyAssociationInterceptor( ManyAssociation<T> delegate, EntityRuntimeContextImpl context ) {
        super( delegate, context );
    }

    public ManyAssociation<T> delegate() {
        return (ManyAssociation<T>)delegate;
    }

    @Override
    public boolean equals( Object o ) {
        throw new UnsupportedOperationException("...");
//        if (o instanceof CollectionProperty) {
//            return info() == ((CollectionProperty)o).info();
//        }
//        else if (o instanceof Collection) {
//            return ((Collection)o).containsAll( delegate() );
//        }
//        else {
//            return false;
//        }
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public String toString() {
        return delegate().toString();
    }


    // Collection *****************************************
    
    @Override
    public boolean add( T e ) {
        context.checkState();
        if (isImmutable) {
            throw new ModelRuntimeException( "Property is @Immutable: " + fullPropName() );
        }
        if (delegate().add( e )) {
            context.raiseStatus( EntityStatus.MODIFIED );
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public Promise<T> fetch() {
        context.checkState();
        return delegate().fetch().onSuccess( value -> {
            context.checkState();            
        });
    }

    @Override
    public int size() {
        context.checkState();
        return delegate().size();
    }

}
