/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.servlet.http.Cookie;

@Stateless
public class MyTimerEJBInWAR {
    private static final ConcurrentHashMap<Serializable, Integer> counters = new ConcurrentHashMap<Serializable, Integer>();

    @Resource
    private SessionContext sessionContext;

    public Integer getRunCount(String testName) {
        return counters.get(testName);
    }

    public Timer getTimer(String name) {
        Timer timer = null;
        for (Timer t : sessionContext.getTimerService().getAllTimers()) {
            Serializable info = t.getInfo();
            String timerName = info instanceof Cookie ? ((Cookie) info).getName() : info.toString();
            System.out.println("Found timer named " + timerName);
            if (name.equals(timerName))
                return t;
        }

        return timer;
    }

    @Timeout
    public void runTimer(Timer timer) {
        String testName;
        Serializable info = timer.getInfo();
        if (info instanceof Cookie)
            testName = ((Cookie) info).getName();
        else
            testName = info.toString();
        System.out.println("The timer for " + testName + " is running");
        Integer previousCount = counters.putIfAbsent(testName, 1);
        if (previousCount != null)
            counters.put(testName, previousCount + 1);
        if (testName.contains("Exception"))
            throw new EJBException("Intentionally raising this error for " + testName);
    }

    public Timer scheduleTimer(String testName, long delay) {
        TimerService timerService = sessionContext.getTimerService();
        TimerConfig timerConfig = new TimerConfig(testName, true);
        Timer timer = timerService.createSingleActionTimer(delay, timerConfig);
        return timer;
    }

    public Timer scheduleTimerWithExpression(String testName, ScheduleExpression schedule) {
        TimerService timerService = sessionContext.getTimerService();
        TimerConfig timerConfig = new TimerConfig(testName, true);
        Timer timer = timerService.createCalendarTimer(schedule, timerConfig);
        return timer;
    }

    public Timer scheduleTimerWithInfo(Cookie timerInfo, long delay) {
        TimerService timerService = sessionContext.getTimerService();
        TimerConfig timerConfig = new TimerConfig(timerInfo, true);
        Timer timer = timerService.createSingleActionTimer(delay, timerConfig);
        return timer;
    }
}
