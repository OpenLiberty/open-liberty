/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.server.transport.http2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.http2.client.SecureHttp2Client;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test for HTTP/2 duplicate END_STREAM issue when filter calls close()
 * after servlet has already sent END_STREAM via auto-flush.
 * 
 * Issue: OLHG34644
 * When Content-Length is set and response body triggers auto-flush,
 * HttpServiceContextImpl.formatBody() sends END_STREAM. If a filter then
 * calls close() on the output stream, it attempts to send another END_STREAM,
 * causing: "H2Exception: stream was already closed!"
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Http2DuplicateEndStreamTest {

    private static final String CLASS_NAME = Http2DuplicateEndStreamTest.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
    
    private static final LibertyServer server = 
        LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.secure");
    
    private static SecureHttp2Client client;

    @BeforeClass
    public static void setup() throws Exception {
        LOGGER.logp(Level.INFO, CLASS_NAME, "setup", "Starting server...");
        
        // Add test application with servlets and filter
        H2FATApplicationHelper.addWarToServerDropins(
            server, "H2EndStreamTest.war", true, 
            "http2.test.war.endstream.servlets");
        
        server.startServer(Http2DuplicateEndStreamTest.class.getSimpleName() + ".log");
        
        // Wait for HTTPS to be ready
        assertNotNull("CWWKO0219I.*ssl not received", 
            server.waitForStringInLog("CWWKO0219I.*ssl"));
        
        client = new SecureHttp2Client();
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "setup", "Server started successfully");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        LOGGER.logp(Level.INFO, CLASS_NAME, "tearDown", "Stopping server...");
        server.stopServer(true);
    }

    /**
     * Test: Filter calls close() after servlet with Content-Length and explicit flush
     * 
     * Scenario:
     * 1. Servlet sets Content-Length: 49152 (48KB)
     * 2. Servlet writes 6 chunks of 8KB with explicit flush() after each
     * 3. Auto-flush triggers when buffer fills, sending END_STREAM
     * 4. Filter's close() attempts to send another END_STREAM
     * 
     * Expected: Response completes successfully. FAT framework will detect any unexpected exceptions/FFDCs.
     */
    @Test
    public void testFilterCloseWithContentLengthAndFlush() throws Exception {
        LOGGER.logp(Level.INFO, CLASS_NAME, "testFilterCloseWithContentLengthAndFlush", 
            "Testing filter close with explicit flush...");
        
        String[] requestUris = new String[] { "/H2EndStreamTest/large-body-flush" };
        int port = server.getHttpSecondarySecurePort();
        
        List<String> results = client.makeSecureRequests(
            server.getHostname(), port, requestUris, 0);
        
        // Verify we got a response
        assertNotNull("No response received", results);
        assertTrue("Expected at least one response", results.size() >= 1);
        
        // Verify response contains expected data
        String response = results.get(0);
        assertNotNull("Response body is null", response);
        assertTrue("Response too short, expected ~48KB", response.length() > 40000);
        
        LOGGER.logp(Level.INFO, CLASS_NAME, "testFilterCloseWithContentLengthAndFlush", 
            "Test completed successfully");
    }

}
