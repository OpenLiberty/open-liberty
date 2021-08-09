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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Http2Config31H2On extends FATServletClient {

    private static final String CLASS_NAME = Http2Config40H2Off.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static String SUCCESS = "SUCCESS";
    private final static String FAIL = "com.ibm.ws.http2.test.exceptions.ClientPrefaceTimeoutException";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.servlet31.h2.on");

    private final static String defaultServletPath = "H2FATDriver/H2FATDriverServlet?hostName=";
    private final static String TEST_NAME = "testHeaderAndDataPost";

    @BeforeClass
    public static void before() throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }

        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", true, "http2.test.war.servlets");
        H2FATApplicationHelper.addWarToServerDropins(runtimeServer, "H2FATDriver.war", true, "http2.test.driver.war.servlets");

        server.startServer(true, true);
        runtimeServer.startServer(true, true);
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
     * Test Outcome: Connection should work ok
     * Spec Section: 6.5
     *
     * @throws Exception
     */
    @Test
    public void servlet31H2On() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

}
