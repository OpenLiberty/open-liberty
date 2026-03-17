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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

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

/*
 * Verifies config is not picked up when a default ID is specified 
 *  - httpOptions, headers, compression, remoteIp, and samesite elements are tested. 
 *  - There are exceptions (element picked up regarddless of ID):
 *        -  httpEndpoint, tcpOptions, and httpAccessLogging.
 *  - Note: defaultSSLOptions is not tested 
 * Links:
 * https://github.com/OpenLiberty/open-liberty/issues/32872 
 * https://github.ibm.com/websphere/POC-Meeting/issues/192 
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class DefaultConfigIdTests {

    private static final String CLASS_NAME = DefaultConfigIdTests.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final String APP_NAME = "ConfigTest";

    @Server("DefaultConfigIdServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, APP_NAME + ".war", "io.openliberty.transport.http.config");
        server.startServer(DefaultConfigIdTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Testing that defaultHeaders configuration is ignored
     */
    @Test
    public void testDefaultHeadersIgnored() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.connect();

            String customHeader = conn.getHeaderField("Custom-Header");
            String missingHeader = conn.getHeaderField("Test-Missing");

            assertNull("defaultHeaders should be ignored - Custom-Header should not be present", customHeader);
            assertNull("defaultHeaders should be ignored - Test-Missing should not be present", missingHeader);

        } finally {
            conn.disconnect();
        }
    }

    /*
     * Testing that defaultCompression configuration is ignored
     */
    @Test
    public void testDefaultCompressionIgnored() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.connect();

            // Verify Vary header is NOT present (compression config is inactive)
            String varyHeader = conn.getHeaderField("Vary");

            assertTrue("defaultCompression should be ignored - Vary header should not contain Accept-Encoding",
                      varyHeader == null || !varyHeader.contains("Accept-Encoding"));

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Testing that defaultRemoteIp configuration is ignored. No messages should appear in the trace. 
     */
    @Test
    public void testDefaultRemoteIpIgnored() throws Exception {
        // Verify that remoteIp configuration messages do NOT appear in the logs
        assertNull("defaultRemoteIp should be ignored - 'HTTP Channel Config: remoteIp has been enabled' should NOT appear",
            server.waitForStringInTrace("HTTP Channel Config: remoteIp has been enabled", 2000));
        assertNull("defaultRemoteIp should be ignored - 'RemoteIp Config: proxies regex set to' should NOT appear",
            server.waitForStringInTrace("RemoteIp Config: proxies regex set to", 2000));
        assertNull("defaultRemoteIp should be ignored - 'RemoteIp Config: useRemoteIpInAccessLog set to' should NOT appear",
            server.waitForStringInTrace("RemoteIp Config: useRemoteIpInAccessLog set to", 2000));
    }

    /* 
     * Testing that defaultSameSite configuration is ignored 
     * TestCookie shouldn't have SameSite=lax. 
     */
    @Test
    public void testDefaultSameSiteIgnored() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                          + "/" + APP_NAME + "/cookie");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();
            LOG.info("Response code: " + responseCode);

            List<String> cookies = conn.getHeaderFields().get("Set-Cookie");

            LOG.info("Cookie: " + (cookies != null ? cookies : "null"));


            // Verify the TestCookie was set
            boolean foundTestCookie = false;
            for (String cookie : cookies) {
                if (cookie.contains("TestCookie=")) {
                    foundTestCookie = true;
                    assertFalse("defaultSameSite should be ignored - SameSite attribute should NOT be found! " + cookie,
                               cookie.toLowerCase().contains("samesite="));
                }
            }
            assertTrue("TestCookie should be present in Set-Cookie headers", foundTestCookie);
        } finally {
            conn.disconnect();
        }
    }

    /*
     * This is one of the exceptions to the default IDs. TCP Options are picked up prior to HTTP, so the 
     * maxOpenConnections value should be picked up. 
     */
    @Test
    public void testDefaultTCPOptions() throws Exception {
        LOG.info("Testing that testTCPOptions configuration is applied");

        assertNotNull("testTCPOptions should be applied - expected maxOpenConnections log message",
                     server.waitForStringInTrace(".*maxOpenConnections: 10.*"));
    }

    /**
     * Verifies the default read / write timeouts are logged instead of the custom values. 
     */
    @Test
    public void testDefaultHttpOptionsNotIgnored() throws Exception {
        LOG.info("Testing that defaultHttpOptions configuration is NOT ignored (exception)");

        // 150s = 150000ms
        assertNotNull("defaultHttpOptions readTimeout=150s should be applied - expected 'Config: Read timeout is 150000'",
                     server.waitForStringInTrace(".*Config: Read timeout is 150000.*"));
        assertNotNull("defaultHttpOptions writeTimeout=150s should be applied - expected 'Config: Write timeout is 150000'",
                     server.waitForStringInTrace(".*Config: Write timeout is 150000.*"));
    }

    /**
     * Verifies access logging works, and the pattern matches the specified pattern: %h %u "%r" %s %b
     */
    @Test
    public void testDefaultAccessLoggingNotIgnored() throws Exception {
        LOG.info("Testing that defaultAccessLogging configuration is NOT ignored (exception)");

        Integer initialLineCount = server.findStringsInFileInLibertyServerRoot("GET", "logs/http_access.log").size();

        // Make a request to generate access log entry
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.connect();
            int responseCode = conn.getResponseCode();
            LOG.info("Response code: " + responseCode);
        } finally {
            conn.disconnect();
        }

        // Small pause
        Thread.sleep(1000);

        // Check that access log has one more entry than before
        List<String> lines = server.findStringsInFileInLibertyServerRoot("GET", "logs/http_access.log");
        assertTrue("Access log should have one more line entry", (initialLineCount + 1) == lines.size());

        String lastLine = lines.get(lines.size() - 1);
        LOG.info("Last access log line: " + lastLine);

        // No timestamp -- for easier matching. Note: bytes may vary between machines?
        // Pattern: %h %u "%r" %s %b
        assertTrue("Access log entry does not match expected format '%h %u \"%r\" %s %b'. Entry: " + lastLine,
                   lastLine.matches(".*127\\.0\\.0\\.1 - \"GET / HTTP/1\\.1\" 200 \\d+.*"));
    }
}
