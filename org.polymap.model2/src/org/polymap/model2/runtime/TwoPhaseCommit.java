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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Simple transaction manager that handles committing of multiple
 * {@link TransactionAware} resources.
 *
 * @author Falko Bräutigam
 */
public class TwoPhaseCommit {

    private static Log log = LogFactory.getLog( TwoPhaseCommit.class );
    
    /**
     * The interface of a transaction aware resource that is managed by
     * {@link TwoPhaseCommit}.
     */
    public interface TransactionAware {
        
        public void prepare() throws Exception;
        
        public void commit();
        
        public void rollback();
        
        public void close();
    }
    
    /**
     * 
     */
    public static class UnitOfWorkAdapter
            implements TransactionAware {
        
        private UnitOfWork          uow;

        public UnitOfWorkAdapter( UnitOfWork uow ) {
            this.uow = uow;
        }

        @Override
        public void prepare() throws Exception {
            throw new UnsupportedOperationException("not implemented: two-phase commit");
        }

        @Override
        public void commit() {
            throw new UnsupportedOperationException("not implemented: two-phase commit");
        }

        @Override
        public void rollback() {
            uow.discard();
        }

        @Override
        public void close() {
            uow.close();
        }
    }
    
    /**
     * 
     */
    public enum CommitType {
        KEEP_OPEN,
        CLOSE_ON_ERROR,
        CLOSE
    }
    

    // instance *******************************************
    
    /** Sequence of resources in order of calling {@link #register(TransactionAware)}. */
    private List<TransactionAware>      resources = new ArrayList<>();
    
    private boolean                     commitStarted = false;
    
    
    public TwoPhaseCommit( TransactionAware... resources ) {
        for (TransactionAware resource : resources) {
            this.resources.add( resource );
        }
    }
    
    
    /**
     * Registers a resource to take part on the 2-phase-commit. Commit/rollback id
     * done in order of registering the resources.
     */
    public <T extends TransactionAware> T register( T resource ) {
        assert !commitStarted;
        resources.add( resource );
        return resource;
    }

    
    public List<TransactionAware> registered() {
        return Collections.unmodifiableList( resources );    
    }

    
    public void commit( CommitType type ) throws Exception {
        try {
            assert !resources.isEmpty();
            commitStarted = true;
            doPrepare();
            doCommit();
        }
        catch (Exception e) {
            if (type.equals( CommitType.CLOSE_ON_ERROR )) {
                doClose();
            }
            throw e;
        }
        finally {
            if (type.equals( CommitType.CLOSE )) {
                doClose();
            }
        }
    }
    
    
    public void rollback( CommitType type ) {
        try {
            assert !resources.isEmpty();
            doRollback();
        }
        catch (Exception e) {
            if (type.equals( CommitType.CLOSE_ON_ERROR )) {
                doClose();
            }
        }
        finally {
            if (type.equals( CommitType.CLOSE )) {
                doClose();
            }
        }
    }
    
    
    public void commitOrRollback( CommitType type ) throws Exception {
        try {
            assert !resources.isEmpty();
            commitStarted = true;
            doPrepare();
            doCommit();
        }
        catch (Exception e) {
            doRollback();
            
            if (type.equals( CommitType.CLOSE_ON_ERROR )) {
                doClose();
            }
            throw e;
        }
        finally {
            if (type.equals( CommitType.CLOSE )) {
                doClose();
            }
        }
    }
    
    
    protected void doPrepare() throws Exception {
        for (TransactionAware r : resources) {
            r.prepare();
        }
    }

    protected void doCommit() {
        for (TransactionAware r : resources) {
            r.commit();
        }
    }
    
    protected void doRollback() {
        List<Exception> excs = new ArrayList<>();
        
        // reverse resources to handle parent/nested UnitOfWork properly
        List<TransactionAware> reverse = new ArrayList<>( resources );
        Collections.reverse( reverse );
        for (TransactionAware r : reverse) {
            try {
                r.rollback();
            }
            catch (Exception e) {
                log.warn( "", e );
                excs.add( e );
            }
        }
        if (!excs.isEmpty()) {
            throw new RuntimeException( "Errors during rollback: " + excs.size(), excs.get( 0 ) );
        }
    }
    
    protected void doClose() {
        for (TransactionAware r : resources) {
            try {
                r.close();
            }
            catch (Exception e) {
                log.warn( "", e );
            }
        }
    }

}
