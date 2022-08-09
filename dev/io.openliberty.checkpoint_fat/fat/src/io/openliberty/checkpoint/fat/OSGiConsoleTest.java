/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class OSGiConsoleTest {

    public static final String APP_NAME = "app2";

    @Server("checkpointFATServer")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = MicroProfileActions.repeat("checkpointFATServer", TestMode.LITE, MicroProfileActions.MP41, MicroProfileActions.MP50);

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "app2");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Test
    public void testOSGiConsoleFeature() throws Exception {
        server.setConsoleLogName(name.getMethodName());

        Map<String, String> properties = new HashMap<>();
        properties.put("osgi.console", "5678");
        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add("osgiConsole-1.0");
        server.updateServerConfiguration(config);

        FATSuite.configureBootStrapProperties(server, properties);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();

        server.checkpointRestore();

        server.stopServer(false, "");
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "app2/request", "Got ServletA");
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

    @AfterClass
    public static void removeWebApp() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();
    }

}
