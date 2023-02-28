package com.ibm.ws.jaxws.security.fat;

import java.util.ArrayList;
import java.util.Arrays;
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
 * Test cases for SSL configuration with no Trust Store in server.xml
 */
@Mode(Mode.TestMode.FULL)
@RunWith(FATRunner.class)
public class SSLConfigurationNoTrustStoreTest extends AbstractJaxWsTransportSecuritySSLTest {

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
            updateSingleFileInServerRoot("server.xml", "serverConfigs/" + NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG);
            lastServerConfig = "serverConfigs/" + NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG;

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

    // 4 No trustStore in customize SSL configuration
    @AllowedFFDC({ "sun.security.validator.ValidatorException", "java.security.cert.CertPathBuilderException",
                   "com.ibm.security.cert.IBMCertPathBuilderException" })
    @Test
    public void testNoTrustStoreInCustomizeSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG,
                       "basicAuthWithSSL_provider_web.xml", "bindings/customizeSSLConfig.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", invalidSSLHandshakeResps),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", invalidSSLHandshakeResps),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", invalidSSLHandshakeResps)));

        runTest(params, null);
    }
}
