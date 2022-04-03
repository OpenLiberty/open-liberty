/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class TestSPIConfig {
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
        assertNotNull("No message prefix found: " + expectedPrefix, actual);
        actual = actual.substring(actual.indexOf(expectedPrefix));
        assertEquals("Unexpected: ", expectedPrefix + expectedPostfix, actual);

    }

    @Test
    public void testRestoreWithDefaults() throws Exception {
        server.startServer();
        findLogMessage("No restore config", "TESTING - restore config: ", "pida=test1 pidb=test1", 0);
        findLogMessage("No RESTORED true found in restore", "TESTING - in restore method RESTORED", " - true -- true", 500);
    }

    @Test
    public void testRestoreWithEnvSet() throws Exception {
        server.startServer();
        findLogMessage("No restore config", "TESTING - modified config: pida=env2 pidb=env2", "", 0);
    }

    @Test
    public void testAddImmutableEnvKey() throws Exception {
        server.startServer();
        findLogMessage("Unexpected value for mutable key", "TESTING - in restore envs -", " v1 - v2 - v3 - v4", 500);
    }

    @Before
    public void setUp() throws Exception {
        TestMethod testMethod = getTestMethod();
        runBeforeCheckpoint(testMethod);
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, true,
                             server -> {
                                 findLogMessage("No prepare config", "TESTING - prepare config:", " pida=test1 pidb=test1", 0);
                                 findLogMessage("No RESTORED false found in prepare", "TESTING - in prepare method RESTORED", " - false -- false", 500);
                                 runBeforeRestore(testMethod);
                             });
    }

    /**
     * @param testMethod
     */
    private void runBeforeCheckpoint(TestMethod testMethod) {
        try {
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                case testAddImmutableEnvKey:
                    server.copyFileToLibertyServerRoot("addImmutableEnvKey/server.env");
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }
    }

    /**
     * @param testMethod
     */
    private void runBeforeRestore(TestMethod testMethod) {
        try {
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                case testAddImmutableEnvKey:
                    findLogMessage("No message for env keys", "TESTING - in prepare envs -", " v1 - v2 - null - null", 500);
                    break;
                case testRestoreWithEnvSet:
                    // environment value overrides defaultValue in restore
                    server.copyFileToLibertyServerRoot("envConfigChange/server.env");
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
    public void tearDown() throws Exception {
        server.stopServer();
        server.restoreServerConfiguration();
        server.deleteFileFromLibertyInstallRoot("server.env");
    }

    public TestMethod getTestMethod() {
        String testMethodSimpleName = testName.getMethodName();
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        try {
            return TestMethod.valueOf(testMethodSimpleName);
        } catch (IllegalArgumentException e) {
            Log.info(getClass(), testName.getMethodName(), "No configuration enum: " + testMethodSimpleName);
            return TestMethod.unknown;
        }
    }

    static enum TestMethod {
        testRestoreWithDefaults,
        testRestoreWithEnvSet,
        testAddImmutableEnvKey,
        unknown
    }

}
