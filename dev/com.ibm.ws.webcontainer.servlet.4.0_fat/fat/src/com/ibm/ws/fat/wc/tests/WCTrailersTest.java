/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class WCTrailersTest {

    private static final Logger LOG = Logger.getLogger(WCTrailersTest.class.getName());

    @Server("servlet40_wcServer")
    public static LibertyServer server;

    private static boolean usingNetty;

    @BeforeClass
    public static void before() throws Exception {
        LOG.info("Setup : add TrailersTest.war to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, "TrailersTest.war", "trailers.servlets", "trailers.listeners");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCTrailersTest.class.getSimpleName() + ".log");
        // Go through Logs and check if Netty is being used
        // Wait for endpoints to finish loading and get the endpoint started messages
        server.waitForStringInLog("CWWKO0219I.*");
        List<String> test = server.findStringsInLogs("CWWKO0219I.*");
        LOG.info("Got port list...... " + Arrays.toString(test.toArray()));
        LOG.info("Looking for port: " + server.getHttpDefaultPort());
        for (String endpoint : test) {
            LOG.info("Endpoint: " + endpoint);
            if (!endpoint.contains("port " + Integer.toString(server.getHttpDefaultPort())))
                continue;
            LOG.info("Netty? " + endpoint.contains("io.openliberty.netty.internal.tcp.TCPUtils"));
            usingNetty = endpoint.contains("io.openliberty.netty.internal.tcp.TCPUtils");
            break;
        }
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
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
        HttpHost target = new HttpHost(server.getHostname(), server.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        LOG.info("Using Netty : " + usingNetty);

        String requestUri = "/TrailersTest/ServletGetTrailers";

        if (parameters != null){
            requestUri += parameters;
            if(usingNetty)
                requestUri += "&usingNetty=true";
        }else if(usingNetty){
            requestUri += "?usingNetty=true";
        }

        ClassicHttpRequest request = new BasicClassicHttpRequest("POST", requestUri);
        Header[] trailers = { new BasicHeader("t1", "TestTrailer1"), new BasicHeader("t2", "TestTrailer2"),
                              new BasicHeader("t3", "TestTrailer3") };

        HttpEntity requestBody = HttpEntities.create("Chunked message with trailers", ContentType.TEXT_PLAIN, trailers[0], trailers[1], trailers[2]);
        request.setEntity(requestBody);

        LOG.info(">> Request URI: " + request.getUri());
        URIAuthority auth = new URIAuthority(server.getHostname(), server.getHttpDefaultPort());
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
        HttpHost target = new HttpHost(server.getHostname(), server.getHttpDefaultPort());
        BasicHttpContext coreContext = new BasicHttpContext();

        LOG.info("Target host : " + target.toURI());

        LOG.info("Using Netty : " + usingNetty);

        String requestUri = "/TrailersTest/ServletSetTrailers";

        if (parameters != null){
            requestUri += parameters;
            if(usingNetty)
                requestUri += "&usingNetty=true";
        }else if(usingNetty){
            requestUri += "?usingNetty=true";
        }

        ClassicHttpRequest request = new BasicClassicHttpRequest("POST", requestUri);

        HttpEntity requestBody = new StringEntity("Inbound request data, please send trailer back", ContentType.TEXT_PLAIN);
        request.setEntity(requestBody);

        LOG.info(">> Request URI: " + request.getUri());
        URIAuthority auth = new URIAuthority(server.getHostname(), server.getHttpDefaultPort());
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
}
