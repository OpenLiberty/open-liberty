/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.api.fat.apps.current;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static java.time.temporal.ChronoUnit.SECONDS;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.annotation.WebServlet;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.cdi.api.fat.apps.current.extension.MyDeploymentVerifier;

import componenttest.app.FATServlet;

@WebServlet("/")
public class CDICurrentTestServlet extends FATServlet {

    private static final Logger LOGGER = Logger.getLogger(CDICurrentTestServlet.class.getName());

    private static final long serialVersionUID = 1L;

    private static Boolean wasCDICurrentFound = null;
    private static volatile Boolean wasCDICurrentFoundViaMES = null;

    @Resource
    ManagedExecutorService managedExecutorService;

    public void testCDICurrent() {

        String message = MyDeploymentVerifier.getMessage();
        assertEquals(message, MyDeploymentVerifier.SUCCESS, message);

        SimpleBean sb = CDI.current().select(SimpleBean.class).get();
        assertNotNull("SimpleBean was null", sb);
        String msg = sb.test();
        assertEquals("SimpleBean message was: " + msg, SimpleBean.MSG, msg);

    }

    /*
     * CDI.current() requires access to contextual information from the thread. ManagedExecutor will propagate that
     * context onto a newly created thread. This test ensures that works correctly and CDI.current() returns a value
     * when called from a new thread.
     *
     * Note that threads created via new Thread() will not have the required context and CDI.current() will return null
     */    
    public void testCDICurrentViaMES() throws Exception {
        long startTime = System.nanoTime();

        assertNotNull(CDI.current()); //Test outside a new thread just for completeness.

        LOGGER.info("calling managedExecutorService");
        managedExecutorService.submit(new CallCDICurrent());

        while (System.nanoTime() - startTime < Duration.of(10, SECONDS).toNanos()) {
            if (wasCDICurrentFoundViaMES != null) {
                assertTrue("CDI.current returned null when called in a new Thread", wasCDICurrentFoundViaMES);
                return;
            }
            Thread.sleep(15);
        }

        LOGGER.info("About to throw exception");  
        assertTrue("The thread with CDI.current never completed", false);
    }

    public static void setWasCDICurrentFound(boolean b) {

        wasCDICurrentFoundViaMES = b;        
        LOGGER.info("Set test variable");  

    }

    public class CallCDICurrent implements Runnable {
 
        @Override
        public void run() {
            CDI cdi = CDI.current();
            LOGGER.info("Found CDI " + cdi);
            if (cdi != null) {
                CDICurrentTestServlet.setWasCDICurrentFound(true);
                LOGGER.info("Calling setter for test variable");
            } else {
                System.out.println("GREP 1 " + java.time.LocalDateTime.now());
                LOGGER.info("Calling setter for test variable");
            }
        }
    }
}
