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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import openAPIapp.ApplicationRoot;
import openAPIapp.Endpoints;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class OpenAPITest {

    public static final String SERVER_NAME = "openAPIserver";

    public static final String APP_NAME = "restClient";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void exportWebApp() throws Exception {

        WebArchive webappWar = ShrinkWrap.create(WebArchive.class, "OpenAPI.war")
                        .addClass(ApplicationRoot.class)
                        .addClass(Endpoints.class);

        ShrinkHelper.exportAppToServer(server, webappWar, DeployOptions.OVERWRITE);
    }

    @Test
    public void testOpenAPI() throws Exception {

        server.startServer();
        HttpUtils.findStringInUrl(server, "/openapi", "- url: http://localhost:" + server.getHttpDefaultPort() + "/OpenAPI");
        server.stopServer();

        server.setCheckpoint(CheckpointPhase.FEATURES, false, null);
        server.startServer();

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("openAPIserver").setValue("alternateServer");
        server.updateServerConfiguration(config);

        server.checkpointRestore();

        HttpUtils.findStringInUrl(server, "/openapi", "- url: http://localhost:" + server.getHttpDefaultPort() + "/alternateServer");

    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
