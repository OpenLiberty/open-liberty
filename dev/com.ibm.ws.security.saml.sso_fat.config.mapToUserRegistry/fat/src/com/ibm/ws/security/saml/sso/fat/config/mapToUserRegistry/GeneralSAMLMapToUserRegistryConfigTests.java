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
package com.ibm.ws.security.saml.sso.fat.config.mapToUserRegistry;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class GeneralSAMLMapToUserRegistryConfigTests extends SAMLCommonTest {

    // TODO - Product not fully baked yet - keyStoreRef (with encryption in mind), keyPassword, trustStoreRef, authnContextClassRef, tokenReplayTimeout
    /**
     * The tests in the class will ensure the proper function/handling of SAML configuration.
     * It will ensure the proper handling of all settings.
     * The tests will make sure that we get the proper error messages and ensure that these messages
     * are appropriate for the audience that receives them.
     * These tests will need to use different partners and federations on the IDP servers
     * that we test with.
     */

    private static final Class<?> thisClass = GeneralSAMLMapToUserRegistryConfigTests.class;
    private final Boolean runSolicitedSPInitiatedTests = true;
    private final Boolean runUnsolicitedSPInitiatedTests = true;
    private final Boolean runIDPInitiatedTests = true;

    List<String> commonAddtlMsgs = new ArrayList<String>();;
    // would like to add passwords for keystores and fimuser (Liberty, LibertyServer, fimuser), but
    // they conflict with valid log entries (change Liberty to LibertyPW, LibertyServer to LibertyServerPW, fimuser to fimuserPW, ...)
    static String[] pwList = { "testuserpwd", "user1pwd", "user2pwd", "keyspass", "samlSPPass", "invalidPassword" };

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        startSPWithIDPServer("com.ibm.ws.security.saml.sso-2.0_fat.config.mapToUserRegistry", "server_orig.xml", extraMsgs, extraApps, true);

        testSAMLServer.setPasswordsUsedWithServer(pwList);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

    }

    // Convenience methods to allow us to write tests to call both IDP and SP initiated flows and enable us to set global flags to
    // enable/disable either of those flows
    public Object solicited_SP_initiated_SAML(String testName, SAMLTestSettings updatedTestSettings, String[] actions,
            List<validationData> expectations) throws Exception {
        if (runSolicitedSPInitiatedTests) {
            printTestTrace("solicited_SP_initiated_SAML", "Running Solicited SP Initiated SAML Flow");
            WebClient webClient = SAMLCommonTestHelpers.getWebClient();
            return genericSAML(testName, webClient, updatedTestSettings, actions, expectations);
        } else {
            return null;
        }

    }

    public Object IDP_initiated_SAML(String testName, SAMLTestSettings updatedTestSettings, String[] actions,
            List<validationData> expectations) throws Exception {
        if (runIDPInitiatedTests) {
            printTestTrace("IDP_initiated_SAML", "Running IDP Initiated SAML Flow");
            WebClient webClient = SAMLCommonTestHelpers.getWebClient();
            return genericSAML(testName, webClient, updatedTestSettings, actions, expectations);
        } else {
            return null;
        }

    }

    public Object unsolicited_SP_initiated_SAML(String testName, SAMLTestSettings updatedTestSettings, String[] actions,
            List<validationData> expectations) throws Exception {
        if (runUnsolicitedSPInitiatedTests) {
            printTestTrace("unsolicited_SP_initiated_SAML", "Running Unsolicited SP Initiated SAML Flow");
            WebClient webClient = SAMLCommonTestHelpers.getWebClient();
            return genericSAML(testName, webClient, updatedTestSettings, actions, expectations);
        } else {
            return null;
        }

    }

    /************************************************* mapToUserRegistry *************************************************/

    /******************************** No ********************************/

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=No.
     * All Identifiers are omitted. The user is in the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_No_inRegistry_identifiersOmitted() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_No_inRegistry_identifiersOmitted.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings,
                helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings,
                helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=No.
     * All Identifiers are omited. The user is not the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_No_notInRegistry_identifiersOmitted() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_No_notInRegistry_identifiersOmitted.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings,
                helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings,
                helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=No.
     * All Identifiers are good/correct. The user is in the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_No_inRegistry_identifiersGood() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_No_inRegistry_identifiersGood.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings,
                helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.TFIM_REGISTRY));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings,
                helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.TFIM_REGISTRY));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=No.
     * All Identifiers are good/correct. The user is not the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    //	@Mode(TestMode.LITE)
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void test_config_mapToUserRegistry_No_notInRegistry_identifiersGood() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_No_notInRegistry_identifiersGood.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings,
                helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.TFIM_REGISTRY));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings,
                helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.TFIM_REGISTRY));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=No.
     * All Identifiers are omitted - except for the userIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive an exception
     * SP initiated - prompted once for login, we will receive an exception
     */
    //		@Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_mapToUserRegistry_No_inRegistry_userIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_No_inRegistry_userIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user id was undefined.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=No.
     * All Identifiers are omitted - except for the userUniqueIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive an exception
     * SP initiated - prompted once for login, we will receive an exception
     */
    //	@Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        //expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the unique user id was undefined.", null, "CWWKS5070E") ;
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the unique user id was undefined.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=No.
     * All Identifiers are omitted - except for the groupIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive and exception
     * SP initiated - prompted once for login, we will receive and exception
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_No_inRegistry_groupIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_No_inRegistry_groupIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=No.
     * All Identifiers are omitted - except for the realmIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive an exception
     * SP initiated - prompted once for login, we will receive an exception
     */
    //		@Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_mapToUserRegistry_No_inRegistry_realmIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_No_inRegistry_realmIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the realm was undefined.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /*
     * the following tests are not being run as they do NOT test anything unique (and I'd like to save a bit of time on doing
     * reconfigs)
     * test_config_mapToUserRegistry_No_notInRegistry_userIdentifierBad
     * test_config_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBad
     * test_config_mapToUserRegistry_No_notInRegistry_groupIdentifierBad
     * test_config_mapToUserRegistry_No_notInRegistry_realmIdentifierBad
     */

    /******************************** User ********************************/

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are omitted. The user is in the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_User_inRegistry_identifiersOmitted() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_inRegistry_identifiersOmitted.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.NAME_ID));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.NAME_ID));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are omitted. The user is NOT in the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "com.ibm.ws.security.saml.error.SamlException" })
    //		@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_User_notInRegistry_identifiersOmitted() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_notInRegistry_identifiersOmitted.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user did not authenticate.", "CWWKS1106A");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user did not authenticate.", SAMLMessageConstants.CWWKS5072E_AUTHN_UNSUCCESSFUL);

        // login, get saml response, try to use saml and since the user isn't in the SP registry, we get prompted AGAIN to login...
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are good. The user is in the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    //		@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_User_inRegistry_identifiersGood() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_inRegistry_identifiersGood.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.NAME_ID));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.NAME_ID));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are good. The user is NOT in the registry.
     * We will end up having to log in again.
     * IDP initiated - prompted twice for login
     * SP initiated - prompted twice for login
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_User_notInRegistry_identifiersGood() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_notInRegistry_identifiersGood.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user did not authenticate.", "CWWKS1106A");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user did not authenticate.", SAMLMessageConstants.CWWKS5072E_AUTHN_UNSUCCESSFUL);

        // login, get saml response, try to use saml and since the user isn't in the SP registry, we get prompted AGAIN to login...
        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are omitted - except for the userIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive an exception
     * SP initiated - prompted once for login, we will receive an exception
     */
    //		 @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_mapToUserRegistry_User_inRegistry_userIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_inRegistry_userIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user id was undefined.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    //	/**
    //	 * Config attribute: mapToUserRegistry
    //	 * This test specifies mapToUserRegistry=User.
    //	 * All Identifiers are omitted - except for the userIdentifier - which is bad.
    //	 * The user is in the registry.
    //	 * We will receive an exception
    //	 * IDP initiated - prompted once for login, we will receive and exception
    //	 * SP initiated - prompted once for login, we will receive and exception
    //	 */
    //	@Mode(TestMode.LITE)
    //	@ExpectedFFDC(value={"com.ibm.ws.security.saml.error.SamlException"})
    //	@Test
    //	public void test_config_mapToUserRegistry_User_notInRegistry_userIdentifierBad() throws Exception {
    //
    //		testSAMLServer.reconfigServer("server_mapToUserRegistry_User_notInRegistry_userIdentifierBad.xml", _testName, commongAddtlMsgs, SAMLConstants.JUNIT_REPORTING) ;
    //
    //		SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //		updatedTestSettings.updatePartnerInSettings("sp1", true) ;
    //
    //		List<validationData> expectations = msgUtils.addAuthenticationFailedExpectation(SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, null);
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user id was undefined.", null, "CWWKS5069E") ;
    //		expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the request falied due to an unsuccessful authentication.", null, SAMLMessageConstants.CWWKS5080E_IDP_MEDATA_MISSING_IN_CONFIG) ;
    //
    //		IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP,expectations);
    //		solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, expectations) ;
    //
    //	}

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are omitted - except for the userUniqueIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive and exception
     * SP initiated - prompted once for login, we will receive and exception
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.LOCAL_REGISTRY));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.LOCAL_REGISTRY));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are omitted - except for the groupIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive and exception
     * SP initiated - prompted once for login, we will receive and exception
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_User_inRegistry_groupIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_inRegistry_groupIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.LOCAL_REGISTRY));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.LOCAL_REGISTRY));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are omitted - except for the groupIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive and exception
     * SP initiated - prompted once for login, we will receive and exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "com.ibm.ws.security.saml.error.SamlException" })
    //		 @Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_User_notInRegistry_groupIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_notInRegistry_groupIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user id was undefined.", "CWWKS1106A");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user id was undefined.", SAMLMessageConstants.CWWKS5072E_AUTHN_UNSUCCESSFUL);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=User.
     * All Identifiers are omitted - except for the realmIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive and exception
     * SP initiated - prompted once for login, we will receive and exception
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_User_inRegistry_realmIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_User_inRegistry_realmIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.LOCAL_REGISTRY));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.LOCAL_REGISTRY));

    }

    /******************************** Group ********************************/
    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are omitted. The user is in the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_Group_inRegistry_identifiersOmitted() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_inRegistry_identifiersOmitted.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are omitted. The user is NOT in the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_Group_notInRegistry_identifiersOmitted() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_notInRegistry_identifiersOmitted.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are good. The user is in the registry.
     * We will have a successful end-to-end flow
     * IDP initiated - prompted once for login, will be able to access the app
     * SP initiated - prompted once for login, will be able to access the app
     */
    //		@Mode(TestMode.LITE)
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void test_config_mapToUserRegistry_Group_inRegistry_identifiersGood() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_inRegistry_identifiersGood.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.TFIM_REGISTRY));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.TFIM_REGISTRY));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are good. The user is NOT in the registry.
     * We will end up having to log in again.
     * IDP initiated - prompted twice for login
     * SP initiated - prompted twice for login
     */
    //	@Mode(TestMode.LITE)
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void test_config_mapToUserRegistry_Group_notInRegistry_identifiersGood() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_notInRegistry_identifiersGood.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.TFIM_REGISTRY));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.TFIM_REGISTRY));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are omitted - except for the userIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive and exception
     * SP initiated - prompted once for login, we will receive and exception
     */
    //		@Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void test_config_mapToUserRegistry_Group_inRegistry_userIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_inRegistry_userIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the user id was undefined.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are omitted - except for the userUniqueIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive an exception
     * SP initiated - prompted once for login, we will receive an exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    //		@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the unique user id was undefined.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are omitted - except for the groupIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive and exception
     * SP initiated - prompted once for login, we will receive and exception
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_Group_inRegistry_groupIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_inRegistry_groupIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are omitted - except for the groupIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive and exception
     * SP initiated - prompted once for login, we will receive and exception
     */
    //	@ExpectedFFDC(value={"com.ibm.ws.security.registry.EntryNotFoundException"})
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_Group_notInRegistry_groupIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_notInRegistry_groupIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_WITH_ALT_APP, msgUtils.addAccessIdCheck(SAMLConstants.INVOKE_ALTERNATE_APP, updatedTestSettings, helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings), SAMLConstants.SAML_TOKEN_ISSUER));

    }

    /**
     * Config attribute: mapToUserRegistry
     * This test specifies mapToUserRegistry=Group.
     * All Identifiers are omitted - except for the realmIdentifier - which is bad.
     * The user is in the registry.
     * We will receive an exception
     * IDP initiated - prompted once for login, we will receive an exception
     * SP initiated - prompted once for login, we will receive an exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    //	@Mode(TestMode.LITE)
    @Test
    public void test_config_mapToUserRegistry_Group_inRegistry_realmIdentifierBad() throws Exception {

        testSAMLServer.reconfigServer("server_mapToUserRegistry_Group_inRegistry_realmIdentifierBad.xml", _testName, commonAddtlMsgs, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not get message indicating that the realm was undefined.", SAMLMessageConstants.CWWKS5068E_MISSING_ATTRIBUTE);

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW, expectations);
        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

}
