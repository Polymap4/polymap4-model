/* 
 * polymap.org
 * Copyright (C) 2014-2021, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.engine;

import static org.polymap.model2.engine.InstanceBuilder.contextOf;

import org.polymap.model2.Association;
import org.polymap.model2.Entity;
import org.polymap.model2.runtime.EntityRuntimeContext;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.store.StoreProperty;

import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Consumer;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class AssociationImpl<T extends Entity>
        implements Association<T> {

    private EntityRuntimeContext    context;
    
    /** Holding the id of the associated Entity. */
    private StoreProperty<?>        storeProp;
    
    
    /**
     * 
     * @param context
     * @param storeProp Holding the id of the associated Entity.
     */
    public AssociationImpl( EntityRuntimeContext context, StoreProperty<?> storeProp ) {
        this.context = context;
        this.storeProp = storeProp;
    }


    @Override
    public String toString() {
        return "Association[name:" + info().getName() + ",id=" + storeProp.get().toString() + "]";
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    public Promise<T> fetch() {
        Object id = storeProp.get();
        return id != null 
                ? context.getUnitOfWork().entity( info().getType(), id )
                : Promise.completed( null );
    }

    
    @Override
    //@SuppressWarnings("unchecked")
    public <E extends Exception> Promise<T> ensure( Consumer<T,E> initializer ) {
//        return context.getUnitOfWork().ensureEntity( (Class<T>)info().getType(),
//                Expressions.id( storeProp.get() != null ? storeProp.get() : "__impossible__" ),
//                proto -> {
//                   initializer.accept( (T)proto );
//                   set( (T)proto );
//                });
        
        return fetch().map( entity -> {
            // already there
            if (entity != null) {
                return entity;
            }
            // created by concurrent thread
            else if (storeProp.get() != null) {
                var uow = (UnitOfWorkImpl)context.getUnitOfWork();
                return Assert.notNull( (T)uow.loaded.get( storeProp.get() ) ); 
            }
            // create new Entity
            else {
                T created = (T)context.getUnitOfWork().createEntity( info().getType(), initializer );
                set( created );
                return created;
            }
        });
    }


    @Override
    public void set( T value ) {
        // make sure that elm belongs to my UoW; it would not break here but
        // on BidiAssociationConcern and/or maybe elsewhere that depends on Entity.equals()
        assert value == null || contextOf( value ).getUnitOfWork() == context.getUnitOfWork() : "Entity does no belong to this UnitOfWork.";
        
        storeProp.set( value != null ? value.id() : null );
    }


    @Override
    public PropertyInfo info() {
        return storeProp.info();
    }
    
}
