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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

/*
 * Test cases for SSL configuration with no Trust Store in server.xml
 */
@Mode(Mode.TestMode.FULL)
@RunWith(FATRunner.class)
// As part of the deprecation of the Java Security Manager, as specified by JEP 411,
// Liberty does not support the Java Security Manager with Java 18 or later, that this test uses.
@MaximumJavaLevel(javaLevel = 17)
public class SSLConfigurationNoTrustStoreTest extends AbstractJaxWsTransportSecuritySSLTest {

    private static final String SCHEMA = "https";

    private static final int SECURE_PORT = server.getHttpDefaultSecurePort();

    static {
        invalidSSLHandshakeResps.add("SSLHandshakeException");
        invalidSSLHandshakeResps.add("CertPathBuilderException");

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
                   "com.ibm.security.cert.IBMCertPathBuilderException", "sun.security.provider.certpath.SunCertPathBuilderException" })
    @Test
    public void testNoTrustStoreInCustomizeSSLConfig() throws Exception {
        prepareForTest("serverConfigs/" + NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG,
                       "basicAuthWithSSL_provider_web.xml", "bindings/customizeSSLConfig.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/unauthorized/employPojoService", invalidSSLHandshakeResps),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/unauthorized/employStatelessService", invalidSSLHandshakeResps),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/unauthorized/employSingletonService", invalidSSLHandshakeResps)));

        runTest(params, null);
    }
}
