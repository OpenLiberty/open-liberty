/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso.fat.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SAMLMisc1ConfigTests extends SAMLConfigCommonTests {

    private static final Class<?> thisClass = SAMLMisc1ConfigTests.class;

    /********************************************************
     * Tests
     ************************************************************/

    /*************************************************
     * id
     *************************************************/

    /**
     * Config attribute: id This test makes sure that the defaultSP ID is used
     * for an SP that does not specify the id attribute in the server config.
     *
     * @throws Exception
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void test_config_id_missing() throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        testSAMLServer.reconfigServer("server_missingSAMLId.xml", _testName, null, SAMLConstants.JUNIT_REPORTING);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: id This test makes sure that we get the proper error
     * message in messages.log for an empty id attribute.
     *
     * @throws Exception
     */
    @Test
    public void test_config_id_empty() throws Exception {
        List<String> addtlMsgs = new ArrayList<String>();
        addtlMsgs.add(SAMLMessageConstants.CWWKS5006E_SP_ID_EMPTY);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5006E_SP_ID_EMPTY);

        testSAMLServer.reconfigServer("server_emptySAMLId.xml", _testName, addtlMsgs, SAMLConstants.JUNIT_REPORTING);
    }

    /**
     * Config attribute: id This test makes sure that we end up with one sp
     * config - the last config in the server.xml should contain what is the
     * active config server.xml has 2 sp instances with the same name - one
     * references a valid filter, one references an invalid reference. When the
     * second sp references a filter that will match our app, we get SAML
     * authorization. When the second sp references a filter that will NOT match
     * our app, we use form login instead.
     *
     * @throws Exception
     */
    // @Mode(TestMode.LITE)
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void test_config_id_duplicated() throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<String> addtlMsgs = new ArrayList<String>();
        // look for the msg about the conflicting partner config settings
        addtlMsgs.add("CWWKG0102I");
        testSAMLServer.reconfigServer("server_duplicateSAMLId.xml", _testName, addtlMsgs, SAMLConstants.JUNIT_REPORTING);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

        // swap order of providers in config
        testSAMLServer.reconfigServer("server_duplicateSAMLId2.xml", _testName, addtlMsgs, SAMLConstants.JUNIT_REPORTING);
        List<validationData> expectations = vData.addSuccessStatusCodes();
        // expectations for steps that are not executed will be ignored
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login form.", null, cttools.getLoginTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
        // When we try to use the SAML token, the filter doesn't match, so we
        // need to log in using form auth.
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the normal form login form.", null, SAMLConstants.STANDARD_LOGIN_HEADER);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on the normal form login form.", null, SAMLConstants.STANDARD_LOGIN_HEADER);
        expectations = vData.addExpectation(expectations, SAMLConstants.PROCESS_FORM_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get output showing that there is NO SAML token - it should not be there", null, SAMLConstants.NO_SAML_TOKEN_FOUND_MSG);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_NO_MATCH_FORMLOGIN, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_NO_MATCH_FORMLOGIN, expectations);

    }

    /**
     * Two tests within one to save reconfig and set-up time. The first
     * reconfigures a partner ID to contain a dash, testing that both SP
     * initiated and IDP initiated flows function accept the name. The second
     * part of the test uses different testSettings to update a different
     * partner ID to contain an underscore, with similar expectations to the
     * first.
     *
     * @throws Exception
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_id_dash_underscore() throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp-dash", true);

        testSAMLServer.reconfigServer("server_dash_underscoreSAMLId.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

        SAMLTestSettings updatedTestSettings2 = testSettings.copyTestSettings();
        updatedTestSettings2.updatePartnerInSettings("sp_underscore", true);
        IDP_initiated_SAML(_testName, updatedTestSettings2, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings2));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings2, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings2));

    }

    /*************************************************
     * forceAuthn
     *************************************************/

    /**
     * Config attribute: forceAuthn This test will set forceAuthn to "true" For
     * SP initiated flow, subsequent app invocations will be required to
     * authenticate. So, the second call to snoop will require a login For IDP
     * initiated flow, there should be no difference in behavior
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_forceAuthn_true() throws Exception {

        // testing both IDP and SP initiated flows in 1 test case to reduce the
        // number of reconfigs what we need to do (they're what costs so much
        // runtime)

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        testSAMLServer.reconfigServer("server_forceAuthn_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = getAndSaveWebClient();

        // make a request using IDP flow and the force flag will be set to the
        // default value which is false (We're making the call directly, we're
        // not using the SP which knows about the setting)
        List<validationData> expectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        // add a check for the forceAuthn flag setting in the IDP's log
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "The forceAuthn flag is NOT set to false in the request received by the IDP", null, "forceAuthn=false");
        printTestTrace("_testName", "Invoking IDP Initiated request");
        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);

        // make a request using Solicied SP flow and the force flag will be set
        // to the configured value which is true
        // create a new client, so, there is no question about cookie
        // contamination
        webClient = getAndSaveWebClient();
        expectations = helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "The forceAuthn flag is NOT set to true in the request received by the IDP", null, "forceAuthn=true");
        printTestTrace("_testName", "Invoking Solicited SP Initiated request");
        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /*************************************************
     * isPassive
     *************************************************/

    /**
     * Config attribute: isPassive This test uses a server with isPassive set to
     * true. It expects the IDP to not process any login requests IDP initiated
     * - No difference in behavior as our client goes directly to the login page
     * on the IDP SP initiated - Login request should NOT be processed - we'll
     * not only check for the post response, but we'll validate that we have
     * unauthenticated_user in the MessageID
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_isPassive_true() throws Exception {

        // testing both IDP and SP initiated flows in 1 test case to reduce the
        // number of reconfigs what we need to do (they're what costs so much
        // runtime)

        // test with isPassive=true
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        testSAMLServer.reconfigServer("server_isPassive_true.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = getAndSaveWebClient();

        // make a request using IDP flow and the isPassive flag will be set to
        // the default value which is false (We're making the call directly,
        // we're not using the SP which knows about the setting)
        List<validationData> expectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings);
        // add a check for the forceAuthn flag setting in the IDP's log
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "The isPassive flag is NOT set to false in the request received by the IDP", null, "isPassive=false");
        printTestTrace("_testName", "Invoking IDP Initiated request");
        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);

        // make a request using Solicied SP flow and the isPassive flag will be
        // set to the configured value which is true
        // create a new client, so, there is no question about cookie
        // contamination
        webClient = getAndSaveWebClient();
        expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "The Response from the IDP did NOT indicate a problem handling the request", null, SAMLConstants.SP_DEFAULT_ERROR_PAGE_TITLE);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "The isPassive flag is NOT set to true in the request received by the IDP", null, "isPassive=true");
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_MATCHES, "The isPassive flag is NOT set to true in the request received by the IDP", null, ".*non-proceed event.*NoPassive");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "The IDP did NOT return an error status code to the SP", SAMLMessageConstants.CWWKS5008E_STATUS_CODE_NOT_SUCCESS);
        printTestTrace("_testName", "Invoking Solicited SP Initiated request");
        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    /**
     * Config attribute: isPassive Almost all of the other FAT tests use
     * isPassive=false, so we won't code a special test for that (to save server
     * reconfig time)
     */

    /*************************************************
     * authnConextClassRef
     *************************************************/
    // TODO authnContextClassRef not implemented yet
    /**
     * Config attribute: authnConextClassRef This test sets the
     * authContextClassRev value to what is returned from our TFIM server. We
     * expect success
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_authnConextClassRef_valid() throws Exception {

        testSAMLServer.reconfigServer("server_authnContextClassRef.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: authnConextClassRef This test sets the
     * authContextClassRev value to some invalid value
     *
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void test_config_authnConextClassRef_invalid() throws Exception {

        testSAMLServer.reconfigServer("server_authnContextClassRef_invalid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("defaultSP", true);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get a SAML Post Response instead of the IDP login page", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_POST_TOKEN, SAMLConstants.STRING_CONTAINS, "SAML Token did not contain expected values", null, null);
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));

        // We test with multiple versions of Java - We need to test with Shibboleth version 3.3.1 with Java < 10 and version 4.1.0 with Java > 10
        // Shibboleth changed the order that they check the AuthenticationContext - when using 3.3.1, we have to authenticate and invoke ACS with the response before we see the failure.
        // When using 4.1.0, we get the failure on the initial request - updating the test case to handle the different behavior.
        String[] spFlow = null;
        String failingStep = null;
        if (System.getProperty("java.specification.version").matches("1\\.[789]")) {
            spFlow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW;
            failingStep = SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE;
        } else {
            spFlow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP;
            failingStep = SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST;
        }
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, failingStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "The IDP did NOT return an error status code to the SP", SAMLMessageConstants.CWWKS5008E_STATUS_CODE_NOT_SUCCESS);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, spFlow, expectations);

    }

    /*************************************************
     * nameIDFormat
     *************************************************/
    /*
     * The Shibboleth config has been updated to support/return each of our
     * supported nameId values
     */

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to email IDP
     * initiated - Value not used, so no difference in behavior SP initiated -
     * Request should be processed - nameID value in the SAMLToken will be our
     * specified user
     */
    // @Mode(TestMode.LITE)
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void test_config_nameIDFormat_email() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_email.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("defaultSP", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to unspecified
     * IDP initiated - Value not used, so no difference in behavior SP initiated
     * - Request should get an exception from the IDP - SAML Post returned with
     * SAMLResponse containing "invalid_request_nameid_policy" in MessageID
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_nameIDFormat_unspecified() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_unspecified.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to
     * x509SubjectName IDP initiated - Value not used, so no difference in
     * behavior SP initiated - Request should get a response with the value
     * test_X509SubjectNameNameIdValue for the nameID and it should be displayed
     * as the principal - it should be in the subject
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_nameIDFormat_x509SubjectName() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_x509SubjectName.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("spNameIdX509SubjectName", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to
     * windowsDomainQualifiedName IDP initiated - Value not used, so no
     * difference in behavior SP initiated - Request should get a response with
     * the value test_WindowsNameNameIdValue for the nameID and it should be
     * displayed as the principal - it should be in the subject
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_nameIDFormat_windowsDomainQualifiedName() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_windowsDomainQualifiedName.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("spNameIdWindowsDomain", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to kerberos
     * IDP initiated - Value not used, so no difference in behavior SP initiated
     * - Request should get a response with the value test_KerberosNameIdValue
     * for the nameID and it should be displayed as the principal - it should be
     * in the subject
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_nameIDFormat_kerberos() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_kerberos.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("spNameIdKerberos", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to entity IDP
     * initiated - Value not used, so no difference in behavior SP initiated -
     * Request should get a response with the value test_EntityNameIdValue for
     * the nameID and it should be displayed as the principal - it should be in
     * the subject
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_nameIDFormat_entity() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_entity.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("spNameidEntity", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to persistent
     * IDP initiated - Value not used, so no difference in behavior SP initiated
     * - Request should get a response with the value testuser for the nameID
     * and it should be displayed as the principal - it should be in the subject
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_nameIDFormat_persistent() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_persistent.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("spNameIdPersistent", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to transient
     * IDP initiated - Value not used, so no difference in behavior (nameID
     * value in the SAMLToken will be our specified user SP initiated - Request
     * should get a response with the value test_EntityNameIdValue for the
     * nameID and it should be displayed as the principal - it should be in the
     * subject
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_nameIDFormat_transient() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_transient.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("spNameIdTransient", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        updatedTestSettings.setSamlTokenValidationData("test_EntityNameIdValue", updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings, SAMLConstants.STRING_CONTAINS));

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to encrypted
     * IDP initiated - Value not used, so no difference in behavior SP initiated
     * - Request should get an exception from the IDP - The IDP server is
     * failing to encrypt the nameID (error msg is: "Profile Action
     * EncryptAttributes: No encryption parameters, nothing to do") After doing
     * some research, it appears that there is a bug in the version of
     * Shibboleth that we're using, so, for now, all I can do is make sure that
     * we get this failure - that implies that our server did in fact pass the
     * correct value: "urn:oasis:names:tc:SAML:1.1:nameid-format:encrypted" as
     * the nameID in the request
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void test_config_nameIDFormat_encrypted() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_encrypted.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("spNameIdEncrypted", true);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not fail trying to use the SAML Response", null, SAMLConstants.SP_DEFAULT_ERROR_PAGE_TITLE);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not find message indicating that Shibboleth had encrypted in the request from our SP in the idp-process.log", null, "urn:oasis:names:tc:SAML:1.1:nameid-format:encrypted");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not find message indicating that the nameID could not be encrypted in the idp-process.log", null, "Profile Action EncryptAttributes: No encryption parameters, nothing to do");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not find message in messages.log indicating that the nameID is mssing in response", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: nameIDFormat This test sets nameIDFormat to customize
     * This test sets nameIDFormat to customize - customizeNameIDFormat should
     * also be set in this case, but this test case is NOT doing that - we'll do
     * that when we test customizeNameIDFormat IDP initiated - Value not used,
     * so no difference in behavior SP initiated - Request should get an
     * exception from the IDP - SAML Post returned with SAMLResponse containing
     * "invalid_request_nameid_policy" in MessageID
     */
    // @Mode(TestMode.LITE)
    // chc@Test
    public void test_config_nameIDFormat_customize() throws Exception {

        testSAMLServer.reconfigServer("server_nameIDFormat_customize.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get a SAML Post Response instead of the IDP login page", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_POST_TOKEN, SAMLConstants.STRING_CONTAINS, "SAML Token did not contain expected values", null, null);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.UNEXPECTED_EXCEPTION, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_GET_SAML_RESPONSE, expectations);

    }

    /*************************************************
     * customizeNameIDFormat
     *************************************************/

    /**
     * Config attribute: customizeNameIDFormat This test sets nameIDFormat to
     * customize and customizeNameIDFormat to someValue IDP initiated - Value
     * not used, so no difference in behavior SP initiated - Request should get
     * an exception from the IDP - SAML Post returned with SAMLResponse
     * containing "invalid_request_nameid_policy" in MessageID
     */
    // @Mode(TestMode.LITE)
    // chc@Test
    public void test_config_customizeNameIDFormat_other() throws Exception {

        testSAMLServer.reconfigServer("server_customizeNameIDFormat_other.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get a SAML Post Response instead of the IDP login page", null, cttools.getResponseTitle(updatedTestSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_POST_TOKEN, SAMLConstants.STRING_CONTAINS, "SAML Token did not contain expected values", null, null);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.INVALID_REQUEST_NAMEID_POLICY, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    /**
     * Config attribute: customizeNameIDFormat This test sets nameIDFormat to
     * customize and customizeNameIDFormat to email
     * (urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress) IDP initiated -
     * Value not used, so no difference in behavior SP initiated - Request
     * should be processed - nameID value in the SAMLToken will be our specified
     * user
     */
    // @Mode(TestMode.LITE)
    // chc@Test
    public void test_config_customizeNameIDFormat_email() throws Exception {

        testSAMLServer.reconfigServer("server_customizeNameIDFormat_email.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // test with forceAuthn=true = subsequent app invocations should require
        // their own logins
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("defaultSP", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /*************************************************
     * idpMetadata
     *************************************************/

    /**
     * Config attribute: idpMetadata This test specifies an idpMetaData file
     * that does not exist - Cert used by test IS in the specified trust store
     * We should see a message logged in the server log that indicates that the
     * file was not found IDP initiated - Certificate validation should be done
     * using the trustStore (trustStoreRef) SP initiated - Should fail as we
     * can't determine what/where the IDP is
     */
    // @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_idpMetadata_invalid() throws Exception {

        testSAMLServer.reconfigServer("server_idpMetadata_invalid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // TODO change expectations when code is added to check the cert against
        // the trust store if the metadata doesn't exist
        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        // expectations = vData.addExpectation(expectations,
        // SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE,
        // SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did
        // not get message recording that idpMetadata file was not found.",
        // null, SAMLMessageConstants.CWWKS5025E_IDP_METADATA_DOES_NOT_EXIST) ;
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message recording that provider was not found.", SAMLMessageConstants.CWWKS5045E_INVALID_ISSUER);

        List<validationData> expectations2 = msgUtils.addForbiddenExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null);
        // expectations2 = vData.addExpectation(expectations2,
        // SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST,
        // SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did
        // not get message recording that idpMetadata file was not found.",
        // null, SAMLMessageConstants.CWWKS5025E_IDP_METADATA_DOES_NOT_EXIST) ;
        expectations2 = helpers.addMessageExpectation(testSAMLServer, expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message recording that provider was not found.", SAMLMessageConstants.CWWKS5079E_CANNOT_FIND_IDP_URL_IN_METATDATA);
        //
        // List<validationData> expectations2 =
        // vData.addSuccessStatusCodes(null,
        // SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST);
        // expectations2 = vData.addResponseStatusExpectation(expectations2,
        // SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST,
        // SAMLConstants.FORBIDDEN_STATUS) ;
        // expectations2 = vData.addExpectation(expectations2,
        // SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST,
        // SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did
        // not get message recording that idpMetadata file was not found.",
        // null, SAMLConstants.FORBIDDEN) ;
        // expectations2 = vData.addExpectation(expectations2,
        // SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST,
        // SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not
        // get message recording that idpMetadata file was not found.", null,
        // SAMLConstants.AUTHENTICATION_FAILED) ;
        // expectations2 = vData.addExpectation(expectations2,
        // SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST,
        // SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did
        // not get message recording that idpMetadata file was not found.",
        // null, SAMLMessageConstants.CWWKS5025E_IDP_METADATA_DOES_NOT_EXIST) ;
        // expectations2 = vData.addExpectation(expectations2,
        // SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST,
        // SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did
        // not get message recording that provider was not found.", null,
        // "CWWKS5037E") ;
        // expectations2 = vData.addExpectation(expectations2,
        // SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST,
        // SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did
        // not get message recording that trust association could not be
        // loaded.", null,
        // SAMLMessageConstants.CWWKS5080E_IDP_MEDATA_MISSING_IN_CONFIG) ;

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations2);

    }

    // TODO - Add a test that has a bad idpMetadata value and the cert is
    // missing from the trustStoreRef
    /**
     * Config attribute: idpMetadata This test specifies an idpMetaData file
     * that does not exist - Cert used by test IS NOT in the specified trust
     * store We should see a message logged in the server log that indicates
     * that the file was not found IDP initiated - Certificate validation will
     * fail because the needed cert is NOT in the trustStore (trustStoreRef) SP
     * initiated - Should fail as we can't determine what/where the IDP is
     */
    // @Mode(TestMode.LITE)
    // @ExpectedFFDC(value={"com.ibm.ws.security.saml.error.SamlException"})
    // @Test
    // public void test_config_idpMetadata_invalid_certMissingFromTrustStore()
    // throws Exception {
    // }

    /**
     * Config attribute: idpMetadata This test does not specify an idpMetaData
     * file The default idpmetadata file does NOT exist IDP initiated -
     * Certificate validation should be done using the trustStore
     * (trustStoreRef) SP initiated - Should fail as we can't determine
     * what/where the IDP is
     */
    // TODO - probably should have a test where we do have the default
    // IDPMetaData file (and things should work) - the default config tests
    // do have the default idpmetadata file, that's probably enough
    // TODO - with/without cert in trust varations too
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_idpMetadata_notSpecified() throws Exception {

        testSAMLServer.reconfigServer("server_idpMetadata_notSpecified.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail to find an identity provider.", SAMLMessageConstants.CWWKS5045E_INVALID_ISSUER);

        List<validationData> expectations2 = msgUtils.addForbiddenExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null);
        expectations2 = helpers.addMessageExpectation(testSAMLServer, expectations2, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail to find an identity provider.", SAMLMessageConstants.CWWKS5080E_IDP_MEDATA_MISSING_IN_CONFIG);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations2);

    }

    // have tests in BasicIDPInitiatedTest.java and
    // BasicSolicitedSPInitiatedTest.java that test with a provider that is not
    // part of the federation specified in the config:
    // testSaml_idpInitiated_noIdAssertNoUser_IDPSign_IDPEncrypt_FederationMismatch()
    // testSaml_solicitedSPInitiated_noIdAssertNoUser_IDPSign_IDPEncrypt_FederationMismatch()
    // testSaml_solicitedSPInitiated_

    /*************************************************
     * loginPageURL
     *************************************************/

    // LoginPageURL using a valid jsp (testIDPCLient.jsp) is tested in the
    // BasicSAMLTests class.
    // (there are many test methods using that specification - so we won't test
    // it any more here
    // we'll focus on testing invalid values and the encoded(logininitial url)
    /**
     * Config attribute: loginPageURL This test will test specifying an encoded
     * version of logininitial Test is only valid with unsolicited SP flow - as
     * the existance of loginPageURL is what dictates the difference between
     * unsolicited and solicited.
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_loginPageURL_encoded_logininitial_url() throws Exception {

        testSAMLServer.reconfigServer(testSettings.getIdpRoot() + "_server_unsolicited_loginPageURL_logininitial_encoded.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        // Because of the content of the loginPageURL(logininitial on the IDP),
        // the flow will behave like Solicited SP, so check against that instead
        // of unsolicited)
        unsolicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_KEEPING_COOKIES, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: loginPageURL This test will test specifying an encoded
     * version of logininitial Test is only valid with unsolicited SP flow - as
     * the existance of loginPageURL is what dictates the difference between
     * unsolicited and solicited. The test expects an error indicating that
     * there is a missing parm
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_loginPageURL_encoded_logininitial_url_noParms() throws Exception {

        testSAMLServer.reconfigServer(testSettings.getIdpRoot() + "_server_unsolicited_loginPageURL_logininitial_encoded_noParms.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));

        // set expectations for Unsolicited SP flow
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST);
        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not find message indicating that the providerId parm was missing in the idp-process.log", null, "Shibboleth Authentication Request message did not contain the providerId query parameter.");

        unsolicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    /**
     * Config attribute: loginPageURL This test will test specifying a
     * non-encoded version of logininitial Test is only valid with unsolicited
     * SP flow - as the existance of loginPageURL is what dictates the
     * difference between unsolicited and solicited. Please note that the
     * server.xml must use &amp; in place of just & because & is a special
     * character in xml.
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_loginPageURL_non_encoded_logininitial_url() throws Exception {

        testSAMLServer.reconfigServer(testSettings.getIdpRoot() + "_server_unsolicited_loginPageURL_logininitial_nonencoded.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        // Because of the content of the loginPageURL(logininitial on the IDP),
        // the flow will behave like Solicited SP, so check against that instead
        // of unsolicited)
        unsolicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_KEEPING_COOKIES, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));

    }

    /**
     * Config attribute: loginPageURL This test will test specifying an encoded
     * version of logininitial Test is only valid with unsolicited SP flow - as
     * the existance of loginPageURL is what dictates the difference between
     * unsolicited and solicited.
     */
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_loginPageURL_invalid() throws Exception {

        testSAMLServer.reconfigServer("server_unsolicited_loginPageURL_invalid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.SRVE0190E_FILE_NOT_FOUND);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
        unsolicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_ONLY_SP, msgUtils.addNotFoundExpectation(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, null));

    }

    /*************************************************
     * errorPageURL
     *************************************************/
    // TODO - Error used to invoke the correct error page are not generating
    // messages in the server side logs
    // that is not something that this test should be checking - we should add
    // some tests for these conditions to
    // our general test bucket
    /**
     * Config attribute: errorPageURL This test will test specifying valid
     * errorPageURL values Test shows that the defined errorPageURL is used
     * instead of the default error page Test shows that the errorPageURL from
     * the correct SP is used. Test shows that we get the SP page for an SP
     * incurred error, errors from the IDP display the IDP's error page. So,
     * several calls will be made (really should be multiple tests, but to save
     * time (not having to reconfig multiple times) we'll do all of this in one
     * test)
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "org.opensaml.xml.signature.SignatureException" })
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_errorPageURL() throws Exception {

        testSAMLServer.reconfigServer("server_errorPageURL.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        // SP1 requires SHA256, but the IDP's SP1 returns SHA1
        SAMLTestSettings updatedTestSettings1 = testSettings.copyTestSettings();
        updatedTestSettings1.updatePartnerInSettings("sp1", true);
        List<validationData> expectations1 = vData.addSuccessStatusCodes();
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not Land on the SP1 error page.", null, SAMLConstants.SP1_ERROR_PAGE_TITLE);

        SAMLTestSettings updatedTestSettings2 = testSettings.copyTestSettings();
        updatedTestSettings2.updatePartnerInSettings("sp2", true);
        updatedTestSettings2.setRemoveTagInResponse("ds:Signature");
        List<validationData> expectations2 = vData.addSuccessStatusCodes();
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not Land on the SP2 error page.", null, SAMLConstants.SP2_ERROR_PAGE_TITLE);

        SAMLTestSettings updatedTestSettings3 = testSettings.copyTestSettings();
        updatedTestSettings3.updatePartnerInSettings("sp1", true);
        updatedTestSettings3.setIdpUserName("badUser");
        List<validationData> expectations3 = vData.addSuccessStatusCodes();
        expectations3 = vData.addExpectation(expectations3, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not Land on the TFIM IDP error page.", null, SAMLConstants.SAML_TFIM_ERROR_HEADER);

        SAMLTestSettings updatedTestSettings4 = testSettings.copyTestSettings();
        updatedTestSettings4.setRemoveTagInResponse("ds:Signature");
        List<validationData> expectations4 = vData.addSuccessStatusCodes();
        expectations4 = vData.addExpectation(expectations4, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not Land on the default SP error page.", null, SAMLConstants.SP_DEFAULT_ERROR_PAGE_TITLE);

        SAMLTestSettings updatedTestSettings5 = testSettings.copyTestSettings();
        updatedTestSettings5.updatePartnerInSettings("sp13", true);
        updatedTestSettings5.setRemoveTagInResponse("ds:Signature");
        List<validationData> expectations5 = vData.addSuccessStatusCodes();
        expectations5 = vData.addExpectation(expectations5, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not Land on the default SP error page.", null, SAMLConstants.SP_DEFAULT_ERROR_PAGE_TITLE);

        // make sure we get the error page defined for SP1, not SP2, nor the
        // default "SP" error page
        // IDP_initiated_SAML(_testName, updatedTestSettings1,
        // SAMLConstants.IDP_INITIATED_FLOW, expectations1);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings1, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations1);

        // make sure we get the error page defined for SP2, not SP1, nor the
        // default "SP" error page
        IDP_initiated_SAML(_testName, updatedTestSettings2, SAMLConstants.IDP_INITIATED_FLOW, expectations2);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings2, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations2);

        // make sure that with SP error pages defined, we get the TFIM error
        // page when the error occurs on the IDP
        IDP_initiated_SAML(_testName, updatedTestSettings3, SAMLConstants.IDP_INITIATED_GET_SAML_TOKEN, expectations3);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings3, SAMLConstants.SOLICITED_SP_INITIATED_GET_SAML_RESPONSE, expectations3);

        // make sure we get the default SP error page not the one defined for
        // SP1, or SP2
        IDP_initiated_SAML(_testName, updatedTestSettings4, SAMLConstants.IDP_INITIATED_FLOW, expectations4);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings4, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations4);

        // make sure we get the default SP error page not the one defined for
        // SP1, or SP2
        IDP_initiated_SAML(_testName, updatedTestSettings5, SAMLConstants.IDP_INITIATED_FLOW, msgUtils.addNoSPExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings5, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations5);

        testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID, SAMLMessageConstants.CWWKS5048E_ERROR_VERIFYING_SIGNATURE, SAMLMessageConstants.CWWKS5004E_SP_NOT_CONFIGURED, SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY);
    }

    /**
     * Config attribute: errorPageURL This test will test specifying some bad
     * value (non existant url) Test shows that we get a decent
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException" })
    @AllowedFFDC(value = { "com.ibm.ws.jsp.webcontainerext.JSPErrorReport" })
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_errorPageURL_invalid() throws Exception {

        testSAMLServer.reconfigServer("server_errorPageURL_invalid.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.SRVE0190E_FILE_NOT_FOUND);

        SAMLTestSettings updatedTestSettings1 = testSettings.copyTestSettings();
        updatedTestSettings1.updatePartnerInSettings("sp1", true);
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE);
        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.NOT_FOUND_STATUS);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not fail to find the specified error page. (full)", null, "Failed to find resource");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to find the specified error page. (message)", null, SAMLConstants.NOT_FOUND_UPPERCASE);

        // make sure we get the default error page indicating that the real
        // error page could not be found because it didn't exist.
        IDP_initiated_SAML(_testName, updatedTestSettings1, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings1, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /*************************************************
     * clockSkew
     *************************************************/

    /**
     * Config attribute: clockSkew This test is configured with a clockSkew set
     * to 30 seconds The test sets the IssueInstant time ahead more than the
     * clockskew. The test sets the IssueInstant time behind more than the
     * clockskew. IDP Initiated - In both cases, we should get a message
     * indicating that the token was not issued in the appropriate timeframe SP
     * Initiated - In both cases, we should get a message indicating that the
     * token was not issued in the appropriate timeframe Note: other time tests
     * (in the Base FAT may use clockskew to test NotBefore, NotOnOrAfter, ...
     * settings)
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_clockSkew_issueBeyondClockSkew() throws Exception {

        testSAMLServer.reconfigServer("server_clockSkew_30s.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        printTestTrace("test_config_clockSkew_issueBeyondClockSkew", "SP time before IDP Issue minus clock skew");

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_ISSUE_INSTANT, SAMLConstants.ADD_TIME, SAMLTestSettings.setTimeArray(0, 0, 5, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a clockSkew error.", SAMLMessageConstants.CWWKS5011E_ISSUE_INSTANT_OUT_OF_RANGE);

        // make sure we get the error page indicating that we're not authorized
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

        /**************************/
        printTestTrace("test_config_clockSkew_issueBeyondClockSkew", "SP time after IDP Issue minus clock skew");

        SAMLTestSettings updatedTestSettings2 = testSettings.copyTestSettings();
        updatedTestSettings2.updatePartnerInSettings("sp1", true);
        updatedTestSettings2.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings2.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_ISSUE_INSTANT, SAMLConstants.SUBTRACT_TIME, SAMLTestSettings.setTimeArray(0, 0, 5, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        // same expectations

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: clockSkew This test is configured with a clockSkew set
     * to 0 seconds - we want to make sure that the clockskew is NOT overridden
     * with the default value of 5 min The test sets the IssueInstant time ahead
     * more than the clockskew. The test sets the IssueInstant time behind more
     * than the clockskew. IDP Initiated - In both cases, we should get a
     * message indicating that the token was not issued in the appropriate
     * timeframe SP Initiated - In both cases, we should get a message
     * indicating that the token was not issued in the appropriate timeframe
     * Note: other time tests (in the Base FAT may use clockskew to test
     * NotBefore, NotOnOrAfter, ... settings)
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    // @Mode(TestMode.LITE)
    @Test
    public void test_config_clockSkew_0() throws Exception {

        testSAMLServer.reconfigServer("server_clockSkew_0s.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        printTestTrace("test_config_clockSkew_0", "SP time before IDP Issue minus clock skew");

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_ISSUE_INSTANT, SAMLConstants.ADD_TIME, SAMLTestSettings.setTimeArray(0, 0, 5, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail with a clockSkew error.", SAMLMessageConstants.CWWKS5011E_ISSUE_INSTANT_OUT_OF_RANGE);

        // make sure we get the error page indicating that we're not authorized
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

        /**************************/
        printTestTrace("test_config_clockSkew_0", "SP time after IDP Issue minus clock skew");

        SAMLTestSettings updatedTestSettings2 = testSettings.copyTestSettings();
        updatedTestSettings2.updatePartnerInSettings("sp1", true);
        updatedTestSettings2.setRemoveTagInResponse("ds:Signature");
        updatedTestSettings2.setSamlTokenUpdateTimeVars(SAMLConstants.SAML_ISSUE_INSTANT, SAMLConstants.SUBTRACT_TIME, SAMLTestSettings.setTimeArray(0, 0, 5, 0), SAMLConstants.DO_NOT_USE_CURRENT_TIME);

        // same expectations

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }
    // Chunlong says that they're going to remove tokenReplayTimeout attribute from the config - replay attack test is already in the basic SAML Fat
    //
    //	/************************************************* tokenReplayTimeout *************************************************/
    //
    //	/**
    //	 * Config attribute: tokenReplayTimeout
    //	 * This test will use the default tokenReplayTimeout value.  It will try to resu
    //	 */
    ////	@Mode(TestMode.LITE)
    //	@ExpectedFFDC(value={"com.ibm.ws.security.saml.error.SamlException"})
    //	@Test
    //	public void test_config_tokenReplayTimeout_default_replayAttack() throws Exception {
    //
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true) ;
    //
    //
    //		// TODO - code up correct expectations when IDP initiated flow replay attack code is added
    //		// IDP flow should fail in a way similar to SP
    //
    //		List<validationData> expectations = helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings) ;
    //			//		List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE);
    //			//		expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.NOT_FOUND_STATUS)  ;
    //			//		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, SAMLConstants.SAML_POST_RESPONSE);
    //			//		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
    //			//		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not received exception that target app was missing.", null, SAMLMessageConstants.CWWKS5003E_ENDPOINT_NOT_SUPPORTED) ;
    //			//		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, SAMLConstants.APP1_TITLE);
    //
    //		List<validationData> expectations2 = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, null);
    //		expectations2 = vData.addExpectation(expectations2, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive a failure due to a replay attack.", null, SAMLMessageConstants.CWWKS5029E_RELAY_STATE_NOT_RECOGNIZED);
    //
    //		IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_AGAIN, expectations) ;
    //		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_AGAIN, expectations2);
    //
    //	}
    //
    //	/**
    //	 * Config attribute: tokenReplayTimeout
    //	 * This test
    //	 */
    //	@Mode(TestMode.LITE)
    //	@Test
    //	public void test_config_tokenReplayTimeout_waitBeyondTimeout() throws Exception {
    //
    //		testSAMLServer.reconfigServer("server_tokenReplayTimeout_30secs.xml", _testName, commongAddtlMsgs, SAMLConstants.JUNIT_REPORTING) ;
    //
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true) ;
    //		updatedTestSettings.setTokenReuseSleep(45);
    //
    //		IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_AGAIN, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings)) ;
    //		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_AGAIN, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings)) ;
    //
    ////		List<validationData>  expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, null);
    ////		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive a failure due to a replay attack.", null, "CWWKS5029W");
    ////		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive a failure due to a replay attack.", null, "CWWKS5030E");
    ////		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_AGAIN, expectations);
    //
    //	}

}
