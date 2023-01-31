/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.transport.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Class to test the DoNotAllowDuplicateSetCookies property
 * with the <remoteIp> configuration in the server.xml
 */
@RunWith(FATRunner.class)
public class HttpSetCookieTests {

    private static final Class<?> ME = HttpSetCookieTests.class;
    public static final String APP_NAME = "EndpointInformation";
    @Server("FATServer")
    public static LibertyServer server;

    // Since we have tracing enabled give server longer timeout to start up.
    private static final long SERVER_START_TIMEOUT = 30 * 1000;

    // Timeout used for searching a string in log files
    private static final long SERVER_LOG_SEARCH_TIMEOUT = 5 * 1000;

    private final int httpDefaultPort = server.getHttpDefaultPort();
    private HttpClient client;

    @BeforeClass
    public static void setupOnlyOnce() throws Exception {
        // Create a WebArchive that will have the file name 'EndpointInformation.war' once it's written to a file
        // Include the 'com.ibm.ws.transport.http.servlets' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transport.http.servlets");
    }

    @Before
    public void setup() throws Exception {
        RequestConfig requestConfig = RequestConfig.custom().setLocalAddress(InetAddress.getByName("127.0.0.1")).build();
        client = HttpClientBuilder.create().setRetryHandler(new DefaultHttpRequestRetryHandler()).setDefaultRequestConfig(requestConfig).build();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer();
    }

    /**
     * Test a config where the property DoNotAllowDuplicateSetCookies=true is set
     *
     * Verify that set-cookie headers are not duplicated in the response.
     *
     * @throws Exception
     */
    @Test
    public void testDoNotAllowDuplicateSetCookiesTrue() throws Exception {
        String variation = "DoNotAllowDuplicateSetCookies";
        String testName = "testDoNotAllowDuplicateSetCookiesTrue";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        HttpResponse httpResponse = execute(APP_NAME, servletName, testName);
        org.apache.http.Header[] headers = httpResponse.getHeaders("Set-Cookie");

        // check to make sure only one set-cookie header is present and it's the last one
        org.junit.Assert.assertEquals(1, headers.length);
        org.junit.Assert.assertEquals("JSESSIONID=gorp2", headers[0].getValue());

    }

    /**
     * Test a config where the property DoNotAllowDuplicateSetCookies=false is set
     *
     * Verify that set-cookie headers are duplicated in the response.
     *
     * @throws Exception
     */
    @Test
    public void testDoNotAllowDuplicateSetCookiesFalse() throws Exception {
        String variation = "AllowDuplicateSetCookies";
        String testName = "testDoNotAllowDuplicateSetCookiesFalse";
        String servletName = "EndpointInformationServlet";
        startServer(variation);

        HttpResponse httpResponse = execute(APP_NAME, servletName, testName);
        org.apache.http.Header[] headers = httpResponse.getHeaders("Set-Cookie");

        //Check to make sure two set-cookie headers for jesssionid are present
        org.junit.Assert.assertEquals(2, headers.length);
        org.junit.Assert.assertEquals("JSESSIONID=gorp1", headers[0].getValue());
        org.junit.Assert.assertEquals("JSESSIONID=gorp2", headers[1].getValue());

    }

    /**
     * Private method to start a server.
     *
     * Please look at publish/files/remoteIPConfig directory for the different server names.
     *
     * @param variation The name of the server that needs to be appended to "-server.xml"
     * @throws Exception
     */
    private void startServer(String variation) throws Exception {
        server.setServerConfigurationFile("remoteIPConfig/" + variation + "-server.xml");
        server.setServerStartTimeout(SERVER_START_TIMEOUT);
        server.startServer(variation + ".log");
        server.waitForStringInLogUsingMark("CWWKT0016I:.*EndpointInformation.*");
    }

    /**
     * Private method to execute/drive an HTTP request and obtain an HTTP response
     *
     * @param app        The app name or context root
     * @param path       The specific path to drive the request
     * @param headerList A list of headers to be added in the request
     * @param variation  The name of the server for logging purposes
     * @return The HTTP response for the request
     * @throws Exception
     */
    private HttpResponse execute(String app, String path, String variation) throws Exception {

        String urlString = "http://" + server.getHostname() + ":" + httpDefaultPort + "/" + app + "/" + path;
        URI uri = URI.create(urlString);
        Log.info(ME, variation, "Execute request to " + uri);

        HttpGet request = new HttpGet(uri);

        HttpResponse response = client.execute(request);
        Log.info(ME, variation, "Returned: " + response.getStatusLine());
        return response;
    }

    /**
     * Get the HTTP response as a String
     *
     * @param response
     * @return A String that contains the response
     * @throws IOException
     */
    private String getResponseAsString(HttpResponse response) throws IOException {

        final HttpEntity entity = response.getEntity();

        org.junit.Assert.assertNotNull("No response found", entity);

        return EntityUtils.toString(entity);
    }
}
