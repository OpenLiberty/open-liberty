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
import junit.framework.AssertionFailedError;
import mapCacheApp.MapCache;

//Verify correct function of distributed map w.r.t timing out of map entries.
// The behavior of the distributed map across a checkpoint/restore is not well defined and
// so we are only testing the correct function after a restore.
//
// The configuration of the distributed map is set in the application, mapCacheApp.MapCache

@RunWith(FATRunner.class)
@CheckpointTest
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
    public void testInactivityTimeout() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("useInactivityParm").setValue("true"); // timetolive is 20 secs
        server.updateServerConfiguration(config); //                             inactivity timeout is 4 secs

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(); // take a checkpoint
        server.checkpointRestore();

        //load servlet and verify mapCache entry is present.
        HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "value");

        //keep inactivity timeout from occurring for 8 secs
        for (int x = 0; x < 8; x++) {
            HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "value");
            Thread.sleep(1 * 1000);
        }

        //Allow (4 sec) inactivity timeout to occur and verify entry is gone from cache.
        Thread.sleep(8 * 1000);
        try {
            HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "Key [key] not in cache");
        } catch (AssertionFailedError afe) {
            // On slow systems entry does not always time out of cache in 8 sec so allow one retry
            Thread.sleep(8 * 1000);
            HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "Key [key] not in cache");
        }
    }

    @Test
    public void testIimeout() throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        config.getVariables().getById("useInactivityParm").setValue("false"); // timetolive is 8 secs
        server.updateServerConfiguration(config);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(); // take a checkpoint
        server.checkpointRestore();

        //servlet init(). Load mapCache. Timeout count start here.
        HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "value");

        //Wait a time less than the timeout
        Thread.sleep(5 * 1000);
        HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "value");

        //Allow item to timeout from cache
        Thread.sleep(10 * 1000);
        try {
            HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "Key [key] not in cache");
        } catch (AssertionFailedError afe) {
            // On slow systems entry does not always time out of cache on time so allow one retry
            Thread.sleep(8 * 1000);
            HttpUtils.findStringInUrl(server, "mapCache/servlet?key=key", "Key [key] not in cache");
        }
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }
}
