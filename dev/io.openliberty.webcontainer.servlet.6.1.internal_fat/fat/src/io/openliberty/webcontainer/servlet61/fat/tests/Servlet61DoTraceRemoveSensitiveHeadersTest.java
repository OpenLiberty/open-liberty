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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpTrace;
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
 * Test remove of sensitive headers from the response for the doTrace() requests
 *
 * Authorization, Cookie, X-Forwarded-For, Forwarded, Proxy-Authorization
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61DoTraceRemoveSensitiveHeadersTest {
    private static final Logger LOG = Logger.getLogger(Servlet61DoTraceRemoveSensitiveHeadersTest.class.getName());
    private static final String TEST_APP_NAME = "DoTraceRemoveSensitiveHeaders";

    private final ArrayList<String> sensitiveHeaders = new ArrayList<String>( Arrays.asList("Authorization", "Cookie", "X-Forwarded-For", "Forwarded", "Proxy-Authorization"));
    private final ArrayList<String> normalHeaders = new ArrayList<String>( Arrays.asList("Auth0rization", "C0okie", "X-F0rwarded-For", "F0rwarded", "Proxy-Auth0rization"));

    @Server("servlet61_DoTraceRemoveHeadersTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_DoTraceRemoveHeadersTest");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "removeheaders.servlets");
        server.startServer(Servlet61DoTraceRemoveSensitiveHeadersTest.class.getSimpleName() + ".log");
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

    @Test
    public void test_doTrace_RemoveSensitiveHeaders() throws Exception {
        LOG.info("====== <test_Request_doTraceRemoveSensitiveHeaders> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestRemoveHeaders";
        HttpTrace httpMethod = new HttpTrace(url);

        //Sensitive headers which are not expected from the doTrace response
        httpMethod.addHeader("Authorization", "Basic base64");
        httpMethod.addHeader("Cookie", "cookietest=cookieValue");
        httpMethod.addHeader("X-Forwarded-For", "192.168.1.1");
        httpMethod.addHeader("Forwarded", "for=192.168.1.1");
        httpMethod.addHeader("Proxy-Authorization:", "Basic base64");


        //Similar to sensitive headers but with a digit 0, instead of o, to make them normal headers
        //doTrace will include them in the response.
        httpMethod.addHeader("Auth0rization", "Basic base64");
        httpMethod.addHeader("C0okie", "cookietest=cookieValue");
        httpMethod.addHeader("X-F0rwarded-For", "192.168.1.1");
        httpMethod.addHeader("F0rwarded", "for=192.168.1.1");
        httpMethod.addHeader("Proxy-Auth0rization:", "Basic base64");


        LOG.info("Sending [" + url + "]");
        int counter = 0;

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(httpMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                Iterator<String> iter = normalHeaders.iterator();
                String header = null;

                LOG.info("Checking normal headers in the doTrace response...");
                while (iter.hasNext()) {
                    LOG.info (header = iter.next());
                    if (responseText.contains(header)) {
                        ++counter;
                    }
                }
                if (counter != 5) {
                    fail("Expected 5 normal headers , but found [" + counter+ "]");
                }

               LOG.info("Checking sensitive headers in the doTrace response...");
               iter = sensitiveHeaders.iterator();
               while (iter.hasNext()) {
                   LOG.info (header = iter.next());
                   if (responseText.contains(header)) {
                       fail("Not expected any sensitive headers [Authorization, Cookie, X-Forwarded, Forwarded, Proxy-Authorization] from doTrace response, but found [" + header + "]");
                   }
               }
            }
        }
    }
}
