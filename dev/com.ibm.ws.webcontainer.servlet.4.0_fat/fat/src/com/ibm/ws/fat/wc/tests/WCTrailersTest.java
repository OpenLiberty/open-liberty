/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.impl.bootstrap.HttpRequester;
import org.apache.hc.core5.http.impl.bootstrap.RequesterBootstrap;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.hc.core5.util.Timeout;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@RunWith(FATRunner.class)
public class WCTrailersTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCTrailersTest.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_wcServer");

    @BeforeClass
    public static void before() throws Exception {
        LOG.info("Setup : add TestServlet40 to the server if not already present.");

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestServlet40.ear", true,
                                                  "TestServlet40.war", true, "TestServlet40.jar", true, "testservlet40.war.servlets",
                                                  "testservlet40.war.listeners", "testservlet40.jar.servlets");

        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("CWWWC0401E:.*");
        SHARED_SERVER.getLibertyServer().addIgnoredErrors(expectedErrors);
        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("TestServlet40", WCTrailersTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        SHARED_SERVER.getLibertyServer().stopServer();
    }

    @Test
    public void testServletRequestsTrailers() throws Exception {
        LOG.info("Starting test testServletRequestsTrailers");

        sendRequestWithTrailers(null);

        LOG.info("Finished test testServletRequestsTrailers");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testReadListenerRequestsTrailers() throws Exception {
        LOG.info("Starting test testReadListenerRequestsTrailers");

        sendRequestWithTrailers("?Test=RL");

        LOG.info("Finished test testReadListenerRequestsTrailers");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testResponseTrailersSetAfterCommit() throws Exception {
        LOG.info("Starting test testResponseTrailersSetAfterCommit");

        getResponseWithTrailers(null);

        LOG.info("Finished test testResponseTrailersSetAfterCommit");
    }

    @Test
    public void testOneResponseTrailers() throws Exception {
        LOG.info("Starting test testOneResponseTrailers");

        getResponseWithTrailers("?Test=Add1Trailer");

        LOG.info("Finished test testOneResponseTrailers");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testTwoResponseTrailers() throws Exception {
        LOG.info("Starting test testTwoResponseTrailers");

        getResponseWithTrailers("?Test=Add2Trailers");

        LOG.info("Finished test testTwoResponseTrailers");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testThreeResponseTrailers() throws Exception {
        LOG.info("Starting test testThreeResponseTrailers");

        getResponseWithTrailers("?Test=Add3Trailers");

        LOG.info("Finished test testThreeResponseTrailers");
    }

    private void sendRequestWithTrailers(String parameters) throws Exception {
        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(SHARED_SERVER.getLibertyServer().getHostname(), SHARED_SERVER.getLibertyServer().getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/TestServlet40/ServletGetTrailers";

        if (parameters != null)
            requestUri += parameters;

        ClassicHttpRequest request = new BasicClassicHttpRequest("POST", requestUri);
        Header[] trailers = { new BasicHeader("t1", "TestTrailer1"), new BasicHeader("t2", "TestTrailer2"),
                              new BasicHeader("t3", "TestTrailer3") };

        HttpEntity requestBody = HttpEntities.create("Chunked message with trailers", ContentType.TEXT_PLAIN, trailers[0], trailers[1], trailers[2]);
        request.setEntity(requestBody);

        LOG.info(">> Request URI: " + request.getUri());
        URIAuthority auth = new URIAuthority(SHARED_SERVER.getLibertyServer().getHostname(), SHARED_SERVER.getLibertyServer().getHttpDefaultPort());
        request.setAuthority(auth);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, Timeout.ofSeconds(5), coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());

            LOG.info("\n" + responseText);

            assertFalse("Response contains as failure message", responseText.contains("FAIL"));
            assertTrue("Response does not contain as pass message", responseText.contains("PASS"));

            for (Header trailerHeader : trailers) {
                assertTrue("Response indicates a trailer header was not received:" + trailerHeader.getName(),
                           responseText.contains(trailerHeader.getValue()));
            }
        }
    }

    private void getResponseWithTrailers(String parameters) throws Exception {
        HttpRequester httpRequester = RequesterBootstrap.bootstrap().create();
        HttpHost target = new HttpHost(SHARED_SERVER.getLibertyServer().getHostname(), SHARED_SERVER.getLibertyServer().getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        String requestUri = "/TestServlet40/ServletSetTrailers";

        if (parameters != null)
            requestUri += parameters;

        ClassicHttpRequest request = new BasicClassicHttpRequest("POST", requestUri);

        HttpEntity requestBody = new StringEntity("Inbound request data, please send trailer back", ContentType.TEXT_PLAIN);
        request.setEntity(requestBody);

        LOG.info(">> Request URI: " + request.getUri());
        URIAuthority auth = new URIAuthority(SHARED_SERVER.getLibertyServer().getHostname(), SHARED_SERVER.getLibertyServer().getHttpDefaultPort());
        request.setAuthority(auth);

        try (ClassicHttpResponse response = httpRequester.execute(target, request, null, Timeout.ofSeconds(5),
                                                                  coreContext)) {

            String responseText = EntityUtils.toString(response.getEntity());

            LOG.info("\n" + responseText);

            assertFalse("Response contains as failure message", responseText.contains("FAIL"));
            assertTrue("Response does not contain as pass message", responseText.contains("PASS"));

            // Currently the test harness does not include support for receiving
            // trailers
            Supplier<List<? extends Header>> responseTrailers = response.getEntity().getTrailers();
            if (responseTrailers != null) {
                List<? extends Header> trailerList = responseTrailers.get();
                for (Header header : trailerList) {
                    LOG.info("Response trailer: " + header.getName() + " = " + header.getValue());
                }
            } else {
                LOG.info("No trailers on response");
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

}
