/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.ejb.TimerService;

@Local(AutoCreatedTimerSS.class)
@Singleton
@Startup
public class AutoCreatedTimerSSBean {
    private static final String CLASS_NAME = AutoCreatedTimerSSBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static final String SINGLETON_STARTUP = "singleton_startup";
    public static volatile int singleton_startup_count = 0;
    public static ArrayList<Long> singleton_startup_timestamps = new ArrayList<Long>();

    public static boolean foundTimer = false;

    @Resource
    private TimerService ivTS;

    public Properties getTimerData(String infoToMatchOn) {
        Properties props = new Properties();
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer oneTimer : timers) {
            String info = (String) oneTimer.getInfo();

            if (info != null && info.equals(infoToMatchOn)) {
                ScheduleExpression schedule = oneTimer.getSchedule();
                Date date = oneTimer.getNextTimeout();
                props.put(AutoCreatedTimerDriverBean.SCHEDULE_KEY, schedule);
                props.put(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY, date);
                return props;
            }

        }
        return null;
    }

    @PostConstruct
    public void postConstructMethod() {
        svLogger.info("Inside .postConstruct() method for singleton startup...");

        Properties props = getTimerData(SINGLETON_STARTUP);

        foundTimer = (props != null) ? true : false;
    }

    @Schedule(second = "39", minute = "*", hour = "*", info = SINGLETON_STARTUP, persistent = false)
    public void singletonStartupTimer(Timer timer) {
        if (singleton_startup_count < 25) {
            svLogger.info("The singleton startup timer method got called back into.");
            singleton_startup_count++;
            singleton_startup_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            if (singleton_startup_count == 1) {
                // Wait for at least 1 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("singletonStartupTimer");
            }
        }
    }

    public void clearAllTimers() {
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

}
