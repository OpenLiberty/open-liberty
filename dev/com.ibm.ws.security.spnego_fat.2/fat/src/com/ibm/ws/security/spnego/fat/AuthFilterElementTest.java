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
package com.ibm.ws.security.spnego.fat;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.MessageConstants;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
public class AuthFilterElementTest extends CommonTest {

    private static final Class<?> c = AuthFilterElementTest.class;
    private final String LTPA_COOKIE_NAME = "LtpaToken2";

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";
        Log.info(c, thisMethod, "Starting the server and kerberos setup ...");
        commonSetUp("AuthFilterElementTest");
    }

    @Before
    public void generateSpnegoToken() throws Exception {
        if (FATSuite.OTHER_SUPPORT_JDKS) {
            createNewSpnegoToken(SPNEGOConstants.CREATE_SPNEGO_TOKEN);
        }
    }

    public void generateSpnegoTokenInsideTest() throws Exception {
        if (FATSuite.OTHER_SUPPORT_JDKS) {
            createNewSpnegoToken(SPNEGOConstants.CREATE_SPNEGO_TOKEN);
        }
    }

    /**
     * Test description:
     * - Set server.xml AuthFilterMatchType to notContain.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - We should see validation messages for requestURL and show it has default value.
     */

    @Test
    public void testAuthFilterRequestUrlMatchTypeToNotContain() {
        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKG0032W");
            testHelper.reconfigureServer("malformedrequestURLmatchType.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKG0032W");
            // check message log for error message will be done in reconfigureServer call

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a malformed ip in the remoteAddress element.
     * - Restart the server and check log results for error message.
     *
     * Expected results:
     * - Should have an error message for "malformed ip range".
     */

    @Test
    @AllowedFFDC({ "java.net.UnknownHostException", "com.ibm.ws.security.authentication.filter.internal.FilterException" })
    public void testRemoteAddressWithMalformedIp() {
        try {
            testHelper.reconfigureServer("remoteAddressWithMalformedIp.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.MALFORMED_IPRANGE_SPECIFIED_CWWKS4354E);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the authFilter element with no elements specified.
     * - Restart the server and check log results for error message.
     *
     * Expected results:
     * - Should have an error message for "authFilter not configured".
     */

    @Test
    public void testNoAuthFilterElementSpecified() {
        try {
            testHelper.reconfigureServer("authFilterRefNoElementSpecified.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_ELEMENT_NOT_SPECIFIED_CWWKS4357I);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a non existant authFilter element.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - Should have a validation message for authFilterRef and all will use SPNEGO.
     */

    @Test
    public void testAuthFilterRefNonExistent() {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), SPNEGOConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKG0033W");
            testHelper.reconfigureServer("authFilterRefNonExistent.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKG0033W");
            // check message log for error message will be done in reconfigureServer call

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have valid values for requestURL, host and UserAgent.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsingRequestUrl_Host_UserAgent_Valid() {
        try {
            testHelper.reconfigureServer("authFilterUsingRequestUrl_Host_UserAgent_Valid.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            successfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml to use RequestUrl with NotContain, Host with Contains and UserAgent with NotContain.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testRequestUrlNotContain_HostContains_UserAgentNotContain() {
        try {
            // Add bootstrap properties
            Map<String, String> sysProps = new HashMap<String, String>();
            sysProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_HOST_NAME, TARGET_SERVER);
            testHelper.addBootstrapProperties(myServer, sysProps);

            testHelper.reconfigureServer("requestUrlNotContain_HostContains_UserAgentNotContain.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have valid values for requestUrl, remoteAddress, host, userAgent and webApp elements.
     * - Restart the server and make a SPNEGO servlet call.
     * - Check log results for validation of configuration.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testAllAuthFilterElements() {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), SPNEGOConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }

        try {
            testHelper.reconfigureServer("authFilterRefAllElements.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            commonSuccessfulSpnegoServletCall();

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to use RequestUrl, Host, UserAgent.
     * - Restart the server and check log results for validation of configuration.
     * - The request should fail since we try to use FireFox but the userAgent is configured for IE.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsingRequestUrl_Host_UserAgent_ClientSendsWrongUserAgent() {
        try {
            testHelper.reconfigureServer("authFilterUsingRequestUrl_Host_UserAgent_Valid.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // check message log for config message will be done in reconfigureServer call
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have all valid authFilter attributes, but set userAgent to unexpected value.
     * - Restart the server and check log results for validation of configuration.
     * - The request should fail since we try to use FireFox but the userAgent is configured for IE.
     *
     * Expected results:
     * - We should fail to get access SPNEGO protected resource (FirFox sent expecting IE).
     */

    @Test
    public void testRequestUrlNotContain_HostContains_UserAgentNotContain_ClientSendsWrongUserAgent() {
        try {
            testHelper.reconfigureServer("requestUrlNotContain_HostContains_UserAgentNotContain.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // check message log for config message will be done in reconfigureServer call
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            unsuccessfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have all valid authFilter attributes, set skipForUnprotectedURI to false.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - We should fail to get access SPNEGO protected resource on null token
     * should have successful access when using valid token.
     */

    @Test
    public void testSkipForUnprotectedURI_False_sendBasicAuthHeader() {
        try {
            Integer expectedStatusCode = 401;
            testHelper.reconfigureServer("skipForUnprotectedURIFalse.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            if (FATSuite.OTHER_SUPPORT_JDKS) {
                createNewSpnegoToken(SPNEGOConstants.SET_AS_COMMON_TOKEN);
            }
            // check message log for config message will be done in reconfigureServer call

            //call with no headers to make sure you get 401 on unprotected servlet with SPNEGO enabled for it
            Map<String, String> headers = testHelper.setTestHeaders("Basic " + null, SPNEGOConstants.IE, TARGET_SERVER, null);
            String response = unsuccessfulServletCall("/UnprotectedSimpleServlet", headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, expectedStatusCode);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);

            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

            expectation.responseShouldContainSpnegoErrorCode(response);

            //call with headers to make sure you get 200 on unprotected servlet with SPNEGO enabled for it
            headers = testHelper.setTestHeaders("Basic " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            response = successfulServletCall("/UnprotectedSimpleServlet", headers, FATSuite.COMMON_TOKEN_USER,
                                             FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE, FATSuite.COMMON_TOKEN_USER_IS_MANAGER);

            Log.info(c, name.getMethodName(), "Servlet response: " + response);

            expectation.responseShouldContaiGSSCredentials(response);

            expectation.responseShouldContainCorrectGSSCredOwner(response);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Starting 19.0.2 we now expect to receive a 200 when sending a Negotiate Header.
     * - However since the header is null we will still cosider it as an invalid servlet call.
     * - The test is only looking to confirm that a 200 is set when sending a Negotiate and skipURI is set.
     * - Set server xml file to have all valid authFilter attributes, set skipForUnprotectedURI to false.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - We should fail to get access SPNEGO protected resource on null token
     * should have successful access when using valid token.
     */

    @Test
    public void testSkipForUnprotectedURI_False_SendNegotiateHeader() {
        try {
            Integer expectedStatusCode = 200;
            testHelper.reconfigureServer("skipForUnprotectedURIFalse.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            if (FATSuite.OTHER_SUPPORT_JDKS) {
                createNewSpnegoToken(SPNEGOConstants.SET_AS_COMMON_TOKEN);
            }
            // check message log for config message will be done in reconfigureServer call

            //call with no headers to make sure you get 401 on unprotected servlet with SPNEGO enabled for it
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + null, SPNEGOConstants.IE, TARGET_SERVER, null);
            String response = unsuccessfulServletCall("/UnprotectedSimpleServlet", headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, expectedStatusCode);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have webApp element set with matchType contains.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testUseOfWebAppAttribute_Contains() {
        try {
            testHelper.reconfigureServer("webAppContainsBasicAuth_webAppNotContainFormLogin.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKZ0014W");
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have webApp attribute set and matchType equals.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testUseOfWebAppAttribute_Equals() {
        try {
            testHelper.reconfigureServer("webAppEquals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have webApp attribute set with matchType notContain.
     * - Restart the server and check log results for validation of configuration.
     * - We will also invoke servlet with BasicAuth credentials after SPNEGO failure.
     *
     * Expected results:
     * - We shouldn't be able to access resource (it will not be protected by SPNEGO).
     */

    @Test
    public void testUseOfWebAppAttribute_NotContain() {
        try {
            testHelper.reconfigureServer("webAppNotEquals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

            String response = myClient.accessProtectedServletWithAuthorizedCredentials(SPNEGOConstants.SIMPLE_SERVLET, InitClass.FIRST_USER, InitClass.FIRST_USER_PWD);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);
            myClient.resetClientState();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description: Checking that SPNEGO can fail over to App Auth type.
     * - Update the server configuration to enable SPNEGO and enable fail over.
     * - Create the header with no SPNEO token.
     * - Access a protected resource.
     *
     * Expected results:
     * - Authentication should fail, and the should fail over to App Auth.
     */
    // @Test   We will come back for this test on 2q15 under task 158241.
    //The reason for this is that The fail over test could not be automated,
    //it needs to have the Negotiate in the http header and also the basicauth
    //information, and we weren't sure how to do that
    public void testSpnegoFailOverToBasicAuth() {
        try {
            testHelper.reconfigureServer("failOverToBasicAuth.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/basicauth" + SPNEGOConstants.SIMPLE_SERVLET;
            Map<String, String> headers = null;
            String response = myClient.accessWithHeaders(url, 401, headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, SPNEGOConstants.HANDLE_SSO_COOKIE);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);
            myClient.resetClientState();
            // Now test that we get to BasicAuth
            response = myClient.accessProtectedServletWithAuthorizedCredentials(SPNEGOConstants.SIMPLE_SERVLET, InitClass.FIRST_USER, InitClass.FIRST_USER_PWD);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);
            myClient.resetClientState();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have all multiple requestURL authFilter attributes.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource, for all in contain list
     * and not for the one in notContain list.
     */

    @Test
    public void testAuthFilterWithMultipleRequestURL() {
        try {
            // Add bootstrap properties
            Map<String, String> sysProps = new HashMap<String, String>();
            sysProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_HOST_NAME, TARGET_SERVER);
            testHelper.addBootstrapProperties(myServer, sysProps);

            testHelper.reconfigureServer("authFilterMultipleRequestUrl.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            String response = successfulSpnegoServletCall(headers, FATSuite.COMMON_TOKEN_USER,
                                                          FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE, FATSuite.COMMON_TOKEN_USER_IS_MANAGER);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);

            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

            if (FATSuite.OTHER_SUPPORT_JDKS) {
                generateSpnegoTokenInsideTest();
                headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            }

            BasicAuthClient myClient2 = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, "/EmployeeRoleServlet", BasicAuthClient.DEFAULT_CONTEXT_ROOT);
            if (FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE) {
                response = myClient2.accessProtectedServletWithValidHeaders("/EmployeeRoleServlet", headers);
            } else {
                response = myClient2.accessProtectedServletWithInvalidHeaders("/EmployeeRoleServlet", headers, false, HttpServletResponse.SC_FORBIDDEN);

            }
            Log.info(c, name.getMethodName(), "Servlet response: " + response);

            if (FATSuite.OTHER_SUPPORT_JDKS) {
                generateSpnegoTokenInsideTest();
                headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            }

            BasicAuthClient myClient3 = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, "/AllRoleServlet", BasicAuthClient.DEFAULT_CONTEXT_ROOT);
            response = myClient3.accessProtectedServletWithValidHeaders("/AllRoleServlet", headers);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);

            if (FATSuite.OTHER_SUPPORT_JDKS) {
                generateSpnegoTokenInsideTest();
                headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            }

            // The ManagerRoleServlet is not included in the auth filter, so this request should not be handled by SPNEGO
            BasicAuthClient myClient4 = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, "/ManagerRoleServlet", BasicAuthClient.DEFAULT_CONTEXT_ROOT);
            response = myClient4.accessProtectedServletWithInvalidHeaders("/ManagerRoleServlet", headers, false);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have 2 values for remoteAddressRef, hostRef and webAppRef.
     * - Make a SPNEGO servlet call, by default "basicauth" web app is used.
     * - Create a url which contains "spnegoauth" web app.
     * - Access a protected resource by including the url created.
     *
     * Expected results:
     * - Authentication to "basicauth" should be successful and access to the protected resource should be granted.
     * - Authentication to "spnegoauth" should fail, resulting in a 401.
     */

    @Test
    public void testAuthFiltersMultipleElements_For_RemoteAddress_Host_WebApp() {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), SPNEGOConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }

        try {
            testHelper.reconfigureServer("authFiltersMultipleElements_RemoteAddress_Host_WebApputhFilterMultipleRequestUrl.xml", name.getMethodName(),
                                         SPNEGOConstants.RESTART_SERVER);

            // Call to the SPNEGO servlet that is expected to be successful
            commonSuccessfulSpnegoServletCall();

            // Access a protected resource, it is expected to be unsuccessful since the web app must not contain 'spnegoauth'
            String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/spnegoauth" + SPNEGOConstants.SIMPLE_SERVLET;
            myClient.accessWithHeaders(url, 401, createCommonHeaders(), SPNEGOConstants.IGNORE_ERROR_CONTENT, SPNEGOConstants.DONT_HANDLE_SSO_COOKIE);
            myClient.resetClientState();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have remoteAddress ip attribute set with wildcard.
     * - Access a protected resource.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     */

    @Test
    public void testRemoteAddressIPUsingWildcard() {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), SPNEGOConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }

        try {
            testHelper.reconfigureServer("remoteAddress_Ip_Wildcard.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have remoteAddress element set with ip=127.0.0.0 and matchType=greaterThan.
     * - Make a SPNEGO servlet call.
     * - Set server xml file to have remoteAddress element set with ip=127.0.0.2 and matchType=greaterThan.
     * - Make a SPNEGO servlet call.
     * - Set server xml file to have remoteAddress element set with ip=127.0.0.2 and matchType=lessThan.
     * - Make a SPNEGO servlet call.
     * - Set server xml file to have remoteAddress element set with ip=127.0.0.0 and matchType=lessThan.
     * - Make a SPNEGO servlet call.
     *
     * Expected results:
     * - Using ip 127.0.0.0 and matchType greaterThan, authentication should be successful and access to the protected resource should be granted.
     * - Using ip 127.0.0.2 and matchType greaterThan, authentication should fail resulting in a 401.
     * - Using ip 127.0.0.2 and matchType lessThan, authentication should be successful and access to the protected resource should be granted.
     * - Using ip 127.0.0.0 and matchType lessThan, authentication should fail resulting in a 401.
     */

    @Test
    public void testRemoteAddressIPRange() {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), SPNEGOConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }

        try {
            testHelper.reconfigureServer("remoteAddress_Ip_GreaterThan_Valid.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // It is expected a remote address ip greater than the specified in the server.xml file,
            // since our local ip 127.0.0.1 is greater than 127.0.0.0 the SPNEGO servlet call must be successful
            commonSuccessfulSpnegoServletCall();

            testHelper.reconfigureServer("remoteAddress_Ip_GreaterThan_Invalid.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // It is expected a remote address ip greater than the specified in the server.xml file,
            // since our local ip 127.0.0.1 is less than 127.0.0.2 the SPNEGO servlet call must be unsuccessful
            commonUnsuccessfulSpnegoServletCall(401);

            testHelper.reconfigureServer("remoteAddress_Ip_LessThan_Valid.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // It is expected a remote address ip less than the specified in the server.xml file,
            // since our local ip 127.0.0.1 is less than 127.0.0.2 the SPNEGO servlet call must be successful
            commonSuccessfulSpnegoServletCall();

            testHelper.reconfigureServer("remoteAddress_Ip_LessThan_Invalid.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // It is expected a remote address ip less than the specified in the server.xml file,
            // since our local ip 127.0.0.1 is greater than 127.0.0.0 the SPNEGO servlet call must be unsuccessful
            commonUnsuccessfulSpnegoServletCall(401);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a filter with an email element configured to equals.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with valid header which includes email.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Equals_Valid() {
        try {
            testHelper.reconfigureServer("requestHeaderEmail_Equals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_EMAIL, "sample@email.com");
            successfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a filter with an email element configured to contains.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with valid header. The header will have a valid email.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Contains_Valid() {
        try {
            testHelper.reconfigureServer("requestHeaderEmail_Equals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_EMAIL, "sample@email.com");
            successfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a filter with an email element configured to not contains.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with valid header. The header will have a valid email.
     *
     * Expected results:
     * - Successfully access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Not_Contains_Valid() {
        try {
            testHelper.reconfigureServer("requestHeaderEmail_Not_Contains.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKG0032W");
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_EMAIL, "mysample@email.com");
            successfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_contains_Valid() {
        try {
            testHelper.reconfigureServer("requestHeader_nameNoValue_contains.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKG0032W");
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_NAME_NO_VALUE, "myCustomData");
            successfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_contains_inValid() {
        try {
            testHelper.reconfigureServer("requestHeader_nameNoValue_contains.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_NAME_NO_VALUE_INVALID, "myInvalidData");
            unsuccessfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_equals_Valid() {
        try {
            testHelper.reconfigureServer("requestHeader_nameNoValue_equals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_NAME_NO_VALUE, "myCustomData");
            successfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_equals_invalid() {
        try {
            testHelper.reconfigureServer("requestHeader_nameNoValue_equals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_NAME_NO_VALUE_INVALID, "myInvalidData");
            unsuccessfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_notContain_Valid() {
        try {
            testHelper.reconfigureServer("requestHeader_nameNoValue_notContain.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_EMAIL, "mysample@email.com");
            successfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_notContain_invalid() {
        try {
            testHelper.reconfigureServer("requestHeader_nameNoValue_notContain.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_NAME_NO_VALUE, "myCustomData");
            unsuccessfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a filter that has the wrong name in the email element configuration.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with valid header. The header will have a valid email.
     *
     * Expected results:
     * - Unable to access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Diff_Name() {
        try {
            testHelper.reconfigureServer("requestHeaderEmail_Diff_Name.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_EMAIL, "sample@email.com");
            unsuccessfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a filter with an email element configured to equals.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with an valid header. The header will have an invalid email.
     *
     * Expected results:
     * - Unable to access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Equals_NotValid() {
        try {
            testHelper.reconfigureServer("requestHeaderEmail_Equals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_EMAIL, "incorrect@email.com");
            unsuccessfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a filter with an email element configured to contains.
     * - The email address set in the header is not supported by the contain element.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with an valid header. The header will have an invalid email.
     *
     * Expected results:
     * - Unable to access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Contains_InValid() {
        try {
            testHelper.reconfigureServer("requestHeaderEmail_Equals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_EMAIL, "mysample@email.com");
            unsuccessfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a filter with an email element configured to not contains.
     * - The header will contain the email address specified in the not contain auth filter configuration.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with an valid header. The header will have an invalid email.
     *
     * Expected results:
     * - Unable to access SPNEGO protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Not_Contains_InValid() {
        try {
            testHelper.reconfigureServer("requestHeaderEmail_Not_Contains.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKG0032W");
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put(SPNEGOConstants.HEADER_EMAIL, "sample@email.com");
            successfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have authfilter config with cookie element set to equals, LTPAToken2.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with an valid header. The header will have an LTPAToken2.
     *
     * Expected results:
     * - Successfully able to access SPNEGO protected resource.
     */
    @Test
    public void testAuthFilterUsing_Cookie_LTPA_Equals_Valid() {
        try {
            testHelper.reconfigureServer("cookieLTPA_Equals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
            String ssoCookie = getAndAssertSSOCookieForUser(InitClass.Z_USER, InitClass.Z_USER_PWD, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            // Add the SSO cookie as a header, in addition to the SPNEGO token, and submit the request
            Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using valid SSO cookie and valid SPNEGO token headers");
            successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, InitClass.Z_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have authfilter config with cookie element set to contains, LTPAToken2.
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with an valid header. The header will have an LTPAToken2.
     *
     * Expected results:
     * - Successfully able to access SPNEGO protected resource.
     */
    @Test
    public void testAuthFilterUsing_Cookie_LTPA_Contains() {
        try {
            testHelper.reconfigureServer("cookieLTPA_Contains.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
            String ssoCookie = getAndAssertSSOCookieForUser(InitClass.Z_USER, InitClass.Z_USER_PWD, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            // Add the SSO cookie as a header, in addition to the SPNEGO token, and submit the request
            Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using valid SSO cookie and valid SPNEGO token headers");
            successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, InitClass.Z_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have authfilter config with cookie element set to not contain, LTPAToken2.
     * - Restart the server and check log results for validation of configuration.
     * - Since LTPA is present we are expected to use it to get through the resource.
     * Expected results:
     * - We are able to access the resource but no GSS Credentials.
     *
     */
    @Test
    public void testAuthFilterUsing_Cookie_LTPA_NotContains() {
        try {
            testHelper.reconfigureServer("cookieLTPA_NotContain.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
            String ssoCookie = getAndAssertSSOCookieForUser(InitClass.Z_USER, InitClass.Z_USER_PWD, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            // Add the SSO cookie as a header, in addition to the SPNEGO token, and submit the request
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put("Cookie", LTPA_COOKIE_NAME + "=" + ssoCookie);
            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using valid SSO cookie and valid SPNEGO token headers");
            successfulSpnegoServletCall(headers, InitClass.Z_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER, SPNEGOConstants.NO_GSS_CREDENTIALS_PRESENT);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have authfilter config with cookie element set to equals
     * - Restart the server and check log results for validation of configuration.
     * - Try to access SPNEGO servlet with an valid header. The header will have an invalid LTPAToken2.
     *
     * Expected results:
     * - Unable to access SPNEGO protected resource.
     */
    @Test
    public void testAuthFilterUsing_Cookie_LTPA_Equals_InValid() {
        try {
            testHelper.reconfigureServer("cookieLTPA_Equals.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.IE, TARGET_SERVER, null);
            headers.put("Cookie", LTPA_COOKIE_NAME + "=" + "someLTPAToken2");
            unsuccessfulSpnegoServletCall(headers);
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Accesses a protected servlet using the given authorized credentials, verifies the response against the user,
     * asserts that an SSO cookie was created, and returns the SSO cookie.
     *
     * @param user
     * @param password
     * @param isEmployee
     * @param isManager
     * @return
     */
    private String getAndAssertSSOCookieForUser(String user, String password, boolean isEmployee, boolean isManager) {
        String SSO_SERVLET_NAME = "AllRoleServlet";
        String SSO_SERVLET = "/" + SSO_SERVLET_NAME;
        Log.info(c, name.getMethodName(), "Accessing servlet in order to obtain SSO cookie for user: " + user);
        BasicAuthClient ssoClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, SSO_SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        String response = ssoClient.accessProtectedServletWithAuthorizedCredentials(SSO_SERVLET, user, password);
        ssoClient.verifyResponse(response, user, isEmployee, isManager);

        String ssoCookie = ssoClient.getCookieFromLastLogin();

        expectation.isSSOCookiePresent(ssoCookie);

        return ssoCookie;
    }

    /**
     * Returns a map with the "Authorization" header set to "Negotiate " + the common SPNEGO token, "User-Agent" set to Firefox,
     * and "Host" set to TARGET_SERVER. In addition, a "Cookie" header with its value set to the specified
     * value is included in the map.
     *
     * @param ssoCookie
     * @return
     * @throws Exception
     */
    private Map<String, String> getCommonHeadersWithSSOCookie(String ssoCookie) throws Exception {
        String SSO_COOKIE_NAME = "LtpaToken2";
        Map<String, String> headers = createCommonHeaders();
        headers.put("Cookie", SSO_COOKIE_NAME + "=" + ssoCookie);
        return headers;
    }
}
