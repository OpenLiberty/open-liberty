/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet60.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for CHIPS (Partitioned Cookies)
 * - Tests behavior with Servlet 6.0's Cookie#setAttribute Method and Server.xml config
 */
@RunWith(FATRunner.class)
public class Servlet60PartitionedAttributeTests {

    private static final Logger LOG = Logger.getLogger(Servlet60PartitionedAttributeTests.class.getName());
    private static final String APP_NAME = "PartitionedServlet60Test";

    @Server("servlet60_partitioned")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        // Create the PartitionedServlet60Test.war application
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "partitioned.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(Servlet60PartitionedAttributeTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * No SameSite MetaType enabled in the server.xml. Only Cookie#setAttribute via Servlet
     * What is set manually in the servlet is what should be sent untouched.
     */
    @Test
    public void testPartitionedCookieSetAttributeDefaultNoConfig() throws Exception {
        String expectedPlainCookie = "Set-Cookie: Cookie_Plain_Name=AddCookie_Plain_Value";
        String expectedSameSiteNoneOnlyCookie = "Set-Cookie: AddCookie_SameSiteNoneOnly_Name=AddCookie_SameSiteNoneOnly_Value; SameSite=None";
        String expectedSameSiteNoneCookie = "Set-Cookie: AddCookie_SameSiteNone_Name=AddCookie_SameSiteNone_Value; SameSite=None; Partitioned";
        String expectedSameSiteLaxCookie = "Set-Cookie: AddCookie_SameSiteLax_Name=AddCookie_SameSiteLax_Value; SameSite=Lax";

        String expectedResponse = "Welcome to the TestSetAttributePartitionedCookieServlet!";
        boolean plainCookieFound = false;
        boolean partitionedOnlyFound = false;
        boolean partitionedSameSiteNoneFound = false;
        boolean partitionedSameSiteLaxFound = false;

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestSetAttributePartitionedCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie headers were found by the client.
                int cookieCount = 0;
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.contains("Set-Cookie:")) {
                        cookieCount++;
                    }
                    if (headerValue.equals(expectedPlainCookie)) {
                        plainCookieFound = true;
                    } else if (headerValue.equals(expectedSameSiteNoneOnlyCookie)) {
                        partitionedOnlyFound = true;
                    } else if (headerValue.equals(expectedSameSiteNoneCookie)) {
                        partitionedSameSiteNoneFound = true;
                    } else if (headerValue.equals(expectedSameSiteLaxCookie)) {
                        partitionedSameSiteLaxFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedPlainCookie, plainCookieFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSameSiteNoneOnlyCookie, partitionedOnlyFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSameSiteNoneCookie, partitionedSameSiteNoneFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSameSiteLaxCookie, partitionedSameSiteLaxFound);
                assertTrue("The response did not contain the expected number of cookie headers", cookieCount == 4);
            }
        }
    }


    /**
     * Verifies Partitioned is added to cookies which have SameSite=None added programmatically. 
     * 
     * Configuration Tested:
     *  <samesite partitioned="true"/>
     */
    @Test
    public void testSetAttributeWithPartitionedConfig() throws Exception {

        // Partitioned added via config
        // (Techically incorrect since Secure is also required here)
        String expectedSameSiteNoneOnlyCookie = "Set-Cookie: AddCookie_SameSiteNoneOnly_Name=AddCookie_SameSiteNoneOnly_Value; SameSite=None; Partitioned";
        // Untouched via Config
        String expectedPlainCookie = "Set-Cookie: Cookie_Plain_Name=AddCookie_Plain_Value";
        String expectedSameSiteNoneCookie = "Set-Cookie: AddCookie_SameSiteNone_Name=AddCookie_SameSiteNone_Value; SameSite=None; Partitioned";
        String expectedSameSiteLaxCookie = "Set-Cookie: AddCookie_SameSiteLax_Name=AddCookie_SameSiteLax_Value; SameSite=Lax";

        String expectedResponse = "Welcome to the TestSetAttributePartitionedCookieServlet!";
        boolean plainCookieFound = false;
        boolean partitionedOnlyFound = false;
        boolean partitionedSameSiteNoneFound = false;
        boolean partitionedSameSiteLaxFound = false;

        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getSameSite().setStrict(null);
        httpEndpoint.getSameSite().setPartitioned(true);
        httpEndpoint.getSameSite().setNone(null);
        httpEndpoint.getSameSite().setLax(null);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedServlet60Test.*");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/TestSetAttributePartitionedCookie";
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                Header[] headers = response.getHeaders("Set-Cookie");
                LOG.info("\n" + "Set-Cookie headers contained in the response:");

                // Verify that the expected Set-Cookie headers were found by the client.
                int cookieCount = 0;
                String headerValue;
                for (Header header : headers) {
                    headerValue = header.toString();
                    LOG.info("\n" + headerValue);
                    if (headerValue.contains("Set-Cookie:")) {
                        cookieCount++;
                    }
                    if (headerValue.equals(expectedPlainCookie)) {
                        plainCookieFound = true;
                    } else if (headerValue.equals(expectedSameSiteNoneOnlyCookie)) {
                        partitionedOnlyFound = true;
                    } else if (headerValue.equals(expectedSameSiteNoneCookie)) {
                        partitionedSameSiteNoneFound = true;
                    } else if (headerValue.equals(expectedSameSiteLaxCookie)) {
                        partitionedSameSiteLaxFound = true;
                    }
                }

                assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedPlainCookie, plainCookieFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSameSiteNoneOnlyCookie, partitionedOnlyFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSameSiteNoneCookie, partitionedSameSiteNoneFound);
                assertTrue("The response did not contain the expected Set-Cookie header: " + expectedSameSiteLaxCookie, partitionedSameSiteLaxFound);
                assertTrue("The response did not contain the expected number of cookie headers", cookieCount == 4);
            }
        } finally {  
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*PartitionedServlet60Test.*");
        }

    }

}
