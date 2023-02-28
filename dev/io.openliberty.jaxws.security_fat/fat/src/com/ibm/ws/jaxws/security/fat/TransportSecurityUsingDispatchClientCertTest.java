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

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

/**
 * Test cases for Dispatch way using the transport security
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class TransportSecurityUsingDispatchClientCertTest extends AbstractJaxWsTransportSecurityTest {

    private static final String DEFAULT_CLIENT_CERT_CONFIG = "JaxWsTransportSecurityServer/serverConfigs/defaultClientCertConfiguration.xml";

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        buildDefaultApps();
        server.setServerConfigurationFile(DEFAULT_CLIENT_CERT_CONFIG);
        lastServerConfig = "/serverConfigs/defaultClientCertConfiguration.xml";

        server.startServer("TransportSecurityUsingDispatchClientCertTest.log");

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @AllowedFFDC({ "java.lang.NullPointerException" })
    @Test
    public void testValidClientCertDispatch() throws Exception {
        updateProviderWEBXMLFile("clientCert_provider_web.xml");
        updateClientBndFile("bindings/validCertAlias.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", TestMode.DISPATCH, "pojo", "https", server.getHttpDefaultSecurePort(), "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "stateless", "https", server.getHttpDefaultSecurePort(), "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "singleton", "https", server.getHttpDefaultSecurePort(), "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);

    }
}
