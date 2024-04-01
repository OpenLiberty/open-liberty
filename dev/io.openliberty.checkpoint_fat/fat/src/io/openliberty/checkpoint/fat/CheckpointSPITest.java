/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CheckpointSPITest {
    @Rule
    public TestName testName = new TestName();

    private static final String USER_FEATURE_PATH = "usr/extension/lib/features/";
    private static final String USER_BUNDLE_PATH = "usr/extension/lib/";
    private static final String USER_FEATURE_USERTEST_MF = "features/test.checkpoint.config-1.0.mf";
    private static final String USER_FEATURE_USERTEST_JAR = "bundles/test.checkpoint.config.bundle.jar";

    @Server("TestSPIConfig")
    public static LibertyServer server;

    @BeforeClass
    public static void addTestFeature() throws Exception {
        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, USER_FEATURE_USERTEST_MF);
        server.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, USER_FEATURE_USERTEST_JAR);

    }

    @AfterClass
    public static void cleanup() throws Exception {
        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + USER_FEATURE_USERTEST_MF);
        server.deleteFileFromLibertyInstallRoot(USER_BUNDLE_PATH + USER_FEATURE_USERTEST_JAR);
    }

    private void findLogMessage(String testMessage, String expectedPrefix, String expectedPostfix, long timeout) {
        String actual = server.waitForStringInLogUsingMark(expectedPrefix, timeout);
        assertNotNull(testMessage + ": No message prefix found: " + expectedPrefix, actual);
        actual = actual.substring(actual.indexOf(expectedPrefix));
        assertEquals(testMessage + ": Unexpected: ", expectedPrefix + expectedPostfix, actual);
    }

    @Test
    public void testAddImmutableEnvKey() throws Exception {
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        findLogMessage("Unexpected value for mutable key", "TESTING - in restore envs -", " v1 - v2 - v3 - v4", 500);
    }

    @Test
    public void testRunningConditionLaunch() throws Exception {
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        findLogMessage("Activate should have non-null running condition", "TESTING - activate running condition: ", "io.openliberty.process.running null", 500);
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testFailedCheckpoint() throws Exception {
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int retureCode = output.getReturnCode();
        assertEquals("Wrong return code for failed checkpoint.", 72, retureCode);
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testFailedRestore() throws Exception {
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        ProgramOutput output = server.checkpointRestore();
        int retureCode = output.getReturnCode();
        assertEquals("Wrong return code for failed checkpoint.", 82, retureCode);
    }

    @Before
    public void beforeEachTest() throws Exception {
        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        try {
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            // first switch is to conditionally change the configuration before checkpoint
            switch (testMethod) {
                case testAddImmutableEnvKey:
                    server.copyFileToLibertyServerRoot("addImmutableEnvKey/server.env");
                    break;
                case testFailedCheckpoint:
                    server.copyFileToLibertyServerRoot("TestSPIConfig.fail.checkpoint/server.xml");
                    break;
                case testFailedRestore:
                    server.copyFileToLibertyServerRoot("TestSPIConfig.fail.restore/server.xml");
                    server.copyFileToLibertyServerRoot("TestSPIConfig.fail.restore/server.env");
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }
            // second switch is to conditionally set the checkpoint
            switch (testMethod) {
                case testRunningConditionLaunch:
                    break;
                default:
                    setCheckpoint(testMethod);
                    Log.info(getClass(), testName.getMethodName(), "Setting the checkpoint for " + testMethod);
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }

    }

    private void setCheckpoint(TestMethod testMethod) {
        boolean autoRestore = true;
        boolean expectCheckpointFailure = false;
        boolean expectRestoreFailure = false;
        if (testMethod == TestMethod.testFailedCheckpoint) {
            autoRestore = false;
            expectCheckpointFailure = true;
        } else if (testMethod == TestMethod.testFailedRestore) {
            autoRestore = false;
            expectRestoreFailure = true;
        }
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, autoRestore, expectCheckpointFailure, expectRestoreFailure, server -> {
            findLogMessage("No RESTORED false found in prepare", "TESTING - in prepare method RESTORED", " - false -- false", 500);
            findLogMessage("Activate should have null running condition", "TESTING - activate running condition: ", "null", 500);
            findLogMessage("Prepare should have null running condition", "TESTING - prepare running condition: ", "null", 500);
            runBeforeRestore(testMethod);
        }));
    }

    /**
     * @param testMethod
     */
    private void runBeforeRestore(TestMethod testMethod) {
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                case testAddImmutableEnvKey:
                    findLogMessage("No message for env keys", "TESTING - in prepare envs -", " v1 - v2 - null - null", 500);
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }
    }

    @After
    public void afterEachTest() throws Exception {
        try {
            server.stopServer();
            server.restoreServerConfiguration();
            server.deleteFileFromLibertyServerRoot("server.env");

        } finally {
            server.unsetCheckpoint();
        }
    }

    static enum TestMethod {
        testAddImmutableEnvKey,
        testRunningConditionLaunch,
        testFailedCheckpoint,
        testFailedRestore,
        unknown
    }

}
