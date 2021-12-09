/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat_helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

public class JavaEESecTestBase {

    // Values to be set by the child class
    protected LibertyServer server;
    protected static Class<?> logClass;
    protected static String serverConfigurationFile = "server.xml";

    protected JavaEESecTestBase(LibertyServer server, Class<?> logClass) {
        this.server = server;
        this.logClass = logClass;
    }

    protected String getCurrentTestName() {
        return "Test name not set";
    }

    protected String executeGetRequestBasicAuthCredsPreemptive(DefaultHttpClient httpClient, String url, String userid, String password, int expectedStatusCode) throws Exception {
        String methodName = "executeGetRequestBasicAuthCredsPreemptive";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + url + " userid: " + userid + ", password: " + password + ", expectedStatusCode=" + expectedStatusCode
                                                 + " , method=" + methodName);

        HttpGet getMethod = new HttpGet(url);

        if (userid != null) {
            getMethod.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((userid + ":" + password).getBytes("UTF8")));
        }
        HttpResponse response = httpClient.execute(getMethod);
        Log.info(logClass, methodName, "Actual response: " + response.toString());
        return processResponse(response, expectedStatusCode);
    }

    protected String executeGetRequestBasicAuthCreds(DefaultHttpClient httpClient, String url, String userid, String password, int expectedStatusCode) throws Exception {
        String methodName = "executeGetRequestBasicAuthCreds";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + url + " userid: " + userid + ", password: " + password + ", expectedStatusCode=" + expectedStatusCode
                                                 + " , method=" + methodName);

        HttpResponse response = executeGetRequestBasicAuthCreds(httpClient, url, userid, password);
        return processResponse(response, expectedStatusCode);
    }

    protected HttpResponse executeGetRequestBasicAuthCreds(DefaultHttpClient httpClient, String url, String userid, String password) throws Exception {
        String methodName = "executeGetRequestBasicAuthCreds";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + url + " userid: " + userid + ", password: " + password + " , method=" + methodName);
        HttpGet getMethod = new HttpGet(url);
        if (userid != null) {
            httpClient.getCredentialsProvider().clear();
            httpClient.getCredentialsProvider().setCredentials(new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                                                               new UsernamePasswordCredentials(userid, password));
        }
        HttpResponse response = httpClient.execute(getMethod);
        Log.info(logClass, methodName, "Actual response: " + response.toString());

        return response;
    }

    protected String executeGetRequestNoAuthCreds(DefaultHttpClient httpClient, String url, int expectedStatusCode) throws Exception {
        return executeGetRequestBasicAuthCreds(httpClient, url, null, null, expectedStatusCode);
    }

    protected HttpResponse executeGetRequestNoAuthCreds(DefaultHttpClient httpClient, String url) throws Exception {
        return executeGetRequestBasicAuthCreds(httpClient, url, null, null);
    }

    /**
     * Process the response from an http invocation, such as validating
     * the status code, extracting the response entity...
     *
     * @param response the HttpResponse
     * @param expectedStatusCode
     * @return The response entity text, or null if request failed
     * @throws IOException
     */
    protected String processResponse(HttpResponse response,
                                     int expectedStatusCode) throws IOException {
        String methodName = "processResponse";

        Log.info(logClass, methodName, "getMethod status: " + response.getStatusLine());
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity);
        Log.info(logClass, methodName, "Servlet full response content: \n" + content);
        EntityUtils.consume(entity);

        assertEquals("Expected " + expectedStatusCode + " was not returned",
                     expectedStatusCode, response.getStatusLine().getStatusCode());

        return content;
    }

    protected Header getCookieHeader(HttpResponse response, String cookieName) {
        String methodName = "getCookie";
        Log.info(logClass, methodName, response.toString() + ", cookieName=" + cookieName);
        Header[] setCookieHeaders = response.getHeaders("Set-Cookie");
        if (setCookieHeaders == null) {
            fail("There must be Set-Cookie headers.");
        }
        for (Header header : setCookieHeaders) {
            Log.info(logClass, methodName, "Header: " + header);
            for (HeaderElement e : header.getElements()) {
                if (e.getName().equals(cookieName)) {
                    return header;
                }
            }
        }
        fail("Set-Cookie for " + cookieName + " not found.");
        return null;
    }

    protected String getCookieValue(Header header, String cookieName) {
        String methodName = "getCookieValue";
        Log.info(logClass, methodName, "header: " + header + ", cookieName=" + cookieName);
        for (HeaderElement e : header.getElements()) {
            Log.info(logClass, methodName, "HeaderElement: " + e);
            if (e.getName().equals(cookieName)) {
                return e.getValue();
            }
        }
        return null;
    }

    protected String accessWithCookie(DefaultHttpClient httpClient, String url, String cookieName, String cookie, int expectedStatusCode) {
        Log.info(logClass, getCurrentTestName(), "accessWithCookie: url=" + url + ", cookie=" + cookie +
                                                 ", expectedStatusCode=" + expectedStatusCode);
        try {
            HttpGet getMethod = new HttpGet(url);
            getMethod.setHeader("Cookie", cookieName + "=" + cookie);
            HttpResponse response = httpClient.execute(getMethod);
            return processResponse(response, expectedStatusCode);
        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
            return null;
        }
    }

    public void validateNoCookie(HttpMessage httpMessage, String cookieName) {
        Log.info(logClass, getCurrentTestName(), "validateNoSSOCookie: httpMessage=" + httpMessage + ", cookieName=" + cookieName);
        Header[] setCookieHeaders = httpMessage.getHeaders("Set-Cookie");
        if (setCookieHeaders != null) {
            for (Header header : setCookieHeaders) {
                Log.info(logClass, "validateNoCookie", "header: " + header);
                for (HeaderElement e : header.getElements()) {
                    if (e.getName().equals(cookieName)) {
                        fail("There must not be a cookie for " + cookieName);
                    }
                }
            }
        }
    }

    protected String accessWithCustomHeader(DefaultHttpClient httpClient, String url, String name, String value, int expectedStatusCode) {
        Log.info(logClass, getCurrentTestName(), "accessWithCustomHeader: url=" + url + ", name=" + name + ", value=" + value +
                                                 ", expectedStatusCode=" + expectedStatusCode);
        try {
            HttpGet getMethod = new HttpGet(url);
            if (name != null && value != null) {
                getMethod.setHeader(name, value);
            }
            HttpResponse response = httpClient.execute(getMethod);
            return processResponse(response, expectedStatusCode);
        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
            return null;
        }
    }

    protected String executeGetRequestFormCreds(DefaultHttpClient httpClient, String resourceUrl, boolean redirect, String loginUrl, String loginTitle, String formUrl,
                                                String userid,
                                                String password, int expectedStatusCode) throws Exception {
        String methodName = "executeGetRequestFormCreds";
        Log.info(logClass, getCurrentTestName(),
                 "Servlet url: " + resourceUrl + " userid: " + userid + ", password: " + password + ", expectedStatusCode=" + expectedStatusCode + " , method=" + methodName);

        HttpResponse response = executeGetRequestFormCreds(httpClient, resourceUrl, redirect, loginUrl, loginTitle, formUrl, userid, password);
        return processResponse(response, expectedStatusCode, resourceUrl);
    }

    protected HttpResponse executeGetRequestFormCreds(DefaultHttpClient httpClient, String resourceUrl, boolean redirect, String loginUrl, String loginTitle, String formUrl,
                                                      String userid,
                                                      String password) throws Exception {
        String methodName = "executeGetRequestFormCreds";
        Log.info(logClass, getCurrentTestName(), "Servlet url: " + resourceUrl + " userid: " + userid + ", password: " + password + " , method=" + methodName);

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String content = getFormLoginPage(httpClient, resourceUrl, redirect, loginUrl, loginTitle);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpClient, formUrl, userid, password, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        return accessPageUsingGet(httpClient, location);
    }

    protected HttpResponse accessPageUsingGet(HttpClient client, String location) {
        String methodName = "accessPageUsingGet";
        Log.info(logClass, methodName, "accessPageUsingGet: location =  " + location);
        return accessPage(client, new HttpGet(location));
    }

    protected HttpResponse accessPageUsingPost(HttpClient client, String location, List<NameValuePair> params) throws Exception {
        String methodName = "accessPageUsingPost";
        Log.info(logClass, methodName, "accessPageUsingPost: location =  " + location);
        HttpPost postMethod = new HttpPost(location);
        postMethod.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
        return accessPage(client, postMethod);
    }

    protected HttpResponse accessPage(HttpClient client, HttpUriRequest request) {
        String methodName = "accessPage";
        Log.info(logClass, methodName, "accessPage: HttpUriRequest =  " + request);
        HttpResponse response = null;

        try {
            response = client.execute(request);
        } catch (IOException e) {
            fail("Caught unexpected exception: " + e);
        }
        return response;
    }

    protected String processResponse(HttpResponse response, int expectedStatusCode, String message) throws IOException {
        String methodName = "processResponse";

        Log.info(logClass, methodName, "getMethod status: " + response.getStatusLine());
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity);
        Log.info(logClass, methodName, "Servlet full response content: \n" + content);
        EntityUtils.consume(entity);

        assertEquals("Expected " + expectedStatusCode + " was not returned",
                     expectedStatusCode, response.getStatusLine().getStatusCode());

        if (response.getStatusLine().getStatusCode() == 200) {
            assertTrue("Response did not contain expected content (" + message + ")",
                       content.contains(message));
            return content;
        } else if (expectedStatusCode == 401) {
            assertTrue("Response was not the expected error page: "
                       + Constants.LOGIN_ERROR_PAGE, content.contains(Constants.LOGIN_ERROR_PAGE));
            return null;
        } else {
            return null;
        }
    }

    /**
     * Send HttpClient get request to the given URL, ensure that the user is redirected to the form login page
     * and that the JASPI provider was or was not called, as expected.
     *
     * @param httpclient HttpClient object to execute request
     * @param url URL for request, should be protected and redirect to form login page
     * @param providerName Name of JASPI provider that should authenticate the request, null if JASPI not enabled for request
     * @param formTitle Name of Login form (defaults to Form Login Page if not specified)
     * @throws Exception
     */
    public void getFormLoginPage(HttpClient httpclient, String url, String providerName) throws Exception {
        String formTitle = Constants.FORM_LOGIN_PAGE;
        getFormLoginPage(httpclient, url, providerName, formTitle);
    }

    public void getFormLoginPage(HttpClient httpclient, String url, String providerName, String formTitle) throws Exception {
        String methodName = "getFormLoginPage";
        Log.info(logClass, methodName, "Form login page url: " + url);

        try {
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = httpclient.execute(getMethod);
            Log.info(logClass, methodName, "Form login page result: " + response.getStatusLine());

            assertEquals("Expected " + HttpServletResponse.SC_OK + " status code for form login page was not returned",
                         HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());

            String content = EntityUtils.toString(response.getEntity());

            Log.info(logClass, methodName, "Form login page content: " + content);
            EntityUtils.consume(response.getEntity());

            if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
                // Verify we get the form login JSP
                assertTrue("Did not find expected form login page: " + formTitle,
                           content.contains(formTitle));
                Log.info(logClass, methodName, "Found expected Form login page title: " + formTitle);
            }

        } catch (IOException e) {
            fail("Caught unexpected exception: " + e);
        }
    }

    /**
     * Send HttpClient get request to the given URL, ensure that the user is redirected or forwarded to the form login page
     * Note that in order to use this method properly, HttlClient needs to be set ClientPNames.HANDLE_REDIRECTS=Boolean.FALSE.
     * This propety let httpclient disable following the redirect automatically.
     *
     * @param httpclient HttpClient object to execute request
     * @param url URL for request, should be protected and redirect to form login page
     * @param redirect true if redirect is used to go to the login page, otherwise, use forward.
     * @param formUrl Url of login page. this value is used when redirect is set as true.
     * @param formTitle Name of Login form.
     * @throws Exception
     */

    public String getFormLoginPage(DefaultHttpClient httpclient, String url, boolean redirect, String formUrl, String formTitle) throws Exception {
        String methodName = "getFormLoginPage";
        Log.info(logClass, methodName, "Resource url: " + url + ", redirect: " + redirect);
        String content = null;
        try {
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = httpclient.execute(getMethod);
            Log.info(logClass, methodName, "Form login page result: " + response.getStatusLine());
            Log.info(logClass, methodName, "Form login page response: " + response.toString());
            if (redirect) {
                assertEquals("Expected " + HttpServletResponse.SC_MOVED_TEMPORARILY + " status code for form login page was not returned",
                             HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
                // check page url.
                String location = response.getFirstHeader("Location").getValue();
                assertTrue("Expected " + formUrl + " location for form login page was not returned", location.equals(formUrl));
                // now get the contents of the redirect url.
                EntityUtils.consume(response.getEntity());
                Log.info(logClass, methodName, "Form login page redirect location : " + location);
                getMethod = new HttpGet(location);
                response = httpclient.execute(getMethod);
            }
            assertEquals("Expected " + HttpServletResponse.SC_OK + " status code for form login page was not returned",
                         HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());

            content = EntityUtils.toString(response.getEntity());

            Log.info(logClass, methodName, "Form login page content: " + content);
            EntityUtils.consume(response.getEntity());

            if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
                // Verify we get the form login JSP
                assertTrue("Did not find expected form login page: " + formTitle,
                           content.contains(formTitle));
                Log.info(logClass, methodName, "Found expected Form login page title: " + formTitle);
            }

        } catch (IOException e) {
            fail("Caught unexpected exception: " + e);
        }
        return content;
    }

    /**
     * Send HttpClient post request with specified parameters to the given URL, ensure that the user is redirected or forwarded
     * to the form login page
     * Note that in order to use this method properly, HttlClient needs to be set ClientPNames.HANDLE_REDIRECTS=Boolean.FALSE.
     * This propety let httpclient disable following the redirect automatically.
     *
     * @param httpclient HttpClient object to execute request
     * @param url URL for request, should be protected and redirect to form login page
     * @param params post parameters.
     * @param redirect true if redirect is used to go to the login page, otherwise, use forward.
     * @param formUrl Url of login page. this value is used when redirect is set as true.
     * @param formTitle Name of Login form.
     * @throws Exception
     */

    public String postFormLoginPage(DefaultHttpClient httpclient, String url, List<NameValuePair> params, boolean redirect, String formUrl, String formTitle) throws Exception {
        String methodName = "postFormLoginPage";
        Log.info(logClass, methodName, "Form login page url: " + url + ", redirect: " + redirect);
        String content = null;
        try {
            HttpPost postMethod = new HttpPost(url);
            postMethod.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

            HttpResponse response = httpclient.execute(postMethod);
            Log.info(logClass, methodName, "Form login page result: " + response.getStatusLine());
            if (redirect) {
                assertEquals("Expected " + HttpServletResponse.SC_MOVED_TEMPORARILY + " status code for form login page was not returned",
                             HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
                // check page url.
                String location = response.getFirstHeader("Location").getValue();
                assertTrue("Expected " + formUrl + " location for form login page was not returned", location.equals(formUrl));
                // now get the contents of the redirect url.
                EntityUtils.consume(response.getEntity());
                Log.info(logClass, methodName, "Form login page redirect location : " + location);
                response = httpclient.execute(new HttpPost(location));
            }
            assertEquals("Expected " + HttpServletResponse.SC_OK + " status code for form login page was not returned",
                         HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());

            content = EntityUtils.toString(response.getEntity());

            Log.info(logClass, methodName, "Form login page content: " + content);
            EntityUtils.consume(response.getEntity());

            if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
                // Verify we get the form login JSP
                assertTrue("Did not find expected form login page: " + formTitle,
                           content.contains(formTitle));
                Log.info(logClass, methodName, "Found expected Form login page title: " + formTitle);
            }

        } catch (IOException e) {
            fail("Caught unexpected exception: " + e);
        }
        return content;
    }

    /**
     * Post HttpClient request to execute a form login on the given page, using the given username and password
     *
     * @param httpclient HttpClient object to execute login
     * @param url URL for login page
     * @param username User name
     * @param password User password
     * @return URL of page redirected to after the login
     * @throws Exception
     */

    public String executeFormLogin(HttpClient httpclient, String url, String username, String password, boolean redirect) throws Exception {
        return executeFormLogin(httpclient, url, username, password, redirect, null, null);
    }

    public String executeFormLogin(HttpClient httpclient, String url, String username, String password, boolean redirect, String description) throws Exception {
        return executeFormLogin(httpclient, url, username, password, redirect, description, null);
    }

    public String executeFormLogin(HttpClient httpclient, String url, String username, String password, boolean redirect, String description, String[] cookies) throws Exception {
        String methodName = "executeFormLogin";
        Log.info(logClass, methodName, "Submitting Login form (POST) =  " + url + " username =" + username + " password=" + password + " description=" + description);

        HttpPost postMethod = new HttpPost(url);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("j_username", username));
        nvps.add(new BasicNameValuePair("j_password", password));
        if (description != null)
            nvps.add(new BasicNameValuePair("j_description", description));

        postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpResponse response = httpclient.execute(postMethod);

        Log.info(logClass, methodName, "postMethod.getStatusCode():  " + response.getStatusLine().getStatusCode());
        Log.info(logClass, methodName, "postMethod response: " + response.toString());

        EntityUtils.consume(response.getEntity());

        String location = "No Redirect";
        if (redirect) {
            // Verify redirect to servlet
            int status = response.getStatusLine().getStatusCode();
            assertTrue("Form login did not result in redirect: " + status, status == HttpServletResponse.SC_MOVED_TEMPORARILY);
            Header header = response.getFirstHeader("Location");

            location = header.getValue();
            Log.info(logClass, methodName, "Redirect location:  " + location);
            Log.info(logClass, methodName, "Modified Redirect location:  " + location);
        } else {
            // Verify we got a 200 from the servlet
            int status = response.getStatusLine().getStatusCode();
            assertTrue("Form login did not result in redirect: " + status, status == HttpServletResponse.SC_OK);
        }
        if (cookies != null) {
            for (String cookie : cookies) {
                Header cookieHeader = getCookieHeader(response, cookie);
                assertCookie(cookieHeader.toString(), false, true);
            }
        }
        return location;
    }

    public String executeCustomFormLogin(HttpClient httpclient, String url, String username, String password, String viewState) throws Exception {
        return executeCustomFormLogin(httpclient, url, username, password, viewState, null);
    }

    public String executeCustomFormLogin(HttpClient httpclient, String url, String username, String password, String viewState, String[] cookies) throws Exception {
        String methodName = "executeCustomFormLogin";
        Log.info(logClass, methodName, "Submitting custom login form (POST) =  " + url + ", username = " + username + ", password = " + password + ", viewState = " + viewState);

        HttpPost postMethod = new HttpPost(url);
//        postMethod.setEntity(new StringEntity(loginData));

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("form:username", username));
        nvps.add(new BasicNameValuePair("form:password", password));
        nvps.add(new BasicNameValuePair("form:j_id_e", "Login"));
        nvps.add(new BasicNameValuePair("form_SUBMIT", "1"));
        if (viewState != null) {
            if (JakartaEE9Action.isActive()) {
                nvps.add(new BasicNameValuePair("jakarta.faces.ViewState", viewState));
            } else {
                nvps.add(new BasicNameValuePair("javax.faces.ViewState", viewState));
            }
        }
        postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpResponse response = httpclient.execute(postMethod);
        Log.info(logClass, methodName, "postMethod.getStatusCode():  " + response.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        Log.info(logClass, methodName, "contents : " + content);

        // Verify redirect to servlet
        int status = response.getStatusLine().getStatusCode();
        assertTrue("Form login did not result in redirect: " + status, status == HttpServletResponse.SC_MOVED_TEMPORARILY);

        Header header = response.getFirstHeader("Location");
        String location = header.getValue();
        Log.info(logClass, methodName, "Redirect location:  " + location);

        if (cookies != null) {
            for (String cookie : cookies) {
                Header cookieHeader = getCookieHeader(response, cookie);
                assertCookie(cookieHeader.toString(), false, true);
            }
        }
        return location;
    }

    /**
     *
     * @param client
     * @param location
     * @param expectedStatusCode
     * @return
     */
    protected String accessPageNoChallenge(HttpClient client, String location, int expectedStatusCode, String message) {
        return accessPageNoChallenge(client, location, expectedStatusCode, message, null);
    }

    protected String accessPageNoChallenge(HttpClient client, String location, int expectedStatusCode, String message, String[] cookies) {
        String methodName = "accessPageNoChallenge";
        Log.info(logClass, methodName, "accessPageNoChallenge: location =  " + location + " expectedStatusCode =" + expectedStatusCode);

        try {
            HttpResponse response;
            // Get method on form login page
            HttpGet getMethod = new HttpGet(location);
            response = client.execute(getMethod);
            Log.info(logClass, methodName, "getMethod status:  " + response.getStatusLine());

            assertEquals("Expected " + expectedStatusCode + " was not returned",
                         expectedStatusCode, response.getStatusLine().getStatusCode());

            String content = EntityUtils.toString(response.getEntity());
            Log.info(logClass, methodName, "Servlet full response content: \n" + content);

            EntityUtils.consume(response.getEntity());

            if (cookies != null) {
                for (String cookie : cookies) {
                    Header cookieHeader = getCookieHeader(response, cookie);
                    assertCookie(cookieHeader.toString(), false, true);
                }
            }

            // Paranoia check, make sure we hit the right servlet
            if (response.getStatusLine().getStatusCode() == 200) {
                assertTrue("Response did not contain expected content (" + message + ")",
                           content.contains(message));
                return content;
            } else if (expectedStatusCode == 401) {
                assertTrue("Response was not the expected error page: "
                           + Constants.LOGIN_ERROR_PAGE, content.contains(Constants.LOGIN_ERROR_PAGE));
                return null;
            } else if (expectedStatusCode == 302) {
                int status = response.getStatusLine().getStatusCode();
                assertTrue("Response was not the expected status code : " + status, status == 302);
                Header header = response.getFirstHeader("Location");
                String redirecturl = header.getValue();
                Log.info(logClass, methodName, "Redirect URL:  " + redirecturl);
                return redirecturl;
            } else {
                return null;
            }
        } catch (IOException e) {
            fail("Caught unexpected exception: " + e);
            return null;
        }
    }

    /**
     *
     * @param client
     * @param location
     * @param expectedStatusCode
     * @return
     */
    protected void accessPageExpectException(HttpClient client, String location) throws IOException, SSLPeerUnverifiedException {
        String methodName = "accessPageExpectException";
        Log.info(logClass, methodName, "accessPageExpectException: location =  " + location);
        HttpGet getMethod = new HttpGet(location);
        HttpResponse response = client.execute(getMethod);
        Log.info(logClass, methodName, "getMethod status:  " + response.getStatusLine());
    }

    public void mustContain(String response, String target) {
        assertTrue("Expected result " + target + " not found in response: " + response, response.contains(target));
    }

    private void mustNotContain(String response, String target) {
        assertFalse("Expected result " + target + " was found in response and should not have been found.", response.contains(target));
    }

    private void mustMatch(String response, String target) {
        assertTrue("Expected result " + target + " not found in response", response.matches(target));
    }

    private void mustNotMatch(String response, String target) {
        assertFalse("Expected result " + target + " found in response", response.matches(target));
    }

    protected void verifyAuthenticatedResponse(String response, String getAuthType, String getUserPrincipal, String getRemoteUser) {
        Log.info(logClass, "verifyAuthenticatedResponse", "Verify response shows:  " + Constants.isAuthenticatedTrue + " ," + getUserPrincipal + " , "
                                                          + getRemoteUser);
        mustContain(response, Constants.isAuthenticatedTrue);
        mustContain(response, getAuthType);
        verifyUserResponse(response, getUserPrincipal, getRemoteUser);
    }

    protected void verifyUnauthenticatedResponse(String response) {
        Log.info(logClass, "verifyUnautenticatedResponse",
                 "Verify response shows: " + Constants.isAuthenticatedFalse + Constants.getAuthTypeNull + Constants.getRemoteUserNull + Constants.getUserPrincipalNull);
        mustContain(response, Constants.isAuthenticatedFalse);
        mustContain(response, Constants.getAuthTypeNull);
        verifyUserResponse(response, Constants.getUserPrincipalNull, Constants.getRemoteUserNull);
    }

    protected void verifyLogoutResponse(String response) {
        Log.info(logClass, "verifyLogoutResponse",
                 "Verify response shows: " + Constants.getAuthTypeNull + Constants.getUserPrincipalNull + Constants.getRemoteUserNull + Constants.getRunAsSubjectNull);
        mustContain(response, Constants.getAuthTypeNull);
        verifyUserResponse(response, Constants.getUserPrincipalNull, Constants.getRemoteUserNull);
        mustContain(response, Constants.getRunAsSubjectNull);

    }

    protected void verifyUnauthenticatedResponseInMessageLog() {
        Log.info(logClass, "verifyUnautenticatedResponseInMessageLog", "Verify messages.log contains unauthenticated results:  " + Constants.isAuthenticatedFalse + ", "
                                                                       + Constants.getUserPrincipalNull
                                                                       + " , " + Constants.getRemoteUserNull);
        assertNotNull("Servlet authenticate call did not return " + Constants.isAuthenticatedFalse + " as expected in messages.log.",
                      server.waitForStringInLogUsingMark(Constants.isAuthenticatedFalse));
        assertNotNull("Servlet getUserPrincipal call did not return " + Constants.getUserPrincipalNull + " as expected in messages.log.",
                      server.waitForStringInLogUsingMark(Constants.getUserPrincipalNull));
        assertNotNull("Servlet getRemoteUser call did not return " + Constants.getRemoteUserNull + " as expected in messages.log.",
                      server.waitForStringInLogUsingMark(Constants.getRemoteUserNull));
    }

    protected void verifyUserResponse(String response, String getUserPrincipal, String getRemoteUser) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + getUserPrincipal + ", " + getRemoteUser);
        mustContain(response, getUserPrincipal);
        mustContain(response, getRemoteUser);
    }

    protected void verifyEjbErrorUserResponse(String response, String errorMsgs) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + errorMsgs);
        mustContain(response, errorMsgs);
    }

    protected void verifyEjbUserResponse(String response, String ejbBean, String ejbBeanMethod, String getEjbRemoteUser) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + ejbBean + ", " + ejbBeanMethod + "," + getEjbRemoteUser);
        mustContain(response, ejbBean);
        mustContain(response, ejbBeanMethod);
        mustContain(response, getEjbRemoteUser);
    }

    protected void verifyEjbRunAsUserResponse(String response, String ejbBean, String ejbBeanMethod, String getEjbRemoteUser, String getEJBRunAsRemoteUser) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + ejbBean + ", " + ejbBeanMethod + "," + getEjbRemoteUser);
        mustContain(response, Constants.getEJBBeanResponse + ejbBean);
        mustContain(response, ejbBeanMethod);
        mustContain(response, getEjbRemoteUser);
        mustContain(response, ejbBean + " is invoking injected " + Constants.ejbRunASBean + " running as specified Employee role: ");
        mustContain(response, Constants.getEJBBeanResponse + Constants.ejbRunASBean);
        mustContain(response, getEJBRunAsRemoteUser);
        mustContain(response, Constants.ejbisCallerManagerFale);
        mustContain(response, Constants.ejbisCallerEmployeeTrue);
    }

    protected void verifyGroupIdsResponse(String response, String realmName, String groupName) {
        Log.info(logClass, "verifyGroupIdsResponse", "Verify groupIds in public credential contains: " + realmName + "//" + groupName);
        mustMatch(response, assembleRegExPublicCredentialGroupIds(realmName, groupName));
    }

    protected void verifyPostResponse(String response, String user, String firstName, String lastName, String eMailAddr, String phoneNumber) {
        Log.info(logClass, "verifyPostResponse", "Verify response shows: user : " + user + ", firstName : " + firstName + ", lastName : " + lastName + ", eMailAddr : " + eMailAddr
                                                 + ", phoneNum : " + phoneNumber);
        mustContain(response, "RemoteUser : " + user);
        mustContain(response, "firstName : " + firstName);
        mustContain(response, "lastName : " + lastName);
        mustContain(response, "eMailAddr : " + eMailAddr);
        mustContain(response, "phoneNum : " + phoneNumber);
    }

    public static String assembleRegExPublicCredentialGroupIds(String realmName, String groupName) {
        return "(?s)\\A.*?\\bCallerSubject:.*?groupIds=\\[.*?group:" + realmName + "/" + groupName + ".*?\\].*\\z";
    }

    protected void verifyNoGroupIdsResponse(String response) {
        Log.info(logClass, "verifyNoGroupIdsResponse", "Verify no groupIds in public credential:  groupIds=[]");
        mustMatch(response, assembleRegExPublicCredentialNoGroupIds());
    }

    public static String assembleRegExPublicCredentialNoGroupIds() {
        return "(?s)\\A.*?\\bCallerSubject:.*?groupIds=\\[\\].*\\z";
    }

    protected void verifyRunAsUserResponse(String response, String userName) {
        Log.info(logClass, "verifyRunAsUserResponse", "Verify RunAs subject contains:  WSPrincipal:" + userName);
        mustMatch(response, assembleRegExRunAsSubject(userName));
    }

    public static String assembleRegExRunAsSubject(String userName) {
        return "(?s)\\A.*?\\bRunAsSubject:.*?\\bWSPrincipal:" + userName + ".*\\z";
    }

    protected void verifyResponseAuthenticationFailed(String response) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + Constants.RESPONSE_AUTHENTICATION_FAILED);
        mustContain(response, Constants.RESPONSE_AUTHENTICATION_FAILED);
    }

    protected void verifyResponseAuthorizationFailed(String response) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + Constants.RESPONSE_AUTHORIZATION_FAILED);
        mustContain(response, Constants.RESPONSE_AUTHORIZATION_FAILED);
    }

    protected void verifyExceptionResponse(String response, String exception) {
        Log.info(logClass, "verifyExceptionResponse", "Verify response contains exception: " + exception);
        mustContain(response, exception);
    }

    protected void verifyMessageReceivedInMessageLog(String message) {
        assertNotNull("The messages.log file should contain the following message but did not --" + message,
                      server.waitForStringInLogUsingMark(message));
    }

    protected String buildQueryString(String operation, String layer, String appContext, String providerClass) {
        String queryString = operation + "**" + layer + "**" + appContext + "**" + providerClass;
        return queryString;
    }

    protected void verifyPersistentProviderRegisteredSuccessfully(String response) {
        mustContain(response, "Successfully registered provider.");
    }

    protected void verifyPersistentProviderRemovedSuccessfully(String response) {
        mustContain(response, "Successfully removed provider registration.");
    }

    protected void verifyPersistentProviderRemovalFailed(String response, String msgLayer, String appContext) {
        mustContain(response, "Failed to remove registered provider for message layer=" + msgLayer + " and application context=" + appContext);
    }

    protected void verifyException(String response, String exMsg, String msgTxt) {
        Log.info(logClass, "verifyException", "Verify response contains exception text" + msgTxt);
        assertTrue("Failed to find exception " + exMsg + " in response", response.contains(exMsg));
        if (msgTxt != null) {
            assertTrue("Failed to message " + msgTxt + " in messages.log", response.contains(msgTxt));
        }
    }

    protected void verifyRuntimeProviderRegistration(String response, String providerClass) {
        mustContain(response, Constants.messageLayerRuntime);
        mustContain(response, Constants.appContextRuntime);
        mustContain(response, Constants.isPersistentFalse);
        mustContain(response, "class " + providerClass);
    }

    protected void verifyPersistentProviderInformation(String response, String msgLayer, String appContext, String providerClass) {
        mustContain(response, msgLayer);
        mustContain(response, appContext);
        mustContain(response, Constants.isPersistentTrue);
        mustContain(response, providerClass);
    }

    protected void verifyPersistentProviderNotRegistered(String response, String msgLayer, String appContext) {
        response.contains("Failed to get registered provider for message layer=" + msgLayer + " and application context=" + appContext);
    }

    protected void verifyPersistentProviderNotRegisteredWithInvalidClass(String response, String providerClass) {
        response.contains("Unable to create a provider, class name: " + providerClass);

    }

    /**
     * verify the group names. Note that this is a simple string comparison.
     **/
    public void verifyGroups(String response, String groups) {
        Log.info(logClass, "verifyGroups", "Verify group contains: " + groups);
        mustContain(response, "groupIds=[" + groups + "]");
    }

    /**
     * verify the group names. Note that this is a simple string comparison.
     **/
    public void verifyNotInGroups(String response, String group) {
        Log.info(logClass, "verifyGroups", "Verify group does not contain: " + group);
        mustNotMatch(response, "(?s)\\A.*?\\bCallerSubject:.*?groupIds=\\[.*" + group + ".*\\].*\\z");
    }

    /**
     * verify the realm name. Note that this is a simple string comparison.
     **/
    public void verifyRealm(String response, String realm) {
        Log.info(logClass, "verifyRealm", "Verify realm is : " + realm);
        mustContain(response, "realmName=" + realm + ",");
    }

    public void verifySecurityContextResponse(String response, String getUserPrincipal, String getUserPrincipalName) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + getUserPrincipal + ", " + getUserPrincipalName);
        mustContain(response, getUserPrincipal);
        mustContain(response, getUserPrincipalName);
    }

    public void verifySecurityContextResponse(String response, String isCallerInRoleString) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + isCallerInRoleString);
        mustContain(response, isCallerInRoleString);
    }

    /**
     * This is an internal method used to set the server.xml
     * if the file in changed, restart the server.
     */
    public void setServerConfiguration(String serverXML, String... appNames) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            Log.info(logClass, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/" + serverXML);
            if (appNames != null) {
                for (String appName : appNames) {
                    server.addInstalledAppForValidation(appName);
                }
            }
            serverConfigurationFile = serverXML;
        }
    }

    public void assertCookie(String cookieHeaderString, boolean secure, boolean httpOnly) {
        assertTrue("The Path parameter must be set.", cookieHeaderString.contains("Path=/"));
        assertEquals("The Secure parameter must" + (secure == true ? "" : " not" + " be set."), secure, cookieHeaderString.contains("Secure"));
        assertEquals("The HttpOnly parameter must" + (httpOnly == true ? "" : " not" + " be set."), httpOnly, cookieHeaderString.contains("HttpOnly"));
    }

    /**
     * Assert that the regular expression string is present or NOT present in the server's logs.
     * 
     * @param regexp The regular expression string to search for.
     * @throws Exception If there was an error checking the log or trace files.
     */
    public void assertStringsInLogsUsingMark(String regexp) throws Exception {
        assertStringsInLogsUsingMark(regexp, true);
    }

    /**
     * Assert that the regular expression string is present or NOT present in the server's logs.
     * 
     * @param regexp The regular expression string to search for.
     * @param isPresent If true, check that the string is present. If false, check that it is NOT present.
     * @throws Exception If there was an error checking the log or trace files.
     */
    public void assertStringsInLogsUsingMark(String regexp, boolean isPresent) throws Exception {
        List<String> results = server.findStringsInLogsUsingMark(regexp, server.getDefaultLogFile());
        if (isPresent) {
            assertFalse("Did not find '" + regexp + "' in trace.", results.isEmpty());
        } else {
            assertTrue("Found '" + regexp + "' in trace: " + results, results.isEmpty());
        }
    }

    /**
     * Assert that the regular expression string is present in the server's logs or trace
     * 
     * @param regexp The regular expression string to search for.
     * @throws Exception If there was an error checking the log or trace files.
     */
    public void assertStringsInLogsAndTraceUsingMark(String regexp) throws Exception {
        assertStringsInLogsAndTraceUsingMark(regexp, true);
    }

    /**
     * Assert that the regular expression string is present or NOT present in the server's logs or trace.
     * 
     * @param regexp The regular expression string to search for.
     * @param isPresent If true, check that the string is present. If false, check that it is NOT present.
     * @throws Exception If there was an error checking the log or trace files.
     */
    public void assertStringsInLogsAndTraceUsingMark(String regexp, boolean isPresent) throws Exception {
        List<String> results = server.findStringsInLogsAndTraceUsingMark(regexp);
        if (isPresent) {
            assertFalse("Did not find '" + regexp + "' in trace.", results.isEmpty());
        } else {
            assertTrue("Found '" + regexp + "' in trace: " + results, results.isEmpty());
        }
    }

    /**
     * Assume we are not on Windows and running the EE9 repeat action. There is an issue with
     * the Jakarta transformer where the application fails to be transformed b/c the application
     * directory cannot be deleted due to a "The process cannot access the file because it is
     * being used by another process" error. I assume that either the transformer or the server
     * is not releasing the handle to the directory, but I have not yet been able to figure it
     * out.
     */
    public static void assumeNotWindowsEe9() {
        if (JakartaEE9Action.isActive() && System.getProperty("os.name").toLowerCase().startsWith("win")) {
            Log.info(logClass, "assumeNotWindowsEe9", "Skipping EE9 repeat action on Windows.");
            assumeTrue(false);
        }
    }
}
