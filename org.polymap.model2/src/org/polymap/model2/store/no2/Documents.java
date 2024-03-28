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
import java.util.Collection;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.common.Constants;

import org.polymap.model2.runtime.UnitOfWork;

import areca.common.Assert;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 * Clone/copy No2 {@link Document}s.
 *
 * @author Falko
 */
class Documents {

    private static final Log LOG = LogFactory.getLog( Documents.class );
    
    /**
     * Deep copy the given {@link Document}. {@link Document#clone()} does not work
     * correctly, child {@link Document}s are not cloned properly.
     * <p>
     * Nitrite seems to cache and re-use {@link Document}s for subsequent get()s for
     * the same collection. So, different {@link UnitOfWork}s would share, and maybe
     * modify, the same documents. {@link No2UnitOfWork} clones documents when
     * reading from backend to prevent this. Each {@link No2CompositeState} can
     * modify its copy.
     * <p>
     * Maybe this is also good for reading if the document is updated in the
     * datastore. I'm not sure (as with many things regarding Nitrite) if this would
     * also update/modify the shared instance of the document. In this case one copy
     * per {@link No2CompositeState} would be good.
     */
    public static Document clone( Document d ) {
        return doClone( d, "" );
    }
    
    protected static Document doClone( Document d, String prefix ) {
        //LOG.debug( "%sclone: %s [id=%s]", prefix, d, System.identityHashCode( d ) );
        var clone = Document.createDocument( Constants.DOC_ID, d.getId().getIdValue() );

        for (var kv : d) {
            //LOG.debug( "%s  %s = %s", prefix, kv.getFirst(), kv.getSecond() );
            // default
            clone.put( kv.getFirst(), kv.getSecond() );
            // Document
            if (kv.getSecond() instanceof Document) {
                var src = (Document)kv.getSecond();
                clone.put( kv.getFirst(), doClone( src, prefix + "    " ) );
            }
            // Collection
            else if (kv.getSecond() instanceof Collection) {
                var src = (Collection<?>)kv.getSecond();
                var target = new ArrayList<Object>( src.size() );
                for (Object v : src) {
                    Assert.that( !(v instanceof Collection) );
                    target.add( v instanceof Document ? doClone( (Document)v, "    " ) : v );
                }
                clone.put( kv.getFirst(), target );
            }
        }
        //LOG.debug( "cloned: %s %s", System.identityHashCode( clone ), clone );
        Assert.isEqual( d.getId().getIdValue(), clone.getId().getIdValue() );
        Assert.isEqual( d, clone );
        return clone;
    }
    

    public static void copy( Document src, Document target ) {
        doCopy( src, target, "" );
    }
    
    protected static void doCopy( Document src, Document target, String prefix ) {
        LOG.info( "%scopy: %s -> %s", prefix, src.getId(), target.getId() );

        for (var kv : src) {
            LOG.info( "%s  %s = %s", prefix, kv.getFirst(), kv.getSecond() );
            
            // Document
            if (kv.getSecond() instanceof Document) {
                var srcDoc = (Document)kv.getSecond();
                var targetDoc = target.get( kv.getFirst(), Document.class );
                Assert.notNull( targetDoc, "Discarding removed Composite is not supported yet." );
                doCopy( srcDoc, targetDoc, prefix + "    " );
            }
            // Collection
            else if (kv.getSecond() instanceof Collection) {
                var srcColl = (Collection<?>)kv.getSecond();
                var targetColl = (Collection<?>)target.get( kv.getFirst(), Collection.class );
                var copy = new ArrayList<>();
                
                for (Object v : srcColl) {
                    if (v instanceof Collection) {
                        throw new IllegalStateException( "Collection of Collections is not supported." );
                    }
                    else if (v instanceof Document) {
                        var doc = (Document)v;
                        targetColl.stream().map( elm -> (Document)elm )
                                .filter( d -> d.getId().equals( doc.getId() ) ).findFirst()
                                .ifPresent( targetDoc -> {
                                    doCopy( doc, targetDoc, prefix + "    " );
                                    copy.add( targetDoc );
                                });
                    }
                    else {
                        copy.add( v );
                    }
                }
                target.put( kv.getFirst(), copy );
            }
            // primitive/simple value
            else {
                target.put( kv.getFirst(), kv.getSecond() );                
            }
        }
        for (var it = target.iterator(); it.hasNext();) {
            var kv = it.next();
            if (!src.containsKey( kv.getFirst() )) {
                it.remove();
                LOG.info( "%s  %s: removed", prefix, kv.getFirst() );
            }
        }
        Assert.isEqual( src.getId().getIdValue(), target.getId().getIdValue() );
        Assert.isEqual( src, target );
    }

}
