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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

/**
 * Test cases for client certificate fall back on basic-auth
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class ClientCertFailOverToBasicAuthTest extends AbstractJaxWsTransportSecurityTest {

	protected static final String FAIL_OVER_TO_BASIC_AUTH_CONFIG = "clientCertFailOverToBasicAuthConfiguration.xml";

	protected static final String PATCHY_SERVER_TRUST_STORE_CONFIG = "patchyServerTrustStoreConfiguration.xml";

	protected static final List<String> INEXISTENT_ALIAS_SERVER_INFO = new ArrayList<String>();

	protected static final List<String> NOT_AUTHORIZAED_SERVER_INFO = new ArrayList<String>();

	protected static final List<String> AUTHENTICATION_FAILED_SERVER_INFO = new ArrayList<String>();

	protected static final String SCHEMA = "https";

	protected static final int PORT = server.getHttpDefaultSecurePort();

	protected static final boolean checkAppUpdate = false;

	static {
		INEXISTENT_ALIAS_SERVER_INFO.add("CWPKI0023E.*");
		INEXISTENT_ALIAS_SERVER_INFO.add("CWWKW0601E.*");

		NOT_AUTHORIZAED_SERVER_INFO.add("CWWKS9104A.*employee");

		AUTHENTICATION_FAILED_SERVER_INFO.add("CWWKS1100A.*manager");
	}

	@BeforeClass
	public static void beforeAllTests() throws Exception {
		buildDefaultApps();
		updateSingleFileInServerRoot("server.xml", "serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG);
		lastServerConfig = "serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG;

		updateSingleFileInServerRoot(WEB_XML_IN_PROVIDER_WAR, "clientCert_provider_web.xml");

		server.startServer("JaxWsTransportSecurityServer.log");
		server.setMarkToEndOfLog();
	}

	@AfterClass
	public static void afterAllTests() throws Exception {
		if (server != null && server.isStarted()) {
			server.stopServer("CWWKW0601E", "CWPKI0023E");
		}
	}

	// 1 Inexistent client cert but valid basic-auth
	@ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Test
	public void testInexistentClientCertButValidBasicAuthPOJO() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/inexistentCertButValidBA.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);

	}

	@ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Test
	public void testInexistentClientCertButValidBasicAuthStateless() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/inexistentCertButValidBA.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/employee/employStatelessService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);
	}

	@ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Test
	public void testInexistentClientCertButValidBasicAuthSingleton() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/inexistentCertButValidBA.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);
	}

	// 2 existent client cert but invalid basic-auth
	@Test
	public void testExistentClientCertButInvalidBasicAuthPOJO() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/existentCertButInvalidBA.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);

	}

	@Test
	public void testExistentClientCertButInvalidBasicAuthStateless() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/existentCertButInvalidBA.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/employee/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	public void testExistentClientCertButInvalidBasicAuthSingleton() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/existentCertButInvalidBA.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	// 3 No client cert configured but valid basic auth
	@Test
	public void testNoClientCertConfiguredButValidBasicAuthPOJO() throws Exception {
		updateServerConfigFile("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/noAliasConfiguredButValidBA.xml");

		RequestParams params = new RequestParams("manager", "pojo", SCHEMA, PORT, "/manager/employPojoService");
		runTest(params, "Hello, manager from SayHelloPojoService", null);

	}

	@Test
	public void testNoClientCertConfiguredButValidBasicAuthStateless() throws Exception {
		updateServerConfigFile("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/noAliasConfiguredButValidBA.xml");

		RequestParams params = new RequestParams("manager", "stateless", SCHEMA, PORT,
				"/manager/employStatelessService");
		runTest(params, "From other bean: Hello, manager from SayHelloSingletonService", null);
	}

	@Test
	public void testNoClientCertConfiguredButValidBasicAuthSingleton() throws Exception {
		updateServerConfigFile("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/noAliasConfiguredButValidBA.xml");

		RequestParams params = new RequestParams("manager", "singleton", SCHEMA, PORT,
				"/manager/employSingletonService");
		runTest(params, "From other bean: Hello, manager from SayHelloStatelessService", null);
	}

	// 1 Inexistent client cert and invalid basic-auth
	@ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testInexistentClientCertAndInvalidBasicAuthPOJO() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/inexistentCertAndInvalidBA.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);

	}

	@ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testInexistentClientCertAndInvalidBasicAuthStateless() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/inexistentCertAndInvalidBA.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/employee/employStatelessService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);
	}

	@ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testInexistentClientCertAndInvalidBasicAuthSingleton() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/inexistentCertAndInvalidBA.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);
	}

	// 2 existent client cert and valid basic-auth
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testExistentClientCertAndValidBasicAuthPOJO() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/existentCertAndValidBA.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);

	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testExistentClientCertAndValidBasicAuthStateless() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/existentCertAndValidBA.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/employee/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testExistentClientCertAndValidBasicAuthSingleton() throws Exception {
		updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/existentCertAndValidBA.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	// 6 No client cert configured and invalid basic auth
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testNoClientCertConfiguredAndInvalidBasicAuthPOJO() throws Exception {
		updateServerConfigFile("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/noAliasConfiguredAndInvalidBA.xml");

		RequestParams params = new RequestParams("manager", "pojo", SCHEMA, PORT, "/manager/employPojoService");
		runTest(params, "401", AUTHENTICATION_FAILED_SERVER_INFO);

	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testNoClientCertConfiguredAndInvalidBasicAuthStateless() throws Exception {
		updateServerConfigFile("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/noAliasConfiguredAndInvalidBA.xml");

		RequestParams params = new RequestParams("manager", "stateless", SCHEMA, PORT,
				"/manager/employStatelessService");
		runTest(params, "401", AUTHENTICATION_FAILED_SERVER_INFO);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testNoClientCertConfiguredAndInvalidBasicAuthSingleton() throws Exception {
		updateServerConfigFile("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, checkAppUpdate);
		updateClientBndFile("bindings/noAliasConfiguredAndInvalidBA.xml");

		RequestParams params = new RequestParams("manager", "singleton", SCHEMA, PORT,
				"/manager/employSingletonService");
		runTest(params, "401", AUTHENTICATION_FAILED_SERVER_INFO);
	}
}
