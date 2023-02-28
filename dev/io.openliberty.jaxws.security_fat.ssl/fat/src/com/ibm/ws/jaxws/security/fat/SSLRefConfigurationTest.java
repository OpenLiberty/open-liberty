package com.ibm.ws.jaxws.security.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class SSLRefConfigurationTest extends AbstractJaxWsTransportSecuritySSLTest {

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
    public void testSSLRefInDefaultSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + SSLREF_IN_SSL_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    @Test
    public void testSSLRefInOutboundDefaultSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + SSLREF_IN_SSL_OUTBOUND_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml",
                       null);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    @Test
    public void testSSLRefInOutboundFilterSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + SSLREF_IN_SSL_OUTBOUND_FILTER_CONFIG, "basicAuthWithSSL_provider_web.xml",
                       null);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }
}
