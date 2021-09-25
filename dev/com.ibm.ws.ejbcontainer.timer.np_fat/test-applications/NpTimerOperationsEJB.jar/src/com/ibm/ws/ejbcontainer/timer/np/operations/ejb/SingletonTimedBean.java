/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.operations.ejb;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.xml.rpc.handler.MessageContext;

import org.junit.Assert;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

/**
 * Bean implementation for a basic Stateless Session bean that implements
 * a timeout callback method. It contains methods to test TimerService
 * access. <p>
 **/
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SingletonTimedBean implements SingletonTimedLocal {
    private static final Logger logger = Logger.getLogger(SingletonTimedBean.class.getName());

    private static Timer timer[] = null;
    private static int timeoutCounts[] = null;
    private static CountDownLatch timerLatch = new CountDownLatch(1);
    private static CountDownLatch timerIntervalLatch = new CountDownLatch(1);
    private static CountDownLatch timerIntervalWaitLatch = new CountDownLatch(1);
    private static CountDownLatch testOpsInTimeoutLatch = new CountDownLatch(1);

    // The time for the container to run post invoke processing for @Timeout.
    // Should be used after a Timer has triggered a CountDownLatch to insure
    // the @Timeout method, including the transaction, has completed and thus
    // updated (or even removed) the timer.
    private static final long POST_INVOKE_DELAY = 700;

    protected SessionContext context;
    private TimerService timerService;

    // These fields hold the test results for EJB callback methods
    private Object setSessionContextResults;
    private Object ejbCreateResults;
    private Object ejbTimeoutResults;

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Stateless Session bean that implements a timeout callback method. <p>
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
    @Resource
    public void setSessionContext(SessionContext sc) {

        context = sc;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("setSessionContext: Calling getTimerService()");
            context.getTimerService();
            Assert.fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            logger.info("1 ---> Caught expected exception : " + ise);
            setSessionContextResults = true;
        } catch (Throwable th) {
            logger.info("1 ---> Unexpected exception from getTimerService() : " + th);
            th.printStackTrace(System.out);
            setSessionContextResults = th;
        }
    }

    private void verifyResults(Object results) {
        if (results instanceof Throwable) {
            throw new Error((Throwable) results);
        }
        Assert.assertEquals(true, results);
    }

    /**
     * Verifies the results of testing performed in {@link #setSessionContext setSessionContext()}. <p>
     *
     * Since setSessionContext may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     */
    @Override
    public void verifySetSessionContextResults() {
        verifyResults(setSessionContextResults);
    }

    /**
     * Test getTimerService()/TimerService access from PostConstruct on a
     * Stateless Session bean that implements a timeout callback method. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a Stateless bean, the results will be stored
     * in an instance variable. The test may then grab any instance (all
     * the same) and extract the results ({@link #getPostConstructResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createSingleActionTimer with null info works
     * <li> TimerService.createSingleActionTimer with info works
     * <li> TimerService.createIntervalTimer with info works
     * <li> TimerService.createSingleActionTimer with date and info works
     * <li> TimerService.createIntervalTimer with date and info works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> TimerService.getTimers() returns all created Timers
     * <li> TimerService.getAllTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * </ol> <p>
     */
    @PostConstruct
    public void ejbCreate() throws CreateException {
        try {
            timerService = context.getTimerService();
            testTimerServicePhase1();
            ejbCreateResults = Boolean.TRUE;
        } catch (Throwable ex) {
            ejbCreateResults = ex;
        } finally {
            clearAllTimers();
        }
    }

    /**
     * Verifies the results of testing performed in {@link #ejbCreate}. <p>
     *
     * Since PostConstruct may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     */
    @Override
    public void verifyPostConstructResults() {
        verifyResults(ejbCreateResults);
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements a timeout callback method. <p>
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
     * <li> TimerService.createSingleActionTimer with null info works
     * <li> TimerService.createSingleActionTimer with info works
     * <li> TimerService.createIntervalTimer with info works
     * <li> TimerService.createSingleActionTimer with date and info works
     * <li> TimerService.createIntervalTimer with date and info works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> TimerService.getTimers() returns all created Timers
     * <li> TimerService.getAllTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * </ol> <p>
     */
    @Override
    public void testTimerServicePhase1() {

        timer = new Timer[6];
        timeoutCounts = new int[timer.length];
        timerLatch = new CountDownLatch(timer.length - 1); // one timer cancelled
        timerIntervalWaitLatch = new CountDownLatch(1); // block interval timer until ready
        timerIntervalLatch = new CountDownLatch(2);

        // -------------------------------------------------------------------
        // 1 - Verify getTimerService() returns a valid TimerService
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling getTimerService()");
        TimerService ts = context.getTimerService();
        Assert.assertNotNull("1 ---> Got TimerService", ts);

        // -------------------------------------------------------------------
        // 2 - Verify TimerService.createSingleActionTimer with null info works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling TimerService.createSingleActionTimer with null info");
        TimerConfig timerConfig = new TimerConfig(null, false);
        timer[0] = ts.createSingleActionTimer(EXPIRATION, timerConfig);
        Assert.assertNotNull("2 ---> TimerService.createSingleActionTimer returned null", timer[0]);

        // -------------------------------------------------------------------
        // 3 - Verify TimerService.createSingleActionTimer with info works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling TimerService.createSingleActionTimer with info");
        timerConfig = new TimerConfig(new Integer(1), false);
        timer[1] = ts.createSingleActionTimer(EXPIRATION, timerConfig);
        Assert.assertNotNull("3 ---> TimerService.createSingleActionTimer returned null", timer[1]);

        // -------------------------------------------------------------------
        // 4 - Verify TimerService.createIntervalTimer with info works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling TimerService.createIntervalTimer with info");
        timerConfig = new TimerConfig(new Integer(2), false);
        timer[2] = ts.createIntervalTimer(EXPIRATION, INTERVAL, timerConfig);
        Assert.assertNotNull("4 ---> TimerService.createIntervalTimer returned null", timer[2]);

        // -------------------------------------------------------------------
        // 5 - Verify TimerService.createSingleActionTimer with date and info works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling TimerService.createSingleActionTimer with date and info");
        timerConfig = new TimerConfig(new Integer(3), false);
        Date expiration = new Date(System.currentTimeMillis() + EXPIRATION);
        timer[3] = ts.createSingleActionTimer(expiration, timerConfig);
        Assert.assertNotNull("5 ---> TimerService.createSingleActionTimer returned null", timer[3]);

        // -------------------------------------------------------------------
        // 6 - Verify TimerService.createIntervalTimer with date and info works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling TimerService.createIntervalTimer with date and info");
        timerConfig = new TimerConfig(new Integer(4), false);
        expiration = new Date(System.currentTimeMillis() + EXPIRATION);
        timer[4] = ts.createIntervalTimer(expiration, INTERVAL, timerConfig);
        Assert.assertNotNull("6 ---> TimerService.createIntervalTimer returned null", timer[4]);

        // -------------------------------------------------------------------
        // 7 - Verify Timer.getTimeRemaining() on single event Timer works
        // -------------------------------------------------------------------
        // Create an extra timer for testing now, and testing cancel later...
        logger.info("testTimerService: Calling TimerService.createSingleActionTimer with info");
        timerConfig = new TimerConfig(new Integer(5), false);
        timer[5] = ts.createSingleActionTimer(EXPIRATION, timerConfig);
        logger.info("testTimerService: Calling Timer.getTimeRemaining()");
        long remaining = timer[5].getTimeRemaining();
        Assert.assertTrue("7 ---> Timer.getTimeRemaining() failed: " + remaining,
                          remaining >= 1 && remaining <= EXPIRATION);

        // -------------------------------------------------------------------
        // 8 - Verify Timer.getTimeRemaining() on repeating Timer works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.getTimeRemaining()");
        remaining = timer[2].getTimeRemaining();
        Assert.assertTrue("8 ---> Timer.getTimeRemaining() failed: " + remaining,
                          remaining >= 1 && remaining <= EXPIRATION);

        // -------------------------------------------------------------------
        // 9 - Verify Timer.getInfo() returning null works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.getInfo()");
        Object timerInfo = timer[0].getInfo();
        Assert.assertNull("9 ---> Timer.getInfo() failed: " + timerInfo,
                          timerInfo);

        // -------------------------------------------------------------------
        // 10 - Verify Timer.getInfo() returning serializable works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.getInfo()");
        timerInfo = timer[1].getInfo();
        Assert.assertEquals("10 --> Timer.getInfo() failed: " + timerInfo,
                            new Integer(1), timerInfo);

        // -------------------------------------------------------------------
        // 11 - Verify Timer.getHandle() fails with IllegalStateException
        // -------------------------------------------------------------------
        try {
            logger.info("testTimerService: Calling Timer.getHandle()");
            TimerHandle timerHandle = timer[5].getHandle();
            Assert.fail("11 --> Timer.getHandle() worked: " + timerHandle);
        } catch (IllegalStateException ex) {
            logger.info("11 --> Caught expected exception on getHandle(): " + ex);
        }

        // -------------------------------------------------------------------
        // 12 - Verify TimerService.getTimers() returns all created Timers
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling getTimers()");
        Collection<Timer> timers = ts.getTimers();

        // Print out the results for debug purposes...
        Object[] timersArray = timers.toArray();
        for (int i = 0; i < timersArray.length; i++) {
            logger.info("  returned : " + timersArray[i]);
        }

        Assert.assertEquals("12 --> getTimers returned 6 Timers", 6, timers.size());

        // Make sure they are the correct timers...
        for (int i = 0; i < timer.length; i++) {
            if (!timers.contains(timer[i]))
                Assert.fail("Timer[" + i + "] not returned: " + timer[i]);
        }

        // -------------------------------------------------------------------
        // 13 - Verify TimerService.getAllTimers() returns all created Timers
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling getAllTimers()");
        timers = ts.getAllTimers();

        // Print out the results for debug purposes...
        timersArray = timers.toArray();
        for (int i = 0; i < timersArray.length; i++) {
            logger.info("  returned : " + timersArray[i]);
        }

        Assert.assertEquals("13 --> getTimers returned 6 Timers", 6, timers.size());

        // Make sure they are the correct timers...
        for (int i = 0; i < timer.length; i++) {
            if (!timers.contains(timer[i]))
                Assert.fail("Timer[" + i + "] not returned: " + timer[i]);
        }

        // -------------------------------------------------------------------
        // 14 - Verify Timer.cancel() works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.cancel()");
        timer[5].cancel();
        logger.info("15 --> Timer.cancel() worked");

        // -------------------------------------------------------------------
        // 15 - Verify NoSuchObjectLocalException occurs accessing canceled
        //      timer
        // -------------------------------------------------------------------
        try {
            logger.info("testTimerService: Calling Timer.getInfo() " +
                        "on cancelled Timer");
            timer[5].getInfo();
            Assert.fail("15 --> Timer.getInfo() worked - " +
                        "expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            logger.info("15 --> Caught expected exception : " + nso);
        }

        // -------------------------------------------------------------------
        // 16 - Verify TimerService.getTimers() does not return cancelled Timers
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling getTimers()");
        timers = ts.getTimers();

        // Print out the results for debug purposes...
        timersArray = timers.toArray();
        for (int i = 0; i < timersArray.length; i++) {
            logger.info("  returned : " + timersArray[i]);
        }

        Assert.assertEquals("17 --> getTimers returned 5 Timers", 5, timers.size());

        // Make sure they are the correct timers...
        for (int i = 0; i < timer.length; i++) {
            switch (i) {
                case 5:
                    if (timers.contains(timer[i]))
                        Assert.fail("Timer[" + i + "] returned: " + timer[i]);
                    break;

                default:
                    if (!timers.contains(timer[i]))
                        Assert.fail("Timer[" + i + "] not returned: " + timer[i]);
                    break;
            }
        }
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements a timeout callback method. <p>
     *
     * This test is executed in 2 phases. The first phase will create
     * multiple Timers, and the second phase will confirm that they are
     * executed correctly. This cannot all be done in one method call,
     * as the Timers will not execute until after their creation has
     * been committed (at the end of phase 1 - and the return from the
     * method call). <p>
     *
     * This test method will confirm the following in Phase 2 :
     * <ol start=17>
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
    @Override
    public void testTimerServicePhase2() {

        // -------------------------------------------------------------------
        // Wait up to MAX_TIMER_WAIT for all of the timers to expire
        // -------------------------------------------------------------------
        waitForTimers(timerLatch, MAX_TIMER_WAIT);

        // -------------------------------------------------------------------
        // 17 - Verify ejbTimeout is executed for valid Timers
        // -------------------------------------------------------------------
        boolean successful = true;

        // See if all except cancelled timers have expired once...
        for (int i = 0; i < timer.length; i++) {
            switch (i) {
                case 5:
                    if (timeoutCounts[i] != 0) {
                        successful = false;
                        logger.info("Cancelled Timer[" + i + "] executed: " +
                                    timer[i]);
                    }
                    break;

                default:
                    if (timeoutCounts[i] != 1) {
                        successful = false;
                        logger.info("Timer[" + i + "] not executed once: " +
                                    timer[i]);
                    }
                    break;
            }
        }

        Assert.assertTrue("17 --> ejbTimeout executed on 5 Timers",
                          successful);

        // -------------------------------------------------------------------
        // 18 - Verify NoSuchObjectLocalException occurs accessing expired timer
        // -------------------------------------------------------------------
        try {
            logger.info("testTimerService: Calling Timer.getInfo() " +
                        "on expired Timer");
            timer[1].getInfo();
            Assert.fail("18 --> Timer.getInfo() worked - " +
                        "expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            logger.info("18 --> Caught expected exception : " + nso);
        }

        // -------------------------------------------------------------------
        // 19 - Verify TimerService.getTimers() does not return expired Timers
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling getTimerService()");
        TimerService ts = context.getTimerService();
        logger.info("testTimerService: Calling getTimers()");
        Collection<Timer> timers = ts.getTimers();

        // Print out the results for debug purposes...
        Object[] timersArray = timers.toArray();
        for (int i = 0; i < timersArray.length; i++) {
            logger.info("  returned : " + timersArray[i]);
        }

        Assert.assertEquals("19 --> getTimers returned 2 Timers", 2, timers.size());

        // Make sure they are the correct timers...
        for (int i = 0; i < timer.length; i++) {
            switch (i) {
                case 0:
                case 1:
                case 3:
                case 5:
                    if (timers.contains(timer[i]))
                        Assert.fail("Timer[" + i + "] returned: " + timer[i]);
                    break;

                default:
                    if (!timers.contains(timer[i]))
                        Assert.fail("Timer[" + i + "] not returned: " + timer[i]);
                    break;
            }
        }

        // -------------------------------------------------------------------
        // 20 - Verify Timer.getNextTimeout() on repeating Timer works
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling Timer.getNextTimeout()");
        Date nextTime = timer[2].getNextTimeout();
        long remaining = nextTime.getTime() - System.currentTimeMillis();
        Assert.assertTrue("20 --> Timer.getNextTimeout() worked: " + remaining,
                          remaining >= (1 - TIMER_PRECISION) && remaining <= (INTERVAL + TIMER_PRECISION));

        // -------------------------------------------------------------------
        // Wait up to MAX_TIMER_WAIT for interval timers to expire
        // -------------------------------------------------------------------
        timerIntervalWaitLatch.countDown(); // allow 2nd interval to run
        waitForTimers(timerIntervalLatch, MAX_TIMER_WAIT);

        // -------------------------------------------------------------------
        // 21 - Verify ejbTimeout is executed multiple times for repeating Timers
        // -------------------------------------------------------------------
        successful = true;

        // See if all except cancelled timers have expired once...
        // and repeating timers have executed twice...
        for (int i = 0; i < timer.length; i++) {
            switch (i) {
                case 2:
                case 4:
                    if (timeoutCounts[i] != 2) {
                        successful = false;
                        logger.info("Timer[" + i + "] not executed twice: " +
                                    timer[i]);
                    }
                    break;

                case 5:
                    if (timeoutCounts[i] != 0) {
                        successful = false;
                        logger.info("Cancelled Timer[" + i + "] executed: " +
                                    timer[i]);
                    }
                    break;

                default:
                    if (timeoutCounts[i] != 1) {
                        successful = false;
                        logger.info("Timer[" + i + "] not executed once: " +
                                    timer[i]);
                    }
                    break;
            }
        }

        Assert.assertTrue("21 --> ejbTimeout executed on 2 Timers",
                          successful);

        // -------------------------------------------------------------------
        // 22 - Verify NoSuchObjectLocalException occurs accessing self
        //      cancelled timer
        // -------------------------------------------------------------------
        try {
            logger.info("testTimerService: Calling Timer.getInfo() " +
                        "on self cancelled Timer");
            timer[2].getInfo();
            Assert.fail("22 --> Timer.getInfo() worked - " +
                        "expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            logger.info("22 --> Caught expected exception : " + nso);
        }

        // -------------------------------------------------------------------
        // 23 - Verify TimerService.getTimers() returns empty collection after
        //      all Timers have expired or been cancelled.
        // -------------------------------------------------------------------
        logger.info("testTimerService: Calling getTimerService()");
        ts = context.getTimerService();
        logger.info("testTimerService: Calling getTimers()");
        timers = ts.getTimers();

        // Print out the results for debug purposes...
        timersArray = timers.toArray();
        for (int i = 0; i < timersArray.length; i++) {
            logger.info("  returned : " + timersArray[i]);
        }

        Assert.assertEquals("23 --> getTimers returned 0 Timers", 0, timers.size());
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Stateless
     * Session bean that implements a timeout callback method. <p>
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
     * <li> Timer.getHandle() fails with IllegalStateException
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
    @Timeout
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

        if (timerIndex >= 0) {

            if (timeoutCounts[timerIndex] == 1) {
                // Don't run the 2nd interval until test is ready
                try {
                    timerIntervalWaitLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                }
            }

            timeoutCounts[timerIndex]++;

            System.out.println("Timer " + timerIndex + " expired " +
                               timeoutCounts[timerIndex] + " time(s)");

            if (timeoutCounts[timerIndex] == 1) {
                timerLatch.countDown();
            } else if (timeoutCounts[timerIndex] > 1) {
                timer.cancel();
                timerIntervalLatch.countDown();
            }

            return;
        }

        // For debug, just print out that the timer expired, and the name of the
        // test it will execute.
        System.out.println("Timer expired: " + info);

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
                    ejbTimeoutResults = true;
                    testOpsInTimeoutLatch.countDown();
                } catch (Throwable t) {
                    logger.info("test failure : " + t);
                    t.printStackTrace(System.out);
                    ejbTimeoutResults = t;
                    testOpsInTimeoutLatch.countDown();
                }
            }

            // --------------------------------------------------------------------
            // Test SessionContext method access for CMT bean.....
            // --------------------------------------------------------------------

            else if (test.equals("testContextMethods-CMT")) {
                if (ejbTimeoutResults != null) {
                    logger.info("13 --> Timer not cancelled: ejbTimeout executed 2nd time");
                    testOpsInTimeoutLatch.countDown();
                    return;
                }

                ejbTimeoutResults = testContextMethodsIndirect("CMT");
            }

            // --------------------------------------------------------------------
            // Test SessionContext method access for BMT bean.....
            // --------------------------------------------------------------------

            else if (test.equals("testContextMethods-BMT")) {
                ejbTimeoutResults = testContextMethodsIndirect("BMT");
                testOpsInTimeoutLatch.countDown();
            }

        }

        // -----------------------------------------------------------------------
        // When the 'info' object is something other than the String
        // "testTimerService", then the Timer was being used for a test that just
        // needed a Timer, and it has probably gone off accidentally.
        // -----------------------------------------------------------------------
    }

    /**
     * Verifies the results of testing performed in {@link #ejbTimeout ejbTimeout()}. <p>
     *
     * Since ejbTimeout may not be called directly, the results are
     * stored in a static variable for later verification. <p>
     */
    @Override
    public void verifyEjbTimeoutResults() {
        verifyResults(ejbTimeoutResults);
    }

    /**
     * Test SessionContext method access from a method on a Stateless
     * Session bean that implements a timeout callback method. <p>
     *
     * This test method will confirm the following:
     * <ol>
     * <li> SessionContext.getEJBObject() fails with IllegalStateException
     * <li> SessionContext.getEJBLocalObject() fails with IllegalStateException
     * <li> SessionContext.getMessageContext() fails with IllegalStateException
     * <li> EJBContext.getEJBHome() fails with IllegalStateException
     * <li> EJBContext.getEJBLocalHome() fails with IllegalStateException
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * </ol>
     *
     * And the following for CMT:
     * <ol start=9>
     * <li> EJBContext.getUserTransaction() fails with IllegalStateException
     * <li> EJBContext.getRollbackOnly() - false works
     * <li> EJBContext.setRollbackOnly() works
     * <li> EJBContext.getRollbackOnly() - true works
     * <li> ejbTimeout is executed again when setRollbackOnly called.
     * </ol> <p>
     *
     * And the following for BMT:
     * <ol start=9>
     * <li> EJBContext.getUserTransaction() works
     * <li> UserTransaction.begin() works
     * <li> EJBContext.getRollbackOnly() fails
     * <li> EJBContext.setRollbackOnly() fails
     * <li> UserTransaction.commit() works
     * </ol>
     */
    public void testContextMethods(String txType) {

        // -----------------------------------------------------------------------
        // 1 - Verify SessionContext.getEJBObject() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testContextMethods: Calling getEJBObject()");
            EJBObject ejb = context.getEJBObject();
            Assert.fail("1 ---> Got EJBObject : " + ejb);
        } catch (IllegalStateException ex) {
            logger.info("1 ---> Caught expected exception from getEJBObject(): " + ex);
        }

        // -----------------------------------------------------------------------
        // 2 - Verify SessionContext.getEJBLocalObject() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testContextMethods: Calling getEJBLocalObject()");
            EJBLocalObject ejblocal = context.getEJBLocalObject();
            Assert.fail("2 ---> Got EJBLocalObject : " + ejblocal);
        } catch (IllegalStateException ex) {
            logger.info("1 ---> Caught expected exception from getEJBLocalObject(): " + ex);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify SessionContext.getMessageContext() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testContextMethods: Calling getMessageContext()");
            MessageContext mc = context.getMessageContext();
            Assert.fail("3 ---> getMessageContext should have failed! " + mc);
        } catch (IllegalStateException ise) {
            logger.info("3 ---> Caught expected exception from getMessageContext : " + ise);
        } catch (NoSuchMethodError nsm) {
            if (nsm.getMessage().contains("jakarta")) {
                logger.info("3 ---> Caught expected exception from getMessageContext : " + nsm);
            } else {
                Assert.fail("3 ---> Caught unexpected exception from getMessageContext : " + nsm);
            }
        }

        // -----------------------------------------------------------------------
        // 4 - Verify EJBContext.getEJBHome()  fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testContextMethods: Calling getEJBHome()");
            EJBHome ejbHome = context.getEJBHome();
            Assert.fail("4 ---> Got EJBHome : " + ejbHome);
        } catch (IllegalStateException ex) {
            logger.info("1 ---> Caught expected exception from getEJBHome(): " + ex);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify EJBContext.getEJBLocalHome()  fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testContextMethods: Calling getEJBLocalHome()");
            EJBLocalHome ejbLocalHome = context.getEJBLocalHome();
            Assert.fail("5 ---> Got EJBLocalHome : " + ejbLocalHome);
        } catch (IllegalStateException ex) {
            logger.info("1 ---> Caught expected exception from getEJBLocalHome(): " + ex);
        }

        // -----------------------------------------------------------------------
        // 6 - Verify EJBContext.getCallerPrincipal() works
        // -----------------------------------------------------------------------
        logger.info("testContextMethods: Calling getCallerPrincipal()");
        Principal principal = context.getCallerPrincipal();
        Assert.assertNotNull("6 ---> Got CallerPrincipal", principal);

        // -----------------------------------------------------------------------
        // 7 - Verify EJBContext.isCallerInRole() works
        // -----------------------------------------------------------------------
        logger.info("testContextMethods: Calling isCallerInRole()");
        boolean inRole = context.isCallerInRole("test");
        // EJB 3.0 and later returns true when security is not enabled
        Assert.assertTrue("7 ---> isCallerInRole returned false", inRole);

        // -----------------------------------------------------------------------
        // 8 - Verify EJBContext.getTimerService() works
        // -----------------------------------------------------------------------
        logger.info("testContextMethods: Calling getTimerService()");
        TimerService ts = context.getTimerService();
        Assert.assertNotNull("8 ---> Got TimerService", ts);

        // -----------------------------------------------------------------------
        // 9 -> 13 - varies based on transaction management type
        // -----------------------------------------------------------------------
        testTransactionalContextMethods(txType);
    }

    private Object testContextMethodsIndirect(String txType) {

        try {
            testContextMethods(txType);
            return true;
        } catch (Throwable t) {
            logger.info("test failure : " + t);
            t.printStackTrace(System.out);
            return t;
        }
    }

    /**
     * Test SessionContext transactional method access from a method
     * on a Stateless Session bean that implements a timeout callback
     * method. <p>
     *
     * This test method will confirm the following for CMT:
     * <ol>
     * <li> EJBContext.getUserTransaction() fails with IllegalStateException
     * <li> EJBContext.getRollbackOnly() - false works
     * <li> EJBContext.setRollbackOnly() works
     * <li> EJBContext.getRollbackOnly() - true works
     * <li> ejbTimeout is executed again when setRollbackOnly called.
     * </ol>
     */
    protected void testTransactionalContextMethods(String txType) {

        if (!"CMT".equals(txType)) {
            throw new EJBException("Requested TransactionManagement type : " + txType + ", bean type : CMT");
        }

        // -----------------------------------------------------------------------
        // 9 - Verify EJBContext.getUserTransaction()
        //     CMT - fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            logger.info("testContextMethods: Calling getUserTransaction()");
            context.getUserTransaction();
            Assert.fail("9 ---> getUserTransaction should have failed!");
        } catch (IllegalStateException ise) {
            logger.info("9 ---> Caught expected exception : " + ise);
        }

        // --------------------------------------------------------------------
        // 10 - Verify EJBContext.getRollbackOnly() - false works           CMT
        // --------------------------------------------------------------------
        logger.info("testContextMethods: Calling getRollbackOnly()");
        boolean rollback = context.getRollbackOnly();
        Assert.assertFalse("10 --> Got RollbackOnly", rollback);

        // --------------------------------------------------------------------
        // 11 - Verify EJBContext.setRollbackOnly() works                   CMT
        // --------------------------------------------------------------------
        logger.info("testContextMethods: Calling setRollbackOnly()");
        context.setRollbackOnly();
        logger.info("11 --> setRollbackOnly worked");

        // --------------------------------------------------------------------
        // 12 - Verify EJBContext.getRollbackOnly() - true works            CMT
        // --------------------------------------------------------------------
        logger.info("testContextMethods: Calling getRollbackOnly()");
        rollback = context.getRollbackOnly();
        Assert.assertTrue("12 --> Got RollbackOnly", rollback);

        // --------------------------------------------------------------------
        // 13 - Verify ejbTimeout is executed again when setRollbackOnly
        //      called.                                                     CMT
        // --------------------------------------------------------------------
        // See ejbTimeout - this must be checked when ejbTimeout is called a
        //                  second time!
    }

    /**
     * Utility method to create a non-persistent Timer. This method is for use
     * by tests that are testing the expiration of a Timer and execution
     * of the ejbTimeout method. <p>
     *
     * Also clears the results from any previous ejbTimeout tests. <p>
     *
     * @param duration duration of the Timer to create
     * @param info info parameter passed through to the createTimer call
     *
     * @return CountDownLatch that can be used wait for the timer to run.
     **/
    @Override
    public CountDownLatch createTimer(long duration, Serializable info) {
        ejbTimeoutResults = null;
        testOpsInTimeoutLatch = new CountDownLatch(1);
        TimerConfig timerConfig = new TimerConfig(info, false);
        Timer timer = timerService.createSingleActionTimer(duration, timerConfig);
        logger.info("Created timer = " + timer);
        return testOpsInTimeoutLatch;
    }

    private static void waitForTimers(CountDownLatch latch, long maxWaitTime) {
        try {
            logger.info("Waiting up to " + maxWaitTime + "ms for timers to fire...");
            if (latch.await(maxWaitTime, TimeUnit.MILLISECONDS)) {
                logger.info("Timers fired; waiting for timeout postInvoke to complete");
                FATHelper.sleep(POST_INVOKE_DELAY); // wait for timer method postInvoke to complete
            }
        } catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }
    }

    @Override
    public void clearAllTimers() {
        Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException nso) {
                logger.info("attempted to cancel timer that no longer exists : " + timer);
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

}
