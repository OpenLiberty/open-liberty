/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.CommonTests;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ClientTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.ValidationDataToExpectationConverter;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 * This is the test class that contains common code for all of the
 * OpenID Connect RP tests. There will be OP specific test classes that extend this class.
 **/

public class GenericOidcClientTests extends CommonTest {

    public static Class<?> thisClass = GenericOidcClientTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.LOGIN_USER;
    protected static String hostName = "localhost";
    public static final String MSG_USER_NOT_IN_REG = "CWWKS1106A";

    // old message before 152627
    // Stack Dump = org.openid4java.discovery.yadis.YadisException:
    //    0x704: I/O transport error: peer not authenticated
    //
    // new message after 152627
    // Stack Dump = org.openid4java.discovery.yadis.YadisException:
    //    0x704: I/O transport error: com.ibm.jsse2.util.j:
    //       PKIX path building failed: java.security.cert.CertPathBuilderException:
    //       PKIXCertPathBuilderImpl could not build a valid CertPath.; internal cause is:
    String errMsg0x704 = "CertPathBuilderException";

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestGetMainPath() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

        testUserInfo(wc);

    }

    /**
     * Test that userinfo is retrieved and available from an API call. If userinfo url is defined and enabled in metadata, then
     * upon authentication the userinfo JSON from the OP, if available, is to be stored in the subject as a string and made
     * accessible through the PropagationHelper API. Since we invoked the protected resource, we should already be authenticated.
     * This calls a jsp that invokes the PropagationHelper.getUserInfo() API to check the userinfo.
     */
    void testUserInfo(WebConversation wc) throws Exception {
        String endpoint = "https://localhost:" + testRPServer.getHttpDefaultSecurePort() + "/formlogin/propagationHelperUserInfoApiTest.jsp";

        GetMethodWebRequest request = new GetMethodWebRequest(endpoint);
        WebResponse resp = wc.getResponse(request);
        String response = resp.getText();
        Log.info(thisClass, _testName, "Got JSP response: " + response);

        String testAction = "testUserInfo";
        String expectedUser = testSettings.getAdminUser();
        Expectations expectations = new Expectations();
        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "\"sub\":\"" + expectedUser + "\"", "Did not find expected \"sub\" claim and value in the JSP response."));
        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "\"name\":\"" + expectedUser + "\"", "Did not find expected \"name\" claim and value in the JSP response."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_MATCHES, "\"iss\":\"http[^\"]+/OidcConfigSample\"", "Did not find expected \"iss\" claim and value in the JSP response."));
        List<validationData> convertedExpectations = ValidationDataToExpectationConverter.convertExpectations(expectations);
        validationTools.validateResult(resp, testAction, convertedExpectations, testSettings);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>This configuration specifies additional valid parameters with values on the authorization code and token endpoint
     * requests to the OP.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue when additional parameters are included in the request
     * because the OIDC OP ignores the additional parameters.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestGetMainPathWithAddlAuthAndTokenParms() throws Exception {
        testRPServer.reconfigServer("rp_server_orig_addlParms.xml", _testName, false, null);
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>This configuration specifies endpoint parameters with format errors on the authorization code and token endpoint
     * requests to the OP.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue when additional bad parameters are included in the
     * request because the poorly formatted parameters are omitted from the endpoint requests.
     * </OL>
     */
    @Test
    public void OidcClientTestGetMainPathWithAddlBadAuthAndTokenParms() throws Exception {
        testRPServer.reconfigServer("rp_server_orig_addlBadParms.xml", _testName, false, null);
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url with the forwardLoginParameter and authzParameter both
     * configured.
     * <LI>This configuration specifies additional valid parameters with values on the authorization code as well
     * as a dynamic authorization login parameter login_hint (forwardLoginParameter="login_hint,ui_locales").
     * This test also configures another authorization endpoint parameter (<authzParameter name="mq_authz1" value="mqa1234" />) to
     * ensure both
     * are passed to the authorization endpoint on the OP.
     *
     * In the config, it is specified that both login_hint and ui_locales parameters should
     * be passed, but only a value for login_hint is supplied to the RP.
     *
     * Note: In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * in server.xml as: autoAuthorize="true"
     * Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue when the additional login_hint value is included but
     * ui_locales value is not
     * included in the query parameters passed to the RP. The additional authz parameter for mq_authz1 is also passed to the OP.
     * The Liberty OP ignores the additional parameters. The trace will be checked to ensure the additional parameters are present
     * in the authorization request.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestGetMainPathWithDyanmicLoginParameter() throws Exception {
        testRPServer.reconfigServer("rp_server_orig_addDynamicParm.xml", _testName, false, null);
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        String applicationUrl = testRPServer.getHttpsString() + "/" + Constants.OPENID_APP + "/" + Constants.DEFAULT_SERVLET + "?&login_hint=testuser1@us.ibm.com";
        updatedTestSettings.setTestURL(applicationUrl);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testOPServer, Constants.TRACE_LOG, Constants.STRING_CONTAINS,
                "OP trace.log should contain an entry showing the OP received the authz parameter and value", null, "urn:ibm:names:oauth:request.*response_type.*[code].*mq_authz.*[mqa1234]");
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testOPServer, Constants.TRACE_LOG, Constants.STRING_CONTAINS,
                "OP trace.log should contain an entry showing the OP received the dynamic login parameter and value", null, "urn:ibm:names:oauth:request.*response_type.*[code].*login_hint.*[testuser1@us.ibm.com]");
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>The client is reconfigured to redirect to client02 on the provider.
     * <LI>The provider's client02 config uses a regular expression in evaluating the redirect URL sent by the client.
     * <LI>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Redirect should be processed and client should authenticate and access the test servlet without issue
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestGetMainPathWithRegexpRedirect() throws Exception {
        testRPServer.reconfigServer("rp_server_orig_regexp_redirect.xml", _testName, false, null);
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setClientID("client02");
        updatedTestSettings.setClientName("client02");
        updatedTestSettings.addRequestParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        //expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        //expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        //expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestGetMainPathWithNonce() throws Exception {
        testRPServer.reconfigServer("rp_server_nonce_enabled.xml", _testName, false, null);

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.setNonce(Constants.EXIST_WITH_ANY_VALUE);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientTestPostMainPath() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();
        updatedTestSettings.addRequestFileParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.POST_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addRequestFileParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_POST_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url,
     * <LI>then specify a valid user id, password, ....
     * <LI>finally, attempt to access the same servlet again
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>Test is showing that we can access the same servlet again without having to log back in.
     * <LI>In this scenario, HTTP is used between RP and OP and httpsRequired is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue, then access the same
     * <LI>servlet again, without requiring another login/authentication
     * </OL>
     */
    @Test
    public void OidcClientTestMainPathAccessAppAgain() throws Exception {

        WebConversation wc = new WebConversation();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_AGAIN, Constants.RESPONSE_FULL, Constants.IDToken_STR);

        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_AGAIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>The test case has almost the same test environment as OidcClientTestGetMainPath()
     * <LI>But it put in a badFilter. So, the OpenID Connect Client got filtered out.
     * <LI>This forces the test to go to a regular login panel.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestFilterOut() throws Exception {
        testRPServer.reconfigServer("rp_server_orig_bad_filter.xml", _testName, false, null);

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the regular login page.", null,
                "<TITLE>login.jsp</TITLE>");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the regular login page.", null,
                "Form Login Page");
        genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a User ID that is NOT in the local registry.
     * <LI>The login attempt should fail with appropriate error.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to authenticate - receive authentication error and log useful messages.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
    public void OidcClientTestUserIdNotInRegistry() throws Exception {

        WebConversation wc = new WebConversation();

        testRPServer.reconfigServer("rp_server_UserNotInReg.xml", _testName, false, null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.FORBIDDEN_STATUS);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get HTTP 403 Authentication exception ", null, Constants.AUTHORIZATION_FAILED);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Client messages.log should contain a message indicating that the RP is unable to authenticate testuser", null, MSG_USER_NOT_IN_REG + ".*testuser");
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.LOGIN_USER);
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url,
     * <LI>then specify a valid user id, password, ....
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is enabled by OP (default behavior).
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should receive consent form, after successful login.
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestConsentForm() throws Exception {

        // Reconfigure OP server with consent form settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_consent.xml", "rp_server_orig.xml");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get Consent Form ", null,
                Constants.APPROVAL_HEADER);

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". In this scenario, redirect url is not
     * <LI>specified in RP's server.xml, it is only specified in OP's server.xml.
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientTestHttpRedirectUrl() throws Exception {

        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_http_redirect.xml", "rp_server_http_redirect.xml");

        WebConversation wc = new WebConversation();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". HTTP is used between RP and OP, but the httpsRequired
     * <LI>attribute is set to "true".
     * <LI>Test is showing that RP request will be rejected when using HTTP, with httpsRequired="true".
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Request is expected to fail with authentication error.
     * </OL>
     */
    @Test
    public void OidcClientTestHTTPWithHttpsTrue() throws Exception {
        WebConversation wc = new WebConversation();

        // Reconfigure OP server with consent form settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_orig.xml", "rp_server_httpstrue.xml");

        TestSettings newSettings = testSettings.copyTestSettings();
        String HttpUrl = testRPServer.getHttpString() + "/" + Constants.OPENID_APP + "/" + Constants.DEFAULT_SERVLET;
        newSettings.setTestURL(HttpUrl);

        List<validationData> expectations = validationTools.add401Responses(Constants.GET_LOGIN_PAGE);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.GET_LOGIN_PAGE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the HTTPS is required", "CWWKS1703E.*requires SSL");

        genericRP(_testName, wc, newSettings, test_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". HTTP is used between RP and OP, but the httpsRequired
     * <LI>attribute is not specified, supposed to use default value of httpsRequired="true".
     * <LI>Test is showing that RP request will be rejected when using HTTP, with httpsRequired not specified.
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Request is expected to fail with authentication error.
     * </OL>
     */
    @Test
    public void OidcClientTestHTTPWithDefaultHttps() throws Exception {

        WebConversation wc = new WebConversation();

        // Reconfigure OP server with consent form settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_orig.xml", "rp_server_defhttps.xml");

        TestSettings newSettings = testSettings.copyTestSettings();
        String HttpUrl = testRPServer.getHttpString() + "/" + Constants.OPENID_APP + "/" + Constants.DEFAULT_SERVLET;
        newSettings.setTestURL(HttpUrl);

        List<validationData> expectations = validationTools.add401Responses(Constants.GET_LOGIN_PAGE);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the HTTPS is required", null, "CWWKS1703E.*requires SSL");

        genericRP(_testName, wc, newSettings, test_LOGIN_PAGE_ONLY, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using SSL between RP and SP.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientSSLTestMainPath() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_basic_ssl.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The redirect attribute is not specified
     * <LI>in RP's server.xml, it is only specified in OP's server.xml.
     * <LI>Test is showing a good main path flow using SSL between RP and SP, when redirect
     * <LI>url is not specified in RP's server.xml.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientSSLTestNoRedirectInRP() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_redirect_ssl.xml", "rp_server_redirect_ssl.xml");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        // expectations = idTokenUtils.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, testSettings);

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is enabled, by OP.
     * <LI>Test is using SSL between RP and SP and shows that consent form is displayed.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get consent form after successful login.
     * </OL>
     */
    @Test
    public void OidcClientSSLTestConsentForm() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_consent_ssl.xml", "rp_server_basic_ssl.xml");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get Consent Form ", null,
                Constants.APPROVAL_HEADER);

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a client request that does not
     * <LI>contain "openid" scope. The request should fail with appropriate error.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail with HTTP 403 error.
     * </OL>
     */

    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestNoOpenidScope() throws Exception {

        WebConversation wc = new WebConversation();

        // Reconfigure OP server with basic SSL settings
        //Reconfigure RP server with no openid scope
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_no_openid.xml");

        List<validationData> expectations = validationTools.add401Responses(Constants.GET_LOGIN_PAGE);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.GET_LOGIN_PAGE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the required scope \"openid\" is missing", "CWWKS1713E.*client01.*openid");

        genericRP(_testName, wc, testSettings, test_LOGIN_PAGE_ONLY, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a client request that contains
     * <LI>client secret which is different from OP's client secret value.
     * <LI>The request should fail with appropriate error, and useful error
     * <LI>messages should be logged in messages.log
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail with HTTP 403 error.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.clients.common.BadPostRequestException" })
    @Test
    public void OidcClientTestBadClientSecret() throws Exception {

        WebConversation wc = new WebConversation();

        // Reconfigure OP server with basic SSL settings
        // Reconfigure RP server with client secret that does not match OP's value
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_bad_secret.xml");

        //        List<validationData> expectations = validationTools.add401Expectations(Constants.LOGIN_USER, Constants.OK_STATUS);
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.BAD_REQUEST_STATUS);

        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message indicating bad client secret in RP messages.log", Constants.MSG_RP_INVALID_CLIENTID_OR_SECRET);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message indicating bad client secret in OP messages.log", Constants.MSG_OP_INVALID_CLIENTID_OR_SECRET + ":.*client01");
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get a message stating that the client credential was not good in OP messages.log.", MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
        
        testRPServer.addIgnoredServerExceptions("CWWKS1859E", "CWWKS1708E", "SRVE8094W", "SRVE8115W");
        
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /***
     * Specify a bad grantType/request response_type and make sure that it is
     * handled properly. The issue is now that the config will override any
     * invalid value with the "default" value. This is what is done when NO
     * value is specified. So, does it make sense to include a test for this
     * particular attribute when it'll take us down the same flow as tests that
     * are already included - and testing generic config processing code...
     */

    //      @Test
    //      public void OidcClientTestBadResponseType() throws Exception {
    //
    //              // Reconfigure RP server with client response that is invalid
    //              testRPServer.reconfigServer("rp_server_bad_response_type.xml", _testName, false, null);
    //
    //              WebConversation wc = new WebConversation();
    //
    //              List <validationData> expectations = vData.addSuccessStatusCodes(null);
    //              expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get to the test servlet.", null, "com.ibm.ws.security.openidconnect.token.IDToken");
    //              expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
    //              expectations = idTokenUtils.addDefaultIDTokenExpectations(expectations, _testName,  eSettings.getProviderType(), test_FinalAction, testSettings) ;
    //              genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS,  expectations) ;
    //
    //              return;
    //      }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". Also httpsRequired attribute is set to "false".
     * <LI>Test is showing a good main path flow using SSL between RP and SP, with httpsRequired="false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */

    @Test
    public void OidcClientTestSSLHttpsFalse() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_httpsfalse_ssl.xml");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        // expectations = idTokenUtils.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, testSettings);

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". Also httpsRequired attribute is set to "true".
     * <LI>Test is showing a good main path flow using SSL between RP and SP, with httpsRequired="true".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */

    @Test
    public void OidcClientTestSSLHttpsTrue() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_httpstrue_ssl.xml");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        // expectations = idTokenUtils.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, testSettings);

        WebConversation wc = new WebConversation();
        genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /***
     * Specify a bad grantType/request response_type and make sure that it is
     * handled properly. The issue is now that the config will override any
     * invalid value with the "default" value. This is what is done when NO
     * value is specified. So, does it make sense to include a test for this
     * particular attribute when it'll take us down the same flow as tests that
     * are already included - and testing generic config processing code...
     */
    //
    //  @Test
    //  public void OidcClientTestBadResponseType() throws Exception {
    //
    //      // Reconfigure RP server with client response that is invalid
    //      testRPServer.reconfigServer("rp_server_bad_response_type.xml", _testName, false, null);
    //
    //      WebConversation wc = new WebConversation();
    //
    //      List <validationData> expectations = vData.addSuccessStatusCodes(null);
    //      expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
    //      expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
    //        expectations = idTokenUtils.addDefaultIDTokenExpectations(expectations, _testName,  eSettings.getProviderType(), test_FinalAction, testSettings) ;
    //      genericRP(_testName, wc, testSettings, test_GOOD_LOGIN_ACTIONS,  expectations) ;
    //
    //      return;
    //  }
    //
    //    // placeholder for a nonce test - specify a nonce in the openidConnectClient (not currently supported) and ensure that the nonce is included in the id_token .
    //
    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>The signature Algorithm is set to "none" in both the OP server as well as the client in the RP.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue. The response should not be signed.
     * </OL>
     */
    @Test
    public void OidcClientTestSigAlgNone() throws Exception {

        WebConversation wc = new WebConversation();

        // Reconfigure OP server with basic SSL settings
        // Reconfigure RP server with basic SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_sig_alg_none.xml", "rp_server_sig_alg_none.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_NONE);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        
        testRPServer.addIgnoredServerExceptions("CWWKS1741W");
        
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>The signature Algorithm is set to "HS256" (which happens to be the default) in both the OP server as well as the client
     * in the RP.
     * <LI>This scenario makes sure that it is picked up properly
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue. The response should be signed using HS256
     * </OL>
     */
    @Test
    public void OidcClientTestSigAlgHS256() throws Exception {

        WebConversation wc = new WebConversation();

        // Reconfigure OP server with basic SSL settings
        // Reconfigure RP server with basic SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_sig_alg_hs256.xml", "rp_server_sig_alg_hs256.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_HS256);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>The signature Algorithm is set to "none" in the OP server and HS256 in the client in the RP.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue. The response should not be signed
     * </OL>
     */
    @Test
    public void OidcClientTestSigAlgMisMatch1() throws Exception {
        WebConversation wc = new WebConversation();

        // Reconfigure OP server with basic SSL settings
        // Reconfigure RP server with basic SSL settings
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_sig_alg_none.xml", "rp_server_sig_alg_hs256.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_NONE);

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Client messages.log should contain a message indicating that the signature is missing from the ID Token", "CWWKS1760E.*client01.*HS256");
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>The signature Algorithm is set to HS256 in the OP server and "none" in the client in the RP.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a negative main path flow.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should not be able to access the test servlet. There will be a failure due to an out of sync signature algorithm
     * </OL>
     */
    @Test
    public void OidcClientTestSigAlgMisMatch2() throws Exception {
        WebConversation wc = new WebConversation();

        //        // Reconfigure OP server with basic SSL settings
        //        // Reconfigure RP server with basic SSL settings

        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_sig_alg_hs256.xml", "rp_server_sig_alg_none.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_HS256);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        //      expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.GET_LOGIN_PAGE,  Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the sig alg is none", "CWWKS1741W:.*client01");
        //List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
        //expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, Constants.FORBIDDEN_STATUS);
        
        testRPServer.addIgnoredServerExceptions("CWWKS1741W");
        
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>Test is showing a good main path flow using HTTP between RP and OP and uses the <LIL leaked password checker to check
     * for passwords in clear text in the trace.
     * <LI>For this scenario the client secret is set to a unique value "s3cr3t" so the trace
     * <LI>can be searched for this value.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without finding the client password or
     * <LI>shared key in the trace.log.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientTestMainPath_VerifyNoPasswordInClearTextInTrace() throws Exception {

        WebConversation wc = new WebConversation();

        // Reconfigure OP server with provider and client using a unique client secret and sharedKey
        // Reconfigure RP server with provider and client using unique client secret and sharedKey
        //ClientTestHelpers.restartServers(_testName, Constants.JUNIT_REPORTING, "op_server_orig_pwdTest.xml", "rp_server_orig_pwdTest.xml");
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_orig_pwdTest.xml", "rp_server_orig_pwdTest.xml");
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientSecret("s3cr3t");
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

        LeakedPasswordChecker passwordCheckerOP = new LeakedPasswordChecker(testOPServer.getServer());
        passwordCheckerOP.checkForPasswordInAnyFormat("s3cr3t");

        LeakedPasswordChecker passwordCheckerRP = new LeakedPasswordChecker(testRPServer.getServer());
        passwordCheckerRP.checkForPasswordInAnyFormat("s3cr3t");

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientTestGetLargeParm() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addLargeRequestParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Test
    public void OidcClientTestPostLargeParm() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addLargeRequestParms();
        updatedTestSettings.addRequestFileParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addRequestFileParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_POST_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true". The OP truststore is missing the certificate for RP.
     * <LI>Test shows an ssl handshake failure between the OP and RP
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should receive 401 error with messages to indicate SSL handshake error.
     * </OL>
     */
    // different versions of java issue different exceptions - allow appropriate exceptions from any of our supported java versions
    @AllowedFFDC({ "java.security.cert.CertPathBuilderException", "com.ibm.security.cert.IBMCertPathBuilderException", "javax.net.ssl.SSLHandshakeException", "java.security.cert.CertificateException", "sun.security.validator.ValidatorException", "java.net.SocketException", "javax.net.ssl.SSLException", "javax.net.ssl.SSLProtocolException" })
    @Test
    public void OidcClientSSLTest_BadOPTrustStore() throws Exception {

        // Reconfigure OP server with SSL settings
        // Reconfigure RP server with SSL settings
        List<String> opMsgs = new ArrayList<String>() {
            {
                add(MessageConstants.CWWKO0219I_TCP_CHANNEL_READY);
            }
        };
        List<String> rpMsgs = new ArrayList<String>() {
            {
                add(MessageConstants.CWWKG0017I_CONFIG_UPDATE_COMPLETE);
            }
        };

        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl_bad_truststore.xml", opMsgs, "rp_server_basic_ssl_bad_op_trust.xml", rpMsgs, null, null);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        WebConversation wc = new WebConversation();

        // set up null expectations so that the actual getLoginPage and processLoginForm helpers won't validate
        // as we don't know which step will fail - let the test case do the checking.
        String failingAction = Constants.GET_LOGIN_PAGE;
        helpers.setOverrideSetServerMark(true);
        WebResponse response = helpers.getLoginPage(_testName, wc, updatedTestSettings, null);
        if (response == null) {
            fail("WebResponse from getLogin page was null");
        }
        // due to redirects going through the test client, the first request may not fail, check when we submit the login page
        // we set up our test clients such that they'll never fail the ssl checks.
        if (AutomationTools.getResponseStatusCode(response) == Constants.OK_STATUS) {
            helpers.setOverrideSetServerMark(true);
            failingAction = Constants.LOGIN_USER;
            response = helpers.processProviderLoginForm(_testName, wc, response, updatedTestSettings, null);
        }
        List<validationData> expectations = validationTools.add401Responses(failingAction);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not fail login", null, Constants.UNAUTHORIZED_MESSAGE);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get a message indicating that there was an SSL HANDSHAKE exception in the messages.log", "CWPKI0022E");
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get a message indicating that the Cert needs to be added to the truststore in the messages.log", "commonBadTrustStore.jks");
        testRPServer.addIgnoredServerException(MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER);
        testRPServer.addIgnoredServerException("CWWKS1524E");
        testRPServer.addIgnoredServerException("CWWKS1525E");
        testOPServer.addIgnoredServerException("CWWKO0801E");

        validationTools.validateResult(response, failingAction, expectations, updatedTestSettings);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a client request that contains
     * <LI>clientID which is different from OP's clientID value. The RP config contains a bad clientID.
     * <LI>The request should fail with appropriate error, and useful error
     * <LI>messages should be logged in messages.log
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Message is returned to the end-user: CWOAU0061E: The OAuth service provider could not find the client because the
     * client name is not valid. Contact your system administrator to resolve the problem.
     * <LI>The OP messages.log gives more detail for the system administrator to resolve the problem: CWOAU0023E: The OAuth
     * service provider could not find the client badclient01.
     * </OL>
     */
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException")
    @Test
    public void OidcClientTestBadClientIDInRPConfig() throws Exception {

        WebConversation wc = new WebConversation();
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_bad_clientid.xml");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get response indicating that the OAuth provider could not find the client because the client name is not valid. ", null, "CWOAU0061E");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message indicating the service provider could not find the client in OP messages.log", null, "CWOAU0023E:.*badclient01");
        genericRP(_testName, wc, testSettings, test_LOGIN_PAGE_ONLY, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet when the OP config contains a
     * <LI>bad redirect URI for the registered client.
     * <LI>The request should fail with appropriate error to end-user, and useful error
     * <LI>messages should be logged in messages.log so the system administrator can
     * <LI>disagnose the problem without calling IBM support.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Message is returned to the end-user: CWOAU0062E: The OAuth service provider could not redirect the request because the
     * redirect URI was not valid. Contact your system administrator to resolve the problem.
     * <LI>The OP messages.log gives more detail for the system administrator to resolve the problem: CWOAU0056E: The redirect URI
     * parameter [https://localhost:8946/oidcclient/redirect/client01] provided in the OAuth or OpenID Connect request did not
     * match any of the redirect URIs registered with the OAuth provider
     * [https://localhost:8946/oidcclient/bad/redirect/client01].
     * </OL>
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void OidcClientTestBadRedirectURIOPConfig() throws Exception {

        WebConversation wc = new WebConversation();
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_badredirect_ssl.xml", "rp_server_basic_ssl.xml");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get response indicating that the OAuth provider could not redirect the request because the redirect URI is not valid. ", null, "CWOAU0062E");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message in OP messages.log indicating the provider could not redirect the request because the redirect URI is not valid.", null, "CWOAU0056E:.*/bad/redirect/client01");
        genericRP(_testName, wc, testSettings, test_LOGIN_PAGE_ONLY, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a client request that contains
     * <LI>clientID for a client with enabled=false in OP config. The login prompt is displayed. After
     * <LI>the userid and password are entered, the request fails with a message to the end-user. More detail
     * <LI>with the client name is logged in messages.log so that the system administrator can resolve the problem.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Message is returned to the end-user: CWOAU0061E: The OAuth service provider could not find the client because the
     * client name is not valid. Contact your system administrator to resolve the problem.
     * <LI>The OP messages.log gives more detail for the system administrator to resolve the problem:
     * <LI>CWOAU0023E: The OAuth service provider could not find the client client01.
     * <LI>The explanation and user action will explain that the client must be valid and enabled in config.
     * </OL>
     */

    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException")
    @Test
    public void OidcClientTestClientDisabledInOPConfig() throws Exception {

        WebConversation wc = new WebConversation();
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_disableclient_ssl.xml", "rp_server_basic_ssl.xml");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did Not get response indicating that the OAuth provider could not find the client because the client name is not valid. ", null, "CWOAU0061E");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get a message indicating the service provider could not find the client in OP messages.log", null, "CWOAU0023E:.*client01");

        genericRP(_testName, wc, testSettings, test_LOGIN_PAGE_ONLY, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet with a client request that contains
     * <LI>clientID whose shared key does not match the OP client. The RP config contains a bad shared key.
     *
     * <LI>When an access token is being used, the request should fail with appropriate error, and useful error
     * messages should be logged in messages.log
     * <LI>When a JWT token with JWK cert is being used, the request will succeed because the RP goes to the OP to get the
     * verification key to verify the token and the request will succeed. The client secret is not used in this case.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>For access token,
     * - A message is returned to the end-user: 403
     * - The RP messages.log gives more detail for the system administrator to resolve the problem:
     * CWWKS1756E:Validation faile for the ID token requested by [client01] using the [HS256] algorithm due to a signature
     * verification failure: [Invalid signature for token: xxx ]
     *
     * CWWKS1706E: The OpenID Connect client [client01] failed to validate the ID token due to [JWS signature is invalid: xxx]
     * <LI>For JWT with JWK,
     * - The servlet is accessed successfully.
     * </OL>
     */
    @Test
    public void OidcClientTestBadClientSharedKeyInRPConfig() throws Exception {

        WebConversation wc = new WebConversation();
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_basic_ssl.xml", "rp_server_bad_sharedkey.xml");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();

        List<validationData> expectations = null;
        if (testSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = validationTools.add401Responses(Constants.LOGIN_USER);
            expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                    "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    "Did not get a message indicating that validation failed for a signature mismatch in RP messages.log", "CWWKS1756E:.*client01");
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    "Did not get a message indicating the client failed to validate because of a signature mismatch in RP messages.log", "CWWKS1706E:.*client01");
        } else {
            expectations = vData.addSuccessStatusCodes(null);
            expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
            expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
            expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
            expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
            expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        }
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);
    }
}
