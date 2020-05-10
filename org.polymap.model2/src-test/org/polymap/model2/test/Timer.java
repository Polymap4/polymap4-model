/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.test;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import areca.common.base.log.LogFactory;
import areca.common.base.log.LogFactory.Log;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Timer {

    private static Log log = LogFactory.getLog( Timer.class );

    public static Timer startNow() {
        Timer result = new Timer();
        result.start();
        return result;
    }

    // instance *******************************************
    
    private Stopwatch       watch = Stopwatch.createUnstarted();
    
    public void start() {
        if (watch.isRunning()) {
            watch.reset().start();
        }
        else {
            watch.start();
        }
    }

    public long elapsedTime() {
        return watch.elapsed( TimeUnit.MILLISECONDS );
    }
    
}
