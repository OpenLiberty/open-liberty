/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.auto.npTimer.context.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBAccessException;
import javax.ejb.Local;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timer;

@Stateless
@Local(NPAutoTimerLocal.class)
public class NPAutoTimerBean {

    private static final Logger logger = Logger.getLogger(NPAutoTimerBean.class.getName());
    private static final CountDownLatch autoTimerLatch = new CountDownLatch(1);
    private static final String expectedPrincipleName = "UNAUTHENTICATED";

    @Resource
    private SessionContext context;

    /**
     * Creates a timer that times out on the second 15 of any minute/hour
     *
     * @see NPAutoTimerLocal.java
     */
    @Schedule(hour = "*", minute = "*", second = "15", info = "NPAutoTimerBean", persistent = false)
    public void timeout(Timer timer) {
        logger.info("TIMEOUT: " + timer);
        try {
            String auth = role1Only();
            logger.info("timeout role1Only called as: " + auth + " Expected: " + expectedPrincipleName);
            assertEquals("role1Only has wrong Principal", expectedPrincipleName, auth);

        } catch (Exception ex) {
            logger.info("Caught expected exception: " + ex);
            Throwable cause = ex.getCause();
            assertTrue("Nested exception is EJBAccessException", cause instanceof EJBAccessException);
        }
        autoTimerLatch.countDown();
        if (autoTimerLatch.getCount() <= 0) {
            timer.cancel();
        }
    }

    /**
     * @see NPAutoTimerLocal.java
     */
    public boolean waitForAutomaticTimer() {
        try {
            // Waits up to 65 seconds for the timer to timeout
            autoTimerLatch.await(65, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean timedout = autoTimerLatch.getCount() == 0;
        logger.info("< waitForAutomaticTimer : " + timedout);
        return timedout;
    }

    /**
     * @see NPAutoTimerLocal.java
     */
    @RolesAllowed("Role1")
    public String role1Only() {
        logger.info("in role1Only");
        return authenticate();
    }

    /**
     * Returns the principle name from the context if Principle is not null,
     * otherwise returns null.
     *
     * @return
     */
    private String authenticate() {
        java.security.Principal principal = context.getCallerPrincipal();
        String principalName = null;
        if (principal != null) {
            principalName = principal.getName();
        } else {
            principalName = "null";
        }
        return principalName;
    }
}
