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

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CheckpointWithSecurityManager {
    @Rule
    public TestName testName = new TestName();

    @Server("checkpointWithSecurityManager")
    public static LibertyServer server;

    @Test
    @ExpectedFFDC({ "java.lang.UnsupportedOperationException", "io.openliberty.checkpoint.internal.criu.CheckpointFailedException" })
    @MaximumJavaLevel(javaLevel = 17)
    // Checkpoint does not support running with security manager. Verify checkpoint fails when security manager configured.
    public void testCheckpointJava2SecurityMaxJava17() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, true, true, null));
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int returnCode = output.getReturnCode();
        assertEquals("Wrong return code for failed checkpoint.", 72, returnCode);
    }

    @Test
    @MinimumJavaLevel(javaLevel = 18)
    //Test that checkpoint works when security manager is configured jvm>=18, since the framework does not
    // honor config to enable security manager on those jvm levels, it is a valid configuration for checkpoint
    public void testCheckpointJava2SecurityMinJava18() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, false, false, null));
        //Allow logged error 'CWWKE0955E: The websphere.java.security property was set ... but Java version is 21.'
        server.addCheckpointRegexIgnoreMessage(".*CWWKE0955E.*");
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int returnCode = output.getReturnCode();
        assertEquals("Wrong return code for successful checkpoint.", 0, returnCode);
        assertNotNull(server.findStringsInLogsAndTrace("CWWKE0955E: The websphere.java.security property was set in the bootstrap.properties"));
    }

    @After
    public void tearDown() throws Exception {
        if (testName.getMethodName().contains("MinJava18")) {
            //Allow 'CWWKE0955E: The websphere.java.security property was set ... but Java version is 21.'
            server.stopServer(".*CWWKE0955E.*");
        } else {
            server.stopServer();
        }
    }

}
