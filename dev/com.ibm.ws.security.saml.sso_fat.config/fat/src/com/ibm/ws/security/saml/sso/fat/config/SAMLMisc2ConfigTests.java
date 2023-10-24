/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso.fat.config;

import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestTools;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.security.saml20.fat.commonTest.config.settings.SAMLConfigSettings;
import com.ibm.ws.security.saml20.fat.commonTest.config.settings.SAMLProviderSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SAMLMisc2ConfigTests extends SAMLConfigCommonTests {

    private static final Class<?> thisClass = SAMLMisc1ConfigTests.class;

    /********************************************************
     * Tests
     ************************************************************/

    /*************************************************
     * includeTokenInSubject
     *************************************************/

    /**
     * Config attribute: includeTokenInSubject This test sets
     * includeTokenInSubject=true With this set to true, we should see the SAML
     * token in the Subject IDP initiated - SAML Token in the Subject SP
     * initiated - SAML Token in the Subject
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_config_includeTokenInSubject_true() throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get SAML Assertion in subject.", null, "SamlAssertion:");
        List<validationData> expectations2 = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get SAML Assertion in subject.", null, "SamlAssertion:");

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, expectations2);

    }

    /**
     * Config attribute: includeTokenInSubject This test sets
     * includeTokenInSubject=false With this set to false, we should NOT see the
     * SAML token in the Subject IDP initiated - No SAML Token in the Subject SP
     * initiated - No SAML Token in the Subject
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_includeTokenInSubject_false() throws Exception {

        testSAMLServer.reconfigServer("server_includeTokenInSubject_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setIncludeTokenInSubject(false);

        List<validationData> expectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Got SAML Assertion in subject.", null, "SamlAssertion:");
        List<validationData> expectations2 = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Got SAML Assertion in subject.", null, "SamlAssertion:");

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, expectations2);

    }

    // mapToUserRegistry tests moved to the
    // com.ibm.ws.security.saml.sso-2.0_fat.config.mapToUserRegistry project as
    // this one was
    // getting too large and running over the time limit.

    /*************************************************
     * authFilterRef
     *************************************************/

    /**
     * Config attribute: authFilterRef This test omits the authFilterRef
     *
     */
    // TODO - what should happen?
    // TODO - With the default sp omitted, we should be protected by the default
    // saml???
    // TODO - with the default sp overridden, what then, no protection, all
    // protected???
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_authFilterRef_omitted() throws Exception {

        testSAMLServer.reconfigServer("server_authFilterRef_omitted.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKG0033W_REF_VALUE_NOT_FOUND_IN_CONFIG);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: authFilterRef This test specifies an authFilterRef that
     * doesn't exist IDP initiated - app should be protected by form login SP
     * initiated - app should be protected by form login
     */
    // TODO - With the default sp omitted, we should be protected by the default
    // saml???
    // TODO - with the default sp overridden, what then, no protection, all
    // protected???
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_authFilterRef_invalid() throws Exception {

        testSAMLServer.reconfigServer("server_authFilterRef_invalid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKG0033W_REF_VALUE_NOT_FOUND_IN_CONFIG);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // // set expectations for SP flow
        // // IDP replies directly to the client - SP has no control over frame
        // content or status code
        // List<validationData> expectations =
        // msgUtils.addAuthenticationFailedExpectation(SAMLConstants.BUILD_POST_SOLICITED_SP_INITIATED_REQUEST,
        // null);
        // expectations = vData.addExpectation(expectations,
        // SAMLConstants.BUILD_POST_SOLICITED_SP_INITIATED_REQUEST,
        // SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did
        // not get message indicating that the keyStoreRef was bad.", null,
        // SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR) ;
        // expectations = vData.addExpectation(expectations,
        // SAMLConstants.BUILD_POST_SOLICITED_SP_INITIATED_REQUEST,
        // SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did
        // not get message indicating that the keyStoreRef was bad.", null,
        // "doesNotExistCert") ;

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /*************************************************
     * disableLtpaCookie
     *************************************************/

    /**
     * Config attribute: disableLtpaCookie This test specifies an
     * disableLtpaCookie value of false (the default is true) This should cause
     * an ltps2 token to be included instead of the SP cookie IDP initiated -
     * LTPA2 token should be generated instead of the SP cookie SP initiated -
     * LTPA2 token should be generated instead of the SP cookie With the LTPA
     * cookie enabled, the realmName in the RunAs subject is generated
     * correctly, with the value of the issuer, from the LTPA cookie and the
     * uniqueSecurityName should not contain the realmName.
     */
    @Test
    public void test_config_disableLtpaCookie_false() throws Exception {

        testSAMLServer.reconfigServer("server_disableLtpaCookie_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setAccessTokenType(SAMLConstants.LTPA_ACCESS_TOKEN_TYPE);

        List<validationData> solicitedSpExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get the expected uniqueSecurityName, without realm, in public credentials: " + updatedTestSettings.getIdpUserName(), null, updatedTestSettings.getIdpUserName());
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not get the expected realmName in RunAs subject: " + updatedTestSettings.getIdpIssuer(), null, SAMLCommonTestHelpers.assembleRegExRealmNameInRunAsSubject(updatedTestSettings.getIdpIssuer()));

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSpExpectations);

    }

    // The default value for disableLtpaCookie is true - all tests other than
    // ones testing
    // this attribute will be true, so, not explicitly testing true

    /*************************************************
     * spCookieName
     *************************************************/

    /**
     * Config attribute: disableLtpaCookie This test specifies an
     * disableLtpaCookie value of false (the default is true) This should cause
     * an ltps2 token to be included instead of the SP cookie IDP initiated -
     * LTPA2 token should be generated instead of the SP cookie SP initiated -
     * LTPA2 token should be generated instead of the SP cookie With the LTPA
     * cookie, the realmName in the RunAs subject should be generated correctly,
     * with the value of the issuer, from the LTPA cookie and the
     * uniqueSecurityName should not contain the realmName.
     */
    @Test
    public void test_config_spCookieName_good() throws Exception {

        testSAMLServer.reconfigServer("server_spCookieName_good.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpCookieName("WASSamlSP_myTestName1");

        List<validationData> solicitedSpExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not get the expected realmName in RunAs subject: " + updatedTestSettings.getIdpIssuer(), null, SAMLCommonTestHelpers.assembleRegExRealmNameInRunAsSubject(updatedTestSettings.getIdpIssuer()));
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get the expected uniqueSecurityName, without realm, in public credentials: " + updatedTestSettings.getIdpUserName(), null, updatedTestSettings.getIdpUserName());

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSpExpectations);

    }

    /**
     * Config attribute: disableLtpaCookie This test specifies an
     * disableLtpaCookie value of false (the default is true) This should cause
     * an ltps2 token to be included instead of the SP cookie IDP initiated -
     * LTPA2 token should be generated instead of the SP cookie SP initiated -
     * LTPA2 token should be generated instead of the SP cookie
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_spCookieName_empty() throws Exception {

        testSAMLServer.reconfigServer("server_spCookieName_empty.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    // skipping the test for spCookieName not being specified
    // This is the default behavior, no need to explicitly test it here

    /*************************************************
     * authnRequestTime
     *************************************************/

    /**
     * Config attribute: authnRequestTime This test specifies an
     * authnRequestTime value of 5 seconds This test waits 6 seconds before
     * submitting the id/pw on the login page - so by the time the login
     * completes, we're already beyond the time allowed for the token to be
     * generated IDP initiated - access is granted as the timeout has no meaning
     * in an IDP flow - login is done before SP is called SP initiated - access
     * is denied as the sp determines that too much time has elapsed since the
     * request was initiated
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_authnRequestTime_waitBeyond() throws Exception {

        testSAMLServer.reconfigServer("server_short_authnRequestTime.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setSleepBeforeTokenUse(6);

        // The IDP flow does not have the concept of the login taking too long
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_SLEEP_BEFORE_LOGIN, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive an error message indicating that the SAML request took too long.", SAMLMessageConstants.CWWKS5081E_SAML_REQUEST_EXPIRED);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_SLEEP_BEFORE_LOGIN, expectations);

    }

    // test_config_authFilterRef_valid - used in most other tests, so, no
    // specific test coded for it...
    // testSaml_idpInitiated_idAssertNoUser_noIDPSign_noIDPEncrypt_badFilter()
    // tests a filter that does not match the app requested
    // (the filtername exists, but the filter content doesn't match the request)

    /*************************************************
     * enabled
     *************************************************/

    /**
     * Config attribute: enabled This test specifies enabled set to true for a
     * specific SP (not defaultSP) That setting indicates that this SP is
     * enabled and can be used. We will invoke an app using this SP IDP
     * Initiated - should use SAML (this SP). Solicited SP Initiated - should
     * use SAML (this SP).
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_config_enable_specific_SP_TRUE() throws Exception {

        testSAMLServer.reconfigServer("server_enabled_defaultSP_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: enabled This test specifies enabled set to false for a
     * specific SP (not defaultSP) That setting indicates that this SP is NOT
     * enabled and can NOT be used. We will invoke an app using this SP IDP
     * Initiated - should fail because the specified SP is NOT enabled.
     * Solicited SP Initiated - should use SAML (default SP).// With the LTPA
     * cookie disabled (default true), the realmName in the RunAs subject is
     * generated correctly, with the value of the issuer, from the subject.
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_enable_specific_SP_FALSE() throws Exception {

        testSAMLServer.reconfigServer("server_enabled_defaultSP_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the realm was undefined.", SAMLMessageConstants.CWWKS5004E_SP_NOT_CONFIGURED);

        List<validationData> solicitedSpExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not get the expected realmName in RunAs subject: " + updatedTestSettings.getIdpIssuer(), null, SAMLCommonTestHelpers.assembleRegExRealmNameInRunAsSubject(updatedTestSettings.getIdpIssuer()));

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSpExpectations);

    }

    /**
     * Config attribute: enabled This test specifies enabled set to true for the
     * default SP That setting indicates that the default SP is enabled and can
     * be used. We will invoke an app using the default SP IDP Initiated -
     * should use SAML (default SP). Solicited SP Initiated - should use SAML
     * (default SP).
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Mode(TestMode.LITE)
    @Test
    public void test_config_enable_default_SP_TRUE() throws Exception {

        testSAMLServer.reconfigServer("server_enabled_defaultSP_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("defaultSP", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: enabled This test specifies enabled set to false for
     * the default SP That setting indicates that the default SP is NOT enabled
     * and can NOT be used. We will invoke an app using the default SP IDP
     * Initiated - should fail because the default SP is NOT enabled. Solicited
     * SP Initiated - should use Form Login instead of SAML.
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_enable_default_SP_FALSE() throws Exception {

        testSAMLServer.reconfigServer("server_enabled_defaultSP_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("defaultSP", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the realm was undefined.", SAMLMessageConstants.CWWKS5004E_SP_NOT_CONFIGURED);

        List<validationData> expectations2 = vData.addSuccessStatusCodes();
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the normal form login form.", null, SAMLConstants.STANDARD_LOGIN_HEADER);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.PROCESS_FORM_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get output showing that there is NO SAML token - it should not be there", null, SAMLConstants.NO_SAML_TOKEN_FOUND_MSG);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_NO_MATCH_FORMLOGIN, expectations2);

    }

    /**
     * Config attribute: enabled This test specifies enabled set to false for a
     * specific SP and the default SP That setting indicates that both SPs are
     * NOT enabled and can NOT be used. We will invoke an app using the specific
     * SP IDP Initiated - should fail because the default SP is NOT enabled.
     * Solicited SP Initiated - should use Form Login instead of SAML.
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_enable_specific_and_default_SP_FALSE() throws Exception {

        testSAMLServer.reconfigServer("server_enabled_defaultSP_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the realm was undefined.", SAMLMessageConstants.CWWKS5004E_SP_NOT_CONFIGURED);

        List<validationData> expectations2 = vData.addSuccessStatusCodes();
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the normal form login form.", null, SAMLConstants.STANDARD_LOGIN_HEADER);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.PROCESS_FORM_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get output showing that there is NO SAML token - it should not be there", null, SAMLConstants.NO_SAML_TOKEN_FOUND_MSG);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_NO_MATCH_FORMLOGIN, expectations2);

    }

    /*************************************************
     * allowCreate
     *************************************************/

    /**
     * Config attribute allowCreate This test runs only the solicited
     * SP-initiated flow because the AuthnRequest is forced back to check
     * whether the SAMLResponse contains the correct server setting, that
     * allowCreate="false." The attribute isPassive is set to "true" to force
     * the SAML Post Response to be returned. This test bypasses the problem of
     * OP's behaving differently for the same attribute settings. Thus the
     * settings for mapToUserRegistry, forceAuthn and allowCreate, have no
     * impact on whether this test runs successfully.
     *
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_allowCreate_true() throws Exception {
        testSAMLServer.reconfigServer("server_allowCreate_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.UNAUTHENTICATED_USER, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        List<validationData> expectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not find AllowCreate=\"true\" in the idp-process.log", null, "AllowCreate=\"true\"");

        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    @Test
    public void test_config_allowCreate_false() throws Exception {
        testSAMLServer.reconfigServer("server_allowCreate_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.UNAUTHENTICATED_USER, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get a SAML Post Response instead of the IDP login page", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not find AllowCreate=\"false\" in the idp-process.log", null, "AllowCreate=\"false\"");

        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    /*************************************************
     * httpsRequired
     *************************************************/

    /**
     * Config attribute: httpsRequired httpsRequired : true The standard SAML
     * flow is followed using an SP that is configured to require HTTPS when
     * accessing SP endpoints. Both flows: All steps should succeed and access
     * to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void test_config_httpsRequired_true() throws Exception {

        SAMLConfigSettings updatedSamlConfigSettings = samlConfigSettings.copyConfigSettings();
        SAMLProviderSettings updatedSamlProviderSettings = updatedSamlConfigSettings.getDefaultSamlProviderSettings();
        updatedSamlProviderSettings.setHttpsRequired("true");
        updatedSamlProviderSettings.setNameIDFormat("unspecified");
        updatedSamlProviderSettings.setSignatureMethodAlgorithm("SHA256");

        updateConfigFile(testSAMLServer, baseSamlServerConfig, updatedSamlConfigSettings, testServerConfigFile);

        testSAMLServer.reconfigServer(testServerConfigFile, _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: httpsRequired httpsRequired : true The standard SAML
     * flow is followed using an SP that is configured to require HTTPS when
     * accessing SP endpoints. The SP's default app and target app URLs are set
     * to use HTTP instead of HTTPS. Both flows: All steps should succeed and
     * access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void test_config_httpsRequired_true_accessDefaultAndTargetAppOverHttp() throws Exception {

        SAMLConfigSettings updatedSamlConfigSettings = samlConfigSettings.copyConfigSettings();
        SAMLProviderSettings updatedSamlProviderSettings = updatedSamlConfigSettings.getDefaultSamlProviderSettings();
        updatedSamlProviderSettings.setHttpsRequired("true");
        updatedSamlProviderSettings.setNameIDFormat("unspecified");
        updatedSamlProviderSettings.setSignatureMethodAlgorithm("SHA256");

        updateConfigFile(testSAMLServer, baseSamlServerConfig, updatedSamlConfigSettings, testServerConfigFile);

        testSAMLServer.reconfigServer(testServerConfigFile, _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        String defaultAppUrl = updatedTestSettings.getSpDefaultApp();
        String targetAppUrl = updatedTestSettings.getSpTargetApp();

        defaultAppUrl = defaultAppUrl.replaceFirst(testSAMLServer.getHttpsString(), testSAMLServer.getHttpString());
        targetAppUrl = targetAppUrl.replaceFirst(testSAMLServer.getHttpsString(), testSAMLServer.getHttpString());

        updatedTestSettings.setSpDefaultApp(defaultAppUrl);
        updatedTestSettings.setSpTargetApp(targetAppUrl);

        Log.info(thisClass, _testName, "New default app URL: " + defaultAppUrl);
        Log.info(thisClass, _testName, "New target app URL: " + targetAppUrl);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: httpsRequired httpsRequired : true The SP's ACS
     * endpoint is accessed over HTTP instead of HTTPS. Only one step is
     * invoked, so the test execution is identical for any flow being used.
     * Access to the ACS endpoint should be denied with a CWWKS5083E message in
     * the log saying HTTP was used when HTTPS is required. A 403 - Forbidden
     * page should be returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    // TODO - doesn't work after porting to OL
    //@Test
    public void test_config_httpsRequired_true_accessAcsEndpointOverHttp() throws Exception {

        SAMLConfigSettings updatedSamlConfigSettings = samlConfigSettings.copyConfigSettings();
        SAMLProviderSettings updatedSamlProviderSettings = updatedSamlConfigSettings.getDefaultSamlProviderSettings();
        updatedSamlProviderSettings.setHttpsRequired("true");

        updateConfigFile(testSAMLServer, baseSamlServerConfig, updatedSamlConfigSettings, testServerConfigFile);

        testSAMLServer.reconfigServer(testServerConfigFile, _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        // Create the conversation object which will maintain state for us
        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        Object page = solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_GET_SAML_RESPONSE, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

        SAMLCommonTestTools samlcttools = new SAMLCommonTestTools();
        com.gargoylesoftware.htmlunit.WebRequest request = samlcttools.getRequestWithSamlToken(page, updatedTestSettings);

        String origUrl = request.getUrl().toString();
        Log.info(thisClass, _testName, "Oroginal ACS request string: " + origUrl);
        URL url = AutomationTools.getNewUrl(origUrl.replaceAll("https", "http"));
        //        URL url = AutomationTools.getNewUrl(origUrl.replaceAll("https", "http").replace(testSAMLServer.getHttpDefaultSecurePort().toString(), testSAMLServer.getHttpDefaultPort().toString()));
        Log.info(thisClass, _testName, "Updated ACS request string: " + url);
        request.setUrl(url);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE);
        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, 0);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, "No HTTP Response");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not find a message indicating that https is required in messages.log", SAMLMessageConstants.CWWKO0801E_HTTPS_REQUIRED);

        // Send the request
        try {
            Object response = webClient.getPage(request);
            msgUtils.printResponseParts(response, _testName, "Response from invoking SP (AssertionConsumer) with SAML response: ");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, response, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, expectations, updatedTestSettings);
        } catch (java.net.SocketException e) {
            Log.info(thisClass, _testName, "Socket exception using http instead of https");
        }
    }

    /**
     * Config attribute: httpsRequired httpsRequired : true Access the SP's
     * metadata endpoint over HTTP. Both flows: - Access to the metadata
     * endpoint should be denied with a CWWKS5083E message in the log saying
     * HTTP was used when HTTPS is required. - A 403 - Forbidden page should be
     * returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_httpsRequired_true_accessMetadataEndpointOverHttp() throws Exception {

        SAMLConfigSettings updatedSamlConfigSettings = samlConfigSettings.copyConfigSettings();
        SAMLProviderSettings updatedSamlProviderSettings = updatedSamlConfigSettings.getDefaultSamlProviderSettings();
        updatedSamlProviderSettings.setHttpsRequired("true");

        updateConfigFile(testSAMLServer, baseSamlServerConfig, updatedSamlConfigSettings, testServerConfigFile);

        testSAMLServer.reconfigServer(testServerConfigFile, _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        String metadataEdpt = updatedTestSettings.getSpMetaDataEdpt();
        Log.info(thisClass, _testName, "Existing metadata endpoint: " + metadataEdpt);
        metadataEdpt = metadataEdpt.replaceFirst(testSAMLServer.getHttpsString(), testSAMLServer.getHttpString());
        Log.info(thisClass, _testName, "New metadata endpoint: " + metadataEdpt);
        updatedTestSettings.setSpMetaDataEdpt(metadataEdpt);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.SAML_META_DATA_ENDPOINT, null);

        String errorMsg = "Expected message saying SSL is required was not found.";
        String logMsg = SAMLMessageConstants.CWWKS5083E_HTTPS_REQUIRED_FOR_REQUEST + ".+service provider.+requires SSL.+HTTP was used.+";
        expectations = vData.addExpectation(expectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_MATCHES, errorMsg, null, logMsg);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5083E_HTTPS_REQUIRED_FOR_REQUEST);
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SP_SAMLMETADATAENDPOINT_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SP_SAMLMETADATAENDPOINT_FLOW, expectations);

    }

    /**
     * Config attribute: httpsRequired httpsRequired : false The standard SAML
     * flow is followed using an SP that is configured to NOT require HTTPS when
     * accessing SP endpoints. Both flows: All steps should succeed and access
     * to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void test_config_httpsRequired_false() throws Exception {

        SAMLConfigSettings updatedSamlConfigSettings = samlConfigSettings.copyConfigSettings();
        SAMLProviderSettings updatedSamlProviderSettings = updatedSamlConfigSettings.getDefaultSamlProviderSettings();
        updatedSamlProviderSettings.setHttpsRequired("false");
        updatedSamlProviderSettings.setNameIDFormat("unspecified");
        updatedSamlProviderSettings.setSignatureMethodAlgorithm("SHA256");

        updateConfigFile(testSAMLServer, baseSamlServerConfig, updatedSamlConfigSettings, testServerConfigFile);

        testSAMLServer.reconfigServer(testServerConfigFile, _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: httpsRequired httpsRequired : false The standard SAML
     * flow is followed using an SP that is configured to NOT require HTTPS when
     * accessing SP endpoints. The SP's default app and target app URLs are set
     * to use HTTP instead of HTTPS. Both flows: All steps should succeed and
     * access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void test_config_httpsRequired_false_accessDefaultAndTargetAppOverHttp() throws Exception {

        SAMLConfigSettings updatedSamlConfigSettings = samlConfigSettings.copyConfigSettings();
        SAMLProviderSettings updatedSamlProviderSettings = updatedSamlConfigSettings.getDefaultSamlProviderSettings();
        updatedSamlProviderSettings.setHttpsRequired("false");
        updatedSamlProviderSettings.setNameIDFormat("unspecified");
        updatedSamlProviderSettings.setSignatureMethodAlgorithm("SHA256");

        updateConfigFile(testSAMLServer, baseSamlServerConfig, updatedSamlConfigSettings, testServerConfigFile);

        testSAMLServer.reconfigServer(testServerConfigFile, _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        String defaultAppUrl = updatedTestSettings.getSpDefaultApp();
        String targetAppUrl = updatedTestSettings.getSpTargetApp();

        defaultAppUrl = defaultAppUrl.replaceFirst(testSAMLServer.getHttpsString(), testSAMLServer.getHttpString());
        targetAppUrl = targetAppUrl.replaceFirst(testSAMLServer.getHttpsString(), testSAMLServer.getHttpString());

        updatedTestSettings.setSpDefaultApp(defaultAppUrl);
        updatedTestSettings.setSpTargetApp(targetAppUrl);

        Log.info(thisClass, _testName, "New default app URL: " + defaultAppUrl);
        Log.info(thisClass, _testName, "New target app URL: " + targetAppUrl);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: httpsRequired httpsRequired : false Access the SP's
     * metadata endpoint over HTTP. Both flows: The invocation should succeed
     * and the SP's metadata XML file should be returned.
     *
     * @throws Exception
     */
    @Test
    public void test_config_httpsRequired_false_accessMetadataEndpointOverHttp() throws Exception {

        SAMLConfigSettings updatedSamlConfigSettings = samlConfigSettings.copyConfigSettings();
        SAMLProviderSettings updatedSamlProviderSettings = updatedSamlConfigSettings.getDefaultSamlProviderSettings();
        updatedSamlProviderSettings.setHttpsRequired("false");

        updateConfigFile(testSAMLServer, baseSamlServerConfig, updatedSamlConfigSettings, testServerConfigFile);

        testSAMLServer.reconfigServer(testServerConfigFile, _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        String metadataEdpt = updatedTestSettings.getSpMetaDataEdpt();
        Log.info(thisClass, _testName, "Existing metadata endpoint: " + metadataEdpt);
        metadataEdpt = metadataEdpt.replaceFirst(testSAMLServer.getHttpsString(), testSAMLServer.getHttpString());
        Log.info(thisClass, _testName, "New metadata endpoint: " + metadataEdpt);
        updatedTestSettings.setSpMetaDataEdpt(metadataEdpt);

        List<validationData> idpExpectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        List<validationData> solicitedSpExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);

        // Ensure we get the metadata file returned in the response
        String metadataHeader = "attachment;filename=\"spMetadata.xml\"";
        idpExpectations = vData.addExpectation(idpExpectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.RESPONSE_HEADER, SAMLConstants.STRING_CONTAINS, "Did not find the expected SAML metadata header.", null, metadataHeader);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.SAML_META_DATA_ENDPOINT, SAMLConstants.RESPONSE_HEADER, SAMLConstants.STRING_CONTAINS, "Did not find the expected SAML metadata header.", null, metadataHeader);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SP_SAMLMETADATAENDPOINT_FLOW, idpExpectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SP_SAMLMETADATAENDPOINT_FLOW, solicitedSpExpectations);

    }

    /*************************************************
     * allowCustomCacheKey
     *************************************************/

    /**
     * Config attribute: allowCustomCacheKey allowCustomCacheKey: true This
     * setting indicates that a custom cache key will be created within the
     * subject and an SP cookie will be added to the response. Both flows: - The
     * custom cache key should be present in the servlet response. - An LTPA
     * cookie should NOT be present in the response. - An SP cookie should be
     * present in the response.
     *
     * @throws Exception
     */
    @Test
    public void test_config_allowCustomCacheKey_true() throws Exception {

        testSAMLServer.reconfigServer("server_allowCustomCacheKey_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setAccessTokenType(SAMLConstants.SP_ACCESS_TOKEN_TYPE);

        List<validationData> idpInitExpectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected custom cache key in the IdP-initiated response.", null, SAMLConstants.CUSTOM_CACHE_KEY);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an LTPA cookie in the IdP-initiated response when one should not be there.", null, "cookie: " + SAMLConstants.LTPA_TOKEN_NAME);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find an SP cookie in the IdP-initiated response.", null, "cookie: " + SAMLConstants.SP_COOKIE_PREFIX);

        List<validationData> solicitedSpExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected custom cache key in the solicited SP response.", null, SAMLConstants.CUSTOM_CACHE_KEY);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an LTPA cookie in the solicited SP response when one should not be there.", null, "cookie: " + SAMLConstants.LTPA_TOKEN_NAME);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find an SP cookie in the solicited SP response.", null, "cookie: " + SAMLConstants.SP_COOKIE_PREFIX);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, idpInitExpectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSpExpectations);

    }

    /**
     * Config attribute: allowCustomCacheKey allowCustomCacheKey: true,
     * disableLtpaCookie: false. This setting indicates that a custom cache key
     * will be created within the subject and an LTPA cookie will be added to
     * the response. Both flows: - The custom cache key should be present in the
     * servlet response. - An LTPA cookie should be present in the response. -
     * An SP cookie should NOT be present in the response.
     *
     * @throws Exception
     */
    @Test
    public void test_config_allowCustomCacheKey_true_disableLtpaCookie_false() throws Exception {

        testSAMLServer.reconfigServer("server_allowCustomCacheKey_true_disableLtpaCookie_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setAccessTokenType(SAMLConstants.LTPA_ACCESS_TOKEN_TYPE);

        List<validationData> idpInitExpectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected custom cache key in the IdP-initiated response.", null, SAMLConstants.CUSTOM_CACHE_KEY);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find an LTPA cookie in the IdP-initiated response.", null, "cookie: " + SAMLConstants.LTPA_TOKEN_NAME);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an SP cookie in the IdP-initiated response when one should not be there.", null, "cookie: " + SAMLConstants.SP_COOKIE_PREFIX);

        List<validationData> solicitedSpExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected custom cache key in the solicited SP response.", null, SAMLConstants.CUSTOM_CACHE_KEY);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find an LTPA cookie in the solicited SP response.", null, "cookie: " + SAMLConstants.LTPA_TOKEN_NAME);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an SP cookie in the IdP-initiated response when one should not be there.", null, "cookie: " + SAMLConstants.SP_COOKIE_PREFIX);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, idpInitExpectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSpExpectations);

    }

    /**
     * Config attribute: allowCustomCacheKey allowCustomCacheKey: false The
     * default setting for the disableLtpaCookie attribute is true, so even
     * though the allowCustomCacheKey attribute is set to false, a cache key
     * value will still be added to the subject since the LTPA cookie is
     * disabled. Both flows: - The custom cache key should be present in the
     * servlet response. - An LTPA cookie should NOT be present in the response.
     * - An SP cookie should be present in the response.
     *
     * @throws Exception
     */
    @Test
    public void test_config_allowCustomCacheKey_false() throws Exception {

        testSAMLServer.reconfigServer("server_allowCustomCacheKey_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setAccessTokenType(SAMLConstants.SP_ACCESS_TOKEN_TYPE);

        List<validationData> idpInitExpectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected custom cache key in the IdP-initiated response.", null, SAMLConstants.CUSTOM_CACHE_KEY);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an LTPA cookie in the IdP-initiated response when one should not be there.", null, "cookie: " + SAMLConstants.LTPA_TOKEN_NAME);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find an SP cookie in the IdP-initiated response.", null, "cookie: " + SAMLConstants.SP_COOKIE_PREFIX);

        List<validationData> solicitedSpExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected custom cache key in the solicited SP response.", null, SAMLConstants.CUSTOM_CACHE_KEY);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an LTPA cookie in the solicited SP response when one should not be there.", null, "cookie: " + SAMLConstants.LTPA_TOKEN_NAME);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find an SP cookie in the solicited SP response.", null, "cookie: " + SAMLConstants.SP_COOKIE_PREFIX);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, idpInitExpectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSpExpectations);

    }

    /**
     * Config attribute: allowCustomCacheKey allowCustomCacheKey: false,
     * disableLtpaCookie: false. This setting indicates that a custom cache key
     * will NOT be created within the subject and an LTPA cookie will be added
     * to the response. Both flows: - The custom cache key should NOT be present
     * in the servlet response. - An LTPA cookie should be present in the
     * response. - An SP cookie should NOT be present in the response.
     *
     * @throws Exception
     */
    @Test
    public void test_config_allowCustomCacheKey_false_disableLtpaCookie_false() throws Exception {

        testSAMLServer.reconfigServer("server_allowCustomCacheKey_false_disableLtpaCookie_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setAccessTokenType(SAMLConstants.LTPA_ACCESS_TOKEN_TYPE);

        List<validationData> idpInitExpectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found custom cache key in the IdP-initiated response when there shouldn't be one.", null, SAMLConstants.CUSTOM_CACHE_KEY);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find an LTPA cookie in the IdP-initiated response.", null, "cookie: " + SAMLConstants.LTPA_TOKEN_NAME);
        idpInitExpectations = vData.addExpectation(idpInitExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an SP cookie in the IdP-initiated response when one should not be there.", null, "cookie: " + SAMLConstants.SP_COOKIE_PREFIX);

        List<validationData> solicitedSpExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found custom cache key in the solicited SP response when there shouldn't be one.", null, SAMLConstants.CUSTOM_CACHE_KEY);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find an LTPA cookie in the solicited SP response.", null, "cookie: " + SAMLConstants.LTPA_TOKEN_NAME);
        solicitedSpExpectations = vData.addExpectation(solicitedSpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found an SP cookie in the IdP-initiated response when one should not be there.", null, "cookie: " + SAMLConstants.SP_COOKIE_PREFIX);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, idpInitExpectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSpExpectations);

    }

    /**
     * Config attribute: createSession createSession=Create session This setting
     * indicates wheter to create an HttpSession if the current HttpSession does
     * not exist.
     *
     * All that we can check is the logging of how the flag is set - the
     * handling is up to htmlunit which isn't behaving
     *
     * @throws Exception
     */
    @Test
    public void test_createSession_true() throws Exception {
        testSAMLServer.reconfigServer("server_createSession_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_TRACE_LOG, SAMLConstants.STRING_CONTAINS, "Trace did not show createSession set to true", null, "createSession:true");

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    @Test
    public void test_createSession_false() throws Exception {
        testSAMLServer.reconfigServer("server_createSession_false.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = getAndSaveWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_TRACE_LOG, SAMLConstants.STRING_CONTAINS, "Trace did not show createSession set to true", null, "createSession:false");

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /*************************************************
     * spHostAndPort
     *************************************************/

    @Test
    public void test_spHostAndPort_https() throws Exception {
        testSAMLServer.reconfigServer("server_spHostAndPort_valid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), "http://localhost:8010", SAMLConstants.AES256);

        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /***
     * Test with a value that the IDP thinks is invalid - for Shibboleth, we
     * can't use http.
     *
     * @throws Exception
     */
    @Test
    public void test_spHostAndPort_http() throws Exception {
        testSAMLServer.reconfigServer("server_spHostAndPort_invalid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), "http://localhost:8010", SAMLConstants.AES256);

        SAMLTestSettings idpUpdatedTestSettings = updatedTestSettings.copyTestSettings();
        String currentHttpPort = testSAMLServer.getHttpDefaultPort().toString();
        String currentHttpsPort = testSAMLServer.getHttpDefaultSecurePort().toString();
        idpUpdatedTestSettings.setSpConsumer(idpUpdatedTestSettings.getSpConsumer().replace("https", "http").replace(currentHttpsPort, currentHttpPort));

        List<validationData> expectations1 = vData.addSuccessStatusCodes(null, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST);
        expectations1 = vData.addResponseStatusExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.BAD_REQUEST_STATUS);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Shibboleth didn't complain about http vs https", null, "Unsupported Request");

        List<validationData> expectations2 = vData.addSuccessStatusCodes(null, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST);
        expectations2 = vData.addResponseStatusExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.BAD_REQUEST_STATUS);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Shibboleth didn't complain about http vs https", null, "Unsupported Request");

        IDP_initiated_SAML(_testName, idpUpdatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_ONLY_IDP, expectations1);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations2);

    }

    /*************************************************
     * realmName
     *************************************************/

    /**
     * Configure realmName=f00, and mapToUserRegistry=N0 in the server.xml. We
     * expect that the realmName in the subject has been mapped to this
     * configured value, "f00"
     *
     * @throws Exception
     */
    @Test
    public void test_realmName_mapToUserRegistry_No() throws Exception {
        testSAMLServer.reconfigServer("server_realmName_mapToUserRegistry_No.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> idpExpectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        idpExpectations = vData.addExpectation(idpExpectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "<tr><td>Realm Name</td><td>f00</td></tr>");
        idpExpectations = vData.addExpectation(idpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "realmName=f00");

        List<validationData> solicitedSPExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSPExpectations = vData.addExpectation(solicitedSPExpectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "<tr><td>Realm Name</td><td>f00</td></tr>");
        solicitedSPExpectations = vData.addExpectation(solicitedSPExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "realmName=f00");

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, idpExpectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSPExpectations);

    }

    /**
     * Configure realmName=f00, and mapToUserRegistry=User in the server.xml. We
     * expect that the realmName in the subject has been mapped to the local
     * realm, "BasicRealm"
     *
     * @throws Exception
     */
    @Test
    public void test_realmName_mapToUserRegistry_User() throws Exception {
        testSAMLServer.reconfigServer("server_realmName_mapToUserRegistry_User.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> idpExpectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        idpExpectations = vData.addExpectation(idpExpectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "<tr><td>Realm Name</td><td>BasicRealm</td></tr>");
        idpExpectations = vData.addExpectation(idpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "realmName=BasicRealm");

        List<validationData> solicitedSPExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSPExpectations = vData.addExpectation(solicitedSPExpectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "<tr><td>Realm Name</td><td>BasicRealm</td></tr>");
        solicitedSPExpectations = vData.addExpectation(solicitedSPExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "realmName=BasicRealm");

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, idpExpectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSPExpectations);

    }

    /**
     * Configure realmName=f00, and mapToUserRegistry=Group in the server.xml.
     * We expect that the realmName in the subject has been mapped to this
     * configured value, "f00"
     *
     * @throws Exception
     */
    @Test
    public void test_realmName_mapToUserRegistry_Group() throws Exception {
        testSAMLServer.reconfigServer("server_realmName_mapToUserRegistry_Group.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> idpExpectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        idpExpectations = vData.addExpectation(idpExpectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "<tr><td>Realm Name</td><td>f00</td></tr>");
        idpExpectations = vData.addExpectation(idpExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "realmName=f00");

        List<validationData> solicitedSPExpectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        solicitedSPExpectations = vData.addExpectation(solicitedSPExpectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "<tr><td>Realm Name</td><td>f00</td></tr>");
        solicitedSPExpectations = vData.addExpectation(solicitedSPExpectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Snoop output did not contain the correct realm name", null, "realmName=f00");

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_EXTENDED_FLOW, idpExpectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW, solicitedSPExpectations);

    }

}
