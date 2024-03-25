/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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

import static io.openliberty.checkpoint.fat.FATSuite.configureBootStrapProperties;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static io.openliberty.checkpoint.spi.CheckpointPhase.AFTER_APP_START;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;

@RunWith(FATRunner.class)
@CheckpointTest
public class CheckpointFailTest {
    @Rule
    public TestName testName = new TestName();

    @Server("checkpointFailServer")
    public static LibertyServer server;

    @Before
    public void beforeEachTest() throws Exception {
        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
        configureBootStrapProperties(server, Collections.singletonMap("io.openliberty.checkpoint.fail.type", testMethod.getType()));
        server.setCheckpoint(new CheckpointInfo(AFTER_APP_START, false, testMethod.failCheckpoint(), testMethod.failRestore(), //
                        // do before checkpoint
                        s -> {
                            if (testMethod == TestMethod.testSystemCheckpointFailed) {
                                try {
                                    server.copyFileToLibertyServerRoot("logs/checkpoint/", "testlogs/checkpoint.log");
                                } catch (Exception e) {
                                    fail(e.getMessage());
                                }
                            }
                        }, //
                        // do before restore
                        s -> {
                            if (testMethod == TestMethod.testCriuRestoreFailedWithRecover) {
                                s.setCriuRestoreDisableRecovery(false);
                            }
                        }));
        ProgramOutput checkpointOutput = server.startServer(getTestMethodNameOnly(testName) + ".log");
        if (testMethod.failCheckpoint()) {
            testMethod.verifyOutput(checkpointOutput);
        }
        if (testMethod.failRestore()) {
            if (testMethod == TestMethod.testCriuRestoreFailed || testMethod == TestMethod.testCriuRestoreFailedWithRecover) {
                RemoteFile checkpointImage = server.getFileFromLibertyServerRoot("workarea/checkpoint/image");
                if (checkpointImage.isDirectory()) {
                    for (RemoteFile imageFile : checkpointImage.list(false)) {
                        // force a failure by removing the ids file from the image
                        if (imageFile.getName().startsWith("ids-")) {
                            imageFile.delete();
                        }
                    }
                } else {
                    fail("No image found");
                }
            }

            ProgramOutput restoreOutput = server.checkpointRestore(!testMethod.failRestore());
            testMethod.verifyOutput(restoreOutput);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (getTestMethod(TestMethod.class, testName) == TestMethod.testCriuRestoreFailedWithRecover) {
            // In the restore recover test, alert the LibertyServer class that a recovered server process is running.
            server.setStarted();
        }
        server.stopServer();
        // restore the default in case it was changed by the test
        server.setCriuRestoreDisableRecovery(true);
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testUnsupportedInJvm() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testDisabledInJvm() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testLibertyPrepareFailed() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testJvmCheckpointFailed() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testSystemCheckpointFailed() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testUnknownCheckpointFailed() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testSystemRestoreFailed() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testJvmRestoreFailed() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testLibertyRestoreFailed() {
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testUnknownRestoreFailed() {
    }

    @Test
    public void testCriuRestoreFailed() {
    }

    @Test
    public void testCriuRestoreFailedWithRecover() {
    }

    static enum TestMethod {
        testUnsupportedInJvm("UNSUPPORTED_IN_JVM", true, 70),
        testDisabledInJvm("UNSUPPORTED_DISABLED_IN_JVM", true, 71),
        testLibertyPrepareFailed("LIBERTY_PREPARE_FAILED", true, 72),
        testJvmCheckpointFailed("JVM_CHECKPOINT_FAILED", true, 73),
        testSystemCheckpointFailed("SYSTEM_CHECKPOINT_FAILED", true, 74, ".*CWWKE0962E.*TESTING CHECKPOINT.LOG.*"),
        testUnknownCheckpointFailed("UNKNOWN_CHECKPOINT", true, 75),
        // Note that 1 is by criu when it fails to restore
        testCriuRestoreFailed("SYSTEM_RESTORE_FAILED", false, 1, //
                              ".*CWWKE0964E: Restoring the checkpoint server process failed.*The server did not launch.*", //
                              ".*No IDS for root task.*"),
        testCriuRestoreFailedWithRecover("SYSTEM_RESTORE_FAILED", false, 0, // expect final RC of success from auto-recovery
                                         ".*CWWKE0961I: Restoring the checkpoint server process failed.*Launching the server without using the checkpoint image.*"
                                                                            + "Server.*started with process ID.*", //
                                         ".*No IDS for root task.*"),
        testSystemRestoreFailed("SYSTEM_RESTORE_FAILED", false, 80),
        testJvmRestoreFailed("JVM_RESTORE_FAILED", false, 81),
        testLibertyRestoreFailed("LIBERTY_RESTORE_FAILED", false, 82),
        testUnknownRestoreFailed("UNKNOWN_RESTORE", false, 83);

        private final String type;
        private final boolean failCheckpoint;
        private final int returnCode;
        private final String[] messages;

        boolean failCheckpoint() {
            return failCheckpoint;
        }

        boolean failRestore() {
            return !failCheckpoint;
        }

        String getType() {
            return type;
        }

        void verifyOutput(ProgramOutput output) {
            assertEquals("Wrong return code.", returnCode, output.getReturnCode());
            String stdOut = output.getStdout();
            for (String message : messages) {
                Pattern p = Pattern.compile(message, Pattern.DOTALL);
                Matcher m = p.matcher(stdOut);
                assertTrue("An expected regEx: [" + message + "] Could not be matched against stdOut: [" + stdOut + ']', m.matches());
            }
        }

        TestMethod(String type, boolean failCheckpoint, int returnCode, String... messages) {
            this.type = type;
            this.failCheckpoint = failCheckpoint;
            this.returnCode = returnCode;
            this.messages = messages;
        }
    }
}
