/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test Servlet 5 native jakarta.servlet (i.e not transformer)
 * Should only run in EE9.
 * The target app is servlet 5.0 version with new namespace.
 * Request to the simple snoop servlet 50 version which test several jakarta.servlet API (along with the new web-app web.xml namespace)
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
public class WC5JakartaServletTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WC5JakartaServletTest.class.getName());
    private static final String APP_NAME = "Servlet5TestApp";

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet50_wcServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void before() throws Exception {
        LOG.info("Setup : add " + APP_NAME + " war to the server if not already present.");

        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(), APP_NAME + ".war", true, "servlet5snoop.war.servlets");
        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart(APP_NAME, WC5JakartaServletTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        SHARED_SERVER.getLibertyServer().stopServer();
    }

    /**
     * Request a simple snoop servlet.
     *
     * @throws Exception
     */
    @Test
    public void testSimple_Servlet50() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/Servlet5TestApp/snoop5", "END OF SNOOP 5. TEST PASS");
    }
}