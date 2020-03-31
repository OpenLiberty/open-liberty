/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These are tests for the Servlet 4.0 HttpServletRequest.getMapping()
 * functionality.
 *
 */
@RunWith(FATRunner.class)
public class WCGetMappingTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_wcServer");

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
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestGetMapping to the server if not already present.");

        WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestGetMapping.war", false,
                                                  "testgetmapping.war.servlets");

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("TestGetMapping", WCGetMappingTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        SHARED_SERVER.getLibertyServer().stopServer();
    }

    /**
     * Test to ensure that a request that uses a context-root mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_ContextRootMapping() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/",
                                     "Mapping values: mappingMatch: CONTEXT_ROOT matchValue:  pattern:  servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses a context-root mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ContextRootMapping_Forward() throws Exception {
        Thread.sleep(1000);
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathFwdMatch?dispatchPath=/",
                                     "Mapping values: mappingMatch: CONTEXT_ROOT matchValue:  pattern:  servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses a context-root mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ContextRootMapping_Include() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathIncMatch?dispatchPath=/",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet");
    }

    /**
     * Test to ensure that a request that uses a context-root mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ContextRootMapping_Async() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathAsyncMatch?dispatchPath=/",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet");
    }

    /**
     * Test to ensure that a request that uses a path mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_PathMapping() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathMatch/testPath",
                                     "ServletMapping values: mappingMatch: PATH matchValue: testPath pattern: /pathMatch/* servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses a path mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_PathMapping_Forward() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathFwdMatch?dispatchPath=pathMatch/testPath",
                                     "ServletMapping values: mappingMatch: PATH matchValue: testPath pattern: /pathMatch/* servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses a path mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_PathMapping_Include() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathIncMatch?dispatchPath=pathMatch/testPath",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet");
    }

    /**
     * Test to ensure that a request that uses a path mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_PathMapping_Async() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathAsyncMatch?dispatchPath=pathMatch/testPath",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet");
    }

    /**
     * Test to ensure that a request that uses a default mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_DefaultMapping() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/invalid",
                                     "ServletMapping values: mappingMatch: DEFAULT matchValue:  pattern: / servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses a default mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_DefaultMapping_Forward() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathFwdMatch?dispatchPath=invalid",
                                     "ServletMapping values: mappingMatch: DEFAULT matchValue:  pattern: / servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses a default mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_DefaultMapping_Include() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathIncMatch?dispatchPath=invalid",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet");
    }

    /**
     * Test to ensure that a request that uses a default mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_DefaultMapping_Async() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathAsyncMatch?dispatchPath=invalid",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet");
    }

    /**
     * Test to ensure that a request that uses an exact mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_ExactMapping() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/exactMatch",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: exactMatch pattern: /exactMatch servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses an exact mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExactMapping_Forward() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathFwdMatch?dispatchPath=exactMatch",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: exactMatch pattern: /exactMatch servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses an exact mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExactMapping_Include() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathIncMatch?dispatchPath=exactMatch",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet");
    }

    /**
     * Test to ensure that a request that uses an exact mapping has the correct
     * values returned from a call to the HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExactMapping_Async() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathAsyncMatch?dispatchPath=exactMatch",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet");
    }

    /**
     * Test to ensure that a request that uses an extension mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    public void test_HttpServletRequestGetMapping_ExtensionMapping() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/extensionMatch.extension",
                                     "ServletMapping values: mappingMatch: EXTENSION matchValue: extensionMatch pattern: *.extension servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses an extension mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExtensionMapping_Forward() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathFwdMatch?dispatchPath=extensionMatch.extension",
                                     "ServletMapping values: mappingMatch: EXTENSION matchValue: extensionMatch pattern: *.extension servletName: GetMappingTestServlet");
    }

    /**
     * Test to ensure that a request that uses an extension mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExtensionMapping_Include() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathIncMatch?dispatchPath=extensionMatch.extension",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathIncMatch pattern: /pathIncMatch servletName: GetMappingIncServlet");
    }

    /**
     * Test to ensure that a request that uses an extension mapping has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_ExtensionMapping_Async() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathAsyncMatch?dispatchPath=extensionMatch.extension",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathAsyncMatch pattern: /pathAsyncMatch servletName: GetMappingAsyncServlet");
    }

    /**
     * Test to ensure that a request that uses named dispatcher has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_NamedDispatcher_Forward() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathNamedDispatcherFwdMatch",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathNamedDispatcherFwdMatch pattern: /pathNamedDispatcherFwdMatch servletName: GetMappingNamedDispatcherFwdServlet");
    }

    /**
     * Test to ensure that a request that uses named dispatcher has the
     * correct values returned from a call to the
     * HttpServletRequest.getMapping() API.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_HttpServletRequestGetMapping_NamedDispatcher_Include() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
                                     "/TestGetMapping/pathNamedDispatcherIncMatch",
                                     "ServletMapping values: mappingMatch: EXACT matchValue: pathNamedDispatcherIncMatch pattern: /pathNamedDispatcherIncMatch servletName: GetMappingNamedDispatcherIncServlet");
    }

}
