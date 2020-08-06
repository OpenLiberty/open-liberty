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
 *
 */

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AuthenticateMethodPersistCredDisabledTest extends CommonAuthenticateMethodScenarios {

    private static final Class<?> thisClass = AuthenticateMethodPersistCredDisabledTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.persistcred.disabled");
    private static CommonTestHelper testHelper = new CommonTestHelper();
    private static ServerTracesPasswordAsserter serverTracesPasswordAsserter;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation("loginmethod");
        //LDAPUtils.addLDAPVariables(server);

        JACCFatUtils.transformApps(server, "loginmethod.ear");

        server.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen

        serverTracesPasswordAsserter = new ServerTracesPasswordAsserter(server);
    }

    public AuthenticateMethodPersistCredDisabledTest() {
        super(server, thisClass, testHelper);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            serverTracesPasswordAsserter.assertNoPasswords(validPassword, managerPassword);
        } finally {
            try {
                server.stopServer();
            } finally {
                JACCFatUtils.uninstallJaccUserFeature(server);
            }
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * <LI> 1) login() of user2 is called and should return the correct values for the passed-in user
     * <LI> 2) logout() of user2 is called and should return null for the APIs
     * <LI> 3) authenticate() is called, prompts for login, log in with user1 and user1 info is shown
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_UnprotectedServlet2() throws Exception {
        METHODS = "testMethod=login,logout,authenticate";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/UnprotectedProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test1, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(managerUser, test2, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test3, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) redirected to unprotected page, so not prompted
     * <LI> 3) logout() of user1 is called and should return null for the APIs
     * <LI> 4) login() of user2 is call and should return user2 info
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_ProtectedRedirectToUnprotected() throws Exception {
        METHODS = "testMethod=authenticate";
        REDIRECT_METHODS = "redirectMethod=logout,login";
        REDIRECT_PAGE = "redirectPage=UnprotectedProgrammaticAPIServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/AuthenticateRedirectServlet?" + METHODS + "&" + REDIRECT_METHODS
                     + "&" + REDIRECT_PAGE + "&user=" + managerUser + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("Start initial values"), response.indexOf("STARTTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test3 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));

        // TEST1 - check values after authenticate
        testHelper.verifyNullValuesAfterLogout(validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after login
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet configured for form login.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * <LI> 1) login() of user2 is called and should return the correct values for the passed-in user
     * <LI> 2) logout() of user2 is called and should return null for the APIs
     * <LI> 3) authenticate() is called, returns false and prompts. log in with user1
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_UnprotectedServlet2() throws Exception {
        METHODS = "testMethod=login,logout,authenticate";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/UnprotectedProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFL(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test1, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(managerUser, test2, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test3, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId (user1) and password permit access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) redirected to unprotected page, so not prompted
     * <LI> 3) logout() of user1 is called and should return null for the APIs
     * <LI> 4) login() of user2 is call and should return user2 info
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_ProtectedRedirectToUnprotected() throws Exception {
        METHODS = "testMethod=authenticate";
        REDIRECT_METHODS = "redirectMethod=logout,login";
        REDIRECT_PAGE = "redirectPage=UnprotectedProgrammaticAPIServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/AuthenticateRedirectServlet?" + METHODS + "&" + REDIRECT_METHODS
                     + "&" + REDIRECT_PAGE + "&user=" + managerUser + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFL(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("Start initial values"), response.indexOf("STARTTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test3 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));

        // TEST1 - check values after authenticate
        testHelper.verifyNullValuesAfterLogout(validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful basic auth is expected when accessing the servlet
     */
    private String authenticateWithValidAuthDataBA(String user, String password, String url, String urlString) {
        String methodName = "authenticateWithValidAuthDataBA";
        String debugData = "URL:" + url + "; userId:[" + user + "]" + "; password:[" + password + "]";
        Log.info(thisClass, methodName, debugData);
        HttpResponse getMethod = null;
        String response = null;
        try {
            getMethod = testHelper.executeGetRequestWithAuthCreds(url, user, password);
            String authResult = getMethod.getStatusLine().toString();
            Log.info(thisClass, methodName, "BasicAuth result: " + authResult);
            HttpEntity entity = getMethod.getEntity();
            response = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(thisClass, methodName, "BasicAuth response: " + response);

            // Verify we get the correct response
            assertTrue("Expected output, " + urlString + ", not returned: " + response, response.contains(urlString));
            assertTrue("Expecting 200, got: " + authResult,
                       authResult.contains("200"));

        } catch (Exception e) {
            testHelper.handleException(e, methodName, debugData);
        }
        return response;
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful form login is expected when accessing the servlet
     */
    private String authenticateWithValidAuthDataFL(HttpClient client, String user, String password, String url) {
        String methodName = "authenticateWithValidAuthDataFL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        Log.info(thisClass, methodName, "Verifying URL: " + debugData);
        String getResponseRedirect = null;
        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            HttpResponse getResponse = client.execute(getMethod);
            String authResult = getResponse.getStatusLine().toString();
            Log.info(thisClass, methodName, "HttpGet(" + url + ") result: " + authResult);
            HttpEntity entity = getResponse.getEntity();
            String response = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);

            Log.info(thisClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
            Log.info(thisClass, methodName, "Expecting form login page. Get response: " + response);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, response.contains(FORMLOGINJSP));

            // Post method to login
            HttpPost postMethod = new HttpPost("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/j_security_check");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            assertTrue("Expecting form login getStatusCode 302", postResponse.getStatusLine().getStatusCode() == 302);

            // Get cookie
            // Todo: cookie name can change in future
            String ssoCookie = testHelper.getCookieValue(postResponse, cookieName);
            if (ssoCookie == null) {
                fail("LtpaToken2 not found in the cookie after login");
            }

            // Verify redirect to servlet
            Header header = postResponse.getFirstHeader("Location");
            String location = header.getValue();
            Log.info(thisClass, methodName, "Redirect location: " + location);
            EntityUtils.consume(postResponse.getEntity());

            HttpGet getMethodRedirect = new HttpGet(location);
            HttpResponse redirectResponse = client.execute(getMethodRedirect);
            authResult = redirectResponse.getStatusLine().toString();
            Log.info(thisClass, methodName, "HttpGet(" + location + ") result: " + authResult);
            entity = redirectResponse.getEntity();
            getResponseRedirect = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(thisClass, methodName, "Get getResponseRedirect: " + getResponseRedirect);
            assertTrue("Did not hit expected servlet: " + PROGRAMMATIC_API_SERVLET, getResponseRedirect.contains(PROGRAMMATIC_API_SERVLET));

        } catch (Exception e) {
            Log.info(thisClass, methodName, "Caught unexpected Exception instead of successful form login: " + e.getMessage());
            fail("Caught unexpected Exception: " + e);
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
        return getResponseRedirect;
    }

}
