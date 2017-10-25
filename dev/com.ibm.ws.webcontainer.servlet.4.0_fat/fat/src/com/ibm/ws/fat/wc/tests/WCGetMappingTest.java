/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;

/**
 * These are tests for the Servlet 4.0 HttpServletRequest.getMapping()
 * functionality.
 *
 */
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
		// TODO Auto-generated method stub
		return SHARED_SERVER;
	}

	@BeforeClass
	public static void setUp() throws Exception {

		LOG.info("Setup : add TestGetMapping to the server if not already present.");

		WCApplicationHelper.addWarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestGetMapping.war", false,
				"testgetmapping.war.servlets");

		if (!SHARED_SERVER.getLibertyServer().isStarted())
			SHARED_SERVER.getLibertyServer().startServer();

		LOG.info("Setup : wait for message to indicate app has started");

		SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* TestGetMapping", 10000);

		LOG.info("Setup : ready to start tests");

	}

	@AfterClass
	public static void testCleanup() throws Exception {

		SHARED_SERVER.getLibertyServer().stopServer(null);
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
	public void test_HttpServletRequestGetMapping_ContextRootMapping_Include() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathIncMatch?dispatchPath=/",
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
	public void test_HttpServletRequestGetMapping_ContextRootMapping_Async() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathAsyncMatch?dispatchPath=/",
				"Mapping values: mappingMatch: CONTEXT_ROOT matchValue:  pattern:  servletName: GetMappingTestServlet");
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
	public void test_HttpServletRequestGetMapping_PathMapping_Include() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
				"/TestGetMapping/pathIncMatch?dispatchPath=pathMatch/testPath",
				"ServletMapping values: mappingMatch: PATH matchValue: testPath pattern: /pathMatch/* servletName: GetMappingTestServlet");
	}

	/**
	 * Test to ensure that a request that uses a path mapping has the correct
	 * values returned from a call to the HttpServletRequest.getMapping() API.
	 *
	 * @throws Exception
	 */
	@Test
	public void test_HttpServletRequestGetMapping_PathMapping_Async() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
				"/TestGetMapping/pathAsyncMatch?dispatchPath=pathMatch/testPath",
				"ServletMapping values: mappingMatch: PATH matchValue: testPath pattern: /pathMatch/* servletName: GetMappingTestServlet");
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
	public void test_HttpServletRequestGetMapping_DefaultMapping_Include() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/TestGetMapping/pathIncMatch?dispatchPath=invalid",
				"ServletMapping values: mappingMatch: DEFAULT matchValue:  pattern: / servletName: GetMappingTestServlet");
	}

	/**
	 * Test to ensure that a request that uses a default mapping has the correct
	 * values returned from a call to the HttpServletRequest.getMapping() API.
	 *
	 * @throws Exception
	 */
	@Test
	public void test_HttpServletRequestGetMapping_DefaultMapping_Async() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
				"/TestGetMapping/pathAsyncMatch?dispatchPath=invalid",
				"ServletMapping values: mappingMatch: DEFAULT matchValue:  pattern: / servletName: GetMappingTestServlet");
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
	public void test_HttpServletRequestGetMapping_ExactMapping_Include() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
				"/TestGetMapping/pathIncMatch?dispatchPath=exactMatch",
				"ServletMapping values: mappingMatch: EXACT matchValue: exactMatch pattern: /exactMatch servletName: GetMappingTestServlet");
	}

	/**
	 * Test to ensure that a request that uses an exact mapping has the correct
	 * values returned from a call to the HttpServletRequest.getMapping() API.
	 *
	 * @throws Exception
	 */
	@Test
	public void test_HttpServletRequestGetMapping_ExactMapping_Async() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
				"/TestGetMapping/pathAsyncMatch?dispatchPath=exactMatch",
				"ServletMapping values: mappingMatch: EXACT matchValue: exactMatch pattern: /exactMatch servletName: GetMappingTestServlet");
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
	public void test_HttpServletRequestGetMapping_ExtensionMapping_Include() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
				"/TestGetMapping/pathIncMatch?dispatchPath=extensionMatch.extension",
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
	public void test_HttpServletRequestGetMapping_ExtensionMapping_Async() throws Exception {
		SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(),
				"/TestGetMapping/pathAsyncMatch?dispatchPath=extensionMatch.extension",
				"ServletMapping values: mappingMatch: EXTENSION matchValue: extensionMatch pattern: *.extension servletName: GetMappingTestServlet");
	}

}
