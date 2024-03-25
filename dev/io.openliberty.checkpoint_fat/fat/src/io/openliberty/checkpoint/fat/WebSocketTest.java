/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import junit.framework.Assert;
import webSocketTest.AlternateEndpoint;
import webSocketTest.DefaultEndpoint;
import webSocketTest.SocketClient;
import webSocketTest.SocketStartup;

@RunWith(FATRunner.class)
@CheckpointTest
public class WebSocketTest {

    public static final String APP_NAME = "webSocketTest";

    @Server("webSocketServer")
    public static LibertyServer server;

    @BeforeClass
    public static void exportWebApp() throws Exception {
        server.deleteAllDropinApplications();
        WebArchive webappWar = ShrinkWrap.create(WebArchive.class, "webSocketWAR.war")
                        .addClass(AlternateEndpoint.class)
                        .addClass(DefaultEndpoint.class)
                        .addClass(SocketClient.class)
                        .addClass(SocketStartup.class);
        ShrinkHelper.exportAppToServer(server, webappWar, DeployOptions.OVERWRITE);
    }

    @Test
    public void testWebSocket() throws Exception {

        server.startServer();

        int responseCode = HttpUtils.getHttpConnection(server, "webSocket/startup").getResponseCode();

        if (responseCode < 200 || responseCode >= 300) {
            Assert.fail("non 200 HTTP response code: " + responseCode);
        }

        server.findStringsInLogs("MESSAGE CLIENT: data 1");
        server.findStringsInLogs("message received Server: data 1");
        server.findStringsInLogs("MESSAGE CLIENT: data 2");
        server.findStringsInLogs("message received Server: data 2");
        server.findStringsInLogs("MESSAGE CLIENT: data 3");
        server.findStringsInLogs("message received Server: data 3");

        server.stopServer();

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
        server.startServer();
        server.stopServer();

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("socketEndpoint").setValue("alternate");
        server.updateServerConfiguration(config);

        server.checkpointRestore();

        int responseCode2 = HttpUtils.getHttpConnection(server, "webSocket/startup").getResponseCode();

        if (responseCode2 < 200 || responseCode2 >= 300) {
            Assert.fail("non 200 HTTP response code: " + responseCode2);
        }

        server.findStringsInLogs("MESSAGE CLIENT: data 1");
        server.findStringsInLogs("message received alternate Server: data 1");
        server.findStringsInLogs("MESSAGE CLIENT: data 2");
        server.findStringsInLogs("message received alternate Server: data 2");
        server.findStringsInLogs("MESSAGE CLIENT: data 3");
        server.findStringsInLogs("message received alternate Server: data 3");
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
