/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests to ensure OpenAPI UI bundles are updated with custom CSS files provided by users
 *
 * - Set a valid CSS file and ensure that the UI is updated with customized value
 * - Set an empty CSS file and server should produce a warning because it does not contain .swagger-ui .headerbar
 * - Set an invalid CSS file where the value of background-image property is not valid and ensure that the right message shows up in the server logs and the CSS content must revert
 * to default
 * - Ensure there are no caching issues. First set customization, then stop the server. When the server is offline, remove the customization. Then start the server and verify
 * customization is no longer applied.
 *
 */
@RunWith(FATRunner.class)
public class UICustomizationTest extends FATServletClient {

    @Server("UICustomizationServer")
    public static LibertyServer server;

    private final static int TIMEOUT = 10000; // in ms
    private final static int START_TIMEOUT = 60000; // in ms

    private final static String WARNING_CUSTOM_CSS_NOT_PROCESSED = "CWWKO1655W";
    private final static String WARNING_CSS_SECTION_NOT_FOUND = "CWWKO1656W";

    private final static String MP_OPEN_API_DIR = "mpopenapi";
    private final static String MP_OPEN_API_CSS_FILE = MP_OPEN_API_DIR + "/customization.css";

    private final static String VALID_CSS_FILE = "css/valid/customization.css";
    private final static String INVALID_CSS_FILE = "css/invalid/customization.css";
    private final static String EMPTY_CSS_FILE = "css/empty/customization.css";

    private final static String UI_CUSTOM_HEADER_CSS = "/openapi/ui/css/custom-header.css";
    private final static String UI_CUSTOM_IMAGE = "/openapi/ui/css/images/custom-logo.png";

    private final static String CSS_CONTENT_CUSTOM_IMAGE = "url(images/custom-logo.png)";
    private final static String CSS_CONTENT_DEFAULT = ""; // default content is empty

    @BeforeClass
    public static void setUp() throws Exception {
        HttpUtils.trustAllCertificates();

        server.copyFileToLibertyServerRoot(MP_OPEN_API_DIR, VALID_CSS_FILE);

        server.startServer("UICustomizationTest.log", true);

        // Wait for /api/ui
        assertTrue("Web application is not available at */openapi/ui", server.waitForMultipleStringsInLog(2,
            "CWWKT0016I.*/openapi/ui/", TIMEOUT, server.getDefaultLogFile()) == 2);

        assertNotNull("FAIL: Did not receive smarter planet message",
            server.waitForStringInLog("CWWKF0011I", START_TIMEOUT));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(WARNING_CUSTOM_CSS_NOT_PROCESSED, WARNING_CSS_SECTION_NOT_FOUND);
        }
    }

    @Test
    public void testCustomizeOpenAPIUI() throws Exception {

        // ------------------------------------------------------------------
        // Process local CSS file (already set during test set up)
        // ------------------------------------------------------------------
        validateCustomOpenAPIUI();

        // ---------------------------------------------------------------------------------
        // Invalid CSS file (value of background-image property is not valid)
        // ---------------------------------------------------------------------------------
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.copyFileToLibertyServerRoot(MP_OPEN_API_DIR, INVALID_CSS_FILE);
        assertNotNull("Expected warning CWWKO1655W with customization.css was not received",
            server.waitForStringInLogUsingMark("CWWKO1655W.*customization.css", TIMEOUT));

        // ---------------------------------------------------------------------------------
        // After an invalid CSS file the CSS content should revert to default
        // ---------------------------------------------------------------------------------
        assertTrue("Web application /openapi/ui/ was not restarted on server",
            server.waitForMultipleStringsInLogUsingMark(1, "CWWKT0016I.*/openapi/ui/", TIMEOUT,
                server.getDefaultLogFile()) == 1);
        validateDefaultOpenAPIUI();

        // ------------------------------------------------------------------------------------------------------------------------------
        // Ensure Caching issue is resolved
        // Scenario: First set customization, then stop the server. When the server is
        // offline,
        // remove the customization. Then start the server and verify customization is
        // no longer applied.
        // ------------------------------------------------------------------------------------------------------------------------------

        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.copyFileToLibertyServerRoot(MP_OPEN_API_DIR, VALID_CSS_FILE);
        assertTrue("Web application /openapi/ui/ was not restarted on server",
            server.waitForMultipleStringsInLogUsingMark(1, "CWWKT0016I.*/openapi/ui/", TIMEOUT,
                server.getDefaultLogFile()) == 1);
        validateCustomOpenAPIUI();

        tearDown();
        server.deleteFileFromLibertyServerRoot(MP_OPEN_API_CSS_FILE);
        server.startServer("UICustomizationTest.log", false);

        // Wait for /openapi/ui to be restarted - first the regular start then it should
        // be restored to default customization values and started again.
        // It's possible that the server didn't cache the old CSS, so we tolerate one or
        // two openapi/ui messages.
        assertTrue("Web application /openapi/ui/ was not restarted on server",
            server.waitForMultipleStringsInLog(2, "CWWKT0016I.*/openapi/ui/", TIMEOUT * 2,
                server.getDefaultLogFile()) > 0);
        validateDefaultOpenAPIUI();

        // ---------------------------------------------------------------------------------
        // Empty CSS file (should produce a warning because it does not contain
        // .swagger-ui .headerbar)
        // ---------------------------------------------------------------------------------
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.copyFileToLibertyServerRoot(MP_OPEN_API_DIR, EMPTY_CSS_FILE);
        assertNotNull("Expected warning CWWKO1656W with customization.css was not received",
            server.waitForStringInLogUsingMark("CWWKO1656W.*customization.css", TIMEOUT));
        validateDefaultOpenAPIUI();
    }

    private void validateDefaultOpenAPIUI() throws IOException, Exception {
        validateOpenAPIUI(CSS_CONTENT_DEFAULT, false, true);
    }

    private void validateCustomOpenAPIUI() throws IOException, Exception {
        validateOpenAPIUI(CSS_CONTENT_CUSTOM_IMAGE, true, true);
    }

    private boolean validateOpenAPIUI(
        String expectedContent,
        boolean validateImage,
        boolean assertion) throws IOException, Exception {
        // UI endpoint - HTTP
        String cssContent = downloadUrl(UI_CUSTOM_HEADER_CSS);
        boolean valid = validateCSS(cssContent, expectedContent, assertion);
        if (validateImage) {
            if (assertion) {
                assertNotNull("(HTTP) Custom image was not found : " + UI_CUSTOM_IMAGE, downloadUrl(UI_CUSTOM_IMAGE));
            } else {
                valid &= downloadUrl(UI_CUSTOM_IMAGE) != null;
            }
        }
        return valid;
    }

    private static boolean validateCSS(
        String body,
        String referenceText,
        boolean assertion) {
        if (assertion) {
            assertNotNull("FAIL: Unexpected null content", body);
            assertTrue("FAIL: Unexpected content : Didn't find '" + referenceText + "' within content : " + body,
                referenceText.isEmpty() ? body.isEmpty() : body.contains(referenceText));
            assertFalse("FAIL: Unexpected content : Found 'invalid-content' : " + body,
                body.contains("invalid-content"));
        } else {
            return body != null &&
                !body.contains("invalid-content") &&
                (referenceText.isEmpty() ? body.isEmpty() : body.contains(referenceText));
        }
        return true;
    }

    private String downloadUrl(
        String path) throws IOException, Exception {
        return new OpenAPIConnection(server, path).download();
    }
}
