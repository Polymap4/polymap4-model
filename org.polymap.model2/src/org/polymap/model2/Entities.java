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
package org.polymap.model2;

import org.polymap.model2.runtime.UnitOfWork;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class Entities {

    /**
     * If both arguments are {@code null}, {@code true} is returned and if exactly
     * one argument is {@code null}, {@code false} is returned. Otherwise, equality
     * is determined by comparing the {@link Entity#id()} of both entities. In
     * contrast to {@link Entity#equals(Object)} this returns <code>true</code> if
     * both entities are living inside different {@link UnitOfWork} instances.
     */
    public static <T extends Entity> boolean equal( T e1, T e2 ) {
        if (e1 == null && e2 == null) {
            return true; // ???
        }
        else if (e1 == null || e2 == null) {
            return false;
        }
        else {
            return e1.id().equals( e2.id() );
        }
    }
}
