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
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.SessionContext;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

public abstract class AbstractAnnotationTxBean implements AnnotationTxLocal {

    private static final String CLASS_NAME = AbstractAnnotationTxBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    // The time for the container to run post invoke processing for @Timeout.
    // Should be used after a Timer has triggered a CountDownLatch to insure
    // the @Timeout method, including the transaction, has completed and thus
    // updated (or even removed) the timer.
    private static final long POST_INVOKE_DELAY = 700;

    public static final List<String> svExpiredTimerInfos = Collections.synchronizedList(new ArrayList<String>());

    private static volatile CountDownLatch svTimerLatch = new CountDownLatch(1);
    private static Timer svHeldTimer = null;

    @Resource
    SessionContext ivSessionCtx;

    /**
     * Note: Many tests rely on this method only logging one message at INFO
     * or higher. Please do not modify this method to log more non-debug
     * messages.
     *
     * @param timer
     */
    protected void myTimeout(Timer timer) {
        String info = (String) timer.getInfo();
        svLogger.info("myTimeout : " + info);
        svExpiredTimerInfos.add(info);
        if (INTERVAL_INFO.equals(timer.getInfo()) && svTimerLatch.getCount() == 1) {
            timer.cancel();
        }
        svTimerLatch.countDown();
    }

    @Override
    public void executeTest(TstName tstName) {
        execute(tstName);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void executeTestNoTx(TstName tstName) {
        execute(tstName);
    }

    @Override
    public void waitForTimer(long maxWaitTime) {
        try {
            svTimerLatch.await(maxWaitTime, TimeUnit.MILLISECONDS);
            if (svTimerLatch.getCount() == 0) {
                svTimerLatch = new CountDownLatch(1); // Reset latch to wait for next timeout
                FATHelper.sleep(POST_INVOKE_DELAY); // wait for timer method postInvoke to complete
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearAllTimers() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "clearAllTimers");
        }

        svHeldTimer = null;

        Collection<Timer> timers = ivSessionCtx.getTimerService().getTimers();
        for (Timer t : timers) {
            svLogger.logp(Level.FINEST, CLASS_NAME, "clearAllTimers", "canceling timer: {0}", t);
            t.cancel();
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "clearAllTimers");
        }
    }

    @Override
    public Collection<String> getInfoOfAllTimers() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "getInfoOfAllTimers");
        }

        Collection<String> infos = new ArrayList<String>();
        Collection<Timer> timers = ivSessionCtx.getTimerService().getTimers();
        for (Timer t : timers) {
            infos.add((String) t.getInfo());
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "getInfoOfAllTimers", infos);
        }

        return infos;
    }

    private void execute(TstName tstName) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "execute", tstName);
        }

        svLogger.logp(Level.INFO, CLASS_NAME, "execute", "Test: " + tstName);

        /////////////////////////////////////////////////////////////////////////
        if (tstName == TstName.TEST_CREATE_AND_CANCEL_IN_SAME_METHOD ||
            tstName == TstName.TEST_CREATE_AND_CANCEL_IN_SAME_METHOD_ROLLBACK) {
            /////////////////////////////////////////////////////////////////////////
            assertEquals(0, ivSessionCtx.getTimerService().getTimers().size());
            createAndCancelInSameMethod();
            //verify that there are still no timers in the timer service
            assertEquals(0, ivSessionCtx.getTimerService().getTimers().size());

            if (tstName == TstName.TEST_CREATE_AND_CANCEL_IN_SAME_METHOD_ROLLBACK) {
                ivSessionCtx.setRollbackOnly();
            }

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_CREATE_TWO_TIMERS_IN_SAME_METHOD ||
                   tstName == TstName.TEST_CREATE_TWO_TIMERS_IN_SAME_METHOD_ROLLBACK) {
            /////////////////////////////////////////////////////////////////////////
            assertEquals(0, ivSessionCtx.getTimerService().getTimers().size());
            createTwoTimersInSameMethod();
            assertEquals(2, ivSessionCtx.getTimerService().getTimers().size());

            if (tstName == TstName.TEST_CREATE_TWO_TIMERS_IN_SAME_METHOD_ROLLBACK) {
                ivSessionCtx.setRollbackOnly();
            }

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_CREATE_TIMER) {
            /////////////////////////////////////////////////////////////////////////
            createTimer(DEFAULT_INFO);

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_CANCEL_TIMER) {
            cancelTimer(DEFAULT_INFO);

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_EXPIRATION_IN_PAST ||
                   tstName == TstName.TEST_NEGATIVE_DURATION ||
                   tstName == TstName.TEST_INVALID_EXPIRATION) {
            /////////////////////////////////////////////////////////////////////////
            createTimerInThePast(tstName);

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_OVERFLOW_DURATION) {
            /////////////////////////////////////////////////////////////////////////
            createOverflowDurationTimer();

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_OVERFLOW_EXPIRATION) {
            /////////////////////////////////////////////////////////////////////////
            createOverflowExpirationTimer();

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_OVERFLOW_INTERVAL) {
            /////////////////////////////////////////////////////////////////////////
            createOverflowIntervalTimer();

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_GET_HANDLE) {
            /////////////////////////////////////////////////////////////////////////
            createTimerGetHandle();

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_CREATE_EXPIRED_TIMER) {
            /////////////////////////////////////////////////////////////////////////
            createAndHoldTimer(EXPIRED_INFO);

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_CANCEL_EXPIRED_TIMER) {
            /////////////////////////////////////////////////////////////////////////
            try {
                cancelHeldTimer(EXPIRED_INFO);
                fail("Expected a NoSuchObjectLocalException");
            } catch (NoSuchObjectLocalException ex) {
                //pass
            }

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_TIMER_API) {
            /////////////////////////////////////////////////////////////////////////
            checkTimerAPI();

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_INTERVAL_TIMER_DURATION) {
            /////////////////////////////////////////////////////////////////////////
            createIntervalTimerViaDuration();

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_INTERVAL_TIMER_DATE) {
            /////////////////////////////////////////////////////////////////////////
            createIntervalTimerViaDate();

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_TIMER_SERVICE_API) {
            /////////////////////////////////////////////////////////////////////////
            checkTimerServiceAPI();

            /////////////////////////////////////////////////////////////////////////
        } else if (tstName == TstName.TEST_CREATE_CALENDAR_AND_FIND_IN_SAME_METHOD) {
            /////////////////////////////////////////////////////////////////////////
            createCalendarAndFindInSameMethod();

        } else {
            fail("Unknown or unimplemented test: " + tstName);
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "execute");
        }
    }

    private void createTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "createTimer", info);
        }

        Timer t = TimerHelper.createTimer(ivSessionCtx.getTimerService(),
                                          DURATION, null, info, false, null);
        assertNotNull("Timer is null.", t);
        svLogger.logp(Level.INFO, CLASS_NAME, "createTimer", "Created timer: " + t);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "createTimer");
        }
    }

    private void cancelTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "cancelTimer", info);
        }

        Collection<Timer> timers = ivSessionCtx.getTimerService().getTimers();
        assertEquals("Expected only one timer from the timer service.", 1, timers.size());
        for (Timer t : timers) {
            assertEquals("The timer returned from the timer service is not the one we expected.",
                         info, t.getInfo());
            t.cancel();
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "cancelTimer");
        }
    }

    private void createAndHoldTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "createAndHoldTimer", info);
        }

        if (svHeldTimer != null) {
            String msg = "Invalid test case! svHeldTimer should be null before invoking createAndHoldTimer()";
            throw new IllegalStateException(msg);
        }

        svTimerLatch = new CountDownLatch(1); // wait for this timer to expire 1x

        Timer t = TimerHelper.createTimer(ivSessionCtx.getTimerService(),
                                          DURATION, null, info, false, null);
        assertNotNull("Timer is null.", t);
        svLogger.logp(Level.INFO, CLASS_NAME, "createAndHoldTimer", "Created timer: " + t);

        svHeldTimer = t;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "createAndHoldTimer");
        }
    }

    private void cancelHeldTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "cancelHeldTimer", info);
        }

        if (svHeldTimer == null) {
            String msg = "Invalid test case! svHeldTimer should never be null before invoking cancelHeldTimer()";
            throw new IllegalStateException(msg);
        }

        Timer t = svHeldTimer;
        svHeldTimer = null;
        t.cancel();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "cancelHeldTimer");
        }
    }

    private void createAndCancelInSameMethod() {
        final String info = "createAndCancelInSameMethod";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, info);
        }

        // create a timer
        Timer t = TimerHelper.createTimer(ivSessionCtx.getTimerService(),
                                          DURATION, null, info, false, null);
        svLogger.logp(Level.INFO, CLASS_NAME, info, "Created timer: " + t);
        // before the tran completes and before the timer expires, cancel it
        t.cancel();
        svLogger.logp(Level.INFO, CLASS_NAME, info, "Canceled timer");

        // verify that the timer is correctly canceled by
        // (1) looking it up in the timer server and verifying that it does not exist
        Timer t2 = TimerHelper.getTimerWithMatchingInfo(
                                                        ivSessionCtx.getTimerService(), info);
        assertNull("Found timer (" + t2 + ") in timer service after it was canceled.", t2);

        // and (2) getting a NoSuchObjectLocalException when invoking a method on it
        try {
            t.getInfo();
            fail("Failed to throw expected NoSuchObjectLocalException when calling getInfo on a canceled timer");
        } catch (NoSuchObjectLocalException ex) {
            //pass
            svLogger.logp(Level.FINEST, CLASS_NAME, info, "threw expected NoSuchObjectLocalException");
        } catch (Exception ex) {
            svLogger.logp(Level.FINEST, CLASS_NAME, info, "threw unexpected Exception", ex);
            fail("Caught unexpected exception: " + ex);
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, info);
        }
    }

    private void createTwoTimersInSameMethod() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "createTwoTimersInSameMethod");
        }

        final String info1 = "createTwoTimersInSameMethod1";
        final String info2 = "createTwoTimersInSameMethod2";

        svTimerLatch = new CountDownLatch(2); // Wait for both timers to expire
        Timer t1 = TimerHelper.createTimer(ivSessionCtx.getTimerService(),
                                           DURATION, null, info1, false, null);
        svLogger.logp(Level.INFO, CLASS_NAME, info1, "Created timer 1: " + t1);

        Timer t2 = TimerHelper.createTimer(ivSessionCtx.getTimerService(),
                                           DURATION + DURATION, null, info2, false, null);
        svLogger.logp(Level.INFO, CLASS_NAME, info2, "Created timer 2: " + t2);

        assertNotSame("Timer service returned the same timer for both create calls", t1, t2);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "createTwoTimersInSameMethod");
        }
    }

    private void createTimerInThePast(TstName testName) {

        final String method = "createTimerInThePast"; // 630958
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, method, testName); // 630958
        }

        TimerService ts = ivSessionCtx.getTimerService();
        Timer t = null;

        if (testName == TstName.TEST_EXPIRATION_IN_PAST) {
            // this create call should work - it should expire immediately
            Date date = new Date();
            date.setTime(date.getTime() - DURATION);
            svTimerLatch = new CountDownLatch(1); // wait for this timer to expire 1x
            t = TimerHelper.createTimer(ts, null, date, "createTimerInThePastExpectingImmediateExpiration", false, null); // 630958
            assertNotNull(t);

        } else {
            try {
                // these create calls should fail with IllegalArgumentException
                if (testName == TstName.TEST_INVALID_EXPIRATION) {
                    Date date = new Date();
                    date.setTime(-1);
                    t = TimerHelper.createTimer(ts, null, date, "createTimerInThePastNegativeTimeExpectingIllegalArgumentException", false, null);
                } else if (testName == TstName.TEST_NEGATIVE_DURATION) {
                    t = TimerHelper.createTimer(ts, -DURATION, null, "createTimerInThePastNegativeDurationExpectingIllegalArgumentException", false, null);
                } else {
                    fail("Unknown Test!");
                }
                fail("Incorrectly created a timer that expires in the past.  Expected an IllegalArgumentException.");
            } catch (IllegalArgumentException ex) {
                // pass
                svLogger.logp(Level.INFO, CLASS_NAME, method, "Caught expected IllegalArgumentException"); // 630958
            }

            assertNull(t);
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, method); // 630958
        }

    }

    private void createOverflowDurationTimer() {
        TimerService ts = ivSessionCtx.getTimerService();
        Timer t = TimerHelper.createTimer(ts, Integer.MAX_VALUE * 3L / 2, null, "createOverflowDurationTimer", false, null);
        assertNotNull(t);
    }

    private void createOverflowExpirationTimer() {
        TimerService ts = ivSessionCtx.getTimerService();
        Timer t = TimerHelper.createTimer(ts, null, new Date(System.currentTimeMillis() + Integer.MAX_VALUE * 3L / 2), "createOverflowExpirationTimer", false, null);
        assertNotNull(t);
    }

    private void createOverflowIntervalTimer() {
        svTimerLatch = new CountDownLatch(1); // wait for this timer to expire 1x
        TimerService ts = ivSessionCtx.getTimerService();
        Timer t = TimerHelper.createTimer(ts, 0L, null, "createOverflowIntervalTimer", false, Integer.MAX_VALUE * 3L / 2);
        assertNotNull(t);
    }

    /**
     * The call to timer.getHandle() should throw an IllegalStateException for
     * non-persistent timers.
     */
    private void createTimerGetHandle() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, "createTimerGetHandle");
        }

        TimerService ts = ivSessionCtx.getTimerService();
        Timer t = TimerHelper.createTimer(ts, DURATION, null, HANDLE_INFO, false, null);
        assertNotNull(t);

        TimerHandle th = null;
        try {
            th = t.getHandle();
            fail("timer.getHandle() did not throw expected IllegalStateException");
        } catch (IllegalStateException ex) {
            //pass
        }

        assertNull(th);
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, "createTimerGetHandle");
        }
    }

    private void createIntervalTimerViaDuration() {
        svTimerLatch = new CountDownLatch(3); // wait for this timer to expire 3x
        TimerService ts = ivSessionCtx.getTimerService();
        Timer t = TimerHelper.createTimer(ts, DURATION, null, INTERVAL_INFO, false, INTERVAL);
        assertNotNull(t);
    }

    private void createIntervalTimerViaDate() {
        svTimerLatch = new CountDownLatch(3); // wait for this timer to expire 3x
        TimerService ts = ivSessionCtx.getTimerService();
        Date date = new Date();
        date.setTime(date.getTime() + DURATION);
        Timer t = TimerHelper.createTimer(ts, null, date, INTERVAL_INFO, false, INTERVAL);
        assertNotNull(t);
    }

    private void checkTimerAPI() {
        TimerService ts = ivSessionCtx.getTimerService();
        Timer t = TimerHelper.createTimer(ts, DURATION, null, "test1", false, null);
        assertEquals(1, ts.getTimers().size());
        assertTrue(t.getTimeRemaining() <= DURATION);
        assertFalse(t.isPersistent());
        assertFalse(t.isCalendarTimer()); // F743-7593
        try {
            t.getHandle();
            fail("timer.getHandle() should have thrown IllegalStateException");
        } catch (IllegalStateException ex) {
            //pass
        }
        assertEquals("test1", t.getInfo());

        // test getNextTimeout()
        Date now = new Date();
        Date oneMinuteFromNow = new Date();
        oneMinuteFromNow.setTime(oneMinuteFromNow.getTime() + 60000);
        Date d = t.getNextTimeout();
        assertNotNull("timer.getNextTimeout() returned null date", d);
        assertTrue(now.before(d));
        assertTrue(oneMinuteFromNow.after(d));

        t.cancel();
        assertEquals(0, ts.getTimers().size());

        // operation after the timer has been canceled should throw a
        // NoSuchObjectLocalException
        try {
            t.getNextTimeout();
            fail("canceledTimer.getNextTimeout() did not throw expected NoSochObjectLocalException");
        } catch (NoSuchObjectLocalException ex) {
            //pass
            svLogger.logp(Level.FINEST, CLASS_NAME, "checkTimerAPI",
                          "canceledTimer.getNextTimeout() threw expected NoSuchObjectLocalException");
        } catch (Exception ex) {
            fail("canceledTimer.getNextTimeout() threw unexpected exception " +
                 "instead of expected NoSochObjectLocalException: " + ex);
        }

        Date date = new Date();
        date.setTime(date.getTime() + DURATION);
        t = TimerHelper.createTimer(ts, null, date, "test2", false, null);
        assertTrue(t.getTimeRemaining() <= DURATION);
        assertFalse(t.isPersistent());
        assertFalse(t.isCalendarTimer()); // F743-7593
        try {
            t.getHandle();
            fail("timer.getHandle() should have thrown IllegalStateException");
        } catch (IllegalStateException ex) {
            //pass
        }
        assertEquals("test2", t.getInfo());
        assertEquals(date, t.getNextTimeout());

        t.cancel();
        assertEquals(0, ts.getTimers().size());

        try {
            t.getNextTimeout();
            fail("canceledTimer.getNextTimeout() did not throw expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException ex) {
            //pass
            svLogger.logp(Level.FINEST, CLASS_NAME, "checkTimerAPI",
                          "canceledTimer.getNextTimeout() threw expected NoSuchObjectLocalException");
        } catch (Exception ex) {
            fail("canceledTimer.getNextTimeout() threw unexpected exception " +
                 "instead of expected NoSuchObjectLocalException: " + ex);
        }
    }

    private void checkTimerServiceAPI() {
        TimerService ts = ivSessionCtx.getTimerService();
        TimerConfig tc = new TimerConfig();
        tc.setPersistent(false);
        tc.setInfo("checkTimerServiceAPI");
        Date goodDate = new Date();
        goodDate.setTime(goodDate.getTime() + DURATION);

        /////////////////////////////////////////////////////////////////////////
        // TimerService.createCalendarTimer(ScheduleExpression)

        //variation 1: null ScheduleExpression
        try {
            ts.createCalendarTimer(null);
            fail("expected IllegalArgumentException from ts.createCalendarTimer(null)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //Other variation of createCalendarTimer(ScheduleExpression) tested elsewhere

        /////////////////////////////////////////////////////////////////////////
        // TimerService.createCalendarTimer(ScheduleExpression, TimerConfig)

        //variation 1: null ScheduleExpression
        try {
            ts.createCalendarTimer(null, tc);
            fail("expected IllegalArgumentException from ts.createCalendarTimer(null, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //Other variation of createCalendarTimer(ScheduleExpression, TimerConfig) tested elsewhere

        /////////////////////////////////////////////////////////////////////////
        // TimerService.createIntervalTimer(Date, long, TimerConfig)

        //variation 1: invalid expiration
        try {
            Date d = new Date();
            d.setTime(-1);
            ts.createIntervalTimer(d, INTERVAL, tc);
            fail("expected IllegalArgumentException from ts.createIntervalTimer(invalidDate, long, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //variation 2: null expiration
        try {
            ts.createIntervalTimer(null, INTERVAL, tc);
            fail("expected IllegalArgumentException from ts.createIntervalTimer(null, long, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //variation 3: invalid interval
        try {
            ts.createIntervalTimer(goodDate, -INTERVAL, tc);
            fail("expected IllegalArgumentException from ts.createIntervalTimer(goodDate, invalidInterval, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //variation 4: null TimerConfig
        try {
            ts.createIntervalTimer(goodDate, 500, null);
            fail("expected EJBException from ts.createIntervalTimer(goodDate, long, null)");
        } catch (EJBException ex) {
            //pass
        }

        /////////////////////////////////////////////////////////////////////////
        // TimerService.createIntervalTimer(long, long, TimerConfig)

        //variation 1: invalid duration
        try {
            ts.createIntervalTimer(-DURATION, INTERVAL, tc);
            fail("expected IllegalArgumentException from ts.createIntervalTimer(invalidDuration, long, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //variation 2: invalid interval
        try {
            ts.createIntervalTimer(DURATION, -INTERVAL, tc);
            fail("expected IllegalArgumentException from ts.createIntervalTimer(long, invalidInterval, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //variation 3: null TimerConfig
        try {
            ts.createIntervalTimer(DURATION, INTERVAL, null);
            fail("expected EJBException from ts.createIntervalTimer(long, long, null)");
        } catch (EJBException ex) {
            //pass
        }

        /////////////////////////////////////////////////////////////////////////
        // TimerService.createSingleActionTimer(Date, TimerConfig)

        //variation 1: invalid expiration
        try {
            Date d = new Date();
            d.setTime(-1);
            ts.createSingleActionTimer(d, tc);
            fail("expected IllegalArgumentException from ts.createSingleActionTimer(invalidDate, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //variation 2: null expiration
        try {
            ts.createSingleActionTimer(null, tc);
            fail("expected IllegalArgumentException from ts.createSingleActionTimer(nullDate, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //variation 3: null TimerConfig
        try {
            ts.createSingleActionTimer(goodDate, null);
            fail("expected EJBException from ts.createSingleActionTimer(goodDate, null)");
        } catch (EJBException ex) {
            //pass
        }

        /////////////////////////////////////////////////////////////////////////
        // TimerService.createSingleActionTimer(long, TimerConfig)

        //variation 1: invalid duration
        try {
            ts.createSingleActionTimer(-DURATION, tc);
            fail("expected IllegalArgumentException from ts.createSingleActionTimer(invalidDuration, TimerConfig)");
        } catch (IllegalArgumentException ex) {
            //pass
        }

        //variation 2: null TimerConfig - not spec defined
        try {
            ts.createSingleActionTimer(DURATION, null);
            fail("expected EJBException from ts.createSingleActionTimer(long, null)");
        } catch (EJBException ex) {
            //pass
        }

        /////////////////////////////////////////////////////////////////////////
        // TimerService.getTimers()
        // Not testing here since this is tested in several other tests in this bean.

        /////////////////////////////////////////////////////////////////////////
        // The following APIs are not tested here as they are persistent Timers:
        // TimerService.createTimer(Date, long, TimerConfig)
        // TimerService.createIntervalTimer(long, long, TimerConfig)
        // TimerService.createSingleActionTimer(Date, TimerConfig)
        // TimerService.createSingleActionTimer(long, TimerConfig)
        /////////////////////////////////////////////////////////////////////////

    }

    private void createCalendarAndFindInSameMethod() {
        final String info = "createCalendarAndFindInSameMethod";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASS_NAME, info);
        }
        TimerService timerService = ivSessionCtx.getTimerService();

        // create a calendar timer that expires every second
        ScheduleExpression schedule1 = new ScheduleExpression().second("*").minute("*").hour("*");
        String timerInfo1 = info + ":T1";
        Timer timer1 = timerService.createCalendarTimer(schedule1, new TimerConfig(timerInfo1, false));
        svLogger.logp(Level.INFO, CLASS_NAME, info, "Created timer: " + timer1);

        // create a calendar timer that ends one minute in the past
        Date end = new Date(System.currentTimeMillis() - 60000);
        ScheduleExpression schedule2 = new ScheduleExpression().second("*").minute("*").hour("*").end(end);
        Timer timer2 = timerService.createCalendarTimer(schedule2, new TimerConfig(info + ":T2", false));
        svLogger.logp(Level.INFO, CLASS_NAME, info, "Created timer: " + timer2);

        // verify that only the first timer is returned by getTimers
        Collection<Timer> timers = timerService.getTimers();
        assertEquals("Found wrong number of timers", 1, timers.size());
        for (Timer timer : timers) {
            assertEquals("Wrong timer found", timerInfo1, timer.getInfo());
        }

        // verify the first timer may be cancelled
        try {
            timer1.cancel();
        } catch (NoSuchObjectLocalException ex) {
            fail("timer could not be cancelled : " + ex);
        }

        // verify the second timer does not exist
        try {
            timer2.cancel();
            fail("timer unexpectedly could be cancelled");
        } catch (NoSuchObjectLocalException ex) {
            // failure expected
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASS_NAME, info);
        }
    }

}
