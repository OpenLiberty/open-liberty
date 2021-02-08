/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/*
 *
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class TrustedIssuerSAMLTests extends SAMLCommonTest {

    //	private static final Class<?> thisClass = TrustedIssuerSAMLTests.class;

    /**
     * The testing SP - sp1 has a valid issuer in the IDPMetaData as well as
     * valid issuers listed in TrustedIssuers. (all other settings are valid)
     * The test should have a successful outcome
     */
    @Test
    public void TrustedIssuerSAMLTests_validIssuer_IDPMetaData_validIssuer_TrustedIssuer() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
    }

    /**
     * The testing SP - sp2 has a valid issuer in the IDPMetaData, but invalid
     * issuers listed in TrustedIssuers. (all other settings are valid)
     * The test should have a successful outcome because the issuer is found in the meta data.
     */
    @Test
    public void TrustedIssuerSAMLTests_validIssuer_IDPMetaData_invalidIssuer_TrustedIssuer() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
    }

    /**
     * The testing SP - sp13 has an invalid issuer in the IDPMetaData, but
     * valid issuers listed in TrustedIssuers. (all other settings are valid)
     * The test should have a successful outcome because the issuer is found in
     * the TrustedIssuers
     */
    @Mode(TestMode.LITE)
    @Test
    public void TrustedIssuerSAMLTests_invalidIssuer_IDPMetaData_validIssuer_TrustedIssuer() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp13", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
    }

    /**
     * The testing SP - sp1 has an invalid issuer in the IDPMetaData as well as
     * invalid issuers listed in TrustedIssuers. (all other settings are valid)
     * The test should have an unsuccessful outcome as no trust can be established
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void TrustedIssuerSAMLTests_invalidIssuer_IDPMetaData_invalidIssuer_TrustedIssuer() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (snoop)", SAMLMessageConstants.CWWKS5045E_INVALID_ISSUER);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }

    /**
     * The testing SP - sp1 has no IDPMetaData file.
     * It has valid issuers listed in TrustedIssuers. (all other settings are valid)
     * The test should have an successful outcome for IDP and Unsolicted SP flows,
     * Solicted SP flow will fail as no trust can be established
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void TrustedIssuerSAMLTests_missing_IDPMetaData_validIssuer_TrustedIssuer() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_trustedIssuers_noIDPMetaData.xml"), _testName, null, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        List<validationData> expectations;
        String[] thisFlow;
        // Solicted SP flow will fail without the idpMetaData file
        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail to find the url in the IDP MetaData", SAMLMessageConstants.CWWKS5080E_IDP_MEDATA_MISSING_IN_CONFIG);
            thisFlow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP;
        } else {
            expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
            thisFlow = standardFlow;
        }

        genericSAML(_testName, webClient, updatedTestSettings, thisFlow, expectations);
    }

    /**
     * The testing SP - sp2 has no IDPMetaData file.
     * It has invalid issuers listed in TrustedIssuers. (all other settings are valid)
     * The test should have an unsuccessful outcome for all flows as no trust can be established
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void TrustedIssuerSAMLTests_missing_IDPMetaData_invalidIssuer_TrustedIssuer() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_trustedIssuers_noIDPMetaData.xml"), _testName, null, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did NOT fail due to an invalid issuer", SAMLMessageConstants.CWWKS5045E_INVALID_ISSUER);
        String[] thisFlow;
        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            thisFlow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP;
        } else {
            thisFlow = standardFlow;
        }
        genericSAML(_testName, webClient, updatedTestSettings, thisFlow, expectations);
    }

    /**
     * The testing SP - sp13 has an invalid issuer in the IDPMetaData, but
     * has ALL_ISSUERS listed in TrustedIssuers. (all other settings are valid)
     * The test should have a successful outcome because the issuer is found in
     * the TrustedIssuers
     */
    @Test
    public void TrustedIssuerSAMLTests_invalidIssuer_IDPMetaData_ALL_ISSUERS_TrustedIssuer() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_trustedIssuers_ALL_ISSUERS.xml"), _testName, null, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp13", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
    }

    /**
     * The testing SP - sp1 has an invalid issuer in the IDPMetaData as well as
     * invalid issuers listed in TrustedIssuers. (all other settings are valid)
     * The test should have an unsuccessful outcome as no trust can be established
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void TrustedIssuerSAMLTests_invalidIssuer_IDPMetaData_invalidIssuer_and_ALL_ISSUERS_TrustedIssuer() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_trustedIssuers_ALL_ISSUERS.xml"), _testName, null, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
    }
}
