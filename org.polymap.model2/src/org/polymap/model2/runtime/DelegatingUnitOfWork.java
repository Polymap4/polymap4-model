/* 
 * polymap.org
 * Copyright (C) 2016, the @authors. All rights reserved.
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
package org.polymap.model2.runtime;

import java.util.Optional;

import java.io.IOException;

import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;

/**
 * Provides a base class for app specific facades of an {@link UnitOfWork}. All
 * methods delegate to the wrapped {@link #delegate}.
 *
 * @author Falko Bräutigam
 */
public abstract class DelegatingUnitOfWork
        implements UnitOfWork {

    private UnitOfWork          delegate;

    
    public DelegatingUnitOfWork( UnitOfWork delegate ) {
        assert delegate != null;
        this.delegate = delegate;
    }

    protected UnitOfWork delegate() {
        return delegate;
    }
    
    public UnitOfWork newUnitOfWork() {
        return delegate.newUnitOfWork();
    }

    public <T extends Entity> T entityForState( Class<T> entityClass, Object state ) {
        return delegate.entityForState( entityClass, state );
    }

    public <T extends Entity> T entity( Class<T> entityClass, Object id ) {
        return delegate.entity( entityClass, id );
    }

    public <T extends Entity> T entity( T entity ) {
        return delegate.entity( entity );
    }

    public <T extends Entity> T createEntity( Class<T> entityClass, Object id, ValueInitializer<T>... initializers ) {
        return delegate.createEntity( entityClass, id, initializers );
    }

    public void removeEntity( Entity entity ) {
        delegate.removeEntity( entity );
    }

    public void prepare() throws IOException, ConcurrentEntityModificationException {
        delegate.prepare();
    }

    public void commit() throws ModelRuntimeException {
        delegate.commit();
    }

    public void rollback() throws ModelRuntimeException {
        delegate.rollback();
    }

    public void close() {
        delegate.close();
    }

    public boolean isOpen() {
        return delegate.isOpen();
    }

    public <T extends Entity> Query<T> query( Class<T> entityClass ) {
        return delegate.query( entityClass );
    }

    @Override
    public Optional<UnitOfWork> parent() {
        return delegate.parent();
    }
    
}
