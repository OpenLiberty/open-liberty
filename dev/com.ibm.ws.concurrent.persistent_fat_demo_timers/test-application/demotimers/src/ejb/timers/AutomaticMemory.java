/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb.timers;

import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;

/**
 * This class uses the @Schedule annotation.
 * Using this annotation will start the timer immediately on start and will run every other second.
 */
@Stateless
public class AutomaticMemory {
    @Resource
    private SessionContext sessionContext; //Used to get information about timer

    private int count; //Incremented with each execution of timers

    /**
     * Cancels timer execution
     */
    public void cancel() {
        for (Timer timer : sessionContext.getTimerService().getTimers())
            timer.cancel();
    }

    /**
     * Get the value of count.
     */
    public int getRunCount() {
        return count;
    }

    /**
     * Runs ever other second. Automatically starts when application starts.
     */
    @Schedule(info = "Performing Memory Operations", hour = "*", minute = "*", second = "*/2", persistent = true)
    public void run(Timer timer) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1)); //sleep for 1 second to simulate long running in memory task
        } catch (InterruptedException e) {
            //ignore
        }
        System.out.println("Running execution " + (++count) + " of timer " + timer.getInfo());
    }
}
