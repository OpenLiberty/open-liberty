/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.webcontainer.servlet60.fat.tests;

import static org.junit.Assert.assertTrue;

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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test Cookie.setAttribute
 *
 * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/http/cookie#setAttribute(java.lang.String,java.lang.String)
 *
 * Any cookie attribute can be sent with a response. Request does not contain attribute.
 * Also test setAttribute for SameSite and its precedence over the server setting
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet60CookieSetAttributeTest {

    private static final Logger LOG = Logger.getLogger(Servlet60CookieSetAttributeTest.class.getName());
    private static final String TEST_APP_NAME = "CookieSetAttributeTest";

    //All tests have this response.  Main data to verify are in the Set-Cookie headers.
    private static final String expectedGeneralResponse = "Hello from the CookieSetAttributeServlet";

    @Server("servlet60_cookieSetAttributeTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "cookiesetattribute.servlets");

        server.startServer(Servlet60CookieSetAttributeTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test basic predefined attribute setters (setPath, setDomain ...) and new setAttribute
     *
     */
    @Test
    public void test_cookiePredefinedSetterAndSetAttribute() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/CookieSetAttributeServlet?testName=basic";
        String expectedSetCookie = "CookieSetAttributeServlet=TestCookieBasic; Path=BasicPath; Domain=basicdomain; Secure; HttpOnly; SameSite=Lax; basicsetattribute1=BasicAttributeValue1; basicsetattribute2=BasicAttributeValue2";
        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");

                //fail-fast check
                assertTrue("The response did not contain the following String: " + expectedGeneralResponse, responseText.contains(expectedGeneralResponse));

                Header[] headers = response.getHeaders();
                for (Header header : headers) {
                    LOG.info("\n" + "Header: " + header);
                }
                String headerValue = response.getHeader("Set-Cookie").getValue();
                LOG.info("\n Set-Cookie value [" + headerValue + "]");
                assertTrue("The response did not contain the following Set-Cookie header " + expectedSetCookie, headerValue.equals(expectedSetCookie));
            }
        }
    }

    /*
     * Test overriding predefined setter with the setAttribute:
     * 1. setPath, setDomain on a cookie
     * 2. Override Path and Domain with setAttribute("Path",value) and setAttribute("Domain", value)
     * 3. Also test overriding the previous setAttribute()
     */
    @Test
    public void test_cookieOverrideSetterWithSetAttribute() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/CookieSetAttributeServlet?testName=override";
        String expectedSetCookie = "CookieSetAttributeServlet=TestCookieOverride; Path=Path_viaSetAttribute; Domain=Domain_viaSetAttribute; HttpOnly; SameSite=Lax; basicsetattribute1=BasicAttributeValue1; basicsetattribute2=BasicAttributeValue2; basicsetattribute3=BasicAttributeValueREPLACED";
        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");

                //fail-fast check
                assertTrue("The response did not contain the following String: " + expectedGeneralResponse, responseText.contains(expectedGeneralResponse));

                Header[] headers = response.getHeaders();
                for (Header header : headers) {
                    LOG.info("\n" + "Header: " + header);
                }

                String headerValue = response.getHeader("Set-Cookie").getValue();

                LOG.info("\n Set-Cookie value [" + headerValue + "]");

                assertTrue("The response did not contain the following Set-Cookie header " + expectedSetCookie, headerValue.equals(expectedSetCookie));
            }
        }
    }

    /*
     * Test Set-Cookie via addHeader and setHeader behavior do not change in Servlet 6.0..i.e it generates 2 Set-Cookie headers
     */
    @Test
    public void test_cookieAddAndSetHeader() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/CookieSetAttributeServlet?testName=addAndSetHeaders";
        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");

                //fail-fast check
                assertTrue("The response did not contain the following String: " + expectedGeneralResponse, responseText.contains(expectedGeneralResponse));

                Header[] headers = response.getHeaders();
                String headerValue;
                int hitFound = 0;

                for (Header header : headers) {
                    LOG.info("\n" + "Header: " + header);
                    if (header.getName().equals("Set-Cookie")) {
                        headerValue = header.getValue();
                        LOG.info("\n" + "Set-Cookie value: " + headerValue);
                        if (headerValue.equals("randomAttributeA=myAttValueA; SameSite=Lax") || headerValue.equals("randomAttributeB=myAttValueB; SameSite=Lax")) {
                            hitFound++;
                        }
                    }
                }

                assertTrue("Expecting 2 Set-Cookie headers for random attribute , but found [" + hitFound + "]", hitFound == 2);
            }
        }
    }

    /*
     * Test cookie setAttribute("SameSite","Strict") that overrides the server setting of <samesite lax="*"/>
     */
    @Test
    public void test_setAttributeSameSitePrecedence() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/CookieSetAttributeServlet?testName=setAttributeSameSite";
        String expectedSetCookie = "CookieSetAttributeServlet=TestSetAttributeSameSite; HttpOnly; SameSite=Strict";

        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");

                //fail-fast check
                assertTrue("The response did not contain the following String: " + expectedGeneralResponse, responseText.contains(expectedGeneralResponse));

                Header[] headers = response.getHeaders();
                for (Header header : headers) {
                    LOG.info("\n" + "Header: " + header);
                }
                String headerValue = response.getHeader("Set-Cookie").getValue();
                LOG.info("\n Set-Cookie value [" + headerValue + "]");
                assertTrue("The response did not contain the following Set-Cookie header " + expectedSetCookie, headerValue.equals(expectedSetCookie));
            }
        }
    }
}
