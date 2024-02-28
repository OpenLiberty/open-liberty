/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import static org.junit.Assert.assertNotNull;

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

/**
 * Test cases for client certificate on JAX-WS transport security
 */
@RunWith(FATRunner.class)
public class ClientCertificateTest extends AbstractJaxWsTransportSecurityTest {

    private static final List<String> INEXISTENT_ALIAS_SERVER_INFO = new ArrayList<String>();

    protected static final String DEFAULT_CLIENT_CERT_CONFIG = "defaultClientCertConfiguration.xml";

    protected static final String NO_APP_SECURITY_CONFIG = "noAppSecurityFeature.xml";

    protected static final List<String> CERT_NOT_TRUST_SERVER_INFO = new ArrayList<String>();

    protected static final List<String> NOT_AUTHORIZED_SERVER_INFO = new ArrayList<String>();

    protected static final String WITH_CLIENT_ALIAS_CONFIG = "withAliasClientCertConfiguration.xml";

    protected static final String PATCHY_SERVER_TRUST_STORE_CONFIG = "patchyServerTrustStoreConfiguration.xml";

    protected static final String CUSTOMIZE_SSL_ENABLE_CN_CHECK = "customizeSSLEnableCNCheck.xml";

    protected static final String SCHEMA = "https";

    protected static final int SECURE_PORT = server.getHttpDefaultSecurePort();

    protected static final int PORT = server.getHttpDefaultPort();

    protected static final int WAIT_TIME_OUT = 10 * 1000;

    private boolean checkAppUpdate = false;

    static {
        INEXISTENT_ALIAS_SERVER_INFO.add("CWPKI0023E.*");
        INEXISTENT_ALIAS_SERVER_INFO.add("CWWKW0601E.*");

        NOT_AUTHORIZED_SERVER_INFO.add("CWWKS9104A.*employee");

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
    public void testValidClientCert() throws Exception {
        prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
                       "bindings/validCertAlias.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, null);
    }

    // 2 Inexistent client certificate
    @AllowedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    @Test
    public void testInexistentClientCert() throws Exception {
        prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml", "bindings/inexistentCertAlias.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "CWPKI0023E"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "CWPKI0023E"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "CWPKI0023E")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, INEXISTENT_ALIAS_SERVER_INFO);
    }

    // 1 Override alias configured in ssl element with customize one
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testOverrideAliasWithCustomizeOnePOJO() throws Exception {
        prepareForTest("serverConfigs/" + WITH_CLIENT_ALIAS_CONFIG, "clientCert_provider_web.xml",
                       "bindings/overrideCertAlias.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/manager/employPojoService", "403")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, NOT_AUTHORIZED_SERVER_INFO);
    }

    @Test
    @Mode(Mode.TestMode.FULL)
    public void testOverrideAliasWithCustomizeOne() throws Exception {
        prepareForTest("serverConfigs/" + WITH_CLIENT_ALIAS_CONFIG, "clientCert_provider_web.xml",
                       "bindings/overrideCertAlias.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("manager", "stateless", SCHEMA, SECURE_PORT, "/manager/employStatelessService", "From other bean: Hello, manager from SayHelloStatelessService"),
                                                                   new RequestParams("manager", "singleton", SCHEMA, SECURE_PORT, "/manager/employSingletonService", "From other bean: Hello, manager from SayHelloSingletonService")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, null);
    }

    // 2 Cert in client keystore but not in server's trustStore
    @AllowedFFDC({ "java.security.cert.CertificateException", "java.security.cert.CertPathBuilderException",
                   "sun.security.validator.ValidatorException", "com.ibm.security.cert.IBMCertPathBuilderException" })
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testCertInClientKeyStoreButNotInServerTrustStore() throws Exception {
        prepareForTest("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, "clientCert_provider_web.xml",
                       "bindings/certInClientKSButNotInServerTS.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "Could not send Message"),
                                                                   new RequestParams("manager", "stateless", SCHEMA, SECURE_PORT, "/manager/employStatelessService", "Could not send Message"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "Could not send Message")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, CERT_NOT_TRUST_SERVER_INFO);

    }

    // 3 JaxWsSecurity feature re-enable
    @AllowedFFDC({ "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" })
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testReEnableJaxWsSecurityFeature() throws Exception {
        // With appSecurity feature
        prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
                       "bindings/validCertAlias.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),

                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, null);

        // Remove appSecurity feature
        server.setMarkToEndOfLog();
        updateServerConfigFile("serverConfigs/" + NO_APP_SECURITY_CONFIG);
        waitForClientAppUpdate();
        for (RequestParams param : params) {
            param.setExpectedResp("Could not send Message");
        }

        runTest(params, null);

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
        List<String> expectedResponses = new ArrayList<>(Arrays.asList("Hello, employee from SayHelloPojoService",
                                                                       "From other bean: Hello, employee from SayHelloStatelessService",
                                                                       "From other bean: Hello, employee from SayHelloSingletonService"));
        for (int i = 0; i < params.size(); ++i) {
            params.get(i).setExpectedResp(expectedResponses.get(i));
        }
        runTest(params, null);
    }

    private void waitForClientAppUpdate() {
        server.waitForStringInLogUsingMark("CWWKZ0001I.*TransportSecurityClient | CWWKZ0003I.*TransportSecurityClient",
                                           WAIT_TIME_OUT);
    }

    // From Lite: 3 Valid certificate but not authorized
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidCertButNotAuthorized() throws Exception {
        prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
                       "bindings/validCertAlias.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/manager/employPojoService", "403"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/manager/employStatelessService", "403"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/manager/employSingletonService", "403")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, NOT_AUTHORIZED_SERVER_INFO);
    }

    // From Lite: 4 enable hostName verifier
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testEnableCNCheck() throws Exception {
        prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_ENABLE_CN_CHECK, "clientCert_provider_web.xml",
                       "bindings/enableCNCheck.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "disableCNCheck"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "disableCNCheck"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "disableCNCheck")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, null);
    }

    @AllowedFFDC({ "java.lang.NullPointerException" })
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidClientCertDispatch() throws Exception {
        prepareForTest("serverConfigs/" + DEFAULT_CLIENT_CERT_CONFIG, "clientCert_provider_web.xml",
                       "bindings/validCertAlias.xml", checkAppUpdate);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", TestMode.DISPATCH, "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, null);
        checkAppUpdate = false;
    }

//  1 Valid name and valid plain password without SSL
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidNameAndValidPlainPasswordWithoutSSLDispatch() throws Exception {
        prepareForTest("serverConfigs/basicAuthWithoutSSL.xml", "basicAuthWithoutSSL_provider_web.xml",
                       "bindings/validNameAndValidPlainPwd.xml", checkAppUpdate);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", TestMode.DISPATCH, "pojo", "http", PORT, "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "stateless", "http", PORT, "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "singleton", "http", PORT, "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
        checkAppUpdate = false;
    }

    // 2 Valid name and valid plain password with SSL
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidNameAndValidPlainPasswordWithSSLDispatch() throws Exception {
        prepareForTest("serverConfigs/basicAuthWithSSL.xml", "basicAuthWithSSL_provider_web.xml",
                       "bindings/validNameAndValidPlainPwd.xml", checkAppUpdate);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", TestMode.DISPATCH, "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, null);
        checkAppUpdate = false;
    }

    // 3 Customize SSL configuration
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testCustomizeSSLConfigDispatch() throws Exception {
        prepareForTest("serverConfigs/customizeSSLConfiguration.xml", "basicAuthWithSSL_provider_web.xml",
                       "bindings/customizeSSLConfig.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", TestMode.DISPATCH, "pojo", SCHEMA, SECURE_PORT, "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "stateless", SCHEMA, SECURE_PORT, "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "singleton", SCHEMA, SECURE_PORT, "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));

        runTest(params, null);
    }
}
