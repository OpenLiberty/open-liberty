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

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import restClient.AlternateApp;
import restClient.AlternateEndpoint;
import restClient.ClientApp;
import restClient.ClientEndpoints;
import restClient.RESTclient;
import restClient.ServerApp;
import restClient.ServerEndpoint;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class RESTclientTest {

    public static final String APP_NAME = "restClient";

    @Server("restClientServer")
    public static LibertyServer server;

    @BeforeClass
    public static void copyAppToDropins() throws Exception {

        WebArchive webappWar = ShrinkWrap.create(WebArchive.class, "webappWAR.war")
                        .addClass(AlternateApp.class)
                        .addClass(AlternateEndpoint.class)
                        .addClass(ClientApp.class)
                        .addClass(ClientEndpoints.class)
                        .addClass(RESTclient.class)
                        .addClass(ServerApp.class)
                        .addClass(ServerEndpoint.class);

        ShrinkHelper.exportAppToServer(server, webappWar);
    }

    @Test
    public void testDefaultEndpoint() throws Exception {
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
        server.startServer();
        server.checkpointRestore();

        try {
            HttpUtils.findStringInUrl(server, "webappWAR/app/client/default", "{\'property\':\'value\'}");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            assertTrue(false);
        }

        try {
            HttpUtils.findStringInUrl(server, "webappWAR/app/client/alternate", "{\'property\':\'alternate value\'}");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            assertTrue(false);
        }

    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer();
    }

}
