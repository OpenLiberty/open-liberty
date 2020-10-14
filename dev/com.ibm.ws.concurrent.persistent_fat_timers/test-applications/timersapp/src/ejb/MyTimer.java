/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb;

import java.util.Date;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

@Stateless
public class MyTimer {
    @Resource
    private SessionContext sessionContext;

    @EJB
    private MyTimerTracker tracker;

    @Timeout
    public void runTimer(Timer timer) {
        MyTimerInfo info = (MyTimerInfo) timer.getInfo();
        int count = tracker.incrementRunCount(info.name);
        System.out.println("Running execution " + count + " of timer " + info.name);

        if (count >= info.cancelOnExecution)
            timer.cancel();
    }

    /**
     * Timer that runs every half second until we stop it.
     */
    public Timer scheduleMultipleExecutionTimer(String name) {
        MyTimerInfo info = new MyTimerInfo(name, Integer.MAX_VALUE);
        TimerConfig timerConfig = new TimerConfig(info, true);

        TimerService timerService = sessionContext.getTimerService();
        return timerService.createIntervalTimer(0, 500, timerConfig);
    }

    /**
     * Timer that runs once after 300ms.
     */
    public Timer scheduleOneExecutionTimer(String name) {
        MyTimerInfo info = new MyTimerInfo(name, Integer.MAX_VALUE);
        TimerConfig timerConfig = new TimerConfig(info, true);

        TimerService timerService = sessionContext.getTimerService();
        return timerService.createSingleActionTimer(300, timerConfig);
    }

    /**
     * Schedule a timer to run 3 times with 200ms between executions.
     */
    public Timer scheduleTripleExecutionTimer(String name) {
        MyTimerInfo info = new MyTimerInfo(name, 3);
        TimerConfig timerConfig = new TimerConfig(info, true);

        TimerService timerService = sessionContext.getTimerService();
        return timerService.createIntervalTimer(new Date(), 200, timerConfig);
    }
}
