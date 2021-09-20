/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * These are tests for the Servlet 4.0 HttpServletRequest.getMapping()
 * functionality.
 *
 */
@RunWith(FATRunner.class)
public class WCGetMappingTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());
    private static final String APP_NAME = "TestGetMapping";

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestGetMapping to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "testgetmapping.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCGetMappingTest.class.getSimpleName() + ".log");

        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/", "Mapping values: mappingMatch: CONTEXT_ROOT matchValue:  pattern:  servletName: GetMappingTestServlet");
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathFwdMatch?dispatchPath=/",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathIncMatch?dispatchPath=/",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=/",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathMatch/testPath",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathFwdMatch?dispatchPath=pathMatch/testPath",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathIncMatch?dispatchPath=pathMatch/testPath",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=pathMatch/testPath",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/invalid",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathFwdMatch?dispatchPath=invalid",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathIncMatch?dispatchPath=invalid",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=invalid",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/exactMatch",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathFwdMatch?dispatchPath=exactMatch",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathIncMatch?dispatchPath=exactMatch",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=exactMatch",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/extensionMatch.extension",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathFwdMatch?dispatchPath=extensionMatch.extension",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathIncMatch?dispatchPath=extensionMatch.extension",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathAsyncMatch?dispatchPath=extensionMatch.extension",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathNamedDispatcherFwdMatch",
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
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/pathNamedDispatcherIncMatch",
                                       "ServletMapping values: mappingMatch: EXACT matchValue: pathNamedDispatcherIncMatch pattern: /pathNamedDispatcherIncMatch servletName: GetMappingNamedDispatcherIncServlet");
    }
}
