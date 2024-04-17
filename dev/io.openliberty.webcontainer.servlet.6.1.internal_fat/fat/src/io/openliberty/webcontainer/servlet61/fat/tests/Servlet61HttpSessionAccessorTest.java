/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 * Test HttpSession.Accessor APIs: getAccessor, accept
 *
 * Application's servlet will do all the tests and report back PASS/FAIL response.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61HttpSessionAccessorTest {
    private static final Logger LOG = Logger.getLogger(Servlet61HttpSessionAccessorTest.class.getName());
    private static final String TEST_APP_NAME = "HttpSessionAccessor";

    @Server("servlet61_HttpSessionAccessor")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_HttpSessionAccessor.");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "accessor.servlets");

        server.startServer(Servlet61HttpSessionAccessorTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Test update the session lastAccessedTime via HttpSession.Accessor
     */
    @Test
    public void test_SessionAccessor_LastAccessedTime() throws Exception {
        runTest("testSessionAccessorLastAccessedTime");
    }

    /*
     * Test access the invalidated session.
     */
    @Test
    public void test_SessionAccessor_InvalidSession() throws Exception {
        runTest("testSessionAccessorInvalidSession");
    }

    /*
     * Test access the session which is invalid session ID
     */
    @Test
    public void test_SessionAccessor_InvalidSessionId() throws Exception {
        runTest("testSessionAccessorInvalidSessionId");
    }

    /**
     * @param testToRun - name of the test/method for the servlet to execute
     */
    private void runTest(String testToRun) throws Exception {
        LOG.info("====== <runTest> [" + testToRun + "] ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestHttpSessionAccessor";
        HttpGet httpMethod = new HttpGet(url);
        httpMethod.addHeader("runTest", testToRun);

        LOG.info("Sending [" + url + "]");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(httpMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                assertTrue("Expecting PASS response but found [" + responseText + "]", responseText.contains("PASS"));
            }
        }
    }
}
