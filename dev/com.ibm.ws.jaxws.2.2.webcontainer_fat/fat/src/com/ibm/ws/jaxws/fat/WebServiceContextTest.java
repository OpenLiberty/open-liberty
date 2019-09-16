/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.liberty.test.wscontext.client.WebServiceContextTestService;
import com.ibm.ws.liberty.test.wscontext.client.WebServiceContextTestServicePortType;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class WebServiceContextTest {

    @Server("WebServiceContextTestServer")
    public static LibertyServer server;

    private static WebServiceContextTestServicePortType testServicePortType = null;
    private static String wsdlLocation;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp("converter", "com.ibm.samples.jaxws.converter",
                                                      "com.ibm.samples.jaxws.converter.bindtype",
                                                      "com.ibm.ws.liberty.test.wscontext");
        app.setWebXML(new File("lib/LibertyFATTestFiles/WebServiceContextTest/web.xml"));
        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.OVERWRITE);

        server.startServer();
        wsdlLocation = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/converter/WebServiceContextTestService?wsdl").toString();
        URL url = new URL(wsdlLocation);
        testServicePortType = new WebServiceContextTestService(url).getWebServiceContextTestServicePort();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testServicePortType = null;
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testInjectionInstanceNull() {
        assertFalse("The WebServiceContext injection instance is null", testServicePortType.isInjectionInstanceNull());
    }

    @Test
    public void testDefaultJndiLookupInstance() {
        assertFalse("The WebServiceContext default lookup injection instance is null", testServicePortType.isDefaultJndiLookupInstanceNull());
    }

    @Test
    public void testSelfDefinedJndiLookupInstance() {
        assertFalse("The WebServiceContext self defined lookup injection instance is null", testServicePortType.isSelfDefinedJndiLookupInstanceNull());
    }

    @Test
    public void testMessageContext() {
        assertFalse("The MessageContext of the WebServiceContext instance is null", testServicePortType.isMessageContextNull());
    }

    @Test
    public void testServletContext() {
        assertFalse("The ServletContext in the MessageContext is null", testServicePortType.isServletContextNull());
    }

    @Test
    public void testServletContextParameter() {
        assertTrue("The value ServletContext parameter 'testValue' is inconsistent", testServicePortType.getServletContextParameter().equals("aaa"));
    }
}
