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

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionContext;
import javax.ejb.Timer;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

public class TimeoutFailureBean implements TimeoutFailureLocal {

    private final static String CLASSNAME = TimeoutFailureBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private final static int ROLLBACK_CMT_TIMEOUT = 0;
    private final static int THROW_EXCEPTION_CMT_TIMEOUT = 1;
    private final static int THROW_EXCEPTION_BMT_TIMEOUT = 2;
    private final static int ROLLBACK_CMT_TIMEOUT_RETRY = 3;
    private final static int THROW_EXCEPTION_CMT_TIMEOUT_RETRY = 4;
    private final static int THROW_EXCEPTION_BMT_TIMEOUT_RETRY = 5;

    private static int svTestCase = -1;
    private static int svResults = RESULTS_NOT_RETRIED;

    public static CountDownLatch svResultsLatch = new CountDownLatch(1);

    @Resource
    private SessionContext ivSC;

    public void xmlTimeout(Timer timer) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "xmlTimeout", timer);
            svLogger.logp(Level.FINEST, CLASSNAME, "xmlTimeout", "svTestCase = {0}", svTestCase);
        }

        switch (svTestCase) {
            case ROLLBACK_CMT_TIMEOUT:
                svTestCase = ROLLBACK_CMT_TIMEOUT_RETRY;
                ivSC.setRollbackOnly();
                break;
            case THROW_EXCEPTION_CMT_TIMEOUT:
                svTestCase = THROW_EXCEPTION_CMT_TIMEOUT_RETRY;
                RuntimeException ex = new RuntimeException("expected exception");
                svLogger.throwing(CLASSNAME, "xmlTimeout", ex);
                throw ex;
            case THROW_EXCEPTION_BMT_TIMEOUT:
                svTestCase = THROW_EXCEPTION_BMT_TIMEOUT_RETRY;
                UserTransaction ut = ivSC.getUserTransaction();
                try {
                    ut.begin();
                    //start a new timer to verify that the tran is rolled back
                    TimerHelper.createTimer(ivSC.getTimerService(), DURATION, null,
                                            "INCOMPLETE_BMT_TIMEOUT", false, null);
                    RuntimeException ex2 = new RuntimeException("expected exception");
                    svLogger.throwing(CLASSNAME, "xmlTimeout", ex2);
                    throw ex2;
                } catch (NotSupportedException nse) {
                    svLogger.logp(Level.SEVERE, CLASSNAME, "xmlTimeout", "Error during ut.begin()", nse);
                    svResults = RESULTS_TEST_ERROR;
                } catch (SystemException se) {
                    svLogger.logp(Level.SEVERE, CLASSNAME, "xmlTimeout", "Error during ut.begin()", se);
                    svResults = RESULTS_TEST_ERROR;
                }

            case ROLLBACK_CMT_TIMEOUT_RETRY:
            case THROW_EXCEPTION_CMT_TIMEOUT_RETRY:
                svResults = RESULTS_RETRIED_AS_EXPECTED;
                svResultsLatch.countDown();
                break;
            case THROW_EXCEPTION_BMT_TIMEOUT_RETRY:
                Collection<Timer> timers = ivSC.getTimerService().getTimers();
                for (Timer t : timers) {
                    String info = (String) t.getInfo();
                    if ("INCOMPLETE_BMT_TIMEOUT".equals(info)) {
                        svResults = RESULTS_BMT_TRAN_NOT_ROLLEDBACK;
                    }
                }

                if (svResults == RESULTS_NOT_RETRIED) {
                    svResults = RESULTS_RETRIED_AS_EXPECTED;
                }
                svResultsLatch.countDown();
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "xmlTimeout");
        }
    }

    @Override
    public void prepRollbackCMTTimeout() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "prepRollbackCMTTimeout");
        }

        svTestCase = ROLLBACK_CMT_TIMEOUT;
        TimerHelper.createTimer(ivSC.getTimerService(), DURATION, null,
                                "prepRollbackCMTTimeout", false, null);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "prepRollbackCMTTimeout");
        }
    }

    @Override
    public void prepThrowExceptionCMTTimeout() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "prepThrowExceptionCMTTimeout");
        }

        svTestCase = THROW_EXCEPTION_CMT_TIMEOUT;
        TimerHelper.createTimer(ivSC.getTimerService(), DURATION, null,
                                "prepThrowExceptionCMTTimeout", false, null);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "prepThrowExceptionCMTTimeout");
        }
    }

    @Override
    public void prepThrowExceptionInTxBMTTimeout() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "prepThrowExceptionInTxBMTTimeout");
        }

        svTestCase = THROW_EXCEPTION_BMT_TIMEOUT;
        TimerHelper.createTimer(ivSC.getTimerService(), DURATION, null,
                                "prepThrowExceptionInTxBMTTimeout", false, null);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "prepThrowExceptionInTxBMTTimeout");
        }
    }

    @Override
    public int getResults() {
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "getResults", "returning " + svResults);
        }
        return svResults;
    }

    @Override
    public void reset() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "reset");
        }

        svResults = RESULTS_NOT_RETRIED;
        svResultsLatch = new CountDownLatch(1);
        svTestCase = -1;

        for (Timer t : ivSC.getTimerService().getTimers()) {
            svLogger.logp(Level.FINEST, CLASSNAME, "reset", "canceling timer: " + t);
            try {
                t.cancel();
            } catch (NoSuchObjectLocalException ex) {
                // Test may complete and try and cleanup before the timer actually finished
                // running, so may find the timer but be unable to cancel it, as it has
                // already cancelled itself (completed single action).
                svLogger.logp(Level.FINEST, CLASSNAME, "reset", "timer has completed; caught allowable exception: " + ex);
            }
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "reset");
        }
    }

}
