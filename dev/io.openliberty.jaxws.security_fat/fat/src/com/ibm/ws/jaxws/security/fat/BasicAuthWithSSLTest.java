/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.security.fat;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

/*
 * Test case for basic authentication with ssl configuration
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class BasicAuthWithSSLTest extends AbstractJaxWsTransportSecurityTest {
	protected static final String SERVER_CONFIG_FILE_NAME = "basicAuthWithSSL.xml";

	@BeforeClass
	public static void beforeAllTests() throws Exception {
		buildDefaultApps();
		updateSingleFileInServerRoot("server.xml", "serverConfigs/" + SERVER_CONFIG_FILE_NAME);
		lastServerConfig = "serverConfigs/" + SERVER_CONFIG_FILE_NAME;

		updateSingleFileInServerRoot(WEB_XML_IN_PROVIDER_WAR, "basicAuthWithSSL_provider_web.xml");

		server.startServer("JaxWsTransportSecurityServer.log");
		server.setMarkToEndOfLog();
	}

	@AfterClass
	public static void afterAllTests() throws Exception {
		if (server != null && server.isStarted()) {
			server.stopServer();
		}
	}

	// Valid name and valid encoded password
	@Test
	public void testValidNameAndValidEncodedPasswordPOJO() throws Exception {
		updateClientBndFile("bindings/validNameAndValidEncodedPwd.xml");

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/employee/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Test
	public void testValidNameAndValidEncodedPasswordStateless() throws Exception {
		updateClientBndFile("bindings/validNameAndValidEncodedPwd.xml");

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/employee/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	public void testValidNameAndValidEncodedPasswordSingleton() throws Exception {
		updateClientBndFile("bindings/validNameAndValidEncodedPwd.xml");

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/employee/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	// Invalid name
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testInvalidNamePOJO() throws Exception {
		updateClientBndFile("bindings/invalidName.xml");

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/employee/employPojoService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS1100A.*inexisteduser");
		runTest(params, "401", serverInfos);
	}

	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testInvalidNameStateless() throws Exception {
		updateClientBndFile("bindings/invalidName.xml");

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/employee/employStatelessService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS1100A.*inexisteduser");
		runTest(params, "401", serverInfos);
	}

	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testInvalidNameSingleton() throws Exception {
		updateClientBndFile("bindings/invalidName.xml");

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/employee/employSingletonService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS1100A.*inexisteduser");
		runTest(params, "401", serverInfos);
	}

	// Valid name and valid plain password but not authorized
	@Test
	public void testValidNameAndValidPlainPasswordButNOTAuthorizedPOJO() throws Exception {
		updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/manager/employPojoService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS9104A.*employee");
		runTest(params, "403", serverInfos);
	}

	@Test
	public void testValidNameAndValidPlainPasswordButNOTAuthorizedStateless() throws Exception {
		updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/manager/employStatelessService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS9104A.*employee");
		runTest(params, "403", serverInfos);
	}

	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testValidNameAndValidPlainPasswordButNOTAuthorizedSingleton() throws Exception {
		updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/manager/employSingletonService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS9104A.*employee");
		runTest(params, "403", serverInfos);
	}

	// Valid name and valid plain password
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidNameAndValidPlainPasswordPOJO() throws Exception {
		updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/employee/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidNameAndValidPlainPasswordStateless() throws Exception {
		updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/employee/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidNameAndValidPlainPasswordSingleton() throws Exception {
		updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/employee/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	// Valid name but invalid password
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidNameButInvalidPasswordPOJO() throws Exception {
		updateClientBndFile("bindings/validNameButInvalidPwd.xml");

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/employee/employPojoService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS1100A.*employee");
		runTest(params, "401", serverInfos);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidNameButInvalidPasswordStateless() throws Exception {
		updateClientBndFile("bindings/validNameButInvalidPwd.xml");

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/employee/employStatelessService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS1100A.*employee");
		runTest(params, "401", serverInfos);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidNameButInvalidPasswordSingleton() throws Exception {
		updateClientBndFile("bindings/validNameButInvalidPwd.xml");

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/employee/employSingletonService");
		List<String> serverInfos = new ArrayList<String>(1);
		serverInfos.add("CWWKS1100A.*employee");
		runTest(params, "401", serverInfos);
	}
}
