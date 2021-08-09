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

import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerService;

public class AutoCreatedTimerXBean {
    private static final String CLASS_NAME = AutoCreatedTimerXBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static String SECONDS_RANGE = "seconds_range";
    public static volatile int seconds_range_count = 0;
    public static ArrayList<Long> seconds_range_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> seconds_range_next = new ArrayList<Long>();

    public static String SECONDS_LIST = "seconds_list";
    public static volatile int seconds_list_count = 0;
    public static ArrayList<Long> seconds_list_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> seconds_list_next = new ArrayList<Long>();

    public static String MINUTES_RANGE = "minutes_range";
    public static volatile int minutes_range_count = 0;
    public static ArrayList<Long> minutes_range_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> minutes_range_next = new ArrayList<Long>();

    public static String MULTIPLE_SETTINGS_DONT_CONFLICT = "multipleSettingsDontConflict";
    public static volatile int multiple_settings_count = 0;
    public static ArrayList<Long> multiple_settings_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> multiple_settings_next = new ArrayList<Long>();

    public static String START_GATE = "startGate";
    public static volatile int start_gate_count = 0;
    public static ArrayList<Long> start_gate_timestamps = new ArrayList<Long>();

    public static String STOP_GATE = "stopGate";
    public static volatile int stop_gate_count = 0;
    public static ArrayList<Long> stop_gate_timestamps = new ArrayList<Long>();

    // begin F743-16271
    public static String TIME_ZONE = "timeZone";
    public static String time_zone_zone = "uninitialized";
    public static volatile int time_zone_count = 0;
    public static ArrayList<Long> time_zone_timestamps = new ArrayList<Long>();
    // end F743-16271

    public static String MINUTES_LIST = "minutes_list";
    public static String HOURS_INTERVAL = "hours_interval";
    public static String HOURS_LIST = "hours_list";
    public static String DAY_OF_WEEK_EXACT = "dayOfWeek_exact";
    public static String DAY_OF_MONTH_RANGE = "dayOfMonth_range";
    public static String MONTH_LIST = "month_list";
    public static String DAY_OF_MONTH_LAST = "dayOfMonth_last";

    @Resource
    private TimerService ivTS;

    //A lot of these timers are going to keep firing, at a rate of a least once a minute, for as long as the app is installed.
    //Each time they fire, we are storing more data in a list.  To avoid having a situation where the app is installed for
    //a long time, and we fill up the lists with so much data that we run out of memory, the number of adds we make to the
    //list is limited to 25, because no test should need data from more than that number of executions.

    public void seconds_range(Timer timer) {
        if (seconds_range_count < 25) {
            svLogger.info("The seconds_range method got called back into.");
            seconds_range_count++;
            seconds_range_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            seconds_range_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (seconds_range_count == 15) {
                // Wait for at least 15 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("seconds_range");
            }
        }
    }

    public void seconds_list(Timer timer) {
        if (seconds_list_count < 25) {
            svLogger.info("The seconds_list method got called back into.");
            seconds_list_count++;
            seconds_list_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            seconds_list_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (seconds_list_count == 9) {
                // Wait for at least 9 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("seconds_list");
            }
        }
    }

    public void minutes_range(Timer timer) {
        if (minutes_range_count < 25) {
            svLogger.info("The minutes_range method got called back into.");
            minutes_range_count++;
            minutes_range_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            minutes_range_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (minutes_range_count == 4) {
                // Wait for at least 4 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("minutes_range");
            }
        }
    }

    public void minutes_list(Timer timer) {
        svLogger.info("The minutes_list method got called back into.");
    }

    public void hours_interval(Timer timer) {
        svLogger.info("The hours_interval method got called back into.");
    }

    public void hours_list(Timer timer) {
        svLogger.info("The hours_list method got called back into.");
    }

    public void dayOfWeek_exact(Timer timer) {
        svLogger.info("The dayOfWeek_exact method got called back into.");
    }

    public void dayOfMonth_range(Timer timer) {
        svLogger.info("The dayOfMonth_range method got called back into.");
    }

    public void month_list(Timer timer) {
        svLogger.info("The month_list method got called back into.");
    }

    public void dayOfMonth_last(Timer timer) {
        svLogger.info("The dayOfMonth_last method got called back into.");
    }

    public void multipleSettingsDontConflict(Timer timer) {
        if (multiple_settings_count < 25) {
            svLogger.info("The multipleSettingsDontConflict method got called back into.");
            multiple_settings_count++;
            multiple_settings_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            multiple_settings_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (multiple_settings_count == 3) {
                // Wait for at least 3 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("multipleSettingsDontConflict");
            }
        }
    }

    public void startGate(Timer timer) {
        if (start_gate_count < 25) {
            svLogger.info("The startGate method got called back into.");
            start_gate_count++;
            start_gate_timestamps.add(Long.valueOf(System.currentTimeMillis()));
        }
    }

    public void stopGate(Timer timer) {
        if (stop_gate_count < 25) {
            svLogger.info("The stopGate method got called back into.");
            stop_gate_count++;
            stop_gate_timestamps.add(Long.valueOf(System.currentTimeMillis()));
        }
    }

    // begin F743-16271
    /**
     * The ejb-jar.xml file has this <timer> element, including
     * the <timezone> element:
     *
     * <timer id="timezone_eastern_timer_ID">
     * <schedule>
     * <second>20</second>
     * <minute>*</minute>
     * <hour>*</hour>
     * </schedule>
     * <info>timeZoneEastern</info>
     * <end>2050</end>
     * <timeout-method id="timezone_eastern_method_ID">
     * <method-name>timeZoneEastern</method-name>
     * <method-params id="timeZoneEastern_param1_ID">
     * <method-param>javax.ejb.Timer</method-param>
     * </method-params>
     * </timeout-method>
     * <timezone>America/Iqaluit</timezone>
     * </timer>
     */
    public void timeZoneEastern(Timer timer) {
        svLogger.info("The timeZoneEastern method got called back into.");

        String inputTimezone = timer.getSchedule().getTimezone();
        time_zone_zone = inputTimezone;
        time_zone_count++;
        time_zone_timestamps.add(Long.valueOf(System.currentTimeMillis()));
    }
    // end F743-16271

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

    public void clearAllTimers() {
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

}
