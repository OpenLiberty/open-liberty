/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.client.servlet.WebFaultTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test suite tests our compliance with @WebFault annotated custom Exceptions
 * It uses the addNumbersProvider and addNumbersClient tests applications to invoke
 * the AddNumbers WebService with inputs guaranteed to throw exceptions
 * so that we can test the validity of the returned SOAPFualt response.
 *
 * TODO: Add more tests that check the multitude of a Faults contents not tested here
 */
@RunWith(FATRunner.class)
public class WebFaultTest extends FATServletClient {

    @Server("WebFaultTestServer")
    @TestServlet(servlet = WebFaultTestServlet.class, contextRoot = "addNumbersClient")
    public static LibertyServer server;

    private static final int TIMEOUT = 5000;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "addNumbersProvider", "com.ibm.ws.jaxws.provider");

        ShrinkHelper.defaultDropinApp(server, "addNumbersClient", "com.ibm.ws.jaxws.client",
                                      "com.ibm.ws.jaxws.client.servlet");

        // Reuse AddNumbers.wsdl from shared HandlerChainWithWebServiceClientTest
        server.copyFileToLibertyServerRoot("", "HandlerChainWithWebServiceClientTest/AddNumbers.wsdl");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Make sure exceptions were logged in trace
        String expected = "Fault occured, printing Exception cause to trace.";
        assertNotNull("Failed to reveal main cause of the exception", server.waitForStringInTrace(expected, TIMEOUT));
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
