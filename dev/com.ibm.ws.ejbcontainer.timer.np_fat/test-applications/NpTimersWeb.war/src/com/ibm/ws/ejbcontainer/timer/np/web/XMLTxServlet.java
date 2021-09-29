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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.ejb.XMLTxBean;
import com.ibm.ws.ejbcontainer.timer.np.ejb.XMLTxLocal;

@WebServlet("/XMLTxServlet")
@SuppressWarnings("serial")
public class XMLTxServlet extends AbstractServlet {

    private final static String CLASSNAME = XMLTxServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB(beanInterface = XMLTxLocal.class)
    XMLTxLocal ivBean;

    @Override
    protected void clearAllTimers() {
        if (ivBean != null) {
            ivBean.clearAllTimers();
        }
    }

    /**
     * This test case verifies the proper transactional behavior of the cancel
     * operation. According to the spec the cancel operation should take place
     * immediately, but if the cancel operation's transaction is rolled back,
     * then the timer should be immediately "un-canceled" - and if the timer
     * should have expired during the cancel transaction, then it should expire
     * immediately.
     * <br/>
     * In this test, the timer is created, then a new transaction starts which
     * does the following:
     * <ol>
     * <li>Cancels the timer</li>
     * <li>Delays until the timer <em>should</em> have expired</li>
     * <li>Verifies that the timer indeed did not expire</li>
     * <li>Marks the transaction rollback only</li>
     * </ol>
     * Then this test code verifies that the timer expires after the cancel
     * transaction has completed (rolled back). It also verifies that the
     * timer only invokes once.
     */
    @Test
    public void testCancelTxSemantics() {
        final String info = "testCancelTxSemantics";
        Date expectedTimeout = null;

        // Some build systems are so slow it takes longer than the expiration interval
        // to create the timer, so it has already expired by the time the next method
        // is called. If this occurs, repeat up to 5 times before giving up. Likely
        // creatTimer will be faster after the path is warmed up.
        for (int i = 0; i < 5; i++) {
            svLogger.info(i + "Attempting reset/createTimer/checkCancelTxSemantics...");
            ivBean.reset();
            expectedTimeout = ivBean.createTimer(info);
            if (ivBean.checkCancelTxSemantics()) {
                svLogger.info("Completed checkCancelTxSemantics successfully");
                break;
            }
            if (i == 4) {
                svLogger.logp(Level.WARNING, CLASSNAME, info, "System running to slowly to complete the test.");
            }
        }
        try {
            svLogger.info("Waiting on latch for timer to expire for " + (XMLTxBean.DURATION + XMLTxBean.ACCURACY) + "ms");
            XMLTxBean.svNextTimeoutLatch.await(XMLTxBean.DURATION + XMLTxBean.ACCURACY, TimeUnit.MILLISECONDS);
            svLogger.info("Returned from latch.await");
        } catch (InterruptedException ex) {
            svLogger.logp(Level.WARNING, CLASSNAME, info,
                          "Unexpected InterruptedException: ", ex);
        }
        assertNotNull("Timer expiration should have set XMLTxBean.svInfo to a non-null string",
                      XMLTxBean.svInfo);

        assertEquals("XMLTxBean.svInfo was set to the wrong string (bad info)",
                     info, XMLTxBean.svInfo);

        String nextTimeoutStr = ivBean.getNextTimeoutString();
        assertNotNull("Timeout method (expireTimeout) did not execute.", nextTimeoutStr);

        try {
            Date nextTimeout = XMLTxLocal.df.parse(nextTimeoutStr);
            assertTrue("getNextTimeout() return an unexpected value of "
                       + nextTimeout.getTime() + "; expected to be "
                       + expectedTimeout.getTime(),
                       expectedTimeout.equals(nextTimeout));
        } catch (ParseException ex) {
            fail("getNextTimeout() threw an exception rather than returning the time of the executing timer: " + nextTimeoutStr);
        }

        ivBean.reset();

        FATHelper.sleep(XMLTxBean.DURATION);

        assertNull("Timer should be completely expired but instead set XMLTxBean.svInfo to a non-null string",
                   XMLTxBean.svInfo);
    }

    /**
     * This test case creates a single action timer, waits for it to expire, and
     * then verify that during its expiration when it calls
     * timer.getNextTimeout() that it returns the current timeout expiration.
     */
    @Test
    public void testGetNextTimeoutExpiredSingleActionTimer() {
        final String info = "testGetNextTimeoutExpiredSingleActionTimer";
        Date expectedTimeout = ivBean.createTimer(info);

        // wait to ensure timer expires:
        try {
            svLogger.info("Waiting on latch for timer to expire for " + XMLTxBean.MAX_TIMER_WAIT + "ms");
            XMLTxBean.svNextTimeoutLatch.await(XMLTxBean.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            svLogger.logp(Level.WARNING, CLASSNAME, info, "InterruptedException", ex);
        }

        String nextTimeoutStr = ivBean.getNextTimeoutString();
        assertNotNull("Timeout method (expireTimeout) did not execute.",
                      nextTimeoutStr);

        try {
            Date nextTimeout = XMLTxLocal.df.parse(nextTimeoutStr);
            assertTrue("getNextTimeout() return an unexpected value of "
                       + nextTimeout.getTime() + "; expected to be "
                       + expectedTimeout.getTime(),
                       expectedTimeout.equals(nextTimeout));
        } catch (ParseException ex) {
            fail("getNextTimeout() threw an exception rather than returning the time of the executing timer: " + nextTimeoutStr);
        }
    }

    /**
     * This test case creates an interval timer, waits for it to expire, and
     * then verify that the timer.getNextTimeout() returns the Date of the next
     * interval (one minute into the future).
     */
    @Test
    public void testGetNextTimeoutOngoingIntervalTimer() {
        final String info = "testGetNextTimeoutOngoingIntervalTimer";

        Date timeOfFirstTimeout = ivBean.createIntervalTimer(info);

        // wait to ensure timer could expires:
        try {
            svLogger.info("Waiting on latch for timer to expire for " + XMLTxBean.MAX_TIMER_WAIT + "ms");
            XMLTxBean.svNextTimeoutLatch.await(XMLTxBean.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            svLogger.logp(Level.WARNING, CLASSNAME, info, "InterruptedException", ex);
        }

        Date timeAfterFirstTimeout = new Date();
        Date timeOfSecondTimeout = new Date(timeOfFirstTimeout.getTime() + XMLTxBean.INTERVAL);

        String nextTimeoutStr = ivBean.getNextTimeoutString();

        assertFalse("timer.getNextTimeout() threw unexpected NoMoreTimeoutsException.",
                    "NoMoreTimeoutsException".equals(nextTimeoutStr));

        assertFalse("timer.getNextTimeout() threw unexpected exception.",
                    "UnexpectedException".equals(nextTimeoutStr));

        assertFalse("timer.getNextTimeout() returned an unknown condition.",
                    "UNKNOWN".equals(nextTimeoutStr));

        Date nextTimeout = null;
        try {
            nextTimeout = XMLTxLocal.df.parse(nextTimeoutStr);
        } catch (ParseException ex) {
            String msg = "Failed to parse: " + nextTimeoutStr;
            svLogger.logp(Level.WARNING, CLASSNAME, info, msg, ex);
            fail(msg);
        }

        assertNotNull("Timeout method (expireTimeout) did not execute.", nextTimeout);

        assertTrue("Next iteration (" + nextTimeout + ") occurs earlier than expected (" + timeAfterFirstTimeout + ")",
                   timeAfterFirstTimeout.before(nextTimeout));

        assertTrue("Next iteration (" + nextTimeout.getTime()
                   + ") occurs later than expected ("
                   + timeOfSecondTimeout.getTime() + ")",
                   timeOfSecondTimeout.equals(nextTimeout));
    }

    /**
     * This test case creates an interval timer, waits for it to expire, and
     * then verify that during its expiration when it calls timer.cancel() and
     * then timer.getNextTimeout() that it throws a NoSuchObjectLocalException.
     */
    @Test
    public void testGetNextTimeoutCanceledIntervalTimer() {
        final String info = "testGetNextTimeoutCanceledIntervalTimer";

        ivBean.createIntervalTimer(info);

        // wait to ensure timer could expires:
        try {
            svLogger.info("Waiting on latch for timer to expire for " + XMLTxBean.MAX_TIMER_WAIT + "ms");
            XMLTxBean.svNextTimeoutLatch.await(XMLTxBean.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            svLogger.logp(Level.WARNING, CLASSNAME, info, "InterruptedException", ex);
        }

        String nextTimeout = ivBean.getNextTimeoutString();

        assertNotNull("Timeout method (expireTimeout) did not execute.", nextTimeout);
        assertEquals("timer.getNextTimeout() did not throw expected NoMoreTimeoutsException."
                     + "  Current time is: " + XMLTxLocal.df.format(new Date()),
                     "NoSuchObjectLocalException", nextTimeout);
    }
}
