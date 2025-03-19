/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

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
import com.ibm.websphere.simplicity.config.HttpSession;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for Incompatible SameSite=None Clients
 * Incompatible clients are based on this https://www.chromium.org/updates/same-site/incompatible-clients/
 */
@RunWith(FATRunner.class)
public class WCSameSiteIncompatibleClientsTests {
    private static final Logger LOG = Logger.getLogger(WCSameSiteIncompatibleClientsTests.class.getName());
    private static final String APP_NAME = "IncompatibleClientTest";
    private enum Compatability {
        COMPATIBLE,
        INCOMPATIBLE,
        ERROR
    };

    @Server("servlet40_incompatible")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        // Create the IncompatibleClientTest.war application
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "incompatible.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCSameSiteIncompatibleClientsTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server

        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Helper method to be run for all versions since the implementation is userAgent dependent
     * Drives a request with the provided userAgent and returns true if the SameSite cookie is part of the response
     */
    private Compatability isCompatibleVersion(String userAgent) throws Exception{
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/IncompatibleClientServlet";

        LOG.info("Navigating to url: " + url);

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        byte correctCookieCount = 0;
        String[] sameSiteNoneCookieNames = {"AddCookieCookie","AddHeaderCookie", "SetHeaderCookie", "JSESSIONID"};
        HttpGet getMethod = new HttpGet(url);
        getMethod.setHeader("User-Agent", userAgent);
        Header[] headers = null;

        try {
            client = HttpClientBuilder.create().build();
            response = client.execute(getMethod);
            String responseText = EntityUtils.toString(response.getEntity());

            LOG.info("Response Text:" + responseText);

                            // Check whether the SameSite=None cookie is received
                            for (Header cookieHeader : response.getHeaders("Set-Cookie")) {
                                String cookieHeaderValue = cookieHeader.getValue();
                                LOG.info("Header Name: " + cookieHeader.getName());
                                LOG.info("Header Value: " + cookieHeaderValue);
                                
                                for (String cookieName : sameSiteNoneCookieNames) {
                                    if (cookieHeaderValue.startsWith(cookieName)) {
                                        if (cookieHeaderValue.contains("SameSite=None")) {
                                            correctCookieCount++;
                                        }
                                    }
                                }
                                
                                if (cookieHeaderValue.startsWith("BasicCookie")) {
                                    correctCookieCount++;
                                }
                            }
        } finally {
            if (client != null)
                client.close();
            if (response != null)
                response.close();
        }

        // The 1 is for the BasicCookie
        if (correctCookieCount == sameSiteNoneCookieNames.length + 1){
            return Compatability.COMPATIBLE;
        } else if (correctCookieCount == 1) {
            return Compatability.INCOMPATIBLE;
        } else {
            return Compatability.ERROR;
        }
    }

    /*
     * Confirm that compatible versions after iOS 12 work
     */
    @Test
    public void testiOSAfterFix() throws Exception {
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/66.6 Mobile/14A5297c Safari/602.1";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent) == Compatability.COMPATIBLE);
    }

    /*
     * Confirm that compatible versions after MacOS 10.14 work on Safari
     */
    @Test
    public void testMacOSSafariAfterFix() throws Exception {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/601.1.39 (KHTML, like Gecko) Version/10.1.2 Safari/601.1.39";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

    /*
     * Confirm that compatible versions after MacOS 10.14 work on embedded browsers
     */
    @Test
    public void testMacOSEmbeddedAfterFix() throws Exception {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/537.36 (KHTML, like Gecko)";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

    /*
     * Confirm that compatible versions after UCBrowser 12.13.4 work
     */
    @Test
    public void testUCBrowserAfterFix() throws Exception {
        String userAgent = "Mozilla/5.0 (Linux; U; Android 8.0.0; en-US; Pixel XL Build/OPR3.170623.007) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/12.13.4.1005 U3/0.8.0 Mobile Safari/534.30";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

    /*
     * Confirm that compatible versions after Chrome 66 work
     */
    @Test
    public void testChromeAfterFix() throws Exception {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.2526.73 Safari/537.36";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

    /*
     * Confirm that the incompatible border version iOS 12 doesn't work
     */
    @Test
    public void testIncompatibleiOS() throws Exception {
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/ 604.1.21 (KHTML, like Gecko) Version/ 12.0 Mobile/17A6278a Safari/602.1.26";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was received when it shouldn't have been for the incompatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.INCOMPATIBLE);
    }

    /*
     * Confirm that the incompatible border version MacOS 10.14 with embedded browsers doesn't work
     */
    @Test
    public void testIncompatibleMacOSEmbedded() throws Exception {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14) AppleWebKit/537.36 (KHTML, like Gecko)";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was received when it shouldn't have been for the incompatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.INCOMPATIBLE);
    }

    /*
     * Confirm that the incompatible border version MacOS 10.14 with Safari doesn't work
     */
    @Test
    public void testIncompatibleMacOSSafari() throws Exception {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Safari/605.1.15";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was received when it shouldn't have been for the incompatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.INCOMPATIBLE);
    }

    /*
     * Confirm that the compatible border version UC Browser 12.13.2 works
     */
    @Test
    public void testCompatibleUCBrowser() throws Exception {
        String userAgent = "Mozilla/5.0 (Linux; U; Android 8.0.0; en-US; Pixel XL Build/OPR3.170623.007) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/12.13.2.1005 U3/0.8.0 Mobile Safari/534.30";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

    /*
     * Confirm that the incompatible border versions Chrome 51 and 66 don't work
     */
    @Test
    public void testIncompatibleChrome() throws Exception {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3334.0 Safari/537.36";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was received when it shouldn't have been for the incompatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.INCOMPATIBLE);
        userAgent = "Mozilla/5.0 doogiePIM/1.0.4.2 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was received when it shouldn't have been for the incompatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.INCOMPATIBLE);
    }

    /*
     * Technically, versions prior to iOS 12 are also most likely incompatible, but there have been no issues reported for treating them as compatible.
     * Confirm that the versions prior to iOS 12 are compatible
     */
    @Test
    public void testiOSBeforeFix() throws Exception {
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/66.6 Mobile/14A5297c Safari/602.1";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

    /*
     * Technically, versions prior to MacOS 10.14 are also most likely incompatible, but there have been no issues reported for treating them as compatible.
     * Confirm that the versions prior to MacOS 10.14 with embedded browsers are compatible
     */
    @Test
    public void testMacOSEmbeddedBeforeFix() throws Exception {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13) AppleWebKit/537.36 (KHTML, like Gecko)";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

    /*
     * Technically, versions prior to MacOS 10.14 are also most likely incompatible, but there have been no issues reported for treating them as compatible.
     * Confirm that the versions prior to MacOS 10.14 with Safari are compatible
     */
    @Test
    public void testMacOSSafariBeforeFix() throws Exception {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Safari/605.1.15";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

    /*
     * Confirm that the versions prior to UCBrowser 12.13.2 are incompatible
     */
    @Test
    public void testUCBrowserBeforeFix() throws Exception {
        String userAgent = "Mozilla/5.0 (Linux; U; Android 7.1.1; en-US; Lenovo K8 Note Build/NMB26.54-74) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/12.0.0.1088 Mobile Safari/537.36";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was received when it shouldn't have been for the incompatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.INCOMPATIBLE);
    }

    /*
     * Prior to Chrome 51, SameSite is ignored, therefore these user-agents are not strictly incompatible.
     * Confirm that the versions prior to Chrome 51 are compatible
     */
    @Test
    public void testChromeBeforeFix() throws Exception {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";
        assertTrue("Either the BasicCookie was not received or the SameSite=None cookie was not received when it should have been for the compatible client:" + userAgent, isCompatibleVersion(userAgent)==Compatability.COMPATIBLE);
    }

}
