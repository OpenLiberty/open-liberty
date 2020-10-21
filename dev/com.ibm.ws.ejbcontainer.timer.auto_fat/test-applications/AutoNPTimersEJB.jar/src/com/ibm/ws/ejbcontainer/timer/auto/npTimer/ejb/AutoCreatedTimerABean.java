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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.ejb.Schedules;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Stateless(name = "AutoCreatedTimerABean")
@Local(AutoCreatedTimerA.class)
public class AutoCreatedTimerABean {
    private static final String CLASS_NAME = AutoCreatedTimerABean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static String SECONDS_EXACT = "seconds_exact";
    public static volatile int seconds_exact_count = 0;
    public static ArrayList<Long> seconds_exact_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> seconds_exact_next = new ArrayList<Long>();

    public static String SECONDS_INTERVAL = "seconds_interval";
    public static volatile int seconds_interval_count = 0;
    public static ArrayList<Long> seconds_interval_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> seconds_interval_next = new ArrayList<Long>();

    public static String MINUTES_INTERVAL = "minutes_interval";
    public static volatile int minutes_interval_count = 0;
    public static ArrayList<Long> minutes_interval_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> minutes_interval_next = new ArrayList<Long>();

    public static String MONTH_RANGE = "month_range";
    public static volatile int month_range_count = 0;
    public static ArrayList<Long> month_range_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> month_range_next = new ArrayList<Long>();

    public static String RANGE_AND_LIST = "range_and_list";
    public static volatile int range_and_list_count = 0;
    public static ArrayList<Long> range_and_list_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> range_and_list_next = new ArrayList<Long>();

    public static final String MULTIPLE_ATTRIBUTES_COMBINED = "multiple_attributes_combined";
    public static volatile int multiple_attributes_combined_count = 0;
    public static ArrayList<Long> multiple_attributes_combined_timestamps = new ArrayList<Long>();
    public static ArrayList<Long> multiple_attributes_combined_next = new ArrayList<Long>();

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

    public static final String MINUTES_EXACT = "minutes_exact";
    public static final String HOURS_EXACT = "hours_exact";
    public static final String HOURS_RANGE = "hours_range";
    public static final String DAY_OF_WEEK_RANGE = "dayOfWeek_range";
    public static final String DAY_OF_WEEK_LIST = "dayOfWeek_list";
    public static final String DAY_OF_MONTH_EXACT = "dayOfMonth_exact";
    public static final String DAY_OF_MONTH_LIST = "dayOfMonth_list";
    public static final String MONTH_EXACT = "month_exact";
    public static final String YEAR_EXACT = "year_exact";
    public static final String DAY_OF_MONTH_NEGATIVE = "dayOfMonth_negative";
    public static final String DAY_OF_MONTH_3RD_SUNDAY_SYNTAX = "dayOfMonth_3rd_sunday_syntax";
    public static final String TIMEZONE_SET = "timezone_set";

    @Resource
    private TimerService ivTS;

    //A lot of these timers are going to keep firing, at a rate of a least once a minute, for as long as the app is installed.
    //Each time they fire, we are storing more data in a list.  To avoid having a situation where the app is installed for
    //a long time, and we fill up the lists with so much data that we run out of memory, the number of adds we make to the
    //list is limited to 25, because no test should need data from more than that number of executions.

    @Schedule(hour = "*", minute = "*", second = "30", persistent = false)
    public void seconds_exact(Timer timer) {
        svLogger.info("The seconds_exact method got called back into.");
        if (seconds_exact_count < 25) {
            seconds_exact_count++;
            seconds_exact_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            seconds_exact_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (seconds_exact_count == 4) {
                // Wait for at least 4 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("seconds_exact");
            }
        }
    }

    @Schedule(hour = "*", minute = "*", second = "30/10", persistent = false)
    public void seconds_interval(Timer timer) {
        if (seconds_interval_count < 25) {
            svLogger.info("The seconds_interval method got called back into.");
            seconds_interval_count++;
            seconds_interval_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            seconds_interval_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (seconds_interval_count == 12) {
                // Wait for at least 12 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("seconds_interval");
            }
        }
    }

    @Schedule(hour = "*", minute = "1", info = MINUTES_EXACT, persistent = false)
    public void minutes_exact(Timer timer) {
        svLogger.info("The minutes_exact method got called back into.");
    }

    @Schedule(hour = "*", minute = "*/1", persistent = false)
    public void minutes_interval(Timer timer) {
        if (minutes_interval_count < 25) {
            svLogger.info("The minutes_interval method got called back into.");
            minutes_interval_count++;
            minutes_interval_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            minutes_interval_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (minutes_interval_count == 4) {
                // Wait for at least 4 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("minutes_interval");
            }
        }
    }

    @Schedule(hour = "1", info = HOURS_EXACT, persistent = false)
    public void hours_exact(Timer timer) {
        svLogger.info("The hours_exact method got called back into.");
    }

    @Schedule(hour = "1-5", info = HOURS_RANGE, persistent = false)
    public void hours_range(Timer timer) {
        svLogger.info("The hours_range method got called back into.");
    }

    @Schedule(dayOfWeek = "4-6", info = DAY_OF_WEEK_RANGE, persistent = false)
    public void dayOfWeek_range(Timer timer) {
        svLogger.info("The dayOfWeek_range method got called back into.");
    }

    @Schedule(dayOfWeek = "1,3,5", info = DAY_OF_WEEK_LIST, persistent = false)
    public void dayOfWeek_list(Timer timer) {
        svLogger.info("The dayOfWeek_list method got called back into.");
    }

    @Schedule(dayOfMonth = "17", info = DAY_OF_MONTH_EXACT, persistent = false)
    public void dayOfMonth_exact(Timer timer) {
        svLogger.info("The dayOfMonth_exact method got called back into.");
    }

    @Schedule(dayOfMonth = "12,16,18", info = DAY_OF_MONTH_LIST, persistent = false)
    public void dayOfMonth_list(Timer timer) {
        svLogger.info("The dayOfMonth_list method got called back into.");
    }

    @Schedule(month = "fEb", info = MONTH_EXACT, persistent = false)
    public void month_exact(Timer timer) {
        svLogger.info("The month_exact method got called back into.");
    }

    @Schedule(minute = "*", hour = "*", month = "1-12", persistent = false)
    public void month_range(Timer timer) {
        if (month_range_count < 25) {
            svLogger.info("The month_range method got called back into.");
            month_range_count++;
            month_range_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            month_range_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (month_range_count == 3) {
                // Wait for at least 3 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("month_range");
            }
        }
    }

    @Schedule(minute = "*", hour = "*", year = "2050", info = YEAR_EXACT, persistent = false)
    public void year_exact(Timer timer) {
        svLogger.info("The year_exact method got called back into.");
    }

    @Schedule(dayOfMonth = "-3", info = DAY_OF_MONTH_NEGATIVE, persistent = false)
    public void dayOfMonth_negative(Timer timer) {
        svLogger.info("The dayOfMonth_negative method got called back into.");
    }

    @Schedule(dayOfMonth = "3rd Sun", info = DAY_OF_MONTH_3RD_SUNDAY_SYNTAX, persistent = false)
    public void dayOfMonth_thirdSundaySyntax(Timer timer) {
        svLogger.info("The dayOfMonth_thirdSundaySyntax method got called back into.");
    }

    @Schedule(timezone = "America/New_York", info = TIMEZONE_SET, persistent = false)
    public void timezone_set(Timer timer) {
        svLogger.info("The timezone_set method got called back into.");
    }

    @Schedule(minute = "*", hour = "*", dayOfMonth = "1,2,3,4,5,6,7, 8-Last", persistent = false)
    public void rangeAndList(Timer timer) {
        if (range_and_list_count < 25) {
            svLogger.info("The rangeAndList method got called back into.");
            range_and_list_count++;
            range_and_list_timestamps.add(Long.valueOf(System.currentTimeMillis()));
            range_and_list_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
            if (range_and_list_count == 3) {
                // Wait for at least 3 expirations before collecting results
                AutoCreatedTimerDriverBean.countDown("rangeAndList");
            }
        }
    }

    @Schedule(second = "30", minute = "*/2", hour = "*", persistent = false)
    public void multipleAttributesCombined(Timer timer) {
        svLogger.info("The multipleAttributesCombined method got called back into.");
        multiple_attributes_combined_count++;
        multiple_attributes_combined_timestamps.add(Long.valueOf(System.currentTimeMillis()));
        multiple_attributes_combined_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
        if (multiple_attributes_combined_count == 2) {
            // Wait for at least 2 expirations before collecting results
            AutoCreatedTimerDriverBean.countDown("multipleAttributesCombined");
        }
    }

    @Schedules({
                 @Schedule(second = "20", minute = "*", hour = "*", info = FIRST_SCHEDULE, persistent = false),
                 @Schedule(second = "10", minute = "*", hour = "*", info = SECOND_SCHEDULE, persistent = false)
    })
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
                if (first_schedule_count == 3) {
                    // Wait for at least 3 expirations before collecting results
                    AutoCreatedTimerDriverBean.countDown("first_schedule_count");
                }
            }
        } else if (SECOND_SCHEDULE.equals(info)) {
            if (second_schedule_count < 25) {
                svLogger.info("The atSchedulesMethod got called back into for the 2nd schedules timer...");
                second_schedule_count++;
                second_schedule_timestamps.add(Long.valueOf(System.currentTimeMillis()));
                second_schedule_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
                if (second_schedule_count == 3) {
                    // Wait for at least 3 expirations before collecting results
                    AutoCreatedTimerDriverBean.countDown("second_schedule_count");
                }
            }
        } else if (THIRD_SCHEDULE.equals(info)) {
            if (third_schedule_count < 25) {
                svLogger.info("The atSchedulesMethod got called back into for the 3rd schedule timer...");
                third_schedule_count++;
                third_schedule_timestamps.add(Long.valueOf(System.currentTimeMillis()));
                third_schedule_next.add(Long.valueOf(timer.getNextTimeout().getTime()));
                if (third_schedule_count == 3) {
                    // Wait for at least 3 expirations before collecting results
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

    public boolean getPersistentStatus(String infoToMatchOn) throws Exception {
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer oneTimer : timers) {
            String info = (String) oneTimer.getInfo();

            if (info != null && info.equals(infoToMatchOn)) {
                return oneTimer.isPersistent();
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
