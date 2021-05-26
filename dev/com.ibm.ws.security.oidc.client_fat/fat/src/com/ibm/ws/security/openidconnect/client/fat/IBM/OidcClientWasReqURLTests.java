/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ClientTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 * This is the test class that will validate the behavior with various settings of
 * <webAppSecurity wasReqURLRedirectDomainNames= /> and the value of the WasReqURLOidc cookie.
 * Tests added for Issue 14692
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcClientWasReqURLTests extends CommonTest {

    public static Class<?> thisClass = OidcClientWasReqURLTests.class;

    private static final String opServerConfig = "op_server_orig.xml";
    private static final String localHost = "localhost";
    private static final String otherHost = "ibm.com";
    private static final String badHost = "abc";

    private static final String simpleString = "Test Value";
    private static final String complexString = "Value;newCookie=Attack;<tag>Hi</tag>";
    private static final String specialChars1 = "! * ' ( ) ; : @ & = + $ , / ? % # [ ]";
    private static final String specialChars2 = "! * ' \" ( ) ; : @ & = + $ , / ? % # [ ]";
    private static final String scriptString1 = "<script>echo \"hiThere\";</script>";
    private static final String scriptString2 = "<script>echo \"hi there\";</script>";
    private static final String scriptString3 = "<script>echo+\"hi there\";</script>";

    //
    public enum ExpectedResult {
        SUCCESS, INVALID_COOKIE, EXCEPTION, MISSING_COOKIE
    }

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", opServerConfig, Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_wasReqUrl_notSet.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                                   Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // speed up the tests by not restoring the config between tests - each test will config the server that it needs (cut the time in half)
        testRPServer.setRestoreServerBetweenTests(false);

        testSettings.setFlowType(Constants.RP_FLOW);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames not set
     * Replace the hostname within WasReqURLOidc... with "abc" before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_NotSet_updateCookieTo_abc() throws Exception {

        testWasReqURLOidc_cookie(badHost, "rp_server_wasReqUrl_notSet.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames not set
     * Replace the hostname within WasReqURLOidc... with "ibm.com" before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_NotSet_updateCookieTo_otherExistingHostName() throws Exception {

        testWasReqURLOidc_cookie(otherHost, "rp_server_wasReqUrl_notSet.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames not set
     * Replace the hostname within WasReqURLOidc... with the test machines ip address before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_NotSet_updateCookieTo_localHostAddress() throws Exception {

        testWasReqURLOidc_cookie(testOPServer.getServerHostIp(), "rp_server_wasReqUrl_notSet.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames not set
     * Replace the hostname within WasReqURLOidc... with the test machines host name before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_NotSet_updateCookieTo_localHostName() throws Exception {

        testWasReqURLOidc_cookie(testOPServer.getServerHostname(), "rp_server_wasReqUrl_notSet.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames not set
     * Do NOT replace the hostname within WasReqURLOidc...
     *
     * Expected results: The login will succeed, and we should be able to successfully invoke the protected app
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_NotSet_doNotUpdateCookie() throws Exception {

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_notSet.xml", ExpectedResult.SUCCESS);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Replace the hostname within WasReqURLOidc... with "abc" before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_updateCookieTo_abc() throws Exception {

        testWasReqURLOidc_cookie(badHost, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Replace the hostname within WasReqURLOidc... with "ibm.com" before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_updateCookieTo_otherExistingHostName() throws Exception {

        testWasReqURLOidc_cookie(otherHost, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Replace the hostname within WasReqURLOidc... with the test machines ip address before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_updateCookieTo_localHostAddress() throws Exception {

        testWasReqURLOidc_cookie(testOPServer.getServerHostIp(), "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Replace the hostname within WasReqURLOidc... with the test machines host name before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1520E message. The WasReqURLOidc cookie ends up getting removed when localhost when localhost was in the
     * original request and the cookie no longer has localhost - so, we end up failing for a missing cookie instead.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_updateCookieTo_localHostName() throws Exception {

        testWasReqURLOidc_cookie(testOPServer.getServerHostname(), "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.MISSING_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Do NOT replace the hostname within WasReqURLOidc...
     *
     * Expected results: The login will succeed, and we should be able to successfully invoke the protected app
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie() throws Exception {

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to a list containing localhost and the test machines host name
     * and ip address
     * Replace the hostname within WasReqURLOidc... with "abc" before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToMultipleHosts_updateCookieTo_abc() throws Exception {

        testWasReqURLOidc_cookie(badHost, "rp_server_wasReqUrl_setToMultipleEntries.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to a list containing localhost and the test machines host name
     * and ip address
     * Replace the hostname within WasReqURLOidc... with "ibm.com" before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToMultipleHosts_updateCookieTo_otherExistingHostName() throws Exception {

        testWasReqURLOidc_cookie(otherHost, "rp_server_wasReqUrl_setToMultipleEntries.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to a list containing localhost and the test machines host name
     * and ip address
     * Replace the hostname within WasReqURLOidc... with the test machines ip address before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1520E message. The WasReqURLOidc cookie ends up getting removed when localhost when localhost was in the
     * original request and the cookie no longer has localhost - so, we end up failing for a missing cookie instead.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToMultipleHosts_updateCookieTo_localHostAddress() throws Exception {

        testWasReqURLOidc_cookie(testOPServer.getServerHostIp(), "rp_server_wasReqUrl_setToMultipleEntries.xml", ExpectedResult.MISSING_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to a list containing localhost and the test machines host name
     * and ip address
     * Replace the hostname within WasReqURLOidc... with the test machines host name before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1520E message. The WasReqURLOidc cookie ends up getting removed when localhost when localhost was in the
     * original request and the cookie no longer has localhost - so, we end up failing for a missing cookie instead.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToMultipleHosts_updateCookieTo_localHostName() throws Exception {

        testWasReqURLOidc_cookie(testOPServer.getServerHostname(), "rp_server_wasReqUrl_setToMultipleEntries.xml", ExpectedResult.MISSING_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to a list containing localhost and the test machines host name
     * and ip address
     * Do NOT replace the hostname within WasReqURLOidc...
     *
     * Expected results: The login will succeed, and we should be able to successfully invoke the protected app
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToMultipleHosts_doNotUpdateCookie() throws Exception {

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToMultipleEntries.xml", ExpectedResult.SUCCESS);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the hostname of a pingable machine (not running
     * SimpleServlet)
     * Replace the hostname within WasReqURLOidc... with "abc" before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToOther_updateCookieTo_abc() throws Exception {

        testWasReqURLOidc_cookie(badHost, "rp_server_wasReqUrl_setToOther.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the hostname of a pingable machine (not running
     * SimpleServlet)
     * Replace the hostname within WasReqURLOidc... with "ibm.com" before invoking the login page
     *
     * Expected results: The login will succeed, we will get a connection failure trying to invoke the app on the target machine
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToOther_updateCookieTo_otherExistingHostName() throws Exception {

        testWasReqURLOidc_cookie(otherHost, "rp_server_wasReqUrl_setToOther.xml", ExpectedResult.EXCEPTION);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the hostname of a pingable machine (not running
     * SimpleServlet)
     * Replace the hostname within WasReqURLOidc... with the test machines ip address before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToOther_updateCookieTo_localHostAddress() throws Exception {

        testWasReqURLOidc_cookie(testOPServer.getServerHostIp(), "rp_server_wasReqUrl_setToOther.xml", ExpectedResult.INVALID_COOKIE);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the hostname of a pingable machine (not running
     * SimpleServlet)
     * Replace the hostname within WasReqURLOidc... with the test machines host name before invoking the login page
     *
     * Expected results: The login will succeed, but the server will return a 500 status and issue
     * the CWWKS1532E or CWWKS1520E message.
     * (NOTE: if the hostname ends in ibm.com, we'll get the missing cookie failure (CWWKS1520E), if it does
     * not, we'll get the invalid cookie failure (CWWKS1532E) in the RP server log.)
     * The CWWKS1520E failure occurs because WasReqURLOidc cookie ends up getting removed when localhost was in the
     * original request and the cookie no longer has localhost - so, we end up failing for a missing cookie instead.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToOther_updateCookieTo_localHostName() throws Exception {

        String hostName = testOPServer.getServerHostname();
        if (hostName.endsWith("ibm.com")) {
            testWasReqURLOidc_cookie(hostName, "rp_server_wasReqUrl_setToOther.xml", ExpectedResult.MISSING_COOKIE);
        } else {
            testWasReqURLOidc_cookie(hostName, "rp_server_wasReqUrl_setToOther.xml", ExpectedResult.INVALID_COOKIE);
        }
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the hostname of a pingable machine (not running
     * SimpleServlet)
     * Do NOT replace the hostname within WasReqURLOidc...
     *
     * Expected results: The login will succeed, and we should be able to successfully invoke the protected app
     *
     * @throws Exception
     */
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToOther_doNotUpdateCookie() throws Exception {

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToOther.xml", ExpectedResult.SUCCESS);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Do NOT replace the hostname within WasReqURLOidc...
     * Pass along an extra parm that is NOT encoded.
     *
     * Expected results: The login will succeed, and we should be able to successfully invoke the protected app.
     * - make sure that the parm value gets to the app as expected
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passClearTextParm_simple() throws Exception {

        String parmValue = simpleString;
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Do NOT replace the hostname within WasReqURLOidc...
     * Pass along an extra parm that is NOT encoded.
     *
     * Expected results: The login will succeed, and we should be able to successfully invoke the protected app.
     * - make sure that the parm value gets to the app as expected
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passClearTextParm_complex1() throws Exception {

        String parmValue = complexString;
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passClearTextParm_complex2() throws Exception {

        String parmValue = specialChars1;
        String parmSearchValue = parmValue.replace("'", "\'");

        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmSearchValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passClearTextParm_complex3() throws Exception {

        String parmValue = specialChars2;
        String parmSearchValue = parmValue.replace("'", "\'");

        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmSearchValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passClearTextParm_complex4() throws Exception {

        String parmValue = scriptString1;
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passClearTextParm_complex5() throws Exception {

        String parmValue = scriptString2;
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passClearTextParm_complex6() throws Exception {

        String parmValue = scriptString3;
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Do NOT replace the hostname within WasReqURLOidc...
     * Pass along an extra parm that is encoded.
     *
     * Expected results: The login will succeed, and we should be able to successfully invoke the protected app.
     * - make sure that the parm value gets to the app as expected
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passEncodedParm_simple() throws Exception {

        String parmValue = simpleString;
        String parmSearchValue = parmValue.replace(" ", "+");

        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", URLEncoder.encode(parmValue, "UTF-8"));
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmSearchValue);

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    /**
     * test with: webAppSecurity wasReqURLRedirectDomainNames set to the test machines host name
     * Do NOT replace the hostname within WasReqURLOidc...
     * Pass along an extra parm that is encoded.
     *
     * Expected results: The login will succeed, and we should be able to successfully invoke the protected app.
     * - make sure that the parm value gets to the app as expected
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passEncodedParm_complex1() throws Exception {

        String parmValue = URLEncoder.encode(complexString, "UTF-8");
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form (htmulinit ends up doubly encoding, so, expected the encoded value for the parm)
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passEncodedParm_complex2() throws Exception {

        String parmValue = URLEncoder.encode(specialChars1, "UTF-8");
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form (htmulinit ends up doubly encoding, so, expected the encoded value for the parm)
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passEncodedParm_complex3() throws Exception {

        String parmValue = URLEncoder.encode(specialChars2, "UTF-8");
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form (htmulinit ends up doubly encoding, so, expected the encoded value for the parm)
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passEncodedParm_complex4() throws Exception {

        String parmValue = URLEncoder.encode(scriptString1, "UTF-8");
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form (htmulinit ends up doubly encoding, so, expected the encoded value for the parm)
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passEncodedParm_complex5() throws Exception {

        String parmValue = URLEncoder.encode(scriptString2, "UTF-8");
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form (htmulinit ends up doubly encoding, so, expected the encoded value for the parm)
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientWasReqURLTests_wasReqUrl_setToLocalHostName_doNotUpdateCookie_passEncodedParm_complex6() throws Exception {

        String parmValue = URLEncoder.encode(scriptString3, "UTF-8");
        Map<String, String> parms = new HashMap<String, String>();
        parms.put("testParm", parmValue);
        testSettings.setRequestParms(parms);

        // test that the parm value gets the servlet in its original form (htmulinit ends up doubly encoding, so, expected the encoded value for the parm)
        List<validationData> extraExpectations = vData.addExpectation(null, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                      "Passed Parm value was incorrect.", null, "Param: testParm with value: " + parmValue);
        // make sure that we don't end up with any new cookies
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: newCookie");
        extraExpectations = vData.addExpectation(extraExpectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                                 "Found a cookie that should not have been created.", null, "cookie: Attack");

        testWasReqURLOidc_cookie(null, "rp_server_wasReqUrl_setToLocalHostName.xml", ExpectedResult.SUCCESS, extraExpectations);
    }

    /**
     * Common test method that will reconfigure the RP server so that the test can use the "webAppSecurity
     * wasReqURLRedirectDomainNames" value
     * that the test requires.
     * This method will try to access a protected app. The response to that request is the login page.
     * Before submitting the login page, this method will update the hostname in WasReqURLOidc cookie with the value
     * that the caller passes in.
     * The caller will specify the behavior that it expects. This behavior can be getting access to the app, failing to validate
     * the WasReqURLOidc token,
     * failure to connect to an invalid system or missing a cookie.
     * This method will validate that we get the behavior that the caller specified.
     *
     * @param updatedHost
     * @param rpServerConfig
     * @param expectedResult
     * @throws Exception
     */
    public void testWasReqURLOidc_cookie(String updatedHost, String rpServerConfig, ExpectedResult expectedResult) throws Exception {
        testWasReqURLOidc_cookie(updatedHost, rpServerConfig, expectedResult, null);

    }

    public void testWasReqURLOidc_cookie(String updatedHost, String rpServerConfig, ExpectedResult expectedResult, List<validationData> extraExpectations) throws Exception {

        // Reconfigure OP server with Basic registry
        // Reconfigure RP server with Basic registry
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, opServerConfig, rpServerConfig);

        List<validationData> expectations = vData.addExpectation(extraExpectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                                 "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);

        switch (expectedResult) {
            case SUCCESS:
                expectations = vData.addSuccessStatusCodes(expectations);
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_MATCHES,
                                                    "Did not receive " + Constants.IDToken_STR + " in the response.", null, Constants.IDToken_STR);
                break;
            case INVALID_COOKIE:
                expectations = vData.addSuccessStatusCodesForActions(expectations, Constants.LOGIN_USER, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.INTERNAL_SERVER_ERROR_STATUS);
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                    "Did not receive error message " + MessageConstants.CWOAU0073E_FRONT_END_ERROR + " in the response", null,
                                                    MessageConstants.CWOAU0073E_FRONT_END_ERROR);
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                                                                     "Server log did not contain an error message about the missing WASReqURLOidc cookie.",
                                                                     MessageConstants.CWWKS1532E_MALFORMED_URL_IN_COOKIE + ".*" + updatedHost + ".*");

                break;
            case EXCEPTION:
                expectations = vData.addSuccessStatusCodesForActions(expectations, Constants.LOGIN_USER, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT);
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS, "Did NOT get expected exception",
                                                    null, "timed out");
                break;
            case MISSING_COOKIE:
                expectations = vData.addSuccessStatusCodesForActions(expectations, Constants.LOGIN_USER, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.INTERNAL_SERVER_ERROR_STATUS);
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                    "Did not receive error message " + MessageConstants.CWOAU0073E_FRONT_END_ERROR + " in the response", null,
                                                    MessageConstants.CWOAU0073E_FRONT_END_ERROR);
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                                                                     "Server log did not contain an error message about the missing WASReqURLOidc cookie.",
                                                                     MessageConstants.CWWKS1520E_MISSING_SAMESITE_COOKIE);
                break;
            default:
                break;
        }

        WebConversation wc = new WebConversation();

        // Attempt to access the protected app and end up on the login page (validate via expectations that we landed on the login page)
        WebResponse response = helpers.getLoginPage(_testName, wc, testSettings, expectations);

        // The RP should have created the WASReqURLOidc* cookie - modify its value as the calling tests requests...
        String[] cookies = wc.getCookieNames();
        if (updatedHost != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith("WASReqURLOidc")) {
                    Log.info(thisClass, _testName, "Altering WASReqURLOidc cookie: " + cookie);
                    String value = wc.getCookieValue(cookie);
                    wc.putCookie(cookie, value.replace(localHost, updatedHost));
                }
            }
            Log.info(thisClass, _testName, "Updated cookies");
            msgUtils.printAllCookies(wc);
        } else {
            Log.info(thisClass, _testName, "Cookies were not updated");
        }

        // Process the login (now with the possibly updated cookie value)
        // Expectations will validate the proper response to the login with the updated cookie (success, bad status and error messages in the log, or an exception)
        response = helpers.processProviderLoginForm(_testName, wc, response, testSettings, expectations);

    }

}
