/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.ejbcontainer.fat.timer.auto.np.ejb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Schedule;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

@Stateless(name = "AutoCreatedTimerABean")
@Local(AutoCreatedTimerA.class)
public class AutoCreatedTimerABean {
    private static final String CLASS_NAME = AutoCreatedTimerABean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static final String FIRST_SCHEDULE = "first_schedule";
    public static volatile int first_schedule_count = 0;
    public static ArrayList<Long> first_schedule_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> first_schedule_next = new ArrayList<Long>();

    public static final String SECOND_SCHEDULE = "second_schedule";
    public static volatile int second_schedule_count = 0;
    public static ArrayList<Long> second_schedule_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> second_schedule_next = new ArrayList<Long>();

    public static final String THIRD_SCHEDULE = "third_schedule";
    public static volatile int third_schedule_count = 0;
    public static ArrayList<Long> third_schedule_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> third_schedule_next = new ArrayList<Long>();

    public static final String PROGRAMATIC_TIMEOUT = "programatic_timeout";
    public static volatile int programatic_count = 0;
    public static ArrayList<Long> programatic_timestamps = new ArrayList<Long>();
    private static CountDownLatch svProgramaticTimerLatch = new CountDownLatch(1);

    @Resource
    private TimerService ivTS;

    // A lot of these timers are going to keep firing, at a rate of a least once a minute, for as long as the app is installed.
    // Each time they fire, we are storing more data in a list.  To avoid having a situation where the app is installed for
    // a long time, and we fill up the lists with so much data that we run out of memory, the number of adds we make to the
    // list is limited to 25, because no test should need data from more than that number of executions.

    @Schedule(second = "20", minute = "*", hour = "*", info = FIRST_SCHEDULE, persistent = false)
    @Schedule(second = "10", minute = "*", hour = "*", info = SECOND_SCHEDULE, persistent = false)
    @Schedule(second = "40", minute = "*", hour = "*", info = THIRD_SCHEDULE, persistent = false)
    @Timeout
    public void atSchedulesMethod(Timer timer) {
        String info = (String) timer.getInfo();
        if (FIRST_SCHEDULE.equals(info)) {
            if (first_schedule_count < 25) {
                svLogger.info("The atSchedulesMethod got called back into for the 1st schedules timer...");
                first_schedule_count++;
                first_schedule_timestamps.add(Long.valueOf(System.currentTimeMillis()));
                first_schedule_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
                if (first_schedule_count == 2) {
                    // Wait for at least 2 expirations before collecting results
                    AutoCreatedTimerDriverBean.countDown("first_schedule_count");
                }
            }
        } else if (SECOND_SCHEDULE.equals(info)) {
            if (second_schedule_count < 25) {
                svLogger.info("The atSchedulesMethod got called back into for the 2nd schedules timer...");
                second_schedule_count++;
                second_schedule_timestamps.add(Long.valueOf(System.currentTimeMillis()));
                second_schedule_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
                if (second_schedule_count == 2) {
                    // Wait for at least 2 expirations before collecting results
                    AutoCreatedTimerDriverBean.countDown("second_schedule_count");
                }
            }
        } else if (THIRD_SCHEDULE.equals(info)) {
            if (third_schedule_count < 25) {
                svLogger.info("The atSchedulesMethod got called back into for the 3rd schedule timer...");
                third_schedule_count++;
                third_schedule_timestamps.add(Long.valueOf(System.currentTimeMillis()));
                third_schedule_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
                if (third_schedule_count == 2) {
                    // Wait for at least 2 expirations before collecting results
                    AutoCreatedTimerDriverBean.countDown("third_schedule_count");
                }
            }
        } else if (PROGRAMATIC_TIMEOUT.equals(info)) {
            if (programatic_count < 25) {
                svLogger.info("The atSchedulesMethod got called back into for the programatic timer...");
                programatic_count++;
                programatic_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            }
            svProgramaticTimerLatch.countDown();
        } else {
            throw new RuntimeException("The atSchedulesMethod got invoked via the wrong timer **" + info + "**, **" + timer + "**");
        }
    }

    public void createProgramaticTimer() {
        svLogger.info("Creating programatic timer...");
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(PROGRAMATIC_TIMEOUT);
        timerConfig.setPersistent(false);
        svProgramaticTimerLatch = new CountDownLatch(1);

        ivTS.createSingleActionTimer(200, timerConfig);
    }

    public void waitForProgramaticTimer(long maxTimeToWait) {
        try {
            svProgramaticTimerLatch.await(maxTimeToWait, TimeUnit.MILLISECONDS);
            svProgramaticTimerLatch = new CountDownLatch(1); // reset in case we want to wait for next timeout
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Properties getTimerData(String infoToMatchOn) throws Exception {
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
        throw new Exception("Could not find timer **" + infoToMatchOn + "**");
    }

    public void clearAllTimers() {
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

}
