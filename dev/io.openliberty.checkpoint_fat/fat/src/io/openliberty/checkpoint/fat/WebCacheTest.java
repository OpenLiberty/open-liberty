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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import WebCacheApp.CachedServlet;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class WebCacheTest {

    public static final String SERVER_NAME = "webCacheServer";
    public static final String APP_NAME = "webCacheApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        WebArchive webappWar = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClass(CachedServlet.class)
                        .addAsWebInfResource(new File(server.getInstallRoot() + "/usr/servers/" + server.getServerName() + "/cachespec.xml"));
        ShrinkHelper.exportAppToServer(server, webappWar, DeployOptions.OVERWRITE);
    }

    @Before
    public void checkpointRestoreServer() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
        server.startServer();
    }

    private String getResponse(String endpoint, String param, String expectedStart, String expectedFullRespose) throws IOException {
        String response = HttpUtils.getHttpResponseAsString(server, APP_NAME + endpoint + "?test=" + param);
        Log.info(getClass(), "Get cache response", response);
        assertTrue("Wrong response: " + response, response.startsWith(expectedStart));
        if (expectedFullRespose != null) {
            assertEquals("Wrong response:", expectedFullRespose, response);
        }
        return response;
    }

    @Test
    public void testWebCacheServlet() throws Exception {
        String responseA1 = getResponse("/cachedServlet", "A", "Hello from cachedServlet:", null);
        for (int i = 0; i < 20; i++) {
            getResponse("/cachedServlet", "A", "Hello from cachedServlet:", responseA1);
        }
        String responseB1 = getResponse("/cachedServlet", "B", "Hello from cachedServlet:", null);
        assertFalse("Wrong responseB1: " + responseB1, responseA1.equals(responseB1));
        for (int i = 0; i < 20; i++) {
            getResponse("/cachedServlet", "B", "Hello from cachedServlet:", responseB1);
        }

        Thread.sleep(20000);
        String responseA2 = getResponse("/cachedServlet", "A", "Hello from cachedServlet:", null);
        assertFalse("Wrong responseA2: " + responseA2, responseA1.equals(responseA2));
        String responseB2 = getResponse("/cachedServlet", "B", "Hello from cachedServlet:", null);
        assertFalse("Wrong responseB2: " + responseB2, responseB1.equals(responseB2));
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }
}