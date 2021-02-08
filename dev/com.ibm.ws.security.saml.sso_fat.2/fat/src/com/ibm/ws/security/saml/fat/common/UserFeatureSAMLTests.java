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
import com.ibm.ws.security.saml20.fat.commonTest.SampleUserFeatureInstaller;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/*
 *
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class UserFeatureSAMLTests extends SAMLCommonTest {

    private static final Class<?> thisClass = UserFeatureSAMLTests.class;

    static {
        samlUserMappingUserFeature = new SampleUserFeatureInstaller(); // install the user Feature
    }

    /**
     * Utility method to add common expectations
     * Adds checks for msgs logged by the user feature to ensure that the correct methods are called
     * Adds checks for user/realm in the app output
     *
     * @param type
     *            - mapToUserRegistry type (No, User, Group)
     * @param beforeUser
     *            - id in the saml response
     * @param afterUser
     *            - id that user feature is to map id to
     * @param expectations
     *            - existing expectations (we'll add to those)
     * @return
     * @throws Exception
     */
    List<validationData> addUserMapMessages(String type, String beforeUser, String afterUser, List<validationData> expectations) throws Exception {

        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_MATCHES, "Did not find message in messages.log showing that the user is being mapped", null, ".*mapSAMLAssertionToUser.*oldId:" + beforeUser + ".*" + "newUserID:" + afterUser + ".*");

        if (type.equals(SAMLConstants.SAML_MAPTOUSERREGISTRY_GROUP)) {
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_MATCHES, "Did not find message in messages.log showing that the group is being mapped", null, ".*mapSAMLAssertionToGroups.*Employee.*SampleSamlResolver1_group.*");
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_MATCHES, "Did not find message in messages.log showing that the userUniqueID is being mapped", null, ".*mapSAMLAssertionToUserUniqueID.*user:SampleSamlResolver1_realm/" + afterUser + "*");
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_MATCHES, "Did not find message in messages.log showing that the realm is being mapped", null, ".*mapSAMLAssertionToRealm.*SampleSamlResolver1_realm.*");
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct unique user identity in the realm in the repsonse output (SimpleServlet)", null, "realmSecurityName=SampleSamlResolver1_realm/" + afterUser);
        } else {
            if (afterUser.equals("null") || afterUser.isEmpty()) {
                expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct unique user identity in the realm in the repsonse output (SimpleServlet)", null, "realmSecurityName=BasicRealm/" + beforeUser);
            } else {
                expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct unique user identity in the realm in the repsonse output (SimpleServlet)", null, "realmSecurityName=BasicRealm/" + afterUser);
            }
        }

        return expectations;
    }

    /**
     * This test uses a server with the user feature included and configured
     * This test uses user1 which the feature will replace with user2. The
     * test will search for user2 in the output
     */
    @Mode(TestMode.LITE)
    @Test
    public void userFeatureSAMLTests_mainFlowTest() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setIdpUserName("user1");
        updatedTestSettings.setIdpUserPwd("security");
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), updatedTestSettings.getSamlTokenValidationData().getEncryptAlg());

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (snoop)", null, "<tr><td>User Principal</td><td>user2</td></tr>");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (SimpleServlet)", null, "WSPrincipal:user2");
        expectations = addUserMapMessages(SAMLConstants.SAML_MAPTOUSERREGISTRY_USER, "user1", "user2", expectations);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, expectations);

    }

    /**
     * This test uses a server with the user feature included and configured
     * This test uses fimuser which is in both the IDP's and local registry, but it is NOT
     * mapped by the user feature. The user feature will throw an exception which the
     * SAML code should
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void userFeatureSAMLTests_userNotHandledByUserFeature() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the UserCredentialResolver fail", SAMLMessageConstants.CWWKS5076E_USERCREDENTIALRESOLVER_FAILED);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * This test uses a server with a user feature included and configured
     * This test uses user4 which is in the IDP's and local registry, but the user is mapped to an
     * empty string "". The return of the empty string will cause the saml code to use
     * the normal methods and we will use user4 after all.
     */
    @Test
    public void userFeatureSAMLTests_usrFeatureReturnsEmptyString() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setIdpUserName("user4");
        updatedTestSettings.setIdpUserPwd("security");
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), updatedTestSettings.getSamlTokenValidationData().getEncryptAlg());

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (snoop)", null, "<tr><td>User Principal</td><td>user4</td></tr>");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (SimpleServlet)", null, "WSPrincipal:user4");
        expectations = addUserMapMessages(SAMLConstants.SAML_MAPTOUSERREGISTRY_USER, "user4", "", expectations);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, expectations);

    }

    /**
     * This test uses a server with a user feature included and configured
     * This test uses user3 which is in the IDP's and local registry, but the user is mapped to
     * null. The return of the null will cause the saml code to use
     * the normal methods and we will use user3 after all.
     */
    @Test
    public void userFeatureSAMLTests_usrFeatureReturnsNullString() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setIdpUserName("user3");
        updatedTestSettings.setIdpUserPwd("security");
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), updatedTestSettings.getSamlTokenValidationData().getEncryptAlg());

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (snoop)", null, "<tr><td>User Principal</td><td>user3</td></tr>");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (SimpleServlet)", null, "WSPrincipal:user3");
        expectations = addUserMapMessages(SAMLConstants.SAML_MAPTOUSERREGISTRY_USER, "user3", "null", expectations);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, expectations);

    }

    /**
     * This test uses a server with a user feature included and configured
     * This test uses user2 which is in the IDP's and local registry, but the user is mapped to a
     * user not in the local registry. The server config uses mapToUserRegistry is set to User.
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void userFeatureSAMLTests_usrFeatureReturnsUserNotInLocalRegistry() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setIdpUserName("user2");
        updatedTestSettings.setIdpUserPwd("security");
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), updatedTestSettings.getSamlTokenValidationData().getEncryptAlg());

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive a messages about an invalid user", "CWWKS1106A");
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive a messages about an invalid user", SAMLMessageConstants.CWWKS5072E_AUTHN_UNSUCCESSFUL);
        expectations = addUserMapMessages(SAMLConstants.SAML_MAPTOUSERREGISTRY_USER, "user2", "user7", expectations);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * This test uses a server with the user feature included and configured
     * This test uses user1 which the feature will replace with user2. The
     * test will search for user2 in the output
     */
    @Test
    public void userFeatureSAMLTests_mapToUserRegistry_User_goodIdentifiers() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);
        updatedTestSettings.setIdpUserName("user1");
        updatedTestSettings.setIdpUserPwd("security");
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), updatedTestSettings.getSamlTokenValidationData().getEncryptAlg());

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (snoop)", null, "<tr><td>User Principal</td><td>user2</td></tr>");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (SimpleServlet)", null, "WSPrincipal:user2");
        expectations = addUserMapMessages(SAMLConstants.SAML_MAPTOUSERREGISTRY_USER, "user1", "user2", expectations);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, expectations);

    }

    /**
     * This test uses a server with the user feature included and configured
     * This test uses user1 which the feature will replace with user2. The
     * test will search for user2 in the output
     */
    @Test
    public void userFeatureSAMLTests_mapToUserRegistry_Group_goodIdentifiers() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp13", true);
        updatedTestSettings.setIdpUserName("user1");
        updatedTestSettings.setIdpUserPwd("security");
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), updatedTestSettings.getSamlTokenValidationData().getEncryptAlg());

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (snoop)", null, "<tr><td>User Principal</td><td>user2</td></tr>");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (SimpleServlet)", null, "WSPrincipal:user2");
        expectations = addUserMapMessages(SAMLConstants.SAML_MAPTOUSERREGISTRY_GROUP, "user1", "user2", expectations);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, expectations);

    }

}
