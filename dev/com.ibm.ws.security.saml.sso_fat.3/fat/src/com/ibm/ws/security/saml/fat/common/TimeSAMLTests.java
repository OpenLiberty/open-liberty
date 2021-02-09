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
package com.ibm.ws.security.saml.fat.common;

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class were ported from tWAS' test SamlWebSSOTests.
 * If a tWAS test is not applicable, it will be noted in the comments below.
 * If a tWAS test fits better into another test class, it will be noted
 * which test project/class it now resides in.
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
 * to an application installed on a WebSphere SP. When the Web client
 * invokes the SP application, it is redirected to a TFIM IdP which issues
 * a login challenge to the Web client. The Web Client fills in the login
 * form and after a successful login, receives a SAML 2.0 token from the
 * TFIM IdP. The client invokes the SP application by sending the SAML
 * 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class TimeSAMLTests extends SAMLCommonTest {

    private static final Class<?> thisClass = TimeSAMLTests.class;

    /**
     * Set NotBefore before the current time (accounting for clock skew).
     * Test shows that we get an exception that the NotBefore is out of bounds - we expect to get the error page with
     * a descriptive msg in the server side log
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void timeSAMLTests_useToken_before_NotBefore() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1NotSigned", true);
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_NOT_BEFORE, SAMLConstants.ADD_TIME, SAMLTestSettings.setTimeArray(0, 0, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a clockSkew error.", SAMLMessageConstants.CWWKS5057E_NOT_BEFORE_OUT_OF_RANGE);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * Set NotOnOrAfter before the current time (accounting for clock skew).
     * Test shows that we get an exception that the NotOnOrAfter is out of bounds - we expect to get the error page with
     * a descriptive msg in the server side log
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void timeSAMLTests_useToken_after_NotOnOrAfter() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1NotSigned", true);
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME, SAMLTestSettings.setTimeArray(0, 0, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a clockSkew error.", SAMLMessageConstants.CWWKS5053E_NOT_ON_OR_AFTER_OUT_OF_RANGE);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * Set SessionNotOnOrAfter before the current time (accounting for clock skew).
     * Test shows that we get an exception that the SessionNotOnOrAfter is out of bounds - we expect to get the error page with
     * a descriptive msg in the server side log
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void timeSAMLTests_useToken_after_SessionNotOnOrAfter() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1NotSigned", true);
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_SESSION_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME, SAMLTestSettings.setTimeArray(0, 1, 10, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a clockSkew error.", SAMLMessageConstants.CWWKS5062E_SESSION_NOT_ON_OR_AFTER_OUT_OF_RANGE);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * SessionNotOnOrAfter sets the expiration time of the SP_Cookie that will be created
     * Set the SessionNotOnOrAfter to 4 min 30 seconds behind the current SP time.
     * The SPCookie will be created and used on the call to ACS (our app will be invoked)
     * After that, sleep for 45 seconds so we're beyond the SessionNotOnOrAfter (or cookie expiration) time.
     * Try to invoke the app using the SP cookie. We'll get the login page again.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void timeSAMLTests_SPCookie_expired() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1NotSigned", true);
        // set clock to 4 min 30 seconds behind current time
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_SESSION_NOT_ON_OR_AFTER, SAMLConstants.SUBTRACT_TIME, SAMLTestSettings.setTimeArray(0, 0, 4, 30), SAMLConstants.USE_CURRENT_TIME);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        if (flowType == SAMLConstants.UNSOLICITED_SP_INITIATED) {
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP Client jsp", null, SAMLConstants.IDP_CLIENT_JSP_TITLE);
        } else {
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP resend response form.", null, SAMLConstants.SAML_REQUEST);
        }

        // make sure we get the error page indicating that we're not authorized
        Object thePage = genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
        helpers.testSleep(45);
        helpers.invokeDefaultAppSameConversation(_testName, webClient, thePage, updatedTestSettings, expectations);

    }

    /**
     * The base security authCache lifetime is shorter than the time specified by SessionNotOnOrAfter
     * Get a SAMLToken, and invoke ACS. The lifetime of the SP Cookie will be longer than the
     * authCache lifetime. After getting the SPCookie, wait beyond the authCache lifetime and try to
     * use the SPCookie again - we should receive the login again.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void timeSAMLTests_SAMLTokenLifetimeLongerThanAuthCacheLifetime() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_1_notSigned_authCache.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP Client jsp", null, SAMLConstants.IDP_CLIENT_JSP_TITLE);
        } else {
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP resend response form.", null, SAMLConstants.SAML_REQUEST);
        }

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1NotSigned", true);

        // make sure we get the error page indicating that we're not authorized
        Object thePage = genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
        helpers.testSleep(15);
        helpers.invokeDefaultAppSameConversation(_testName, webClient, thePage, updatedTestSettings, expectations);

    }

}
