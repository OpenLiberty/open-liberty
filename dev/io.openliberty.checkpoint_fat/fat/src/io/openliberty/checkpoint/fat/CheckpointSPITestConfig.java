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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CheckpointSPITestConfig {
    @Rule
    public TestName testName = new TestName();

    public final static String STATIC_SINGLE_PREPARE = "STATIC SINGLE PREPARE - ";
    public final static String STATIC_SINGLE_RESTORE = "STATIC SINGLE RESTORE - ";

    public final static String STATIC_SINGLE_PREPARE_RANK = "STATIC SINGLE PREPARE RANK - ";
    public final static String STATIC_SINGLE_RESTORE_RANK = "STATIC SINGLE RESTORE RANK - ";

    public final static String STATIC_MULTI_PREPARE = "STATIC MULTI PREPARE - ";
    public final static String STATIC_MULTI_RESTORE = "STATIC MULTI RESTORE - ";

    public final static String STATIC_MULTI_PREPARE_RANK = "STATIC MULTI PREPARE RANK - ";
    public final static String STATIC_MULTI_RESTORE_RANK = "STATIC MULTI RESTORE RANK - ";

    public final static String STATIC_ONRESTORE = "STATIC ONRESTORE - ";

    public final static String WITH_LOCK = "WITH LOCK - ";

    private static final String USER_FEATURE_PATH = "usr/extension/lib/features/";
    private static final String USER_BUNDLE_PATH = "usr/extension/lib/";
    private static final String USER_FEATURE_USERTEST_MF = "features/test.checkpoint.config-1.0.mf";
    private static final String USER_FEATURE_USERTEST_JAR = "bundles/test.checkpoint.config.bundle.jar";

    @Server("TestSPIConfig")
    public static LibertyServer server;

    @BeforeClass
    public static void addTestFeatureAndCheckpoint() throws Exception {
        server.copyFileToLibertyInstallRoot(USER_FEATURE_PATH, USER_FEATURE_USERTEST_MF);
        server.copyFileToLibertyInstallRoot(USER_BUNDLE_PATH, USER_FEATURE_USERTEST_JAR);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
        // check for proper messages on the checkpoint side:
        findLogMessage("No prepare config", "TESTING - prepare config: ", "a=test1 b=test1 c=test1 d=test1", 0);
        findLogMessage("No RESTORED false found in prepare", "TESTING - in prepare method RESTORED", " - false -- false", 500);
        findLogMessage("Activate should have null running condition", "TESTING - activate running condition: ", "null", 500);
        findLogMessage("Prepare should have null running condition", "TESTING - prepare running condition: ", "null", 500);

        findLogMessage("Static single prepare method", STATIC_SINGLE_PREPARE, "SUCCESS", 500);
        findLogMessage("Static single prepare method", STATIC_MULTI_PREPARE, "SUCCESS", 500);

        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "50 1 ", "SUCCESS", 500);
        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "50 2 ", "SUCCESS", 500);
        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "50 3 ", "SUCCESS", 500);
        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "0 1 ", "SUCCESS", 500);
        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "0 2 ", "SUCCESS", 500);
        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "0 3 ", "SUCCESS", 500);
        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "-50 1 ", "SUCCESS", 500);
        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "-50 2 ", "SUCCESS", 500);
        findLogMessage("Static rank prepare method", STATIC_MULTI_PREPARE_RANK + "-50 3 ", "SUCCESS", 500);

        findLogMessage("ProtectedString should be *****", "TESTING - ProtectedString prepare password: ", "*****", 500);

        findLogMessage("runWithCheckpointLock", WITH_LOCK + "CALLED: ", "SUCCESS", 500);
        findLogMessage("runWithCheckpointLock", WITH_LOCK + "NESTED IN READ: ", "SUCCESS", 500);
        findLogMessage("runWithCheckpointLock", WITH_LOCK + "DELAYED: ", "SUCCESS", 500);
        findLogMessage("runWithCheckpointLock", WITH_LOCK + "NESTED IN WRITE: ", "SUCCESS", 500);
    }

    @Before
    public void beforeEachTest() throws Exception {
        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        server.setArchiveMarker(testMethod + ".marker");

        runBeforeRestore(testMethod);
        server.checkpointRestore();
    }

    /**
     * @param testMethod
     */
    private void runBeforeRestore(TestMethod testMethod) {
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                case testRestoreWithEnvSet:
                    // environment value overrides defaultValue in restore
                    server.copyFileToLibertyServerRoot("envConfigChange/server.env");
                    // need to delete the default out of variables because they take precedence over env
                    server.deleteFileFromLibertyServerRoot("variables/c_value");
                    server.deleteFileFromLibertyServerRoot("variables/d_value_props.properties");
                    break;
                case testRestoreWithDropinConfig:
                    // dropin configs value overrides defaultValue in restore
                    server.addDropinOverrideConfiguration("dropinConfigChange/override.xml");
                    break;
                case testRestoreWithVariableDirConfig:
                    // add files to variables directory that overrides defaultValue in restore
                    new File(server.getServerRoot(), "variables").mkdirs();
                    server.copyFileToLibertyServerRoot("variables", "configVariables/a_value");
                    server.copyFileToLibertyServerRoot("variables", "configVariables/b_value");
                    server.copyFileToLibertyServerRoot("variables", "configVariables/c_value");
                    server.copyFileToLibertyServerRoot("variables", "configVariables/d_value_props.properties");
                case testStaticHook:
                case testProtectedString:
                case testRestoreWithDefaults:
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
        server.stopServer();
        server.deleteFileFromLibertyServerRoot("server.env");
        server.deleteDropinOverrideConfiguration("override.xml");
        server.deleteFileFromLibertyServerRoot("variables/a_value");
        server.deleteFileFromLibertyServerRoot("variables/b_value");
        server.deleteFileFromLibertyServerRoot("variables/c_value");
        server.copyFileToLibertyServerRoot("variables", "configVariablesDefaults/c_value");
        server.copyFileToLibertyServerRoot("variables", "configVariablesDefaults/d_value_props.properties");
    }

    @AfterClass
    public static void cleanup() throws Exception {
        server.deleteFileFromLibertyInstallRoot(USER_FEATURE_PATH + USER_FEATURE_USERTEST_MF);
        server.deleteFileFromLibertyInstallRoot(USER_BUNDLE_PATH + USER_FEATURE_USERTEST_JAR);
    }

    private static void findLogMessage(String testMessage, String expectedPrefix, String expectedPostfix, long timeout) {
        String actual = server.waitForStringInLogUsingMark(expectedPrefix, timeout);
        assertNotNull(testMessage + ": No message prefix found: " + expectedPrefix, actual);
        actual = actual.substring(actual.indexOf(expectedPrefix));
        assertEquals(testMessage + ": Unexpected: ", expectedPrefix + expectedPostfix, actual);
    }

    @Test
    public void testRestoreWithDefaults() throws Exception {
        findLogMessage("No restore config", "TESTING - restore config: ", "a=test1 b=test1 c=test1 d=test1", 0);
        findLogMessage("No RESTORED true found in restore", "TESTING - in restore method RESTORED", " - true -- true", 500);
        findLogMessage("Restore should have null running condition", "TESTING - restore running condition: ", "null", 500);
        findLogMessage("Bind should have non-null running condition", "TESTING - bind running condition: ", "io.openliberty.process.running AFTER_APP_START", 500);
    }

    @Test
    public void testRestoreWithEnvSet() throws Exception {
        findLogMessage("No restore config", "TESTING - modified config: ", "a=env2 b=env2 c=env2 d=env2", 500);
    }

    @Test
    public void testRestoreWithDropinConfig() throws Exception {
        findLogMessage("No restore config", "TESTING - modified config: ", "a=override b=override c=override d=override", 500);
    }

    @Test
    public void testRestoreWithVariableDirConfig() throws Exception {
        findLogMessage("No restore config", "TESTING - modified config: ", "a=fileValue b=fileValue c=fileValue d=fileValue", 500);
    }

    @Test
    public void testStaticHook() throws Exception {
        findLogMessage("Static single restore method", STATIC_SINGLE_RESTORE, "SUCCESS", 500);
        findLogMessage("Static single restore method", STATIC_MULTI_RESTORE, "SUCCESS", 500);

        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "-50 1 ", "SUCCESS", 500);
        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "-50 2 ", "SUCCESS", 500);
        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "-50 3 ", "SUCCESS", 500);
        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "0 1 ", "SUCCESS", 500);
        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "0 2 ", "SUCCESS", 500);
        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "0 3 ", "SUCCESS", 500);
        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "50 1 ", "SUCCESS", 500);
        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "50 2 ", "SUCCESS", 500);
        findLogMessage("Static onRestore method", STATIC_ONRESTORE + "50 3 ", "SUCCESS", 500);

        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "-50 4 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "-50 5 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "-50 6 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "0 4 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "0 5 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "0 6 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "50 4 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "50 5 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method multi threaded", STATIC_MULTI_RESTORE_RANK + "50 6 ", "SUCCESS", 500);

        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "-50 4 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "-50 5 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "-50 6 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "0 4 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "0 5 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "0 6 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "50 4 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "50 5 ", "SUCCESS", 500);
        findLogMessage("Static rank restore method single threaded", STATIC_SINGLE_RESTORE_RANK + "50 6 ", "SUCCESS", 500);
    }

    @Test
    public void testProtectedString() throws Exception {
        String firstRestore = server.waitForStringInLogUsingMark("TESTING - ProtectedString restore password: ", 500);
        assertNotNull("No restored ProtectedString found.", firstRestore);
        server.stopServer();

        server.checkpointRestore();
        String secondRestore = server.waitForStringInLogUsingMark("TESTING - ProtectedString restore password: ", 500);
        assertNotNull("No restored ProtectedString found.", secondRestore);

        // the two trace strings must be different
        assertFalse("ProtectedString traces strings must be different: " + firstRestore + " - " + secondRestore, firstRestore.equals(secondRestore));
    }

    static enum TestMethod {
        testRestoreWithDefaults,
        testRestoreWithEnvSet,
        testRestoreWithDropinConfig,
        testRestoreWithVariableDirConfig,
        testStaticHook,
        testProtectedString,
        unknown
    }

}
