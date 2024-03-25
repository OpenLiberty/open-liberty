/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@OnlyIfSysProp("os.name=Linux")
@CheckpointTest(alwaysRun = true)
public class JavaInfoIsCriuSupportedTest extends FATServletClient {

    public static final String APP_NAME = "app2";

    @Server("checkpointFATServer")
    public static LibertyServer server;

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Test
    @AllowedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testFailedCheckpointIfRunningOnNonCRIUJVM() throws Exception {
        int returnCode = 0;
        if (JavaInfo.forCurrentVM().isCriuSupported()) {
            server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, false, false, null));
            ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
            returnCode = output.getReturnCode();
            assertEquals("Return code 0 expected", 0, returnCode);
        } else {
            server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, true, false, null));
            ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
            returnCode = output.getReturnCode();
            // return code 70 is returned when the JVM doesn't support CRIU at all
            // return code 71 is returned for Semeru when the criu dependency is not installed properly
            assertTrue("Return code 70 or 71 expected", returnCode == 70 || returnCode == 71);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }
}
