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

    @Test
    public void testRestoreWithDefaults() throws Exception {
        server.startServer();
        assertNotNull("No restore config",
                      server.waitForStringInLogUsingMark("TESTING - restore config: pida=test1 pidb=test1", 0));
        assertNotNull("No RESTORED true found in restore",
                      server.waitForStringInLogUsingMark("TESTING - in restore method RESTORED - true -- true", 500));
    }

    @Test
    public void testRestoreWithEnvSet() throws Exception {
        server.startServer();
        assertNotNull("No restore config",
                      server.waitForStringInLogUsingMark("TESTING - modified config: pida=env2 pidb=env2", 0));
        assertNotNull("No RESTORED true found in restore",
                      server.waitForStringInLogUsingMark("TESTING - in restore method RESTORED - true -- true", 500));

    }

    @Before
    public void setUp() throws Exception {
        TestMethod testMethod = getTestMethod();
        configureBeforeCheckpoint(testMethod);
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, true,
                             server -> {
                                 assertNotNull("No prepare config",
                                               server.waitForStringInLogUsingMark("TESTING - prepare config: pida=test1 pidb=test1", 0));
                                 assertNotNull("No RESTORED false found in prepare",
                                               server.waitForStringInLogUsingMark("TESTING - in prepare method RESTORED - false -- false", 500));
                                 configureBeforeRestore(testMethod);
                             });

    }

    /**
     * @param testMethod
     */
    private void configureBeforeCheckpoint(TestMethod testMethod) {
        // do nothing for now
    }

    /**
     * @param testMethod
     */
    private void configureBeforeRestore(TestMethod testMethod) {
        try {
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
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
        unknown
    }

}
