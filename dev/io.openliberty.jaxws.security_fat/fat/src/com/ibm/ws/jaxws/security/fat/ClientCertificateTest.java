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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

/**
 * Test cases for client certificate on JAX-WS transport security
 */
@RunWith(FATRunner.class)
public class ClientCertificateTest extends AbstractJaxWsTransportSecurityTest {
	private static final List<String> INEXISTENT_ALIAS_SERVER_INFO = new ArrayList<String>();

	protected static final String DEFAULT_CLIENT_CERT_CONFIG = "defaultClientCertConfiguration.xml";

	protected static final String NO_APP_SECURITY_CONFIG = "noAppSecurityFeature.xml";

	protected static final List<String> CERT_NOT_TRUST_SERVER_INFO = new ArrayList<String>();

	protected static final List<String> NOT_AUTHORIZAED_SERVER_INFO = new ArrayList<String>();

	protected static final String WITH_CLIENT_ALIAS_CONFIG = "withAliasClientCertConfiguration.xml";

	protected static final String PATCHY_SERVER_TRUST_STORE_CONFIG = "patchyServerTrustStoreConfiguration.xml";

	protected static final String SCHEMA = "https";

	protected static final int PORT = server.getHttpDefaultSecurePort();

	static {
		INEXISTENT_ALIAS_SERVER_INFO.add("CWPKI0023E.*");
		INEXISTENT_ALIAS_SERVER_INFO.add("CWWKW0601E.*");

		NOT_AUTHORIZAED_SERVER_INFO.add("CWWKS9104A.*employee");

		CERT_NOT_TRUST_SERVER_INFO.add("CWPKI0022E.* | CWWKO0801E.*");

		dynamicUpdate = false;
	}

	@BeforeClass
	public static void beforeAllTests() throws Exception {

		buildDefaultApps();
		if (dynamicUpdate) {
			updateSingleFileInServerRoot("server.xml", "serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG);
			lastServerConfig = "serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG;

			updateSingleFileInServerRoot(WEB_XML_IN_PROVIDER_WAR, "clientCert_provider_web.xml");

			server.startServer("JaxWsTransportSecurityServer.log");
			server.setMarkToEndOfLog();
		}
	}

	@AfterClass
	public static void afterAllTests() throws Exception {
		if (dynamicUpdate) {
			if (server != null && server.isStarted()) {
				server.stopServer("CWPKI0023E.*", "CWWKW0601E.*", "CWPKI0022E.*", "CWWKO0801E.*");
			}
		}
	}

	@After
	public void afterTest() throws Exception {
		if (!dynamicUpdate) {
			if (server != null && server.isStarted()) {
				server.stopServer("CWPKI0023E.*", "CWWKW0601E.*", "CWPKI0022E.*", "CWWKO0801E.*"); // trust stop server to ensure server
																					// is stopped
			}
		}
	}

	// 1 Valid client certificate
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testValidClientCertPOJO() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);

	}

	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testValidClientCertStateless() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/employee/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testValidClientCertSingleton() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	// 2 Inexistent client certificate
	@AllowedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testInexistentClientCertPOJO() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/inexistentCertAlias.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);

	}

	@AllowedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testInexistentClientCertStateless() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/inexistentCertAlias.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/employee/employStatelessService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);
	}

	@AllowedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testInexistentClientCertSingleton() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/inexistentCertAlias.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "CWPKI0023E", INEXISTENT_ALIAS_SERVER_INFO);
	}

	// private static final String WITH_CLIENT_ALIAS_CONFIG =
	// "withAliasClientCertConfiguration.xml";

	// private static final String PATCHY_SERVER_TRUST_STORE_CONFIG =
	// "patchyServerTrustStoreConfiguration.xml";

	private static final int WAIT_TIME_OUT = 10 * 1000;

	// 1 Override alias configured in ssl element with customize one
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testOverrideAliasWithCustomizeOnePOJO() throws Exception {
		prepareForTest("serverConfigs/" + WITH_CLIENT_ALIAS_CONFIG, "clientCert_provider_web.xml",
				"bindings/overrideCertAlias.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/manager/employPojoService");
		runTest(params, "403", NOT_AUTHORIZAED_SERVER_INFO);

	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testOverrideAliasWithCustomizeOneStateless() throws Exception {
		prepareForTest("serverConfigs/" + WITH_CLIENT_ALIAS_CONFIG, "clientCert_provider_web.xml",
				"bindings/overrideCertAlias.xml");

		RequestParams params = new RequestParams("manager", "stateless", SCHEMA, PORT,
				"/manager/employStatelessService");
		runTest(params, "From other bean: Hello, manager from SayHelloSingletonService", null);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testOverrideAliasWithCustomizeOneSingleton() throws Exception {
		prepareForTest("serverConfigs/" + WITH_CLIENT_ALIAS_CONFIG, "clientCert_provider_web.xml",
				"bindings/overrideCertAlias.xml");

		RequestParams params = new RequestParams("manager", "singleton", SCHEMA, PORT,
				"/manager/employSingletonService");
		runTest(params, "From other bean: Hello, manager from SayHelloStatelessService", null);
	}

	// 2 Cert in client keystore but not in server's trustStore
	@AllowedFFDC({ "java.security.cert.CertificateException", "java.security.cert.CertPathBuilderException",
			"sun.security.validator.ValidatorException", "com.ibm.security.cert.IBMCertPathBuilderException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testCertInClientKeyStoreButNotInServerTrustStorePOJO() throws Exception {
		prepareForTest("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, "clientCert_provider_web.xml",
				"bindings/certInClientKSButNotInServerTS.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "Could not send Message", CERT_NOT_TRUST_SERVER_INFO);

	}

	@AllowedFFDC({ "java.security.cert.CertificateException", "java.security.cert.CertPathBuilderException",
			"sun.security.validator.ValidatorException", "com.ibm.security.cert.IBMCertPathBuilderException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testCertInClientKeyStoreButNotInServerTrustStoreStateless() throws Exception {
		prepareForTest("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, "clientCert_provider_web.xml",
				"bindings/certInClientKSButNotInServerTS.xml");

		RequestParams params = new RequestParams("manager", "stateless", SCHEMA, PORT,
				"/manager/employStatelessService");
		runTest(params, "Could not send Message", CERT_NOT_TRUST_SERVER_INFO);
	}

	@AllowedFFDC({ "java.security.cert.CertificateException", "java.security.cert.CertPathBuilderException",
			"sun.security.validator.ValidatorException", "com.ibm.security.cert.IBMCertPathBuilderException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testCertInClientKeyStoreButNotInServerTrustStoreSingleton() throws Exception {
		prepareForTest("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, "clientCert_provider_web.xml",
				"bindings/certInClientKSButNotInServerTS.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "Could not send Message", CERT_NOT_TRUST_SERVER_INFO);
	}

	// 3 JaxWsSecurity feature re-enable
	@AllowedFFDC({ "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testReEnableJaxWsSecurityFeaturePOJO() throws Exception {
		// With appSecurity feature
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);

		// Remove appSecurity feature
		server.setMarkToEndOfLog();
		updateServerConfigFile("serverConfigs/" + NO_APP_SECURITY_CONFIG);
		waitForClientAppUpdate();
		runTest(params, "Could not send Message", null);

		// reAdd appSecurity feature
		server.setMarkToEndOfLog();
		updateServerConfigFile("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG);
		// This message never occurs. Commenting out to avoid 2 minute wait.
		/*
		 * String appSecurityUpdate =
		 * server.waitForStringInLogUsingMark("CWWKS9112A.*"); if (null ==
		 * appSecurityUpdate) { Log.warning(ClientCertificateTest.class,
		 * "The web application security settings have not changed."); }
		 */
		waitForClientAppUpdate();
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@AllowedFFDC({ "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testReEnableJaxWsSecurityFeatureStateless() throws Exception {
		// With appSecurity feature
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/employee/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);

		// Remove appSecurity feature
		server.setMarkToEndOfLog();
		updateServerConfigFile("serverConfigs/" + NO_APP_SECURITY_CONFIG);
		waitForClientAppUpdate();
		runTest(params, "Could not send Message", null);

		// reAdd appSecurity feature
		server.setMarkToEndOfLog();
		updateServerConfigFile("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG);
		// This message never occurs. Commenting out to avoid 2 minute wait.
		/*
		 * String appSecurityUpdate =
		 * server.waitForStringInLogUsingMark("CWWKS9112A.*"); if (null ==
		 * appSecurityUpdate) { Log.warning(ClientCertificateTest.class,
		 * "The web application security settings have not changed."); }
		 */
		waitForClientAppUpdate();
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@AllowedFFDC({ "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" })
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testReEnableJaxWsSecurityFeatureSingleton() throws Exception {
		// With appSecurity feature
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);

		// Remove appSecurity feature
		server.setMarkToEndOfLog();
		updateServerConfigFile("serverConfigs/" + NO_APP_SECURITY_CONFIG);
		waitForClientAppUpdate();
		runTest(params, "Could not send Message", null);

		// reAdd appSecurity feature
		server.setMarkToEndOfLog();
		updateServerConfigFile("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG);
		// This message never occurs. Commenting out to avoid 2 minute wait.
		/*
		 * String appSecurityUpdate =
		 * server.waitForStringInLogUsingMark("CWWKS9112A.*"); if (null ==
		 * appSecurityUpdate) { Log.warning(ClientCertificateTest.class,
		 * "The web application security settings have not changed."); }
		 */
		waitForClientAppUpdate();
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	private void waitForClientAppUpdate() {
		server.waitForStringInLogUsingMark("CWWKZ0001I.*TransportSecurityClient | CWWKZ0003I.*TransportSecurityClient",
				WAIT_TIME_OUT);
	}

	// From Lite: 3 Valid certificate but not authorized
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidCertButNotAuthorizedPOJO() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/manager/employPojoService");
		runTest(params, "403", NOT_AUTHORIZAED_SERVER_INFO);

	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidCertButNotAuthorizedStateless() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/manager/employStatelessService");
		runTest(params, "403", NOT_AUTHORIZAED_SERVER_INFO);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testValidCertButNotAuthorizedSingleton() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/validCertAlias.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/manager/employSingletonService");
		runTest(params, "403", NOT_AUTHORIZAED_SERVER_INFO);
	}

	// From Lite: 4 enable hostName verifier
	@Test
	@Mode(Mode.TestMode.FULL)
	public void testEnableCNCheckPOJO() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/enableCNCheck.xml");

		RequestParams params = new RequestParams("employee", "pojo", SCHEMA, PORT, "/employee/employPojoService");
		runTest(params, "disableCNCheck", null);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testEnableCNCheckStateless() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/enableCNCheck.xml");

		RequestParams params = new RequestParams("employee", "stateless", SCHEMA, PORT,
				"/employee/employStatelessService");
		runTest(params, "disableCNCheck", null);
	}

	@Test
	@Mode(Mode.TestMode.FULL)
	public void testEnableCNCheckSingleton() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
				"bindings/enableCNCheck.xml");

		RequestParams params = new RequestParams("employee", "singleton", SCHEMA, PORT,
				"/employee/employSingletonService");
		runTest(params, "disableCNCheck", null);
	}

}
