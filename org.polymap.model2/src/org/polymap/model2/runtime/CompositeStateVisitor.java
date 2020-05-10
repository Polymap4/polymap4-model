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
package org.polymap.model2.runtime;

import java.util.Collection;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Mixins;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.engine.PropertyInterceptorBase;

import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;

/**
 * Allows to visit the entire hierachy of properties of the given {@link Composite}
 * and all of its {@link Mixins annotated} mixins.
 * <p/>
 * <b>Example:</b>
 * 
 * <pre>
 * // visit all simple properties
 * new CompositeStateVisitor() {
 * 
 *     protected void visitProperty( Property prop ) {
 *         log.info( &quot;simple prop: &quot; + prop.getInfo().getName() );
 *     }
 * }.process( entity );
 * </pre>
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class CompositeStateVisitor<E extends Exception> {

    private static Log log = LogFactory.getLog( CompositeStateVisitor.class );
    
    private Composite           target;

    /**
     * Override this in order to visit simple {@link Property}s. 
     */
    protected void visitProperty( Property prop ) throws E {
    }
    
    /**
     * Override this in order to visit {@link Property}s with a {@link Composite}
     * value.
     * 
     * @return False specifies that the properties of the {@link Composite} will not
     *         be visited. (default: true)
     */
    protected boolean visitCompositeProperty( Property prop ) throws E {
        return true; 
    }

    /**
     * Override this in order to visit {@link CollectionProperty}s with simple
     * values.
     */
    protected void visitCollectionProperty( CollectionProperty prop ) throws E {
    }
    
    /**
     * Override this in order to visit {@link CollectionProperty}s with
     * {@link Composite} values.
     * 
     * @return False specifies that the members of the collection will not be
     *         visited. (default: true)
     */
    protected boolean visitCompositeCollectionProperty( CollectionProperty prop ) throws E { 
        return true; 
    }
    
    /**
     * Override this in order to visit {@link Association}s. 
     */
    protected void visitAssociation( Association prop ) throws E {
    }
    

    /**
     * Override this in order to visit {@link ManyAssociation}s. 
     */
    protected void visitManyAssociation( ManyAssociation prop ) throws E {
    }
    

    /**
     * 
     *
     * @param composite The {@link Composite} to visit.
     */
    public void process( Composite composite ) throws E {
        this.target = composite;
        
        // composite
        processComposite( composite );

        // mixins
        if (composite instanceof Entity) {
            Collection<Class<? extends Composite>> mixins = composite.info().getMixins();
            for (Class<? extends Composite> mixinClass : mixins) {
                Composite mixin = ((Entity)composite).as( mixinClass ).get();
                processComposite( mixin );
            }
        }
    }
    
    
    /**
     * Recursivly process properties of the given Composite.
     */
    protected final void processComposite( Composite composite ) throws E {
        Collection<PropertyInfo<?>> props = composite.info().getProperties();
        for (PropertyInfo propInfo : props) {
            PropertyBase prop = propInfo.get( composite );
            
            // Concerns may implement multiple interfaces, find the rootDelegate
            // the get the real type of the Property
            PropertyBase rootDelegate = PropertyInterceptorBase.rootDelegate( (PropertyInterceptorBase)prop );
            
            // Association
            if (rootDelegate instanceof Association) {
                visitAssociation( (Association)prop );
            }
            // ManyAssociation
            else if (rootDelegate instanceof ManyAssociation) {
                visitManyAssociation( (ManyAssociation)prop );
            }
            // Collection
            else if (rootDelegate instanceof CollectionProperty) {
                if (Composite.class.isAssignableFrom( propInfo.getType() )) {
                    if (visitCompositeCollectionProperty( (CollectionProperty)prop )) {
                        for (Composite value : ((CollectionProperty<Composite>)prop)) {
                            processComposite( value );                            
                        }
                    }
                }
                else {
                    visitCollectionProperty( (CollectionProperty)prop );
                }
            }
            // Property
            else if (rootDelegate instanceof Property) {
                if (Composite.class.isAssignableFrom( propInfo.getType() )) {
                    if (visitCompositeProperty( (Property)prop )) {
                        Composite value = (Composite)((Property)prop).opt().orElse( null );
                        if (value != null) {
                            processComposite( value );
                        }
                    }
                }
                else {
                    visitProperty( (Property)prop );
                }
            }
            else {
                throw new RuntimeException( "Unhandled Property type:" + prop );
            }
        }
        
    }
    
}
