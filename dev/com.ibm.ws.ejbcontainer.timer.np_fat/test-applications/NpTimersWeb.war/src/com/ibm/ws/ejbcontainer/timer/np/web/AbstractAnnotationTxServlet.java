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
package com.ibm.ws.ejbcontainer.timer.np.web;

import static com.ibm.ws.ejbcontainer.timer.np.ejb.AbstractAnnotationTxBean.svExpiredTimerInfos;
import static com.ibm.ws.ejbcontainer.timer.np.ejb.AnnotationTxLocal.DEFAULT_INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.Timer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.ejb.AnnotationTxLocal;
import com.ibm.ws.ejbcontainer.timer.np.ejb.AnnotationTxLocal.TstName;
import com.ibm.ws.ejbcontainer.timer.np.ejb.SimpleTimerLocal;

/**
 * This test case class contains tests that verify the proper behavior of
 * non-persistent timers defined for a stateless session bean using only
 * annotations - no XML, not implementing TimedObject.
 */
@SuppressWarnings("serial")
public abstract class AbstractAnnotationTxServlet extends AbstractServlet {

    private final static String CLASSNAME = AbstractAnnotationTxServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    protected AnnotationTxLocal ivBean;

    /**
     * This test creates a timer and then cancels it in the same CMT method -
     * thus the create and cancel occur in the same transaction.
     * <br/>
     * The expected outcome is that the timer has been canceled, an the timer
     * service would not contain any active timers.
     */
    @Test
    public void testCreateAndCancelInSameTx() {
        try {
            ivBean.executeTest(TstName.TEST_CREATE_AND_CANCEL_IN_SAME_METHOD);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        assertEquals("One or more Timers exist - expected none", 0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * This test creates two timers in the same CMT method - thus in the same
     * transaction.
     * The expected result is that the timer service should show two active
     * timers and that after the duration of both timers, the timer's timeout
     * method should have been invoked twice.
     */
    @Test
    public void testCreateTwoTimersInSameTx() {
        try {
            doTestCreateTwoTimersInSameMethod(TstName.TEST_CREATE_TWO_TIMERS_IN_SAME_METHOD);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
    }

    /**
     * This test creates a timer in a non-transactional bean method. It then
     * cancels the timer (before expiration) in another non-transactional
     * method.
     * <br/>
     * The expected outcome is that the timer service shows one active timer
     * before it has been canceled. After cancellation, the timer service
     * should show no active timers.
     */
    @Test
    public void testCreateAndCancelBeforeExpirationNoTx() {
        try {
            svExpiredTimerInfos.clear();
            ivBean.executeTestNoTx(TstName.TEST_CREATE_TIMER);
            Collection<String> infos = ivBean.getInfoOfAllTimers();
            assertEquals("Unexpected number of timers", 1, infos.size());
            assertTrue("Did not contain expected timer info", infos.contains(DEFAULT_INFO));
            ivBean.executeTestNoTx(TstName.TEST_CANCEL_TIMER);
            infos = ivBean.getInfoOfAllTimers();
            assertEquals("Unexpected number of timers", 0, infos.size());
            assertEquals("Unexpected timer expiration : " + svExpiredTimerInfos, 0, svExpiredTimerInfos.size());

            FATHelper.sleep(AnnotationTxLocal.DURATION + AnnotationTxLocal.BUFFER);

            assertEquals("Timeout method invoked after being canceled : " + svExpiredTimerInfos, 0, svExpiredTimerInfos.size());

        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
    }

    /**
     * This test creates a timer in a CMT transactional bean method. It then
     * cancels the timer (before expiration) in the same transaction. The bean
     * methods are CMT using Required transaction attribute, but both methods
     * are invoked in the same transaction because this test case uses a global
     * UserTransaction acquired from java:comp/UserTransaction. In this test,
     * the UserTransaction commits - thus committing both the timer create and
     * cancel.
     * <br/>
     * The expected outcome is that the timer service shows one active timer
     * before it has been canceled. After cancellation and transaction commit,
     * the timer service should show no active timers.
     */
    @Test
    public void testCreateAndCancelBeforeExpiration() {
        try {
            svExpiredTimerInfos.clear();
            //start user tran so that the create and cancel are in the same tx
            UserTransaction ut = null;
            try {
                ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            } catch (NamingException ex) {
                String msg = "NamingException caught while looking up java:comp/UserTransaction";
                svLogger.logp(Level.SEVERE, CLASSNAME, "testCreateAndCancelBeforeExpiration",
                              msg, ex);
                fail(msg + " - " + ex);
            }
            ut.begin();
            ivBean.executeTest(TstName.TEST_CREATE_TIMER);
            Collection<String> infos = ivBean.getInfoOfAllTimers();
            assertEquals("Unexpected number of timers", 1, infos.size());
            assertTrue("Did not contain expected timer info", infos.contains(DEFAULT_INFO));
            ivBean.executeTest(TstName.TEST_CANCEL_TIMER);
            ut.commit();
            infos = ivBean.getInfoOfAllTimers();
            assertEquals("Unexpected number of timers", 0, infos.size());
            assertEquals("Unexpected timer expiration : " + svExpiredTimerInfos, 0, svExpiredTimerInfos.size());

            FATHelper.sleep(AnnotationTxLocal.DURATION + AnnotationTxLocal.BUFFER);

            assertEquals("Timeout method invoked after being canceled : " + svExpiredTimerInfos, 0, svExpiredTimerInfos.size());

        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
            String msg = "Caught unexpected exception in testCreateAndCancelBeforeExpiration";
            svLogger.logp(Level.SEVERE, CLASSNAME, "testCreateAndCancelBeforeExpiration",
                          msg, t);
            fail(msg + t);

        }
    }

    /**
     * This test attempts to get a javax.ejb.TimerHandle to a newly created
     * non-persistent timer. Since the timer is non-persistent, it should
     * always throw an IllegalStateException.
     * <br/>
     * The expected outcome of this test is for the timer.getHandle() call
     * to throw an IllegalStateException (this occurs in the bean class) and
     * that the timer exists following this call.
     */
    @Test
    public void testGetHandle() {
        try {
            ivBean.executeTest(TstName.TEST_GET_HANDLE);
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }

        Collection<String> infos = ivBean.getInfoOfAllTimers();
        assertEquals("Timer was never created.", 1, infos.size());
        for (String info : infos) {
            assertEquals("Wrong timer was created.", AnnotationTxLocal.HANDLE_INFO, info);
        }
    }

    /**
     * This test creates a single action timer in a CMT transactional method,
     * which commits. Then it waits until the timer has timed out. Last, it
     * attempts to cancel the timer that has already expired - this call should
     * result in a NoSuchObjectLocalException.
     * <br/>
     * The expected outcome of this test should be that the timer was created,
     * and executed its timeout method correctly. The attempt to cancel the
     * already-expired timer should throw a NoSuchObjectLocalException.
     */
    @Test
    public void testCreateAndCancelAfterExpiration() {
        Collection<String> infos = null;
        svExpiredTimerInfos.clear();

        //create the timer
        try {
            ivBean.executeTest(TstName.TEST_CREATE_EXPIRED_TIMER);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        //verify timer was created:
        infos = ivBean.getInfoOfAllTimers();
        assertEquals("Timer was not created.", 1, infos.size());

        //sleep long enough for the timer to expire
        ivBean.waitForTimer(AnnotationTxLocal.MAX_WAIT_TIME);

        //verify that the timer's timeout method was invoked
        assertEquals("Timeout method for timer failed to execute exactly once : " + svExpiredTimerInfos, 1, svExpiredTimerInfos.size());
        assertTrue("Timeout method for timer failed to execute : " + svExpiredTimerInfos, svExpiredTimerInfos.contains(AnnotationTxLocal.EXPIRED_INFO));
        infos = ivBean.getInfoOfAllTimers();
        assertEquals("Timer still exists after expiration.", 0, infos.size());

        //attempt to cancel the already-expired timer
        try {
            ivBean.executeTest(TstName.TEST_CANCEL_EXPIRED_TIMER);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
    }

    /**
     * This test creates a single action timer in a non-transactional method.
     * Then it waits until the timer has timed out. Last, it attempts to cancel
     * the timer that has already expired - this call should result in a
     * NoSuchObjectLocalException.
     * <br/>
     * The expected outcome of this test should be that the timer was created,
     * and executed its timeout method correctly. The attempt to cancel the
     * already-expired timer should throw a NoSuchObjectLocalException.
     */
    @Test
    public void testCreateAndCancelAfterExpirationNoTx() {
        Collection<String> infos = null;
        svExpiredTimerInfos.clear();

        //create the timer
        try {
            ivBean.executeTestNoTx(TstName.TEST_CREATE_EXPIRED_TIMER);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        //verify timer was created:
        infos = ivBean.getInfoOfAllTimers();
        assertEquals("Timer was not created.", 1, infos.size());

        //sleep long enough for the timer to time out
        ivBean.waitForTimer(AnnotationTxLocal.MAX_WAIT_TIME);

        //verify that the timer's timeout method was invoked
        assertEquals("Timeout method for timer failed to execute exactly once : " + svExpiredTimerInfos, 1, svExpiredTimerInfos.size());
        assertTrue("Timeout method for timer failed to execute : " + svExpiredTimerInfos, svExpiredTimerInfos.contains(AnnotationTxLocal.EXPIRED_INFO));
        infos = ivBean.getInfoOfAllTimers();
        assertEquals("Timer still exists after expiration.", 0, infos.size());

        //attempt to cancel the already-expired timer
        try {
            ivBean.executeTestNoTx(TstName.TEST_CANCEL_EXPIRED_TIMER);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
    }

    /**
     * This test creates a timer in a CMT transactional bean method and then
     * cancels it in a non-transactional method.
     * <br/>
     * The result of this test is that the timer should be active before it is
     * canceled, and after cancellation, no timers should be active.
     */
    @Test
    public void testCreateInTxButCancelInNoTx() {
        Collection<String> infos = null;
        try {
            ivBean.executeTest(TstName.TEST_CREATE_TIMER);
            infos = ivBean.getInfoOfAllTimers();
            assertEquals("Unexpected number of timers.", 1, infos.size());
            for (String info : infos) {
                assertEquals("Incorrect timer found", DEFAULT_INFO, info);
            }
            ivBean.executeTestNoTx(TstName.TEST_CANCEL_TIMER);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
        infos = ivBean.getInfoOfAllTimers();
        assertEquals("Timer was not canceled", 0, infos.size());
    }

    /**
     * This test creates a timer with an invalid expiration in a CMT
     * transactional bean method- this means that the Date object for the
     * expiration contains a negative time property (i.e. date.getTime()
     * returns a negative long integer). Since the time property is a long
     * to indicate the number of milliseconds since Epoch (January 1, 1970),
     * a negative time is clearly in the past. The spec states that if the
     * expiration Date object contains a negative time property, the create
     * method should throw an IllegalArgumentException.
     * <br/>
     * The expected outcome of this test is that the create method (with an
     * invalid expiration Date object) will throw an IllegalArgumentException.
     * It also fails to create the timer object - so the TimerService will show
     * no active timers.
     */
    @Test
    public void testCreateTimerWithInvalidExpiration() {
        try {
            ivBean.executeTest(TstName.TEST_INVALID_EXPIRATION);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
        assertEquals("TimerService allowed creation of timer with invalid expiration - pre-epoch (Jan 1, 1970).",
                     0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * This test creates a timer with a negative duration using a CMT
     * transactional bean method - this implies that the timer should expire
     * in the past.
     * <br/>
     * The expected outcome of this test is that the create method (with a
     * negative duration) will throw an IllegalArgumentException. It also fails
     * create the timer object - so the TimerService will show no active timers.
     */
    @Test
    public void testCreateTimerWithNegativeDuration() {
        try {
            ivBean.executeTest(TstName.TEST_NEGATIVE_DURATION);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
        assertEquals("TimerService allowed creation of timer with negative duration.",
                     0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * This test creates a timer with an expiration in the past via a CMT
     * transactional bean method - this means that the Date object for the
     * expiration represents a date/time that is before now (i.e.
     * expDate.before(new Date()) would return true). In this case, the timer
     * service is required to execute the timer immediately.
     * <br/>
     * The expected outcome of this test is that (1) the create method will
     * succeed, (2) the timer will appear in TimerService.getTimers() call, and
     * (3) the timeout method will be invoked (nearly) immediately.
     */
    @Test
    public void testCreateTimerToExpireInThePast() {
        // when creating a timer to expire in the past, it should time out immediately
        // we will wait for BUFFER seconds before checking

        svExpiredTimerInfos.clear();

        try {
            ivBean.executeTest(TstName.TEST_EXPIRATION_IN_PAST);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // wait BUFFER seconds
        ivBean.waitForTimer(AnnotationTxLocal.MAX_WAIT_TIME);

        assertEquals("Unexpected number of timers left in timer service", 0, ivBean.getInfoOfAllTimers().size());
        assertEquals("Unexpected number of timers expired during this test", 1, svExpiredTimerInfos.size());
        for (String info : svExpiredTimerInfos) {
            assertEquals("Timeout method invoked for incorrect timer (bad info)", "createTimerInThePastExpectingImmediateExpiration", info); //630958
        }
    }

    /**
     * This test creates a timer with an expiration in the past in a
     * non-transactional bean method - this means that the Date object for the
     * expiration represents a date/time that is before now (i.e.
     * expDate.before(new Date()) would return true). In this case, the timer
     * service is required to execute the timer immediately.
     * <br/>
     * The expected outcome of this test is that (1) the create method will
     * succeed, (2) the timer will appear in TimerService.getTimers() call, and
     * (3) the timeout method will be invoked (nearly) immediately.
     */
    @Test
    public void testCreateTimerToExpireInThePastNoTx() {
        // when creating a timer to expire in the past, it should time out immediately
        // we will wait for BUFFER seconds before checking

        svExpiredTimerInfos.clear();

        try {
            ivBean.executeTestNoTx(TstName.TEST_EXPIRATION_IN_PAST);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // wait BUFFER seconds
        ivBean.waitForTimer(AnnotationTxLocal.MAX_WAIT_TIME);

        assertEquals("Unexpected number of timers left in tiemr service", 0, ivBean.getInfoOfAllTimers().size());
        assertEquals("Unexpected number of timers expired during this test", 1, svExpiredTimerInfos.size());
        for (String info : svExpiredTimerInfos) {
            assertEquals("Timeout method invoked for incorrect timer (bad info)", "createTimerInThePastExpectingImmediateExpiration", info); // 630958
        }
    }

    /**
     * This test creates a timer with a duration greater than Integer.MAX_VALUE.
     * If the timer implementation incorrectly truncates to an integer
     * internally, then the timer will fire immediately rather delaying.
     */
    @Test
    public void testOverflowDurationTimer() throws Exception {
        svExpiredTimerInfos.clear();

        ivBean.executeTest(TstName.TEST_OVERFLOW_DURATION);

        try {
            FATHelper.sleep(AnnotationTxLocal.BUFFER);
            assertEquals("Unexpected number of timers left in timer service", 1, ivBean.getInfoOfAllTimers().size());
            assertEquals("Unexpected number of timers expired during this test", 0, svExpiredTimerInfos.size());
        } finally {
            ivBean.clearAllTimers();
        }
    }

    /**
     * This test creates a timer with an expiration date that is greater than
     * Integer.MAX_VALUE milliseconds from now. If the timer implementation
     * incorrectly truncates to an integer internally, then the timer will fire
     * immediately rather delaying.
     */
    @Test
    public void testOverflowExpirationTimer() throws Exception {
        svExpiredTimerInfos.clear();

        ivBean.executeTest(TstName.TEST_OVERFLOW_EXPIRATION);

        try {
            FATHelper.sleep(AnnotationTxLocal.BUFFER);
            assertEquals("Unexpected number of timers left in timer service", 1, ivBean.getInfoOfAllTimers().size());
            assertEquals("Unexpected number of timers expired during this test", 0, svExpiredTimerInfos.size());
        } finally {
            ivBean.clearAllTimers();
        }
    }

    /**
     * This test creates a timer with no duration but an interval greater than
     * Integer.MAX_VALUE. If the timer implementation incorrectly truncates to
     * an integer internally, then the timer will fire twice immediately rather
     * than delaying.
     */
    @Test
    public void testOverflowIntervalTimer() throws Exception {
        svExpiredTimerInfos.clear();

        ivBean.executeTest(TstName.TEST_OVERFLOW_INTERVAL);

        // Wait for interval timer to expire the first time
        ivBean.waitForTimer(AnnotationTxLocal.MAX_WAIT_TIME);

        try {
            // Timer interval should be very large; but if it is going to fail, it will fail
            // fairly quickly, so just wait a short amount of time to insure it doesn't run.
            FATHelper.sleep(AnnotationTxLocal.BUFFER);
            assertEquals("Unexpected number of timers left in timer service", 1, ivBean.getInfoOfAllTimers().size());
            assertEquals("Unexpected number of timers expired during this test", 1, svExpiredTimerInfos.size());
        } finally {
            ivBean.clearAllTimers();
        }
    }

    /**
     * This test creates a timer with an invalid expiration in a non-
     * transactional bean method- this means that the Date object for the
     * expiration contains a negative time property (i.e. date.getTime()
     * returns a negative long integer). Since the time property is a long
     * to indicate the number of milliseconds since Epoch (January 1, 1970),
     * a negative time is clearly in the past. The spec states that if the
     * expiration Date object contains a negative time property, the create
     * method should throw an IllegalArgumentException.
     * <br/>
     * The expected outcome of this test is that the create method (with an
     * invalid expiration Date object) will throw an IllegalArgumentException.
     * It also fails to create the timer object - so the TimerService will show
     * no active timers.
     */
    @Test
    public void testCreateTimerWithInvalidExpirationNoTx() {
        try {
            ivBean.executeTestNoTx(TstName.TEST_INVALID_EXPIRATION);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
        assertEquals("TimerService allowed creation of timer with invalid expiration - pre-epoch (Jan 1, 1970).",
                     0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * This test creates a timer with a negative duration using a non-
     * transactional bean method - this implies that the timer should expire
     * in the past.
     * <br/>
     * The expected outcome of this test is that the create method (with a
     * negative duration) will throw an IllegalArgumentException. It also fails
     * create the timer object - so the TimerService will show no active timers.
     */
    @Test
    public void testCreateTimerWithNegativeDurationNoTx() {
        try {
            ivBean.executeTestNoTx(TstName.TEST_NEGATIVE_DURATION);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
        assertEquals("TimerService allowed creation of timer with negative duration.",
                     0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * This test creates a timer and then cancels it in the same CMT method -
     * thus the create and cancel occur in the same transaction. The bean
     * method then marks the transaction to rollback-only.
     * <br/>
     * The expected outcome is that the timer has been canceled, an the timer
     * service would not contain any active timers.
     */
    @Test
    public void testCreateAndCancelInSameTxRollback() {
        try {
            ivBean.executeTest(TstName.TEST_CREATE_AND_CANCEL_IN_SAME_METHOD_ROLLBACK);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        assertEquals("Timers exist when the transaction rolled back",
                     0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * This test creates two timers in the same CMT method - thus in the same
     * transaction. The bean method then marks the transaction to
     * rollback-only.
     * The expected result is that the timer service should show no active
     * timers and that after the duration of both timers, the timer's timeout
     * method should not have been invoked at all.
     */
    @Test
    public void testCreateTwoTimersInSameTxRollback() {
        try {
            doTestCreateTwoTimersInSameMethod(TstName.TEST_CREATE_TWO_TIMERS_IN_SAME_METHOD_ROLLBACK);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        assertEquals("Timers exist when the transaction rolled back",
                     0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * This test creates a timer and then cancels it in the same
     * non-transactional bean method.
     * <br/>
     * The expected outcome is that the timer has been canceled, an the timer
     * service would not contain any active timers.
     */
    @Test
    public void testCreateAndCancelInNoTx() {
        try {
            ivBean.executeTestNoTx(TstName.TEST_CREATE_AND_CANCEL_IN_SAME_METHOD);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
        assertEquals("One or more Timers exist - expected none",
                     0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * This test creates a timer and then cancels it in separate CMT
     * transactional bean methods before the timer expires.
     * <br/>
     * The expected outcome is that the timer was created successfully and that
     * it is in the TimerService.getTimers() method. Once it has been canceled,
     * the timer should not longer appear in the getTimers() method. Last, the
     * timer's timeout method should not have been invoked.
     * service would not contain any active timers.
     */
    @Test
    public void testCreateAndCancelInSeparateTx() {
        try {
            svExpiredTimerInfos.clear();
            ivBean.executeTest(TstName.TEST_CREATE_TIMER);
            Collection<String> infos = ivBean.getInfoOfAllTimers();
            assertEquals("Unexpected number of timers", 1, infos.size());
            assertTrue("Did not contain expected timer info", infos.contains(DEFAULT_INFO));
            ivBean.executeTest(TstName.TEST_CANCEL_TIMER);
            infos = ivBean.getInfoOfAllTimers();
            assertEquals("Unexpected number of timers", 0, infos.size());
            assertEquals("Unexpected timer expiration : " + svExpiredTimerInfos, 0, svExpiredTimerInfos.size());

            FATHelper.sleep(AnnotationTxLocal.DURATION + AnnotationTxLocal.BUFFER);

            assertEquals("Timeout method invoked after being canceled : " + svExpiredTimerInfos, 0, svExpiredTimerInfos.size());
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
    }

    /**
     * This test crates an interval based timer based on duration (i.e. it will
     * first execute at DURATION milliseconds from when it is created), sleeps
     * for three timeouts, and then cancels the timer.
     * <br/>
     * The expected behavior is that the timer is successfully created, and its
     * timeout method is invoked exactly three times.
     */
    @Test
    public void testCreateIntervalDurationTimer() {
        svExpiredTimerInfos.clear();

        long startTime = System.nanoTime();
        try {
            ivBean.executeTest(TstName.TEST_INTERVAL_TIMER_DURATION);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // sleep long enough for initial wait time + two intervals
        long minimumDuration = AnnotationTxLocal.DURATION + AnnotationTxLocal.INTERVAL + AnnotationTxLocal.INTERVAL;
        ivBean.waitForTimer(AnnotationTxLocal.MAX_WAIT_TIME);
        long actualDuration = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        ivBean.clearAllTimers();

        assertEquals("Unexpected number of timout invocations", 3, svExpiredTimerInfos.size());
        for (String info : svExpiredTimerInfos) {
            assertEquals("Unexpected timer invoked (bad info)",
                         AnnotationTxLocal.INTERVAL_INFO, info);
        }
        assertTrue("Timers ran too quickly; minimum=" + minimumDuration + ", actual=" + actualDuration, actualDuration >= minimumDuration);
    }

    /**
     * This test crates an interval based timer based on expiration date (i.e.
     * it will first execute at the Date specified when it is created), sleeps
     * for three timeouts, and then cancels the timer.
     * <br/>
     * The expected behavior is that the timer is successfully created, and its
     * timeout method is invoked exactly three times.
     */
    @Test
    public void testCreateIntervalDateTimer() {
        svExpiredTimerInfos.clear();

        long startTime = System.nanoTime();
        try {
            ivBean.executeTest(TstName.TEST_INTERVAL_TIMER_DATE);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        // sleep long enough for initial wait time + two intervals
        long minimumDuration = AnnotationTxLocal.DURATION + AnnotationTxLocal.INTERVAL + AnnotationTxLocal.INTERVAL;
        ivBean.waitForTimer(AnnotationTxLocal.MAX_WAIT_TIME);
        long actualDuration = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        ivBean.clearAllTimers();

        assertEquals("Unexpected number of timout invocations", 3, svExpiredTimerInfos.size());
        for (String info : svExpiredTimerInfos) {
            assertEquals("Unexpected timer invoked (bad info)",
                         AnnotationTxLocal.INTERVAL_INFO, info);
        }
        assertTrue("Timers ran too quickly; minimum=" + minimumDuration + ", actual=" + actualDuration, actualDuration >= minimumDuration);
    }

    /**
     * This test case creates a timer using one bean then queries the
     * TimerService of another bean to verify that it does not return any timers
     * from the first bean.
     */
    @Test
    public void testCheckAnotherBeansTimers() {
        try {
            //lookup second bean so that we can easily detect if it returns timers
            // from the first
            Context ctx = new InitialContext();
            SimpleTimerLocal simpleBean = (SimpleTimerLocal) ctx.lookup("java:app/NpTimersEJB/SimpleTimerBean");

            //create a timer in the first bean, and verify it is started
            ivBean.executeTest(TstName.TEST_CREATE_TIMER);
            assertEquals("Initial timer creation failed.", 1, ivBean.getInfoOfAllTimers().size());

            //verify second bean has no started timers
            Collection<Timer> timers = simpleBean.getTimers();
            assertEquals("Second bean returned timer from first bean", 0, timers.size());

            //cancel the timer in the first bean, and verify no active timers
            ivBean.clearAllTimers();
            assertEquals("Timers still exist on first bean after clearing all timers.",
                         0, ivBean.getInfoOfAllTimers().size());

            //create a timer in the second bean, and verify it is started
            simpleBean.createTimer("secondBean");
            Collection<String> infos = simpleBean.getInfoOfAllTimers(); // d585483
            assertEquals("Initial timer creation of simple bean failed.", 1, infos.size());
            for (String info : infos) {
                assertEquals("Wrong timer info for simple bean", "secondBean", info);
            }

            //verify first bean has no started timers
            assertEquals("Timers exist on first bean when creating a timer on second bean.",
                         0, ivBean.getInfoOfAllTimers().size());
            simpleBean.clearAllTimers();

        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    /**
     * This test creates calendar timers and verifies that TimerService.getTimers()
     * called in the same transaction returns the created timers, except for
     * calendar timers that were created to expire in the past.
     */
    @Test
    public void testCreateCalendarAndFindInSameTx() {
        try {
            ivBean.executeTest(TstName.TEST_CREATE_CALENDAR_AND_FIND_IN_SAME_METHOD);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        assertEquals("One or more Timers exist - expected none", 0, ivBean.getInfoOfAllTimers().size());
    }

    /**
     * Helper method for multiple tests that create two timers in the same
     * method.
     *
     * @param tstName Test name enum used to determine this method's behavior.
     */
    private void doTestCreateTwoTimersInSameMethod(TstName tstName) {

        svExpiredTimerInfos.clear();

        try {
            ivBean.executeTest(tstName);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }

        if (tstName == TstName.TEST_CREATE_TWO_TIMERS_IN_SAME_METHOD_ROLLBACK) {
            // No need to wait; just make sure they don't exist and haven't run
            assertEquals("Timers exist even though rolled back", 0, ivBean.getInfoOfAllTimers().size());
            assertEquals("A timer executed, though the transaction was rolled back.", 0, svExpiredTimerInfos.size());
        } else {
            // wait long enough for both timers to expire...
            ivBean.waitForTimer(AnnotationTxLocal.MAX_WAIT_TIME);

            assertTrue("Timeout method for first timer failed to execute.",
                       svExpiredTimerInfos.contains("createTwoTimersInSameMethod1"));
            assertTrue("Timeout method for second timer failed to execute.",
                       svExpiredTimerInfos.contains("createTwoTimersInSameMethod2"));
        }

    }

    protected abstract void setIVBean(AnnotationTxLocal bean);
}
