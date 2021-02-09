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

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 * The testcase will send request to a servlet in TestServlet40.war
 * which has ApplicationMBean and checks for the status of the
 * TestBadServletContextListener.war web application
 * which is expected to fail to start due to runtime exception.
 *
 * PASS: if status is INSTALLED
 *
 */
@RunWith(FATRunner.class)
public class WCApplicationMBeanStatusTest extends LoggingTest {

    private final static String CLASSNAME = WCApplicationMBeanStatusTest.class.getName();
    private final static Logger LOG = Logger.getLogger(CLASSNAME);

    @ClassRule
    public static SharedServer SHARED_SERVER;

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * @throws Exception
     *
     *                       The test case expects start of the application fails due to runtime
     *                       exception on server start, add the expected ffdc.
     *                       FFDC1015I: ... RuntimeException ... com.ibm.ws.webcontainer.webapp.WebApp.notifyServletContextCreated 1341
     *                       FFDC1015I: ... RuntimeException ... com.ibm.ws.webcontainer.osgi.DynamicVirtualHost startWebApp
     *                       FFDC1015I: ... Exception ... com.ibm.ws.webcontainer.osgi.WebContainer$3 startModule async
     */
    @BeforeClass
    @ExpectedFFDC({ "java.lang.Exception", "java.lang.RuntimeException" })
    public static void setUp() throws Exception {
        if (componenttest.rules.repeater.JakartaEE9Action.isActive()) {
            LOG.info("Setup : Prepare EE9 server");
            if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
                SHARED_SERVER.getLibertyServer().stopServer("SRVE0283E:.*", "SRVE0265E:.*");
                LOG.info("Setup : Prepare EE9, STOP the current server");
            }
            SHARED_SERVER = new SharedServer("servlet50_stopAppStartListenerExceptionServer");
        }
        else {
            LOG.info("Setup : Prepare EE8 server");
            SHARED_SERVER = new SharedServer("servlet40_stopAppStartListenerExceptionServer");
        }

        LOG.info("Setup : add TestBadServletContextListener.war to the server if not already present.");

        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(),
                                                  "TestBadServletContextListener.war",
                                                  false,
                                                  "testbadscl.war.listener");
        LOG.info("Setup : TestBadServletContextListener.war added to the server");

        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(),
                                                  "TestServlet40.war",
                                                  true,
                                                  "testservlet40.war.servlets");

        LOG.info("Setup : TestServlet40.war added to the server");

        SHARED_SERVER.startIfNotStarted();

        // The ApplicationMBean is added in TestServlet40
        WCApplicationHelper.waitForAppStart("TestServlet40", CLASSNAME, SHARED_SERVER.getLibertyServer());

        LOG.info("Setup : TestServlet40.war started on the server");

        // The failing servletcontext is added in TestBadServletContextListener
        WCApplicationHelper.waitForAppFailStart("TestBadServletContextListener", CLASSNAME,
                                                SHARED_SERVER.getLibertyServer());

        LOG.info("Setup : TestBadServletContextListener failed to start on the server as expected");

        // wait for the three ffdc which are listed above
        SHARED_SERVER.getLibertyServer().waitForMultipleStringsInLog(3, "FFDC1015I");

        LOG.info("Setup : complete, ready for Tests");
    }

    /**
     * @throws Exception
     *
     *                       The test case expects start of the application fails due to runtime
     *                       exception in listener, add the expected errors
     *                       E SRVE0283E: Exception caught while initializing context: java.lang.RuntimeException: ..
     *                       E SRVE0265E: Error occured while notifying listeners of web application start: ..
     */
    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer("SRVE0283E:.*", "SRVE0265E:.*");
        }
        
        //Null out the static server to prevent an auto-restart upon repeated tests.
        SHARED_SERVER = null;
    }

    /**
     * @throws Exception
     *
     *                       The result in logs should be INSTALLED
     */
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) 
    public void test_AppStatusInstalled() throws Exception {
        this.verifyResponse("/TestServlet40/ApplicationMBeanServlet?AppName=TestBadServletContextListener", "PASS: INSTALLED");
    }

    /**
     * @throws Exception
     *
     *                       The result in logs should be STARTED
     */
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) 
    public void test_AppStatusStarted() throws Exception {
        this.verifyResponse("/TestServlet40/ApplicationMBeanServlet?AppName=TestServlet40", "PASS: STARTED");
    }

    
    /**
     * Rerun the above 2 tests using the default setting of stopappstartuponlistenerexception which is true in servlet 5
     * i.e the main difference is the setting of stopappstartuponlistenerexception NOT explicitly set in the server.xml in servlet 5 (or EE9)
     * TestBadServletContextListener.war should fail to start.
     * 
     * Note: though the tests are skipped when running under NO_MODIFICATION (i.e EE8/servlet4), the junit still reports the tests SUCCESS.
     *        This is due to the junit old framework.
     *        Example from output.txt during EE8 tests:
     *        
     *                  Skipping test method test_AppStatusInstalled_servlet5_default on action NO_MODIFICATION_ACTION
     *                  Skipping test method test_AppStatusStarted_servlet5_default on action NO_MODIFICATION_ACTION
     */
    
    
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION) 
    public void test_AppStatusInstalled_servlet5_default() throws Exception {
        this.verifyResponse("/TestServlet40/ApplicationMBeanServlet?AppName=TestBadServletContextListener", "PASS: INSTALLED");
    }
    
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void test_AppStatusStarted_servlet5_default() throws Exception {
        this.verifyResponse("/TestServlet40/ApplicationMBeanServlet?AppName=TestServlet40", "PASS: STARTED");
    }

}
