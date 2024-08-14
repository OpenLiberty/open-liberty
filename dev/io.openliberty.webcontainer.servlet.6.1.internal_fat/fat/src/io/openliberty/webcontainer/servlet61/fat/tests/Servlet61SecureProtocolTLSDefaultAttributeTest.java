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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Logger;

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
import componenttest.topology.utils.HttpUtils;


/**
 * Test TLSv1.3 (default) set in request attribute "jakarta.servlet.request.secure_protocol"
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61SecureProtocolTLSDefaultAttributeTest {
    private static final Logger LOG = Logger.getLogger(Servlet61SecureProtocolTLSDefaultAttributeTest.class.getName());
    private static final String TEST_APP_NAME = "SecureProtocolRequestAttribute";

    private static final int CONN_TIMEOUT = 5;

    @Server("servlet61_SecureProtocolTLSv1_3AttributeTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        LOG.info("Setup : servlet61_SecureProtocolTLSv1_3AttributeTest.");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "secureprotocol.servlets");

        server.startServer(Servlet61SecureProtocolTLSDefaultAttributeTest.class.getSimpleName() + ".log");

        //MUST WAIT FOR SECURE PORT...else this test will fail.
        assertNotNull("SSL port is not ready", server.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl.*"));

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
     * Test send HTTPS request; application retrieves request.getAttribute("jakarta.servlet.request.secure_protocol")
     * and report back in the response's message
     */
    @Test
    public void test_RequestAttribute_SecureProtocol_TLSv1_3() throws Exception {
        LOG.info("====== <test_RequestAttribute_SecureProtocol_TLSv1_3> ======");

        HttpUtils.trustAllCertificates();
        String url = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() +  "/" + TEST_APP_NAME + "/TestSecureProtocolRequestAttribute";
        LOG.info("Sending [" + url + "]");

        URL testHttps = new URL(url);
        HttpURLConnection conn = HttpUtils.getHttpConnection(testHttps, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        try {

            BufferedReader reader = HttpUtils.getResponseBody(conn);
            String body = null;
            while ((body = reader.readLine()) != null) {
                LOG.info("\n" + body ); //there should be only one line response that contains the TLS

                assertTrue("The response did not contain [TLSv1.3] . Response [" + body + "]", body != null ? body.contains("TLSv1.3") : null);
            }

        } finally {
            conn.disconnect();
        }
    }
}
