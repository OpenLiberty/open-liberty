/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AuthFilterElementTest extends CommonTest {

    private static final Class<?> c = AuthFilterElementTest.class;
    private static boolean checkForAuthFilterMsg = false;
    private final String LTPA_COOKIE_NAME = "LtpaToken2";

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";
        Log.info(c, thisMethod, "Starting the server ...");
        CommonTest.commonSetUp("AuthFilterElementTest", null, AuthFilterConstants.NO_APPS, AuthFilterConstants.NO_PROPS,
                               AuthFilterConstants.START_SERVER);
    }

    @Before
    public void obtainSSOCookie() throws Exception {
        ssoCookie = getAndAssertSSOCookieForUser(AuthFilterConstants.USER0, AuthFilterConstants.USER0_PWD,
                                                 AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
    }

    /**
     * Test description: - Set server.xml AuthFilterMatchType to notContain. -
     * Restart the server and check log results for validation of configuration.
     *
     * Expected results: - We should see validation messages for requestURL and
     * show it has default value.
     */

    @Test
    public void testAuthFilterRequestUrlMatchTypeToNotContain() throws Exception {
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKG0032W");
        // check message log for error message will be done in reconfigureServer
        // call
        testHelper.reconfigureServer("malformedrequestURLmatchType.xml", name.getMethodName(), startMsgs,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        testHelper.setShutdownMessages("CWWKG0032W");
    }

    /**
     * Test description: - Set server xml file to have a malformed ip in the
     * remoteAddress element. - Restart the server and check log results for
     * error message.
     *
     * Expected results: - Should have an error message for "malformed ip
     * range".
     */

    // @Test
    @AllowedFFDC({ "java.net.UnknownHostException",
                   "com.ibm.ws.security.authentication.filter.internal.FilterException" })
    public void testRemoteAddressWithMalformedIp() throws Exception {
        testHelper.reconfigureServer("remoteAddressWithMalformedIp.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);
        successfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, MALFORMED_IPRANGE_SPECIFIED_CWWKS4354E);
    }

    /**
     * Test description: - Set server xml file to have the authFilter element
     * with no elements specified. - Restart the server and check log results
     * for error message.
     *
     * Expected results: - Should have an error message for "authFilter not
     * configured".
     */

    @Test
    public void testNoAuthFilterElementSpecified() throws Exception {
        testHelper.reconfigureServer("authFilterRefNoElementSpecified.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);
        successfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_ELEMENT_NOT_SPECIFIED_CWWKS4357I);
    }

    /**
     * Test description: - Set server xml file to have a non existant authFilter
     * element. - Restart the server and check log results for validation of
     * configuration.
     *
     * Expected results: - Should have a validation message for authFilterRef
     * and all will use SSO LTPA.
     */

    @Test
    public void testAuthFilterRefNonExistent() throws Exception {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), AuthFilterConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKG0033W");
        testHelper.reconfigureServer("authFilterRefNonExistent.xml", name.getMethodName(), startMsgs,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        testHelper.setShutdownMessages("CWWKG0033W");
    }

    /**
     * Test description: - Set server xml file to have valid values for
     * requestURL, host and UserAgent. - Restart the server and check log
     * results for validation of configuration.
     *
     * Expected results: - Successfully access a protected resource.
     */

    @Test
    public void testAuthFilterUsingRequestUrl_Host_UserAgent_Valid() throws Exception {
        testHelper.reconfigureServer("authFilterUsingRequestUrl_Host_UserAgent_Valid.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);
        Log.info(c, name.getMethodName(), "Accessing servlet using valid SSO LTPA cookie");
        successfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml to use RequestUrl with NotContain,
     * Host with Contains and UserAgent with NotContain. - Restart the server
     * and check log results for validation of configuration.
     *
     * Expected results: - Successfully access a protected resource.
     */

    @Test
    public void testRequestUrlNotContain_HostContains_UserAgentNotContain() throws Exception {
        addHostnameVariable();

        testHelper.reconfigureServer("requestUrlNotContain_HostContains_UserAgentNotContain.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);
        successfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have valid values for
     * requestUrl, remoteAddress, host, userAgent and webApp elements. - Restart
     * the server and make a servlet call. - Check log results for validation of
     * configuration.
     *
     * Expected results: - Successfully access a protected resource.
     */

    @Test
    public void testAllAuthFilterElements() throws Exception {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), AuthFilterConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }
        addHostnameVariable();

        testHelper.reconfigureServer("authFilterRefAllElements.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);

        commonSuccessfulLtpaServletCall(ssoCookie, AuthFilterConstants.IE);

        List<String> checkMsgs = new ArrayList<String>();
        checkMsgs.add(AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
        testHelper.waitForMessages(checkMsgs, true);
    }

    /**
     * Test description: - Set server xml file to use RequestUrl, Host,
     * UserAgent. - Restart the server and check log results for validation of
     * configuration. - The request should fail since we try to use FireFox but
     * the userAgent is configured for IE.
     *
     * Expected results: - Successfully access a protected resource.
     */

    @Test
    public void testAuthFilterUsingRequestUrl_Host_UserAgent_ClientSendsWrongUserAgent() throws Exception {
        testHelper.reconfigureServer("authFilterUsingRequestUrl_Host_UserAgent_Valid.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie, AuthFilterConstants.IE);
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have all valid authFilter
     * attributes, but set userAgent to unexpected value. - Restart the server
     * and check log results for validation of configuration. - The request
     * should fail since we try to use FireFox but the userAgent is configured
     * for IE.
     *
     * Expected results: - We should fail to get access a protected resource
     * (FirFox sent expecting IE).
     */

    @Test
    public void testRequestUrlNotContain_HostContains_UserAgentNotContain_ClientSendsWrongUserAgent() throws Exception {
        testHelper.reconfigureServer("requestUrlNotContain_HostContains_UserAgentNotContain.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Starting 19.0.2 we now expect to receive a 200 when
     * sending a Negotiate Header. - However since the header is null we will
     * still cosider it as an invalid servlet call. - The test is only looking
     * to confirm that a 200 is set when sending a Negotiate and skipURI is set.
     * - Set server xml file to have all valid authFilter attributes, set
     * skipForUnprotectedURI to false. - Restart the server and check log
     * results for validation of configuration.
     *
     * Expected results: - We should fail to get access a protected resource on
     * null token should have successful access when using valid token.
     */

    // @Test
    public void testSkipForUnprotectedURI_False_SendNegotiateHeader() throws Exception {
        Integer expectedStatusCode = 200;
        testHelper.reconfigureServer("skipForUnprotectedURIFalse.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + null, AuthFilterConstants.IE,
                                                                TARGET_SERVER, null);
        String response = unsuccessfulServletCall("/UnprotectedSimpleServlet", headers,
                                                  AuthFilterConstants.DONT_IGNORE_ERROR_CONTENT, expectedStatusCode);
        Log.info(c, name.getMethodName(), "Servlet response: " + response);
    }

    /**
     * Test description: - Set server xml file to have webApp element set with
     * matchType contains. - Restart the server and check log results for
     * validation of configuration.
     *
     * Expected results: - Successfully access protected resource.
     */

    @Test
    public void testUseOfWebAppAttribute_Contains() throws Exception {
        testHelper.reconfigureServer("webAppContainsBasicAuth_webAppNotContainFormLogin.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        testHelper.setShutdownMessages("CWWKZ0014W");
        commonSuccessfulLtpaServletCall(ssoCookie);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have webApp attribute set and
     * matchType equals. - Restart the server and check log results for
     * validation of configuration.
     *
     * Expected results: - Successfully access protected resource.
     */

    @Test
    public void testUseOfWebAppAttribute_Equals() throws Exception {
        testHelper.reconfigureServer("webAppEquals.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have webApp attribute set with
     * matchType notContain. - Restart the server and check log results for
     * validation of configuration. - We will also invoke servlet with BasicAuth
     * credentials.
     *
     * Expected results: - We shouldn't be able to access resource.
     */

    @Test
    public void testUseOfWebAppAttribute_NotContain() throws Exception {
        testHelper.reconfigureServer("webAppNotEquals.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);

        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        unsuccessfulLtpaServletCall(headers);

        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have all multiple requestURL
     * authFilter attributes. - Restart the server and check log results for
     * validation of configuration.
     *
     * Expected results: - Successfully access protected resource, for all in
     * contain list and not for the one in notContain list.
     */

    @Test
    public void testAuthFilterWithMultipleRequestURL() throws Exception {
        addHostnameVariable();

        testHelper.reconfigureServer("authFilterMultipleRequestUrl.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "sample@email.com");
        String response = successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                                                AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);

        Log.info(c, name.getMethodName(), "Servlet response: " + response);

        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have 2 values for
     * remoteAddressRef, hostRef and webAppRef. - Make a servlet call, by
     * default "basicauth" web app is used. - Create a url which contains
     * "basicauth" web app. - Access a protected resource by including the url
     * created.
     *
     * Expected results: - Authentication to "basicauth" should be successful
     * and access to the protected resource should be granted. - Authentication
     * to "basicauth" should fail, resulting in a 401.
     */

    @Test
    public void testAuthFiltersMultipleElements_For_RemoteAddress_Host_WebApp() throws Exception {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), AuthFilterConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }
        addHostnameVariable();

        testHelper.reconfigureServer(
                                     "authFiltersMultipleElements_RemoteAddress_Host_WebApputhFilterMultipleRequestUrl.xml",
                                     name.getMethodName(), AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);

        commonSuccessfulLtpaServletCall(ssoCookie);

        // Access a protected resource, it is expected to be unsuccessful since
        // the web app must not contain 'basicauth'
        String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/basicauth"
                     + AuthFilterConstants.SIMPLE_SERVLET;
        myClient.accessWithHeaders(url, 401, createCommonHeaders(), AuthFilterConstants.IGNORE_ERROR_CONTENT,
                                   AuthFilterConstants.DONT_HANDLE_SSO_COOKIE);
        myClient.resetClientState();
    }

    /**
     * @throws Exception
     */
    private void addHostnameVariable() throws Exception {
        // Add bootstrap properties
        Map<String, String> sysProps = new HashMap<String, String>();
        sysProps.put(AuthFilterConstants.PROP_TEST_SYSTEM_HOST_NAME, TARGET_SERVER);
        testHelper.addBootstrapProperties(myServer, sysProps);
    }

    /**
     * Test description: - Set server xml file to have remoteAddress ip
     * attribute set with wildcard. - Access a protected resource.
     *
     * Expected results: - Authentication should be successful and access to the
     * protected resource should be granted.
     */

    @Test
    public void testRemoteAddressIPUsingWildcard() throws Exception {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), AuthFilterConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }
        testHelper.reconfigureServer("remoteAddress_Ip_Wildcard.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);

        commonSuccessfulLtpaServletCall(ssoCookie);
    }

    /**
     * Test description: - Set server xml file to have remoteAddress element set
     * with ip=127.0.0.0 and matchType=greaterThan. - Make a servlet call. - Set
     * server xml file to have remoteAddress element set with ip=127.0.0.2 and
     * matchType=greaterThan. - Make a servlet call. - Set server xml file to
     * have remoteAddress element set with ip=127.0.0.2 and matchType=lessThan.
     * - Make a servlet call. - Set server xml file to have remoteAddress
     * element set with ip=127.0.0.0 and matchType=lessThan. - Make a servlet
     * call.
     *
     * Expected results: - Using ip 127.0.0.0 and matchType greaterThan,
     * authentication should be successful and access to the protected resource
     * should be granted. - Using ip 127.0.0.2 and matchType greaterThan,
     * authentication should fail resulting in a 401. - Using ip 127.0.0.2 and
     * matchType lessThan, authentication should be successful and access to the
     * protected resource should be granted. - Using ip 127.0.0.0 and matchType
     * lessThan, authentication should fail resulting in a 401.
     */

    @Test
    public void testRemoteAddressIPRange() throws Exception {
        if (!FATSuite.LOCALHOST_DEFAULT_IP_ADDRESS) {
            Log.info(c, name.getMethodName(), AuthFilterConstants.LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE);
            return;
        }
        testHelper.reconfigureServer("remoteAddress_Ip_GreaterThan_Valid.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        // It is expected a remote address ip greater than the specified in the
        // server.xml file,
        // since our local ip 127.0.0.1 is greater than 127.0.0.0 the servlet
        // call must be successful
        commonSuccessfulLtpaServletCall(ssoCookie);

        testHelper.reconfigureServer("remoteAddress_Ip_GreaterThan_Invalid.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        // It is expected a remote address ip greater than the specified in the
        // server.xml file,
        // since our local ip 127.0.0.1 is less than 127.0.0.2 the servlet call
        // must be unsuccessful
        commonUnsuccessfulLtpaServletCall(ssoCookie);

        testHelper.reconfigureServer("remoteAddress_Ip_LessThan_Valid.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        // It is expected a remote address ip less than the specified in the
        // server.xml file,
        // since our local ip 127.0.0.1 is less than 127.0.0.2 the servlet call
        // must be successful
        commonSuccessfulLtpaServletCall(ssoCookie);

        testHelper.reconfigureServer("remoteAddress_Ip_LessThan_Invalid.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        // It is expected a remote address ip less than the specified in the
        // server.xml file,
        // since our local ip 127.0.0.1 is greater than 127.0.0.0 the servlet
        // call must be unsuccessful
        commonUnsuccessfulLtpaServletCall(ssoCookie);
    }

    /**
     * Test description: - Set server xml file to have a filter with an email
     * element configured to equals. - Restart the server and check log results
     * for validation of configuration. - Try to access a servlet with valid
     * header which includes email.
     *
     * Expected results: - Successfully access protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Equals_Valid() throws Exception {
        testHelper.reconfigureServer("requestHeaderEmail_Equals.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "sample@email.com");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have a filter with an email
     * element configured to contains. - Restart the server and check log
     * results for validation of configuration. - Try to access a servlet with
     * valid header. The header will have a valid email.
     *
     * Expected results: - Successfully access protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Contains_Valid() throws Exception {
        testHelper.reconfigureServer("requestHeaderEmail_Equals.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "sample@email.com");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have a filter with an email
     * element configured to not contains. - Restart the server and check log
     * results for validation of configuration. - Try to access a servlet with
     * valid header. The header will have a valid email.
     *
     * Expected results: - Successfully access protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Not_Contains_Valid() throws Exception {
        testHelper.reconfigureServer("requestHeaderEmail_Not_Contains.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        testHelper.setShutdownMessages("CWWKG0032W");
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "mysample@email.com");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_contains_Valid() throws Exception {
        testHelper.reconfigureServer("requestHeader_nameNoValue_contains.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        testHelper.setShutdownMessages("CWWKG0032W");
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_NAME_NO_VALUE, "myCustomData");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_contains_inValid() throws Exception {
        testHelper.reconfigureServer("requestHeader_nameNoValue_contains.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_NAME_NO_VALUE_INVALID, "myInvalidData");
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_equals_Valid() throws Exception {
        testHelper.reconfigureServer("requestHeader_nameNoValue_equals.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_NAME_NO_VALUE, "myCustomData");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_equals_invalid() throws Exception {
        testHelper.reconfigureServer("requestHeader_nameNoValue_equals.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_NAME_NO_VALUE_INVALID, "myInvalidData");
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_notContain_Valid() throws Exception {
        testHelper.reconfigureServer("requestHeader_nameNoValue_notContain.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "mysample@email.com");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    @Test
    public void testAuthFilterUsing_RequestHeader_custom_notContain_invalid() throws Exception {
        testHelper.reconfigureServer("requestHeader_nameNoValue_notContain.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_NAME_NO_VALUE, "myCustomData");
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have a filter that has the
     * wrong name in the email element configuration. - Restart the server and
     * check log results for validation of configuration. - Try to access a
     * servlet with valid header. The header will have a valid email.
     *
     * Expected results: - Unable to access protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Diff_Name() throws Exception {
        testHelper.reconfigureServer("requestHeaderEmail_Diff_Name.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "sample@email.com");
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have a filter with an email
     * element configured to equals. - Restart the server and check log results
     * for validation of configuration. - Try to access a servlet with an valid
     * header. The header will have an invalid email.
     *
     * Expected results: - Unable to access protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Equals_NotValid() throws Exception {
        testHelper.reconfigureServer("requestHeaderEmail_Equals.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "incorrect@email.com");
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have a filter with an email
     * element configured to contains. - The email address set in the header is
     * not supported by the contain element. - Restart the server and check log
     * results for validation of configuration. - Try to access a servlet with
     * an valid header. The header will have an invalid email.
     *
     * Expected results: - Unable to access protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Contains_InValid() throws Exception {
        testHelper.reconfigureServer("requestHeaderEmail_Equals.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "mysample@email.com");
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have a filter with an email
     * element configured to not contains. - The header will contain the email
     * address specified in the not contain auth filter configuration. - Restart
     * the server and check log results for validation of configuration. - Try
     * to access a servlet with an valid header. The header will have an invalid
     * email.
     *
     * Expected results: - Unable to access protected resource.
     */

    @Test
    public void testAuthFilterUsing_RequestHeader_Email_Not_Contains_InValid() throws Exception {
        testHelper.reconfigureServer("requestHeaderEmail_Not_Contains.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        testHelper.setShutdownMessages("CWWKG0032W");
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put(AuthFilterConstants.HEADER_EMAIL, "sample@email.com");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have authfilter config with
     * cookie element set to equals, LTPAToken2. - Restart the server and check
     * log results for validation of configuration. - Try to access a servlet
     * with an valid header. The header will have an LTPAToken2.
     *
     * Expected results: - Successfully able to access protected resource.
     */
    @Test
    public void testAuthFilterUsing_Cookie_LTPA_Equals_Valid() throws Exception {
        testHelper.reconfigureServer("cookieLTPA_Equals.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);

        Log.info(c, name.getMethodName(), "Accessing a servlet using valid SSO LTPA cookie headers");
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have authfilter config with
     * cookie element set to contains, LTPAToken2. - Restart the server and
     * check log results for validation of configuration. - Try to access a
     * servlet with an valid header. The header will have an LTPAToken2.
     *
     * Expected results: - Successfully able to access protected resource.
     */

    @Test
    public void testAuthFilterUsing_Cookie_LTPA_Contains() throws Exception {
        testHelper.reconfigureServer("cookieLTPA_Contains.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
        Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);
        successfulServletCall(AuthFilterConstants.SIMPLE_SERVLET, headers, AuthFilterConstants.USER0,
                              AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have authfilter config with
     * cookie element set to not contain, LTPAToken2. - Restart the server and
     * check log results for validation of configuration. - Since LTPA is
     * present we are expected to use it to get through the resource. Expected
     * results: - We are able to access the resource but no GSS Credentials.
     *
     */

    @Test
    public void testAuthFilterUsing_Cookie_LTPA_NotContains() throws Exception {
        testHelper.reconfigureServer("cookieLTPA_NotContain.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
        Map<String, String> headers = testHelper.setTestHeaders(ssoCookie, AuthFilterConstants.IE, TARGET_SERVER, null);
        Log.info(c, name.getMethodName(), "Accessing a servlet using valid SSO cookie");
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description: - Set server xml file to have authfilter config with
     * cookie element set to equals - Restart the server and check log results
     * for validation of configuration. - Try to access a servlet with an valid
     * header. The header will have an invalid LTPAToken2.
     *
     * Expected results: - Unable to access a protected resource.
     */

    @Test
    public void testAuthFilterUsing_Cookie_LTPA_Equals_InValid() throws Exception {
        testHelper.reconfigureServer("cookieLTPA_Equals.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkForAuthFilterMsg);
        Map<String, String> headers = testHelper.setTestHeaders(null, AuthFilterConstants.IE, TARGET_SERVER, null);
        headers.put("Cookie", LTPA_COOKIE_NAME + "=" + "someLTPAToken2");
        unsuccessfulLtpaServletCall(headers);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }
}
