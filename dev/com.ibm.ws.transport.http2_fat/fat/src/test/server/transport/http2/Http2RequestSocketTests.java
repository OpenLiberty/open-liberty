/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.server.transport.http2;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.ws.http2.client.SecureHttp2Client;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Http2RequestSocketTests extends FATServletClient {

    private static final String CLASS_NAME = Http2RequestSocketTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private static SecureHttp2Client client;

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.requestsocket");
    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void before() throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }

        H2FATApplicationHelper.addWarToServerDropins(runtimeServer, "H2FATDriver.war", true, "http2.test.driver.war.servlets");
        runtimeServer.startServer(true, true);

        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", true, "http2.test.war.servlets");
        server.installSystemFeature("webcontainerlibertyinternals");
        server.startServer(Http2SecureTests.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", server.waitForStringInLog("CWWKO0219I.*ssl"));
        client = new SecureHttp2Client();

    }

    @AfterClass
    public static void after() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "after()", "Stopping servers......");
        }
        // try for an orderly quiet shutdown
        Thread.sleep(5000);
        runtimeServer.stopServer(true);
        Thread.sleep(5000);
        server.stopServer(true);
    }

    @Test
    public void testGetRequestSocketSecure() throws Exception {
        String[] requestUris = new String[] { "/H2TestModule/GetRequestSocketServlet" };
        int port = server.getHttpSecondarySecurePort();
        String expectedResponse = "RequestSocket called from socket LocalPort: " + port + System.lineSeparator();
        List<String> results = client.makeSecureRequests(server.getHostname(), port, requestUris, 0);
        validateSingleResponse(results, expectedResponse);
    }

    @Test
    public void testGetRequestSocketInsecure() throws Exception {
        String testName = "testGetRequestSocketInsecure";
        FATServletClient.runTest(runtimeServer,
                                 "H2FATDriver/H2FATDriverServlet?hostName=" + server.getHostname() + "&port=" + server.getHttpSecondaryPort(),
                                 testName);
    }

    private void validateSingleResponse(List<String> results, String expectedResponse) {
        Assert.assertTrue("At least one result not present, results size = " + results.size(), results.size() >= 1);
        String firstResult = results.get(0);
        Assert.assertEquals("Expected response not found in result", expectedResponse, firstResult);
    }

}
