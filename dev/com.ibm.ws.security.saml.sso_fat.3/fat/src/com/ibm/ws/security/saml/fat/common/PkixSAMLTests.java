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

import java.util.ArrayList;
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

/*
 *
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class PkixSAMLTests extends SAMLCommonTest {

    private static final Class<?> thisClass = PkixSAMLTests.class;

    /**
     * The testing SP - sp1 has an pkixTrustEngine which does not specify trustAnchor.
     * It will then get the trustStoreRef from the default SSL config.
     * The default trustStoreRef is serverStoreTfim which points to sslServerTrustTfim.jks
     * The jks file has the self-signed certificates from the tfim IdP. So, the test passes
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void pkixSAMLTests_defaultTrustAnchorTest() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

    /**
     * The testing SP - sp2 has an pkixTrustEngine which specify trustAnchor
     * as serverStoreTfim. It points to sslServerTrustTfim.jks
     * The jks file has the self-signed certificates from the tfim IdP. So, the test passes
     */
    @Test
    public void pkixSAMLTests_SpecifiedTrustAnchorTest() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));
    }

    /**
     * The testing SP - sp13 has an pkixTrustEngine which specify trustAnchor
     * as serverStoreNoTfim. It points to sslServerTrust.jks
     * The jks file does not have the self-signed certificates from the tfim IdP.
     * So, the test fails
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException" })
    @Mode(TestMode.LITE)
    @Test
    public void pkixSAMLTests_badTrustAnchorTest() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp13", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);

        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the expected message saying the signature was not trusted or valid in messages.log.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }

    /**
     * The testing SP - defaultSP has an pkixTrustEngine which specify trustAnchor
     * as serverStoreTfim. It points to sslServerTrustTfim_badRef.jks
     * The jks file does not exit. The runtime should NOT use the IDPMetaData.
     * So, the test fails
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException", "java.security.KeyStoreException" })
    @Mode(TestMode.LITE)
    @Test
    public void pkixSAMLTests_badTrustAnchorReference() throws Exception {

        String keystoreNotFoundMsg = SAMLMessageConstants.CWPKI0807W_KEYSTORE_NOT_FOUND;
        String failureLoadingKeystoreMsg = SAMLMessageConstants.CWPKI0809W_FAILURE_LOADING_KEYSTORE;
        String keystoreDidntLoadMsg = SAMLMessageConstants.CWPKI0033E_KEYSTORE_DID_NOT_LOAD;

        // The reconfig will generate errors - tell the server to ignore those now and when the server shuts down
        List<String> startupExceptions = new ArrayList<String>();
        startupExceptions.add(keystoreNotFoundMsg);
        startupExceptions.add(failureLoadingKeystoreMsg);
        startupExceptions.add(keystoreDidntLoadMsg);
        testSAMLServer.addIgnoredServerException(keystoreNotFoundMsg);
        testSAMLServer.addIgnoredServerException(failureLoadingKeystoreMsg);
        testSAMLServer.addIgnoredServerException(keystoreDidntLoadMsg);

        testSAMLServer.reconfigServer(buildSPServerName("server_pkix_badTrustAnchorRef.xml"), _testName, startupExceptions, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);

        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the expected internal server error for a java.security.KeyStoreException.", SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not see the expected message saying the signature was not trusted or valid in messages.log.", SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);
    }

    /**
     * The testing SP - sp2 has an pkixTrustEngine which specify trustAnchor
     * as serverStoreTfim. It points to sslServerTrustTfim.jks
     * The jks file has the self-signed certificates from the tfim IdP. So, the test passes
     * The IDPMetaData file has the wrong cert - this test shows that the cert is not used from the IDPMetaData file
     */
    @Mode(TestMode.LITE)
    @Test
    public void pkixSAMLTests_wrongCertInIDPMetaData() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_pkix_wrongCert.xml"), _testName, null, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp2", true);

        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings));

    }

}
