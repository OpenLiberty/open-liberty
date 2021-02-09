/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a test class for the servlet-4.0 feature to test the behavior when servletPathForDefaultMapping
 * is true. There is no need to set the property in the server.xml because the value is true by default
 * for the servlet-4.0 feature.
 *
 */
@RunWith(FATRunner.class)
public class WCServletPathForDefaultMappingDefault {

    private static final Logger LOG = Logger.getLogger(WCServletPathForDefaultMappingDefault.class.getName());
    private static final String APP_NAME = "ServletPathDefaultMapping";

    @Server("servlet40_ServletPathForDefaultMapping_Default")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add ServletPathDefaultMapping.war to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "servletpathdefaultmapping.war.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCServletPathForDefaultMappingDefault.class.getSimpleName() + ".log");
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
     * Test to ensure that the default behavior when using the servlet-4.0 feature is
     * that a request to the default servlet will result in:
     *
     * servlet path = / + requestURI - context path
     * path info = null
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_Default() throws Exception {
        String expectedResponse = "ServletPath = / PathInfo = null";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME;

        testServletPathForDefaultMappingDefault(url, expectedResponse);
    }

    /**
     * Test to ensure that the default behavior when using the servlet-4.0 feature is
     * that a request to the default servlet will result in:
     *
     * servlet path = / + requestURI - context path
     * path info = null
     *
     * @throws Exception
     */
    @Test
    public void testServletPathForDefaultMapping_Default_2() throws Exception {
        String expectedResponse = "ServletPath = /index.html PathInfo = null";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/index.html";

        testServletPathForDefaultMappingDefault(url, expectedResponse);
    }

    private void testServletPathForDefaultMappingDefault(String url, String expectedResponse) throws Exception {
        LOG.info("url: " + url);
        LOG.info("expectedResponse: " + expectedResponse);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
            }
        }
    }
}
