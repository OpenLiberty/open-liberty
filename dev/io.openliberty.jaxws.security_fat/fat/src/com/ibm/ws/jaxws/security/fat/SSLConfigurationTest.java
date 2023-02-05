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

/*
 * Test cases for SSL configuration
 */
@RunWith(FATRunner.class)
public class SSLConfigurationTest extends AbstractJaxWsTransportSecurityTest {

	private static final String DEFAULT_SSL_SERVER_CONFIG = "defaultSSLConfiguration.xml";

	private static final String NO_SSL_CONFIG = "noSSLConfiguration.xml";

	protected static final String DEFAULT_SSL_WITHOUT_TRUST_STORE_SERVER_CONFIG = "noTrustStoreInDefaultSSLConfiguration.xml";

	static {
		invalidSSLHandshakeResps.add("SSLHandshakeException");
		invalidSSLHandshakeResps.add("java.security.cert.CertPathBuilderException");

		noSSLResps.add("NullPointerException");
		noSSLResps.add("ConnectException");

		dynamicUpdate = true;
	}

	@BeforeClass
	public static void beforeAllTests() throws Exception {
		buildDefaultApps();
		if (dynamicUpdate) {
			updateSingleFileInServerRoot("server.xml", "serverConfigs/" + DEFAULT_SSL_SERVER_CONFIG);
			lastServerConfig = "serverConfigs/" + DEFAULT_SSL_SERVER_CONFIG;

			updateSingleFileInServerRoot(WEB_XML_IN_PROVIDER_WAR, "basicAuthWithSSL_provider_web.xml");

			server.startServer("JaxWsTransportSecurityServer.log");
			server.setMarkToEndOfLog();
		}
	}

	@AfterClass
	public static void afterAllTests() throws Exception {
		if (dynamicUpdate) {
			if (server != null && server.isStarted()) {
				server.stopServer("CWPKI0823E"); // trust stop server to ensure server is stopped
			}
		}
	}

	@After
	public void afterTest() throws Exception {
		if (!dynamicUpdate) {
			if (server != null && server.isStarted()) {
				server.stopServer("CWPKI0823E"); // trust stop server to make sure the server is stopped.
			}
		}
	}

	// 1 Default SSL configuration
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testDefaultSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_SSL_SERVER_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testDefaultSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_SSL_SERVER_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testDefaultSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_SSL_SERVER_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	// 5 No SSL configuration
	@AllowedFFDC({ "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" })
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testNoSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + NO_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, noSSLResps, null);
	}

	@AllowedFFDC({ "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" })
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testNoSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + NO_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, noSSLResps, null);
	}

	@AllowedFFDC({ "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" })
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	@Test
	public void testNoSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + NO_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, noSSLResps, null);
	}

	// private static final String DEFAULT_SSL_WITHOUT_TRUST_STORE_SERVER_CONFIG =
	// "noTrustStoreInDefaultSSLConfiguration.xml";

	private static final String CUSTOMIZE_SSL_CONFIG = "customizeSSLConfiguration.xml";

	// private static final String NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG =
	// "noValidTrustCertInCustomizeSSLConfiguration.xml";

	private static final String NO_TRUST_STORE_IN_CUSTOMIZE_SSL_CONFIG = "noTrustStoreInCustomizeSSLConfiguration.xml";

	// 1 No trustStore in default SSl configuration
	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testNoTrustStoreInDefaultSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_SSL_WITHOUT_TRUST_STORE_SERVER_CONFIG,
				"basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testNoTrustStoreInDefaultSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_SSL_WITHOUT_TRUST_STORE_SERVER_CONFIG,
				"basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testNoTrustStoreInDefaultSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + DEFAULT_SSL_WITHOUT_TRUST_STORE_SERVER_CONFIG,
				"basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	// 2 No valid certification in trustStore of customize SSL configuration
	@AllowedFFDC({ "sun.security.validator.ValidatorException", "java.security.cert.CertPathBuilderException",
			"com.ibm.security.cert.IBMCertPathBuilderException" })
	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testNoValidTrustCertInCustomizeSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG,
				"basicAuthWithSSL_provider_web.xml", "bindings/customizeSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, invalidSSLHandshakeResps, null);
	}

	@AllowedFFDC({ "sun.security.validator.ValidatorException", "java.security.cert.CertPathBuilderException",
			"com.ibm.security.cert.IBMCertPathBuilderException" })
	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testNoValidTrustCertInCustomizeSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG,
				"basicAuthWithSSL_provider_web.xml", "bindings/customizeSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, invalidSSLHandshakeResps, null);
	}

	@AllowedFFDC({ "sun.security.validator.ValidatorException", "java.security.cert.CertPathBuilderException",
			"com.ibm.security.cert.IBMCertPathBuilderException" })
	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testNoValidTrustCertInCustomizeSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG,
				"basicAuthWithSSL_provider_web.xml", "bindings/customizeSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, invalidSSLHandshakeResps, null);
	}

	// 3 inexistent customize and fall back on default SSL configuration
	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testInexistentCustomizeButFallbackOnDefaultSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
				"bindings/inexistentCustomizeButFallbackOnDefaultSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testInexistentCustomizeButFallbackOnDefaultSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
				"bindings/inexistentCustomizeButFallbackOnDefaultSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testInexistentCustomizeButFallbackOnDefaultSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
				"bindings/inexistentCustomizeButFallbackOnDefaultSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	// From Lite: 3 Customize SSL configuration
	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testCustomizeSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
				"bindings/customizeSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Test
	@Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
	public void testCustomizeSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
				"bindings/customizeSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	public void testCustomizeSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
				"bindings/customizeSSLConfig.xml");

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

}
