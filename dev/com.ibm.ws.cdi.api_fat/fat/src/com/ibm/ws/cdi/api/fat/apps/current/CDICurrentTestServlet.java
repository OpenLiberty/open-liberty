/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.api.fat.apps.current;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.api.fat.apps.current.extension.MyDeploymentVerifier;
import com.ibm.ws.cdi.api.fat.apps.current.sharedLib.ISimpleBean;
import com.ibm.ws.cdi.api.fat.apps.current.sharedLib.SharedLibBean;

import componenttest.app.FATServlet;

@WebServlet("/")
public class CDICurrentTestServlet extends FATServlet {

    private static final Logger LOGGER = Logger.getLogger(CDICurrentTestServlet.class.getName());

    private static final long serialVersionUID = 1L;

    //Must be manually cleared at the end of a test
    private static Boolean wasCDICurrentFound = null;

    @Inject
    private SharedLibBean sharedLibBean;

    @Resource
    ManagedExecutorService managedExecutorService;

    /*
     * Not annotated @Test because it's called manually from CDIAPITests
     */
    public void testCDICurrent() {

        String message = MyDeploymentVerifier.getMessage();
        assertEquals(message, MyDeploymentVerifier.SUCCESS, message);

        ISimpleBean sb = CDI.current().select(ISimpleBean.class).get();
        assertNotNull("SimpleBean was null", sb);
        String msg = sb.test();
        assertEquals("SimpleBean message was: " + msg, SimpleBean.MSG, msg);

    }

    /*
     * CDI.current() requires access to contextual information from the thread. ManagedExecutor will propagate that
     * context onto a newly created thread. This test ensures that works correctly and CDI.current() returns a value
     * when called from a new thread.
     *
     */
    @Test
    public void testCDICurrentViaMES() throws Exception {
        try {
            long startTime = System.nanoTime();

            assertNotNull(CDI.current()); //Test outside a new thread just for completeness.

            LOGGER.info("calling managedExecutorService");
            managedExecutorService.submit(new CallCDICurrent());

            while (System.nanoTime() - startTime < Duration.of(10, SECONDS).toNanos()) {
                if (wasCDICurrentFound != null) {
                    assertTrue("CDI.current returned null when called in a new Managed Thread", wasCDICurrentFound);
                    return;
                }
                Thread.sleep(15);
            }

            LOGGER.info("About to throw exception");
            assertTrue("The thread with CDI.current never completed", false);
        } finally {
            wasCDICurrentFound = null;
        }
    }

    /*
     * We have implemented a fallback that allows CDI.current() to work if you call a new thread
     * without propagating context via a ManagedExecutorService, this tests that code.
     */
    @Test
    public void testCDICurrentViaNewThread() throws Exception {
        try {
            long startTime = System.nanoTime();

            assertNotNull(CDI.current()); //Test outside a new thread just for completeness.
            LOGGER.info("calling managedExecutorService");
            Thread thread = new Thread(new CallCDICurrent());
            thread.run();

            while (System.nanoTime() - startTime < Duration.of(10, SECONDS).toNanos()) {
                if (wasCDICurrentFound != null) {
                    assertTrue("CDI.current returned null when called in a new Thread", wasCDICurrentFound);
                    return;
                }
                Thread.sleep(15);
            }

            LOGGER.info("About to throw exception");
            fail("The thread with CDI.current never completed");
        } finally {
            wasCDICurrentFound = null;
        }
    }

    @Test
    public void testCDICurrentViaSharedLib() {
        sharedLibBean.testCdiCurrentSharedLib();
    }

    public static void setWasCDICurrentFound(boolean b) {
        wasCDICurrentFound = b;
        LOGGER.info("Set test variable");
    }

    public class CallCDICurrent implements Runnable {

        @Override
        public void run() {
            CDI cdi = CDI.current();
            LOGGER.info("Found CDI " + cdi);
            if (cdi != null) {
                CDICurrentTestServlet.setWasCDICurrentFound(true);
                LOGGER.info("Calling setter for test variable with true");
            } else {
                LOGGER.info("Calling setter for test variable with false");
                CDICurrentTestServlet.setWasCDICurrentFound(false);
            }
        }
    }
}
