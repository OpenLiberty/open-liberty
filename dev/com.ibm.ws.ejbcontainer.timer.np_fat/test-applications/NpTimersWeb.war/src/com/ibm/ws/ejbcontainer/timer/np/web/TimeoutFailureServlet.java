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

import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.np.ejb.TimeoutFailureBean;
import com.ibm.ws.ejbcontainer.timer.np.ejb.TimeoutFailureLocal;

import componenttest.annotation.ExpectedFFDC;

@WebServlet("/TimeoutFailureServlet")
@SuppressWarnings("serial")
public class TimeoutFailureServlet extends AbstractServlet {
    private final static String CLASSNAME = TimeoutFailureServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB(beanName = "TimeoutFailureBeanCMT")
    private TimeoutFailureLocal ivCMTBean;

    @EJB(beanName = "TimeoutFailureBeanBMT")
    private TimeoutFailureLocal ivBMTBean;

    @Override
    protected void clearAllTimers() {
        if (ivCMTBean != null) {
            ivCMTBean.reset();
        }
        if (ivBMTBean != null) {
            ivBMTBean.reset();
        }
    }

    /**
     * This test case creates a timer on a CMT SLSB and waits for it to timeout.
     * The timeout method then sets rollback only and exits. The container is
     * then required to immediately retry the timeout method at least once. This
     * test verifies that the timeout method was retried once.
     */
    @Test
    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRolledbackException")
    public void testRollbackCMTTimeout() {
        final String method = "testRollbackCMTTimeout";

        ivCMTBean.prepRollbackCMTTimeout();

        try {
            svLogger.info("Waiting on latch for timer to expire for " + TimeoutFailureLocal.MAX_TIMER_WAIT + "ms");
            TimeoutFailureBean.svResultsLatch.await(TimeoutFailureLocal.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            svLogger.logp(Level.WARNING, CLASSNAME, method, "Unexpected exception during sleep", ex);
        }

        int results = ivCMTBean.getResults();

        switch (results) {
            case TimeoutFailureLocal.RESULTS_NOT_RETRIED:
                fail("Timeout method was never retried");
                break;
            case TimeoutFailureLocal.RESULTS_RETRIED_AS_EXPECTED:
                //pass
                break;
            default:
                fail("Unexpected test condition");
        }
    }

    /**
     * This test case creates a timer on a CMT SLSB and waits for it to timeout.
     * The timeout method then throws a RuntimeException. The container is then
     * required to immediately retry the timeout method at least once. This test
     * verifies that the timeout method was retried once.
     */
    @Test
    @ExpectedFFDC("java.lang.RuntimeException")
    public void testThrowExceptionCMTTimeout() {
        final String method = "testThrowExceptionCMTTimeout";

        ivCMTBean.prepThrowExceptionCMTTimeout();

        try {
            svLogger.info("Waiting on latch for timer to expire for " + TimeoutFailureLocal.MAX_TIMER_WAIT + "ms");
            TimeoutFailureBean.svResultsLatch.await(TimeoutFailureLocal.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            svLogger.logp(Level.WARNING, CLASSNAME, method, "Unexpected exception during sleep", ex);
        }

        int results = ivCMTBean.getResults();

        switch (results) {
            case TimeoutFailureLocal.RESULTS_NOT_RETRIED:
                fail("Timeout method was never retried");
                break;
            case TimeoutFailureLocal.RESULTS_RETRIED_AS_EXPECTED:
                //pass
                break;
            default:
                fail("Unexpected test condition");
        }
    }

    /**
     * This test case creates a timer on a BMT SLSB and waits for it to timeout.
     * The timeout method then starts a transaction and throws a
     * RuntimeException before completing the transaction. The container is
     * then required to rollback the unresolved transaction and then immediately
     * retry the timeout method at least once. This test verifies that the
     * timeout method was retried once.
     */
    @Test
    @ExpectedFFDC("java.lang.RuntimeException")
    public void testThrowExceptionInTxBMTTimeout() {
        final String method = "testThrowExceptionInTxBMTTimeout";

        ivBMTBean.prepThrowExceptionInTxBMTTimeout();

        try {
            svLogger.info("Waiting on latch for timer to expire for " + TimeoutFailureLocal.MAX_TIMER_WAIT + "ms");
            TimeoutFailureBean.svResultsLatch.await(TimeoutFailureLocal.MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            svLogger.logp(Level.WARNING, CLASSNAME, method, "Unexpected exception during sleep", ex);
        }

        int results = ivBMTBean.getResults();

        switch (results) {
            case TimeoutFailureLocal.RESULTS_NOT_RETRIED:
                fail("Timeout method was never retried");
                break;
            case TimeoutFailureLocal.RESULTS_RETRIED_AS_EXPECTED:
                //pass
                break;
            case TimeoutFailureLocal.RESULTS_BMT_TRAN_NOT_ROLLEDBACK:
                fail("Timeout method with unresolved BMT transaction did not roll back as expected.");
            default:
                fail("Unexpected test condition");
        }
    }
}
