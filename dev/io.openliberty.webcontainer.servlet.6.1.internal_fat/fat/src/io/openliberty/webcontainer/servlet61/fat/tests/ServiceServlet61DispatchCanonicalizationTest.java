/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet61.fat.tests;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test canonicalization path during dispatch
 * Use the request header "runTest" to specify a test to run
 *
 * The rules from Servlet 6.0, 3.5.2 are applied to all path parameter but NOT query string
 *
 * Test:
 *      1. Dispatch with (path + encoded_QueryString) result in 200
 *      2. Dispatch with encoded_path result in 500
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ServiceServlet61DispatchCanonicalizationTest {

    private static final Logger LOG = Logger.getLogger(ServiceServlet61DispatchCanonicalizationTest.class.getName());
    private static final String TEST_APP_NAME = "DispatchCanonicalizationTest";

    @Server("servlet61_DispatchCanonicalizationTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_DispatchCanonicalizationTest");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "servlets.async");
        server.startServer(ServiceServlet61DispatchCanonicalizationTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        if (server != null && server.isStarted()) {
            
            //Expecting: SRVE8046E: An error occurred while invoking a call to AsyncContext dispatch.
            server.stopServer("SRVE8046E");
        }
    }

    /**
     * Servlet async dispatch with a not_allow encoded %2F (forward slash) in query string which is not subjected to the verification.
     * @throws Exception
     */
//    @Test
    public void test_dispatchEncodedQueryString() throws Exception {
        LOG.info("====== <test_dispatchEncodedQueryString> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/AsyncDispatch/test_dispatchEncodedQueryString";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "test_dispatchEncodedQueryString");

        LOG.info("Sending ["+ url + "]");
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Result does not contain PASS_1 and PASS_2 checks", responseText.contains("PASS_1") && responseText.contains("PASS_2"));
            }
        }
    }
    
    /**
     * Servlet async dispatch with a not_allow encoded %2F (forward slash) in the path which is subjected to the verification and rejected
     * 
     * Response reports the exception message - SRVE8046E: An error occurred while invoking a call to AsyncContext dispatch.
     * @throws Exception
     */
    @Test
    public void test_dispatchEncodedPath() throws Exception {
        LOG.info("====== <test_dispatchEncodedPath> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/AsyncDispatch/test_dispatchEncodedPath";
        HttpGet getMethod = new HttpGet(url);

        getMethod.addHeader("runTest", "test_dispatchEncodedPath");

        LOG.info("Sending ["+ url + "]");
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Code : [" + response.getCode() + "]");
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("SRVE8046E is not found", responseText.contains("SRVE8046E"));
                
                //make sure it does not inadvertently dispatch to the next resource.
                assertTrue("PASS_2 is not expected", !responseText.contains("PASS_2"));
            }
        }
    }
}
