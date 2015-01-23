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
package org.polymap.model2.test;

import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.core.runtime.Timer;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class PerformanceTest
        extends TestCase {

    private static final Log log = LogFactory.getLog( PerformanceTest.class );
    
    protected EntityRepository      repo;

    protected UnitOfWork            uow;
    

    public PerformanceTest( String name ) {
        super( name );
    }

    protected void setUp() throws Exception {
        log.info( " --------------------------------------- " + getClass().getSimpleName() + " : " + getName() );
    }

    protected void tearDown() throws Exception {
        uow.close();
        repo.close();
    }


    public void testPerformance() throws Exception {
        logHeap();
        Timer timer = new Timer();
        int loops = 1000;
        for (int i=0; i<loops; i++) {
            Employee employee = uow.createEntity( Employee.class, null );
            employee.jap.set( i );
        }
        log.info( "Employees created: " + loops + " in " + timer.elapsedTime() + "ms" );
        logHeap();

        // commit
        timer.start();
        uow.commit();
        log.info( "Commit time: " + timer.elapsedTime() + "ms" );
        logHeap();
        
        // load
        timer.start();
        UnitOfWork uow2 = repo.newUnitOfWork();
        ResultSet<Employee> results = uow2.query( Employee.class ).execute();

        for (Employee employee : results) {
            int jap = employee.jap.get();
            assert jap >= 0 && jap <= results.size() : "jap = " + jap + ", result.size() = " + results.size();
        }
        log.info( "Load time: " + timer.elapsedTime() + "ms" );
        logHeap();
    }

    
    protected void logHeap() {
        System.gc();
        Runtime rt = Runtime.getRuntime();
        log.info( "HEAP: free/total: " + 
                FileUtils.byteCountToDisplaySize( rt.freeMemory() ) + " / " + 
                FileUtils.byteCountToDisplaySize( rt.totalMemory() ) );
    }

}
