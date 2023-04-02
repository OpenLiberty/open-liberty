/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.authentication.filter.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.ExternalTestService;

public class CommonTest {
    private static final Class<?> c = CommonTest.class;

    public static LibertyServer myServer;
    public static BasicAuthClient myClient;
    protected static CommonTestHelper testHelper;
    protected static String TARGET_SERVER = "";

    public final static String MALFORMED_IPRANGE_SPECIFIED_CWWKS4354E = "CWWKS4354E: A malformed IP address range was specified. Found ";
    public final static String AUTHENTICATION_FILTER_ELEMENT_NOT_SPECIFIED_CWWKS4357I = "CWWKS4357I: The authFilter element is not specified in the server.xml file.";
    public final static String AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I = "CWWKS4358I: The authentication filter .* configuration was successfully processed.";
    public final static String AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I = "CWWKS4359I: The authentication filter .* configuration was successfully modified.";
    public final static String AUTHENTICATION_FILTER_MISSING_ID_ATTRIBUTE_CWWKS4360E = "CWWKS4360E: The authFilter element specified in the server.xml file is missing the required id attribute ";
    public final static String NTLM_TOKEN_RECEIVED_CWWKS4307E = "CWWKS4307E: <html><head><title>An NTLM Token was received.</title></head> ";
    public String ssoCookie = null;
    @Rule
    public TestName name = new TestName();

    /**
     * Sets up protected variables. This method also determines whether an SPN, keytab, and/or initial LTPA token
     * should be created, and whether the specified server should be started.
     *
     * @param testServerName
     * @param serverXml - Server config file within the server's configs/ directory to use. If null, a server.xml file
     *            is expected to be present in the server's root directory.
     * @param checkApps - List of apps to be validated as ready upon server start
     * @param testProps - Map of bootstrap property names and values to be set so they can be used in server
     *            configurations
     * @param startServer - Boolean indicating whether the server should be started once setup is complete
     * @throws Exception
     */
    public static void commonSetUp(String testServerName, String serverXml, List<String> checkApps, Map<String, String> testProps,
                                   boolean startServer) throws Exception {
        String thisMethod = "commonSetUp";
        Log.info(c, thisMethod, "***Starting testcase: " + testServerName + "...");

        if (testServerName != null) {
            setMyServer(LibertyServerFactory.getLibertyServer(testServerName));
        }
        testHelper = new CommonTestHelper(getMyServer(), myClient);

        String hostName = testHelper.getTestSystemFullyQualifiedDomainName();
        TARGET_SERVER = hostName;
        int hostPort = getMyServer().getHttpDefaultPort();
        Log.info(c, thisMethod, "setting up BasicauthClient with server " + hostName + "and port " + hostPort);

        myClient = new BasicAuthClient(getMyServer(), BasicAuthClient.DEFAULT_REALM, AuthFilterConstants.SIMPLE_SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);

        // Copy in the new server config
        if (serverXml != null) {
            String config = testHelper.buildFullServerConfigPath(getMyServer(), serverXml);
            testHelper.copyNewServerConfig(config);
            Log.info(c, thisMethod, "Using initial config: " + config);
        }

        getMyServer().copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");

        if (startServer) {
            testHelper.startServer(serverXml, checkApps, null);

            // Wait for feature update to complete

            serverUpdate(getMyServer());
        }
    }

    /**
     * Sets up the specified server without using any of the protected static variables in this class. This should facilitate setting
     * up servers without stepping on the feet of other test classes that extend this class. Bootstrap properties are set, the common
     * keytab file is optionally pulled in, and the server is optionally started.
     *
     * @param testServer
     * @param serverXml
     * @param checkApps
     * @param testProps
     * @param copyCommonKeytab
     * @param startServer
     * @throws Exception
     */
    public static void setupServer(LibertyServer testServer, String serverXml, List<String> checkApps, Map<String, String> testProps,
                                   boolean copyCommonKeytab, boolean startServer) throws Exception {
        String thisMethod = "setupServer";
        Log.info(c, thisMethod, "Setting up server");

        CommonTestHelper localTestHelper = new CommonTestHelper(testServer, null);

        // Copy in the new server config
        if (serverXml != null) {
            String config = localTestHelper.buildFullServerConfigPath(testServer, serverXml);
            localTestHelper.copyNewServerConfig(config);
            Log.info(c, thisMethod, "Using initial config: " + config);
        }

        testServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");

        if (startServer) {
            localTestHelper.startServer(serverXml, checkApps, null);

            // Wait for feature update to complete

            serverUpdate(getMyServer());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "commonTearDown", "Common tear down");
        if (getMyServer() != null) {
            getMyServer().stopServer(testHelper.getShutdownMessages());
        }
    }

    /**
     * Performs a call to the specified servlet that is expected to be successful using the headers provided. The
     * response received is then verified and checked to make sure the subject returned contains the user provided, as
     * well as the appropriate security roles for the user.
     *
     * @param servlet
     * @param headers
     * @param user
     * @param isEmployee
     * @param isManager
     * @return
     */
    public String successfulServletCall(String servlet, Map<String, String> headers, String user, boolean isEmployee, boolean isManager) {
        String response = myClient.accessProtectedServletWithValidHeaders(servlet, headers, AuthFilterConstants.DONT_IGNORE_ERROR_CONTENT);

        return checkExpectation(user, isEmployee, isManager, response);
    }

    public String successfulServletCall(String servlet, Map<String, String> headers, String user, boolean isEmployee, boolean isManager, boolean handleSSOCookie) {
        String response = myClient.accessProtectedServletWithValidHeaders(servlet, headers, AuthFilterConstants.DONT_IGNORE_ERROR_CONTENT, handleSSOCookie);

        return checkExpectation(user, isEmployee, isManager, response);
    }

    /**
     * @param user
     * @param isEmployee
     * @param isManager
     * @param response
     * @return
     */
    private String checkExpectation(String user, boolean isEmployee, boolean isManager, String response) {
        successfulServletResponse(response, myClient, user, isEmployee, isManager);

        myClient.resetClientState();
        return response;
    }

    /**
     * Performs a call to the specified servlet that is expected to be unsuccessful using the headers provided.
     *
     * @param servlet
     * @param headers
     * @param ignoreErrorContent - If true, the response received is expected to be null
     * @param expectedStatusCode
     * @return
     */
    public String unsuccessfulServletCall(String servlet, Map<String, String> headers, boolean ignoreErrorContent, int expectedStatusCode) {
        String response = myClient.accessProtectedServletWithInvalidHeaders(servlet, headers, ignoreErrorContent, expectedStatusCode);

        if (ignoreErrorContent) {
            assertNull("Expected response to be null, but content was found.", response);
        }

        myClient.resetClientState();
        return response;
    }

    /**
     * Creates and returns a map of headers using the common LTPA token in the Authorization header, Firefox as the
     * User-Agent header value, TARGET_SERVER as the Host header value, and null as the remote address header value.
     *
     * @return
     * @throws Exception
     */
    protected Map<String, String> createCommonHeaders() throws Exception {
        return testHelper.setTestHeaders(null, AuthFilterConstants.FIREFOX, TARGET_SERVER, null);
    }

    protected Map<String, String> createCommonHeaders(String cookie) throws Exception {
        return testHelper.setTestHeaders(cookie, AuthFilterConstants.FIREFOX, TARGET_SERVER, null);
    }

    /**
     * @return the myServer
     */
    public static LibertyServer getMyServer() {
        return myServer;
    }

    /**
     * @param myServer the myServer to set
     */
    public static void setMyServer(LibertyServer myServer) {
        CommonTest.myServer = myServer;
    }

    /**
     * Release a collection of Consul test services.
     *
     * @param services The services to release.
     */
    public static void releaseServices(Collection<ExternalTestService> services) {
        if (services != null) {
            for (ExternalTestService service : services) {
                service.release();
            }
        }
    }

    /**
     * Returns a map with the "Authorization" header set to "Negotiate " + the common LTPA token, "User-Agent" set to Firefox,
     * and "Host" set to TARGET_SERVER. In addition, a "Cookie" header with its value set to the specified
     * value is included in the map.
     *
     * @param ssoCookie
     * @return
     * @throws Exception
     */
    public Map<String, String> getCommonHeadersWithSSOCookie(String ssoCookie) throws Exception {
        return getCommonHeadersWithSSOCookie(ssoCookie, AuthFilterConstants.FIREFOX);
    }

    public Map<String, String> getCommonHeadersWithSSOCookie(String ssoCookie, String userAgent) throws Exception {
        return setTestHeaders(ssoCookie, userAgent, TARGET_SERVER, null);
    }

    public Map<String, String> setTestHeaders(String ssoCookie, String userAgent, String host, String remoteAddr) {
        Map<String, String> headers = new HashMap<String, String>();
        if (ssoCookie != null) {
            headers.put(AuthFilterConstants.COOKIE, AuthFilterConstants.SSO_COOKIE_NAME + "=" + ssoCookie);
        }
        if (userAgent != null) {
            headers.put(AuthFilterConstants.HEADER_USER_AGENT, userAgent);
        }
        if (host != null) {
            headers.put(AuthFilterConstants.HEADER_HOST, host);
        }
        if (remoteAddr != null) {
            headers.put(AuthFilterConstants.HEADER_REMOTE_ADDR, remoteAddr);
        }
        return headers;
    }

    public void commonSuccessfulLtpaServletCall(String ssoCookie) throws Exception {
        commonSuccessfulLtpaServletCall(ssoCookie, AuthFilterConstants.FIREFOX);
    }

    public void commonSuccessfulLtpaServletCall(String ssoCookie, String userAgent) throws Exception {
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);
        Log.info(c, name.getMethodName(), "Successfull accessing servlet using valid SSO LTPA cookie");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0, AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
    }

    public void commonUnsuccessfulLtpaServletCall(String ssoCookie) throws Exception {
        commonUnsuccessfulLtpaServletCall(ssoCookie, AuthFilterConstants.FIREFOX);
    }

    public void commonUnsuccessfulLtpaServletCall(String ssoCookie, String userAgent) throws Exception {
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);
        Log.info(c, name.getMethodName(), "Unsuccessfull accessing servlet using valid SSO LTPA cookie");
        unsuccessfulLtpaServletCall(headers);
    }

    public String unsuccessfulLtpaServletCall(Map<String, String> headers) {
        String response = unsuccessfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, true, 401);
        return response;
    }

    public void successfulLtpaServletCall(Map<String, String> headers) {
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0, AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
    }

    public void verifySSOCookiePresent(String ssoCookie) {
        assertNotNull("Did not obtain SSO cookie despite successfully accessing protected resource.", ssoCookie);

    }

    public static void serverUpdate(LibertyServer myServer) {
        assertNotNull("FeatureManager did not report update was complete", myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Application did not start", myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("LTPA configuration did not report it was ready", myServer.waitForStringInLog("CWWKS4105I"));
    }

    public void successfulServletResponse(String response, BasicAuthClient myClient, String user, boolean isEmployee, boolean isManager) {
        assertTrue("Expected to receive a successful response but found a problem.", myClient.verifyResponse(response, user, isEmployee, isManager));
    }

    /**
     * @param ex
     */
    public void logFailException(Exception ex) {
        Log.info(c, name.getMethodName(), "Unexpected exception: " + ex.getMessage());
        fail("Exception was thrown: " + ex.getMessage());
    }

    public String getAndAssertSSOCookieForUser(String user, String password, boolean isEmployee, boolean isManager) {
        String SERVLET_NAME = "AllRoleServlet";
        String SERVLET = "/" + SERVLET_NAME;
        Log.info(c, name.getMethodName(), "Accessing servlet in order to obtain SSO cookie for user: " + user);
        BasicAuthClient basicAuthClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        String response = basicAuthClient.accessProtectedServletWithAuthorizedCredentials(SERVLET, user, password);
        basicAuthClient.verifyResponse(response, user, isEmployee, isManager);

        String ssoCookie = basicAuthClient.getCookieFromLastLogin();

        verifySSOCookiePresent(ssoCookie);

        return ssoCookie;
    }
}
