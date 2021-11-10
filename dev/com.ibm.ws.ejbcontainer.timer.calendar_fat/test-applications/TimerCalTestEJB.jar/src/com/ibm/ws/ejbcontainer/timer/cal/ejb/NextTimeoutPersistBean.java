/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.cal.ejb;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

/**
 * May want to refer to http://www.timeanddate.com/calendar/ for other years in the future
 */
@Stateless
@Local(NextTimeoutPersistIntf.class)
public class NextTimeoutPersistBean implements NextTimeoutPersistIntf {

    public final static String CLASSNAME = NextTimeoutBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static Timer svTimer = null;
    public static String svInfo = null;
    public static Date svNextTimeout;
    public static String svNextTimeoutFailure;
    public static CountDownLatch svTimerLatch = new CountDownLatch(1);

    // The time for the container to run post invoke processing for @Timeout.
    // Should be used after a Timer has triggered a CountDownLatch to insure
    // the @Timeout method, including the transaction, has completed and thus
    // updated (or even removed) the timer.
    private static final long POST_INVOKE_DELAY = 700;

    @Resource
    SessionContext ivSessionCtx;

    @Resource
    private TimerService ivTimerService;

    /**
     * Verify first timeout calculations using year expressions
     * From 18.2.1 of the EJB 3.1 spec:
     * year : a particular calendar year
     * Allowable values :
     * a four-digit calendar year
     */
    @Override
    public void testYear() {

        ScheduleExpression se = new ScheduleExpression();

        se.year(9996);
        Timer t = createCalTimerWithSE(se, "verify first timeout with year");
        verifyFirstTimeout(t, "9996-01-01");

        se.year("9996,9997,9998"); // List
        t = createCalTimerWithSE(se, "verify first timeout with list of years");
        verifyFirstTimeout(t, "9996-01-01");

        se.year("9996-9998"); // Range
        t = createCalTimerWithSE(se, "verify first timeout with range of years");
        verifyFirstTimeout(t, "9996-01-01");

    }

    /**
     * Verify first timeout calculations using month expressions
     * From 18.2.1 of the EJB 3.1 spec:
     * month : one or more months within a year
     * Allowable values :
     * [1,12] or
     * {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}
     **/
    @Override
    public void testMonth() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);

        se.month(12); // last valid month
        Timer t = createCalTimerWithSE(se, "verify first timeout with month");
        verifyFirstTimeout(t, "9996-12-01");

        se.month("*"); // Wildcard
        t = createCalTimerWithSE(se, "verify first timeout with wildcard month");
        verifyFirstTimeout(t, "9996-01-01");

        se.month("Aug, sep,Oct"); // List
        t = createCalTimerWithSE(se, "verify first timeout with list of months");
        verifyFirstTimeout(t, "9996-08-01");

        se.month("Jul-Sep"); // Range
        t = createCalTimerWithSE(se, "verify first timeout with range of months");
        verifyFirstTimeout(t, "9996-07-01");

    }

    /**
     * Verify first timeout calculations using dayOfMonth expressions
     * From 18.2.1 of the EJB 3.1 spec:
     * dayOfMonth : one or more days within a month
     * Allowable values :
     * [1,31] or
     * [-7, -1] or
     * "Last" or
     * {"1st", "2nd", "3rd", "4th", "5th", "Last"} {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}
     * "Last" means the last day of the month
     * -x (where x is in the range [-7, -1]) means x day(s) before the last day of the month
     * "1st","2nd", etc. applied to a day of the week identifies a single occurrence of that day within the month.
     **/
    @Override
    public void testDayOfMonth() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);
        // with no month specified, Jan is implied

        se.dayOfMonth(31);
        Timer t = createCalTimerWithSE(se, "verify first timeout with dayOfMonth");
        verifyFirstTimeout(t, "9996-01-31");

        se.dayOfMonth(-7);
        t = createCalTimerWithSE(se, "verify first timeout with negative DayOfMonth");
        verifyFirstTimeout(t, "9996-01-24");

        se.dayOfMonth("-7");
        t = createCalTimerWithSE(se, "verify first timeout with negative DayOfMonth");
        verifyFirstTimeout(t, "9996-01-24");

        se.dayOfMonth("*"); // Wildcard
        t = createCalTimerWithSE(se, "verify first timeout with wildcard DayOfMonth");
        verifyFirstTimeout(t, "9996-01-01");

        se.dayOfMonth("2nd Mon"); // 2nd Monday
        t = createCalTimerWithSE(se, "verify first timeout with 2nd Mon dayOfMonth");
        verifyFirstTimeout(t, "9996-01-08");

        se.dayOfMonth("Last"); // Last
        t = createCalTimerWithSE(se, "verify first timeout with Last dayOfMonth");
        verifyFirstTimeout(t, "9996-01-31");

        se.dayOfMonth("4,2,3"); // List
        t = createCalTimerWithSE(se, "verify first timeout with unordered list of DayOfMonth");
        verifyFirstTimeout(t, "9996-01-02");

        se.dayOfMonth("2nd mon"); // Compound spec
        t = createCalTimerWithSE(se, "verify first timeout with compound DayOfMonth");
        verifyFirstTimeout(t, "9996-01-08");

        se.dayOfMonth("4-9"); // Range
        t = createCalTimerWithSE(se, "verify first timeout with range of DayOfMonth");
        verifyFirstTimeout(t, "9996-01-04");

        se.dayOfMonth("Last"); // Last - in a simple leap year
        se.month("Feb");
        se.year(9604);
        t = createCalTimerWithSE(se, "verify first timeout with 'Last' DayOfMonth in a leap year");
        verifyFirstTimeout(t, "9604-02-29");

        se.dayOfMonth("Last"); // Last - in a (NON-leap) year divisible by 100
        se.month("Feb");
        se.year(9500);
        t = createCalTimerWithSE(se, "verify first timeout with 'Last' DayOfMonth in a (non-leap) year div by 100");
        verifyFirstTimeout(t, "9500-02-28");

        se.dayOfMonth("Last"); // Last - in a leap year divisible by 400
        se.month("Feb");
        se.year(9600);
        t = createCalTimerWithSE(se, "verify first timeout with 'Last' DayOfMonth in a (NON-leap) year div by 400");
        verifyFirstTimeout(t, "9600-02-29");

    }

    /**
     * Verify first timeout using dayOfWeek expressions
     * From 18.2.1 of the EJB 3.1 spec:
     * dayOfWeek : one or more days within a week
     * Allowable values :
     * [0,7] or
     * {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}
     * "0" and "7" both refer to Sunday
     **/
    @Override
    public void testDayOfWeek() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);
        se.month("May");

        se.dayOfWeek("0");
        Timer t = createCalTimerWithSE(se, "verify first timeout with dayOfWeek");
        verifyFirstTimeout(t, "9996-05-05"); // First Sunday in May 9996 is the 5th

        se.dayOfWeek(0);
        t = createCalTimerWithSE(se, "verify first timeout with dayOfWeek");
        verifyFirstTimeout(t, "9996-05-05"); // First Sunday in May 9996 is the 5th

        se.dayOfWeek("sun"); // note lower-case, which is valid
        t = createCalTimerWithSE(se, "verify first timeout with dayOfWeek");
        verifyFirstTimeout(t, "9996-05-05"); // First Sunday in May 9996 is the 5th

        se.dayOfWeek("7");
        t = createCalTimerWithSE(se, "verify first timeout with dayOfWeek");
        verifyFirstTimeout(t, "9996-05-05"); // First Sunday in May 9996 is the 5th

        se.dayOfWeek("Sat");
        t = createCalTimerWithSE(se, "verify first timeout with dayOfWeek");
        verifyFirstTimeout(t, "9996-05-04"); // First Saturday in May 9996 is the 4th

        se.dayOfWeek("*"); // Wildcard
        t = createCalTimerWithSE(se, "verify first timeout with wildcard dayOfWeek");
        verifyFirstTimeout(t, "9996-05-01");

        se.dayOfWeek("Tue,Thu, Sat"); // List (match any of these being first)
        t = createCalTimerWithSE(se, "verify first timeout with list of dayOfWeek");
        verifyFirstTimeout(t, "9996-05-02");

        se.dayOfWeek("Wed-Sat"); // Range
        t = createCalTimerWithSE(se, "verify first timeout with range of dayOfWeek");
        verifyFirstTimeout(t, "9996-05-01");

    }

    /**
     * Verify first timeout using hour expressions
     * From 18.2.1 of the EJB 3.1 spec:
     * hour : one or more hours within a day
     * Allowable values : [0,23]
     */
    @Override
    public void testHour() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);

        se.hour(16);
        Timer t = createCalTimerWithSE(se, "verify first timeout with hour");
        verifyFirstTimeout(t, "9996-01-01:16:00:00");

        se.hour(23);
        t = createCalTimerWithSE(se, "verify first timeout with hour");
        verifyFirstTimeout(t, "9996-01-01:23:00:00");

        se.hour("*"); // Wildcard
        t = createCalTimerWithSE(se, "verify first timeout with wildcard hour");
        verifyFirstTimeout(t, "9996-01-01:00:00:00");

        se.hour(" 4,6,9 "); // List
        t = createCalTimerWithSE(se, "verify first timeout with list of hour");
        verifyFirstTimeout(t, "9996-01-01:04:00:00");

        se.hour("13-23"); // Range
        t = createCalTimerWithSE(se, "verify first timeout with range of hour");
        verifyFirstTimeout(t, "9996-01-01:13:00:00");

        se.hour("2/3"); // increment: every 3rd hour starting with 2:00
        t = createCalTimerWithSE(se, "verify first timeout with increment hours");
        verifyFirstTimeout(t, "9996-01-01:02:00:00");

        se.hour("0");
        t = createCalTimerWithSE(se, "verify first timeout with increment hours");
        verifyFirstTimeout(t, "9996-01-01:00:00:00");

        se.hour(0);
        t = createCalTimerWithSE(se, "verify first timeout with increment hours");
        verifyFirstTimeout(t, "9996-01-01:00:00:00");
    }

    /**
     * Verify first timeout using minute expressions
     * From 18.2.1 of the EJB 3.1 spec:
     * minute : one or more minutes within an hour
     * Allowable values : [0,59]
     */
    @Override
    public void testMinute() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);

        se.minute("55");
        Timer t = createCalTimerWithSE(se, "verify first timeout with minute");
        verifyFirstTimeout(t, "9996-01-01:00:55:00");

        se.minute(55);
        t = createCalTimerWithSE(se, "verify first timeout with minute");
        verifyFirstTimeout(t, "9996-01-01:00:55:00");

        se.minute(0);
        t = createCalTimerWithSE(se, "verify first timeout with minute");
        verifyFirstTimeout(t, "9996-01-01:00:00:00");

        se.minute("*"); // Wildcard
        t = createCalTimerWithSE(se, "verify first timeout with wildcard minute");
        verifyFirstTimeout(t, "9996-01-01:00:00:00");

        se.minute(" 7,  11,   13,     17       "); // List
        t = createCalTimerWithSE(se, "verify first timeout with list of minute");
        verifyFirstTimeout(t, "9996-01-01:00:07:00");

        se.minute("15-30"); // Range
        t = createCalTimerWithSE(se, "verify first timeout with range of minute");
        verifyFirstTimeout(t, "9996-01-01:00:15:00");

        se.minute("15/30"); // increment: every 30 min, starting at 15 min. past the hour
        t = createCalTimerWithSE(se, "verify first timeout with increment minutes");
        verifyFirstTimeout(t, "9996-01-01:00:15:00");

    }

    /**
     * Verify first timeout using second expressions
     * From 18.2.1 of the EJB 3.1 spec:
     * second : one or more seconds within a minute
     * Allowable values : [0,59]
     *
     */
    @Override
    public void testSecond() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);

        se.second("55");
        Timer t = createCalTimerWithSE(se, "verify first timeout with second");
        verifyFirstTimeout(t, "9996-01-01:00:00:55");

        se.second(55);
        t = createCalTimerWithSE(se, "verify first timeout with second");
        verifyFirstTimeout(t, "9996-01-01:00:00:55");

        se.second(0);
        t = createCalTimerWithSE(se, "verify first timeout with second");
        verifyFirstTimeout(t, "9996-01-01:00:00:00");

        se.second("*"); // Wildcard
        t = createCalTimerWithSE(se, "verify first timeout with wildcard second");
        verifyFirstTimeout(t, "9996-01-01:00:00:00");

        se.second("58,54,50"); // List (note not in sequential order)
        t = createCalTimerWithSE(se, "verify first timeout with list of second");
        verifyFirstTimeout(t, "9996-01-01:00:00:50");

        se.second("51-59"); // Range
        t = createCalTimerWithSE(se, "verify first timeout with range of second");
        verifyFirstTimeout(t, "9996-01-01:00:00:51");

        se.second("10/20"); // Increment: every 20 sec. starting at 00:00:10
        t = createCalTimerWithSE(se, "verify first timeout with range of second");
        verifyFirstTimeout(t, "9996-01-01:00:00:10");
    }

    /**
     * Since the local system running the FAT may be in any timezone, we must calculate the expected
     * expiration time as a function of both the timer's timezone and the FAT host's timezone
     *
     * @param timezoneID String timezone ID. For example, see http://en.wikipedia.org/wiki/List_of_zoneinfo_timezones
     *
     */
    @Override
    public void testTimezone(String timezoneID) {

        // Get the TimeZone in which this FAT host is running
        TimeZone localTimezone = SimpleTimeZone.getDefault();

        // TimeZone in which the timer will be running
        TimeZone timerTimezone = TimeZone.getTimeZone(timezoneID);

        long expirationLong = stringToMillis("9996-09-15");

        int localUToffset = localTimezone.getOffset(expirationLong);
        int timerUToffset = timerTimezone.getOffset(expirationLong);
        int UTdelta = localUToffset - timerUToffset;
        Date predictedDate = new Date(expirationLong + UTdelta);

        ScheduleExpression se = new ScheduleExpression();
        se.timezone(timezoneID);
        se.year(9996);
        se.month(9);
        se.dayOfMonth(15);

        String info = "Timer in timezone " + timerTimezone.getDisplayName();
        Timer t = createCalTimerWithSE(se, info);

        if (svLogger.isLoggable(Level.FINE)) {
            svLogger.logp(Level.FINE, CLASSNAME, "testTimezone", "se = " + dumpSE(se));
            svLogger.logp(Level.FINE, CLASSNAME, "testTimezone", "localTimezone = " + localTimezone.getDisplayName() + " == " + formattedUTCoffset(localTimezone));
            svLogger.logp(Level.FINE, CLASSNAME, "testTimezone", "timerTimezone = " + timerTimezone.getDisplayName() + " == " + formattedUTCoffset(timerTimezone));
            svLogger.logp(Level.FINE, CLASSNAME, "testTimezone", "localUToffset = " + localUToffset / 3600000);
            svLogger.logp(Level.FINE, CLASSNAME, "testTimezone", "timerUToffset = " + timerUToffset / 3600000);
            svLogger.logp(Level.FINE, CLASSNAME, "testTimezone", "expiration    = " + expirationLong / 3600000);
            svLogger.logp(Level.FINE, CLASSNAME, "testTimezone", "UTdelta       = " + UTdelta / 3600000);
        }

        Date nextTimeoutDate = t.getNextTimeout();

        String msg = "Expected nextTimeout to be " + predictedDate + ".  Instead, received " + nextTimeoutDate;
        assertTrue(msg, predictedDate.equals(nextTimeoutDate));

        t.cancel();

    }

    @Override
    public void testStart() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);
        se.dayOfMonth(5); // 5th of each month
        se.start(new Date(stringToMillis("9996-02-01"))); // but don't start until 2nd month
        Timer t = createCalTimerWithSE(se, "verify first timeout with start");
        verifyFirstTimeout(t, "9996-02-05");

    }

    @Override
    public void testStartLTend() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);
        se.month(12);
        se.dayOfMonth("Last");
        se.start(new Date(stringToMillis("9996-04-15")));
        se.end(new Date(stringToMillis("9997-05-16")));

        String info = "Timer with start < end";
        Timer t = createCalTimerWithSE(se, info);
        Date startDate = t.getSchedule().getStart();
        Date endDate = t.getSchedule().getEnd();

        try {
            t.getNextTimeout();
        } catch (Throwable th) {
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", erroneously got Throwable: " + th);
        }

        t.cancel();

    }

    @Override
    public void testStartGEend() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);
        se.month(11); // Note failure if month(12)
        se.dayOfMonth("Last");
        se.start(new Date(stringToMillis("9996-04-15")));
        se.end(new Date(0));

        String info = "Timer with start > end";
        Timer t1 = createCalTimerWithSE(se, info);

        Date startDate = null;
        Date endDate = null;

        try { // F743-9442
            startDate = t1.getSchedule().getStart();
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", expected NoSuchObjectLocalException");
            endDate = t1.getSchedule().getEnd();
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nsole) {
            // pass
        }

        try {
            t1.getNextTimeout();
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nsole) { // 595255
            // pass
            // Now verify that cancel() throws NSOLE too
            try {
                t1.cancel();
            } catch (NoSuchObjectLocalException nsole1) {
                // pass
            }
        } catch (Throwable th) {
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", expected NoSuchObjectLocalException, but got Throwable: " + th);
        } finally {
            try {
                t1.cancel();
            } catch (NoSuchObjectLocalException nsole2) {
                ; // ignore
            }
        }

        se = new ScheduleExpression();
        se.year(9996);
        se.month(11);
        se.dayOfMonth("Last");
        Date startAndEndDate = new Date(stringToMillis("9996-04-15"));
        se.start(startAndEndDate);
        se.end(startAndEndDate);
        info = "Timer with start == end";
        Timer t2 = createCalTimerWithSE(se, info);

        try { // F743-9442
            startDate = t2.getSchedule().getStart();
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", expected NoSuchObjectLocalException");
            endDate = t2.getSchedule().getEnd();
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nsole) {
            // pass
        }

        try {
            t2.getNextTimeout();
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nsole) { // 595255
            // pass
            // Now verify that cancel() throws NSOLE too
            try {
                t2.cancel();
            } catch (NoSuchObjectLocalException nsole1) {
                // pass
            }
        } catch (Throwable th) {
            fail("With se.start==" + startDate + " and se.end==" + endDate + ", expected NoSuchObjectLocalException, but got Throwable: " + th);
        } finally {
            try {
                t2.cancel();
            } catch (NoSuchObjectLocalException nsole2) {
                ; // ignore
            }
        }
    }

    // Used for most variations, to create a persistent timer.
    private Timer createCalTimerWithSE(ScheduleExpression se, Serializable info) {
        Timer timer = null;
        TimerConfig tc = new TimerConfig();
        svTimerLatch = new CountDownLatch(1);

        try {
            tc.setPersistent(true);
            tc.setInfo(info);
            timer = ivTimerService.createCalendarTimer(se, tc);
        } catch (Throwable t) {
            svLogger.logp(Level.WARNING, CLASSNAME, "createCalTimerWithSE", "Failing in createCalTimerWithSE.  info = " + tc.getInfo());
            fail("Throwable from ts.createCalendarTimer(" + dumpSE(se) + ", TimerConfig=" + tc + ").  Throwable was: " + t);
        }
        svTimer = timer;
        return timer;
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public Date createCalTimer(ScheduleExpression se, Serializable info) {
        Timer timer = createCalTimerWithSE(se, info);
        return timer.getNextTimeout();
    }

    @Override
    public void waitForTimer(long maxWaitTime) {
        try {
            svTimerLatch.await(maxWaitTime, TimeUnit.MILLISECONDS);
            svTimerLatch = new CountDownLatch(1); // Reset latch to wait for next timeout
            FATHelper.sleep(POST_INVOKE_DELAY); // wait for timer method postInvoke to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Date getNextTimeoutFromExpiration() {
        return svNextTimeout;
    }

    @Override
    public String getNextTimeoutFailureFromExpiration() {
        return svNextTimeoutFailure;
    }

    @Override
    public String getScheduleString() {
        try {
            return svTimer.getSchedule().toString();
        } catch (NoMoreTimeoutsException nmte) {
            return "NoMoreTimeoutsException";
        } catch (NoSuchObjectLocalException nsole) {
            return "NoSuchObjectLocalException";
        }
    }

    @Override
    public void testGetSchedule() {

        ScheduleExpression se = new ScheduleExpression();
        se.year(9996);
        se.month("Oct");
        se.dayOfMonth(10);
        String info = "Timer to compare with timer created w/same SE";
        Timer t = createCalTimerWithSE(se, info);
        compareSchedules(t);
        t.cancel();

    }

    private String dumpSE(ScheduleExpression se) {

        StringBuffer sb = new StringBuffer(256);

        sb.append(nl + "Year:       " + se.getYear());
        sb.append(nl + "Month:      " + se.getMonth());
        sb.append(nl + "DayOfMonth: " + se.getDayOfMonth());
        sb.append(nl + "DayOfWeek:  " + se.getDayOfWeek());
        sb.append(nl + "Hour:       " + se.getHour());
        sb.append(nl + "Minute:     " + se.getMinute());
        sb.append(nl + "Second:     " + se.getSecond());
        sb.append(nl + "Timezone    " + se.getTimezone());
        sb.append(nl + "Start:      " + se.getStart());
        sb.append(nl + "End:        " + se.getEnd());

        return sb.toString();

    }

    /**
     * Verify that a timer created from the input timer's SE
     * has the same attributes, e.g., nextTimeout()
     *
     * @param t timer
     *
     */
    private void compareSchedules(Timer t) {

        ScheduleExpression se = t.getSchedule();
        String info = (String) t.getInfo();
        Date nextTimeout = t.getNextTimeout();
        Timer t1 = createCalTimerWithSE(se, info);
        long timeRemaining = t.getTimeRemaining();
        long t1TimeRemaining = t1.getTimeRemaining(); // 591377

        assertTrue("Expected same info", info.equals(t1.getInfo()));
        checkEquality(se, t1.getSchedule()); // F743-9442
        assertTrue("Expected same nextTimeout", nextTimeout.equals(t1.getNextTimeout()));

        // getTimeRemaining() may not return the exact same no. of ms for the two timers, due to
        // time elapsed between invoking the methods on each timer.  Therefore allow a generous
        // 5 ms difference or less for this purpose of ensuring that the created timer has approximately
        // the same time remaining as that from whose ScheduleExpression it was created.
        assertTrue("Expected same timeRemaining", t1TimeRemaining - timeRemaining < 5); // 591377

    }

    private void verifyFirstTimeout(Timer t, String expected) {

        Date nextTimeout = null;
        try {
            nextTimeout = t.getNextTimeout();
        } catch (Throwable throwable) {
            fail("Caught Throwable from t.getNextTimeout()");
        }
        Date expectedNextTimeout = new Date(stringToMillis(expected));
        boolean same = (nextTimeout.compareTo(expectedNextTimeout) == 0);
        assertTrue("Expected first timeout " + expected + " but getNextTimeout() returned " + nextTimeout, same);
        t.cancel();

    }

    private static long stringToMillis(String dateTime) {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);

        java.util.StringTokenizer st = new java.util.StringTokenizer(dateTime, "- :");
        cal.set(Calendar.YEAR, Integer.parseInt(st.nextToken()));
        cal.set(Calendar.MONTH, Integer.parseInt(st.nextToken()) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(st.nextToken()));
        if (st.hasMoreTokens()) {
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(st.nextToken()));
            cal.set(Calendar.MINUTE, Integer.parseInt(st.nextToken()));
            cal.set(Calendar.SECOND, Integer.parseInt(st.nextToken()));

            if (st.hasMoreTokens()) {
                cal.setTimeZone(TimeZone.getTimeZone(st.nextToken()));
            }
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        }

        return cal.getTimeInMillis();
    }

    private String formattedUTCoffset(TimeZone tz) {

        String sign;
        int offset = tz.getOffset(System.currentTimeMillis());
        int hours = Math.abs(offset / 3600000);
        int minutes;

        if (offset < 0) {
            sign = "-";
            minutes = Math.abs((offset + hours * 3600000) / 60000);
        } else {
            sign = "+";
            minutes = Math.abs((offset - hours * 3600000) / 60000);
        }

        String retval = sign + hours + ":" + minutes;
        return retval;

    }

    /**
     * Not expected to go off except for the few tests which actually
     * allow a timer to expire. (Most tests schedule for a year in the distant future)
     *
     * @param timer
     */
    @Timeout
    public void myTimeout(Timer timer) {

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "myTimeout", timer);
        }

        svInfo = (String) timer.getInfo();
        svNextTimeout = null;
        svNextTimeoutFailure = null;
        svLogger.logp(Level.INFO, CLASSNAME, "myTimeout", svInfo);

        try {
            svNextTimeout = timer.getNextTimeout();
            if (svLogger.isLoggable(Level.FINE)) {
                svLogger.logp(Level.FINE, CLASSNAME, "myTimeout", "svNextTimeout = " + df.format(svNextTimeout));
            }
        } catch (NoMoreTimeoutsException nmtoe) {
            svNextTimeoutFailure = "NoMoreTimeoutsException";
        } catch (NoSuchObjectLocalException nsole) {
            svNextTimeoutFailure = "NoMoreTimeoutsException";
        } catch (Throwable th) {
            String msg = "Caught unexpected exception from timer.getNextTimeout()" + th.toString();
            svLogger.logp(Level.INFO, CLASSNAME, "myTimeout", msg, th);
            svNextTimeoutFailure = th.getClass().getSimpleName();
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "myTimeout", timer);
        }

        svTimerLatch.countDown();
    }

    private void checkEquality(ScheduleExpression se1, ScheduleExpression se2) {

        assertEquals("ScheduleExpression returned from getSchedule() does not match Month of ScheduleExpression used when timer was created", se1.getMonth(), se2.getMonth());
        assertEquals("ScheduleExpression returned from getSchedule() does not match DayOfMonth of ScheduleExpression used when timer was created", se1.getDayOfMonth(),
                     se2.getDayOfMonth());
        assertEquals("ScheduleExpression returned from getSchedule() does not match DayOfWeek of ScheduleExpression used when timer was created", se1.getDayOfWeek(),
                     se2.getDayOfWeek());
        assertEquals("ScheduleExpression returned from getSchedule() does not match DayOfMonth of ScheduleExpression used when timer was created", se1.getEnd(), se2.getEnd());
        assertEquals("ScheduleExpression returned from getSchedule() does not match End of ScheduleExpression used when timer was created", se1.getHour(), se2.getHour());
        assertEquals("ScheduleExpression returned from getSchedule() does not match Minute of ScheduleExpression used when timer was created", se1.getMinute(), se2.getMinute());
        assertEquals("ScheduleExpression returned from getSchedule() does not match Second of ScheduleExpression used when timer was created", se1.getSecond(), se2.getSecond());
        assertEquals("ScheduleExpression returned from getSchedule() does not match Start of ScheduleExpression used when timer was created", se1.getStart(), se2.getStart());
        assertEquals("ScheduleExpression returned from getSchedule() does not match Timezone of ScheduleExpression used when timer was created", se1.getTimezone(),
                     se2.getTimezone());
        assertEquals("ScheduleExpression returned from getSchedule() does not match Year of ScheduleExpression used when timer was created", se1.getYear(), se2.getYear());
    }

    @Override
    public void clearAllTimers() {
        Collection<Timer> timers = ivTimerService.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

} // end ScheduleExpressionBean
