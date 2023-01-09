/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.fat.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

import com.ibm.ws.ui.fat.FATSuite;

/**
 *
 */
public abstract class CommonRESTTest {
    private final Class<?> c;
    protected final boolean DEBUG = true;

    @Rule
    public final TestName method = new TestName();
    // @Rule
    // public final TestLogger logger = new TestLogger();

    //protected ClientResponse response;
    protected JsonObject response;
    protected String url;
    // protected final BasicAuthSecurityHandler adminCredentials;
    // protected final BasicAuthSecurityHandler nonAdminCredentials;
    // protected final BasicAuthSecurityHandler readerCredentials;
    // protected final BasicAuthSecurityHandler testCredentials;
    protected final String adminUser = "admin";
    protected final String adminPassword = "adminpwd";
    protected final String nonadminUser = "nonadmin";
    protected final String nonadminPassword = "nonadminpwd";
    protected final String readerUser = "reader";
    protected final String readerPassword = "readerpwd";
    protected final String testUser = "test/test";
    protected final String testPassword = "testpwd";

    /**
     * Initializes the common properties for all REST tests.
     *
     * @param c The implementing class
     */
    protected CommonRESTTest(Class<?> c) {
        this.c = c;

        // adminCredentials = new BasicAuthSecurityHandler();
        // adminCredentials.setUserName("admin");
        // adminCredentials.setPassword("adminpwd");

        // nonAdminCredentials = new BasicAuthSecurityHandler();
        // nonAdminCredentials.setUserName("nonadmin");
        // nonAdminCredentials.setPassword("nonadminpwd");

        // readerCredentials = new BasicAuthSecurityHandler();
        // readerCredentials.setUserName("reader");
        // readerCredentials.setPassword("readerpwd");

        // testCredentials = new BasicAuthSecurityHandler();
        // testCredentials.setUserName("test/test");
        // testCredentials.setPassword("testpwd");
    }

    /**
     * Initialize the HTTP connection for this JVM to ignore SSL security.
     * These tests are not SSL centric and we can safely ignore SSL in our tests.
     */
    @BeforeClass
    public static void setupHttpConnection() throws NoSuchAlgorithmException, KeyManagementException {

        // Ignore SSL certificate trust
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Ignore host names during SSL validation
        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

    }

    /**
     * Consume the response. This is indicated as required by the RestClient
     * documentation and is safe to do repeatedly.
     */
    @After
    public void tearDown() {
    }

    /**
     * Get the URL for the server, or a default if none can be determined.
     * <p>
     * We have this implemented to allow for local direct JUnit invocation of the tests.
     *
     * @return The server's base URL. e.g. https://localhost:9443/
     */
    protected String getHTTPSHostAndPort(LibertyServer server) {
        String defaultURL = "https://localhost:9443";

        if (server != null) {
            return "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort();
        } else {
            return defaultURL;
        }
    }

    /**
     * Stop the server. The operation is validated so this operation will not
     * return until the server is fully stopped.
     *
     * @throws Exception
     */
    protected void stopServerAndValidate(LibertyServer server) throws Exception {
        server.stopServer();
        assertFalse("FAIL: Server is not stopped.",
                    server.isStarted());
    }

    /**
     * Start the server. The operation is validated so this operation will not
     * return until the server is fully started and ready for operations.
     *
     * @throws Exception
     */
    protected void startServerAndValidate(LibertyServer server) throws Exception {
        server.startServer();
        assertTrue("FAIL: Server is not started.",
                   server.isStarted());
        assertNotNull("FAIL: failed to detect that HTTPS has started. Aborting test setup",
                      server.waitForStringInLog("CWWKO0219I:.*ssl.*"));
        assertNotNull("The FILE persistence service did not report initialized",
                      server.waitForStringInLog("CWWKX1015I:.*FILE.*"));
        assertNotNull("The server did not report the ibm/api application was ready",
                      server.waitForStringInLog("CWWKT0016I:.*ibm/api/.*"));
        assertNotNull("The server did not report 'The server uiDev is ready to run a smarter planet.'",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    /**
     * Validate that the UI (stand alone) is ready.
     *
     * @param server
     */
    protected void validateUIStarted(LibertyServer server) throws Exception {
        Log.info(c, method.getMethodName(), "Validating the server's UI component has started");
        assertNotNull("The server did not report the UI app (adminCenter) has started. Aborting test setup",
                      server.waitForStringInLogUsingMark("CWWKT0016I:.*/adminCenter"));
        assertNotNull("The server did not report the ibm/api application was ready. Aborting test setup",
                      server.waitForStringInLogUsingMark("CWWKT0016I:.*ibm/api/.*"));
        assertNotNull("The server did not report the FILE persistence layer was available. Aborting test setup",
                      server.waitForStringInLogUsingMark("CWWKX1015I: FILE"));
    }

    /**
     * Updates the end of log mark after a configuration change has been made.
     * This needs to make sure that the 'CWWKF0008I: Feature update completed'
     * message has been reported before moving the log line up.
     */
    protected void setMarkAfterFeatureChange(LibertyServer server) throws Exception {
        Log.info(c, method.getMethodName(), "Setting the new log mark after finding CWWKF0008I");
        assertNotNull("The server did not report feature update completed",
                      server.waitForStringInLogUsingMark("CWWKF0008I:"));
        server.setMarkToEndOfLog();
    }

    /**
     * Setup the fields to create a HTTP request
     *
     * @param url url for the request
     * @param basicAuthUser authorized user 
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * 
     * @return The internal HTTP request class used for testing
     */
    protected CommonHttpsRequest setupHttpsRequest(String url, String basicAuthUser, String basicAuthPassword, int expectedCode) {
        Log.info(c, "setupHttpsRequest", "url=" + url + ", expectedCode=" + expectedCode);
        CommonHttpsRequest request = new CommonHttpsRequest(FATSuite.server, url);
        if ((basicAuthUser != null && basicAuthUser.length() > 0) && (basicAuthPassword != null && basicAuthPassword.length() > 0)) {
            request.basicAuth(basicAuthUser, basicAuthPassword);
        }
        if (expectedCode != -1) {
            request.expectCode(expectedCode);
        }
        return request;
    }

    /**
     * Invoke the HTTP GET method on the specified URL with the given credentials and expected response code.
     *
     * @param url the URL to GET
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * @return The JsonObject response to the GET method
     */
    protected JsonObject get(String url, String basicAuthUser, String basicAuthPassword, int expectedCode) throws Exception { //BasicAuthSecurityHandler creds) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Log.info(c, "get", "url=" + url + ", start at " + dateFormat.format(new Date()));
        CommonHttpsRequest getRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        JsonObject getResponse = getRequest.run(JsonObject.class);
        Log.info(c, "get", "url=" + url + ", finished at " + dateFormat.format(new Date()));

        return getResponse;
    }

    /**
     * Invoke the HTTP GET method on the specified URL with the given credentials and expected response code.
     *
     * @param url the URL to GET
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * @return The String response to the GET method
     */
    protected String getWithStringResponse(String url, String basicAuthUser, String basicAuthPassword, int expectedCode) throws Exception { //BasicAuthSecurityHandler creds) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Log.info(c, "getWithStringResponse", "url=" + url + ", start at " + dateFormat.format(new Date()));

        CommonHttpsRequest getRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        String getResponse = getRequest.run(String.class);
        Log.info(c, "getWithStringResponse", "url=" + url + ", finished at " + dateFormat.format(new Date()));

        return getResponse;
    }

    /**
     * Invoke the HTTP GET method on the specified URL with the given credentials and expected response code.
     *
     * @param url the URL to GET
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * @return The JsonArray response to the GET method
     */
    protected JsonArray getWithJsonArrayResponse(String url, String basicAuthUser, String basicAuthPassword, int expectedCode) throws Exception { //BasicAuthSecurityHandler creds) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Log.info(c, "getWithJsonArrayReponse", "url=" + url + ", start at " + dateFormat.format(new Date()));
        CommonHttpsRequest getRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        JsonArray getResponse = getRequest.run(JsonArray.class);
        Log.info(c, "getWithJsonArrayResponse", "url=" + url + ", finished at " + dateFormat.format(new Date()));

        return getResponse;
    }

    /**
     * Invoke the HTTP GET method on the specified URL with the given credentials and expected response code.
     *
     * @param url the URL to GET
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * @return The byte[] response to the GET method
     */
    protected byte[] getImage(String url, String basicAuthUser, String basicAuthPassword, int expectedCode) throws Exception {
        return getImage(url, basicAuthUser, basicAuthPassword, expectedCode, "image/png");
    }

    /**
     * Invoke the HTTP GET method on the specified URL with the given credentials, expected response code, and expected content type.
     *
     * @param url the URL to GET
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * @param imageType expected content type in the response header
     * @return The byte[] response to the GET method
     */
    protected byte[] getImage(String url, String basicAuthUser, String basicAuthPassword, int expectedCode, String imageType) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Log.info(c, "getImage", "url=" + url + ", start at " + dateFormat.format(new Date()));
        CommonHttpsRequest getRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        byte[] getResponse = getRequest.run(byte[].class);
        Log.info(c, "getImage", "url=" + url + ", finished at " + dateFormat.format(new Date()));
        if (getResponse != null) {
            byte[] image = getResponse;
            Log.info(c, "getImage", "response size in byte[] from getImage: " + image.length);

            Map<String,List<String>> headers =  getRequest.getConnection().getHeaderFields();
            Log.info(c, "getImage", "response headers: " + headers);
            assertContains(headers, "Content-Type");
            assertEquals("FAIL: The expected Content-Type should only be 1",
                         1, headers.get("Content-Type").size());
            assertEquals("FAIL: The expected Content-Type of the response should be " + imageType,
                         imageType, headers.get("Content-Type").get(0));

            return image;
        }
        return null;
    }

    /**
     * Invoke the HTTP GET method on the specified URL with the given credentials and expected response code.
     *
     * @param url the URL to GET
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * @return The Set response to the GET method
     */
    protected Set<String> getImages(String url, String basicAuthUser, String basicAuthPassword, int expectedCode) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Log.info(c, "getImages", "url=" + url + ", start at " + dateFormat.format(new Date()));
        CommonHttpsRequest getRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        String getResponse = getRequest.run(String.class);
        Log.info(c, "getImages", "url=" + url + ", finished at " + dateFormat.format(new Date()));
        Log.info(c, "getImages", "response=" + getResponse);

        if (getResponse != null && getResponse.length() > 0 && !getResponse.equals("[]")) {
            if ((getResponse.indexOf("[") == 0) && (getResponse.indexOf("]") == getResponse.length() - 1)) {
                // split the string by comma, convert array to a list, create HashSet from the list
                String[] strParts = getResponse.substring(1, getResponse.length()-1).split(",");  
                List<String> listParts = Arrays.asList(strParts);
                HashSet<String> hsetFromString = new HashSet<String>( listParts );
                Log.info(c, "getImages", "hashSet=" + hsetFromString);

                return hsetFromString;
            } else {
                return new HashSet<String>();
            }
        }
        return new HashSet<String>();
    }

    /**
     * Invoke the HTTP POST method on the specified URL with the given credentials. The payload is JSON format.
     *
     * @param url the URL to POST
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param payload the payload to send as part of the POST
     * @param expectedCode expected response code
     * @return The JsonObject to the POST method
     */
    protected JsonObject post(String url, String basicAuthUser, String basicAuthPassword, Object payload, int expectedCode) throws Exception {
         Log.info(c, "post", "url=" + url + " expect code=" + expectedCode + " payload=" + payload);
         CommonHttpsRequest postRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
         postRequest.method("POST");
         if (payload != null) {
            postRequest.jsonBody(payload.toString());
         } else {
            postRequest.jsonBody("{}");
         }
         JsonObject postResponse = postRequest.run(JsonObject.class);
         return postResponse;
     }

    /**
     * Invoke the HTTP POST method on the specified URL with the given credentials. The payload is plaintext format.
     *
     * @param url the URL to POST
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param payload the payload to send as part of the POST
     * @param expectedCode expected response code
     * @return The CommonHttpsRequest for the POST method. Call CommonHttpsRequest getConnection, getResponse, and getResponseHeaders to inspect the response.
     */
    protected CommonHttpsRequest getHTTPRequestWithPostPlainText(String url, String basicAuthUser, String basicAuthPassword, Object payload, int expectedCode) throws Exception {
        Log.info(c, "getHTTPRequestWithPostPlainText", "url=" + url + " payload=" + payload);
        CommonHttpsRequest postRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        postRequest.method("POST");
        if (payload != null) {
           postRequest.plainTextBody(payload.toString());
        }
        JsonObject postResponse = postRequest.run(JsonObject.class);
        return postRequest;
    }

    /**
     * Invoke the HTTP PUT method on the specified URL with the given credentials. The payload is JSON format.
     *
     * @param url the URL to PUT
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param payload the payload to send as part of the PUT
     * @param expectedCode expected response code
     * @return The JsonObject to the PUT method
     */
    protected JsonObject put(String url, String basicAuthUser, String basicAuthPassword, Object payload, int expectedCode) throws Exception {
         Log.info(c, "put", "url=" + url + " payload=" + payload);
         CommonHttpsRequest putRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
         putRequest.method("PUT");
         if (payload != null) {
            putRequest.jsonBody(payload.toString());
         } else {
            putRequest.jsonBody("{}");
         }
         JsonObject putResponse = putRequest.run(JsonObject.class);
        return putResponse;
    }

    /**
     * Invoke the HTTP PUT method on the specified URL with the given credentials. The payload is JSON format.
     *
     * @param url the URL to PUT
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param payload the payload to send as part of the PUT
     * @param expectedCode expected response code
     * @return The JsonArray to the PUT method
     */    
    protected JsonArray putWithJsonArrayResponse(String url, String basicAuthUser, String basicAuthPassword, Object payload, int expectedCode) throws Exception {
        Log.info(c, "put", "url=" + url + " payload=" + payload);
        CommonHttpsRequest putRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        putRequest.method("PUT");
        if (payload != null) {
           putRequest.jsonBody(payload.toString());
        } else {
           putRequest.jsonBody("{}");
        }
        JsonArray putResponse = putRequest.run(JsonArray.class);
       return putResponse;
   }

    /**
     * Invoke the HTTP PUT method on the specified URL with the given credentials. The payload is plaintext format.
     *
     * @param url the URL to PUT
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param payload the payload to send as part of the PUT
     * @param expectedCode expected response code
     * @return The CommonHttpsRequest for the PUT method. Call CommonHttpsRequest getConnection, getResponse, and getResponseHeaders to inspect the response.
     */
    protected CommonHttpsRequest getHTTPRequestWithPutPlainText(String url, String basicAuthUser, String basicAuthPassword, Object payload, int expectedCode) throws Exception {
        Log.info(c, "getHTTPRequestWithPutPlainText", "url=" + url + " payload=" + payload);
        CommonHttpsRequest putRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        putRequest.method("PUT");
        if (payload != null) {
           putRequest.plainTextBody(payload.toString());
        }
        JsonObject putResponse = putRequest.run(JsonObject.class);
        return putRequest;
    }

    /**
     * Invoke the HTTP PUT method on the specified URL with the given credentials. The payload is plaintext format.
     *
     * @param url the URL to PUT
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param payload the payload to send as part of the PUT
     * @param header the property key to be included in the request heeader
     * @param headerValue the property value to be included in the request header
     * @param expectedCode expected response code
     * @return The CommonHttpsRequest for the PUT method. Call CommonHttpsRequest getConnection, getResponse, and getResponseHeaders to inspect the response.
     */
    protected CommonHttpsRequest getHTTPRequestWithPutPlainTextWithHeader(String url, String basicAuthUser, String basicAuthPassword, Object payload, 
                                                String header, String headerValue, int expectedCode) throws Exception {
        Log.info(c, "getHTTPRequestWithPutPlainTextWithHeader", "url=" + url + " payload=" + payload);
        CommonHttpsRequest putRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        putRequest.method("PUT");
        if (payload != null) {
           putRequest.plainTextBody(payload.toString());
        }
        if (header != null && headerValue != null) {
            putRequest.requestProp(header, headerValue);
        }
        JsonObject putResponse = putRequest.run(JsonObject.class);
        return putRequest;
    }

    /**
     * Invoke the HTTP PUT method on the specified URL with the given credentials. The payload is plaintext format.
     *
     * @param url the URL to PUT
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param payload the payload to send as part of the PUT
     * @param header the property key to be included in the request heeader
     * @param headerValue the property value to be included in the request header
     * @param expectedCode expected response code
     * @return The CommonHttpsRequest for the PUT method. Call CommonHttpsRequest getConnection, getResponse, and getResponseHeaders to inspect the response.
     */
    protected CommonHttpsRequest getHTTPRequestWithPutPlainTextWithHeaderWithStringResponse(String url, String basicAuthUser, String basicAuthPassword, Object payload, 
                                                String header, String headerValue, int expectedCode) throws Exception {
        Log.info(c, "getHTTPRequestWithPutPlainTextWithHeader", "url=" + url + " payload=" + payload);
        CommonHttpsRequest putRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        putRequest.method("PUT");
        if (payload != null) {
           putRequest.plainTextBody(payload.toString());
        }
        if (header != null && headerValue != null) {
            putRequest.requestProp(header, headerValue);
        }
        String putResponse = putRequest.run(String.class);
        return putRequest;
    }

    /**
     * Invoke the HTTP DELETE method on the specified URL with the given credentials.
     *
     * @param url the URL to DELETE
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * @return The JsonObject response to the DELETE method
     */
    protected JsonObject delete(String url, String basicAuthUser, String basicAuthPassword, int expectedCode) throws Exception{
        Log.info(c, "delete", "url=" + url);
        CommonHttpsRequest deleteRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        deleteRequest.method("DELETE");
        JsonObject deleteResponse = deleteRequest.run(JsonObject.class);
        return deleteResponse;
    }

    /**
     * Invoke the HTTP DELETE method on the specified URL with the given credentials.
     *
     * @param url the URL to DELETE
     * @param basicAuthUser authorized user
     * @param basicAuthPassword authorized user password
     * @param expectedCode expected response code
     * @return The String response to the DELETE method
     */
    protected String deleteWithStringResponse(String url, String basicAuthUser, String basicAuthPassword, int expectedCode) throws Exception{
        Log.info(c, "delete", "url=" + url);
        CommonHttpsRequest deleteRequest = setupHttpsRequest(url, basicAuthUser, basicAuthPassword, expectedCode);
        deleteRequest.method("DELETE");
        String deleteResponse = deleteRequest.run(String.class);
        return deleteResponse;
    }

    /**
     * Assert (with junit assertions) that the object is of a specific size.
     *
     * @param obj The JSON object
     */
    protected void assertSize(Map<?, ?> obj, int size) {
        assertEquals("FAIL: The JSON object did not have the expected number of fields",
                     size, obj.size());
    }

    /**
     * Assert (with junit assertions) that the list is of a specific size.
     *
     * @param obj The JSON list
     */
    protected void assertSize(List<?> list, int size) {
        assertEquals("FAIL: The JSON list did not have the expected number of elements",
                     size, list.size());
    }

    /**
     * Assert (with junit assertions) that the specified key is present in
     * the JSON object.
     *
     * @param obj   The JSON object
     * @param field The field (a.k.a. key) to check
     */
    protected void assertContains(Map<?, ?> obj, String field) {
        assertTrue("FAIL: The object does not contain the '" + field + "' field",
                   obj.containsKey(field));
    }

    /**
     * Assert (with junit assertions) that the specified key is present in
     * the JSON object, and that the key has the expected value.
     *
     * @param obj   The JSON object
     * @param field The field (a.k.a. key) to check
     * @param value The expected value of the field
     */
    protected void assertContains(Map<?, ?> obj, String field, Object value) {
        assertContains(obj, field);
        assertEquals("FAIL: The '" + field + "' field does not match the expected value",
                     value, obj.get(field));
    }

    protected void assertContains(JsonObject obj, String field, Object value) {
        assertContains(obj, field, value, false);
    }

    protected void assertContains(JsonObject obj, String field, Object value, boolean isBooleanValue) {
        assertContains(obj, field);
        if (isBooleanValue) {
            assertEquals("FAIL: The '" + field + "' field does not match the expected boolean value",
                value, obj.getBoolean(field));
        } else {
            assertEquals("FAIL: The '" + field + "' field does not match the expected string value",
                     value, obj.getString(field));
        }
    }

    /**
     * This method finds the tool with the specified id from the list of tools.
     *
     * @param tools  - The list of Tool Maps
     * @param toolId - The tool ID to search for.
     * @return - The specified tool, or null if there is no matching tool.
     */
    protected Map<String, String> findTool(List<Map<String, String>> tools, String toolId) {
        Map<String, String> tool = null;
        for (Map<String, String> currentTool : tools) {
            if (currentTool.get("id").equals(toolId))
                tool = currentTool;
        }

        return tool;
    }
}
