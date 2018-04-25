/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Http2ConfigTests extends FATServletClient {

    private static final String CLASS_NAME = Http2ConfigTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static String SUCCESS = "SUCCESS";
    private final static String FAIL = "com.ibm.ws.http2.test.exceptions.ClientPrefaceTimeoutException";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");

    private final static LibertyServer serverServlet40 = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.servlet40.h2.off");
    private final static LibertyServer serverServlet31H2On = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.servlet31.h2.on");
    private final static LibertyServer serverServlet31H2Off = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.servlet31.h2.off");

    private final static String defaultServletPath = "H2FATDriver/H2FATDriverServlet?hostName=";
    private final static String TEST_NAME = "testHeaderAndDataPost";

    private static void startServers(LibertyServer server) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "startServers()", "Starting servers...");
        }

        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", false, "http2.test.war.servlets");
        H2FATApplicationHelper.addWarToServerDropins(runtimeServer, "H2FATDriver.war", false, "http2.test.driver.war.servlets");

        server.startServer(true, true);
        runtimeServer.startServer(true, true);

    }

    private static void stopServers(LibertyServer server) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "stopServers()", "Stopping servers......");
        }
        server.stopServer(true);
        runtimeServer.stopServer(true);
    }

    private void test(LibertyServer server, String expectedMessage) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Running test " + defaultServletPath + " on server " + server.getServerName());
        }

        String path = (new StringBuilder()).append(defaultServletPath).append(server.getHostname()).append("&port=").append(server.getHttpSecondaryPort()).append("&testdir=").append(Utils.TEST_DIR).toString();

        HttpUtils.findStringInReadyUrl(runtimeServer, getPathAndQuery(path, TEST_NAME), expectedMessage);

    }

    /**
     * Test Coverage: Executes the testHeaderAndDataPost() test in order to request an HTTP/2
     * connection over a server configured with the Servlet-4.0 feature and
     * the protocolVersion attribute of the httpEndpoint element set to "http/1.1".
     * Test Outcome: Server is not configured for HTTP/2, thus FAT client timeouts out with a
     * com.ibm.ws.http2.test.exceptions.ClientPrefaceTimeoutException.
     * Spec Section: NA
     *
     * @throws Exception
     */
    @Test
    public void testServlet40H2Off() throws Exception {

        try {
            startServers(serverServlet40);

            test(serverServlet40, FAIL);

        }

        finally {
            stopServers(serverServlet40);
        }

    }

    /**
     * Test Coverage: Executes the testHeaderAndDataPost() test in order to request an HTTP/2
     * connection over a server configured with the Servlet-3.1 feature and
     * the protocolVersion attribute of the httpEndpoint element set to "http/2".
     * Test Outcome: Server is configured for HTTP/2, thus a SUCCESS message is returned as the
     * output from the URL's response.
     * Spec Section: NA
     *
     * @throws Exception
     */
    @Test
    public void testServlet31H2On() throws Exception {

        try {
            startServers(serverServlet31H2On);
            test(serverServlet31H2On, SUCCESS);
        }

        finally {
            stopServers(serverServlet31H2On);
        }
    }

    @Test
    /**
     * Test Coverage: Executes the testHeaderAndDataPost() test in order to request an HTTP/2
     * connection over a server configured with the Servlet-3.1 feature.
     * Test Outcome: Server is not configured for HTTP/2, thus FAT client timeouts out with a
     * com.ibm.ws.http2.test.exceptions.ClientPrefaceTimeoutException.
     * Spec Section: NA
     *
     * @throws Exception
     */
    public void testServlet31H2Off() throws Exception {

        try {
            startServers(serverServlet31H2Off);
            test(serverServlet31H2Off, FAIL);
        }

        finally {
            stopServers(serverServlet31H2Off);
        }

    }
}
