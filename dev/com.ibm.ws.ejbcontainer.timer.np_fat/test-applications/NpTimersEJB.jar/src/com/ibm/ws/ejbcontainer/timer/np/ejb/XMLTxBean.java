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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionContext;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

public class XMLTxBean implements XMLTxLocal {

    private final static String CLASSNAME = XMLTxBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static volatile CountDownLatch svNextTimeoutLatch = new CountDownLatch(1);

    private static Timer svTimer;

    public static volatile String svInfo;
    private static volatile String svNextTimeout;

    public static final long DURATION = 1000;
    public static final long INTERVAL = 60 * 1000;
    public static final long ACCURACY = 3000;
    public static final long MAX_TIMER_WAIT = 3 * 60 * 1000;

    @Resource
    private TimerService ivTS;

    @Resource
    private SessionContext ivSC;

    public void expireTimeout(Timer t) {
        svLogger.logp(Level.INFO, CLASSNAME, "expireTimeout", "Timeout, t == {0}", t);
        svInfo = (String) t.getInfo();
        svLogger.logp(Level.FINEST, CLASSNAME, "expireTimeout", "info == {0}", svInfo);

        if ("testGetNextTimeoutCanceledIntervalTimer".equals(svInfo)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "expireTimeout", "canceling timer");
            t.cancel();
        }

        try {
            svNextTimeout = df.format(t.getNextTimeout());
        } catch (NoMoreTimeoutsException ex) {
            svNextTimeout = "NoMoreTimeoutsException";
        } catch (NoSuchObjectLocalException ex) {
            svNextTimeout = "NoSuchObjectLocalException";
        } catch (Throwable th) {
            String msg = "Caught unexpected exception from timer.getNextTimeout()" + th.toString();
            svLogger.logp(Level.INFO, CLASSNAME, "expireTimeout", msg, th);
            svNextTimeout = th.getClass().getSimpleName();
        }

        if (svNextTimeout == null) {
            svNextTimeout = "UNKNOWN";
        }

        svLogger.info("svNextTimeout = " + svNextTimeout);
        svNextTimeoutLatch.countDown();
        svNextTimeoutLatch = new CountDownLatch(1);
    }

    @Override
    public boolean checkCancelTxSemantics() {
        final String info = "checkCancelTxSemantics";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, info);
        }
        assertNotNull("Failed pre-condition - svTimer must be initialized.", svTimer);

        Timer t = svTimer;

        try {
            t.cancel();
        } catch (NoSuchObjectLocalException ex) {
            // System is too slow; it took too long to create the timer, so it
            // has already expired and no longer exists. Report test not attempted.
            return false;
        }

        FATHelper.sleep(DURATION + ACCURACY);

        assertNull("Timout method executed, even though it was canceled prior", svInfo);

        ivSC.setRollbackOnly();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, info);
        }

        // Report that the test completed.
        return true;
    }

    @Override
    public Date createTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "createTimer", info);
        }

        Timer t = TimerHelper.createTimer(ivTS, DURATION, null, info, false, null);
        svTimer = t;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "createTimer - created timer: " + t);
        }
        return t.getNextTimeout();
    }

    @Override
    public Date createIntervalTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "createIntervalTimer", info);
        }

        Timer t = TimerHelper.createTimer(ivTS, DURATION, null, info, false, INTERVAL);
        svTimer = t;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "createIntervalTimer - created timer: " + t);
        }

        return t.getNextTimeout();
    }

    /**
     * This method assumes that the timer has expired or has been canceled.
     */
    @Override
    public void checkNoSuchObjectLocalException() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "checkNoSuchObjectLocalException");
        }

        try {
            long time = svTimer.getTimeRemaining();
            fail("svTimer.getTimeRemaining() should have thrown a NoSuchObjectLocalException"
                 + ", but instead returned " + time + " milliseconds.");
        } catch (NoSuchObjectLocalException ex) {
            // pass
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "checkNoSuchObjectLocalException");
        }
    }

    @Override
    public String getNextTimeoutString() {
        return svNextTimeout;
    }

    @Override
    public void reset() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "reset");
        }

        svTimer = null;
        svInfo = null;
        svNextTimeout = null;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "reset");
        }
    }

    @Override
    public void clearAllTimers() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "clearAllTimers");
        }

        // Because the test uses a CountDownLatch to detect when a timer is running
        // there is a race to cancel the timer between the end of the test (here)
        // and completion of the final timeout expiration; ignore failure due to
        // timer already being cancelled.

        Collection<Timer> timers = ivTS.getTimers();
        for (Timer t : timers) {
            try {
                t.cancel();
            } catch (NoSuchObjectLocalException ex) {
                svLogger.info("Ignoring exception; timer destroyed after final expiration : " + ex);
            }
        }

        reset();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "clearAllTimers");
        }
    }

}
