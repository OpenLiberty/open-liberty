/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This tests the functionality of the 
 * ServletContainerInitializer.onStartup(Set<Class<?>> c, ServletContext ctx) API.
 *
 * @See https://github.com/OpenLiberty/open-liberty/issues/16598
 *
 */
@RunWith(FATRunner.class)
public class WCSCIHandlesTypesTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    private static final String APP_NAME = "TestHandlesTypesClasses";

    @Server("servlet40_excludeAllHandledTypesClasses")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestHandlesTypesClasses to the server if not already present.");

        WCApplicationHelper.addWarToServerDropins(server, APP_NAME + ".war", true,
            "testhandlestypesclasses.war.examples", "testhandlestypesclasses.war.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCSCIHandlesTypesTest.class.getSimpleName() + ".log");
        WCApplicationHelper.waitForAppStart(APP_NAME, WCEncodingTest.class.getName(), server);
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test to verify that the correct set of Classes is passed via 
     * ServletContainerInitializer.onStartup(Set<Class<?>> c, ServletContext ctx)
     * 
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_ServletContainerInitializer_HandlesTypes_Classes() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME 
            + "/GetMappingTestServletSlashStar";
        LOG.info("url: " + url);
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);
                assertTrue("The response did not contain \"PASS\"", responseText.contains("PASS"));
            }
        }
    }

}
