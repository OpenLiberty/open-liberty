/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests that inbound HTTP/1.1 requests with obsolete line folding are rejected
 * before folded framing headers can create a request-boundary mismatch.
 */
@RunWith(FATRunner.class)
public class ObsoleteLineFoldingTest {

    private static final Logger LOG = Logger.getLogger(ObsoleteLineFoldingTest.class.getName());
    private static final String APP_NAME = "obsFoldApp";
    private static final String SERVLET_PATH = "/" + APP_NAME + "/ObsFoldEchoServlet";
    private static final String HOST_HEADER_NAME = "obsfold1.example.test";
    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";
    private static final int SOCKET_TIMEOUT_MS = 1000;
    private static boolean runningNetty = false;

    @Server("ObsoleteLineFolding")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "io.openliberty.transport.http_fat.obsfold.servlets");
        server.addInstalledAppForValidation(APP_NAME);
        configurePorts();
        server.startServer();
        server.waitForStringInLog("CWWKT0016I:.*" + APP_NAME + ".*");
        logHttpTransport();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private static void logHttpTransport() throws Exception {
        String tcpChannelMessage = server.waitForStringInLog("CWWKO0219I: TCP Channel defaultHttpEndpoint");
        assertNotNull("The default HTTP endpoint did not start.", tcpChannelMessage);
        runningNetty = tcpChannelMessage.contains(NETTY_TCP_CLASS_NAME);
        //Next obs-fold fix covers parity for netty and legacy
        if (runningNetty) {
            LOG.info("Skipping obs-fold FAT behavior assertions on Netty transport: " + tcpChannelMessage);
        } else {
            LOG.info("Running obs-fold FAT on classic HTTP transport: " + tcpChannelMessage);
        }
    }

    private static void assumeClassicTransport() {
        Assume.assumeTrue(!runningNetty);
    }

    private static void configurePorts() throws Exception {
        int httpPort = Integer.getInteger("HTTP_default", server.getHttpDefaultPort());
        if (httpPort == 0) {
            httpPort = 8010;
        }
        int httpsPort = Integer.getInteger("HTTP_default.secure", server.getHttpDefaultSecurePort());
        if (httpsPort == 0) {
            httpsPort = 8020;
        }

        server.setHttpDefaultPort(httpPort);
        server.setHttpDefaultSecurePort(httpsPort);

        RemoteFile bootstrapPropertiesFile = server.getServerBootstrapPropertiesFile();
        Properties props = new Properties();
        if (bootstrapPropertiesFile.exists()) {
            try (InputStream in = bootstrapPropertiesFile.openForReading()) {
                props.load(in);
            }
        }
        props.setProperty("bvt.prop.HTTP_default", String.valueOf(httpPort));
        props.setProperty("bvt.prop.HTTP_default.secure", String.valueOf(httpsPort));
        try (OutputStream out = bootstrapPropertiesFile.openForWriting(false)) {
            props.store(out, "");
        }
    }

    @Test
    public void rejectsFoldedContentLength() throws Exception {
        assumeClassicTransport();

        String marker = "smuggled=fix1-cl";
        String smuggledRequest = smuggledGetRequest(marker);
        String request = "GET " + SERVLET_PATH + " HTTP/1.1\r\n" +
                         "Host: " + hostHeader() + "\r\n" +
                         "Content-Length:\r\n" +
                         " " + smuggledRequest.getBytes(StandardCharsets.ISO_8859_1).length + "\r\n" +
                         "Connection: keep-alive\r\n" +
                         "\r\n" +
                         smuggledRequest;

        assertRejectedWithoutSmuggledResponse(request, marker);
    }

    @Test
    public void rejectsFoldedTransferEncodingContinuation() throws Exception {
        assumeClassicTransport();

        String marker = "smuggled=fix1-te";
        String request = "POST " + SERVLET_PATH + " HTTP/1.1\r\n" +
                         "Host: " + hostHeader() + "\r\n" +
                         "Transfer-Encoding: identity\r\n" +
                         " ,chunked\r\n" +
                         "Content-Length: 5\r\n" +
                         "Connection: keep-alive\r\n" +
                         "\r\n" +
                         "0\r\n\r\n" +
                         smuggledGetRequest(marker);

        assertRejectedWithoutSmuggledResponse(request, marker);
    }

    private static void assertRejectedWithoutSmuggledResponse(String request, String marker) throws Exception {
        String response = sendRawRequest(request);
        LOG.info("Raw response for " + marker + ":\n" + response);

        List<String> statusLines = statusLines(response);
        assertEquals("Expected a single HTTP response for malformed obs-fold request. Response was:\n" + response,
                     1, statusLines.size());
        assertTrue("Expected HTTP 400 for malformed obs-fold request. Status lines were: " + statusLines,
                   statusLines.get(0).startsWith("HTTP/1.1 400"));
        assertFalse("Smuggled request marker must not be processed. Response was:\n" + response,
                    response.contains(marker));
    }

    private static String sendRawRequest(String request) throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            InputStream in = socket.getInputStream();
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (true) {
                try {
                    int read = in.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    response.write(buffer, 0, read);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            return response.toString(StandardCharsets.ISO_8859_1.name());
        }
    }

    private static String smuggledGetRequest(String marker) throws Exception {
        return "GET " + SERVLET_PATH + "?" + marker + " HTTP/1.1\r\n" +
               "Host: " + hostHeader() + "\r\n" +
               "Connection: close\r\n" +
               "\r\n";
    }

    private static String hostHeader() {
        return HOST_HEADER_NAME + ":" + server.getHttpDefaultPort();
    }

    private static List<String> statusLines(String response) {
        String[] lines = response.split("\\r?\\n");
        List<String> statusLines = new ArrayList<String>();
        for (String line : lines) {
            if (line.startsWith("HTTP/1.1 ")) {
                statusLines.add(line);
            }
        }
        return statusLines;
    }
}
