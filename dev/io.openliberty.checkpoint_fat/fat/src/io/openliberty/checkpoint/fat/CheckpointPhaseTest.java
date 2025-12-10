/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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

import static app2.EarlyStartupAppCode.PROP_FAIL_STARTUP;
import static io.openliberty.checkpoint.fat.FATSuite.configureBootStrapProperties;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static io.openliberty.checkpoint.fat.FATSuite.removeBootStrapProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CheckpointPhaseTest {
    @Rule
    public TestName testName = new TestName();
    public static final String APP_NAME = "app2";

    @Server("checkpointPhaseServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "app2");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Test
    public void testAfterAppStartMultRestore() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, (s) -> {
            assertNotNull("App code should have run.", server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
        }));
        server.startServer();
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
        assertEquals("Unexpected app code ran.", null, server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));

        server.stopServer(false, "");
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testLogDirLinkEnvChange() throws Exception {

        File logs = new File(server.getServerRoot() + "/logs");
        if (logs.exists()) {
            server.deleteDirectoryFromLibertyServerRoot("logs");
        }
        new File(server.getServerRoot() + "/logsLink1").mkdirs();
        Files.createSymbolicLink(logs.toPath(), Paths.get("logsLink1"));

        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, (s) -> {
            assertNotNull("App code should have run.", server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
        }));
        server.startServerAndValidate(false, true, LibertyServer.validateApps);

        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");

        // Set the LOG_DIR and switch the symbolic link;
        // this mimics what the operator does for serviceability of logs
        Files.delete(logs.toPath());
        new File(server.getServerRoot() + "/logsLink2").mkdirs();
        Files.createSymbolicLink(logs.toPath(), Paths.get("logsLink2"));
        FATSuite.configureEnvVariable(server, Collections.singletonMap("LOG_DIR", server.getServerRoot() + "/logsLink2"));

        // Not validating if the server started on restore because the LibertyServer is not equipped to handle this scenario with log dir changing.
        // It does not know how to look in the new location.
        // Instead of trying to enhance LibertyServer for this very limited scenario we validate the expected console log below.
        server.checkpointRestore(false);
        // Mark the server as started so it will be properly stopped after test; validation code typically does this; but that was skipped above.
        server.setStarted();

        // Note that we use the original logLink1 location for the console log because InstantOn (criu) does not currently
        // allow us to swap out the file the process is piping to.
        server.waitForStringInLog("Launching checkpointFATServer", 100, server.getFileFromLibertyServerRoot("logsLink1/" + getTestMethodNameOnly(testName)));
        // Note that the messages.log is checked in the new linked location because we expect the restored Liberty logger
        // to follow the new link from the logs->logsLink2
        server.waitForStringInLog("Launching checkpointFATServer", 100, server.getFileFromLibertyServerRoot("logsLink2/messages.log"));

        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException", "com.ibm.ws.container.service.state.StateChangeException", "io.openliberty.checkpoint.internal.criu.CheckpointFailedException" })
    public void testFailAppStart() throws Exception {
        configureBootStrapProperties(server, Collections.singletonMap(PROP_FAIL_STARTUP, "true"));
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, true, true, (s) -> {
            assertNotNull("App code should have run.", server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
        }));
        ProgramOutput output = server.startServer();
        assertEquals("Wrong return code from checkpoint", 72, output.getReturnCode());
    }

    @Test
    public void testBeforeAppStart() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.BEFORE_APP_START, true, (s) -> {
            assertEquals("Unexpected app code ran.", null, s.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
        }));
        server.startServer();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
        assertNotNull("App code should have run.", server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
    }

    @Test
    public void testMultCheckpointNoClean() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        server.startServerAndValidate(LibertyServer.DEFAULT_PRE_CLEAN, false /* clean start */,
                                      LibertyServer.DEFAULT_VALIDATE_APPS, false /* expectStartFailure */ );
        restoreServerCheckConsoleLogHeader(server);
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    private void restoreServerCheckConsoleLogHeader(LibertyServer server) throws Exception {
        server.checkpointRestore();
        server.waitForStringInLog("Launching checkpointFATServer", 100);
    }

    @Before
    public void setConsoleLogName() {
        server.setConsoleLogName(getTestMethodNameOnly(testName));
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
        removeBootStrapProperties(server, PROP_FAIL_STARTUP);
    }

}
