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
package io.openliberty.checkpoint.fat.mp;

import static io.openliberty.checkpoint.fat.mp.FATSuite.configureEnvVariable;
import static io.openliberty.checkpoint.fat.mp.FATSuite.emptyEnvVariable;
import static io.openliberty.checkpoint.fat.mp.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.mp.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.fat.utils.HealthFileUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPHealthTestFileBasedConfig extends FATServletClient {

    @Server("checkpointMPHealthFileBasedEmpty")
    public static LibertyServer server;

    /* different from "mphealth" app; this one returns UP wheras the other one fails */
    private static final String APP_NAME = "mphealthup";

    private static final String MESSAGE_LOG = "logs/messages.log";

    public TestMethod testMethod;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.MPHealthFileBasedRepeat("checkpointMPHealthFileBasedEmpty");

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        server.saveServerConfiguration();
        server.setCheckpoint(getCheckpointPhase(), true,
                             server -> {
                                 configureAndTestBeforeRestore();
                             });
        server.setConsoleLogName(getTestMethod(TestMethod.class, testName) + ".log");
        server.startServer(true, false); // Do not validate apps since we have a delayed startup.
    }

    private CheckpointPhase getCheckpointPhase() {
        CheckpointPhase phase = CheckpointPhase.AFTER_APP_START;
        switch (testMethod) {
            default:
                break;
        }
        return phase;
    }

    @Test
    public void testDefaultFileBasedHealthChecksEnvVarRestore() throws Exception {
        String name = getTestMethodNameOnly(testName);

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        /*
         * Ensure that the Application has started message exists (CWWKZ0001I) and then check that the files are there.
         */

        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Test that the expected file-based health check files are present");

        /*
         * We expect all files to be created.
         * App reports all UP and we've since enabled file-based health check
         * during restore.
         *
         * Expect:
         * [X] /health dir
         * [x] Started
         * [x] Ready
         * [x] Live
         *
         */
        assertTrue("Expected all files to be created: Review isAllHealthCheckFilesCreated logs for state of files.", HealthFileUtils.isFilesCreated(serverRootDirFile));

    }

    @Test
    public void testDisabledFileBasedHealthChecks() throws Exception {
        String name = getTestMethodNameOnly(testName);

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        /*
         * Ensure that the Application has started message exists (CWWKZ0001I) and then check that the files don't exist.
         */

        List<String> lines = server.findStringsInFileInLibertyServerRoot("CWWKZ0001I:", MESSAGE_LOG);
        assertEquals("The CWWKZ0001I Application started message did not appear in messages.log", 1, lines.size());

        Log.info(getClass(), name, "Testing now that no healthcheck files are present. Not even directory.");

        /*
         * We expect the directory and the files not to be created.
         * In this test, nothing changes and the original config does
         * not enable the file check functionality.
         *
         * Expect:
         * [ ] /health dir
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         *
         */

        assertFalse(HealthFileUtils.HEALTH_DIR_SHOULD_NOT_HAVE, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.STARTED_SHOULD_NOT_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.LIVE_SHOULD_NOT_HAVE, HealthFileUtils.getLiveFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.READY_SHOULD_NOT_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());

    }

    private void configureAndTestBeforeRestore() {

        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring during restore: " + testMethod);
            switch (testMethod) {
                case testDefaultFileBasedHealthChecksEnvVarRestore:
                    Log.info(getClass(), testName.getMethodName(), "Adding a server environmenvt value for test: " + testMethod);
                    Map<String, String> config = new HashMap<>();
                    config.put("MP_HEALTH_CHECK_INTERVAL", "15s");
                    configureEnvVariable(server, config);
                    break;
                default:
                    /*
                     * Make sure server.env has no values in it.
                     */
                    emptyEnvVariable(server);
                    Log.info(getClass(), testName.getMethodName(), "No configuration change required for test: " + testMethod);
                    break;
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }

        Log.info(getClass(), getTestMethodNameOnly(testName), "Testing that health files do not exist before restore");

        /*
         * This is a test before a restore.
         * We expect nothing to be created (the server.xml only had <mphealth />
         * Which is effectively disabled functionality for mpHealth.
         *
         * Expect:
         * [ ] /health dir
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         *
         */

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        assertFalse(HealthFileUtils.HEALTH_DIR_SHOULD_NOT_HAVE, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.STARTED_SHOULD_NOT_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.LIVE_SHOULD_NOT_HAVE, HealthFileUtils.getLiveFile(serverRootDirFile).exists());
        assertFalse(HealthFileUtils.READY_SHOULD_NOT_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer("CWMMH0052W");
        server.restoreServerConfiguration();
    }

    static enum TestMethod {
        testDefaultFileBasedHealthChecksEnvVarRestore,
        testDisabledFileBasedHealthChecks,
        unknown
    }
}
