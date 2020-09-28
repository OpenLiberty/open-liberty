/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.persistent.core.ejb;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.TimedObject;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import org.junit.Assert;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

/**
 * Bean implementation for a basic Stateless Session bean that implements
 * the TimedObject interface. It contains methods to test TimerService
 * access. <p>
 **/
@SuppressWarnings("serial")
public class StatelessTimedBean implements SessionBean, TimedObject {
    public static final long DEFAULT_EXPIRATION = 60 * 1000;
    public static final long EXPIRATION = 3 * 1000;
    public static final long INTERVAL = 5 * 1000;
    public static final long MAX_TIMER_WAIT = 2 * 60 * 1000;
    public static final long TIMER_PRECISION = 900;

    private static final Logger svLogger = Logger.getLogger(StatelessTimedBean.class.getName());

    private static Timer timer[] = new Timer[10];
    public static int svTimeoutCounts[] = new int[10];
    private static CountDownLatch timerLatch = new CountDownLatch(1);
    private static CountDownLatch timerIntervalLatch = new CountDownLatch(1);
    private static CountDownLatch timerFirstIntervalLatch = new CountDownLatch(1);
    private static CountDownLatch timerIntervalWaitLatch = new CountDownLatch(1);
    private static CountDownLatch testOpsInTimeoutLatch = new CountDownLatch(1);

    private SessionContext ivContext;
    private TimerService ivTimerService;
    private final boolean isJavaEE;

    // These fields hold the test results for EJB callback methods
    private static Object svSetSessionContextResults;
    private static Object svEjbCreateResults;
    private static Object svEjbRemoveResults;
    private static Object svEjbTimeoutResults = null;

    /** Required default constructor. **/
    public StatelessTimedBean() {
        isJavaEE = SessionContext.class.getName().startsWith("javax.");
    }

    /**
     * Recursive call to trigger creation of multiple bean instances.
     */
    public void recursiveCall(int depth) {
        if (depth <= 1) {
            return;
        }
        StatelessTimedObject bean = (StatelessTimedObject) ivContext.getEJBLocalObject();
        bean.recursiveCall(depth - 1);
    }

    public TimerService getTimerService() {
        return ivTimerService;
    }

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Stateless Session bean that implements the TimedObject interface. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateless bean, the results will be stored
     * in an instance variable. The test may then grab any instance (all
     * the same) and extract the results
     * ({@link #getSetSessionContextResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * </ol> <p>
     *
     * @param sc session context provided by container.
     */
    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;

        if (svSetSessionContextResults != null) {
            return;
        }

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info("setSessionContext: Calling getTimerService()");
            ivContext.getTimerService();
            Assert.fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "1 ---> Caught expected exception", ise);
            svSetSessionContextResults = true;
        } catch (Throwable th) {
            svLogger.log(Level.SEVERE, "Unexpected exception from getTimerService()", th);
            svSetSessionContextResults = th;
        }
    }

    private void verifyResults(Object results) {
        if (results instanceof Throwable) {
            throw new Error((Throwable) results);
        }

        Assert.assertEquals(true, results);
    }

    /**
     * Returns the results of testing performed in {@link #setSessionContext setSessionContext()}. <p>
     *
     * Since setSessionContext may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in setSesionContext method.
     */
    public void verifySetSessionContextResults() {
        verifyResults(svSetSessionContextResults);
    }

    /**
     * Test getTimerService()/TimerService access from ejbCreate on a
     * Stateless Session bean that implements the TimedObject interface. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateless bean, the results will be stored
     * in an instance variable. The test may then grab any instance (all
     * the same) and extract the results ({@link #getEjbCreateResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * </ol> <p>
     */
    public void ejbCreate() throws CreateException {
        TimerService ts = null;
        Timer timer = null;

        if (svEjbCreateResults != null) {
            ts = ivContext.getTimerService();
            ivTimerService = ts;
            return;
        }

        try {
            // -----------------------------------------------------------------------
            // 1 - Verify getTimerService() returns a valid TimerService
            // -----------------------------------------------------------------------
            svLogger.info("ejbCreate: Calling getTimerService()");
            ts = ivContext.getTimerService();
            ivTimerService = ts;
            Assert.assertNotNull("1 ---> Could not get TimerService", ts);

            // -----------------------------------------------------------------------
            // 2 - Verify TimerService.createTimer() fails with IllegalStateException
            // -----------------------------------------------------------------------
            try {
                svLogger.info("ejbCreate: Calling TimerService.createTimer()");
                timer = ts.createTimer(DEFAULT_EXPIRATION, (java.io.Serializable) null);
                Assert.fail("2 ---> createTimer should have failed!");
                timer.cancel();
            } catch (IllegalStateException ise) {
                svLogger.log(Level.INFO, "2 ---> Caught expected exception", ise);
            }

            // -----------------------------------------------------------------------
            // 3 - Verify TimerService.getTimers() fails with IllegalStateException
            // -----------------------------------------------------------------------
            try {
                svLogger.info("ejbCreate: Calling getTimers()");
                Collection<Timer> timers = ts.getTimers();
                Assert.fail("3 ---> getTimers should have failed! " + timers);
            } catch (IllegalStateException ise) {
                svLogger.log(Level.INFO, "3 ---> Caught expected exception", ise);
            }

            svEjbCreateResults = true;
        } catch (Throwable t) {
            svLogger.log(Level.SEVERE, "test failure", t);
            svEjbCreateResults = t;
        }

    }

    /**
     * Returns the results of testing performed in {@link #ejbCreate}. <p>
     *
     * Since ejbCreate may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbCreate method.
     */
    public void verifyEjbCreateResults() {
        verifyResults(svEjbCreateResults);
    }

    /**
     * Test getTimerService()/TimerService access from ejbRemove on a
     * Stateless Session bean that implements the TimedObject interface. <p>
     *
     * Since this method may not be called directly, and is only called by
     * EJB Container when it feels like it, there is no way to obtain the
     * results of this test from a test case. Instead, the results will just
     * be printed out, in hopes that they are noticed. An attempt will be made
     * to make negative results very obvious. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * </ol> <p>
     */
    @Override
    public void ejbRemove() {
        TimerService ts = null;
        Timer timer = null;

        if (svEjbRemoveResults != null) {
            return;
        }

        try {
            // -----------------------------------------------------------------------
            // 1 - Verify getTimerService() returns a valid TimerService
            // -----------------------------------------------------------------------
            ts = ivContext.getTimerService();
            Assert.assertNotNull("1 ---> Could not get TimerService", ts);

            // -----------------------------------------------------------------------
            // 2 - Verify TimerService.createTimer() fails with IllegalStateException
            // -----------------------------------------------------------------------
            try {
                timer = ts.createTimer(DEFAULT_EXPIRATION, (java.io.Serializable) null);
                Assert.fail("2 ---> createTimer should have failed! " + timer);
            } catch (IllegalStateException ise) {
                svLogger.log(Level.INFO, "StatelessTimedBean.ejbRemove: " +
                                         "2 ---> Caught expected exception",
                             ise);
            }

            // -----------------------------------------------------------------------
            // 3 - Verify TimerService.getTimers() fails with IllegalStateException
            // -----------------------------------------------------------------------
            try {
                Collection<Timer> timers = ts.getTimers();
                Assert.fail("3 ---> getTimers should have failed! " + timers);
            } catch (IllegalStateException ise) {
                svLogger.log(Level.INFO, "StatelessTimedBean.ejbRemove: " +
                                         "3 ---> Caught expected exception",
                             ise);
            }

            svEjbRemoveResults = true;
        } catch (Throwable t) {
            svLogger.info("test failure : " + t);
            t.printStackTrace(System.out);
            svEjbRemoveResults = t;
        }
    }

    /**
     * Verifies the results of testing performed in {@link #ejbRemove}. <p>
     *
     * Since ejbRemove may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     */
    public void verifyEjbRemoveResults() {
        verifyResults(svEjbRemoveResults);
    }

    /** Never called for Stateless Session Bean. **/
    @Override
    public void ejbActivate() {
    }

    /** Never called for Stateless Session Bean. **/
    @Override
    public void ejbPassivate() {
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test is executed in 2 phases. The first phase will create
     * multiple Timers, and the second phase will confirm that they are
     * executed correctly. This cannot all be done in one method call,
     * as the Timers will not execute until after their creation has
     * been committed (at the end of phase 1 - and the return from the
     * method call). <p>
     *
     * This test method will confirm the following in Phase 1 :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer(duration, null) works
     * <li> TimerService.createTimer(duration, info) works
     * <li> TimerService.createTimer(duration, interval, info) works
     * <li> TimerService.createTimer(date, info) works
     * <li> TimerService.createTimer(date, interval, info) works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> TimerService.getTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * </ol> <p>
     */
    public void testTimerServicePhase1() throws Exception {
        clearAllTimers();
        TimerHandle timerHandle = null;
        Timer retTimer = null;

        timer = new Timer[6];
        svTimeoutCounts = new int[timer.length];

        timerLatch = new CountDownLatch(timer.length - 1); // one timer cancelled
        timerIntervalWaitLatch = new CountDownLatch(1); // block interval timer until ready
        timerIntervalLatch = new CountDownLatch(2);
        timerFirstIntervalLatch = new CountDownLatch(1);

        // -------------------------------------------------------------------
        // 1 - Verify getTimerService() returns a valid TimerService
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling getTimerService()");
        ivTimerService = ivContext.getTimerService();
        Assert.assertNotNull("1 ---> Could not get TimerService", ivTimerService);

        // -------------------------------------------------------------------
        // 2 - Verify TimerService.createTimer(duration, null) works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling " +
                      "TimerService.createTimer(duration, null)");
        timer[0] = ivTimerService.createTimer(EXPIRATION, (java.io.Serializable) null);
        Assert.assertNotNull("2 ---> TimerService.createTimer" +
                             "(duration, null) did not work", timer[0]);

        // -------------------------------------------------------------------
        // 3 - Verify TimerService.createTimer(duration, info) works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling " +
                      "TimerService.createTimer(duration, info)");
        timer[1] = ivTimerService.createTimer(EXPIRATION, new Integer(1));
        Assert.assertNotNull("3 ---> TimerService.createTimer" +
                             "(duration, info) did not work", timer[1]);

        // -------------------------------------------------------------------
        // 4 - Verify TimerService.createTimer(duration, interval, info) works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling " +
                      "TimerService.createTimer(duration, interval, info)");
        timer[2] = ivTimerService.createTimer(EXPIRATION, INTERVAL, new Integer(2));
        Assert.assertNotNull("4 ---> TimerService.createTimer" +
                             "(duration, interval, info) did not work", timer[2]);

        // -------------------------------------------------------------------
        // 5 - Verify TimerService.createTimer(date, info) works
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling " +
                          "TimerService.createTimer(date, info)");
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            timer[3] = ivTimerService.createTimer(expiration, new Integer(3));
            Assert.assertNotNull("5 ---> TimerService.createTimer" +
                                 "(date, info) did not work", timer[3]);
        }

        // -------------------------------------------------------------------
        // 6 - Verify TimerService.createTimer(date, interval, info) works
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling " +
                          "TimerService.createTimer(date, interval, info)");
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
            timer[4] = ivTimerService.createTimer(expiration, INTERVAL, new Integer(4));
            Assert.assertNotNull("6 ---> TimerService.createTimer" +
                                 "(date, interval, info) did not work", timer[4]);
        }

        // -------------------------------------------------------------------
        // 7 - Verify Timer.getTimeRemaining() on single event Timer works
        // -------------------------------------------------------------------
        {
            // Create an extra timer for testing now, and testing cancel later...
            svLogger.info("testTimerService: Calling " +
                          "TimerService.createTimer(duration, info)");
            timer[5] = ivTimerService.createTimer(EXPIRATION, new Integer(5));
            svLogger.info("testTimerService: Calling Timer.getTimeRemaining()");
            long remaining = timer[5].getTimeRemaining();
            Assert.assertTrue("7 ---> Timer.getTimeRemaining() returned unexpected value: " + remaining,
                              remaining >= 1 && remaining <= EXPIRATION);
        }

        // -------------------------------------------------------------------
        // 8 - Verify Timer.getTimeRemaining() on repeating Timer works
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling Timer.getTimeRemaining()");
            long remaining = timer[2].getTimeRemaining();
            Assert.assertTrue("8 ---> Timer.getTimeRemaining() returned unexpected value: " + remaining,
                              remaining >= 1 && remaining <= EXPIRATION);
        }

        // -------------------------------------------------------------------
        // 9 - Verify Timer.getInfo() returning null works
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling Timer.getInfo()");
            Object timerInfo = timer[0].getInfo();
            Assert.assertNull("9 ---> Timer.getInfo() was not null: " + timerInfo +
                              "", timerInfo);
        }

        // -------------------------------------------------------------------
        // 10 - Verify Timer.getInfo() returning serializable works
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling Timer.getInfo()");
            Object timerInfo = timer[1].getInfo();
            Assert.assertEquals("10 --> Timer.getInfo() returned unexpected value: " + timerInfo +
                                "", new Integer(1), timerInfo);
        }

        // -------------------------------------------------------------------
        // 11 - Verify Timer.getHandle() works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.getHandle()");
        timerHandle = timer[5].getHandle();
        Assert.assertNotNull("11 --> Timer.getHandle() was null: " +
                             timerHandle, timerHandle);

        // -------------------------------------------------------------------
        // 12 - Verify TimerHandle.getTimer() works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling TimerHandle.getTimer()");
        retTimer = timerHandle.getTimer();
        Assert.assertNotNull("12 --> TimerHandle.getTimer() was null: " +
                             retTimer, retTimer);

        // -------------------------------------------------------------------
        // 13 - Verify Timer.equals() works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.equals()");
        Assert.assertEquals("13 --> Timer.equals() returned unexpected value: " + timer[5],
                            timer[5], retTimer);

        // -------------------------------------------------------------------
        // 14 - Verify TimerService.getTimers() returns all created Timers
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling getTimers()");
            Collection<Timer> timers = ivTimerService.getTimers();

            // Print out the results for debug purposes...
            Object[] timersArray = timers.toArray();
            for (int i = 0; i < timersArray.length; i++) {
                svLogger.info("  returned : " + timersArray[i]);
            }

            Assert.assertEquals("14 --> getTimers did not return 6 Timers", 6, timers.size());

            // Make sure they are the correct timers...
            for (int i = 0; i < timer.length; i++) {
                if (!timers.contains(timer[i]))
                    Assert.fail("Timer[" + i + "] not returned: " + timer[i]);
            }
        }

        // -------------------------------------------------------------------
        // 15 - Verify Timer.cancel() works
        // -------------------------------------------------------------------
        svLogger.info("testTimerService: Calling Timer.cancel()");
        timer[5].cancel();
        svLogger.info("15 --> Timer.cancel() worked");

        // -------------------------------------------------------------------
        // 16 - Verify NoSuchObjectLocalException occurs accessing canceled
        //      timer
        // -------------------------------------------------------------------
        try {
            svLogger.info("testTimerService: Calling Timer.getInfo() " +
                          "on cancelled Timer");
            timer[5].getInfo();
            Assert.fail("16 --> Timer.getInfo() did not throw expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            svLogger.log(Level.INFO, "16 --> Caught expected exception", nso);
        }

        // -------------------------------------------------------------------
        // 17 - Verify TimerService.getTimers() does not return cancelled Timers
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling getTimers()");
            Collection<Timer> timers = ivTimerService.getTimers();

            // Print out the results for debug purposes...
            Object[] timersArray = timers.toArray();
            for (int i = 0; i < timersArray.length; i++) {
                svLogger.info("  returned : " + timersArray[i]);
            }

            Assert.assertEquals("17 --> getTimers did not return 5 Timers", 5, timers.size());

            // Make sure they are the correct timers...
            for (int i = 0; i < timer.length; i++) {
                switch (i) {
                    case 5:
                        if (timers.contains(timer[i]))
                            Assert.fail("Timer[" + i + "] returned (wrong timer): " + timer[i]);
                        break;

                    default:
                        if (!timers.contains(timer[i]))
                            Assert.fail("Timer[" + i + "] not returned: " + timer[i]);
                        break;
                }
            }
        }

    }

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test is executed in 2 phases. The first phase will create
     * multiple Timers, and the second phase will confirm that they are
     * executed correctly.
     *
     *
     * This test method will confirm the following in Phase 2 :
     * <ol start=18>
     * <li> ejbTimeout is executed for valid Timers
     * <li> NoSuchObjectLocalException occurs accessing expired timer
     * <li> TimerService.getTimers() does not return expired Timers
     * <li> Timer.getNextTimeout() on repeating Timer works
     * <li> ejbTimeout is executed multiple times for repeating Timers
     * <li> NoSuchObjectLocalException occurs accessing self cancelled timer
     * <li> TimerService.getTimers() returns empty collection after all Timers
     * have expired or been cancelled.
     * </ol>
     */

    public void testTimerServicePhase2() throws Exception {

        // ---------------------------------------------------------------------
        //  Beginning Phase 2
        // ---------------------------------------------------------------------
        waitForTimers(timerLatch, MAX_TIMER_WAIT);

        // -------------------------------------------------------------------
        // 18 - Verify ejbTimeout is executed for valid Timers
        // -------------------------------------------------------------------
        boolean successful = true;

        // See if all except cancelled timers have expired once...
        for (int i = 0; i < timer.length; i++) {
            switch (i) {
                case 5:
                    if (svTimeoutCounts[i] != 0) {
                        successful = false;
                        svLogger.info("Cancelled Timer[" + i + "] executed: " +
                                      timer[i]);
                    }
                    break;

                default:
                    if (svTimeoutCounts[i] != 1) {
                        successful = false;
                        svLogger.info("Timer[" + i + "] not executed once: " +
                                      timer[i]);
                    }
                    break;
            }
        }

        Assert.assertTrue("18 --> ejbTimeout not executed on 5 Timers",
                          successful);

        // -------------------------------------------------------------------
        // 19 - Verify NoSuchObjectLocalException occurs accessing expired timer
        // -------------------------------------------------------------------
        try {
            svLogger.info("testTimerService: Calling Timer.getInfo() " +
                          "on expired Timer");
            timer[1].getInfo();
            Assert.fail("19 --> Timer.getInfo() did not throw expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            svLogger.log(Level.INFO, "19 --> Caught expected exception", nso);
        }

        // -------------------------------------------------------------------
        // 20 - Verify TimerService.getTimers() does not return expired Timers
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling getTimerService()");
            ivTimerService = ivContext.getTimerService();
            svLogger.info("testTimerService: Calling getTimers()");
            Collection<Timer> timers = ivTimerService.getTimers();

            // Print out the results for debug purposes...
            Object[] timersArray = timers.toArray();
            for (int i = 0; i < timersArray.length; i++) {
                svLogger.info("  returned : " + timersArray[i]);
            }

            Assert.assertEquals("20 --> getTimers did not return 2 Timers", 2, timers.size());

            // Make sure they are the correct timers...
            for (int i = 0; i < timer.length; i++) {
                switch (i) {
                    case 0:
                    case 1:
                    case 3:
                    case 5:
                        if (timers.contains(timer[i]))
                            Assert.fail("Timer[" + i + "] returned (wrong timer): " + timer[i]);
                        break;

                    default:
                        if (!timers.contains(timer[i]))
                            Assert.fail("Timer[" + i + "] not returned: " + timer[i]);
                        break;
                }
            }
        }

        // -------------------------------------------------------------------
        // 21 - Verify Timer.getNextTimeout() on repeating Timer works
        // -------------------------------------------------------------------
        {
            //wait for first interval
            waitForTimers(timerFirstIntervalLatch, MAX_TIMER_WAIT);
            svLogger.info("testTimerService: Calling Timer.getNextTimeout()");
            Date nextTime = timer[2].getNextTimeout();
            long remaining = nextTime.getTime() - System.currentTimeMillis();
            Assert.assertTrue("21 --> Timer.getNextTimeout() returned unexpected value: " + remaining,
                              remaining >= (1 - TIMER_PRECISION) && remaining <= (INTERVAL + TIMER_PRECISION));
        }

        // -------------------------------------------------------------------
        // 22 - Verify ejbTimeout is executed multiple times for repeating Timers
        // -------------------------------------------------------------------
        {
            successful = true;

            svLogger.info("testTimerService: Waiting for timers to expire. . .");
            timerIntervalWaitLatch.countDown(); // allow 2nd interval to run
            waitForTimers(timerIntervalLatch, MAX_TIMER_WAIT);

            // See if all except cancelled timers have expired once...
            // and repeating timers have executed twice...
            for (int i = 0; i < timer.length; i++) {
                switch (i) {
                    case 2:
                    case 4:
                        if (svTimeoutCounts[i] != 2) {
                            successful = false;
                            svLogger.info("Timer[" + i + "] not executed twice: " +
                                          timer[i]);
                        }
                        break;

                    case 5:
                        if (svTimeoutCounts[i] != 0) {
                            successful = false;
                            svLogger.info("Cancelled Timer[" + i + "] unexpectedly executed: " +
                                          timer[i]);
                        }
                        break;

                    default:
                        if (svTimeoutCounts[i] != 1) {
                            successful = false;
                            svLogger.info("Timer[" + i + "] not executed once: " +
                                          timer[i]);
                        }
                        break;
                }
            }

            Assert.assertTrue("22 --> ejbTimeout not executed on exactly 2 Timers",
                              successful);
        }

        // -------------------------------------------------------------------
        // 23 - Verify NoSuchObjectLocalException occurs accessing self
        //      cancelled timer
        // -------------------------------------------------------------------
        try {
            svLogger.info("testTimerService: Calling Timer.getInfo() " +
                          "on self cancelled Timer");
            timer[2].getInfo();
            Assert.fail("23 --> Timer.getInfo() did not throw expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            svLogger.log(Level.INFO, "23 --> Caught expected exception", nso);
        }

        // -------------------------------------------------------------------
        // 24 - Verify TimerService.getTimers() returns empty collection after
        //      all Timers have expired or been cancelled.
        // -------------------------------------------------------------------
        {
            svLogger.info("testTimerService: Calling getTimerService()");
            ivTimerService = ivContext.getTimerService();
            svLogger.info("testTimerService: Calling getTimers()");
            Collection<Timer> timers = ivTimerService.getTimers();

            // Print out the results for debug purposes...
            Object[] timersArray = timers.toArray();
            for (int i = 0; i < timersArray.length; i++) {
                svLogger.info("  returned : " + timersArray[i]);
            }

            Assert.assertEquals("24 --> getTimers did not return 0 Timers", 0, timers.size());
        }
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test method will just perform a System.out for test01().
     * This should not occur, since test01 should cancel the timer prior to
     * its expiration. <p>
     *
     * This test method will count the number of times it is executed for
     * test04() and test05(). This method identifies that it is to perform
     * counting behavior when the Timer info object is either null or an Integer.
     * The integer value is used to differentiate multiple timers. The Timer
     * is cancelled after the 2nd expiration. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container expires a Timer, the results will be stored
     * in a static variable. The test may then grab any instance (all
     * the same) and extract the results ({@link #getEjbTimeoutResults}).
     * A static variable must be used, since only one of possibly many
     * bean instances will be used to execute the test. <p>
     *
     * This test method will confirm the following for test05(), when the
     * Timer info object is the String "testTimerService":
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer(duration, null) works
     * <li> TimerService.createTimer(duration, info) works
     * <li> TimerService.createTimer(duration, interval, info) works
     * <li> TimerService.createTimer(date, info) works
     * <li> TimerService.createTimer(date, interval, info) works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> TimerService.getTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * <li> ejbTimeout is executed for valid Timers
     * <li> NoSuchObjectLocalException occurs accessing expired timer
     * <li> TimerService.getTimers() does not return expired Timers
     * <li> Timer.getNextTimeout() on repeating Timer works
     * <li> ejbTimeout is executed multiple times for repeating Timers
     * <li> NoSuchObjectLocalException occurs accessing self cancelled timer
     * <li> TimerService.getTimers() returns empty collection after all Timers
     * have expired or been cancelled.
     * </ol> <p>
     *
     * This test method will confirm the following for test06(), when the
     * Timer info object is the String "testContextMethods-CMT":
     * <ol>
     * <li> SessionContext.getEJBObject() works
     * <li> SessionContext.getEJBLocalObject() works
     * <li> SessionContext.getMessageContext() fails with IllegalStateException
     * <li> EJBContext.getEJBHome() works
     * <li> EJBContext.getEJBLocalHome() works
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getUserTransaction() fails with IllegalStateException
     * <li> EJBContext.getRollbackOnly() - false works
     * <li> EJBContext.setRollbackOnly() works
     * <li> EJBContext.getRollbackOnly() - true works
     * <li> ejbTimeout is executed again when setRollbackOnly called.
     * </ol> <p>
     *
     * This test method will confirm the following for test07(), when the
     * Timer info object is the String "testContextMethods-BMT":
     * <ol>
     * <li> SessionContext.getEJBObject() works
     * <li> SessionContext.getEJBLocalObject() works
     * <li> SessionContext.getMessageContext() fails with IllegalStateException
     * <li> EJBContext.getEJBHome() works
     * <li> EJBContext.getEJBLocalHome() works
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getUserTransaction() works
     * <li> UserTransaction.begin() works
     * <li> EJBContext.getRollbackOnly() fails
     * <li> EJBContext.setRollbackOnly() fails
     * <li> UserTransaction.commit() works
     * </ol>
     **/
    @Override
    public void ejbTimeout(Timer timer) {
        int timerIndex = -1;
        Object info = timer.getInfo();

        // -----------------------------------------------------------------------
        // When the 'info' object is 'null' or an 'Integer', then this method
        // uses the Integer (0 for null) to differentiate between timers, and
        // keeps a count of how often each timer is executed.  Repeating timers
        // are cancelled after the 2nd expiration.
        // -----------------------------------------------------------------------
        if (info == null)
            timerIndex = 0;
        else if (info instanceof Integer)
            timerIndex = ((Integer) info).intValue();
        if (timerIndex == 2) {
            timerFirstIntervalLatch.countDown();
        }

        if (timerIndex >= 0) {
            if (svTimeoutCounts[timerIndex] == 1) {
                // Don't run the 2nd interval until test is ready
                try {
                    timerIntervalWaitLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                }
            }

            svTimeoutCounts[timerIndex]++;

            svLogger.info("Timer " + timerIndex + " expired " +
                          svTimeoutCounts[timerIndex] + " time(s)");

            if (svTimeoutCounts[timerIndex] == 1) {
                timerLatch.countDown();
            } else if (svTimeoutCounts[timerIndex] > 1) {
                timer.cancel();
                timerIntervalLatch.countDown();
            }

            return;
        }

        // For debug, just print out that the timer expired, and the name of the
        // test it will execute.
        svLogger.info("Timer expired: " + info);

        // -----------------------------------------------------------------------
        // Execute Test - if info is a String, then it is probably the name
        //                of a test to execute.
        // -----------------------------------------------------------------------
        if (info instanceof String) {
            String test = (String) info;

            // --------------------------------------------------------------------
            // Test getTimerService/TimerService access.....
            // --------------------------------------------------------------------

            // Timer Service access in ejbTimeout should be identical to that of a
            // business method..... so just call the business method implementation.
            // This is a direct Java call... so is functionally equivalent to the
            // code being here.  Saves duplicating the code.
            if (test.equals("testTimerService")) {
                // Cancel self, so this Timer doesn't interfere with getTimers()
                // calls in test.
                timer.cancel();

                try {
                    testTimerServicePhase1();
                    svEjbTimeoutResults = true;
                    testOpsInTimeoutLatch.countDown();
                } catch (Throwable t) {
                    svLogger.log(Level.SEVERE, "test failure", t);
                    t.printStackTrace(System.out);
                    svEjbTimeoutResults = t;
                    testOpsInTimeoutLatch.countDown();
                }
            }

            // --------------------------------------------------------------------
            // Test SessionContext method access for CMT bean.....
            // --------------------------------------------------------------------

            else if (test.equals("testContextMethods-CMT")) {
                if (svEjbTimeoutResults != null) {
                    svLogger.info("testContextMethods-CMT --> Timer not cancelled: ejbTimeout executed 2nd time");
                    testOpsInTimeoutLatch.countDown();
                    return;
                }

                svEjbTimeoutResults = testContextMethodsIndirect("CMT");
            }

            // --------------------------------------------------------------------
            // Test SessionContext method access for BMT bean.....
            // --------------------------------------------------------------------

            else if (test.equals("testContextMethods-BMT")) {
                svEjbTimeoutResults = testContextMethodsIndirect("BMT");
                testOpsInTimeoutLatch.countDown();
            }
        } // info instanceof String

        // -----------------------------------------------------------------------
        // When the 'info' object is something other than the String
        // "testTimerService", then the Timer was being used for a test that just
        // needed a Timer, and it has probably gone off accidentally.
        // -----------------------------------------------------------------------
    }

    /**
     * Returns the results of testing performed in {@link #ejbTimeout ejbTimeout()}. <p>
     *
     * Since ejbTimeout may not be called directly, the results are
     * stored in a static variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbTimeout method.
     */
    public void verifyEjbTimeoutResults() {
        verifyResults(svEjbTimeoutResults);
    }

    /**
     * Test SessionContext method access from a method on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * This test method will confirm the following for CMT (test06):
     * <ol>
     * <li> SessionContext.getEJBObject() fails with IllegalStateException
     * <li> SessionContext.getEJBLocalObject() works
     * <li> SessionContext.getMessageContext() fails with IllegalStateException
     * <li> EJBContext.getEJBHome() fails with IllegalStateException
     * <li> EJBContext.getEJBLocalHome() works
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getUserTransaction() fails with IllegalStateException
     * <li> EJBContext.getRollbackOnly() - false works
     * <li> EJBContext.setRollbackOnly() works
     * <li> EJBContext.getRollbackOnly() - true works
     * <li> ejbTimeout is executed again when setRollbackOnly called.
     * </ol> <p>
     *
     * This test method will confirm the following for BMT (test07):
     * <ol>
     * <li> SessionContext.getEJBObject() works
     * <li> SessionContext.getEJBLocalObject() works
     * <li> SessionContext.getMessageContext() fails with IllegalStateException
     * <li> EJBContext.getEJBHome() works
     * <li> EJBContext.getEJBLocalHome() works
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getUserTransaction() works
     * <li> UserTransaction.begin() works
     * <li> EJBContext.getRollbackOnly() fails
     * <li> EJBContext.setRollbackOnly() fails
     * <li> UserTransaction.commit() works
     * </ol>
     */
    public void testContextMethods(String txType) throws Exception {
        UserTransaction userTran = null;

        // -----------------------------------------------------------------------
        // 1 - Verify SessionContext.getEJBObject() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info("testContextMethods: Calling getEJBObject()");
            EJBObject ejb = ivContext.getEJBObject();
            Assert.fail("1 ---> getEJBObject should have failed! " + ejb);
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "1 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 2 - Verify SessionContext.getEJBLocalObject() works
        // -----------------------------------------------------------------------
        {
            svLogger.info("testContextMethods: Calling getEJBLocalObject()");
            EJBLocalObject ejb = ivContext.getEJBLocalObject();
            Assert.assertNotNull("2 ---> Could not get EJBLocalObject", ejb);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify SessionContext.getMessageContext() fails with
        //     IllegalStateException
        // -----------------------------------------------------------------------
        if (isJavaEE) {
            try {
                svLogger.info("testContextMethods: Calling getMessageContext()");
                MessageContext mc = ivContext.getMessageContext();
                Assert.fail("3 ---> getMessageContext should have failed! " + mc);
            } catch (IllegalStateException ise) {
                svLogger.log(Level.INFO, "3 ---> Caught expected exception", ise);
            }
        }

        // -----------------------------------------------------------------------
        // 4 - Verify EJBContext.getEJBHome() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            svLogger.info("testContextMethods: Calling getEJBHome()");
            EJBHome ejbHome = ivContext.getEJBHome();
            Assert.fail("4 ---> getEJBHome should have failed! " + ejbHome);
        } catch (IllegalStateException ise) {
            svLogger.log(Level.INFO, "4 ---> Caught expected exception", ise);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify EJBContext.getEJBLocalHome() works
        // -----------------------------------------------------------------------
        {
            svLogger.info("testContextMethods: Calling getEJBLocalHome()");
            EJBLocalHome ejbHome = ivContext.getEJBLocalHome();
            Assert.assertNotNull("5 ---> Could not get EJBLocalHome", ejbHome);
        }

        // -----------------------------------------------------------------------
        // 6 - Verify EJBContext.getCallerPrincipal() works
        // -----------------------------------------------------------------------
        {
            svLogger.info("testContextMethods: Calling getCallerPrincipal()");
            Principal principal = ivContext.getCallerPrincipal();
            Assert.assertNotNull("6 ---> Could not get CallerPrincipal", principal);
        }

        // -----------------------------------------------------------------------
        // 7 - Verify EJBContext.isCallerInRole() works
        // -----------------------------------------------------------------------
        {
            svLogger.info("testContextMethods: Calling isCallerInRole()");
            boolean inRole = ivContext.isCallerInRole("test");
            Assert.assertFalse("7 ---> Got CallerInRole (unexpectedly)", inRole);
        }

        // -----------------------------------------------------------------------
        // 8 - Verify EJBContext.getTimerService() works
        // -----------------------------------------------------------------------
        {
            svLogger.info("testContextMethods: Calling getTimerService()");
            TimerService ts = ivContext.getTimerService();
            Assert.assertNotNull("8 ---> Could not get TimerService", ts);
        }

        // -----------------------------------------------------------------------
        // 9 - Verify EJBContext.getUserTransaction()
        //     CMT - fails with IllegalStateException
        //     BMT - works
        // -----------------------------------------------------------------------
        try {
            svLogger.info("testContextMethods: Calling getUserTransaction()");
            userTran = ivContext.getUserTransaction();
            if (txType.equals("CMT"))
                Assert.fail("9 ---> getUserTransaction should have failed!");
            else
                Assert.assertNotNull("9 ---> Could not get UserTransaction", userTran);
        } catch (IllegalStateException ise) {
            if (txType.equals("CMT"))
                svLogger.log(Level.INFO, "9 ---> Caught expected exception", ise);
            else
                throw ise;
        }

        if (txType.equals("CMT")) {
            // --------------------------------------------------------------------
            // 10 - Verify EJBContext.getRollbackOnly() - false works           CMT
            // --------------------------------------------------------------------
            {
                svLogger.info("testContextMethods: Calling getRollbackOnly()");
                boolean rollback = ivContext.getRollbackOnly();
                Assert.assertFalse("10 --> Got RollbackOnly (unexpectedly)", rollback);
            }

            // --------------------------------------------------------------------
            // 11 - Verify EJBContext.setRollbackOnly() works                   CMT
            // --------------------------------------------------------------------
            {
                svLogger.info("testContextMethods: Calling setRollbackOnly()");
                ivContext.setRollbackOnly();
                svLogger.info("11 --> setRollbackOnly worked");
            }

            // --------------------------------------------------------------------
            // 12 - Verify EJBContext.getRollbackOnly() - true works            CMT
            // --------------------------------------------------------------------
            {
                svLogger.info("testContextMethods: Calling getRollbackOnly()");
                boolean rollback = ivContext.getRollbackOnly();
                Assert.assertTrue("12 --> Got RollbackOnly", rollback);
            }

            // --------------------------------------------------------------------
            // 13 - Verify ejbTimeout is executed again when setRollbackOnly
            //      called.                                                     CMT
            // --------------------------------------------------------------------
            // See ejbTimeout - this must be checked when ejbTimeout is called a
            //                  second time!
        } else {
            // --------------------------------------------------------------------
            // 10 - UserTransaction.begin() works                               BMT
            // --------------------------------------------------------------------
            svLogger.info("testContextMethods: Calling UserTran.begin()");
            userTran.begin();
            Assert.assertEquals("10 --> Started UserTransaction",
                                userTran.getStatus(), Status.STATUS_ACTIVE);

            // --------------------------------------------------------------------
            // 11 - Verify EJBContext.getRollbackOnly() fails                   BMT
            // --------------------------------------------------------------------
            try {
                svLogger.info("testContextMethods: Calling getRollbackOnly()");
                boolean rollback = ivContext.getRollbackOnly();
                Assert.fail("11 --> getRollbackOnly should have failed! " + rollback);
            } catch (IllegalStateException ise) {
                svLogger.log(Level.INFO, "testContextMethods() --> Caught expected exception", ise);
            }

            // --------------------------------------------------------------------
            // 12 - Verify EJBContext.setRollbackOnly() fails                   BMT
            // --------------------------------------------------------------------
            try {
                svLogger.info("testContextMethods: Calling setRollbackOnly()");
                ivContext.setRollbackOnly();
                Assert.fail("12 --> setRollbackOnly should have failed!");
            } catch (IllegalStateException ise) {
                svLogger.log(Level.INFO, "testContextMethods() --> Caught expected exception", ise);
            }

            // --------------------------------------------------------------------
            // 13 - UserTransaction.commit() works                              BMT
            // --------------------------------------------------------------------
            svLogger.info("testContextMethods: Calling UserTran.commit()");
            userTran.commit();
            Assert.assertEquals("13 --> Transaction did not commit successfully",
                                userTran.getStatus(), Status.STATUS_NO_TRANSACTION);
        }
    }

    private Object testContextMethodsIndirect(String txType) {
        try {
            testContextMethods(txType);
            return true;
        } catch (Throwable t) {
            svLogger.log(Level.SEVERE, "test failure", t);
            return t;
        }
    }

    /**
     * Test TimerService.createTimer() IllegalArgumentExceptions from
     * a method on a Stateless Session bean that implements the TimedObject
     * interface. <p>
     *
     * This test method will confirm an IllegalArgumentException for the following :
     * <ol>
     * <li> TimerService.createTimer(duration, null)
     * - where duration is negative
     * <li> TimerService.createTimer(duration, interval, info)
     * - where duration is negative
     * <li> TimerService.createTimer(duration, interval, info)
     * - where interval is negative
     * <li> TimerService.createTimer(date, info)
     * - where date is null
     * <li> TimerService.createTimer(date, info)
     * - where date.getTime() is negative
     * <li> TimerService.createTimer(date, interval, info)
     * - where date is null
     * <li> TimerService.createTimer(date, interval, info)
     * - where date.getTime() is negative
     * <li> TimerService.createTimer(date, interval, info)
     * - where interval is negative
     * </ol>
     */
    public void testCreateTimerExceptions() {
        Timer timer = null;

        // -----------------------------------------------------------------------
        // 1 - Verify TimerService.createTimer(duration, info)
        //     where duration is negative
        // -----------------------------------------------------------------------
        try {
            svLogger.info("ejbCreate: Calling TimerService.createTimer(-1)");
            timer = ivTimerService.createTimer(-1, "IllegalArgumentException");
            Assert.fail("1 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalArgumentException iae) {
            svLogger.log(Level.INFO, "1 ---> Caught expected exception", iae);
        }

        // -----------------------------------------------------------------------
        // 2 - Verify TimerService.createTimer(duration, interval, info)
        //     where duration is negative
        // -----------------------------------------------------------------------
        try {
            svLogger.info("ejbCreate: Calling TimerService.createTimer(-1,1000)");
            timer = ivTimerService.createTimer(-1, 1000, "IllegalArgumentException");
            Assert.fail("2 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalArgumentException iae) {
            svLogger.log(Level.INFO, "2 ---> Caught expected exception", iae);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify TimerService.createTimer(duration, interval, info)
        //     where interval is negative
        // -----------------------------------------------------------------------
        try {
            svLogger.info("ejbCreate: Calling TimerService.createTimer(1000,-1)");
            timer = ivTimerService.createTimer(1000, -1, "IllegalArgumentException");
            Assert.fail("3 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalArgumentException iae) {
            svLogger.log(Level.INFO, "3 ---> Caught expected exception", iae);
        }

        // -----------------------------------------------------------------------
        // 4 - Verify TimerService.createTimer(date, info)
        //     where date is null
        // -----------------------------------------------------------------------
        try {
            svLogger.info("ejbCreate: Calling TimerService.createTimer(null)");
            timer = ivTimerService.createTimer((java.util.Date) null, "IllegalArgumentException");
            Assert.fail("4 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalArgumentException iae) {
            svLogger.log(Level.INFO, "4 ---> Caught expected exception", iae);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify TimerService.createTimer(date, info)
        //     where date.getTime() is negative
        // -----------------------------------------------------------------------
        try {
            svLogger.info("ejbCreate: Calling TimerService.createTimer(-1 time)");
            Date expiration = new Date(-1);
            timer = ivTimerService.createTimer(expiration, "IllegalArgumentException");
            Assert.fail("5 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalArgumentException iae) {
            svLogger.log(Level.INFO, "5 ---> Caught expected exception", iae);
        }

        // -----------------------------------------------------------------------
        // 6 - Verify TimerService.createTimer(date, interval, info)
        //     where date is null
        // -----------------------------------------------------------------------
        try {
            svLogger.info("ejbCreate: Calling TimerService.createTimer(null,1000)");
            timer = ivTimerService.createTimer(null, 1000, "IllegalArgumentException");
            Assert.fail("6 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalArgumentException iae) {
            svLogger.log(Level.INFO, "6 ---> Caught expected exception", iae);
        }

        // -----------------------------------------------------------------------
        // 7 - Verify TimerService.createTimer(date, interval, info)
        //     where date.getTime() is negative
        // -----------------------------------------------------------------------
        try {
            svLogger.info("ejbCreate: Calling TimerService.createTimer" +
                          "(-1 time, 1000)");
            Date expiration = new Date(-1);
            timer = ivTimerService.createTimer(expiration, 1000,
                                               "IllegalArgumentException");
            Assert.fail("7 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalArgumentException iae) {
            svLogger.log(Level.INFO, "7 ---> Caught expected exception", iae);
        }

        // -----------------------------------------------------------------------
        // 8 - Verify TimerService.createTimer(date, interval, info)
        //     where interval is negative
        // -----------------------------------------------------------------------
        try {
            svLogger.info("ejbCreate: Calling TimerService.createTimer" +
                          "(1000 time,-1)");
            Date expiration = new Date(1000);
            timer = ivTimerService.createTimer(expiration, -1,
                                               "IllegalArgumentException");
            Assert.fail("8 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalArgumentException iae) {
            svLogger.log(Level.INFO, "8 ---> Caught expected exception", iae);
        }
    }

    /**
     * Utility method that may be used to create a Timer when a Timer is
     * required to perform a test, but cannot be created directly by
     * the bean performing the test. For example, if the bean performing
     * the test does not implement the TimedObject interface. <p>
     *
     * Local interface only! <p>
     *
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with 1 minutie duration and specified info.
     **/
    public Timer createTimer(Serializable info) {
        Timer timer = ivTimerService.createTimer(DEFAULT_EXPIRATION, info);
        return timer;
    }

    /**
     * Utility method to create a Timer remotely. This method is for use
     * by tests that are testing the expiration of a Timer and execution
     * of the ejbTimeout method. <p>
     *
     * Also clears the results from any previous ejbTimeout tests. <p>
     *
     * @param duration duration of the Timer to create
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with the duration and info specified.
     **/
    public CountDownLatch createTimer(long duration, Serializable info) {
        svEjbTimeoutResults = null;
        testOpsInTimeoutLatch = new CountDownLatch(1);
        @SuppressWarnings("unused")
        Timer timer = ivTimerService.createTimer(duration, info);
        return testOpsInTimeoutLatch;
    }

    private static void waitForTimers(CountDownLatch latch, long maxWaitTime) {
        try {
            svLogger.info("Waiting up to " + maxWaitTime + "ms for timers to fire...");
            if (latch.await(maxWaitTime, TimeUnit.MILLISECONDS)) {
                svLogger.info("Timers fired; waiting for timeout postInvoke to complete");
                FATHelper.sleep(600); // wait for timer method postInvoke to complete
            }
        } catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }
    }

    private void clearAllTimers() {
        Collection<Timer> timers = ivTimerService.getTimers();
        for (Timer timer : timers) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException nso) {
                svLogger.info("attempted to cancel timer that no longer exists : " + timer);
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

}
