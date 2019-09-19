/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Stateless
public class MyTimerEJBInWAR {
    private static final ConcurrentHashMap<Serializable, Integer> counters = new ConcurrentHashMap<Serializable, Integer>();

    @Resource
    private SessionContext sessionContext;

    public Integer getRunCount(String testName) {
        return counters.get(testName);
    }

    @Timeout
    public void runTimer(Timer timer) {
        Serializable testName = timer.getInfo();
        System.out.println("The timer for " + testName + " is running");
        Integer previousCount = counters.putIfAbsent(testName, 1);
        if (previousCount != null)
            counters.put(testName, previousCount + 1);
    }

    public Timer scheduleTimer(String testName, long delay) {
        TimerService timerService = sessionContext.getTimerService();
        TimerConfig timerConfig = new TimerConfig(testName, true);
        Timer timer = timerService.createSingleActionTimer(delay, timerConfig);
        return timer;
    }
}
