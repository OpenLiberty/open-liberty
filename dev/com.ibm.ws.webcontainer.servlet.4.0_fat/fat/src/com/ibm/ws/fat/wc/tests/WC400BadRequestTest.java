/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/*
 * Testing WC property
 *      com.ibm.ws.webcontainer.set400SCOnTooManyParentDirs="true" (default is false)
 *
 * return a 400 instead of 500 for Non-valid URI (detected by WSUtils)
 *
 * NOTE: in servlet-6.0+, this property is not needed.
 * These type of requests are rejected during the canonicalizeURI() process. java.lang.IllegalArgumentException is also not thrown.
 *
 * For this reason, skip repeat on EE10_FEATURES; otherwise, it will get a failure in EE10 with the message:
 * [An FFDC reporting java.lang.IllegalArgumentException was expected but none was found.]
 */

@RunWith(FATRunner.class)
@SkipForRepeat(EE10_OR_LATER_FEATURES)
@Mode(TestMode.FULL)
public class WC400BadRequestTest {

    @Server("servlet40_400ResponseServer")
    public static LibertyServer server;

    private static final Logger LOG = Logger.getLogger(WC400BadRequestTest.class.getName());
    private static final String APP_NAME = "Test400BadRequestURI";

    @BeforeClass
    public static void setupClass() throws Exception {
        LOG.info("Setup : " + APP_NAME);
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "test400BadRequest.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WC400BadRequestTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (server != null && server.isStarted()) {
            //Expecting - com.ibm.ws.webcontainer.webapp E SRVE0315E: An exception occurred: java.lang.Throwable: java.lang.IllegalArgumentException: Non-valid URI.
            server.stopServer("SRVE0315E");
        }
    }

    /*
     * Normal request to test servlet to make sure it works.
     */
    @Test
    public void testGoodRequestURI() throws Exception {
        String expectedResponse = "Hello from TestServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/Test400BadRequest";

        LOG.info("\n #################### [testGoodRequestURI]: BEGIN ####################");

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            LOG.info("\nSending Request: [" + url + "]");

            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                LOG.info("\nResponse code: " + response.getCode());

                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity).trim();

                //fully consumed the underlying so the connection can be safely re-used
                EntityUtils.consume(entity);
                LOG.info("\nResponse content: " + content);

                assertTrue("Response did not contain expected response: ", content.contains(expectedResponse));
            }
        }

        LOG.info("\n #################### [testGoodRequestURI] FINISH #################### ");
    }

    /*
     * Bad request contains path traverse /context/../../../
     *
     * >Exception = java.lang.IllegalArgumentException
     * >Source = com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters
     * >probeid = 1105
     * >Stack Dump = java.lang.IllegalArgumentException: Non-valid URI.
     * > at com.ibm.ws.util.WSUtil.resolveURI(WSUtil.java:135)
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testBadRequestURI() throws Exception {
        int expectedResponse = 400;
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/../../../../Test400BadRequest";
        int responseCode;
        LOG.info("\n #################### [testBadRequestURI]: BEGIN ####################");

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            LOG.info("\nSending Request: [" + url + "]");

            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                responseCode = response.getCode();
                LOG.info("\nResponse code: " + responseCode);

                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity).trim();

                //fully consumed the underlying so the connection can be safely re-used
                EntityUtils.consume(entity);
                LOG.info("\nResponse content: " + content);

                assertTrue("Response did not contain expected status code: ", responseCode == expectedResponse);
            }
        }

        LOG.info("\n #################### [testBadRequestURI] FINISH #################### ");
    }
}
