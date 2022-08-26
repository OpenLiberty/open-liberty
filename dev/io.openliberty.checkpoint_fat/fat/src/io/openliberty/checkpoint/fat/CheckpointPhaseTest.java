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

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class CheckpointPhaseTest {
    @Rule
    public TestName testName = new TestName();
    public static final String APP_NAME = "app2";

    @Server("checkpointFATServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = MicroProfileActions.repeat("checkpointFATServer", TestMode.LITE, MicroProfileActions.MP41, MicroProfileActions.MP50);

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "app2");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Test
    public void testAtFeatures() throws Exception {
        server.setCheckpoint(CheckpointPhase.FEATURES);
        server.startServer();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testAtApplicationsMultRestore() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.APPLICATIONS, false, (s) -> {
            assertNotNull("App code should have run.", server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
        }));
        server.startServer();
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
        assertEquals("Unexpected app code ran.", null, server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));

        server.stopServer(false, "");
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testAtDeployment() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.DEPLOYMENT, true, (s) -> {
            assertEquals("Unexpected app code ran.", null, s.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
        }));
        server.startServer();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
        assertNotNull("App code should have run.", server.waitForStringInLogUsingMark("TESTING - contextInitialized", 100));
    }

    @Test
    public void testMultCheckpointNoClean() throws Exception {
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        server.startServerAndValidate(LibertyServer.DEFAULT_PRE_CLEAN, false /* clean start */,
                                      LibertyServer.DEFAULT_VALIDATE_APPS, false /* expectStartFailure */ );
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testCheckpointFeatureMissingError() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.APPLICATIONS, false, true, true, null));
        ServerConfiguration svrCfg = server.getServerConfiguration();
        Set<String> features = svrCfg.getFeatureManager().getFeatures();
        features.remove("checkpoint-1.0");
        server.updateServerConfiguration(svrCfg);

        server.startServerAndValidate(LibertyServer.DEFAULT_PRE_CLEAN, LibertyServer.DEFAULT_CLEANSTART,
                                      LibertyServer.DEFAULT_VALIDATE_APPS, true /* expectStartFailure */ );

        assertNotNull("'CWWKF0048E:",
                      server.waitForStringInLogUsingMark("CWWKF0048E: .* the checkpoint-1.0 feature is not configured in the server.xml file", 0));
    }

    @Before
    public void setConsoleLogName() {
        server.setConsoleLogName(getTestMethodNameOnly(testName));
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
