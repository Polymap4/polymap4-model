/* 
 * polymap.org
 * Copyright (C) 2020, the @authors. All rights reserved.
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

import java.util.logging.Logger;

import areca.common.testrunner.LogDecorator;
import areca.common.testrunner.TestRunner;
import areca.rt.teavm.testapp.HtmlTestRunnerDecorator;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class Main {

    private static final Logger LOG = Logger.getLogger( Main.class.getName() );
    

    @SuppressWarnings( "unchecked" )
    public static void main( String[] args ) throws Exception {
        try {
            new TestRunner()
                    .addDecorators( HtmlTestRunnerDecorator.info, LogDecorator.info )
                    .addTests( SimpleModelTest.info )
                    .run();
            
//            EntityRepository repo = EntityRepository.newConfiguration()
//                    .entities.set( Arrays.asList( Person.info ) )
//                    .store.set( new IDBStore( "test2" ) )
//                    .create();
//            LOG.info( "MAIN: repo created" );
//            
////            try (
//                UnitOfWork uow = repo.newUnitOfWork();
////            ){
//                LOG.info( "MAIN: uow = " + uow );
//                Person person = uow.createEntity( Person.class, null, (Person proto) -> {
//                    LOG.info( "MAIN: proto = " + proto.id() );
//                    proto.name.set( "Schäfchen!" );
//                    return proto;
//                });
//                
//                LOG.info( "Person: " + person.name.get() + " / " + person.firstname.get() + " / " + person.birthday.get() );
//                //uow.commit();
////            }
        }
        catch (Exception e) {
            System.out.println( "Exception: " + e + " --> " );
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            System.out.println( "Root cause: " + rootCause );
            throw (Exception)rootCause;
        }
    }
    
}
