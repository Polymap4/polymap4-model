/* 
 * polymap.org
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package org.polymap.model2.test2;

import org.polymap.model2.Association;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Queryable;

import areca.common.reflect.RuntimeInfo;

@RuntimeInfo
public class Company
        extends Entity {

    public static final CompanyClassInfo info = CompanyClassInfo.instance();

    public static Company TYPE;

    public Association<Person>          chief;
    
    @Queryable
    public ManyAssociation<Person>      employees;
}