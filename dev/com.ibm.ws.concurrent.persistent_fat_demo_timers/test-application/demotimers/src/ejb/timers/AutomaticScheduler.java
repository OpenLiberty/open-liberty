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

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timer;

/**
 * This class uses the @Schedule annotation.
 * Using this annotation will start the timer immediately on start and will run every hour.
 */
@Singleton
public class AutomaticScheduler {
    @Resource
    private SessionContext sessionContext; //Used to get information about timer

    @EJB
    private ProgrammaticTimer programmaticTimer;

    private int count;

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
     * Runs once an hour. Automatically starts when application starts.
     * When run will spawn a programmaticTimer.
     */
    @Schedule(info = "Scheduling Programmatic Timers", hour = "*", minute = "0", second = "0", persistent = true)
    public void run(Timer timer) {
        System.out.println("Running execution " + (++count) + " of timer " + timer.getInfo());

        Timer spawnedTimer = programmaticTimer.setTimer();

        System.out.println("Spawned New Timer: " + spawnedTimer.getInfo());
    }

}
