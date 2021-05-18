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

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.cdi.api.fat.apps.current.extension.MyDeploymentVerifier;

import componenttest.app.FATServlet;

@WebServlet("/")
public class CDICurrentTestServlet extends FATServlet {

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

        assertNotNull(CDI.current()); //Test outside a new thread just for completeness.

        managedExecutorService.submit(new CallCDICurrent());

        int i = 40;

        while (i > 0) {
            i--;
            if (wasCDICurrentFoundViaMES != null) {
                assertTrue("CDI.current returned null when called in a new Thread", wasCDICurrentFoundViaMES);
                return;
            }
            Thread.sleep(5);
        }

        assertTrue("The thread with CDI.current never completed", false);
    }


    public class CallCDICurrent implements Runnable {
 
        @Override
        public void run() {
            CDI cdi = CDI.current();
            System.out.println("GREP MOE + " + cdi);
            if (cdi != null) {
                CDICurrentTestServlet.wasCDICurrentFoundViaMES = true;
            } else {
                CDICurrentTestServlet.wasCDICurrentFoundViaMES = false;
            }
        }
    }
}
