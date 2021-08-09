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

import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.ejb.Schedules;
import javax.ejb.Timer;
import javax.ejb.TimerService;

public class AutoCreatedTimerMBean {
    private static final String CLASS_NAME = AutoCreatedTimerMBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static final String FIRST_OVERRIDE_ANNOTATION = "firstOverrideAnnotation";
    public static final String FIRST_OVERRIDE_XML = "firstOverrideXML";

    public static final String SECOND_OVERRIDE_ANNOTATION = "secondOverrideAnnotation";
    public static final String THIRD_OVERRIDE_ANNOTATION = "thirdOverrideAnnotation";
    public static final String SECOND_OVERRIDE_XML = "secondOverrideXML";

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

    @Schedule(info = FIRST_OVERRIDE_ANNOTATION, persistent = false)
    public void firstOverridenMethod(Timer timer) {
        svLogger.info("The firstOverriddenMethod got called back into.");
    }

    @Schedules({ @Schedule(info = SECOND_OVERRIDE_ANNOTATION, persistent = false), @Schedule(info = THIRD_OVERRIDE_ANNOTATION, persistent = false) })
    public void secondOverridenMethod(Timer timer) {
        svLogger.info("The secondOverridenMethod got called back into.");
    }

    public void clearAllTimers() {
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }
}
