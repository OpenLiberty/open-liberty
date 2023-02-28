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
 * Test cases for SSL configuration
 */
@RunWith(FATRunner.class)
public class SSLConfigurationTest extends AbstractJaxWsTransportSecuritySSLTest {

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
    public void testDefaultSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + DEFAULT_SSL_SERVER_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // 5 No SSL configuration
    @AllowedFFDC({ "com.ibm.wsspi.channelfw.exception.InvalidChainNameException" })
    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    @Test
    public void testNoSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + NO_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", noSSLResps),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", noSSLResps),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", noSSLResps)));

        runTest(params, null);
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
    public void testNoTrustStoreInDefaultSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + DEFAULT_SSL_WITHOUT_TRUST_STORE_SERVER_CONFIG,
                       "basicAuthWithSSL_provider_web.xml", null);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // 2 No valid certification in trustStore of customize SSL configuration
    @AllowedFFDC({ "sun.security.validator.ValidatorException", "java.security.cert.CertPathBuilderException",
                   "com.ibm.security.cert.IBMCertPathBuilderException" })
    @Test
    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    public void testNoValidTrustCertInCustomizeSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG,
                       "basicAuthWithSSL_provider_web.xml", "bindings/customizeSSLConfig.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", invalidSSLHandshakeResps),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", invalidSSLHandshakeResps),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", invalidSSLHandshakeResps)));

        runTest(params, null);
    }

    // 3 inexistent customize and fall back on default SSL configuration
    @Test
    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    public void testInexistentCustomizeButFallbackOnDefaultSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
                       "bindings/inexistentCustomizeButFallbackOnDefaultSSLConfig.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // From Lite: 3 Customize SSL configuration
    @Test
    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    public void testCustomizeSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
                       "bindings/customizeSSLConfig.xml");
        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }
}
