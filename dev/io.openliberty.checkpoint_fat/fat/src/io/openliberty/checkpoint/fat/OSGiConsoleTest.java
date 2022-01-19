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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
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
public class OSGiConsoleTest {

    public static final String APP_NAME = "app2";

    @Server("FATServer")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "app2");
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    private void configureBootStrapProperties(Map<String, String> properties) throws Exception, IOException, FileNotFoundException {
        Properties bootStrapProperties = new Properties();
        File bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        bootStrapProperties.put("bootstrap.include", "../testports.properties");
        bootStrapProperties.putAll(properties);
        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.store(out, "");
        }
    }

    @Test
    public void testOSGiConsoleFeature() throws Exception {
        server.setConsoleLogName(name.getMethodName());

        Map<String, String> properties = new HashMap<>();
        properties.put("osgi.console", "5678");
        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add("osgiConsole-1.0");
        server.updateServerConfiguration(config);

        configureBootStrapProperties(properties);

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

}
