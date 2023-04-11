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

import static org.junit.Assert.assertNotNull;

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
import mapCacheApp.MapCache;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class MapCacheTest {

    public static final String WAR_APP_NAME = "mapCache";
    public static final String SERVER_NAME = "checkpointMapCache";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void exportWebApp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_APP_NAME + ".war").addClass(MapCache.class);
        ShrinkHelper.exportAppToServer(server, war, DeployOptions.OVERWRITE);
    }

    @Test
    public void testMapCacheOneSecWait() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("useInactivityParm").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, server -> {
            String result = server.waitForStringInLog("on start");
            assertNotNull("cache not populated at startup", result);
        });
        server.startServer();
        Thread.sleep(1000);
        server.checkpointRestore();
        Thread.sleep(1000);
        HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "value");

    }

    @Test
    public void testMapCacheSevenSecWait() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("useInactivityParm").setValue("false");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, server -> {
            String result = server.waitForStringInLog("on start");
            assertNotNull("cache not populated at startup", result);
        });
        server.startServer();
        Thread.sleep(7000);
        server.checkpointRestore();
        Thread.sleep(1000);
        HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "value");

    }

    @Test
    public void testMapCacheOneSecWaitInactivity() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("useInactivityParm").setValue("true");
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, server -> {
            String result = server.waitForStringInLog("on start");
            assertNotNull("cache not populated at startup", result);
        });

        server.startServer();
        Thread.sleep(7000);
        server.checkpointRestore();
        Thread.sleep(1000);
        HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "value");
        Thread.sleep(7000);
        HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "Key [key] not in cache");

    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

}
