/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsat.virtualhost.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests that the wsAtomicTransaction virtualHostRef configuration attribute
 * correctly restricts the WS-AT web service WAB to the configured virtual host's ports.
 *
 * FAT-1: Posting a WS-AT SOAP Commit message to the default-host port (not in the
 *        virtualHost hostAlias list) must return HTTP 404.
 *
 * FAT-2: Posting the same message to the secondary port (the one in the virtualHost
 *        hostAlias list) must return a non-404 response (HTTP 202 Accepted), confirming
 *        the WAB is reachable on that port.
 */
@RunWith(FATRunner.class)
public class VirtualHostBindingTest {

    private static final LibertyServer server =
        LibertyServerFactory.getLibertyServer("WsatVhostServer");

    // SOAP 1.2 Commit notification targeting an unknown transaction.
    // Liberty's WS-AT participant endpoint uses SOAP 1.2 (soap-envelope 2003/05).
    // The wsa:To address is informational; routing is by context path only.
    static final String SOAP_COMMIT_BODY =
        "<soap:Envelope" +
        "    xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"" +
        "    xmlns:wsa=\"http://www.w3.org/2005/08/addressing\"" +
        "    xmlns:wsat=\"http://docs.oasis-open.org/ws-tx/wsat/2006/06\"" +
        "    xmlns:wscoor=\"http://docs.oasis-open.org/ws-tx/wscoor/2006/06\">" +
        "  <soap:Header>" +
        "    <wsa:Action>" +
        "      http://docs.oasis-open.org/ws-tx/wsat/2006/06/Commit" +
        "    </wsa:Action>" +
        "    <wsa:MessageID>urn:test:virtualhost:msg:001</wsa:MessageID>" +
        "    <wsa:To>" +
        "      http://localhost/ibm/wsatservice/ParticipantService" +
        "    </wsa:To>" +
        "    <wsa:ReplyTo>" +
        "      <wsa:Address>http://www.w3.org/2005/08/addressing/none</wsa:Address>" +
        "    </wsa:ReplyTo>" +
        "    <wscoor:Identifier>" +
        "      unknown-test-tx-id-vhost-00000000" +
        "    </wscoor:Identifier>" +
        "  </soap:Header>" +
        "  <soap:Body>" +
        "    <wsat:Notification/>" +
        "  </soap:Body>" +
        "</soap:Envelope>";

    @BeforeClass
    public static void setUp() throws Exception {
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);
    }

    /**
     * FAT-1: WS-AT SOAP to default-host port must return HTTP 404.
     * The WS-AT WAB is bound to wsatHost (secondary port only).
     * The default-host port is not in wsatHost's hostAlias list.
     */
    @Test
    public void testWrongPortReturns404() throws Exception {
        int defaultPort = server.getHttpDefaultPort();
        int responseCode = postSoapCommit(defaultPort);
        assertEquals("Expected HTTP 404 on default-host port (WS-AT WAB not bound there)",
                     404, responseCode);
    }

    /**
     * FAT-2: WS-AT SOAP to the virtualHost port must NOT return HTTP 404.
     * The WS-AT WAB is bound to wsatHost (secondary port).
     * Liberty returns HTTP 202 Accepted for a Commit to an unknown transaction
     * (spec-conformant: participant sends Committed for unknown tx, response path
     * returns without a SOAP fault).
     */
    @Test
    public void testCorrectPortReachesWsatWab() throws Exception {
        int wsatPort = server.getHttpSecondaryPort();
        int responseCode = postSoapCommit(wsatPort);
        assertFalse("Expected non-404 on wsatHost port (WS-AT WAB must be reachable)",
                    responseCode == 404);
    }

    private int postSoapCommit(int port) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + port
                          + "/ibm/wsatservice/ParticipantService");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type",
                               "application/soap+xml; charset=UTF-8; action=\""
                               + "http://docs.oasis-open.org/ws-tx/wsat/2006/06/Commit\"");
        con.setRequestProperty("SOAPAction",
                               "\"http://docs.oasis-open.org/ws-tx/wsat/2006/06/Commit\"");
        con.setConnectTimeout(10000);
        con.setReadTimeout(30000);
        try (OutputStream os = con.getOutputStream()) {
            os.write(SOAP_COMMIT_BODY.getBytes("UTF-8"));
        }
        try {
            return con.getResponseCode();
        } catch (java.io.FileNotFoundException e) {
            // HttpURLConnection throws FileNotFoundException for 404 responses
            // when the server returns an error body. Return 404 explicitly.
            return 404;
        }
    }
}
