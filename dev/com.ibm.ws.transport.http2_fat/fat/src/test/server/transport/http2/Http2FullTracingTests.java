/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
@Mode(TestMode.FULL)
public class Http2FullTracingTests extends FATServletClient {

    private static final String CLASS_NAME = Http2FullTracingTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime.tracing");
    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.tracing");

    String defaultServletPath = "H2FATDriver/H2FATDriverServlet?hostName=";
    String dataServletPath = "H2FATDriver/DataFrameTests?hostName=";
    String genericServletPath = "H2FATDriver/GenericFrameTests?hostName=";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void before() throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }

        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", false, "http2.test.war.servlets");
        H2FATApplicationHelper.addWarToServerDropins(runtimeServer, "H2FATDriver.war", false, "http2.test.driver.war.servlets");

        server.startServer(true, true);
        runtimeServer.startServer(true, true);
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

    @Test
    public void testHeaderAndDataPost() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    // moved for debug
    @Test
    public void testDataFrameExceedingMaxFrameSize() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a SETTINGS frame of the wrong size (other than a multiple of 6 octets)
     * Test Outcome: Return a connection error of type FRAME_SIZE_ERROR and close the connection.
     * Spec Section: 6.5
     *
     * @throws Exception
     */
    // moved for debug
    @Test
    public void testSettingFrameBadSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    // moved for debug
    @Test
    public void testZeroLengthPadding() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }

    // Moved to trace
    @Test
    public void testDataOnStreamZero() throws Exception {
        runTest(genericServletPath, testName.getMethodName());
    }

    // Moved to tracing
    @Test
    public void testHeaderFrameAfterHeaderFrameWithEndOfStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    // Moved to tracing
    @Test
    public void testDataFrameAfterContinuationFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    // Moved to tracing
    @Test
    public void testUnknownFrameType() throws Exception {
        runTest(genericServletPath, testName.getMethodName());
    }

    @Test
    public void testContinuationFrameAfterDataFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testInvalidPaddingValue() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }
}
