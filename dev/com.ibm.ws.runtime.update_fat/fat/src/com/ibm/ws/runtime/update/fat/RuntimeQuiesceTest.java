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
package com.ibm.ws.runtime.update.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This class is a little odd:
 * We're testing the behavior of server stop. One server will be used for all test methods.
 * The server will be started and stopped within each test method, BUT..
 * when the server is stopped within the tests, the logs will not be collected.
 *
 * The server logs will be collected at the end, in the tearDown.
 */
public class RuntimeQuiesceTest {

    private static final String QUIESCE_LISTENER_EXCEPTION_MESSAGE = "WOOPS!";
    private static final String QUIESCE_LISTENER_CALLED_MESSAGE = "WHEE!";
    private static final String QUIESCE_START_MESSAGE = "CWWKE1100I";
    private static final String SERVER_STOPPED_MESSAGE = "CWWKE0036I";
    private static final String QUIESCE_THREADS_HUNG_WARNING = "CWWKE1107W";
    private static final String QUIESCE_LISTENER_HUNG_WARNING = "CWWKE1106W";
    private static final String QUIESCE_FAILURE_WARNING = "CWWKE1102W";
    private static final String QUIESCE_SUCCESS_MESSAGE = "CWWKE1101I";

    private static final Class<?> c = RuntimeQuiesceTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.runtime.quiesce.fat");

    @Rule
    public final TestName method = new TestName();

    @BeforeClass
    public static void setUpClass() throws Exception {
        WebArchive dropinsApp = ShrinkHelper.buildDefaultApp("mbean", "web");
        ShrinkHelper.exportDropinAppToServer(server, dropinsApp);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            // make sure server is torn down -- don't collect archive
            if (server != null && server.isStarted()) {
                server.stopServer(false);
            }
        } finally {
            // ONE archive for the whole run (multiple starts/stops of the server)
            server.postStopServerArchive();
        }
    }

    enum TestType {
        EXCEPTION, QUIESCE_HANG, THREAD_HANG, SUCCESS
    }

    @Before
    public void setup() {
        Log.info(c, method.getMethodName(), "**** ENTER: " + method.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        try {
            // make sure server is torn down -- don't collect archive
            if (server.isStarted()) {
                server.stopServer(false);
            }

            server.renameLibertyServerRootFile("logs/trace.log", "logs/" + method.getMethodName() + ".trace.log");
            server.renameLibertyServerRootFile("logs/messages.log", "logs/" + method.getMethodName() + ".messages.log");
            server.resetLogOffsets();

            // Always ensure that the installed resources are cleaned up between runs..
            server.uninstallSystemBundle("test.server.quiesce");
            server.uninstallSystemFeature("quiescelistener-1.0");
            server.uninstallSystemFeature("quiesceInternal-1.0");
            server.uninstallUserBundle("test.server.quiesce");
            server.uninstallUserFeature("quiescelistener-1.0");
        } finally {
            Log.info(c, method.getMethodName(), "**** EXIT: " + method.getMethodName());
        }
    }

    // Note: methods can execute in any order. We do (sadly) have to clean start the server
    // because the same feature and bundle are moved between the system (wlp/lib) and
    // the user extension (usr/extension/lib).

    /**
     * If there are no quiesce listeners registered (and no pending notifications), we shouldn't
     * see any messages about quiesce processing..
     *
     * @throws Exception
     */
    @Test
    public void testForceStop() throws Exception {
        // Add a single quiesce listener as a runtime feature/bundle (internal)
        server.setServerConfigurationFile("quiesce-listener.server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("quiescelistener-1.0");

        // start the server, do not clean-start, and do not pre-clean the logs dir
        server.startServer(method.getMethodName() + ".console.log", true, false);

        // stop the server, do not clean up the archive, and FORCE STOP (no queisce)
        server.stopServer(false, true);

        // These messages flat out shouldn't be in there!
        Assert.assertNull("FAIL: for " + method.getMethodName() + ", " + server.getServerName() + " should not contain information about server quiesce",
                          server.waitForStringInLog(QUIESCE_START_MESSAGE, 1));
    }

    /**
     * Define/invoke a runtime-level quiesce listener
     *
     * @throws Exception
     */
    @Test
    public void testSingleRuntimeQuiesceListener() throws Exception {
        // Add a single quiesce listener as a runtime feature/bundle (internal)
        server.setServerConfigurationFile("quiesce-listener.server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("quiescelistener-1.0");

        startStopServer(TestType.SUCCESS);
    }

    /**
     * Make sure a user feature/product extension can provide a quiesce listener
     * (SPI is defined properly).
     *
     * @throws Exception
     */
    @Test
    public void testSingleUserQuiesceListener() throws Exception {
        // Add a single quiesce listener as a usr feature/bundle (SPI)
        server.setServerConfigurationFile("user-quiesce-listener.server.xml");
        server.installSystemFeature("quiesceInternal-1.0");
        server.installUserBundle("test.server.quiesce");
        server.installUserFeature("quiescelistener-1.0");

        startStopServer(TestType.SUCCESS);
    }

    /**
     * Try a quiesce service that throws an exception, and make sure that doesn't
     * prevent the quiesce activity from completing.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("java.lang.RuntimeException")
    public void testQuiesceListenerException() throws Exception {
        // Add a single quiesce listener as a usr feature/bundle (SPI)
        server.setServerConfigurationFile("bad-quiesce-listener.server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("quiescelistener-1.0");

        startStopServer(TestType.EXCEPTION);
    }

    /**
     * Long running test (at least 30s), push this into the full bucket.
     * This triggers a quiesce listener that takes longer than 30s to complete.
     * Make sure we get a warning that not all quiesce activity completed (and that
     * we don't see the message indicating that it did).
     *
     * @throws Exception
     */
    @Test
    public void testLongRunningQuiesceListener() throws Exception {
        // Add a single quiesce listener as a usr feature/bundle (SPI)
        server.setServerConfigurationFile("longrunning-quiesce-listener.server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("quiescelistener-1.0");

        startStopServer(TestType.QUIESCE_HANG);
    }

    @Test
    public void testLongRunningThreads() throws Exception {
        server.setServerConfigurationFile("longrunning-threads-server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("quiescelistener-1.0");

        startStopServer(TestType.THREAD_HANG);
    }

    @Test
    public void testNonBlockingThreads() throws Exception {
        server.setServerConfigurationFile("non-blocking-threads-server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("quiescelistener-1.0");

        startStopServer(TestType.SUCCESS);
    }

    private void startStopServer(TestType type) throws Exception {
        // start the server, do a clean start, and do not pre-clean the logs dir
        server.startServer(method.getMethodName() + ".console.log", true, false);

        // wait for port to start
        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain info msg about open port",
                             server.waitForStringInLog("CWWKO0219I", 0));

        // stop the server, do not clean up the archive
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        if (type == TestType.QUIESCE_HANG)
            server.stopServer(false, QUIESCE_LISTENER_HUNG_WARNING, QUIESCE_FAILURE_WARNING);
        else if (type == TestType.THREAD_HANG)
            server.stopServer(false, QUIESCE_THREADS_HUNG_WARNING, QUIESCE_FAILURE_WARNING);
        else
            server.stopServer(false);

        // Make sure stop has completed
        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain warning msg about the failure to complete server quiesce",
                             server.waitForStringInLog(SERVER_STOPPED_MESSAGE));

        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain info msg about the start of server quiesce",
                             server.waitForStringInLog(QUIESCE_START_MESSAGE, 0));

        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain WHEE! because the test quiesce listener was called",
                             server.waitForStringInLog(QUIESCE_LISTENER_CALLED_MESSAGE, 0));

        if (type == TestType.EXCEPTION) {
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName()
                                 + " should contain WOOPS! because the test quiesce listener threw an exception",
                                 server.waitForStringInLog(QUIESCE_LISTENER_EXCEPTION_MESSAGE, 0));

        } else if (type == TestType.QUIESCE_HANG) {
            Assert.assertNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should NOT contain info msg about the completion of server quiesce",
                              server.waitForStringInLog(QUIESCE_SUCCESS_MESSAGE, 0));
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain warning msg about the failure to complete server quiesce",
                                 server.waitForStringInLog(QUIESCE_FAILURE_WARNING, 0));
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain warning message indicating that 1 quiesce listener hung",
                                 server.waitForStringInLog(QUIESCE_LISTENER_HUNG_WARNING, 0));
        } else if (type == TestType.THREAD_HANG) {
            Assert.assertNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should NOT contain info msg about the completion of server quiesce",
                              server.waitForStringInLog(QUIESCE_SUCCESS_MESSAGE, 0));
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain warning msg about the failure to complete server quiesce",
                                 server.waitForStringInLog(QUIESCE_FAILURE_WARNING, 0));
            String threadWarning = server.waitForStringInLog(QUIESCE_THREADS_HUNG_WARNING, 0);
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain warning message about hung threads",
                                 threadWarning);
            threadWarning = threadWarning.substring(threadWarning.indexOf(QUIESCE_THREADS_HUNG_WARNING));
            // We should report two hung threads, not four
            Assert.assertTrue("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should be blocked by two threads, not four",
                              threadWarning.contains("2") && !threadWarning.contains("4"));

        } else {
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain information about the completion of server quiesce",
                                 server.waitForStringInLog(QUIESCE_SUCCESS_MESSAGE, 0));
            Assert.assertNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should NOT contain information about the failure to complete server quiesce",
                              server.waitForStringInLog(QUIESCE_FAILURE_WARNING, 0));
        }
    }

}
