/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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

import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class TestWithFATServlet2 {

    public static final String APP_NAME = "app2";

    @Server("FATServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "app2");
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
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testMultCheckpointNoClean() throws Exception {
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");

        server.stopServer(false, "");
        server.startServerAndValidate(LibertyServer.DEFAULT_PRE_CLEAN, false /* clean start */,
                                      LibertyServer.DEFAULT_VALIDATE_APPS, false /* expectStartFailure */ );;
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @Test
    public void testCheckpointFeatureMissingError() throws Exception {
        server.setCheckpoint(CheckpointPhase.APPLICATIONS);
        ServerConfiguration svrCfg = server.getServerConfiguration();
        Set<String> features = svrCfg.getFeatureManager().getFeatures();
        features.remove("checkpoint-1.0");
        server.updateServerConfiguration(svrCfg);

        server.startServerAndValidate(LibertyServer.DEFAULT_PRE_CLEAN, LibertyServer.DEFAULT_CLEANSTART,
                                      LibertyServer.DEFAULT_VALIDATE_APPS, true /* expectStartFailure */ );

        assertNotNull("'CWWKF0048E: .* the checkpoint-1.0 feature was not configured in the server.xml file' message was not found",
                      server.waitForStringInLogUsingMark("CWWKF0048E: .* the checkpoint-1.0 feature was not configured in the server.xml file", 0));
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
