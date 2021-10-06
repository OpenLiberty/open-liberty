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
package com.ibm.ws.ejbcontainer.timer.np.lifecycle.web;

import static com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf.INFO_ASYNC_PREP_PRE_DESTROY;
import static com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf.INFO_POST_CONSTRUCT;
import static com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf.INFO_PREP_PRE_DESTROY;
import static com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf.INFO_PRE_DESTROY;
import static com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf.MAX_WAIT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleHelper;
import com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf;

/**
 * Test that non-persistent timers respect application lifecycle.
 */
@WebServlet("LifecycleServlet")
@SuppressWarnings("serial")
public class LifecycleServlet extends AbstractServlet {

    private static final Logger logger = Logger.getLogger(LifecycleServlet.class.getName());

    private static Date timerExpiration;

    /**
     * Verify that a timer can be created during singleton PostConstruct, that
     * the timer is returned as part of getTimers, but that it does not fire
     * until after PostConstruct has returned (the application is fully
     * started).
     */
    public void testPostConstructWaits() throws Exception {

        // Verify the results recorded during @PostConstruct
        assertNotNull("getTimers not called", LifecycleHelper.svPostConstructTimers);
        assertTrue("getTimers wrong size in PostConstruct = " + LifecycleHelper.svPostConstructTimers, LifecycleHelper.svPostConstructTimers.size() == 1);
        assertNull("timer fired during PostConstruct = " + LifecycleHelper.svPostConstructTimerFired, LifecycleHelper.svPostConstructTimerFired);

        // Wait for timer to fire (probably already has by now, but just in case)
        logger.info("Waiting for @PostConstruct created timer to run, for maximum time  =" + MAX_WAIT);
        LifecycleHelper.svPostConstructTimerLatch.await(MAX_WAIT, TimeUnit.MILLISECONDS);

        // Verify timer created during @PostConstruct does eventually run
        assertNotNull("timer not fired", LifecycleHelper.svTimerFired.get(INFO_POST_CONSTRUCT));
    }

    /**
     * Verify that existing timers are cancelled when an application is stopped
     * and that new timers cannot be created during PreDestroy.
     */
    public void prepareForTestApplicationStopCancelsTimers() throws Exception {

        LifecycleIntf bean = (LifecycleIntf) new InitialContext().lookup("java:global/NpTimerLifecycleApp/NpTimerLifecycleEJB/LifecycleBean!com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf");

        // Create a timer now, that won't run before the application is stopped.
        timerExpiration = bean.createTimer(LifecycleIntf.STOP_DURATION, INFO_PREP_PRE_DESTROY);

        // Create a timer in a transaction that will not commit until after the application is stopped.
        bean.createTimerAsync(0, INFO_ASYNC_PREP_PRE_DESTROY);
    }

    /**
     * Verify that existing timers are cancelled when an application is stopped
     * and that new timers cannot be created during PreDestroy.
     */
    public void testApplicationStopCancelsTimers() throws Exception {

        // The application has stopped; notify async timer creation to commit.
        LifecycleHelper.svAsyncCreateTimerLatch.countDown();

        // calculate how long before the timer is expected to expire
        long sleepTime = 500;
        long expiration = timerExpiration.getTime();
        long current = System.currentTimeMillis();
        if (expiration > current) {
            sleepTime += (expiration - current);
        }

        // Wait a short while for any timers to go off (shouldn't happen)
        FATHelper.sleep(sleepTime);

        assertNull("timer created during PreDestroy = " + LifecycleHelper.svPreDestroyTimerCreated, LifecycleHelper.svPreDestroyTimerCreated);
        assertNotNull("getTimers not called", LifecycleHelper.svPreDestroyTimers);
        assertTrue("getTimers not empty in PreDestroy = " + LifecycleHelper.svPreDestroyTimers, LifecycleHelper.svPreDestroyTimers.isEmpty());
        assertNull("timer fired after application stop = " + LifecycleHelper.svTimerFired.get(INFO_PRE_DESTROY), LifecycleHelper.svTimerFired.get(INFO_PRE_DESTROY));
        if (expiration >= current) { // skip check if timer may have run BEFORE application stopped
            assertNull("timer fired after application stop = " + LifecycleHelper.svTimerFired.get(INFO_PREP_PRE_DESTROY), LifecycleHelper.svTimerFired.get(INFO_PREP_PRE_DESTROY));
        }
        assertNull("timer fired after application stop = " + LifecycleHelper.svTimerFired.get(INFO_ASYNC_PREP_PRE_DESTROY),
                   LifecycleHelper.svTimerFired.get(INFO_ASYNC_PREP_PRE_DESTROY));
    }

    @Override
    protected void clearAllTimers() {
    }

}
