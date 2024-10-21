/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package test.server.transport.http2;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class Http2Config31H2Off extends FATServletClient {

    private static final String CLASS_NAME = Http2Config40H2Off.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.servlet31.h2.off");

    @Rule
    public TestName testName = new Utils.CustomTestName();

    @BeforeClass
    public static void before() throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }

        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", true, "http2.test.war.servlets");
        H2FATApplicationHelper.addWarToServerDropins(runtimeServer, "H2FATDriver.war", true, "http2.test.driver.war.servlets");

        server.startServer(true, true);
        runtimeServer.startServer(true, true);
        // Go through Logs and check if Netty is being used
        boolean runningNetty = false;
        // Wait for endpoints to finish loading and get the endpoint started messages
        server.waitForStringInLog("CWWKO0219I.*");
        runtimeServer.waitForStringInLog("CWWKO0219I.*");
        List<String> test = server.findStringsInLogs("CWWKO0219I.*");
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Got port list...... " + Arrays.toString(test.toArray()));
            LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Looking for port: " + server.getHttpSecondaryPort());
        }
        for (String endpoint : test) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Endpoint: " + endpoint);
            }
            if (!endpoint.contains("port " + Integer.toString(server.getHttpSecondaryPort())))
                continue;
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Netty? " + endpoint.contains("io.openliberty.netty.internal.tcp.TCPUtils"));
            }
            runningNetty = endpoint.contains("io.openliberty.netty.internal.tcp.TCPUtils");
            break;
        }
        if (runningNetty)
            FATServletClient.runTest(runtimeServer,
                                     Http2FullModeTests.defaultServletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
                                     "setUsingNetty");
    }

    @AfterClass
    public static void after() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "after()", "Stopping servers......");
        }
        server.stopServer(true);
        runtimeServer.stopServer(true);
    }

    private void runTest(String servletPath, String testName) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test " + servletPath + " on server " + server.getServerName());
        }
        FATServletClient.runTest(runtimeServer,
                                 servletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    /**
     * Test Coverage: Client Sends Upgrade Header followed by a SETTINGS frame
     * Test Outcome: The connection should time out
     * Spec Section: 6.5
     *
     * @throws Exception
     */
    @Test
    public void servlet31H2Off() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Connect to server via the insecure port and immediately send the HTTP/2 magic string
     * HTTP/2 is disabled on the server
     * Test Outcome: The server should NOT send the HTTP/2 preface
     *
     * @throws Exception
     */
    @Test
    public void servlet31H2OffDirectConnection() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }
}
