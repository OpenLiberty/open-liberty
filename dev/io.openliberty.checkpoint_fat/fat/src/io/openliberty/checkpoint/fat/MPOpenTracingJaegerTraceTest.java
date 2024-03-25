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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPOpenTracingJaegerTraceTest {

    @Rule
    public TestName testName = new TestName();

    public static LibertyServer server1;

    public static LibertyServer server2;

    /**
     * Deploy the application
     *
     * @throws Exception Errors deploying the application.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        server1 = LibertyServerFactory.getLibertyServer("checkpointMPOpenTracing-jaegerServerSystem");
        server2 = LibertyServerFactory.getLibertyServer("checkpointMPOpenTracing-jaegerServerInventory");

        WebArchive systemWar = ShrinkWrap.create(WebArchive.class, "system.war");
        WebArchive inventoryWar = ShrinkWrap.create(WebArchive.class, "inventory.war");

        systemWar.addPackages(true, "io.openliberty.guides.system");
        inventoryWar.addPackages(true, "io.openliberty.guides.inventory");

        ShrinkHelper.exportAppToServer(server1, systemWar);
        ShrinkHelper.exportAppToServer(server2, inventoryWar);

        File libsDir = new File("lib");
        File[] libs = libsDir.listFiles();
        for (File file : libs) {
            server1.copyFileToLibertyServerRoot(file.getParent(), "jaegerLib", file.getName());
            server2.copyFileToLibertyServerRoot(file.getParent(), "jaegerLib", file.getName());
        }
    }

    @AfterClass
    public static void shutdown() throws Exception {
        LibertyServer[] serversToShutDown = { server1, server2 };
        for (LibertyServer server : serversToShutDown) {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        }
    }

    /**
     * Check that spans are created across multiple servers.
     *
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testMultiSpans() throws Exception {
        //checkpoint system server
        server1.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server1.startServer(testName.getMethodName() + ".log");
        assertEquals("Expected checkpoint message not found", 1, server1.findStringsInLogs("CWWKC0451I", server1.getDefaultLogFile()).size());
        server1.copyFileToLibertyServerRoot("mpOpenTracing/server.env"); //Update the server.env with JAEGER_SAMPLER_PARAM=0
        server1.checkpointRestore();
        assertEquals("Expected restore message not found", 1, server1.findStringsInLogs("CWWKC0452I", server1.getDefaultLogFile()).size());

        //checkpoint inventory server
        server2.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server2.startServer(testName.getMethodName() + ".log");
        assertEquals("Expected checkpoint message not found", 1, server2.findStringsInLogs("CWWKC0451I", server2.getDefaultLogFile()).size());
        server2.checkpointRestore();
        assertEquals("Expected restore message not found", 1, server2.findStringsInLogs("CWWKC0452I", server2.getDefaultLogFile()).size());

        // Both servers should have a tracer created when hitting the localhost endpoint on the Inventory server
        URL url = createURL(server2, "inventory/systems/localhost");
        String response = HttpUtils.getHttpResponseAsString(url);
        assertNotNull(response);
        assertTrue(response.contains("at=AFTER_APP_START"));

        // Jaeger tracing starts after the http request
        String logMsg = server1.waitForStringInLog("INFO io.jaegertracing");
        String logMsg2 = server2.waitForStringInLog("INFO io.jaegertracing");

        assertNotNull(logMsg);
        assertTrue(logMsg.contains("sampler.param=false")); //Environment variable JAEGER_SAMPLER_PARAM=0
        assertNotNull(logMsg2);
        assertTrue(logMsg2.contains("sampler.param=true")); //Environment variable JAEGER_SAMPLER_PARAM=1
    }

    public static URL createURL(LibertyServer server, String path) throws MalformedURLException {
        if (!path.startsWith("/"))
            path = "/" + path;
        return new URL("http://" + server.getHostname() + ":" + server.getHttpSecondaryPort() + path);
    }
}
