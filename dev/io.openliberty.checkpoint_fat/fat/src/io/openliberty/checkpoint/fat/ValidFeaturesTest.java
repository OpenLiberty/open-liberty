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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class ValidFeaturesTest {
    @Rule
    public TestName testName = new TestName();

    @Server("ValidFeatures")
    public static LibertyServer server;

    @Test
    public void testMicroProfile4_1() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testMicroProfile5_0() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testMicroProfile6_0() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile8_0() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile9_1() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile10_0() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    @MinimumJavaLevel(javaLevel = 17)
    public void testWebProfile11_0() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile8_0_testMicroProfile4_1() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile9_1_testMicroProfile5_0() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile10_0_testMicroProfile6_0() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile8_0_ValueAdd() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile9_1_ValueAdd() throws Exception {
        doSuccesfulRestoreTest();
    }

    @Test
    public void testWebProfile10_0_ValueAdd() throws Exception {
        doSuccesfulRestoreTest();
    }

    public void doSuccesfulRestoreTest() throws Exception {
        server.startServer(getTestMethod(TestMethod.class, testName) + ".log");
        // should restore fine
        server.checkpointRestore();
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testEjb3_2() throws Exception {
        ProgramOutput out = server.startServer(getTestMethod(TestMethod.class, testName) + ".log");
        assertEquals("Wrong return code", 72, out.getReturnCode());
    }

    @Before
    public void beforeEachTest() throws Exception {
        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        if (testMethod == TestMethod.unknown) {
            fail("Test method is unknown");
        }
        try {
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            features.addAll(testMethod.getFeatures());
            server.updateServerConfiguration(config);

            setCheckpoint(testMethod);
        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }

    }

    private void setCheckpoint(TestMethod testMethod) {
        final boolean autoRestore = false;
        final boolean expectRestoreFailure = false;
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, autoRestore, testMethod.expectCheckpointFailure, expectRestoreFailure, (s) -> {
            try {
                // make sure we don't have any "feature not found" errors
                List<String> notFound = s.findStringsInLogs("CWWKF0001E");
                assertTrue("Feature not found: " + notFound, notFound.isEmpty());
            } catch (Exception e) {
                throw new AssertionError("Unexpected error.", e);
            }
        }));
    }

    @After
    public void afterEachTest() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.unsetCheckpoint();
            server.restoreServerConfiguration();
        }
    }

    private static String[] valueAddFeatures = { "restConnector-2.0",
                                                 "audit-1.0",
                                                 "bells-1.0",
                                                 "sessionDatabase-1.0",
                                                 "distributedMap-1.0",
                                                 "federatedRegistry-1.0",
                                                 "ldapRegistry-3.0",
                                                 "openidConnectClient-1.0",
                                                 "passwordUtilities-1.1",
                                                 "monitor-1.0",
                                                 "socialLogin-1.0",
                                                 "webCache-1.0" };

    static enum TestMethod {
        testWebProfile8_0("webProfile-8.0"),
        testWebProfile9_1("webProfile-9.1"),
        testWebProfile10_0("webProfile-10.0"),
        testWebProfile11_0("webProfile-11.0"),
        testMicroProfile4_1("microProfile-4.1"),
        testMicroProfile5_0("microProfile-5.0"),
        testMicroProfile6_0("microProfile-6.0"),
        testWebProfile8_0_testMicroProfile4_1("webProfile-8.0", "microProfile-4.1"),
        testWebProfile9_1_testMicroProfile5_0("webProfile-9.1", "microProfile-5.0"),
        testWebProfile10_0_testMicroProfile6_0("webProfile-10.0", "microProfile-6.0"),
        testWebProfile8_0_ValueAdd(false, true, "webProfile-8.0"),
        testWebProfile9_1_ValueAdd(false, true, "webProfile-9.1"),
        testWebProfile10_0_ValueAdd(false, true, "webProfile-10.0"),
        testEjb3_2(true, "ejb-3.2"),
        unknown();

        private final List<String> features;
        private final boolean expectCheckpointFailure;

        private TestMethod(String... features) {
            this(false, false, features);
        }

        private TestMethod(boolean expectCheckpointFailure, String... features) {
            this(expectCheckpointFailure, false, features);
        }

        private TestMethod(boolean expectCheckpointFailure, boolean valueAdd, String... features) {
            if (valueAdd) {
                List<String> temp = new ArrayList<String>(Arrays.asList(features));
                temp.addAll(Arrays.asList(valueAddFeatures));
                this.features = Collections.unmodifiableList(temp);
            } else {
                this.features = Collections.unmodifiableList(Arrays.asList(features));
            }
            this.expectCheckpointFailure = expectCheckpointFailure;
        }

        List<String> getFeatures() {
            return features;
        }
    }

}
