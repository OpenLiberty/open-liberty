/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.update.fat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
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
    private static final Class<?> c = RuntimeQuiesceTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.runtime.quiesce.fat");

    @Rule
    public final TestName method = new TestName();

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

    boolean throwException = false;
    boolean takeForever = false;

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
            server.uninstallSystemBundle("test.server.quiesce_1.0.0");
            server.uninstallSystemFeature("quiescelistener-1.0");
            server.uninstallUserBundle("test.server.quiesce_1.0.0");
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
    public void testNoQuiesceListeners() throws Exception {

        server.setServerConfigurationFile("no-quiesce-listener.server.xml");
        // start the server, do not clean-start, and do not pre-clean the logs dir
        server.startServer(method.getMethodName() + ".console.log", true, false);

        // stop the server, do not clean up the archive
        server.stopServer(false);

        // These messages flat out shouldn't be in there!
        Assert.assertNull("FAIL: for " + method.getMethodName() + ", " + server.getServerName() + " should not contain information about server quiesce",
                          server.waitForStringInLog("CWWKE1100I", 1));
    }

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
        server.installSystemBundle("test.server.quiesce_1.0.0");
        server.installSystemFeature("quiescelistener-1.0");

        // start the server, do not clean-start, and do not pre-clean the logs dir
        server.startServer(method.getMethodName() + ".console.log", true, false);

        // stop the server, do not clean up the archive, and FORCE STOP (no queisce)
        server.stopServer(false, true);

        // These messages flat out shouldn't be in there!
        Assert.assertNull("FAIL: for " + method.getMethodName() + ", " + server.getServerName() + " should not contain information about server quiesce",
                          server.waitForStringInLog("CWWKE1100I", 1));
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
        server.installSystemBundle("test.server.quiesce_1.0.0");
        server.installSystemFeature("quiescelistener-1.0");

        startStopServer();
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
        server.installUserBundle("test.server.quiesce_1.0.0");
        server.installUserFeature("quiescelistener-1.0");

        startStopServer();
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
        server.installSystemBundle("test.server.quiesce_1.0.0");
        server.installSystemFeature("quiescelistener-1.0");

        throwException = true;
        startStopServer();
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
    @Mode(TestMode.FULL)
    public void testLongRunningQuiesceListener() throws Exception {
        // Add a single quiesce listener as a usr feature/bundle (SPI)
        server.setServerConfigurationFile("longrunning-quiesce-listener.server.xml");
        server.installSystemBundle("test.server.quiesce_1.0.0");
        server.installSystemFeature("quiescelistener-1.0");

        takeForever = true;
        startStopServer();
    }

    private void startStopServer() throws Exception {
        // start the server, do a clean start, and do not pre-clean the logs dir
        server.startServer(method.getMethodName() + ".console.log", true, false);

        // wait for port to start
        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain info msg about open port",
                             server.waitForStringInLog("CWWKO0219I", 0));

        // stop the server, do not clean up the archive
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.stopServer(false);

        // Make sure stop has completed
        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain warning msg about the failure to complete server quiesce",
                             server.waitForStringInLog("CWWKE0036I"));

        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain info msg about the start of server quiesce",
                             server.waitForStringInLog("CWWKE1100I", 0));

        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain WHEE! because the test quiesce listener was called",
                             server.waitForStringInLog("WHEE!", 0));

        if (throwException) {
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName()
                                 + " should contain WOOPS! because the test quiesce listener threw an exception",
                                 server.waitForStringInLog("WOOPS!", 0));
        }

        if (takeForever) {
            Assert.assertNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should NOT contain info msg about the completion of server quiesce",
                              server.waitForStringInLog("CWWKE1101I", 0));
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain warning msg about the failure to complete server quiesce",
                                 server.waitForStringInLog("CWWKE1102W", 0));
        } else {
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain information about the completion of server quiesce",
                                 server.waitForStringInLog("CWWKE1101I", 0));
            Assert.assertNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should NOT contain information about the failure to complete server quiesce",
                              server.waitForStringInLog("CWWKE1102W", 0));
        }
    }

}
