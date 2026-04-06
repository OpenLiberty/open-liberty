/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Abstract base test class for Jakarta Security 4.0 tests.
 * Contains common reusable methods for server setup, HTTP requests, and assertions.
 */
public abstract class BaseJakartaSecurity40Test {

    protected static final String MULTIPLE_HAM_SERVER_NAME = "multipleHAMServer";
    protected static final String IN_MEM_ID_STORE_ENABLED_SERVER_NAME = "inMemIdStoreEnabledServer";

    /**
     * Get the test class for logging purposes.
     * Subclasses should override this to return their own class.
     */
    protected abstract Class<?> getTestClass();

    /**
     * Get the Liberty server instance.
     * Subclasses must implement this to provide their server instance.
     */
    protected abstract LibertyServer getServer();

    /**
     * Build the URL for the application endpoint.
     *
     * @param contextRoot  the context root of the application
     * @param resourcePath the resource path
     * @return the complete URL string
     */
    protected String buildUrl(String contextRoot, String resourcePath) {
        return "http://localhost:" + getServer().getHttpDefaultPort() + contextRoot + resourcePath;
    }

    /**
     * Execute a GET request and verify the expected response code.
     *
     * @param url                the URL to request
     * @param expectedStatusCode the expected HTTP status code
     * @return the HTTP connection
     * @throws Exception if an error occurs
     */
    protected HttpURLConnection executeGetRequest(String url, int expectedStatusCode) throws Exception {
        Log.info(getTestClass(), "executeGetRequest", "Executing GET request to: " + url);

        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        int responseCode = conn.getResponseCode();

        assertEquals("Expected status code " + expectedStatusCode + " but got " + responseCode,
                     expectedStatusCode, responseCode);

        return conn;
    }

    /**
     * Execute a GET request with Basic Authentication and verify the expected response code.
     *
     * @param url                the URL to request
     * @param username
     * @param password
     * @param expectedStatusCode the expected HTTP status code
     * @return the HTTP connection
     * @throws Exception if an error occurs
     */
    protected HttpURLConnection executeGetRequest(String url, String username, String password, int expectedStatusCode) throws Exception {
        Log.info(getTestClass(), "executeGetRequest", "Executing GET request to: " + url);

        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

        // Set Basic Authentication header
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        int responseCode = conn.getResponseCode();

        assertEquals("Expected status code " + expectedStatusCode + " but got " + responseCode,
                     expectedStatusCode, responseCode);

        return conn;
    }

    /**
     * Get the response from a GET request with Basic Authentication
     *
     * @param url
     * @param username
     * @param password
     * @param expectedStatusCode
     * @return the response code as String
     */
    protected String getResponseFromGetRequest(String url, String username, String password, int expectedStatusCode) {
        HttpURLConnection conn = null;
        int responseCode = -1;
        try {
            conn = executeGetRequest(url, username, password, expectedStatusCode);

            responseCode = conn.getResponseCode();
            assertEquals("Expected status code " + expectedStatusCode + " but got " + responseCode,
                         expectedStatusCode, responseCode);
        } catch (Exception e) {
            Log.error(getTestClass(), "getResponseFromGetRequest", e);
            conn.disconnect();
        }

        // Read response
        StringBuilder response = new StringBuilder();
        if (responseCode == 200) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();

            } catch (IOException e) {
                Log.error(getTestClass(), "getResponseFromGetRequest", e);
                conn.disconnect();
            }
        }

        conn.disconnect();

        Log.info(getTestClass(), "getResponseFromGetRequest",
                 "Request to " + url + " with user " + username + " returned status " + responseCode);

        return response.toString();
    }

    /**
     * Mark the server trace log and execute a GET request.
     * Useful for tests that need to check trace output after a request.
     *
     * @param url                the URL to request
     * @param expectedStatusCode the expected HTTP status code
     * @return the HTTP connection
     * @throws Exception if an error occurs
     */
    protected HttpURLConnection executeGetRequestWithTraceMark(String url, int expectedStatusCode) throws Exception {
        getServer().setTraceMarkToEndOfDefaultTrace();
        return executeGetRequest(url, expectedStatusCode);
    }

    /**
     * Mark the server log and execute a GET request.
     * Useful for tests that need to check log output after a request.
     *
     * @param url                the URL to request
     * @param expectedStatusCode the expected HTTP status code
     * @return the HTTP connection
     * @throws Exception if an error occurs
     */
    protected HttpURLConnection executeGetRequestWithLogMark(String url, int expectedStatusCode) throws Exception {
        getServer().setMarkToEndOfLog();
        return executeGetRequest(url, expectedStatusCode);
    }

    /**
     * Wait for a string in the trace log using the current mark.
     *
     * @param searchString the string to search for
     * @return the found string or null if not found
     * @throws Exception if an error occurs
     */
    protected String waitForStringInTrace(String searchString) throws Exception {
        return getServer().waitForStringInTraceUsingMark(searchString);
    }

    /**
     * Wait for a string in the trace log using the current mark with a timeout.
     *
     * @param searchString the string to search for
     * @param timeout      the timeout in milliseconds
     * @return the found string or null if not found
     * @throws Exception if an error occurs
     */
    protected String waitForStringInTrace(String searchString, long timeout) throws Exception {
        return getServer().waitForStringInTraceUsingMark(searchString, timeout);
    }

    /**
     * Wait for a string in the server log using the current mark.
     *
     * @param searchString the string to search for
     * @return the found string or null if not found
     * @throws Exception if an error occurs
     */
    protected String waitForStringInLog(String searchString) throws Exception {
        return getServer().waitForStringInLogUsingMark(searchString);
    }

    /**
     * Wait for a string in the server log using the current mark with a timeout.
     *
     * @param searchString the string to search for
     * @param timeout      the timeout in milliseconds
     * @return the found string or null if not found
     * @throws Exception if an error occurs
     */
    protected String waitForStringInLog(String searchString, long timeout) throws Exception {
        return getServer().waitForStringInLogUsingMark(searchString, timeout);
    }

    /**
     * Assert that a string appears in the trace log.
     *
     * @param message      the assertion message
     * @param searchString the string to search for
     * @throws Exception if an error occurs
     */
    protected void assertStringInTrace(String message, String searchString) throws Exception {
        assertNotNull(message, waitForStringInTrace(searchString));
    }

    /**
     * Assert that a string appears in the trace log with a timeout.
     *
     * @param message      the assertion message
     * @param searchString the string to search for
     * @param timeout      the timeout in milliseconds
     * @throws Exception if an error occurs
     */
    protected void assertStringInTrace(String message, String searchString, long timeout) throws Exception {
        assertNotNull(message, waitForStringInTrace(searchString, timeout));
    }

    /**
     * Assert that a string appears in the server log.
     *
     * @param message      the assertion message
     * @param searchString the string to search for
     * @throws Exception if an error occurs
     */
    protected void assertStringInLog(String message, String searchString) throws Exception {
        assertNotNull(message, waitForStringInLog(searchString));
    }

    /**
     * Assert that a string appears in the server log with a timeout.
     *
     * @param message      the assertion message
     * @param searchString the string to search for
     * @param timeout      the timeout in milliseconds
     * @throws Exception if an error occurs
     */
    protected void assertStringInLog(String message, String searchString, long timeout) throws Exception {
        assertNotNull(message, waitForStringInLog(searchString, timeout));
    }

    /**
     * Log test information.
     *
     * @param methodName the test method name
     * @param message    the message to log
     */
    protected void logInfo(String methodName, String message) {
        Log.info(getTestClass(), methodName, message);
    }

    /**
     * Start the server and log the action.
     *
     * @throws Exception if an error occurs
     */
    protected void startServer() throws Exception {
        logInfo("startServer", "Starting server...");
        getServer().startServer();
        logInfo("startServer", "Server started successfully");
    }

    /**
     * Start the server using a specified configuration file and log the action.
     *
     * @throws Exception if an error occurs
     */
    protected void startServer(String configFile) throws Exception {
        logInfo("startServer", "Starting server...");
        getServer().startServerUsingConfiguration(configFile);
        logInfo("startServer", "Server started successfully");
    }

    /**
     * Stop the server with expected error/warning messages.
     *
     * @param expectedMessages the expected error/warning message IDs
     * @throws Exception if an error occurs
     */
    protected void stopServer(String... expectedMessages) throws Exception {
        logInfo("stopServer", "Stopping server...");
        getServer().stopServer(expectedMessages);
        logInfo("stopServer", "Server stopped successfully");
    }
}
