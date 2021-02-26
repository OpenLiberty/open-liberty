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
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using httpunit to simulate browser requests.
 * In this scenario, a Web client accesses a static Web page on IdP and obtains a SAML HTTP-POST link to an application
 * installed on a WebSphere SP. When the Web client invokes the SP application, it is redirected to a TFIM IdP which
 * issues a login challenge to the Web client. The Web Client fills in the login form and after a successful login,
 * receives a SAML 2.0 token from the TFIM IdP. The client invokes the SP application by sending the SAML 2.0 token in
 * the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class BasicEncryptionTests extends SAMLCommonTest {

    private final static Class<?> thisClass = BasicEncryptionTests.class;

    private final String CLASS_DEFAULT_SP = "sp_enc_aes128";
    private final String SP_ENCRYPTION_AES_128 = "sp_enc_aes128";
    private final String SP_ENCRYPTION_AES_192 = "sp_enc_aes192";
    private final String SP_ENCRYPTION_AES_256 = "sp_enc_aes256";
    private final String DEFAULT_ENCRYPTION_KEY_USER = "CN=new_user2, O=IBM, C=US";
    private final String SAML_SIGNATURE_NOT_VALIDATED = "The request cannot be fulfilled because the message received does not meet the security requirements of the login service";
    private final String SAML_ERROR_HAS_OCCURRED_TITLE = "Web Login Service - Message Security Error";

    /**
     * Test description:
     * - keyAlias: Not specified
     * - The keystore configured for SAML contains no certificates.
     * Expected results:
     * - The server should not be able to find an appropriate certificate to use for SSL communication when communicating with the
     * SP.
     * - The server's messages.log should contain the CWWKS5073E message saying the server could not find a private key from the
     * service provider.
     * - A 403 - Forbidden page should be returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    @Test
    public void testNoKeyAlias_EmptyKeystore() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_noKeyAlias_emptyKeyStore.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        unsuccessfulFlow(CLASS_DEFAULT_SP, SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not fail to find the private key for the service provider.");
    }

    /**
     * Test description:
     * - keyAlias: Not specified
     * - The configured keystore contains multiple certificates, including the one capable of decrypting the SAML response.
     * - The certificate capable of decrypting the SAML response is NOT mapped to the default key alias.
     * - The configured keystore does not contain a certificate mapped to the default key alias.
     * Expected results:
     * - The server should not be able to find an appropriate certificate to use for SSL communication when communicating with the
     * SP.
     * - The server's messages.log should contain the CWWKS5073E message saying the server could not find a private key from the
     * service provider.
     * - A 403 - Forbidden page should be returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    @Test
    public void testNoKeyAlias_MultipleCertsInKeystore_IncludesCorrectCert_CorrectCertMappedToNonDefaultAlias() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_noKeyAlias_multiCertKeyStore_missingDefaultKeyAliasCert.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        unsuccessfulFlow(CLASS_DEFAULT_SP, SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not fail to find the private key for the service provider.");
    }

    /**
     * Test description:
     * - keyAlias: Not specified
     * - The configured keystore contains one certificate - the one capable of decrypting the SAML response.
     * - The single certificate in the keystore is NOT mapped to the default key alias.
     * Expected results:
     * - Because keyAlias is not configured and there is only one certificate in the keystore, the existing certificate should be
     * used to decrypt the SAML response.
     * - Decryption should be successful and valid assertion data should be produced.
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void testNoKeyAlias_OneCertInKeystore_CorrectCert_CertMappedToNonDefaultAlias() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_noKeyAlias_singleCertKeyStore_nonDefaultKeyAliasCert.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, CLASS_DEFAULT_SP);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES128);
        successfulFlow(updatedTestSettings, CLASS_DEFAULT_SP);

    }

    /**
     * Test description:
     * - keyAlias: Not specified
     * - The configured keystore contains multiple certificates, but not one capable of decrypting the SAML response.
     * - The default key alias is in the configured keystore but is not mapped to a certificate capable of decrypting the SAML
     * response.
     * Expected results:
     * - The default SAML key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - The server's messages.log should contain the CWWKS5007E message.
     * - For IdP-initiated and unsolicited SP-initiated flows, the message should say the encrypted data could not be decrypted
     * properly.
     * - For the solicited SP-initiated flow, the message should say there was a signature computation error.
     * - A 403 - Forbidden page should be returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    @MaximumJavaLevel(javaLevel = 8) // test uses DSA cert and that is no longer supported
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException", "org.opensaml.xml.signature.SignatureException" })
    @Test
    public void testNoKeyAlias_DefaultKeyAliasInKeystore_MultipleCertsInKeystore_MissingCorrectCert() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_noKeyAlias_multiCertKeyStore_defaultKeyAliasCertWithWrongKeyAlias.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String errorMsg = SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+Failed to decrypt EncryptedData.*";
        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            errorMsg = SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+Signature computation error.*";
        }

        unsuccessfulFlow(CLASS_DEFAULT_SP, errorMsg, "Did not find message saying we failed to decrypt the encrypted data.");
    }

    /**
     * Test description:
     * - keyAlias: Not specified
     * - The configured keystore contains multiple certificates, including the one capable of decrypting the SAML response.
     * - The certificate capable of decrypting the SAML response is mapped to the default key alias.
     * Expected results:
     * - The default SAML key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - Decryption should be successful and valid assertion data should be produced.
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testNoKeyAlias_DefaultKeyAliasInKeystore_MultipleCertsInKeystore_IncludesCorrectCert() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_noKeyAlias_multiCertKeyStore_includesDefaultKeyAliasCert.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, CLASS_DEFAULT_SP);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES128);
        successfulFlow(updatedTestSettings, CLASS_DEFAULT_SP);
    }

    /**
     * Test description:
     * - keyAlias: Not specified
     * - The configured keystore contains one certificate which is mapped to the default key alias.
     * - The SAML response was encrypted using a different certificate than the one contained in the keystore.
     * Expected results:
     * - The default SAML key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - The server's messages.log should contain the CWWKS5007E message.
     * - For IdP-initiated and unsolicited SP-initiated flows, the message should say the encrypted data could not be decrypted
     * properly.
     * - For the solicited SP-initiated flow, the message should say there was a signature computation error.
     * - A 403 - Forbidden page should be returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    @MaximumJavaLevel(javaLevel = 8) // test uses DSA cert and that is no longer supported
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException", "org.opensaml.xml.signature.SignatureException" })
    @Test
    public void testNoKeyAlias_DefaultKeyAliasInKeystore_OneCertInKeystore_WrongCert() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_noKeyAlias_singleCertKeyStore_nonDefaultKeyAliasCertUnderDefaultKeyAlias.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String errorMsg = SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+Failed to decrypt EncryptedData.*";
        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            errorMsg = SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+Signature computation error.*";
        }

        unsuccessfulFlow(CLASS_DEFAULT_SP, errorMsg, "Did not find message saying we failed to decrypt the encrypted data.");
    }

    /**
     * Test description:
     * - keyAlias: Not specified
     * - The configured keystore contains one certificate which is mapped to the default key alias.
     * - The certificate capable of decrypting the SAML response is mapped to the default key alias.
     * Expected results:
     * - The default SAML key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - Decryption should be successful and valid assertion data should be produced.
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void testNoKeyAlias_DefaultKeyAliasInKeystore_OneCertInKeystore_CorrectCert() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_noKeyAlias_singleCertKeyStore_defaultKeyAliasCert.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, CLASS_DEFAULT_SP);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES128);
        successfulFlow(updatedTestSettings, CLASS_DEFAULT_SP);

    }

    /**
     * Test description:
     * - keyAlias: Configured
     * - The keystore configured for SAML contains no certificates.
     * Expected results:
     * - The server's messages.log should contain the CWWKS5007E message saying the specified alias could not be found in the
     * keystore.
     * - A 403 - Forbidden page should be returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "java.security.cert.CertificateException" })
    @Test
    public void testKeyAlias_EmptyKeystore() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_emptyKeyStore.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        unsuccessfulFlow(CLASS_DEFAULT_SP, SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+alias.+is not present in the KeyStore as a key entry.*", "Did not fail to find the configured key alias in the empty keystore.");
    }

    /**
     * Test description:
     * - keyAlias: Configured
     * - The configured keystore contains multiple certificates.
     * - The certificate mapped to the configured key alias cannot be used to decrypt the SAML response.
     * Expected results:
     * - The configured key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - For the solicited SP-initiated flow, an error page should be reached that indicates a signature validation error when
     * making the initial SP request.
     * - For other flows, the server's messages.log should contain the CWWKS5007E message saying the encrypted data could not be
     * decrypted properly.
     * - A 403 - Forbidden page should be returned when attempting to invoke the ACS with the SAML response from the IDP.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException" })
    @Test
    public void testKeyAlias_MultipleCertsInKeystore_KeyAliasIsWrongCert() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_multiCertKeyStore_wrongKeyAlias.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, CLASS_DEFAULT_SP);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES128);

        List<validationData> expectations = null;

        String[] flow = standardFlow;

        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            flow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP;
            expectations = vData.addSuccessStatusCodes(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST);
            expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.BAD_REQUEST_STATUS);

            expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not reach the expected error page.", null, SAML_ERROR_HAS_OCCURRED_TITLE);
            expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not reach the expected error page.", null, SAML_ERROR_HAS_OCCURRED_TITLE);
            expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the expected message saying the SAML signature couldn't be validated.", null, SAML_SIGNATURE_NOT_VALIDATED);

        } else {
            expectations = vData.addSuccessStatusCodes();
            expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML response.", null, SAMLConstants.SAML_RESPONSE);
            // Ensure we validate the SAML token content for an EncryptedAssertion
            expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN_ENCRYPTED, SAMLConstants.STRING_CONTAINS, "Did not receive the expected encrypted SAML token content.", null, null);

            String errorMsg = "Did not find message saying we failed to decrypt the encrypted data.";
            String logMsg = SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+Failed to decrypt EncryptedData.*";
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_MATCHES, errorMsg, logMsg);

        }

        performSamlFlow(CLASS_DEFAULT_SP, flow, expectations);
    }

    /**
     * Test description:
     * - keyAlias: Configured
     * - The configured keystore contains multiple certificates, including one mapped to the configured key alias.
     * - The certificate capable of decrypting the SAML response is mapped to the configured key alias.
     * Expected results:
     * - The configured key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - Decryption should be successful and valid assertion data should be produced.
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void testKeyAlias_MultipleCertsInKeystore_KeyAliasIsCorrectCert() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_multiCertKeyStore_missingDefaultKeyAliasCert.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, CLASS_DEFAULT_SP);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES128);
        successfulFlow(updatedTestSettings, CLASS_DEFAULT_SP);
    }

    /**
     * Test description:
     * - keyAlias: Configured
     * - The configured keystore contains one certificate, but not the one capable of decrypting the SAML response.
     * - The single certificate in the keystore is mapped to the configured key alias.
     * Expected results:
     * - The configured key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - The server's messages.log should contain the CWWKS5007E message.
     * - For IdP-initiated and unsolicited SP-initiated flows, the message should say the encrypted data could not be decrypted
     * properly.
     * - For the solicited SP-initiated flow, the message should say there was a signature computation error.
     * - A 403 - Forbidden page should be returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    @MaximumJavaLevel(javaLevel = 8) // test uses DSA cert and that is no longer supported
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException", "org.opensaml.xml.signature.SignatureException" })
    @Test
    public void testKeyAlias_OneCertInKeystore_KeyAliasIsWrongCert() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_singleCertKeyStore_nonDefaultKeyAliasCert_keyAliasIsWrongCert.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String errorMsg = SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+Failed to decrypt EncryptedData.*";
        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            errorMsg = SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+Signature computation error.*";
        }

        unsuccessfulFlow(CLASS_DEFAULT_SP, errorMsg, "Did not find message saying we failed to decrypt the encrypted data.");
    }

    /**
     * Test description:
     * - keyAlias: Configured
     * - The configured keystore contains one certificate - the one capable of decrypting the SAML response.
     * - The single certificate in the keystore is mapped to the configured key alias.
     * Expected results:
     * - The configured key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - Decryption should be successful and valid assertion data should be produced.
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void testKeyAlias_OneCertInKeystore_CorrectCert() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_singleCertKeyStore_nonDefaultKeyAliasCert.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, CLASS_DEFAULT_SP);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES128);
        successfulFlow(updatedTestSettings, CLASS_DEFAULT_SP);
    }

    /**
     * Test description:
     * - keyAlias: Configured
     * - The configured keystore contains one certificate - the one capable of decrypting the SAML response.
     * - The single certificate in the keystore is mapped to the default key alias, not the configured key alias.
     * Expected results:
     * - The server's messages.log should contain the CWWKS5007E message saying the specified alias could not be found in the
     * keystore.
     * - A 403 - Forbidden page should be returned when attempting to contact the SP.
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException", "java.security.cert.CertificateException" })
    @Test
    public void testKeyAlias_DefaultKeyAliasInKeystore_OneCertInKeystore_WrongKeyAlias() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_singleCertKeyStore_defaultKeyAliasCert_wrongKeyAlias.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        unsuccessfulFlow(CLASS_DEFAULT_SP, SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR + ".+alias.+is not present in the KeyStore as a key entry.*", "Did not fail to find the configured key alias in the keystore.");
    }

    /**
     * Test description:
     * - keyAlias: Configured
     * - The configured keystore contains multiple certificates, including one mapped to the default key alias.
     * - The certificate capable of decrypting the SAML response is mapped to the default key alias.
     * - The configured key alias is set to the default key alias value.
     * Expected results:
     * - The configured key alias should be used to retrieve the key for decrypting the EncryptedAssertion in the SAML response.
     * - Decryption should be successful and valid assertion data should be produced.
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void testKeyAlias_DefaultKeyAliasInKeystore_MultipleCertsInKeystore_DefaultKeyAlias() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_multiCertKeyStore_defaultKeyAlias.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, CLASS_DEFAULT_SP);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES128);
        successfulFlow(updatedTestSettings, CLASS_DEFAULT_SP);
    }

    /**
     * Test description:
     * - The standard SAML flow is followed using an SP that is configured to encrypt assertions using the AES-128 algorithm.
     * Expected results:
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void testEncryptionAlgorithm_AES128() throws Exception {

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_aes128.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, SP_ENCRYPTION_AES_128);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES128);
        successfulFlow(updatedTestSettings, SP_ENCRYPTION_AES_128);
    }

    /**
     * Test description:
     * - The standard SAML flow is followed using an SP that is configured to encrypt assertions using the AES-192 algorithm.
     * Expected results:
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Test
    public void testEncryptionAlgorithm_AES192() throws Exception {

        if (!cipherMayExceed128) {
            Log.info(thisClass, _testName, "Skipping test as AES192 is not supported");
            return;
        }

        testSAMLServer.reconfigServer(buildSPServerName("server_enc_aes192.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, SP_ENCRYPTION_AES_192);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES192);

        successfulFlow(updatedTestSettings, SP_ENCRYPTION_AES_192);

    }

    /**
     * Test description:
     * - The standard SAML flow is followed using an SP that is configured to encrypt assertions using the AES-256 algorithm.
     * Expected results:
     * - Access to the protected resource should be successful.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testEncryptionAlgorithm_AES256() throws Exception {

        if (!cipherMayExceed128) {
            Log.info(thisClass, _testName, "Skipping test as AES256 is not supported");
            return;
        }
        testSAMLServer.reconfigServer(buildSPServerName("server_enc_aes256.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, SP_ENCRYPTION_AES_256);
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getSamlTokenValidationData().getNameId(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), SAMLConstants.BAD_TOKEN_EXCHANGE, updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        successfulFlow(updatedTestSettings, SP_ENCRYPTION_AES_256);

    }

    private void successfulFlow(SAMLTestSettings settings, String sp) throws Exception {
        SAMLTestSettings updatedTestSettings = getTestSettings(settings, sp);

        List<validationData> expectations = vData.addSuccessStatusCodes();

        String firstAction = standardFlow[0];
        if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
            expectations = vData.addExpectation(expectations, firstAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP client JSP page.", null, SAMLConstants.IDP_CLIENT_JSP_TITLE);
        } else {
            expectations = vData.addExpectation(expectations, firstAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login page.", null, cttools.getLoginTitle(updatedTestSettings.getIdpRoot()));
        }
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML response.", null, SAMLConstants.SAML_RESPONSE);

        // Ensure we validate the SAML token content for an EncryptedAssertion
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN_ENCRYPTED, SAMLConstants.STRING_CONTAINS, "Did not receive the expected encrypted SAML token content.", null, null);

        // Should successfully reach snoop servlet
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_MATCHES, "Did not get expected OK message.", null, SAMLConstants.OK_MESSAGE);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not see the expected snoop servlet title.", null, SAMLConstants.APP1_TITLE);

        performSamlFlow(sp, standardFlow, updatedTestSettings, expectations);
    }

    /**
     * Runs through an unsuccessful standard SAML flow. This verifies that a SAML response is obtained from the IDP
     * that contains an EncryptedAssertion element in the IdP-initiated and unsolicited SP flows. For the solicited SP
     * flow, this verifies that the initial SP invocation is unsuccessful. This also verifies that we reach an HTTP
     * Forbidden error page when attempting to access the protected resource and that the server's log contains the
     * given logMessage. If logMessage is not found, the test fails with the specified failureMessage.
     *
     * @param sp
     *            The service provider to use.
     * @param logMessage
     *            Regular expression to be search for in the server's messages.log.
     * @param failureMessage
     *            Message that will be reported through JUnit if logMessage is not found.
     *
     * @throws Exception
     */
    private void unsuccessfulFlow(String sp, String logMessage, String failureMessage) throws Exception {
        unsuccessfulFlow(testSettings, sp, logMessage, failureMessage);
    }

    private void unsuccessfulFlow(SAMLTestSettings settings, String sp, String logMessage, String failureMessage) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();

        String[] flow = standardFlow;

        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {
            expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML response.", null, SAMLConstants.SAML_RESPONSE);
            // Ensure we validate the SAML token content for an EncryptedAssertion
            expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN_ENCRYPTED, SAMLConstants.STRING_CONTAINS, "Did not receive the expected encrypted SAML token content.", null, null);
        }

        String errorPageStep = SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE;

        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            flow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP;
            errorPageStep = SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST;
        }

        // Should reach an error page with the expected message appearing in the logs
        expectations = msgUtils.addForbiddenExpectation(errorPageStep, expectations);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, errorPageStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_MATCHES, failureMessage, logMessage);

        performSamlFlow(settings, sp, flow, expectations);
    }

    private SAMLTestSettings getTestSettings(SAMLTestSettings settings, String sp) throws Exception {

        String encryptAlg = settings.getSamlTokenValidationData().getEncryptAlg();
        SAMLTestSettings updatedTestSettings = settings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings(sp, true);
        updatedTestSettings.setSpecificIDPChallenge(1);
        updatedTestSettings.getSamlTokenValidationData().setEncryptionKeyUser(DEFAULT_ENCRYPTION_KEY_USER);
        updatedTestSettings.getSamlTokenValidationData().setEncryptAlg(encryptAlg);
        return updatedTestSettings;
    }

    private void performSamlFlow(String sp, String[] flow, List<validationData> expectations) throws Exception {
        SAMLTestSettings updatedTestSettings = getTestSettings(testSettings, sp);

        performSamlFlow(sp, flow, updatedTestSettings, expectations);
    }

    private void performSamlFlow(SAMLTestSettings settings, String sp, String[] flow, List<validationData> expectations) throws Exception {
        SAMLTestSettings updatedTestSettings = getTestSettings(settings, sp);

        performSamlFlow(sp, flow, updatedTestSettings, expectations);
    }

    private void performSamlFlow(String sp, String[] flow, SAMLTestSettings settings, List<validationData> expectations) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        genericSAML(_testName, webClient, settings, flow, expectations);
    }
}
