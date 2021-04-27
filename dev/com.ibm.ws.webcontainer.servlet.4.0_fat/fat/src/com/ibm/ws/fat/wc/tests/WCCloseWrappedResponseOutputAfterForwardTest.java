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

import static org.junit.Assert.assertFalse;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Purpose: verify that the wrapped response's output stream is closed before exiting a dispatch forward.
 *
 * Flow:
 *
 * filter will wrap the response.
 * application index*.jsp will dispatch forward to a displayPage.jsp.
 * Upon return from forward, index*.jsp attempts to write out below string which should NOT be included in the response.
 *
 * "FAIL_CLOSE_RESPONSE_OUTPUT_AFTER_FORWARD This text is after dispatch forward. It should not be seen in the response."
 *
 * Tests PASS if the above string is NOT in the response. Otherwise, FAIL.
 *
 * @See https://github.com/OpenLiberty/open-liberty/issues/16053
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCCloseWrappedResponseOutputAfterForwardTest {

    private static final Logger LOG = Logger.getLogger(WCCloseWrappedResponseOutputAfterForwardTest.class.getName());

    private static final String APP_NAME = "CloseWrappedResponseOutputForward";

    @Server("servlet40_closeWrappedOutputStreamAfterForward")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        LOG.info("Setup : add " + APP_NAME + " application to the server if not already present.");
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "filterandwrapper.war.files");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCCloseWrappedResponseOutputAfterForwardTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        LOG.info("testCleanUp : stop server");
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test the non wrap response.
     * PASS if response does not contain string "FAIL_CLOSE_RESPONSE_OUTPUT_AFTER_FORWARD".
     *
     * @throws Exception
     */
    @Test
    public void test_nonWrapResponse_closeOutputStreamAfterForward() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/indexNoWrap.jsp";
        LOG.info("url: " + url);
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);
                assertFalse("The response output was NOT closed after dispatch forward", responseText.contains("FAIL_CLOSE_RESPONSE_OUTPUT_AFTER_FORWARD"));
            }
        }
    }

    /**
     * Test the wrap response.
     * PASS if response does not contain string "FAIL_CLOSE_RESPONSE_OUTPUT_AFTER_FORWARD".
     *
     * @throws Exception
     */
    @Test
    public void test_wrapResponse_closeOutputStreamAfterForward() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/wrapped/indexWrapped.jsp";
        LOG.info("url: " + url);
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);
                assertFalse("The response output was NOT closed after dispatch forward", responseText.contains("FAIL_CLOSE_RESPONSE_OUTPUT_AFTER_FORWARD"));
            }
        }
    }
}
