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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to verify that the Open Liberty welcome page displays the runtime version
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WelcomePageVersionTest {
    private static final Class<?> c = WelcomePageVersionTest.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Server("WelcomePageVersionServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server with no applications deployed
        // This will show the default welcome page
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that the welcome page displays the runtime version.
     * Verifies that:
     * 1. The actual server version appears in the page title
     * 2. The actual server version appears in the page content (h2 subtitle)
     */
    @Test
    public void testWelcomePageDisplaysVersion() throws Exception {
        LOG.info("Testing welcome page displays runtime version");
        
        String actualVersion = server.getOpenLibertyVersion();
        assertNotNull("Server version should not be null", actualVersion);
        LOG.info("Server version: " + actualVersion);
        
        String welcomePageContent = getWelcomePageContent();

        // Printing the welcomePageContent received
        LOG.info("=== Welcome Page Content START ===");
        LOG.info(welcomePageContent);
        LOG.info("=== Welcome Page Content END ===");
        assertNotNull("Welcome page content should not be null", welcomePageContent);
        
        // Check that the title contains the actual server version
        assertTrue("Welcome page title should contain version: " + actualVersion,
                   welcomePageContent.contains("<title>" + actualVersion + "</title>") ||
                   welcomePageContent.matches("(?s).*<title>.*" + Pattern.quote(actualVersion) + ".*</title>.*"));
        
        // Check that the page content h2 subtitle contains the actual server version
        assertTrue("Welcome page subtitle should contain version: " + actualVersion,
                   welcomePageContent.matches("(?s).*<h2[^>]*>.*" + Pattern.quote(actualVersion) + ".*</h2>.*"));
        
        LOG.info("Successfully verified welcome page displays runtime version: " + actualVersion);
    }

    /**
     * Helper method to get the welcome page content
     */
    private String getWelcomePageContent() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        int responseCode = conn.getResponseCode();
        assertTrue("Expected HTTP 200 response, got: " + responseCode, responseCode == 200);
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }
}
