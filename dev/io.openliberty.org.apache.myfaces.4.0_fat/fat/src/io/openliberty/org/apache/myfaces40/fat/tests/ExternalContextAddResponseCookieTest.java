/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test to ensure that we can add Cookies with a number of different attributes defined
 * using the ExternalContext addResponseCookie API.
 */
@RunWith(FATRunner.class)
public class ExternalContextAddResponseCookieTest {
    private static final Logger LOG = Logger.getLogger(ExternalContextAddResponseCookieTest.class.getName());
    private static final String APP_NAME = "ExternalContextAddResponseCookie";

    @Server("faces40_externalContextAddResponseCookieTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "io.openliberty.org.apache.myfaces40.fat.externalContext.addResponseCookie.bean");

        server.startServer(ExternalContextAddResponseCookieTest.class.getSimpleName() + ".log");
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
     * Test the ExternalContext addResponseCookie API. We should be able to add any attributes
     * we want to the Cookie being added including common known attributes like httpOnly which is
     * part of the Servlet Cookie API as well as sameSite which is a well known Cookie attribute
     * but is not part of the Servlet Cookie API. In addition ensure we can add any undefined
     * Cookie attribute and ensure a Set-Cookie header is returned to the client for each
     * scenario.
     *
     * @throws Exception
     */
    @Test
    public void testExternalContextAddResponseCookie() throws Exception {
        String expectedSameSiteCookieHeaderValue = "sameSiteCookieName=sameSiteCookieValue; SameSite=lax";
        String expectedHttpOnlyCookieHeaderValue = "httpOnlyCookieName=httpOnlyCookieValue; HttpOnly";
        String expectedUndefinedCookieHeaderValue = "undefinedCookieName=undefinedCookieValue; undefinedattributename=undefinedAttributeValue";
        boolean sameSiteCookieHeaderFound = false;
        boolean httpOnlyCookieHeaderFound = false;
        boolean undefinedCookieHeaderFound = false;

        URL url = HttpUtils.createURL(server, "/" + APP_NAME + "/ExternalContextAddResponseCookie.xhtml");

        try (WebClient webClient = new WebClient()) {
            // Drive a request to the page.
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the form.
            HtmlForm form = page.getFormByName("form");

            // Get the button and click it.
            HtmlSubmitInput addResponseCookiesButton = form.getInputByName("form:addResponseCookies");
            page = addResponseCookiesButton.click();

            // Get all Headers from the Response.
            List<NameValuePair> headers = page.getWebResponse().getResponseHeaders();

            // Look for each of the expected Set-Cookie Headers.
            for (NameValuePair header : headers) {
                LOG.info(header.getName() + ":" + header.getValue());
                if (header.getName().equals("Set-Cookie")) {
                    if (header.getValue().equals(expectedSameSiteCookieHeaderValue)) {
                        sameSiteCookieHeaderFound = true;
                    } else if (header.getValue().equals(expectedHttpOnlyCookieHeaderValue)) {
                        httpOnlyCookieHeaderFound = true;
                    } else if (header.getValue().equals(expectedUndefinedCookieHeaderValue)) {
                        undefinedCookieHeaderFound = true;
                    }
                }
            }

            // Test that all three Set-Cookie headers were in the response.
            assertTrue("The following Set-Cookie header value was not found: " +
                       expectedSameSiteCookieHeaderValue, sameSiteCookieHeaderFound);
            assertTrue("The following Set-Cookie header value was not found: " +
                       expectedHttpOnlyCookieHeaderValue, httpOnlyCookieHeaderFound);
            assertTrue("The following Set-Cookie header value was not found: " +
                       expectedUndefinedCookieHeaderValue, undefinedCookieHeaderFound);
        }
    }
}
