/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.jacc15.fat;

/**
 * Note:
 * 1. User registry:
 * At this time, the test uses users, passwords, roles, and groups predefined in server.xml as
 * test user registry.
 *
 * TODO:  use different user registry
 *
 * 2. The constraints (which servlets can be accessed by which user/group/role) are defined in web.xml
 *
 * 3. Note on *Overlap* test:
 * When there are more than one constraints applied to the same servlet, the least constraint will win,
 * e.g.,
 *   <auth-constraint id="AuthConstraint_5">
 <role-name>Employee</role-name>
 </auth-constraint>

 and

 <security-constraint id="SecurityConstraint_5">
 <web-resource-collection id="WebResourceCollection_5">
 <web-resource-name>Protected with overlapping * and Employee roles</web-resource-name>
 <url-pattern>/OverlapNoConstraintServlet</url-pattern>
 <http-method>GET</http-method>
 <http-method>POST</http-method>
 </web-resource-collection>
 <auth-constraint id="AuthConstraint_5">
 <role-name>*</role-name>
 </auth-constraint>
 </security-constraint>

 servlet OverlapNoConstraintServlet will allow access to all roles since
 the role = * (any role) and role =  Employee are combined and * will win.

 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@Ignore("This is not a test")
@RunWith(FATRunner.class)
public class CommonTestHelper {

    private final Class<?> thisClass = CommonTestHelper.class;
    private final int GET_METHOD = 1;
    private final int POST_METHOD = 2;

    public CommonTestHelper() {}

    //----------------------------------
    // utility methods
    //----------------------------------

    /**
     * This is used for Basic Auth HttpGet with httpclient
     *
     * @throws IOException
     */
    HttpResponse executeGetRequestWithAuthCreds(String queryString, String username, String password) throws IOException {
        return executeGetRequestWithAuthCreds(queryString, username, password, null);
    }

    public HttpResponse executeGetRequestWithAuthCreds(String queryString, String username, String password, LibertyServer server) throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        if (queryString.contains("https")) {
            SSLHelper.establishSSLContext(client, server.getHttpDefaultSecurePort(), server);
        }
        if (username != null) {
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                                                           new UsernamePasswordCredentials(username, password));
        }
        HttpGet getMethod = new HttpGet(queryString);
        HttpResponse response = client.execute(getMethod);

        return response;
    }

    /**
     * This is used for Basic Auth HttpGet with new httpclient, with cookie
     */
    public HttpResponse executeGetRequestWithAuthCredsWithCookie(String queryString, String ssoCookie) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet getMethod = new HttpGet(queryString);
        getMethod.setHeader("Cookie", "LtpaToken2=" + ssoCookie);
        HttpResponse response = client.execute(getMethod);
        return response;
    }

    /**
     * This is used for Basic Auth to handle exceptions with httpclient
     */
    public void handleException(Exception e, String methodName, String debugData) {
        Log.info(thisClass, methodName, "caught unexpected exception for: " + debugData + e.getMessage());
        fail("Caught unexpected exception: " + e);
    }

    public void handleException(Exception e, String methodName, String debugData, String debugInfo) {
        Log.info(thisClass, methodName, debugInfo + debugData + e);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        Log.info(thisClass, methodName, "\n" + e.getMessage() + "\n" + sw.toString());
        fail("Caught unexpected exception: " + e);
    }

    /**
     * This method sets the server.xml for the running server.
     *
     * @param server
     * @param serverXML
     * @param waitForApplicationUpdateMessage set this to true when you are adding/removing the appSecurity feature, since this triggers an app restart
     */
    public void setServerConfiguration(LibertyServer server, String serverXML, boolean waitForApplicationUpdateMessage, boolean waitForPropertyUpdateMessage) throws Exception {
        Log.info(thisClass, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("/" + serverXML);
        if (waitForApplicationUpdateMessage) {
            assertNotNull("The application should have been updated", server.waitForStringInLogUsingMark("CWWKZ0003I"));
        }

        if (waitForPropertyUpdateMessage) {
            Log.info(thisClass, "setServerConfiguration", "waitForStringInLogUsingMark: The web application security settings have changed.");
            server.waitForStringInLogUsingMark("CWWKS9112A");
        }

        Log.info(thisClass, "setServerConfiguration", "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
        server.waitForStringInLogUsingMark("CWWKG0017I");
    }

    /**
     * @param getMethod
     * @param cookieName
     * @return
     */
    public String getCookieValue(HttpMessage httpMessage, String cookieName) {
        String ssoCookie = null;
        Header[] setCookieHeaders = httpMessage.getHeaders("Set-Cookie");
        if (setCookieHeaders == null) {
            Log.info(thisClass, "getCookieValue", "setCookieHeaders was null and should not be");
        }
        for (Header header : setCookieHeaders) {
            Log.info(thisClass, "getCookieValue", "header: " + header);
            for (HeaderElement e : header.getElements()) {
                if (e.getName().equals(cookieName)) {
                    ssoCookie = e.getValue();
                    break;
                }
            }
        }

        Log.info(thisClass, "getCookieValue", cookieName + ": " + ssoCookie);
        return ssoCookie;
    }

    /**
     * @param cookieHeader
     * @param cookieName
     * @return
     */
    public String getCookieValue(Header cookieHeader, String cookieName) {
        String ltpaCookie = null;
        if (cookieHeader != null) {
            Log.info(thisClass, "getCookieValue", "cookieHeader: " + cookieHeader.toString());
            int startIndex = cookieHeader.toString().indexOf(cookieName + "=", 0);
            if (startIndex != -1) {
                int endIndex = cookieHeader.toString().indexOf(";", startIndex);
                if (endIndex == -1) {
                    endIndex = cookieHeader.toString().length();
                }
                ltpaCookie = cookieHeader.toString().substring(startIndex + cookieName.length() + 1, endIndex);
            }
        }
        Log.info(thisClass, "getCookieValue", cookieName + ": " + ltpaCookie);
        return ltpaCookie;
    }

    public void verifyNullValuesAfterLogout(String user, String test2, boolean inMgrRole, boolean inEmpRole) {
        // Verify programmatic APIs return null
        assertTrue("getAuthType not null after logout: " + user, test2.contains("getAuthType: null"));
        assertTrue("getRemoteUser not null after logout: " + user, test2.contains("getRemoteUser: null"));
        assertTrue("getUserPrincipal not null after logout: " + user, test2.contains("getUserPrincipal: null"));
        assertTrue("callerSubject not null after logout: " + user, test2.contains("callerSubject: null"));
        assertTrue("callerCredential not null after logout: " + user, test2.contains("callerCredential: null"));
        assertTrue("isUserInRole(Employee) not false after logout: " + inEmpRole, test2.contains("isUserInRole(Employee): false"));
        assertTrue("isUserInRole(Manager) not false after logout: " + inMgrRole, test2.contains("isUserInRole(Manager): false"));
    }

    public void verifyNullValuesAfterLogout(String user, String test2) {
        // Verify programmatic APIs return null
        assertTrue("getAuthType not null after logout: " + user, test2.contains("getAuthType: null"));
        assertTrue("getRemoteUser not null after logout: " + user, test2.contains("getRemoteUser: null"));
        assertTrue("getUserPrincipal not null after logout: " + user, test2.contains("getUserPrincipal: null"));
        assertTrue("callerSubject not null after logout: " + user, test2.contains("callerSubject: null"));
    }

    public void verifyProgrammaticAPIValues(String loginUser, String test3, String authType) {
        // Verify programmatic APIs
        assertTrue("Failed to find expected getAuthType: " + loginUser, test3.contains("getAuthType: " + authType));
        assertTrue("Failed to find expected getRemoteUser: " + loginUser, test3.contains("getRemoteUser: " + loginUser));
        assertTrue("Failed to find expected getUserPrincipal: " + loginUser, test3.contains("getUserPrincipal: " + "WSPrincipal:" + loginUser));
    }

    public void verifyProgrammaticAPIValues(String authType, String loginUser, String test3, boolean inMgrRole, boolean inEmpRole) {
        // Verify programmatic APIs
        assertTrue("Failed to find expected getAuthType: " + loginUser, test3.contains("getAuthType: " + authType));
        assertTrue("Failed to find expected getRemoteUser: " + loginUser, test3.contains("getRemoteUser: " + loginUser));
        assertTrue("Failed to find expected getUserPrincipal: " + loginUser, test3.contains("getUserPrincipal: " + "WSPrincipal:" + loginUser));
        assertTrue("Failed to find expected callerSubject: " + loginUser, test3.contains("callerSubject: Subject:"));
        assertTrue("Failed to find expected callerCredential: " + loginUser,
                   test3.contains("callerCredential: com.ibm.ws.security.credentials.wscred.WSCredentialImpl"));
        assertTrue("Failed to find expected isUserInRole(Employee): " + inEmpRole, test3.contains("isUserInRole(Employee): " + inEmpRole));
        assertTrue("Failed to find expected isUserInRole(Manager): " + inMgrRole, test3.contains("isUserInRole(Manager): " + inMgrRole));
    }

    public void verifyProgrammaticAPIValues(String response, String expectedAuthType, String expectedRemoteUser, String expectedUserPrincipal, String expectedIsUserInEmployeeRole,
                                            String expectedIsUserInManagerRole, String expectedIsUserInSpecifiedRole) {
        // Verify programmatic APIs
        assertTrue("Failed to find expected authType: " + expectedRemoteUser, response.contains(expectedAuthType));
        assertTrue("Failed to find expected remote user: " + expectedRemoteUser, response.contains(expectedRemoteUser));
        assertTrue("Failed to find expected user principal: " + expectedUserPrincipal, response.contains(expectedUserPrincipal));
        assertTrue("Failed to find expected employee role " + expectedIsUserInEmployeeRole, response.contains(expectedIsUserInEmployeeRole));
        assertTrue("Failed to find expected manager role " + expectedIsUserInManagerRole, response.contains(expectedIsUserInManagerRole));
        assertTrue("Failed to find expected specified role " + expectedIsUserInSpecifiedRole, response.contains(expectedIsUserInSpecifiedRole));
    }

    public void verifyAuthenticationAlreadyHappenedException(String response) {
        assertTrue("The authentication is expected to already had happened.",
                   response.contains("ServletException: Authentication had been already established"));
    }

    /**
     * Access unprotected servlet for POST
     */
    public void accessPostUnprotectedServlet(String url) throws Exception {
        String methodName = "accessPostUnprotectedServlet";
        Log.info(thisClass, methodName, "url = " + url);

        String response = access(POST_METHOD, url, 200);

        // validate the request went to the correct servlet
        assertTrue("Failed to connect to servlet.", response.contains(url));
    }

    /**
     * Access unprotected servlet for GET
     */
    public String accessGetUnprotectedServlet(String url) throws Exception {
        String methodName = "accessGetUnprotectedServlet";
        Log.info(thisClass, methodName, "url = " + url);

        String response = access(GET_METHOD, url, 200);

        assertTrue("Failed to connect to servlet.", response.contains(url));
        return response;
    }

    /**
     * Access protected servlet for GET with invalid credentials
     */
    public void accessGetProtectedServletWithInvalidCredentials(String url, String user, String password, String secureUrl, LibertyServer server) throws Exception {
        String methodName = "accessGetProtectedServletWithInvalidCredentials";
        Log.info(thisClass, methodName, "url = " + url + ", user = " + user + ", password = " + password);

        accessAndAuthenticate(GET_METHOD, url, user, password, secureUrl, 403, server);
        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Access protected servlet for GET with authorized credentials
     */
    public String accessGetProtectedServletWithAuthorizedCredentials(String url, String user, String password, String secureUrl, LibertyServer server) throws Exception {
        String methodName = "accessGetProtectedServletWithAuthorizedCredentials";
        Log.info(thisClass, methodName, "url = " + url + ", user = " + user + ", password = " + password);

        String response = accessAndAuthenticate(GET_METHOD, url, user, password, secureUrl, 200, server);

        // validate the request went to the correct servlet
        assertTrue("Failed to connect to expected servlet.", response.contains(url));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
        return response;
    }

    /**
     * Access protected servlet for POST with authorized credentials
     */
    public String accessPostProtectedServletWithAuthorizedCredentials(String url, String user, String password, String secureUrl, LibertyServer server) throws Exception {
        String methodName = "accessPostProtectedServletWithAuthorizedCredentials";
        Log.info(thisClass, methodName, "url = " + url + ", user = " + user + ", password = " + password);

        String response = accessAndAuthenticate(POST_METHOD, url, user, password, secureUrl, 200, server);

        // validate the request went to the correct servlet
        assertTrue("Failed to connect to url.", response.contains(url));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
        return response;
    }

    /**
     * Access protected servlet for POST with invalid credentials
     */
    public void accessPostProtectedServletWithInvalidCredentials(String url, String user, String password, String secureUrl, LibertyServer server) throws Exception {
        String methodName = "accessPostProtectedServletWithInvalidCredentials";
        Log.info(thisClass, methodName, "url = " + url + ", user = " + user + ", password = " + password);

        accessAndAuthenticate(POST_METHOD, url, user, password, secureUrl, 403, server);
        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Access protected servlet
     */
    private String accessAndAuthenticate(int accessType, String url, String user, String password, String secureUrl, int expectedStatusCode,
                                         LibertyServer server) throws Exception {
        String methodName = "accessAndAuthenticate";
        Log.info(thisClass, methodName, "url = " + url + ", user = " + user + ", password = " + password);

        String responseContent = null;

        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                                                           new UsernamePasswordCredentials(user, password));
        String queryString = url;

        if (secureUrl != null) {
            // Setup for SSL for HTTP port (not HTTPS port)
            SSLHelper.establishSSLContext(httpClient, server.getHttpDefaultPort(), null);
            queryString = secureUrl;
        }

        try {
            HttpResponse response = null;
            if (accessType == POST_METHOD) {
                // use POST method to access servlet
                HttpPost postMethod = new HttpPost(queryString);
                response = httpClient.execute(postMethod);
            } else {
                // use GET method to access servlet
                HttpGet getMethod = new HttpGet(queryString);
                response = httpClient.execute(getMethod);
            }
            assertEquals("Expected " + expectedStatusCode + " was not returned",
                         expectedStatusCode, response.getStatusLine().getStatusCode());
            Log.info(thisClass, methodName, "response status: " + response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            responseContent = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "response: " + responseContent);
            EntityUtils.consume(entity);

        } catch (Exception e) {
            Log.info(thisClass, methodName, "Caught unexpected exception: " + e);
        }

        Log.info(thisClass, methodName, "Exiting test " + methodName);
        return responseContent;
    }

    /**
     * Access unprotected servlet
     */
    private String access(int accessType, String url, int expectedStatusCode) {
        String methodName = "access";
        Log.info(thisClass, methodName, "expectedStatusCode = " + expectedStatusCode);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        String responseContent = null;

        try {
            HttpResponse response = null;
            if (accessType == POST_METHOD) {
                // use POST method to access servlet
                HttpPost postMethod = new HttpPost(url);
                response = httpClient.execute(postMethod);
            } else {
                // use GET method to access servlet
                HttpGet getMethod = new HttpGet(url);
                response = httpClient.execute(getMethod);
            }
            assertEquals("Expected " + expectedStatusCode + " was not returned",
                         expectedStatusCode, response.getStatusLine().getStatusCode());
            Log.info(thisClass, methodName, "response status: " + response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            responseContent = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "response: " + responseContent);
            EntityUtils.consume(entity);
        } catch (Exception e) {
            Log.info(thisClass, methodName, "Caught unexpected exception: " + e);
        }

        Log.info(thisClass, methodName, "Exiting test " + methodName);
        return responseContent;
    }

    /**
     * Redirect to https port for GET
     */
    public void testSSLRedirectGet(String url, String user, String password, String secureUrl, LibertyServer server) throws Exception {
        String methodName = "testSSLRedirectGet";
        Log.info(thisClass, methodName, "url = " + url + ", user = " + user + ", password = " + password);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                                                           new UsernamePasswordCredentials(user, password));

        // Setup for SSL for HTTP port (not HTTPS port)
        SSLHelper.establishSSLContext(httpClient, server.getHttpDefaultPort(), null);

        try {
            // access via http to validate redirect to https
            Log.info(thisClass, methodName, "url = " + url);
            sendRequestAndValidate(httpClient, url);

            // access via https directly using SSL url
            Log.info(thisClass, methodName, "Access https port directly, secureUrl: " + secureUrl);
            sendRequestAndValidate(httpClient, secureUrl);

        } catch (Exception e) {
            Log.info(thisClass, methodName, "Exception: " + e.getMessage());
        }

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Redirect to https port for POST
     */
    public void testSSLRedirectPost(String url, String user, String password, int expectedStatusCode, String secureUrl, LibertyServer server) throws Exception {
        String methodName = "testSSLRedirectPost";
        Log.info(thisClass, methodName, "url = " + url + ", user = " + user + ", password = " + password);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                                                           new UsernamePasswordCredentials(user, password));

        // Setup for SSL for HTTP port (not HTTPS port)
        SSLHelper.establishSSLContext(httpClient, server.getHttpDefaultPort(), null);

        try {

            // use Post method to access servlet
            HttpPost postMethod = new HttpPost(url);
            HttpResponse response = httpClient.execute(postMethod);

            assertEquals("Expecting post getStatusCode 302", 302,
                         response.getStatusLine().getStatusCode());

            Log.info(thisClass, methodName, "response status: " + response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            String content = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "response: " + content);
            EntityUtils.consume(entity);

            // redirect location from login JSP
            Header header = response.getFirstHeader("Location");
            String location = header.getValue();

            Log.info(thisClass, methodName, "Redirect location: " + location);
            assertTrue("Failed to redirect to https site.", location.contains("https:"));

            if (expectedStatusCode == 200) {
                sendRequestAndValidate(httpClient, location);

                // access via https directly using sslQueryString to verify that it works
                Log.info(thisClass, methodName, "Access https port directly, secureUrl = " + secureUrl);
                sendRequestAndValidate(httpClient, secureUrl);
            } else {
                //Verify that expected status code is received from request
                sendRequest(httpClient, location, expectedStatusCode);
            }

        } catch (Exception e) {
            Log.info(thisClass, methodName, "Caught unexpected exception: " + e);
        }

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * @param httpClient
     * @param queryString
     * @throws IOException
     * @throws ClientProtocolException
     */
    private void sendRequestAndValidate(DefaultHttpClient httpClient, String queryString) throws IOException, ClientProtocolException {
        String methodName = "sendRequestAndValidate";
        HttpResponse response;
        String content;

        HttpGet getMethod;
        getMethod = new HttpGet(queryString);
        response = httpClient.execute(getMethod);

        Log.info(thisClass, methodName, "response status: " + response.getStatusLine().toString());

        content = EntityUtils.toString(response.getEntity());
        Log.info(thisClass, methodName, "Servlet content: " + content);
        EntityUtils.consume(response.getEntity());

        // validate the request was redirected to https URL
        assertTrue("Failed to redirect to https site.", content.contains("getRequestURL: https"));
    }

    /**
     * @param httpClient
     * @param queryString
     * @throws IOException
     * @throws ClientProtocolException
     */
    private void sendRequest(DefaultHttpClient httpClient, String queryString, int expectedStatusCode) throws IOException, ClientProtocolException {
        String methodName = "sendRequest";
        HttpResponse response;

        HttpGet getMethod;
        getMethod = new HttpGet(queryString);
        response = httpClient.execute(getMethod);

        int getResponseStatusCode = response.getStatusLine().getStatusCode();
        Log.info(thisClass, methodName, "Get getResponseStatusCode: " + getResponseStatusCode);
        assertTrue("Expecting " + expectedStatusCode + ", got: " + getResponseStatusCode,
                   expectedStatusCode == getResponseStatusCode);
    }

    /**
     * Helper method to process custom methods
     */
    public String processDoCustom(String url, boolean secure, int port, String user, String password, LibertyServer server) {
        String methodName = "processDoCustom";
        Log.info(thisClass, methodName, "Entering method " + methodName);
        String httpMethod = "CUSTOM";
        String response = null;
        String trustStoreFile = null;
        String trustStorePwd = null;

        if (url.contains("https")) {
            trustStoreFile = server.getServerRoot() + "/resources/security/key.jks";
            trustStorePwd = "Liberty";
        }

        try {
            response = httpCustomMethodResponse(url, httpMethod, secure, port, user, password, trustStoreFile, trustStorePwd);
            Log.info(thisClass, methodName, "response: " + response);
        } catch (Exception e) {
            e.printStackTrace();
            fail("--debug Test failed to access the URL " + url);
        }
        Log.info(thisClass, methodName, "Exiting method " + methodName);
        return response;
    }

    /*
     * Helper method to make custom method call
     */
    public String httpCustomMethodResponse(String urlLink, String httpMethod, boolean secure, int port, String user, String password) {
        return httpCustomMethodResponse(urlLink, httpMethod, secure, port, user, password, null, null);
    }

    /*
     * Helper method to make custom method call
     */
    public String httpCustomMethodResponse(String urlLink, String httpMethod, boolean secure, int port, String user, String password, String trustStore, String trustStorePwd) {
        String methodName = "httpCustomMethodResponse";
        Log.info(thisClass, methodName, "urlLink = " + urlLink + ", user = " + user + ", password = " + password);
        String responseString = null;

        URL url = null;
        try {
            url = new URL(urlLink);
        } catch (MalformedURLException e) {
            return "Invalid URL " + urlLink;
        }

        try {
            Socket socket = null;

            if (urlLink.indexOf("https") > -1) {
                if (trustStore != null && trustStorePwd != null) {
                    // get a socket for the specified truststore
                    socket = SSLHelper.getSocketForURL(url, trustStore, trustStorePwd, trustStore, trustStorePwd, null);
                } else {
                    // get a socket for the process default
                    SocketFactory socketFactory = SSLSocketFactory.getDefault();
                    socket = socketFactory.createSocket(url.getHost(), url.getPort());
                }
            } else {
                socket = new Socket(url.getHost(), url.getPort());
            }
            Log.info(thisClass, "--debug httpCustomMethodResponse", "url.getHost()=" + url.getHost() + ",url.getPort()=" + url.getPort() + ",url.getPath()=" + url.getPath());

            // Send header
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
            wr.write(httpMethod + " " + url.getPath() + " HTTP/1.0\r\n");

            if (secure) {
                byte[] encoding = Base64.encodeBase64((user + ":" + password).getBytes());

                String encodedStr = new String(encoding);
                wr.write("Authorization: Basic " + encodedStr + "\r\n");
            }
            wr.write("\r\n");
            wr.flush();

            // Get response
            Log.info(thisClass, "--debug httpCustomMethodResponse", ", getting response");

            BufferedReader d = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuffer responseString1 = new StringBuffer();
            String line1 = null;
            try {
                while ((line1 = d.readLine()) != null) {
                    if (!(line1.isEmpty())) {
                        line1.concat(line1.trim());
                        responseString1.append(line1);
                        responseString1.append("\n");
                    }
                }
            } catch (IOException e) {
                responseString = "Failed to access the URL " + urlLink + " with message " + e.getMessage();
            }
            responseString = responseString1.toString();
        } catch (Exception e) {
            Log.info(thisClass, "--debug failed with", e.getMessage());
            responseString = "Failed with " + e.getMessage();
        }
        return responseString;

    }

    /**
     * This is used for Basic Auth Http<Get, Post, Put, Delete, Head, Options, Trace with httpclient
     *
     * @param String
     * @param String
     * @param String
     * @param LibertyServer
     * @param boolean
     * @throws IOException
     */
    public HttpResponse executeHttpMethodRequestWithAuthCreds(String method, String queryString, String username, String password, LibertyServer server,
                                                              boolean sslSetup) throws IOException {

        DefaultHttpClient client = new DefaultHttpClient();
        Log.info(thisClass, "executeHttpMethodRequestWithAuthCreds", "Servlet Url: " + queryString);
        Log.info(thisClass, "executeHttpMethodRequestWithAuthCreds", "Requested Method: " + method);

        if (username != null) {
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                                                           new UsernamePasswordCredentials(username, password));

        }

        // sometimes we need to do the SSL setup for the non-SSL port
        // (ie: in the case of doGet which automatically redirects
        if (sslSetup) {
            int sslPort = server.getHttpDefaultSecurePort();
            int nonSSLPort = server.getHttpDefaultPort();
            int useThisPort = 0;

            if (queryString.contains(Integer.toString(nonSSLPort))) {
                useThisPort = nonSSLPort;
            } else {
                useThisPort = sslPort;
            }
            //Log.info(thisClass, "executeHttpMethodRequestWithAuthCreds", "SSLHelper " + useThisPort);
            SSLHelper.establishSSLContext(client, useThisPort, server);
            Log.info(thisClass, "executeHttpMethodRequestWithAuthCreds", "SSLHelper:  " + useThisPort);
        }

        Log.info(thisClass, "executeHttpMethodRequestWithAuthCreds", "auth " + username);
        HttpUriRequest methodToExecute = null;

        if (method.equals("doGet")) {
            methodToExecute = new HttpGet(queryString);
        } else if (method.equals("doPut")) {
            methodToExecute = new HttpPut(queryString);
        } else if (method.equals("doPost")) {
            methodToExecute = new HttpPost(queryString);
        } else if (method.equals("doHead")) {
            methodToExecute = new HttpHead(queryString);
        } else if (method.equals("doDelete")) {
            methodToExecute = new HttpDelete(queryString);
        } else if (method.equals("doTrace")) {
            methodToExecute = new HttpTrace(queryString);
        } else if (method.equals("doOptions")) {
            methodToExecute = new HttpOptions(queryString);
            // SSL setup on custom is a little differently - handle separately
            //} else if (method.equals("doCustom")) {
            //    methodToExecute = new httpCustomMethodResponse(queryString, method, true, username, password);
        } else {
            // default is doGet
            methodToExecute = new HttpGet(queryString);
        }

        HttpResponse response = client.execute(methodToExecute);
        //Log.info(thisClass, "executeHttpMethodRequestWithAuthCreds", "After Execute");

        return response;
    }

    /**
     * Wrapper used to setup SSL before running the Custom method.
     *
     * @param String
     * @param String
     * @param boolean
     * @param int
     * @param String
     * @param String
     * @param LibertyServer
     * @throws IOException
     */
    public String httpCustomMethodResponse(String urlLink, String httpMethod, boolean secure, int port, String user, String password, LibertyServer server) {

        String trustStoreFile = null;
        String trustStorePwd = null;
        Log.info(thisClass, "httpCustomMethodResponse", urlLink);
        if (urlLink.indexOf("https") > -1) {
            //* String trustStoreFile = server.getServerSharedPath() + "resources/security/DummyServerKeyFile.jks";
            trustStoreFile = server.getServerRoot() + "/resources/security/key.jks";
            trustStorePwd = "Liberty";
        }
        return httpCustomMethodResponse(urlLink, httpMethod, secure, port, user, password, trustStoreFile, trustStorePwd);
    }

}