/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.transport.http2;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.http2.client.SecureHttp2Client;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class Http2SecureTests extends FATServletClient {

    private static final String CLASS_NAME = Http2LiteModeTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private static SecureHttp2Client client;

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.secure");

    @BeforeClass
    public static void before() throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }
        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", true, "http2.test.war.servlets");
        server.startServer(true, true);
        client = new SecureHttp2Client();
    }

    @AfterClass
    public static void after() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "after()", "Stopping servers......");
        }
        server.stopServer(true);
    }

    /**
     * Test Coverage: Client requests a servlet that will generate a push request
     * Test Outcome: Server pushes a resource to the client
     * Note: JDK9+ required here for ALPN
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testSimplePushSecure() throws Exception {
        String[] requestUris = new String[] { "/H2TestModule/SimplePushServlet" };
        int port = server.getHttpSecondarySecurePort();
        String pushBody = "the push worked!";
        List<String> results = client.makeSecureRequests(server.getHostname(), port, requestUris, 1);
        Assert.assertTrue("secure push was not successful!", results.contains(pushBody));
    }

    /**
     * Test Coverage: Client makes multiple requests to a servlet that will generate a push request
     * Test Outcome: Server pushes multiple resources to the client
     * Note: JDK9+ required here for ALPN
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testMultiplePushesSecure() throws Exception {
        String[] requestUris = new String[] { "/H2TestModule/SimplePushServlet",
                                              "/H2TestModule/SimplePushServlet",
                                              "/H2TestModule/SimplePushServlet" };
        int port = server.getHttpSecondarySecurePort();
        String pushBody = "the push worked!";
        List<String> results = client.makeSecureRequests(server.getHostname(), port, requestUris, requestUris.length);
        Assert.assertTrue("all three pushed resources were not received", Collections.frequency(results, pushBody) == 3);
    }

    /**
     * Test Coverage: Client makes a request to a servlet with a simple body
     * Test Outcome: Server responds with the servlet body
     * Note: JDK9+ required here for ALPN
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testSimpleRequestSecure() throws Exception {
        String[] requestUris = new String[] { "/H2TestModule/H2HeadersAndBody" };
        int port = server.getHttpSecondarySecurePort();
        String body = "ABC123";
        List<String> results = client.makeSecureRequests(server.getHostname(), port, requestUris, 0);
        Assert.assertTrue("secure request was not successful!", results.contains(body));
    }

    /**
     * Test Coverage: Client makes multiple requests to a servlet with a simple body
     * Test Outcome: Server responds multiple times
     * Note: JDK9+ required here for ALPN
     * @throws Exception
     */
    @Test
    @MinimumJavaLevel(javaLevel = 9)
    public void testMultipleRequestsSecure() throws Exception {
        String[] requestUris = new String[] { "/H2TestModule/H2HeadersAndBody",
                                              "/H2TestModule/H2HeadersAndBody",
                                              "/H2TestModule/H2HeadersAndBody" };
        int port = server.getHttpSecondarySecurePort();
        String body = "ABC123";
        List<String> results = client.makeSecureRequests(server.getHostname(), port, requestUris, 0);
        Assert.assertTrue("all three requests did not complete", Collections.frequency(results, body) == 3);
    }

}
