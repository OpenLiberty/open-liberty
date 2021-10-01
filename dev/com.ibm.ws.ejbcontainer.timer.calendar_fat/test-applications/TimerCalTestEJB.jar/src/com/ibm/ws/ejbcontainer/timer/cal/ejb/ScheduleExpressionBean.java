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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Stateless
@Local(ScheduleExpressionIntf.class)
public class ScheduleExpressionBean implements ScheduleExpressionIntf {

    public final static String CLASSNAME = ScheduleExpressionBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @Resource
    SessionContext ivSessionCtx;

    @Resource
    private TimerService ivTimerService;

    /**
     * Verify ScheduleExpression with bogus year values gets
     * IllegalArgumentException <br>
     * From 18.2.1 of the EJB 3.1 spec:
     * year : a particular calendar year
     * Allowable values : a four-digit calendar year
     */
    @Override
    public void testYear() {

        final String method = "testYear";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing invalid year in ScheduleExpression");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerServiceAPIforBogusYear");

        ScheduleExpression invalidYearScheduleExpression = new ScheduleExpression();
        invalidYearScheduleExpression.year(12009); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidYearScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidYearScheduleExpression.year(-1961); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidYearScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidYearScheduleExpression.year(0000); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidYearScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

    }

    /**
     * Verify ScheduleExpression with bogus month values gets
     * IllegalArgumentException <br>
     * From 18.2.1 of the EJB 3.1 spec:
     * month : one or more months within a year
     * Allowable values :
     * [1,12]
     * or
     * {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}
     **/
    @Override
    public void testMonth() {

        final String method = "testMonth";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing invalid month in ScheduleExpression");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerServiceAPIforBogusMonth");

        ScheduleExpression monthScheduleExpression = new ScheduleExpression();

        monthScheduleExpression.month(13); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(monthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        monthScheduleExpression.month("jul"); // (lower-case should be accepted)
        createCalTimerWithValidSchedExpr(monthScheduleExpression, tc);

        monthScheduleExpression.month("Duc"); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(monthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        monthScheduleExpression.month("Dec."); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(monthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        monthScheduleExpression.month("December"); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(monthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        // increment is not applicable to month (only hours, min, sec)
        monthScheduleExpression.month("2/3"); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(monthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

    }

    /**
     * Verify ScheduleExpression with bogus dayOfMonth values
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

        final String method = "testDayOfMonth";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing invalid dayOfMonth in ScheduleExpression");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerServiceAPIforBogusDayOfMonth");

        ScheduleExpression invalidDayOfMonthScheduleExpression = new ScheduleExpression();

        invalidDayOfMonthScheduleExpression.dayOfMonth(32); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidDayOfMonthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidDayOfMonthScheduleExpression.dayOfMonth("Day of Bogusity"); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidDayOfMonthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidDayOfMonthScheduleExpression.dayOfMonth(-8); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidDayOfMonthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidDayOfMonthScheduleExpression.dayOfMonth("0th,4th"); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidDayOfMonthScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidDayOfMonthScheduleExpression.dayOfMonth("1st,2nd,umpteenth,6th"); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidDayOfMonthScheduleExpression, tc);;
        } catch (IllegalArgumentException iae) {
            // pass
        }

        ScheduleExpression validDayOfMonthScheduleExpression = new ScheduleExpression();
        validDayOfMonthScheduleExpression.dayOfMonth("-7");
        createCalTimerWithValidSchedExpr(validDayOfMonthScheduleExpression, tc);

        // begin 587889
        svLogger.logp(Level.FINE, CLASSNAME, "testDayOfMonth", "trying -7 w/o quotes");
        validDayOfMonthScheduleExpression.dayOfMonth(-7);
        createCalTimerWithValidSchedExpr(validDayOfMonthScheduleExpression, tc);
        // end 587889

    }

    /**
     * Verify ScheduleExpression with bogus dayOfWeek values
     * From 18.2.1 of the EJB 3.1 spec:
     * dayOfWeek : one or more days within a week
     * Allowable values :
     * [0,7] or
     * {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}
     * "0" and "7" both refer to Sunday
     **/
    @Override
    public void testDayOfWeek() {

        final String method = "testDayOfWeek";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing invalid dayOfWeek in ScheduleExpression");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerServiceAPIforBogusDayOfWeek");

        ScheduleExpression invalidDayOfWeekScheduleExpression = new ScheduleExpression();

        invalidDayOfWeekScheduleExpression.dayOfWeek(8); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidDayOfWeekScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidDayOfWeekScheduleExpression.dayOfWeek(-1); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidDayOfWeekScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidDayOfWeekScheduleExpression.dayOfWeek("Tues"); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidDayOfWeekScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

    }

    /**
     * Verify ScheduleExpression with bogus hour values
     * From 18.2.1 of the EJB 3.1 spec:
     * hour : one or more hours within a day
     * Allowable values : [0,23]
     */
    @Override
    public void testHour() {

        final String method = "testHour";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing invalid hour in ScheduleExpression");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerServiceAPIforBogusHour");

        ScheduleExpression invalidHourScheduleExpression = new ScheduleExpression();

        invalidHourScheduleExpression.hour(-1); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidHourScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidHourScheduleExpression.hour(24); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidHourScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

    }

    /**
     * Verify ScheduleExpression with bogus minute values
     * From 18.2.1 of the EJB 3.1 spec:
     * minute : one or more minutes within an hour
     * Allowable values : [0,59]
     */
    @Override
    public void testMinute() {

        final String method = "testMinute";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing invalid minute in ScheduleExpression");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerServiceAPIforBogusHour");

        ScheduleExpression invalidMinuteScheduleExpression = new ScheduleExpression();

        invalidMinuteScheduleExpression.minute(-1); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidMinuteScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidMinuteScheduleExpression.minute(60); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidMinuteScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        // increment: every 60 sec., starting at 00:00:30 (60 is invalid - must be [0,59]
        invalidMinuteScheduleExpression.minute("30/60"); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidMinuteScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

    }

    /**
     * Verify ScheduleExpression with bogus second values
     * From 18.2.1 of the EJB 3.1 spec:
     * second : one or more seconds within a minute
     * Allowable values : [0,59]
     *
     */
    @Override
    public void testSecond() {

        final String method = "testSecond";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing invalid second in ScheduleExpression");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerServiceAPIforBogusHour");

        ScheduleExpression invalidSecondScheduleExpression = new ScheduleExpression();

        invalidSecondScheduleExpression.second(-1); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidSecondScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

        invalidSecondScheduleExpression.second(60); // 587889
        try { // F743-9442
            createCalTimerWithInvalidSchedExpr(invalidSecondScheduleExpression, tc);
        } catch (IllegalArgumentException iae) {
            // pass
        }

    }

    /**
     * Verify that timer.getSchedule() retrieves the same ScheduleExpression used to create the timer.
     * Verify that timer.getSchedule() throws NoSuchObjectLocalException for a canceled or expired timer.
     */
    @Override
    public void testGetScheduleForNpTimer() {

        final String method = "testGetScheduleForNpTimer";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing getSchedule for NP timer.");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerAPIforGetSchedule non-persistent");

        ScheduleExpression se = new ScheduleExpression();
        se.year(9993);
        Timer timer = ivTimerService.createCalendarTimer(se, tc);

        ScheduleExpression retrievedSE = null;
        try {
            retrievedSE = timer.getSchedule();
        } catch (Throwable t) {
            fail("Throwable from np timer.getSchedule(" + dumpSE(se) + ", TimerConfig=" + tc + ").  Throwable was: " + t);
        } finally {
            timer.cancel();
        }

        checkEquality(se, retrievedSE); // F743-9442

        // Invoke getSchedule on the now canceled timer
        // (NextTimeoutTest.testGetNextTimeoutWithActualExpiration() covers the case of invoking getSchedule on an expired timer)
        try {
            timer.getSchedule();
            fail("expected NoSuchObjectLocalException from timer.getSchedule on canceled timer " + timer);
        } catch (NoSuchObjectLocalException nsole) {
            // pass
        }

    }

    // begin F743-9442
    /**
     * Verify that timer.getSchedule() retrieves the same ScheduleExpression used to create the timer.
     * Verify that timer.getSchedule() throws NoSuchObjectLocalException for a canceled or expired timer.
     */
    @Override
    public void testGetScheduleForPersistentTimer() {

        final String method = "testGetScheduleForPersistentTimer";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing getSchedule for persistent timer.");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(true);
        tc.setInfo("checkTimerAPIforGetSchedule persistent");

        ScheduleExpression se = new ScheduleExpression();
        se.year(9994);
        Timer timer = ivTimerService.createCalendarTimer(se, tc);
        ScheduleExpression retrievedSE = null;

        try {
            retrievedSE = timer.getSchedule();
        } catch (Throwable t) {
            fail("Throwable from persistent timer.getSchedule(" + dumpSE(se) + ", TimerConfig=" + tc + ").  Throwable was: " + t);
        } finally {
            timer.cancel();
        }

        checkEquality(se, retrievedSE); // F743-9442

        // Invoke getSchedule on the now canceled timer
        try {
            timer.getSchedule();
            fail("expected NoSuchObjectLocalException from timer.getSchedule on canceled timer " + timer);
        } catch (NoSuchObjectLocalException nsole) {
            // pass
        }

    }
    // end F743-9442

    //////////////////////////////////////////////////////////////////////////////////////

    // begin F743-16271
    /**
     * Verify that a subclass of ScheduleExpression may be used
     *
     * Since javax.ejb.ScheduleExpression is not declared as final,
     * it is conceivable that users may extend it.
     *
     */
    @Override
    public void testScheduleExpressionSubclassing() {

        final String method = "testScheduleExpressionSubclassing";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "testing subclassed ScheduleExpression");
        }

        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkUseOfSubclassedScheduleExpression");

        ScheduleExpressionExtension mySE = new ScheduleExpressionExtension("Phanerozoic", "Mesozoic", "Jurassic", "Middle", "Bathonian"); // F743-16271
        assertTrue("ScheduleExpression subclass did not return expected value for ivPeriod.  Expected Jurassic and got " + mySE.getIvPeriod(),
                   mySE.getIvPeriod().equals("Jurassic")); // F743-16271
        mySE.hour(5);
        createCalTimerWithValidSchedExpr(mySE, tc);

    }

    // end F743-16271

    private void createCalTimerWithInvalidSchedExpr(ScheduleExpression se, TimerConfig tc) {

        Timer t = null;
        try {
            t = ivTimerService.createCalendarTimer(se, tc);
            fail("expected IllegalArgumentException from ts.createCalendarTimer(" + dumpSE(se) + ", TimerConfig=" + tc + ")");
        } catch (IllegalArgumentException ex) {
            // pass
        } finally {
            if (t != null)
                t.cancel();
        }

    }

    private void createCalTimerWithValidSchedExpr(ScheduleExpression se, TimerConfig tc) {

        Timer t = null;
        try {
            t = ivTimerService.createCalendarTimer(se, tc);
            assertTrue("Expected isCalendarTimer() to return true", t.isCalendarTimer());
        } catch (Throwable th) {
            fail("Caught Throwable from ts.createCalendarTimer(" + dumpSE(se) + ", TimerConfig=" + tc + ").  Throwable was: " + th);
        } finally {
            t.cancel();
        }

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

    private String dumpSE(ScheduleExpression se) {

        StringBuffer sb = new StringBuffer(256);

        // begin F743-16271
        if (se instanceof ScheduleExpressionExtension) {
            sb.append(nl + "Eon    = " + ((ScheduleExpressionExtension) se).getIvEon());
            sb.append(nl + "Era    = " + ((ScheduleExpressionExtension) se).getIvEra());
            sb.append(nl + "Period = " + ((ScheduleExpressionExtension) se).getIvPeriod());
            sb.append(nl + "Epoch  = " + ((ScheduleExpressionExtension) se).getIvEpoch());
            sb.append(nl + "Age    = " + ((ScheduleExpressionExtension) se).getIvAge());
        }
        // end F743-16271

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

    @Timeout
    public void myTimeout(Timer timer) {

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "myTimeout", timer);
        }

        String info = (String) timer.getInfo();
        svLogger.logp(Level.INFO, CLASSNAME, "myTimeout", info);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "myTimeout", timer);
        }

    }

    @Override
    public void clearAllTimers() {
        Collection<Timer> timers = ivTimerService.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

} // end ScheduleExpressionBean
