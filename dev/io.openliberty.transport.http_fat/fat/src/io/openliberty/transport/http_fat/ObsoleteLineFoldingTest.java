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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests that inbound HTTP/1.1 requests with malformed framing headers 
 * are rejected to prevent request-boundary ambiguity.
 */
@RunWith(FATRunner.class)
public class ObsoleteLineFoldingTest {

    private static final Logger LOG = Logger.getLogger(ObsoleteLineFoldingTest.class.getName());
    private static final String APP_NAME = "obsFoldApp";
    private static final String SERVLET_PATH = "/" + APP_NAME + "/ObsFoldEchoServlet";
    private static final String HOST_HEADER_NAME = "malformed-framing.example.test";
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
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
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
        String marker = "probe=fix1-cl";
        String secondaryRequest = secondaryGetRequest(marker);
        String request = "GET " + SERVLET_PATH + " HTTP/1.1\r\n" +
                "Host: " + hostHeader() + "\r\n" +
                "Content-Length:\r\n" +
                " " + secondaryRequest.getBytes(StandardCharsets.ISO_8859_1).length + "\r\n" +
                "Connection: keep-alive\r\n" +
                "\r\n" +
                secondaryRequest;

        assertMalformedRequestRejected(request, marker);
    }

    @Test
    public void rejectsFoldedTransferEncodingContinuationWithContentLength() throws Exception {
        String marker = "probe=fix1-te";
        String request = "POST " + SERVLET_PATH + " HTTP/1.1\r\n" +
                "Host: " + hostHeader() + "\r\n" +
                "Transfer-Encoding: identity\r\n" +
                " ,chunked\r\n" +
                "Content-Length: 5\r\n" +
                "Connection: keep-alive\r\n" +
                "\r\n" +
                "0\r\n\r\n" +
                secondaryGetRequest(marker);

        assertMalformedRequestRejected(request, marker);
    }

    @Test
    public void rejectsContentLengthWithMalformedTransferEncoding() throws Exception {
        String marker = "probe=fix2-tecl";
        String request = "GET " + SERVLET_PATH + " HTTP/1.1\r\n" +
                         "Host: " + hostHeader() + "\r\n" +
                         "Content-Length: 0\r\n" +
                         "Transfer-Encoding: ,chunked\t\r\n" +
                         "Connection: keep-alive\r\n" +
                         "\r\n" +
                         secondaryGetRequest(marker);

        assertMalformedRequestRejected(request, marker);
    }

    private static void assertMalformedRequestRejected(String request, String marker) throws Exception {
        RawHttpExchange exchange = sendRawRequest(request);
        String response = exchange.getResponse();
        LOG.info("Raw response for " + marker + ":\n" + response);

        List<String> statusLines = statusLines(response);
        assertEquals("Expected a single HTTP response for malformed framing request. Response was:\n" + response,
                1, statusLines.size());
        assertTrue("Expected HTTP 400 for malformed framing request. Status lines were: " + statusLines,
                statusLines.get(0).startsWith("HTTP/1.1 400"));
        assertFalse("Malformed request marker must not be processed. Response was:\n" + response,
                response.contains(marker));
        assertTrue("Malformed framing request should result in a closed connection. Response was:\n" + response,
                   exchange.isClosedByServer() || hasConnectionCloseHeader(response));
    }

    private static RawHttpExchange sendRawRequest(String request) throws Exception {
        try (Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort())) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            InputStream in = socket.getInputStream();
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            boolean closedByServer = false;
            byte[] buffer = new byte[4096];
            while (true) {
                try {
                    int read = in.read(buffer);
                    if (read < 0) {
                        closedByServer = true;
                        break;
                    }
                    response.write(buffer, 0, read);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
            return new RawHttpExchange(response.toString(StandardCharsets.ISO_8859_1.name()), closedByServer);
        }
    }

    private static boolean hasConnectionCloseHeader(String response) {
        String[] lines = response.split("\\r?\\n");
        for (String line : lines) {
            if (line.toLowerCase(Locale.ENGLISH).startsWith("connection:") && line.toLowerCase(Locale.ENGLISH).contains("close")) {
                return true;
            }
        }
        return false;
    }

    private static final class RawHttpExchange {
        private final String response;
        private final boolean closedByServer;

        RawHttpExchange(String response, boolean closedByServer) {
            this.response = response;
            this.closedByServer = closedByServer;
        }

        String getResponse() {
            return response;
        }

        boolean isClosedByServer() {
            return closedByServer;
        }
    }

    private static String secondaryGetRequest(String marker) {
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
