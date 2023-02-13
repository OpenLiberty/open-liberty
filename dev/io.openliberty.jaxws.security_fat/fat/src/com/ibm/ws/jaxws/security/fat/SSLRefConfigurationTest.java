package com.ibm.ws.jaxws.security.fat;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(Mode.TestMode.FULL)
public class SSLRefConfigurationTest extends AbstractJaxWsTransportSecurityTest {

	private static final String SSLREF_IN_SSL_DEFAULT_CONFIG = "sslRefInSSLDefaultConfiguration.xml";

	private static final String SSLREF_IN_SSL_OUTBOUND_DEFAULT_CONFIG = "sslRefInSSLOutboundDefaultConfiguration.xml";

	private static final String SSLREF_IN_SSL_OUTBOUND_FILTER_CONFIG = "sslRefInSSLOutboundFilterConfiguration.xml";

	static {
		invalidSSLHandshakeResps.add("SSLHandshakeException");
		invalidSSLHandshakeResps.add("java.security.cert.CertPathBuilderException");

		noSSLResps.add("NullPointerException");
		noSSLResps.add("ConnectException");
	}

	@BeforeClass
	public static void beforeAllTests() throws Exception {
		if (server == null)
			server = LibertyServerFactory.getLibertyServer("JaxWsTransportSecurityServer");

		buildDefaultApps();
		if (dynamicUpdate) {
			updateSingleFileInServerRoot("server.xml", "serverConfigs/" + SSLREF_IN_SSL_DEFAULT_CONFIG);
			lastServerConfig = "serverConfigs/" + SSLREF_IN_SSL_DEFAULT_CONFIG;

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

	@Test
	public void testSSLRefInDefaultSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Test
	public void testSSLRefInDefaultSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	public void testSSLRefInDefaultSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	@Test
	public void testSSLRefInOutboundDefaultSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_OUTBOUND_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml",
				null);

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Test
	public void testSSLRefInOutboundDefaultSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_OUTBOUND_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml",
				null);

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	public void testSSLRefInOutboundDefaultSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_OUTBOUND_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml",
				null);

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}

	@Test
	public void testSSLRefInOutboundFilterSSLConfigPOJO() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_OUTBOUND_FILTER_CONFIG, "basicAuthWithSSL_provider_web.xml",
				null);

		RequestParams params = new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employPojoService");
		runTest(params, "Hello, employee from SayHelloPojoService", null);
	}

	@Test
	public void testSSLRefInOutboundFilterSSLConfigStateless() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_OUTBOUND_FILTER_CONFIG, "basicAuthWithSSL_provider_web.xml",
				null);

		RequestParams params = new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employStatelessService");
		runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
	}

	@Test
	public void testSSLRefInOutboundFilterSSLConfigSingleton() throws Exception {
		prepareForTest("serverConfigs/" + SSLREF_IN_SSL_OUTBOUND_FILTER_CONFIG, "basicAuthWithSSL_provider_web.xml",
				null);

		RequestParams params = new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(),
				"/unauthorized/employSingletonService");
		runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
	}
}
