/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    public TestName testName = new Utils.CustomTestName();

    @BeforeClass
    public static void before() throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }

        // I think we need this for tracing to turn on (as well as changes in bootstrap.properties)
        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", true, "http2.test.war.servlets");
        H2FATApplicationHelper.addWarToServerDropins(runtimeServer, "H2FATDriver.war", true, "http2.test.driver.war.servlets");

        server.startServer(true, true);
        runtimeServer.startServer(true);
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

    public void runStressTest() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test with iterations");
        }

        FATServletClient.runTest(runtimeServer,
                                 "H2FATDriver/H2FATDriverServlet?hostName=" + server.getHostname() +
                                                "&port=" + server.getHttpSecondaryPort() +
                                                "&testdir=" + Utils.TEST_DIR,
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

    // test multiple connections with multiple streams.  Same as testMultiData above, except adds parallel http2 connectinon.
    //     see test.server.transport.http2.Utils.java for how parameters are set up to run this test.
    @Test
    public void testMultipleConnectionStress() throws Exception {
        Queue<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();
        ExecutorService es = Executors.newFixedThreadPool(Utils.STRESS_CONNECTIONS);
        for (int i = 0; i < Utils.STRESS_CONNECTIONS; i++) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "Starting Next Connection");
            }

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "submitting H2FATStressCallable number: " + i);
            }
            Future<Boolean> f = es.submit(new H2FATStressCallable());
            futures.offer(f);
            try {
                Thread.sleep(Utils.STRESS_DELAY_BETWEEN_CONN_STARTS);
            } catch (Exception x) {
            }
        }
        while (!futures.isEmpty()) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "waiting for future");
            }
            Future<Boolean> current = futures.poll();
            assertTrue(current.get());
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "futures complete");
        }
    }

    class H2FATStressCallable implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            try {
                runStressTest();
                return true;
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.logp(Level.INFO, CLASS_NAME, "testMultipleConnectionStress", "There was an exception during H2FATStressCallable.call(): e");
                }
                throw e;
            }
        }
    }

}
