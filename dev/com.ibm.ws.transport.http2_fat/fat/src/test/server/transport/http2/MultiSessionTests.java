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

import static org.junit.Assert.assertTrue;

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
public class MultiSessionTests extends FATServletClient {

    private static final String CLASS_NAME = MultiSessionTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");
    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat");
    String defaultServletPath = "H2FATDriver/H2FATDriverServlet?hostName=";

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void before() throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }
        server.startServer(true, true);
        runtimeServer.startServer(true);
    }

    @AfterClass
    public static void after() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "after()", "Stopping servers......");
        }
        server.stopServer(true);
        runtimeServer.stopServer(true);
    }

    private void runTest(String servletPath) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test " + servletPath + " on server " + server.getServerName());
        }
        FATServletClient.runTest(runtimeServer,
                                 servletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    public void runStressTest() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test with iterations of: " + Utils.STRESS_ITERATIONS);
        }

        FATServletClient.runTest(runtimeServer,
                                 "H2FATDriver/H2FATDriverServlet?hostName=" + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&iterations="
                                                + Utils.STRESS_ITERATIONS + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    /**
     * This test is just to get us started with the
     * test framework and bucket. Remove it after
     * development is done... The test is useless
     * in any other context.
     */
    //@Test
    public void testDummyRemoveLater() throws Exception {
        assertTrue(true);
    }

    // Test a single connection with multiple streams using a basic webpage/servlet at the server.
    // Utils.STRESS_ITERATIONS is how many streams will be used on this one connection
    // Test timeout is defined by this variable:  Utils.STRESS_TEST_TIMEOUT_testSingleConnectionStress
    @Test
    public void testSingleConnectionStress() throws Exception {
        runStressTest();
    }

    // Test a single connection with multiple streams, using a servlet on the server that will send multiple Data frames, and
    // will prompt the client side to send WindowUpdate and Ping frames.
    // The client side will also create a dependent priority tree for the streams to be placed in.
    // Client side parameters:
    //    H2FATDriverServlet.testMultiData -> SERVLET_INSTANCES   is the number of parallel http2 streams that will be used.
    // Server side parameters
    //    H2MultiDataFrame.SERVLET_INSTANCES  is the number of parallel http2 streams the server side expects.
    //    H2MultiDataFrame.MINUTES_PER_STREAM is how long each stream will be active
    //    H2MultiDataFrame.<other parameters>  will control the flow rate of Data, Ping, and WindowUpdate frames.
    // The above two SERVLET_INSTANCES need to be the same.
    // Test timeout is defined by this variable:  Utils.STRESS_TEST_TIMEOUT_testMulitData
    @Test
    public void testMultiData() throws Exception {
        runTest(defaultServletPath);
    }

    // test multiple connections with multiple streams.  Same as testMultiData above, except adds parallel http2 connectinon.
    // Client side parameters:
    //    H2FATDriverServlet.testMultiData -> SERVLET_INSTANCES   is the number of parallel http2 streams that will be used.
    //    Utils.STRESS_CONNECTIONS is the number of parallel http2 connections that will be used.
    //    Utils.STRESS_DELAY_BETWEEN_CONN_STARTS time to delay between starting connections
    // Server side parameters
    //    H2MultiDataFrame.SERVLET_INSTANCES  is the numberof parallel http2 streams the server side expects.
    //    H2MultiDataFrame.MINUTES_PER_STREAM is how long each stream will be active
    //    H2MultiDataFrame.<other parameters>  will control the flow rate of Data, Ping, and WindowUpdate frames.
    // The above two SERVLET_INSTANCES need to be the same.
    // Test timeout is defined by this variable:  Utils.STRESS_TEST_TIMEOUT_testMultipleConnectionStress
    @Test
    public void testMultipleConnectionStress() throws Exception {
        Thread[] ta = new Thread[Utils.STRESS_CONNECTIONS];

        for (int i = 0; i < Utils.STRESS_CONNECTIONS; i++) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "Starting Next Connection");
            }
            Thread t = new Thread(new H2FATStressRunnable());
            ta[i] = t;
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "starting thread iteration: " + i);
            }
            t.start();
            try {
                Thread.sleep(Utils.STRESS_DELAY_BETWEEN_CONN_STARTS);
            } catch (Exception x) {
            }
        }

        for (int i = 0; i < Utils.STRESS_CONNECTIONS; i++) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "joining thread iteration: " + i);
            }
            ta[i].join();
        }
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "join complete");
        }
    }

    class H2FATStressRunnable implements Runnable {

        @Override
        public void run() {
            try {
                runStressTest();
            } catch (Exception e) {
                // TODO How to handle this? We cannot throw due to Runnable.run() not defining the Exception
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "There was an exception during H2FATStressRunnable.run(): e");
                }
            }
        }

    }

}
