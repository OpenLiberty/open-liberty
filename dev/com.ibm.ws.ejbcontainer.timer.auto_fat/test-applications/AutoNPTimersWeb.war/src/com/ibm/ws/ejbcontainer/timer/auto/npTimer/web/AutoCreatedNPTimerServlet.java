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

package com.ibm.ws.ejbcontainer.timer.auto.npTimer.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.AutoCreatedTimerABean;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.AutoCreatedTimerDriver;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.AutoCreatedTimerDriverBean;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.AutoCreatedTimerMBean;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.AutoCreatedTimerSSBean;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.AutoCreatedTimerXBean;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.ChildBean;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.GrandchildBean;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb.ParentBean;

@WebServlet("/AutoCreatedNPTimerServlet")
@SuppressWarnings({ "unchecked", "serial" })
public class AutoCreatedNPTimerServlet extends AbstractServlet {
    private static final String CLASS_NAME = AutoCreatedNPTimerServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    private AutoCreatedTimerDriver ivTestDriver;

    @Override
    public void setup() throws Exception {
        svLogger.entering(CLASS_NAME, "setUp");

        // Wait for some of the timers to expire a few times
        AutoCreatedTimerDriver driverBean = getDriverBean();
        driverBean.setup();

        svLogger.exiting(CLASS_NAME, "setUp");
    }

    @Override
    public void cleanup() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();
        driverBean.clearAllTimers();
    }

    private boolean verifyAttemptsIsAcceptable(int actualAttempts, int minimumAcceptableAttempts) {
        if (actualAttempts < minimumAcceptableAttempts) {
            log("The actual number of attempts **" + actualAttempts + "** was less than the minimum required attempts **" + minimumAcceptableAttempts + "**");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Looks up the bean that is used as the driver for these tests.
     *
     * The tests are physically run inside of an AppClient JVM, and so they need help to communicate with the server. The client side test method looks
     * up this bean and gives it directions, and then the 'driver' bean tells the various 'worker' beans to do the needed things (like create timers).
     */
    private AutoCreatedTimerDriver getDriverBean() throws Exception {
        return ivTestDriver;
    }

    /**
     * Writes log messages to both SystemOut and the test logs.
     *
     * Log messages that get written to the svLogger end up getting dumped into the 'results' output directory for the test. However, this data doesn't
     * appear to get physically written out to the disk until the testing has fully completed.
     *
     * These timer tests take a long time due to all of the delays built into them, and I want to be able to follow along with the progress in 'real time',
     * instead of being in the dark until everything is done. Thus, I'm writing out log messages to SystemOut as well, because that data will get sent to the
     * client log in 'real time'.
     */
    @Override
    public void log(String message) {
        System.out.println(message);
        svLogger.info(message);
        super.log(message);
    }

    private boolean verifyScheduleExpressionIsCorrect(ScheduleExpression schedule,
                                                      String expectedSeconds,
                                                      String expectedMinutes,
                                                      String expectedHours,
                                                      String expectedDayOfWeek,
                                                      String expectedDayOfMonth,
                                                      String expectedMonth,
                                                      String expectedYear,
                                                      String expectedTimezone) {
        if (schedule == null) {
            svLogger.info("The scheduled expression for the timer was null.");
            return false;
        }

        boolean secondsBad = false;
        boolean minutesBad = false;
        boolean hoursBad = false;
        boolean dayOfWeekBad = false;
        boolean dayOfMonthBad = false;
        boolean monthBad = false;
        boolean yearBad = false;
        boolean timezoneBad = false;

        if (expectedSeconds != null) {
            secondsBad = valueIsBad(schedule.getSecond(), expectedSeconds, "seconds");
        }
        if (expectedMinutes != null) {
            minutesBad = valueIsBad(schedule.getMinute(), expectedMinutes, "minutes");
        }
        if (expectedHours != null) {
            hoursBad = valueIsBad(schedule.getHour(), expectedHours, "hours");
        }
        if (expectedDayOfWeek != null) {
            dayOfWeekBad = valueIsBad(schedule.getDayOfWeek(), expectedDayOfWeek, "dayOfWeek");
        }
        if (expectedDayOfMonth != null) {
            dayOfMonthBad = valueIsBad(schedule.getDayOfMonth(), expectedDayOfMonth, "dayOfMonth");
        }
        if (expectedMonth != null) {
            monthBad = valueIsBad(schedule.getMonth(), expectedMonth, "month");
        }
        if (expectedYear != null) {
            yearBad = valueIsBad(schedule.getYear(), expectedYear, "year");
        }
        if (expectedTimezone != null) {
            timezoneBad = valueIsBad(schedule.getTimezone(), expectedTimezone, "timezone");
        }

        if (secondsBad ||
            minutesBad ||
            hoursBad ||
            dayOfWeekBad ||
            dayOfMonthBad ||
            monthBad ||
            yearBad ||
            timezoneBad) {
            return false;
        } else {
            return true;
        }

    }

    private boolean valueIsBad(String actual, String expected, String attribute) {
        if (expected.equals(actual)) {
            svLogger.info("For **" + attribute + "**, the actual value **" + actual + "** matches the expected value **" + expected + "**");
            return false;
        } else {
            svLogger.info("For **" + attribute + "**, the actual value **" + actual + "** does NOT match the expected value **" + expected + "**");
            return true;
        }
    }

    private HashSet<Integer> createListOfValidIntegers(int[] validInts) {
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (int i = 0; i < validInts.length; i++) {
            int x = validInts[i];
            hashSet.add(Integer.valueOf(x));
        }
        return hashSet;
    }

    private boolean verifyNextTimeoutIsCorrect(Date nextTimeout,
                                               HashSet<Integer> expectedSeconds,
                                               HashSet<Integer> expectedMinutes,
                                               HashSet<Integer> expectedHours,
                                               HashSet<Integer> expectedDayOfWeek,
                                               HashSet<Integer> expectedDayOfMonth,
                                               HashSet<Integer> expectedMonth,
                                               HashSet<Integer> expectedYear) {
        if (nextTimeout == null) {
            svLogger.info("The next timeout date for the timer was null.");
            return false;
        }

        Calendar nextTimeoutCalendar = Calendar.getInstance();
        nextTimeoutCalendar.setTime(nextTimeout);

        boolean secondsBad = false;
        boolean minutesBad = false;
        boolean hoursBad = false;
        boolean dayOfWeekBad = false;
        boolean dayOfMonthBad = false;
        boolean monthBad = false;
        boolean yearBad = false;

        if (expectedSeconds != null) {
            secondsBad = nextTimeoutValueIsBad(nextTimeoutCalendar.get(Calendar.SECOND), expectedSeconds, "seconds");
        }
        if (expectedMinutes != null) {
            minutesBad = nextTimeoutValueIsBad(nextTimeoutCalendar.get(Calendar.MINUTE), expectedMinutes, "minutes");
        }
        if (expectedHours != null) {
            hoursBad = nextTimeoutValueIsBad(nextTimeoutCalendar.get(Calendar.HOUR), expectedHours, "hours");
        }
        if (expectedDayOfWeek != null) {
            dayOfWeekBad = nextTimeoutValueIsBad(nextTimeoutCalendar.get(Calendar.DAY_OF_WEEK), expectedDayOfWeek, "dayOfWeek");
        }
        if (expectedDayOfMonth != null) {
            dayOfMonthBad = nextTimeoutValueIsBad(nextTimeoutCalendar.get(Calendar.DAY_OF_MONTH), expectedDayOfMonth, "dayOfMonth");
        }
        if (expectedMonth != null) {
            monthBad = nextTimeoutValueIsBad(nextTimeoutCalendar.get(Calendar.MONTH), expectedMonth, "month");
        }
        if (expectedYear != null) {
            yearBad = nextTimeoutValueIsBad(nextTimeoutCalendar.get(Calendar.YEAR), expectedYear, "year");
        }

        if (secondsBad ||
            minutesBad ||
            hoursBad ||
            dayOfWeekBad ||
            dayOfMonthBad ||
            monthBad ||
            yearBad) {
            return false;
        } else {
            return true;
        }

    }

    private boolean nextTimeoutValueIsBad(int actualValue, HashSet<Integer> expectedValues, String attribute) {
        Integer actualInteger = Integer.valueOf(actualValue);

        if (expectedValues.contains(actualInteger)) {
            log("For **" + attribute + "**, the actual value **" + actualInteger + "** matches the expected value(s) **" + expectedValues + "**");
            return false;
        } else {
            log("For **" + attribute + "**, the actual value **" + actualInteger + "** does NOT match the expected values(s) **" + expectedValues + "**");
            return true;
        }
    }

    private boolean verifyNextTimeoutsInterval(ArrayList<Long> nextTimeouts, long firstOffset, long interval) {
        if (nextTimeouts == null) {
            svLogger.info("The next timeout list for the timer was null.");
            return false;
        }

        Long previousTimeout = null;
        for (Long nextTimeout : nextTimeouts) {
            if (previousTimeout == null) {
                if ((nextTimeout - firstOffset) % interval != 0) {
                    svLogger.info("next timeout not scheduled on correct boundary : timeout=" + nextTimeout + ", firstOffset=" + firstOffset + ", interval=" + interval);
                    svLogger.info("next timeouts=" + nextTimeouts);
                    return false;
                }
            } else if (nextTimeout - previousTimeout != interval) {
                svLogger.info("next timeout not scheduled at correct interval : previous=" + previousTimeout + ", next=" + nextTimeout + ", interval=" + interval);
                svLogger.info("next timeouts=" + nextTimeouts);
                return false;
            }
            previousTimeout = nextTimeout;
        }
        return true;
    }

    private boolean verifyNextTimeoutsRange(ArrayList<Long> nextTimeouts, long firstOffset, long interval, long range, long rangeInterval) {
        if (nextTimeouts == null) {
            svLogger.info("The next timeout list for the timer was null.");
            return false;
        }

        Long previousTimeout = null;
        long nextDifference = 0;
        long rangeIndex = 0;
        long maxRangeIndex = range - 1;
        long endOfRangeToNextRange = interval - (maxRangeIndex * rangeInterval);
        for (Long nextTimeout : nextTimeouts) {
            if (previousTimeout == null) {
                long offset = nextTimeout % interval;
                while (offset > firstOffset && rangeIndex < maxRangeIndex) {
                    offset -= rangeInterval;
                    ++rangeIndex;
                }
                if (offset < firstOffset) {
                    svLogger.info("next timeout not scheduled on correct range boundary : timeout=" + nextTimeout +
                                  ", firstOffset=" + firstOffset + ", interval=" + interval +
                                  ", range=" + range + ", rangeInterval=" + rangeInterval);
                    svLogger.info("next timeouts=" + nextTimeouts);
                    return false;
                }
                if (offset > firstOffset) {
                    svLogger.info("next timeout scheduled outside of range : timeout=" + nextTimeout +
                                  ", firstOffset=" + firstOffset + ", interval=" + interval +
                                  ", range=" + range + ", rangeInterval=" + rangeInterval + ", rangeIndex=" + rangeIndex);
                    svLogger.info("next timeouts=" + nextTimeouts);
                    return false;
                }
            } else if (nextTimeout - previousTimeout != nextDifference) {
                svLogger.info("next timeout not scheduled at correct interval : previous=" + previousTimeout +
                              ", next=" + nextTimeout + ", nextDifference=" + nextDifference +
                              ", rangeIndex=" + rangeIndex);
                svLogger.info("next timeouts=" + nextTimeouts);
                return false;
            }

            previousTimeout = nextTimeout;
            rangeIndex = rangeIndex == maxRangeIndex ? 0 : rangeIndex + 1;
            nextDifference = rangeIndex == 0 ? endOfRangeToNextRange : rangeInterval;
        }
        return true;
    }

    // -----------------------------------------------------------------------------------------
    // Note about validating the actual time of the timeouts:
    //
    // Unfortunately, it looks like its not possible to ensure the exact second that timeouts
    // will occur. Most of the time, for most of the tests, the timeouts *do* occur at the exact
    // second they were configured for. For example, if we configure a timer to go off at 30
    // seconds past the minute, in most cases it does exactly that.
    //
    // However, some of the time in some of the tests, we don't get the exact second for a number
    // of reasons...sometimes the timer starts to go off in the correct second, but by the time
    // we actually get into the timeout method and record the timestamp the clock has already
    // rolled over into the next second...or the exact second that the timer is suppose to fire
    // just so happens to come around after the timer has been created, but before the
    // application is completely started, and so the timer tries to fire and we block it, and
    // the timer keeps retrying itself until the application is actually up, and when the
    // application does come up and the timer is allowed to fire its now past the exact second
    // it was suppose to go off at, and so according to the timestamp it fired at the 'wrong'
    // time (even though our behavior was actually correct).
    //
    // Worst of all, these 'false failures' are random....they will occur in a method in one run
    // (because the timing of the application startup was unlucky) and then not occur in the
    // next run (because the timing of the application startup was lucky), etc.
    //
    // The net is that if we check for an exact second on the timeout, we'll likely be forever
    // chasing false failures in the automated moonstone testing that aren't actually real
    // problems (but rather, the result of unlucky server startup timing), and which we'll
    // waste a bunch of time trying unsuccessfully to reproduce them in our own environment.
    //
    // So, to prevent this, for the tests with timeouts that are expected to occur, the tests
    // will just verify the timeout occurred (and not worry about exactly when occurred) and
    // also collect and verify that the next scheduled timeout(s) is/are correct.
    // -----------------------------------------------------------------------------------------

    /**
     * Verifies that exact seconds are respected.
     */
    @Test
    public void testSeconds_exact() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerABean.SECONDS_EXACT);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 4);
        boolean validNextTimeouts = verifyNextTimeoutsInterval(nextTimeouts, 30 * 1000, 60 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that a range of seconds is respected.
     */
    @Test
    public void testSeconds_range() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerXBean.SECONDS_RANGE);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 15);
        boolean validNextTimeouts = verifyNextTimeoutsRange(nextTimeouts, 30 * 1000, 60 * 1000, 7, 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that an interval of seconds is respected.
     */
    @Test
    public void testSeconds_interval() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerABean.SECONDS_INTERVAL);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 12);
        boolean validNextTimeouts = verifyNextTimeoutsRange(nextTimeouts, 30 * 1000, 60 * 1000, 3, 10 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that a list of seconds is respected.
     */
    @Test
    public void testSeconds_list() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerXBean.SECONDS_LIST);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 9);
        boolean validNextTimeouts = verifyNextTimeoutsRange(nextTimeouts, 35 * 1000, 60 * 1000, 3, 10 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that an exact minute is respected.
     */
    @Test
    public void testMinutes_exact() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.MINUTES_EXACT);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      "1", //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                createListOfValidIntegers(new int[] { 1 }), //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the exact minute specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that an interval of minutes is respected.
     */
    @Test
    public void testMinutes_interval() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerABean.MINUTES_INTERVAL);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 4);
        boolean validNextTimeouts = verifyNextTimeoutsInterval(nextTimeouts, 60 * 1000, 60 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that a range of minutes is acceptable.
     */
    @Test
    public void testMinutes_range() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerXBean.MINUTES_RANGE);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 4);
        boolean validNextTimeouts = verifyNextTimeoutsRange(nextTimeouts, 60 * 1000, 60 * 60 * 1000, 59, 60 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that a list of minutes is respected.
     */
    @Test
    public void testMinutes_list() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_X, AutoCreatedTimerXBean.MINUTES_LIST);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      "4, 37", //minutes...space is required to match results
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                createListOfValidIntegers(new int[] { 4, 37 }), //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the list of minutes specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);

    }

    /**
     * Verifies that a range of hours is respected.
     */
    @Test
    public void testHours_range() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.HOURS_RANGE);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      "1-5", //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                createListOfValidIntegers(new int[] { 1, 2, 3, 4, 5 }), //hours
                                                                null, //day of week
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the range of hours specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);

    }

    /**
     * Verifies that an interval of hours is respected.
     */
    @Test
    public void testHours_interval() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_X, AutoCreatedTimerXBean.HOURS_INTERVAL);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      "*/2", //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                createListOfValidIntegers(new int[] { 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22 }), //hours
                                                                null, //day of week
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the interval of hours specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);

    }

    /**
     * Verifies that an exact hour is respected.
     */
    @Test
    public void testHours_exact() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.HOURS_EXACT);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      "1", //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                createListOfValidIntegers(new int[] { 1 }), //hours
                                                                null, //day of week
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the exact hour specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that a list of hours is respected.
     */
    @Test
    public void testHours_list() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_X, AutoCreatedTimerXBean.HOURS_LIST);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      "3, 17, 23", //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                createListOfValidIntegers(new int[] { 3, 5, 11 }), //hours....hour '17' shows up in Calendar as 5 (ie, 5:00 pm), and hour '23' shows up in Calendar as 11 (ie, 11:00 pm)
                                                                null, //day of week
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the list of hours specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that an exact day of the week is respected.
     */
    @Test
    public void testDayOfWeek_exact() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_X, AutoCreatedTimerXBean.DAY_OF_WEEK_EXACT);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      "Sat", //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                createListOfValidIntegers(new int[] { 7 }), //day of week
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the exact day-of-week specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that a range of days of the week is respected.
     */
    @Test
    public void testDayOfWeek_range() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.DAY_OF_WEEK_RANGE);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      "4-6", //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                createListOfValidIntegers(new int[] { 5, 6, 7 }), //day of week...according to calendar obj, thur=5, fri=6, sat=7
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the day-of-week range specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that a list of days of the week is respected.
     */
    @Test
    public void testDayOfWeek_list() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.DAY_OF_WEEK_LIST);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      "1,3,5", //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                createListOfValidIntegers(new int[] { 2, 4, 6 }), //day of week
                                                                null, //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the day-of-week list specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that an exact day-of-the-month is respected.
     */
    @Test
    public void testDayOfMonth_exact() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.DAY_OF_MONTH_EXACT);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      "17", //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                createListOfValidIntegers(new int[] { 17 }), //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the exact day-of-month specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that a range of days of the month is respected.
     */
    @Test
    public void testDayOfMonth_range() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_X, AutoCreatedTimerXBean.DAY_OF_MONTH_RANGE);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      "12-14", //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                createListOfValidIntegers(new int[] { 12, 13, 14 }), //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the day-of-month range specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that a list of days of the month is respected.
     */
    @Test
    public void testDayOfMonth_list() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.DAY_OF_MONTH_LIST);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      "12,16,18", //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                createListOfValidIntegers(new int[] { 12, 16, 18 }), //day of month
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the day-of-month list specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that an exact month is respected.
     */
    @Test
    public void testMonth_exact() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.MONTH_EXACT);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      "fEb", //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                null, //day of month
                                                                createListOfValidIntegers(new int[] { 1 }), //month...Feb is month 1 according to the Calendar object we are validating against
                                                                null); //year

        assertTrue("The timer data did not reflect the exact month specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);
    }

    /**
     * Verifies that range of months is respected.
     */
    @Test
    public void testMonth_range() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerABean.MONTH_RANGE);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 3);
        boolean validNextTimeouts = verifyNextTimeoutsInterval(nextTimeouts, 60 * 1000, 60 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that a list of months is respected.
     */
    @Test
    public void testMonth_list() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_X, AutoCreatedTimerXBean.MONTH_LIST);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      "1,4,9", //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                null, //day of month
                                                                createListOfValidIntegers(new int[] { 0, 3, 8 }), //month...Calendar object we are validating against starts months from 0, so need to decrement each month by 1
                                                                null); //year

        assertTrue("The timer data did not reflect the list of months specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);

    }

    /**
     * Verifies that an exact year is respected.
     */
    @Test
    public void testYear_exact() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.YEAR_EXACT);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      "2050", //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                null, //day of month
                                                                null, //month
                                                                createListOfValidIntegers(new int[] { 2050 })); //year

        assertTrue("The timer data did not reflect the list of months specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);

    }

    /**
     * Verifies that a day of the month, specified in a negative form, is respected.
     */
    @Test
    public void testDayOfMonth_negative() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.DAY_OF_MONTH_NEGATIVE);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      "-3", //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                createListOfValidIntegers(new int[] { 24, 25, 26, 27, 28 }), //day of month...month could have 31 to 27 days in it...so 3 less than those is 28-24
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the negative day-of-month specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);

    }

    /**
     * Specifies that the Last keyword is respected for days-of-month.
     */
    @Test
    public void testDayOfMonth_last() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_X, AutoCreatedTimerXBean.DAY_OF_MONTH_LAST);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);
        Date nextTimeout = (Date) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      "Last", //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        boolean nextTimeoutCorrect = verifyNextTimeoutIsCorrect(nextTimeout,
                                                                null, //seconds
                                                                null, //minutes
                                                                null, //hours
                                                                null, //day of week
                                                                createListOfValidIntegers(new int[] { 27, 28, 29, 30, 31 }), //last day of month could be 27-31
                                                                null, //month
                                                                null); //year

        assertTrue("The timer data did not reflect the Last day of month specified.", scheduleIsCorrect);
        assertTrue("The next-timeout data was not correct.", nextTimeoutCorrect);

    }

    /**
     * Verifies that the 3rd Sun style of syntax is respected for days of month.
     */
    @Test
    public void testDayOfMonth_3rdSundaySyntax() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.DAY_OF_MONTH_3RD_SUNDAY_SYNTAX);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      "3rd Sun", //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      null); //timezone

        assertTrue("The timer data did not reflect the 3rd sunday of the month syntax specified.", scheduleIsCorrect);

    }

    /*
     * Verifies that the persistent flag can be explicitly set to true.
     */
    /*
     * public void testPersistentFlagExplicitlySetToTrue() throws Exception
     * {
     * AutoCreatedTimerDriver driverBean = getDriverBean();
     *
     * boolean persistent = driverBean.getTimerPersistentStatus(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.HOURS_EXACT);
     * assertTrue("The timer said it was non-persistent, even though it was explicitly configured to be persistent.", persistent);
     * }
     */

    /**
     * Verifies that the persistent flag can be explicitly set to false.
     */
    @Test
    public void testPersistentFlagExplicitlySetToFalse() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        boolean persistent = driverBean.getTimerPersistentStatus(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.DAY_OF_WEEK_RANGE);
        assertFalse("The timer said it was persistent, even though it was explicitly configured to be non-persistent.", persistent);
    }

    /*
     * Verifies that the persistent flag defaults to true.
     */
    /*
     * public void testPersistentFlagDefaultsToTrue() throws Exception
     * {
     * AutoCreatedTimerDriver driverBean = getDriverBean();
     *
     * boolean persistent = driverBean.getTimerPersistentStatus(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.DAY_OF_WEEK_LIST);
     * assertTrue("The timer said it was non-persistent, even though it should have been defaulted to persistent.", persistent);
     * }
     */

    /**
     * Verifies that you can explicitly set the timezone.
     */
    @Test
    public void testTimezone_set() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.TIMEZONE_SET);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);

        boolean scheduleIsCorrect = verifyScheduleExpressionIsCorrect(schedule,
                                                                      null, //seconds
                                                                      null, //minutes
                                                                      null, //hours
                                                                      null, //dayOfWeek
                                                                      null, //dayOfMonth
                                                                      null, //month
                                                                      null, //year
                                                                      "America/New_York"); //timezone

        assertTrue("The timer data did not reflect the timezone specified.", scheduleIsCorrect);

    }

    @Test
    public void testTimezone_default() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_A, AutoCreatedTimerABean.HOURS_EXACT);
        ScheduleExpression schedule = (ScheduleExpression) props.get(AutoCreatedTimerDriverBean.SCHEDULE_KEY);

        String timezone = schedule.getTimezone();
        boolean scheduleIsCorrect = (timezone == null) ? true : false;

        assertTrue("The default timezone **" + timezone + "** data was non null.", scheduleIsCorrect);

    }

    /**
     * Verifies that you can specify both dayOfMonth and dayOfWeek, and that if either one is true, even when the other is
     * not, the timer still fires.
     */
    @Test
    public void testMultipleSettingsDontConflict() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerXBean.MULTIPLE_SETTINGS_DONT_CONFLICT);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 3);
        boolean validNextTimeouts = verifyNextTimeoutsInterval(nextTimeouts, 60 * 1000, 60 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that you can specify a range, and a list, in the same attribute.
     */
    @Test
    public void testRangeAndListInSameAttribute() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerABean.RANGE_AND_LIST);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 3);
        boolean validNextTimeouts = verifyNextTimeoutsInterval(nextTimeouts, 60 * 1000, 60 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that you can have multiple attributes, like second and minute, and they are both respected and are combined
     * to create the timeout schedule.
     */
    @Test
    public void testMultipleAttributesCombined() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerABean.MULTIPLE_ATTRIBUTES_COMBINED);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts = (ArrayList<Long>) props.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        boolean validTimeoutCount = verifyAttemptsIsAcceptable(count, 2);
        boolean validNextTimeouts = verifyNextTimeoutsInterval(nextTimeouts, 30 * 1000, 2 * 60 * 1000);

        assertTrue("There were not enough timeouts.", validTimeoutCount);
        assertTrue("The next scheduled timeout values were not correct.", validNextTimeouts);
    }

    /**
     * Verifies that you can have a Schedules, Schedule, and Timeout annotation on the same method, and that all get respected.
     */
    @Test
    public void testAtSchedulesWithAtSchedule() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        driverBean.driveCreationOfProgramaticTimer();
        driverBean.waitForProgramaticTimer(6000);

        Properties firstProps = driverBean.getTimeoutResults(AutoCreatedTimerABean.FIRST_SCHEDULE);
        int count1 = ((Integer) firstProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts1 = (ArrayList<Long>) firstProps.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        Properties secondProps = driverBean.getTimeoutResults(AutoCreatedTimerABean.SECOND_SCHEDULE);
        int count2 = ((Integer) secondProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts2 = (ArrayList<Long>) secondProps.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        Properties thirdProps = driverBean.getTimeoutResults(AutoCreatedTimerABean.THIRD_SCHEDULE);
        int count3 = ((Integer) thirdProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();
        ArrayList<Long> nextTimeouts3 = (ArrayList<Long>) thirdProps.get(AutoCreatedTimerDriverBean.NEXT_TIMEOUT_KEY);

        Properties fourthProps = driverBean.getTimeoutResults(AutoCreatedTimerABean.PROGRAMATIC_TIMEOUT);
        int count4 = ((Integer) fourthProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();

        boolean validTimeoutCount1 = verifyAttemptsIsAcceptable(count1, 3);
        boolean validNextTimeouts1 = verifyNextTimeoutsInterval(nextTimeouts1, 20 * 1000, 60 * 1000);

        boolean validTimeoutCount2 = verifyAttemptsIsAcceptable(count2, 3);
        boolean validNextTimeouts2 = verifyNextTimeoutsInterval(nextTimeouts2, 10 * 1000, 60 * 1000);

        boolean validTimeoutCount3 = verifyAttemptsIsAcceptable(count3, 3);
        boolean validNextTimeouts3 = verifyNextTimeoutsInterval(nextTimeouts3, 40 * 1000, 60 * 1000);

        assertTrue("There were not enough timeouts for the 1st @Schedule.", validTimeoutCount1);
        assertTrue("There were not enough timeouts for the 2nd @Schedule.", validTimeoutCount2);
        assertTrue("There were not enough timeouts for the 3rd @Schedule.", validTimeoutCount3);
        assertEquals("There were not enough timeouts for the programatic timer.", 1, count4);
        assertTrue("The next scheduled timeout values for the 1st @Schedule were not correct.", validNextTimeouts1);
        assertTrue("The next scheduled timeout values for the 2st @Schedule were not correct.", validNextTimeouts2);
        assertTrue("The next scheduled timeout values for the 3st @Schedule were not correct.", validNextTimeouts3);
    }

    /**
     * Verifies that the xml start tag prevents the timer from firing, when the specified start time hasn't been reached.
     */
    @Test
    public void testStartGate() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties firstProps = driverBean.getTimeoutResults(AutoCreatedTimerXBean.START_GATE);
        int count = ((Integer) firstProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();

        boolean noHits = (count < 1) ? true : false;
        assertTrue("The start attribute did not prevent timeouts from occuring, but it should have.", noHits);
    }

    /**
     * Verifies that the xml end tag does not prevent timer from firing, when the specified end time has not been reached.
     */
    @Test
    public void testStopGate() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties firstProps = driverBean.getTimeoutResults(AutoCreatedTimerXBean.STOP_GATE);
        int count = ((Integer) firstProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();

        boolean hadHits = (count < 1) ? false : true;
        assertTrue("The stop attribute prevented timeouts from occuring, but it should not have.", hadHits);
    }

    // begin F743-16271
    /**
     * Verifies that the <timezone> element is read from ejb-jar.xml.
     *
     * Method AutoCreatedTimerXBean.timeZoneEastern will fire, and report that it's
     * input Timer object has the identifiably different but equivalent America/Iqaluit timezone
     * which is coded in the <timer id="timezone_eastern_timer_ID"> element as <timezone>America/Iqaluit</timezone>
     */
    @Test
    public void testTimeZone() throws Exception {

        String expectedZone = "America/Iqaluit";

        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties firstProps = driverBean.getTimeoutResults(AutoCreatedTimerXBean.TIME_ZONE);
        int count = ((Integer) firstProps.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();

        boolean hadHits = (count < 1) ? false : true;
        assertTrue("The timeZone timeout method did not fire.", hadHits);

        String timeZone = (String) firstProps.get(AutoCreatedTimerDriverBean.TIMEZONE_KEY);
        boolean foundExpected = timeZone.equals(expectedZone);
        assertTrue("Expected timezone of " + expectedZone + " but found " + timeZone, foundExpected);
    }
    // end F743-16271

    /**
     * Verifies that a timer stanza in xml overrides a Schedule annotation in the code.
     *
     */
    @Test
    public void testXMLOverridesAtSchedule() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties propsANO = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.FIRST_OVERRIDE_ANNOTATION);
        Properties propsXML = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.FIRST_OVERRIDE_XML);

        boolean noAnnotation = (propsANO == null) ? true : false;
        boolean yesXML = (propsXML != null) ? true : false;

        assertTrue("The timer defined via annoation existed, but it should have been overriden by the timer defined via xml.", noAnnotation);
        assertTrue("The timer defined via xml did not exist, but it should have.", yesXML);
    }

    /**
     * Verifies that a timer stanza in xml overrides a Schedules annotation in the code.
     *
     */
    @Test
    public void testXMLOverridesAtSchedules() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties propsAno1 = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.SECOND_OVERRIDE_ANNOTATION);
        Properties propsAno2 = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.THIRD_OVERRIDE_ANNOTATION);
        Properties propsXML = driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_M, AutoCreatedTimerMBean.SECOND_OVERRIDE_XML);

        boolean noAnnotation1 = (propsAno1 == null) ? true : false;
        boolean noAnnotation2 = (propsAno2 == null) ? true : false;
        boolean yesXML = (propsXML != null) ? true : false;

        assertTrue("The first timer defined via annoation existed, but it should have been overriden by the timer defined via xml.", noAnnotation1);
        assertTrue("The second timer defined via annoation existed, but it should have been overriden by the timer defined via xml.", noAnnotation2);
        assertTrue("The timer defined via xml did not exist, but it should have.", yesXML);
    }

    @Test
    public void testSingletonStartupCanSeeItsTimers() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        boolean singletonStartupSawItsTimer = driverBean.didSingletonStartupBeanSeeItsTimer();

        assertTrue("The singleton startup bean did not see its timer in the postConstruct() method.", singletonStartupSawItsTimer);
    }

    /**
     * Verifies that when no info is specified at timer creation, the info object returned is null.
     */
    @Test
    public void testGetInfoReturnsNullWhenNothingSpecified() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        driverBean.getTimerScheduleData(AutoCreatedTimerDriverBean.BEAN_NI, null);

        boolean foundTimer = driverBean.didNIBeanFindItsTimer();
        boolean hadNullInfo = driverBean.didNIBeanHaveNullInfo();

        assertTrue("The NI bean failed to find its time.", foundTimer);
        assertTrue("The timer info data was not null, but it should have been.", hadNullInfo);
    }

    /**
     * Verifies that timers can be created from a singleton startup bean.
     *
     */
    @Test
    public void testTimeoutsWorkFromSingletonStartup() throws Exception {
        AutoCreatedTimerDriver driverBean = getDriverBean();

        Properties props = driverBean.getTimeoutResults(AutoCreatedTimerSSBean.SINGLETON_STARTUP);
        int count = ((Integer) props.get(AutoCreatedTimerDriverBean.COUNT_KEY)).intValue();

        boolean validCount = verifyAttemptsIsAcceptable(count, 1);

        assertTrue("Timeout method on singleton startup bean was not invoked.", validCount);
    }

    /**
     * Verifies that various timer inheritence scenarios are handled properly.
     *
     * Verifies that private methods are not overridden.
     *
     * Verifies that non-private methods are overridden.
     *
     * Verifies that a mix of private and non-private methods are both overridden
     * and not overridden.
     *
     * @throws Exception
     */
    @Test
    public void testTimerInheritence() throws Exception {
        // The test uses the following bean heirarchy:
        //
        // ParentBean
        //       public method_one()
        //       private method_two()
        //       private method_three()
        //
        // ChildBean extends ParentBean
        //       public method_one()
        //       private method_two()
        //       public method_three()
        //
        // GrandchildBean extends ChildBean
        //       public method_one() // NOT a timer method
        //       public method_three()

        // Each time a bean is processed, a Timer instance is created for any timer
        // defined in the class heirarchy (even for a private method on a parent
        // class), except when the timer method on the parent is overridden.
        //
        // Thus, the above class heirarchy should result in the following Timer
        // instances being created and associated with each bean class:

        // ParentBean:
        //    Timer instance 1...calling into ParentBean.method_one()
        //    Timer instance 2...calling into ParentBean.method_two()
        //    Timer instance 3...calling into ParentBean.method_three()
        //
        // ChildBean:
        //    Timer instance 4...calling into ChildBean.method_one()
        //    Timer instance 5...calling into ChildBean.method_two()
        //    Timer instance 6...calling into ChildBean.method_three()
        //    Timer instance 7...calling into ParentBean.method_two()
        //    Timer instance 8...calling into ParentBean.method_three()
        //
        //    note: We do not have a Timer instance calling into ParentBean.method_one(),
        //          because that method is overridden by ChildBean.method_one()
        //
        // GrandchildBean:
        //    Timer instance 9...calling into GrandchildBean.method_three()
        //    Timer instance 10...calling into ChildBean.method_two()
        //    Timer instance 11...calling into ParentBean.method_two()
        //    Timer instance 12...calling into ParentBean.method_three()
        //
        //    note 1: We do not have a Timer instance calling into ParentBean.method_one(),
        //            because its overridden by ChildBean.method_one()
        //
        //    note 2: We do not have a Timer instance calling into ChildBean.method_three(),
        //            because its overridden by GrandchildBean.method_three()
        //
        //    note 3: We do not a Timer instance calling into ChildBean.method_one() because
        //            that is overridden by non-timer method GrandchildBean.method_one()

        // Verify that each of the three beans had the correct list of timers
        // associated with it.
        //
        // The expected number of timers, and the info associated with them
        // (the info is the name of the method they are targeting) is shown
        // above.
        AutoCreatedTimerDriver driverBean = getDriverBean();

        HashSet<String> timersForParentBean = driverBean.getTimerInfos(AutoCreatedTimerDriverBean.PARENT_BEAN);
        log("Timer infos for ParentBean: " + timersForParentBean);
        assertEquals("ParentBean had an unexpected number of timers associated with it.", 3, timersForParentBean.size());
        assertTrue("ParentBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_ONE,
                   timersForParentBean.contains(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_ONE));
        assertTrue("ParentBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO,
                   timersForParentBean.contains(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO));
        assertTrue("ParentBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE,
                   timersForParentBean.contains(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE));

        HashSet<String> timersForChildBean = driverBean.getTimerInfos(AutoCreatedTimerDriverBean.CHILD_BEAN);
        log("Timer infos for ChildBean: " + timersForChildBean);
        assertEquals("ChildBean had an unexpected number of timers associated with it.", 5, timersForChildBean.size());
        assertTrue("ChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_ONE,
                   timersForChildBean.contains(AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_ONE));
        assertTrue("ChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_TWO,
                   timersForChildBean.contains(AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_TWO));
        assertTrue("ChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_THREE,
                   timersForChildBean.contains(AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_THREE));
        assertTrue("ChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO,
                   timersForChildBean.contains(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO));
        assertTrue("ChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE,
                   timersForChildBean.contains(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE));

        HashSet<String> timersForGrandChildBean = driverBean.getTimerInfos(AutoCreatedTimerDriverBean.GRANDCHILD_BEAN);
        log("Timer infos for GrandchildBean: " + timersForGrandChildBean);
        assertEquals("GrandChildBean had an unexpected number of timers associated with it.", 4, timersForGrandChildBean.size());
        assertTrue("GrandChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.GRANDCHILD_BEAN_METHOD_THREE,
                   timersForGrandChildBean.contains(AutoCreatedTimerDriverBean.GRANDCHILD_BEAN_METHOD_THREE));
        assertTrue("GrandChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_TWO,
                   timersForGrandChildBean.contains(AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_TWO));
        assertTrue("GrandChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO,
                   timersForGrandChildBean.contains(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO));
        assertTrue("GrandChildBean did contain have expected timer targeting " + AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE,
                   timersForGrandChildBean.contains(AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE));

        // Verify that these timer methods got invoked by Timers associated with the correct class
        ArrayList<String> errorMessages = new ArrayList<String>();
        HashMap<String, HashSet<Class<?>>> timersToInvokingClasses = driverBean.getTimerMethodToInvokingClassMap();

        HashSet<Class<?>> expectedInvokersForParentMethodOne = new HashSet<Class<?>>();
        expectedInvokersForParentMethodOne.add(ParentBean.class);
        verifyInvokingClassesAreCorrect(timersToInvokingClasses,
                                        AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_ONE,
                                        expectedInvokersForParentMethodOne,
                                        errorMessages);

        HashSet<Class<?>> expectedInvokersForParentMethodTwo = new HashSet<Class<?>>();
        expectedInvokersForParentMethodTwo.add(ParentBean.class);
        expectedInvokersForParentMethodTwo.add(ChildBean.class);
        expectedInvokersForParentMethodTwo.add(GrandchildBean.class);
        verifyInvokingClassesAreCorrect(timersToInvokingClasses,
                                        AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_TWO,
                                        expectedInvokersForParentMethodTwo,
                                        errorMessages);

        HashSet<Class<?>> expectedInvokersForParentMethodThree = new HashSet<Class<?>>();
        expectedInvokersForParentMethodThree.add(ParentBean.class);
        expectedInvokersForParentMethodThree.add(ChildBean.class);
        expectedInvokersForParentMethodThree.add(GrandchildBean.class);
        verifyInvokingClassesAreCorrect(timersToInvokingClasses,
                                        AutoCreatedTimerDriverBean.PARENT_BEAN_METHOD_THREE,
                                        expectedInvokersForParentMethodThree,
                                        errorMessages);

        HashSet<Class<?>> expectedInvokersForChildMethodOne = new HashSet<Class<?>>();
        expectedInvokersForChildMethodOne.add(ChildBean.class);
        verifyInvokingClassesAreCorrect(timersToInvokingClasses,
                                        AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_ONE,
                                        expectedInvokersForChildMethodOne,
                                        errorMessages);

        HashSet<Class<?>> expectedInvokersForChildMethodTwo = new HashSet<Class<?>>();
        expectedInvokersForChildMethodTwo.add(ChildBean.class);
        expectedInvokersForChildMethodTwo.add(GrandchildBean.class);
        verifyInvokingClassesAreCorrect(timersToInvokingClasses,
                                        AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_TWO,
                                        expectedInvokersForChildMethodTwo,
                                        errorMessages);

        HashSet<Class<?>> expectedInvokersForChildMethodThree = new HashSet<Class<?>>();
        expectedInvokersForChildMethodThree.add(ChildBean.class);
        verifyInvokingClassesAreCorrect(timersToInvokingClasses,
                                        AutoCreatedTimerDriverBean.CHILD_BEAN_METHOD_THREE,
                                        expectedInvokersForChildMethodThree,
                                        errorMessages);

        HashSet<Class<?>> expectedInvokersForGrandchildMethodThree = new HashSet<Class<?>>();
        expectedInvokersForGrandchildMethodThree.add(GrandchildBean.class);
        verifyInvokingClassesAreCorrect(timersToInvokingClasses,
                                        AutoCreatedTimerDriverBean.GRANDCHILD_BEAN_METHOD_THREE,
                                        expectedInvokersForGrandchildMethodThree,
                                        errorMessages);

        if (!errorMessages.isEmpty()) {
            // There are error messages, so something went wrong.
            // Log them, and then throw error.
            log("Encountered the following errors with timer inheritence processing:\n");
            for (String oneError : errorMessages) {
                log(oneError + "\n");
            }

            fail("Timer inheritence did not work properly.  See the log for the full list of errors.");
        }
    }

    private void verifyInvokingClassesAreCorrect(HashMap<String, HashSet<Class<?>>> timersToInvokingClasses,
                                                 String timerMethodAndClass,
                                                 HashSet<Class<?>> expectedInvokers,
                                                 ArrayList<String> errorMessages) {
        HashSet<Class<?>> invokingClasses = timersToInvokingClasses.get(timerMethodAndClass);
        if (invokingClasses == null) {
            // Timer method was never called back into...this is bad
            errorMessages.add("Timer method " + timerMethodAndClass + " was never called into.");
        } else {
            // Timer method was called into...now make sure the proper stuff called it.
            boolean hadAllExpectedInvokers = true;
            for (Class<?> oneExpectedInvoker : expectedInvokers) {
                if (!invokingClasses.contains(oneExpectedInvoker)) {
                    // Timer method was NOT invoked by a Timer instance associated
                    // with an expected class...this is bad
                    errorMessages.add("Timer method " + timerMethodAndClass + " was not invoked by a " +
                                      "Timer associated with expected " + oneExpectedInvoker);

                    hadAllExpectedInvokers = false;
                } else {
                    log("Timer method **" + timerMethodAndClass + "** was invoked by expected " + oneExpectedInvoker);
                }
            }

            if (hadAllExpectedInvokers) {
                // All the of the classes that were supposed to be associated with invoking
                // Timer instances were found, so this is good.  Now, check to make sure that
                // there aren't any extra invoking classes that were not expected. (This check
                // is not done when the there are missing expected invokers because the count
                // should already be off in that case, so one or more expected invokers are not
                // there.)
                if (invokingClasses.size() != expectedInvokers.size()) {
                    errorMessages.add("Timer method " + timerMethodAndClass + " had extra, unwanted invokers.");
                }
            }
        }
    }

}
