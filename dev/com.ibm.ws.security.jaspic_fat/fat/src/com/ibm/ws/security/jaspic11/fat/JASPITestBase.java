/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspic11.fat;

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
import org.apache.http.HttpEntity;
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

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

public class JASPITestBase {

    public static final String DEFAULT_REALM = "JaspiRealm";
    //Value of JASPI_GROUP must match group.name property passed in provider bnd.bnd.
    public static final String JASPI_GROUP = "JASPIGroup";

    public static final String DEFAULT_BASICAUTH_SERVLET_NAME = "ServletName: JASPIBasicAuthServlet";
    public static final String DEFAULT_FORMLOGIN_SERVLET_NAME = "ServletName: JASPIFormLoginServlet";
    public static final String DEFAULT_REGISTRATION_SERVLET_NAME = "ServletName: JASPIRegistrationTestServlet";

    public static final String DEFAULT_CALLBACK_SERVLET_NAME = "ServletName: JASPICallbackTestBasicAuthServlet";
    public static final String DEFAULT_CALLBACK_FORMLOGIN_SERVLET_NAME = "ServletName: JASPICallbackTestFormLoginServlet";
    public static final String DEFAULT_CALLBACK_FORM_LOGIN_PAGE = "/JASPICallbackTestFormLoginServlet/j_security_check";

    public static final String DEFAULT_SERVLET30_NAME = "ServletName: JASPIServlet30";
    public static final String NO_NAME_VALIDATION = "NONE";
    public static final String DEFAULT_BASIC_REGISTRATION = "/JASPIBasic";
    public static final String DEFAULT_FORM_REGISTRATION = "/JASPIForm";
    public static final String DEFAULT_FORM_LOGIN_PAGE = "/JASPIFormLoginServlet/j_security_check";

    public static final String AUTH_TYPE_BASIC = "BASIC";
    public static final String AUTH_TYPE_FORM = "FORM";

    public static final String DEFAULT_APP = "JASPIBasicAuthServlet";
    public static final String DEFAULT_REGISTRATION_APP = "JASPIRegistrationTestServlet";
    public static final String DEFAULT_FORM_APP = "JASPIFormLoginServlet";
    public static final String DEFAULT_SERVLET30_APP = "JASPIServlet30";
    public static final String DEFAULT_CALLBACK_APP = "JASPICallbackTestBasicAuthServlet";
    public static final String DEFAULT_CALLBACK_FORM_APP = "JASPICallbackTestFormLoginServlet";
    public static final String DEFAULT_WRAPPING_APP = "JASPIWrappingServlet";

    public final static String FORM_LOGIN_PAGE = "Form Login Page";
    public final static String FORM_LOGOUT_PAGE = "Form Logout Page";
    public final static String LOGIN_ERROR_PAGE = "Form Login Error Page";
    public final static String FORM_LOGIN_JASPI_PAGE = "Form Login Page For Include and Forward";

    public final static String ALL_CALLBACKS = "?PVCB=YES&CPCB=YES&GPCB=YES";
    public final static String CPCB_PRINCIPAL_CALLBACK = "?PVCB=NO&CPCB=YES&GPCB=NO&cpcbType=JASPI_PRINCIPAL";
    public final static String MANUAL_CALLBACK = "?PVCB=MANUAL&CPCB=NO&GPCB=NO";
    public final static String PVCB_CALLBACK = "?PVCB=YES&CPCB=NO&GPCB=NO";
    public final static String NO_CALLBACKS = "?PVCB=NO&CPCB=NO&GPCB=NO";
    public final static String CPCB_CALLBACK = "?PVCB=NO&CPCB=YES&GPCB=NO";
    public final static String GPCB_CALLBACK = "?PVCB=NO&CPCB=NO&GPCB=YES";
    public final static String CPCBGPCB_CALLBACK = "?PVCB=NO&CPCB=YES&GPCB=YES";
    public final static String PVCBGPCB_CALLBACK = "?PVCB=YES&CPCB=NO&GPCB=YES";
    public final static String CPCB_CALLBACK_DESCR = "PVCB=NO&CPCB=YES&GPCB=NO";
    public final static String GPCB_CALLBACK_DESCR = "PVCB=NO&CPCB=NO&GPCB=YES";
    public final static String ALL_CALLBACK_DESCR = "PVCB=YES&CPCB=YES&GPCB=YES";
    public final static String CPCBGPCB_CALLBACK_DESCR = "PVCB=NO&CPCB=YES&GPCB=YES";
    public final static String PVCB_CALLBACK_DESCR = "PVCB=YES&CPCB=NO&GPCB=NO";

    public static final String DEFAULT_APP_CONTEXT = "default_host " + DEFAULT_APP;
    public static final String TEST_APP1_CONTEXT = "testApp1Context";
    public static final String TEST_APP2_CONTEXT = "testApp2Context";
    public static final String PROFILE_SERVLET_MSG_LAYER = "HttpServlet";

    protected final String DEFAULT_JASPI_PROVIDER = "bob";

    protected final String DEFAULT_PROVIDER_CLASS = "com.ibm.ws.security.jaspi.test.AuthProvider";
    protected final String PERSISTENT_PROVIDER_CLASS = "com.ibm.ws.security.jaspi.test.AuthProvider_1";
    protected final String REGISTER = "?REGISTER";
    protected final String GET = "?GET";
    protected final String REMOVE = "?REMOVE";
    protected final String REGISTERINVALIDCLASS = "?REGISTERINVALIDCLASS";
    protected final String REMOVEINVALID = "?REMOVEINVALID";

    // Audit record
    protected final String DEFAULT_PROVIDER_AUDIT = "class com.ibm.ws.security.jaspi.test.AuthProvider";
    protected static String BASICAUTH_PROTECTED_URI = "/JASPIBasicAuthServlet" + DEFAULT_BASIC_REGISTRATION;
    protected static String BASICAUTH_UNPROTECTED_URI = "/JASPIBasicAuthServlet/JASPIUnprotected";

    // Jaspi test users
    protected final String jaspi_basicRoleGroup = "group1";
    protected final String jaspi_basicRoleUser = "jaspiuser1";
    protected final String jaspi_basicRolePwd = "s3cur1ty";
    protected final String jaspi_basicRoleGroupUser = "jaspiuser2";
    protected final String jaspi_basicRoleGroupPwd = "s3cur1ty";
    protected final String jaspi_basicRoleUserPrincipal = "jaspiuser6";
    protected final String jaspi_formRoleGroup = "group3";
    protected final String jaspi_formRoleUser = "jaspiuser3";
    protected final String jaspi_formRolePwd = "s3cur1ty";
    protected final String jaspi_formRoleGroupUser = "jaspiuser5";
    protected final String jaspi_formRoleGroupPwd = "s3cur1ty";
    protected final String jaspi_noRoleUser = "jaspiuser4";
    protected final String jaspi_noRolePwd = "s3cur1ty";
    protected final String jaspi_servlet30User = "jaspiuser1";
    protected final String jaspi_servlet30UserPwd = "s3cur1ty";
    protected final String jaspi_invalidUser = "invalidUserName";
    protected final String jaspi_invalidPwd = "invalidPassword";
    protected final String jaspi_notInRegistryNotInRoleUser = "jaspiUser100";
    protected final String jaspi_notInRegistryNotInRolePwd = "jaspiUser100Pwd";
    protected final String jaspi_notInRegistryInBasicRoleUser = "jaspiuser101";
    protected final String jaspi_notInRegistryInBasicRolePwd = "jaspiuser101Pwd";
    protected final String jaspi_notInRegistryInFormRoleUser = "jaspiuser102";
    protected final String jaspi_notInRegistryInFormRolePwd = "jaspiuser102Pwd";

    // Jaspi roles
    protected final String BasicRole = "jaspi_basic";
    protected final String FormRole = "jaspi_form";

    // Values to be verified in servlet response

    protected static final String RESPONSE_AUTHENTICATION_FAILED = "JASPIC Authenticated with status: SEND_FAILURE";
    protected static final String RESPONSE_AUTHORIZATION_FAILED = "AuthorizationFailed";

    protected final String jaspiValidateRequest = "JASPI validateRequest called with auth provider=";
    protected final String jaspiSecureResponse = "JASPI secureResponse called with auth provider=";
    protected final String userRegistryRealm = "JaspiRealm";
    protected final String isAuthenticatedTrue = "isAuthenticated: true";
    protected final String isAuthenticatedFalse = "isAuthenticated: false";
    protected final String getRemoteUserFound = "getRemoteUser: ";
    protected final String getUserPrincipalFound = "getUserPrincipal: WSPrincipal:";
    protected final String getUserPrincipalFoundJaspiPrincipal = "getUserPrincipal: com.ibm.ws.security.jaspi.test.AuthModule$JASPIPrincipal";
    protected final String getRemoteUserNull = "getRemoteUser: null";
    protected final String getUserPrincipalNull = "getUserPrincipal: null";
    protected final String getAuthTypeBasic = "getAuthType: BASIC";
    protected final String getAuthTypeJaspi = "getAuthType: JASPI_AUTH";
    protected final String getAuthTypeForm = "getAuthType: FORM";
    protected final String getAuthTypeNull = "getAuthType: null";
    protected final String getRunAsSubjectNull = "RunAsSubject: null";
    protected final String isManadatoryFalse = "isManadatory=false";
    protected final String isManadatoryTrue = "isManadatory=true";
    protected final String requestIsWrapped = "The httpServletRequest has been wrapped by httpServletRequestWrapper.";
    protected final String responseIsWrapped = "The httpServletRestponse has been wrapped by httpServletResponseWrapper.";

    protected final String messageLayerRuntime = "null";
    protected final String messageLayerDefault = "HttpServlet";
    protected final String appContext = "default_host /";
    protected final String appContextRuntime = "null";
    protected final String isPersistentTrue = "true";
    protected final String isPersistentFalse = "false";
    protected final String providerClass = "Provider class: ";
    protected final String providerClassDefault = "Provider class: com.ibm.ws.security.jaspi.test.AuthProvider";

    // Values to be verified in messages
    protected static final String MSG_JASPI_AUTHENTICATION_FAILED = "CWWKS1652A:.*";
    protected static final String PROVIDER_AUTHENTICATION_FAILED = "Invalid user or password";
    protected static final String MSG_AUTHORIZATION_FAILED = "CWWKS9104A:.*";
    protected static final String MSG_JACC_AUTHORIZATION_FAILED = "CWWKS9124A:.*";

    protected static final String MSG_JASPI_PROVIDER_ACTIVATED = "CWWKS1653I";
    protected static final String MSG_JASPI_PROVIDER_DEACTIVATED = "CWWKS1654I";

    protected static final String MSG_JACC_SERVICE_STARTING = "CWWKS2850I";
    protected static final String MSG_JACC_SERVICE_STARTED = "CWWKS2851I";

    protected static final String EXCEPTION_JAVA_LANG_SECURITY = "java.lang.SecurityException";
    protected final String SERVLET_EXCEPTION = "javax.servlet.ServletException";
    protected final String SERVLET_EXCEPTION_JASPI_LOGIN = "javax.servlet.ServletException: The login method may not be invoked while JASPI authentication is active.";

    // Jaspi server.xml files
    protected static final String SERVLET_SECURITY_NOJASPI_SERVER_XML = "dynamicSecurityFeature/servlet31_appSecurity20_noJaspi.xml";
    protected static final String SERVLET_SECURITY_JASPI_SERVER_XML = "dynamicSecurityFeature/servlet31_appSecurity20_withJaspi.xml";

    // Jaspi helper methods
    protected static void verifyServerStarted(LibertyServer server) {
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLogUsingMark("CWWKS0008I"));
    }

    protected static void verifyServerUpdated(LibertyServer server) {
        assertNotNull("Feature update wasn't complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("The server configuration wasn't updated.",
                      server.waitForStringInLogUsingMark("CWWKG0017I:.*"));

    }

    protected static void verifyServerUpdatedWithJaspi(LibertyServer server, String appName) {
        verifyServerUpdated(server);
        assertNotNull("The JASPI user feature did not report it was ready",
                      server.waitForStringInLogUsingMark(MSG_JASPI_PROVIDER_ACTIVATED));
        if (JakartaEE9Action.isActive()) {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-2.0"));
        } else {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-1.0"));
        }
        // Need to wait for the application started message.
        assertNotNull("URL not available " + server.waitForStringInLogUsingMark("CWWKT0016I.*" + appName + ".*"));
    }

    protected static void verifyServerStartedWithJaspiFeatureAndJacc(LibertyServer server) {
        verifyServerStartedWithJaspiFeature(server);
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog(MSG_JACC_SERVICE_STARTING));
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog(MSG_JACC_SERVICE_STARTED));

    }

    protected static void verifyServerRemovedJaspi(LibertyServer server, String appName) {
        verifyServerUpdated(server);
        assertNotNull("The JASPI user feature did not report it was ready",
                      server.waitForStringInLogUsingMark(MSG_JASPI_PROVIDER_DEACTIVATED));
        // Need to wait for the application started message.
        assertNotNull("URL not available " + server.waitForStringInLogUsingMark("CWWKT0016I.*" + appName + ".*"));
    }

    protected static void verifyServerStartedWithJaspiFeature(LibertyServer server) {
        verifyServerStarted(server);
        assertNotNull("The JASPI user feature did not report it was ready",
                      server.waitForStringInLogUsingMark(MSG_JASPI_PROVIDER_ACTIVATED));
        if (JakartaEE9Action.isActive()) {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-2.0"));
        } else {
            assertNotNull("The feature manager did not report the JASPI provider is included in features.",
                          server.waitForStringInLogUsingMark("CWWKF0012I.*" + "usr:jaspicUserTestFeature-1.0"));
        }

    }

    // Values to be set by the child class
    protected LibertyServer server;
    protected Class<?> logClass;

    protected JASPITestBase(LibertyServer server, Class<?> logClass) {
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
        HttpGet getMethod = new HttpGet(url);
        if (userid != null)
            httpClient.getCredentialsProvider().setCredentials(new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                                                               new UsernamePasswordCredentials(userid, password));
        HttpResponse response = httpClient.execute(getMethod);
        Log.info(logClass, methodName, "Actual response: " + response.toString());

        return processResponse(response, expectedStatusCode);

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
        String formTitle = FORM_LOGIN_PAGE;
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

    public String executeFormLogin(HttpClient httpclient, String url, String username, String password) throws Exception {
        return executeFormLogin(httpclient, url, username, password, null);
    }

    public String executeFormLogin(HttpClient httpclient, String url, String username, String password, String description) throws Exception {
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

        // Verify redirect to servlet
        int status = response.getStatusLine().getStatusCode();
        assertTrue("Form login did not result in redirect: " + status, status == HttpServletResponse.SC_MOVED_TEMPORARILY);

        Header header = response.getFirstHeader("Location");

        String location = header.getValue();
        Log.info(logClass, methodName, "Redirect location:  " + location);

        Log.info(logClass, methodName, "Modified Redirect location:  " + location);
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
                           + LOGIN_ERROR_PAGE, content.contains(LOGIN_ERROR_PAGE));
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
        if (servletName != NO_NAME_VALIDATION)
            mustContain(response, servletName);
        verifyJaspiAuthenticationUsed(response, jaspiProvider);
    }

    protected void verifyJaspiAuthenticationProcessedOnFormLoginError(String response, String jaspiProvider) {
        Log.info(logClass, "verifyJaspiAuthenticationProcessedByProvider", "Verify response shows Login Error Page and JASPI processed validateRequest");
        mustContain(response, LOGIN_ERROR_PAGE);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();
    }

    protected void verifyJaspiAuthenticationUsed(String response, String jaspiProvider) {
        Log.info(logClass, "verifyJaspiAuthenticationUsed", "Verify response shows JASPI validateRequest and secureResponse called");
        mustContain(response, jaspiValidateRequest + jaspiProvider);
        mustContain(response, jaspiSecureResponse + jaspiProvider);
    }

    protected void verifyJaspiRequestAndResponseWrapping(String response) {
        Log.info(logClass, "verifyJaspiRequestAndResponseWrapping", "Verify JASPI request and response have been wrapped.");
        mustContain(response, requestIsWrapped);
        mustContain(response, responseIsWrapped);
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
        mustNotContain(response, jaspiValidateRequest + jaspiProvider);
        mustNotContain(response, jaspiSecureResponse + jaspiProvider);
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
        Log.info(logClass, "verifyAuthenticatedResponse", "Verify response shows:  " + isAuthenticatedTrue + " ," + getUserPrincipal + " , "
                                                          + getRemoteUser);
        mustContain(response, isAuthenticatedTrue);
        mustContain(response, getAuthType);
        verifyUserResponse(response, getUserPrincipal, getRemoteUser);
    }

    protected void verifyUnauthenticatedResponse(String response) {
        Log.info(logClass, "verifyUnautenticatedResponse", "Verify response shows: " + isAuthenticatedFalse + getAuthTypeNull + getRemoteUserNull + getUserPrincipalNull);
        mustContain(response, isAuthenticatedFalse);
        mustContain(response, getAuthTypeNull);
        verifyUserResponse(response, getUserPrincipalNull, getRemoteUserNull);
    }

    protected void verifyLogoutResponse(String response) {
        Log.info(logClass, "verifyLogoutResponse", "Verify response shows: " + getAuthTypeNull + getUserPrincipalNull + getRemoteUserNull + getRunAsSubjectNull);
        mustContain(response, getAuthTypeNull);
        verifyUserResponse(response, getUserPrincipalNull, getRemoteUserNull);
        mustContain(response, getRunAsSubjectNull);
        verifyJaspiLogoutProcessedCleanSubjectInMessageLog();
    }

    protected void verifyUnauthenticatedResponseInMessageLog() {
        Log.info(logClass, "verifyUnautenticatedResponseInMessageLog", "Verify messages.log contains unauthenticated results:  " + isAuthenticatedFalse + ", "
                                                                       + getUserPrincipalNull
                                                                       + " , " + getRemoteUserNull);
        assertNotNull("Servlet authenticate call did not return " + isAuthenticatedFalse + " as expected in messages.log.",
                      server.waitForStringInLogUsingMark(isAuthenticatedFalse));
        assertNotNull("Servlet getUserPrincipal call did not return " + getUserPrincipalNull + " as expected in messages.log.",
                      server.waitForStringInLogUsingMark(getUserPrincipalNull));
        assertNotNull("Servlet getRemoteUser call did not return " + getRemoteUserNull + " as expected in messages.log.",
                      server.waitForStringInLogUsingMark(getRemoteUserNull));
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
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + RESPONSE_AUTHENTICATION_FAILED);
        mustContain(response, RESPONSE_AUTHENTICATION_FAILED);
    }

    protected void verifyResponseAuthorizationFailed(String response) {
        Log.info(logClass, "verifyUserResponse", "Verify response contains: " + RESPONSE_AUTHORIZATION_FAILED);
        mustContain(response, RESPONSE_AUTHORIZATION_FAILED);
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
        mustContain(response, messageLayerRuntime);
        mustContain(response, appContextRuntime);
        mustContain(response, isPersistentFalse);
        mustContain(response, "class " + providerClass);
    }

    protected void verifyPersistentProviderInformation(String response, String msgLayer, String appContext, String providerClass) {
        mustContain(response, msgLayer);
        mustContain(response, appContext);
        mustContain(response, isPersistentTrue);
        mustContain(response, providerClass);
    }

    protected void verifyPersistentProviderNotRegistered(String response, String msgLayer, String appContext) {
        response.contains("Failed to get registered provider for message layer=" + msgLayer + " and application context=" + appContext);
    }

    protected void verifyPersistentProviderNotRegisteredWithInvalidClass(String response, String providerClass) {
        response.contains("Unable to create a provider, class name: " + providerClass);

    }

}