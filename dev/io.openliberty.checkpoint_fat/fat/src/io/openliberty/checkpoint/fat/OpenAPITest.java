/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import openAPIapp.ApplicationRoot;
import openAPIapp.Endpoints;

@RunWith(FATRunner.class)
@CheckpointTest
public class OpenAPITest {

    public static final String SERVER_NAME = "openAPIserver";

    public static final String APP_NAME = "OpenAPI";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void exportWebApp() throws Exception {

        WebArchive webappWar = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClass(ApplicationRoot.class)
                        .addClass(Endpoints.class);

        ShrinkHelper.exportAppToServer(server, webappWar, DeployOptions.OVERWRITE);

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("openAPIserver").setValue("alternateServer");
        server.updateServerConfiguration(config);
    }

    @Test
    public void testOpenAPI() throws Exception {

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();

        server.checkpointRestore();

        HttpUtils.findStringInUrl(server, "/openapi", "- url: http://localhost:" + server.getHttpDefaultPort() + "/alternateServer");

    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
