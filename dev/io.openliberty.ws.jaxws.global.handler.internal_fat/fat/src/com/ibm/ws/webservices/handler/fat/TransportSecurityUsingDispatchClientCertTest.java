/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Test cases for Dispatch way using the transport security
 */
@RunWith(FATRunner.class)
public class TransportSecurityUsingDispatchClientCertTest extends AbstractJaxWsTransportSecurityTest {

    private static final String DEFAULT_CLIENT_CERT_CONFIG = "JaxWsTransportSecurityServer/serverConfigs/defaultClientCertConfiguration.xml";
    private static WebArchive clientApp;
    private static WebArchive providerApp; 

    @BeforeClass
    public static void beforeAllTests() throws Exception {
        providerApp = ShrinkHelper.defaultApp(server, "TransportSecurityProvider", "com.ibm.ws.jaxws.transport.server.security.*");
        clientApp = ShrinkHelper.defaultApp(server, "TransportSecurityClient", "com.ibm.ws.jaxws.transport.client.security.servlet",
                                                                   "com.ibm.ws.jaxws.transport.security");
        //server.installUserBundle("TestHandler1_1.0.0.201311011652");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle2", "com.ibm.ws.userbundle2.myhandler");
        TestUtils.installUserFeature(server, "TestHandler1Feature1");
        server.setServerConfigurationFile(DEFAULT_CLIENT_CERT_CONFIG);
        lastServerConfig = "/serverConfigs/defaultClientCertConfiguration.xml";

        server.startServer("TransportSecurityUsingDispatchClientCertTest.log");

        assertNotNull("Wait for the SSL port to open", server.waitForStringInLog("CWWKO0219I:.*-ssl"));
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @AllowedFFDC({ "java.lang.NullPointerException" })
    @Test
    public void testValidClientCertDispatchStatelessWithGlobalHandlers() throws Exception {
        updateProviderWEBXMLFile(providerApp, "JaxWsTransportSecurityServer/clientCert_provider_web.xml");
        updateClientBndFile(clientApp, "JaxWsTransportSecurityServer/bindings/validCertAlias.xml");

        RequestParams params = new RequestParams("employee", TestMode.DISPATCH, "stateless", "https", server.getHttpDefaultSecurePort(), "/employee/employStatelessService");
        runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
        assertNotNull("handle outbound message in TestHandler1InBundle2", server.waitForStringInLog("handle outbound message in TestHandler1InBundle2"));
        assertNotNull("handle outbound message in TestHandler2InBundle2", server.waitForStringInLog("handle outbound message in TestHandler2InBundle2"));
    }
}