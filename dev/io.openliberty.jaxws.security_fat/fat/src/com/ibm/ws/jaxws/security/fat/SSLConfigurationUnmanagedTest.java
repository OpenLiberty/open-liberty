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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class SSLConfigurationUnmanagedTest extends AbstractJaxWsTransportSecuritySSLTest {

    private static final String DEFAULT_SSL_SERVER_CONFIG = "defaultSSLConfiguration.xml";

    private static final String SSLREF_IN_SSL_DEFAULT_CONFIG = "sslRefInSSLDefaultConfiguration.xml";

    private static final String CUSTOMIZE_SSL_CONFIG = "customizeSSLConfiguration.xml";

    private static final String NO_TRUST_STORE_IN_CUSTOMIZE_SSL_CONFIG = "noTrustStoreInCustomizeSSLConfiguration.xml";

    private static final String NO_SSL_CONFIG = "noSSLConfiguration.xml";

    protected static final List<String> invalidSSLHandshakeResps = new ArrayList<String>(2);

    protected static final List<String> noSSLResps = new ArrayList<String>(2);

    protected static final String DEFAULT_SSL_WITHOUT_TRUST_STORE_SERVER_CONFIG = "noTrustStoreInDefaultSSLConfiguration.xml";

    protected static final String NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG = "noValidTrustCertInCustomizeSSLConfiguration.xml";

    static {
        invalidSSLHandshakeResps.add("SSLHandshakeException");
        invalidSSLHandshakeResps.add("java.security.cert.CertPathBuilderException");

        noSSLResps.add("NullPointerException");
        noSSLResps.add("ConnectException");

        dynamicUpdate = false;
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
                server.stopServer(); // trust stop server to ensure server is stopped
            }
        }
    }

    @After
    public void afterTest() throws Exception {
        if (!dynamicUpdate) {
            if (server != null && server.isStarted()) {
                server.stopServer(); // trust stop server to make sure the server is stopped.
            }
        }
    }

    // 1 Default SSL configuration
    @Test
    public void testUnmanagedDefaultSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + DEFAULT_SSL_SERVER_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        // change the servlet path to unmanaged test servlet
        this.SERVLET_PATH = "/TransportSecurityClient/TestUnmanagedTransportSecurityServlet";
        runTest(params, null);
    }

    // 2 SSLRef in DefaultSSL configuration
    @Test
    public void testSSLRefInDefaultSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + SSLREF_IN_SSL_DEFAULT_CONFIG, "basicAuthWithSSL_provider_web.xml", null);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        // change the servlet path to unmanaged test servlet
        this.SERVLET_PATH = "/TransportSecurityClient/TestUnmanagedTransportSecurityServlet";
        runTest(params, null);
    }

    // 3 Customize SSL configuration
    @Test
    public void testCustomizeSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + CUSTOMIZE_SSL_CONFIG, "basicAuthWithSSL_provider_web.xml",
                       "bindings/customizeSSLConfig.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        this.SERVLET_PATH = "/TransportSecurityClient/TestUnmanagedTransportSecurityServlet";
        runTest(params, null);
    }
}
