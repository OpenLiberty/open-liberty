/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;

public class BeanDiscoveryModeNoneTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12BeanDiscoveryModeNoneServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Mode
    @Test
    @ExpectedFFDC({ "javax.servlet.UnavailableException", "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testcorrectExceptionThrown() throws Exception {
        this.verifyStatusCode("/beanDiscoveryModeNone/TestServlet", 404);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * Error 404: javax.servlet.UnavailableException: SRVE0319E: For the
             * [com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet] servlet,
             * com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet servlet class
             * was found, but a resource injection failure has occurred. The @Inject
             * java.lang.reflect.Field.testBean1 reference of type com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestBean1
             * for the null component in the beanDiscoveryModeNone.war module of the
             * beanDiscoveryModeNone application cannot be resolved.
             */
            SHARED_SERVER.getLibertyServer().stopServer("SRVE0319E", "CWNEN0035E", "CWOWB1008E");
        }
    }

}
