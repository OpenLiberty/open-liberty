/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test Servlet 5 native jakarta.servlet (i.e not transformer)
 * Should only run in EE9.
 * The target app is servlet 5.0 version with new namespace.
 * Request to the simple snoop servlet 50 version which test several jakarta.servlet API (along with the new web-app web.xml namespace)
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
public class WC5JakartaServletTest {

    private static final Logger LOG = Logger.getLogger(WC5JakartaServletTest.class.getName());
    private static final String APP_NAME = "Servlet5TestApp";

    @Server("servlet50_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void before() throws Exception {
        LOG.info("Setup : add " + APP_NAME + " war to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "servlet5snoop.servlets");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WC5JakartaServletTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        // Stop the server
        if (server != null && server.isStarted()) {
            /*
             * testSuppressHtmlRecursiveErrorOutput:
             * SRVE0777E is thrown purposely by application to generate recursive error.
             */
            server.stopServer("SRVE0777E");
        }
    }

    /**
     * Request a simple snoop servlet.
     *
     * @throws Exception
     */
    @Test
    public void test_Simple_Servlet50() throws Exception {
        requestHelper("http", "GET", "/" + APP_NAME + "/snoop5", "", "END OF SNOOP 5. TEST PASS", "");
    }

    /*
     * com.ibm.ws.webcontainer.suppresshtmlrecursiveerroroutput is set to true by default.
     * Also, it is no longer configurable via the property.
     * Test should verify the response which contain only "Error Page Exception" and nothing else.
     *
     * java.io.IOException is throwing purposely to create a recursive error handling.
     */
    @Test
    @ExpectedFFDC({ "java.io.IOException" })
    public void test_SuppressHtmlRecursiveErrorOutput() throws Exception {
        requestHelper("http", "GET", "/" + APP_NAME + "/jsp/JSPErrorsGenerator.jsp?test=tellerrorpagetogeneraterecursiveerror", "500", "", "");
    }

    /**
     *
     * Ensure query param with no equals is registered as an empty string for servlet 5.0.
     *
     * @throws Exception
     */
    @Test
    public void testNoEqualsQueryParameter() throws Exception {
        requestHelper("http", "GET", "/" + APP_NAME + "/query?test", "", "SUCCESS!", "");
    }

    /*
     * Test default enablePostOnlyJSecurityCheck which is true for Servlet 5.0+
     * Any method, but POST, will get a 404 status
     *
     * POST test to verify if the authentication process can redirect to ReLogin.jsp for form login; no login is needed.
     */
    @Test
    public void test_Default_POST_j_security_check() throws Exception {
        requestHelper("http", "POST", "/" + APP_NAME + "/j_security_check", "200", "ReLogin", "");
    }

    /*
     * Test default enablePostOnlyJSecurityCheck not allow GET /j_security_check. Response with a 404
     */
    @Test
    public void test_Default_GET_j_security_check() throws Exception {
        requestHelper("http", "GET", "/" + APP_NAME + "/j_security_check", "404", "", "");
    }

    /*
     * Test enablePostOnlyJSecurityCheck = "false" to allow GET /j_security_check. Redirect to ReLogin.jsp; no login is needed
     *
     * Load EnablePostOnlyJSecurityCheckServer.xml which contains custom property
     */
    @Test
    public void test_GET_j_security_check() throws Exception {
        server.saveServerConfiguration();
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("serverConfigs/EnablePostOnlyJSecurityCheckServer.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false);

        configuration = server.getServerConfiguration();
        LOG.info("Updated server configuration: " + configuration);

        try {
            requestHelper("http", "GET", "/" + APP_NAME + "/j_security_check", "200", "ReLogin", "");
        } finally {
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false);
        }
    }

    /**
     * Common request helper
     *
     * reqScheme - http or https (empty will default to http)
     * reqHttpMethod - GET/POST/PUT .. (default is GET)
     * reqURI - /ContextRoot/path?queryName=queryValue
     *
     * resExpectedStatus - Response expecting status code
     * resExpectedText - Response expecting text
     * resNotExpectedText - Response NOT expecting text
     *
     */
    private void requestHelper(String reqScheme, String reqHttpMethod, String reqURI,
                               String resExpectedStatus, String resExpectedText, String resNotExpectedText) throws Exception {

        String reqSchemeType = "http";
        HttpUriRequestBase method;

        LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        LOG.info("RequestHelper sends request. ENTER");

        if (reqScheme != null && !reqScheme.isEmpty())
            reqSchemeType = reqScheme.toLowerCase();

        String url = reqSchemeType + "://" + server.getHostname() + ":" + server.getHttpDefaultPort() + reqURI;

        LOG.info("Expecting response text [" + resExpectedText + "]");
        LOG.info("Expecting NO response text [" + resNotExpectedText + "]");
        LOG.info("Expecting status code [" + resExpectedStatus + "]");
        LOG.info("Sending --> " + reqHttpMethod + " [" + url + "]");

        switch (reqHttpMethod) {
            case "POST":
                method = new HttpPost(url);
                break;
            default:
                method = new HttpGet(url);
                break;
        }

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(method)) {
                String responseText = EntityUtils.toString(response.getEntity());
                String responseCode = String.valueOf(response.getCode());

                LOG.info("\n" + "##### Response Text ##### \n[" + responseText + "]");
                LOG.info("##### Response Code ###### [" + responseCode + "]");

                if (resExpectedStatus != null && !resExpectedStatus.isEmpty())
                    assertTrue("The response did not contain the status code " + resExpectedStatus, responseCode.equals(resExpectedStatus));

                if (resExpectedText != null && !resExpectedText.isEmpty())
                    assertTrue("The response did not contain the following text: " + resExpectedText, responseText.contains(resExpectedText));

                if (resNotExpectedText != null && !resNotExpectedText.isEmpty())
                    assertFalse("The response did not contain the following text: " + resNotExpectedText, responseText.contains(resNotExpectedText));
            }
        }

        LOG.info("RequestHelper. RETURN");
        LOG.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }
}
