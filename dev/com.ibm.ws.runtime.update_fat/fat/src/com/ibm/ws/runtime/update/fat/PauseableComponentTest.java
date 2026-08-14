/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This class is a little odd:
 * We're testing the behavior of pauseable components during server stop. One server will be used for all test methods.
 * The server will be started and stopped within each test method, BUT..
 * when the server is stopped within the tests, the logs will not be collected.
 *
 * The server logs will be collected at the end, in the tearDown.
 */
@RunWith(FATRunner.class)
public class PauseableComponentTest {

    private static final String PAUSEABLE_COMPONENT_EXCEPTION_MESSAGE = "WOOPS!";
    private static final String PAUSEABLE_COMPONENT_CALLED_MESSAGE = "WHEE!";
    private static final String PAUSEABLE_START_MESSAGE = "CWWKE1100I";
    private static final String SERVER_STOPPED_MESSAGE = "CWWKE0036I";
    private static final String PAUSEABLE_COMPONENT_HUNG_WARNING = "CWWKE1106W";
    private static final String PAUSEABLE_FAILURE_WARNING = "CWWKE1102W";
    private static final String PAUSEABLE_SUCCESS_MESSAGE = "CWWKE1101I";

    private static final Class<?> c = PauseableComponentTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.runtime.pauseable.fat");

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
        EXCEPTION, PAUSEABLE_HANG, SUCCESS
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
            server.resetLogMarks();

            // Always ensure that the installed resources are cleaned up between runs..
            server.uninstallSystemBundle("test.server.quiesce");
            server.uninstallUserBundle("test.server.quiesce");
        } finally {
            Log.info(c, method.getMethodName(), "**** EXIT: " + method.getMethodName());
        }
    }

    // Note: methods can execute in any order. We do (sadly) have to clean start the server
    // because the same feature and bundle are moved between the system (wlp/lib) and
    // the user extension (usr/extension/lib).

    /**
     * If there are no pauseable components registered, we shouldn't
     * see any messages about pauseable component processing..
     *
     * @throws Exception
     */
    @Test
    public void testForceStop() throws Exception {
        // Add a single pauseable component as a runtime feature/bundle (internal)
        server.setServerConfigurationFile("pauseable-component.server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("pauseablecomponent-1.0");

        // start the server, do not clean-start, and do not pre-clean the logs dir
        server.startServer(method.getMethodName() + ".console.log", true, false);

        // stop the server, do not clean up the archive, and FORCE STOP (no pause)
        server.stopServer(false, true);

        // These messages flat out shouldn't be in there!
        Assert.assertNull("FAIL: for " + method.getMethodName() + ", " + server.getServerName() + " should not contain information about pauseable component processing",
                          server.waitForStringInLog(PAUSEABLE_START_MESSAGE, 1));
    }

    /**
     * Define/invoke a runtime-level pauseable component
     *
     * @throws Exception
     */
    @Test
    public void testSingleRuntimePauseableComponent() throws Exception {
        // Add a single pauseable component as a runtime feature/bundle (internal)
        server.setServerConfigurationFile("pauseable-component.server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("pauseablecomponent-1.0");

        startStopServer(TestType.SUCCESS);
    }

    /**
     * Try a pauseable component that throws an exception, and make sure that doesn't
     * prevent the pause activity from completing.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("java.lang.RuntimeException")
    public void testPauseableComponentException() throws Exception {
        // Add a single pauseable component as a usr feature/bundle (SPI)
        server.setServerConfigurationFile("bad-pauseable-component.server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("pauseablecomponent-1.0");

        startStopServer(TestType.EXCEPTION);
    }

    /**
     * Long running test (at least 30s), push this into the full bucket.
     * This triggers a pauseable component that takes longer than 30s to complete.
     * Make sure we get a warning that not all pauseable activity completed (and that
     * we don't see the message indicating that it did).
     *
     * Note: This test expects the server stop to timeout and be forcefully terminated.
     * The server will be killed after detecting the hang warnings, resulting in RC 137.
     *
     * @throws Exception
     */
    @Test
    public void testLongRunningPauseableComponent() throws Exception {
        // Add a single pauseable component as a usr feature/bundle (SPI)
        server.setServerConfigurationFile("longrunning-pauseable-component.server.xml");
        server.installSystemBundle("test.server.quiesce");
        server.installSystemFeature("pauseablecomponent-1.0");

        startStopServer(TestType.PAUSEABLE_HANG);
    }

    private void startStopServer(TestType type) throws Exception {
        // start the server, do a clean start, and do not pre-clean the logs dir
        server.startServer(method.getMethodName() + ".console.log", true, false);

        // wait for port to start
        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain info msg about open port",
                             server.waitForStringInLog("CWWKO0219I", 0));

        // stop the server, do not clean up the archive
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        if (type == TestType.PAUSEABLE_HANG)
            server.stopServer(false, PAUSEABLE_COMPONENT_HUNG_WARNING, PAUSEABLE_FAILURE_WARNING);
        else
            server.stopServer(false);

        // Make sure stop has completed
        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain info msg about server stopped",
                             server.waitForStringInLog(SERVER_STOPPED_MESSAGE));

        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain info msg about the start of pauseable component processing",
                             server.waitForStringInLog(PAUSEABLE_START_MESSAGE, 0));

        Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName() + " should contain WHEE! because the test pauseable component was called",
                             server.waitForStringInLog(PAUSEABLE_COMPONENT_CALLED_MESSAGE, 0));

        if (type == TestType.EXCEPTION) {
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName()
                                 + " should contain WOOPS! because the test pauseable component threw an exception",
                                 server.waitForStringInLog(PAUSEABLE_COMPONENT_EXCEPTION_MESSAGE, 0));

        } else if (type == TestType.PAUSEABLE_HANG) {
            Assert.assertNull("FAIL for " + method.getMethodName() + ": " + server.getServerName()
                              + " should NOT contain info msg about the completion of pauseable component processing",
                              server.waitForStringInLog(PAUSEABLE_SUCCESS_MESSAGE, 0));
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName()
                                 + " should contain warning msg about the failure to complete pauseable component processing",
                                 server.waitForStringInLog(PAUSEABLE_FAILURE_WARNING, 0));
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName()
                                 + " should contain warning message indicating that 1 pauseable component hung",
                                 server.waitForStringInLog(PAUSEABLE_COMPONENT_HUNG_WARNING, 0));
        } else {
            Assert.assertNotNull("FAIL for " + method.getMethodName() + ": " + server.getServerName()
                                 + " should contain information about the completion of pauseable component processing",
                                 server.waitForStringInLog(PAUSEABLE_SUCCESS_MESSAGE, 0));
            Assert.assertNull("FAIL for " + method.getMethodName() + ": " + server.getServerName()
                              + " should NOT contain information about the failure to complete pauseable component processing",
                              server.waitForStringInLog(PAUSEABLE_FAILURE_WARNING, 0));
        }
    }

}