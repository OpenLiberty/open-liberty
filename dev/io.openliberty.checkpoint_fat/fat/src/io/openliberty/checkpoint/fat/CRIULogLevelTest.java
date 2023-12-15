/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import static io.openliberty.checkpoint.fat.FATSuite.configureEnvVariable;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CRIULogLevelTest {
    static final String CRIU_LOG_LEVEL = "CRIU_LOG_LEVEL";
    @Rule
    public TestName testName = new TestName();

    @Server("CRIULogLevelTest")
    public static LibertyServer server;

    TestMethod testMethod = TestMethod.unknown;

    @Before
    public void runBeforeCheckpoint() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            configureEnvVariable(server, Collections.singletonMap(CRIU_LOG_LEVEL, testMethod.level));
            setCheckpoint(testMethod);
        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }

    }

    private void setCheckpoint(TestMethod testMethod) {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, testMethod.expectFailure, testMethod.expectFailure, server -> {
            runBeforeRestore(testMethod);
        }));
    }

    /**
     * @param testMethod
     */
    private void runBeforeRestore(TestMethod testMethod) {
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring for restore: " + testMethod);
            if (!testMethod.expectFailure) {
                // always set to something different from checkpoint
                configureEnvVariable(server, Collections.singletonMap(CRIU_LOG_LEVEL, "1"));
            }
        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }
    }

    @After
    public void afterEachTest() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.unsetCheckpoint();
        }
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testNegativeLevel() throws Exception {
        doTest();
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testNonIntegerLevel() throws Exception {
        doTest();
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testTooBigLevel() throws Exception {
        doTest();
    }

    @Test
    public void testHighestLevel() throws Exception {
        doTest();
    }

    private void doTest() throws Exception {
        ProgramOutput checkpointOutput = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int retureCode = checkpointOutput.getReturnCode();
        if (testMethod.expectFailure) {
            assertEquals("Wrong return code for failed checkpoint.", 75, retureCode);
        } else {
            assertEquals("Wrong return code for successful checkpoint.", 0, retureCode);
            ProgramOutput restoreOutput = server.checkpointRestore();
            int restoreRetureCode = restoreOutput.getReturnCode();
            assertEquals("Wrong return code for successful restore.", 0, restoreRetureCode);
        }
    }

    static enum TestMethod {
        testNegativeLevel("-2", true),
        testNonIntegerLevel("fooLevel", true),
        testTooBigLevel("100", true),
        testHighestLevel("4", false),
        unknown("unknown", true);

        final String level;
        final boolean expectFailure;

        private TestMethod(String level, boolean expectFailure) {
            this.level = level;
            this.expectFailure = expectFailure;
        }
    }

}
