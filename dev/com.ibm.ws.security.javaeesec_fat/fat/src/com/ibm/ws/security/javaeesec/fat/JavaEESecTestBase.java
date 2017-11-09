/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;

import componenttest.topology.impl.LibertyServer;

public class JavaEESecTestBase {

    // Values to be set by the child class
    protected LibertyServer server;
    protected static Class<?> logClass;

    protected JavaEESecTestBase(LibertyServer server, Class<?> logClass) {
        this.server = server;
        this.logClass = logClass;
    }

    protected String getCurrentTestName() {
        return "Test name not set";
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
        return executeFormLogin(httpclient, url, username, password, redirect, null);
    }

    public String executeFormLogin(HttpClient httpclient, String url, String username, String password, boolean redirect, String description) throws Exception {
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

        return location;
    }

    /**
     *
     * @param client
     * @param location
     * @param expectedStatusCode
     * @return
     */
    protected String accessPageNoChallenge(HttpClient client, String location, int expectedStatusCode, String servletName) {
        String methodName = "accessPageNoChallenge";
        Log.info(logClass, methodName, "accessPageNoChallenge: location =  " + location + " expectedStatusCode =" + expectedStatusCode);

        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(location);
            HttpResponse response = client.execute(getMethod);

            Log.info(logClass, methodName, "getMethod status:  " + response.getStatusLine());

            assertEquals("Expected " + expectedStatusCode + " was not returned",
                         expectedStatusCode, response.getStatusLine().getStatusCode());

            String content = EntityUtils.toString(response.getEntity());
            Log.info(logClass, methodName, "Servlet full response content: \n" + content);

            EntityUtils.consume(response.getEntity());

            // Paranoia check, make sure we hit the right servlet
            if (response.getStatusLine().getStatusCode() == 200) {
                assertTrue("Response did not contain expected servlet name (" + servletName + ")",
                           content.contains(servletName));
                return content;
            } else if (expectedStatusCode == 401) {
                assertTrue("Response was not the expected error page: "
                           + Constants.LOGIN_ERROR_PAGE, content.contains(Constants.LOGIN_ERROR_PAGE));
                return null;
            } else {
                return null;
            }
        } catch (IOException e) {
            fail("Caught unexpected exception: " + e);
            return null;
        }
    }

    protected void verifyJaspiAuthenticationProcessedByProvider(String response, String jaspiProvider, String servletName) {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedByProvider", "Verify response contains Servlet name and JASPI authentication processed");
        if (servletName != Constants.NO_NAME_VALIDATION)
            mustContain(response, servletName);
        verifyJaspiAuthenticationUsed(response, jaspiProvider);
    }

    protected void verifyJaspiAuthenticationProcessedOnFormLoginError(String response, String jaspiProvider) {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedByProvider", "Verify response shows Login Error Page and JASPI processed validateRequest");
        mustContain(response, Constants.LOGIN_ERROR_PAGE);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();
    }

    protected void verifyJaspiAuthenticationUsed(String response, String jaspiProvider) {
        Log.info(logClass, "verifyJaspiAuthenticationUsed", "Verify response shows JASPI validateRequest and secureResponse called");
        mustContain(response, Constants.jaspiValidateRequest + jaspiProvider);
        mustContain(response, Constants.jaspiSecureResponse + jaspiProvider);
    }

    protected void verifyJaspiRequestAndResponseWrapping(String response) {
        Log.info(logClass, "verifyJaspiRequestAndResponseWrapping", "Verify JASPI request and response have been wrapped.");
        mustContain(response, Constants.requestIsWrapped);
        mustContain(response, Constants.responseIsWrapped);
        responseMustBeWrapped(response, "SCIProgrammaticRequestFilter");
        responseMustBeWrapped(response, "SCLProgrammaticRequestFilter");
        responseMustBeWrapped(response, "DeclaredRequestFilter");
        responseMustBeWrapped(response, "AnnotatedRequestFilter");
    }

    private void responseMustBeWrapped(String response, String filterName) {
        mustContain(response, "The httpServletRequest in the " + filterName + " filter has been wrapped by httpServletRequestWrapper.");
        mustContain(response, "The httpServletRestponse in the " + filterName + " filter has been wrapped by httpServletResponseWrapper.");
    }

    protected void verifyNoJaspiAuthentication(String response, String jaspiProvider) {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedByProvider", "Verify response does NOT show calls to JASPI validateRequest and secureRespone");
        mustNotContain(response, Constants.jaspiValidateRequest + jaspiProvider);
        mustNotContain(response, Constants.jaspiSecureResponse + jaspiProvider);
    }

    private void mustContain(String response, String target) {
        assertTrue("Expected result " + target + " not found in response", response.contains(target));
    }

    private void mustNotContain(String response, String target) {
        assertFalse("Expected result " + target + " was found in response and should not have been found.", response.contains(target));
    }

    private void mustMatch(String response, String target) {
        assertTrue("Expected result " + target + " not found in response", response.matches(target));
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
        verifyJaspiLogoutProcessedCleanSubjectInMessageLog();
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

    protected void verifyJaspiAuthenticationProcessedInMessageLog() {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedInMessageLog", "Verify messages.log contains isMandatory=true and calls to validateRequest and secureResponse ");
        assertNotNull("Messages.log did not show that security runtime indicated that isMandatory=true and the JASPI provider must protect this resource. ",
                      server.waitForStringInLogUsingMark("JASPI_PROTECTED isMandatory=true"));
        assertNotNull("Messages.log did not show JASPI provider validateRequest was called. ",
                      server.waitForStringInLogUsingMark("validateRequest"));
        assertNotNull("Messages.log did not show JASPI provider secureResponse was called. ",
                      server.waitForStringInLogUsingMark("secureResponse"));
    }

    protected void verifyAuthenticateMethodProcessedInMessageLog(String method) {
        Log.info(logClass, "verifyAuthenticateMethodProcessedInMessageLog", "Verify messages.log contains QueryString: method=" + method
                                                                            + " followed by call to validateRequest");
        assertNotNull("Messages.log did not show QueryString: method=" + method,
                      server.waitForStringInLogUsingMark("QueryString: method=" + method));
        assertNotNull("Messages.log did not show validateRequest",
                      server.waitForStringInLogUsingMark("validateRequest"));
        assertNotNull("Messages.log did not show Servlet authenticate invoked for " + method,
                      server.waitForStringInLogUsingMark("Servlet authenticate invoked for " + method));
    }

    protected void verifyJaspiAuthenticationProcessedUnprotectedInMessageLog() {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedUnprotectedInMessageLog",
                 "Verify messages.log contains isMandatory=false and calls to validateRequest and secureResponse ");
        assertNotNull("Messages.log did not show that the security runtime indicated that authentication is optional for unprotected servlet through isMandatory=false. ",
                      server.waitForStringInLogUsingMark("JASPI_UNPROTECTED isMandatory=false"));
        assertNotNull("Messages.log did not show JASPI provider validateRequest was called. ",
                      server.waitForStringInLogUsingMark("validateRequest"));
        assertNotNull("Messages.log did not show JASPI provider secureResponse was called. ",
                      server.waitForStringInLogUsingMark("secureResponse"));
    }

    // For Form Login positive flow, after the form is submitted, we only get validateRequest and not a call to secureResponse
    protected void verifyJaspiAuthenticationProcessedValidateRequestInMessageLog() {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedValidateRequestInMessageLog", "Verify messages.log contains calls to validateRequest and secureResponse ");
        assertNotNull("Messages.log did not show JASPI provider is protecting this resource. ",
                      server.waitForStringInLogUsingMark("JASPI_UNPROTECTED isMandatory=false"));
        assertNotNull("Messages.log did not show JASPI provider validateRequest was called. ",
                      server.waitForStringInLogUsingMark("validateRequest"));
    }

    protected void verifyJaspiLogoutProcessedCleanSubjectInMessageLog() {
        Log.info(logClass, "verifyJaspiLogoutProcessedCleanSubjectInMessageLog", "Verify messages.log contains call to cleanSubject.");
        assertNotNull("Messages.log did not show JASPI provider called cleanSubject. ",
                      server.waitForStringInLogUsingMark("cleanSubject"));
    }

    protected void verifyUserResponse(String response, String getUserPrincipal, String getRemoteUser) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + getUserPrincipal + ", " + getRemoteUser);
        mustContain(response, getUserPrincipal);
        mustContain(response, getRemoteUser);
    }

    protected void verifyGroupIdsResponse(String response, String realmName, String groupName) {
        Log.info(logClass, "verifyGroupIdsResponse", "Verify groupIds in public credential contains: " + realmName + "//" + groupName);
        mustMatch(response, assembleRegExPublicCredentialGroupIds(realmName, groupName));
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

}