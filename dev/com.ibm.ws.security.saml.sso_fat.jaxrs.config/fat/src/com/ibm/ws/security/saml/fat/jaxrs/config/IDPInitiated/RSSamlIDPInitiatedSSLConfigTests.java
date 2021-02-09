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
package com.ibm.ws.security.saml.fat.jaxrs.config.IDPInitiated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlConfigSettings;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlProviderSettings;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link to an
 * application installed on a WebSphere SP. When the Web client invokes the SP
 * application, it is redirected to a TFIM IdP which issues a login challenge to
 * the Web client. The Web Client fills in the login form and after a successful
 * login, receives a SAML 2.0 token from the TFIM IdP. The client invokes the SP
 * application by sending the SAML 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RSSamlIDPInitiatedSSLConfigTests extends RSSamlIDPInitiatedConfigCommonTests {

    /*****************************************
     * TESTS
     **************************************/

    /**************************************
     * signatureMethodAlgorithm
     **************************************/

    /**
     * Test purpose: - signatureMethodAlgorithm: SHA1 Expected results: - The
     * SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_signatureMethodAlgorithm_SHA1() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setSignatureMethodAlgorithm("SHA1");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - signatureMethodAlgorithm: SHA128 Expected results: - The
     * SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_signatureMethodAlgorithm_SHA128() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setSignatureMethodAlgorithm("SHA128");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - signatureMethodAlgorithm: SHA256 Expected results: - 401
     * when invoking JAX-RS. - CWWKS5049E message in the app server log saying
     * the signature was not valid (weaker than required).
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.ws.security.SecurityPolicyException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_signatureMethodAlgorithm_SHA256() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setSignatureMethodAlgorithm("SHA256");

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not get the expected message saying the received signature was not valid and weaker than required.",
                SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - signatureMethodAlgorithm: SHA128 - SAML SP specifies
     * SHA256 as the signature algorithm Expected results: - TODO
     *
     * @throws Exception
     */
    // !@Test
    public void RSSamlIDPInitiatedConfigTests_signatureMethodAlgorithm_SHA128_sp256() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setSignatureMethodAlgorithm("SHA128");

        // Update test settings to use an SP that encrypts SAML assertions
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", SP_SIG_ALG_SHA256, true);
        updatedTestSettings.setSpecificIDPChallenge(2);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }

    /**************************************
     * wantAssertionsSigned
     **************************************/

    // There should be sufficient coverage for this attribute in other tests.

    /**************************************
     * keyStoreRef
     **************************************/

    /**
     * Test purpose: - keyStoreRef: Not specified in the config Expected
     * results: - Partner that does not encrypt assertions: - Keystore doesn't
     * come into play; SAML token should be successfully processed by JAX-RS. -
     * Partner that encrypts assertions: - 401 when invoking JAX-RS - CWWKS5073E
     * message in RS log saying service provider couldn't find the private key
     * in the keystore - CWWKS5007E message in RS log saying an internal error
     * occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyStoreRef_notSpecified() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyStoreRef_notSpecified";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not get message saying private key could not be found.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyStoreRef: "" Expected results: - Partner that does not
     * encrypt assertions: - Keystore doesn't come into play; SAML token should
     * be successfully processed by JAX-RS. - Partner that encrypts assertions:
     * - 401 when invoking JAX-RS - CWWKS5073E message in RS log saying service
     * provider couldn't find the private key in the keystore - CWWKS5007E
     * message in RS log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyStoreRef_empty() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyStoreRef_empty";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not get message saying private key could not be found.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyStoreRef: Points to non-existent keystore Expected
     * results: - Partner that does not encrypt assertions: - Keystore doesn't
     * come into play; SAML token should be successfully processed by JAX-RS. -
     * Partner that encrypts assertions: - 401 when invoking JAX-RS - CWWKS5007E
     * message in RS log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.security.KeyStoreException", "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyStoreRef_nonExistentKeystore() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyStoreRef_nonExistent";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyStoreRef: Points to keystore containing a single key,
     * one that cannot properly be used. Expected results: - Partner that does
     * not encrypt assertions: - Keystore doesn't come into play; SAML token
     * should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWWKS5007E message in RS log
     * saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyStoreRef_singleKey_invalid() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyStoreRef_singleKey_invalid";

        // TODO also get open source errors for decrypting the key
        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyStoreRef: Points to keystore containing a single key
     * that can properly be used. Expected results: - Partner that does not
     * encrypt assertions: - Keystore doesn't come into play; SAML token should
     * be successfully processed by JAX-RS. - Partner that encrypts assertions:
     * - The SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyStoreRef_singleKey_valid() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyStoreRef_singleKey_valid";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyStoreRef: Points to keystore containing multiple keys,
     * including a usable key under the "samlsp" alias Expected results: -
     * Partner that does not encrypt assertions: - Keystore doesn't come into
     * play; SAML token should be successfully processed by JAX-RS. - Partner
     * that encrypts assertions: - The SAML token should be successfully
     * processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyStoreRef_multiKey_samlspIncluded() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyStoreRef_multiKey_samlspIncluded";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyStoreRef: Points to keystore containing multiple keys
     * but does not include the default key alias Expected results: - Partner
     * that does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWWKS5073E message in RS log
     * saying service provider couldn't find the private key in the keystore -
     * CWWKS5007E message in RS log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyStoreRef_multiKey_samlspMissing() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyStoreRef_multiKey_samlspMissing";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not get message saying private key could not be found.");
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**************************************
     * keyAlias
     **************************************/

    /**
     * Test purpose: - keyAlias: Not specified in config - Configured keystore
     * is empty Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - 401 when
     * invoking JAX-RS - CWWKS5073E message in RS log saying service provider
     * couldn't find the private key in the keystore - CWWKS5007E message in RS
     * log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_notSpecified_emptyKeystore() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_notSpecified_emptyKeystore";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not get message saying private key could not be found.");
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyAlias: Not specified in config - Configured keystore
     * contains one key - Key is invalid Expected results: - Partner that does
     * not encrypt assertions: - Keystore doesn't come into play; SAML token
     * should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWWKS5007E message in RS log
     * saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_notSpecified_oneKeyInKeystore_invalid() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_notSpecified_oneKeyInKeystore_invalid";

        // TODO also get open source errors for decrypting the key
        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyAlias: Not specified in config - Configured keystore
     * contains one key - Key is valid and mapped to a non-default key alias
     * value Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - The SAML token
     * should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_notSpecified_oneKeyInKeystore_valid_certMappedToNonDefaultAlias() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_notSpecified_oneKeyInKeystore_valid_certMappedToNonDefaultAlias";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyAlias: Not specified in config - Configured keystore
     * contains multiple keys - Valid key is included but mapped to non-default
     * key alias Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - 401 when
     * invoking JAX-RS - CWWKS5007E message in RS log saying an internal error
     * occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_notSpecified_multipleKeysInKeystore_includesValid_validKeyMappedToNonDefaultAlias() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_notSpecified_multipleKeysInKeystore_includesValid_validKeyMappedToNonDefaultAlias";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyAlias: Not specified in config - Configured keystore
     * contains multiple keys - Valid key is included and mapped to default key
     * alias Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - The SAML token
     * should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_notSpecified_multipleKeysInKeystore_includesValid_validKeyMappedToDefaultAlias() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_notSpecified_multipleKeysInKeystore_includesValid_validKeyMappedToDefaultAlias";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyAlias: Empty string - Configured keystore is empty
     * Expected results: - Partner that does not encrypt assertions: - Keystore
     * doesn't come into play; SAML token should be successfully processed by
     * JAX-RS. - Partner that encrypts assertions: - 401 when invoking JAX-RS -
     * CWWKS5073E message in RS log saying service provider couldn't find the
     * private key in the keystore - CWWKS5007E message in RS log saying an
     * internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_empty_emptyKeystore() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_empty_emptyKeystore";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not get message saying private key could not be found.");
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyAlias: Empty string - Configured keystore is empty
     * Expected results: - Partner that does not encrypt assertions: - Keystore
     * doesn't come into play; SAML token should be successfully processed by
     * JAX-RS. - Partner that encrypts assertions: - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_empty_oneKeyInKeystore_valid_certMappedToNonDefaultAlias() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_empty_oneKeyInKeystore_valid_certMappedToNonDefaultAlias";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyAlias: Empty string - Configured keystore contains
     * multiple keys - Valid key is included and mapped to default key alias
     * Expected results: - Partner that does not encrypt assertions: - Keystore
     * doesn't come into play; SAML token should be successfully processed by
     * JAX-RS. - Partner that encrypts assertions: - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_empty_multipleKeysInKeystore_includesValid_validKeyMappedToDefaultAlias() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_empty_multipleKeysInKeystore_includesValid_validKeyMappedToDefaultAlias";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyAlias: Set to alias that doesn't exist in the keystore
     * - Configured keystore contains only a single key. Expected results: -
     * Partner that does not encrypt assertions: - Keystore doesn't come into
     * play; SAML token should be successfully processed by JAX-RS. - Partner
     * that encrypts assertions: - 401 when invoking JAX-RS - CWWKS5007E message
     * in RS log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.cert.CertificateException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_nonExistentKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_nonExistentKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyAlias: Valid alias, points to alias with correct key
     * in keystore - Configured keystore contains one keys that matches keyAlias
     * and is the correct key Expected results: - Partner that does not encrypt
     * assertions: - Keystore doesn't come into play; SAML token should be
     * successfully processed by JAX-RS. - Partner that encrypts assertions: -
     * The SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_valid_oneKeyInKeystore() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_valid_oneKeyInKeystore";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyAlias: Valid alias, points to alias with correct key
     * in keystore - Configured keystore contains multiple keys, including one
     * that matches keyAlias and is the correct key Expected results: - Partner
     * that does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - The SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_valid_multipleKeysInKeystore() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_valid_multipleKeysInKeystore";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyAlias: Non-existent alias - Configured keystore
     * contains multiple keys, including one that is the correct key and mapped
     * to the default alias - Verifies that we don't fall back to using the
     * default key alias if we put the wrong value for the keyAlias config
     * attribute Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - 401 when
     * invoking JAX-RS - CWWKS5007E message in RS log saying an internal error
     * occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.cert.CertificateException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_keyAlias_invalid_multipleKeysInKeystore() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyAlias_invalid_multipleKeysInKeystore";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**************************************
     * keyPassword
     **************************************/

    /**
     * Test purpose: - keyPassword (RS): Not specified - keyPassword (keystore):
     * "" - keyAlias: Not specified - Configured keystore contains one key -
     * Configured keystore contains valid key Expected results: - Partner that
     * does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWPKI0812E message in RS log
     * saying there was an error getting the key - CWWKS5007E message in RS log
     * saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_notSpecified_keystoreKeyPassword_empty_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_notSpecified_keystoreKeyPassword_empty_singleKeyInKeystore_noKeyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Not specified - keyPassword (keystore):
     * "" - keyAlias: Not specified - Configured keystore contains multiple keys
     * - Configured keystore contains valid key Expected results: - Partner that
     * does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWPKI0812E message in RS log
     * saying there was an error getting the key - CWWKS5073E message in RS log
     * saying service provider couldn't find the private key in the keystore
     * (since there was an error using the default key) - CWWKS5007E message in
     * RS log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_notSpecified_keystoreKeyPassword_empty_multipleKeysInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_notSpecified_keystoreKeyPassword_empty_multipleKeysInKeystore_noKeyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not get message saying private key could not be found.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Not specified - keyPassword (keystore):
     * Non-empty value - keyAlias: Not specified - Configured keystore contains
     * one key - Configured keystore contains valid key Expected results: -
     * Partner that does not encrypt assertions: - Keystore doesn't come into
     * play; SAML token should be successfully processed by JAX-RS. - Partner
     * that encrypts assertions: - 401 when invoking JAX-RS - CWPKI0812E message
     * in RS log saying there was an error getting the key - CWWKS5007E message
     * in RS log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_notSpecified_keystoreKeyPassword_nonEmpty_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_notSpecified_keystoreKeyPassword_nonEmpty_singleKeyInKeystore_noKeyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Not specified - keyPassword (keystore):
     * Matches the keystore password - keyAlias: Not specified - Configured
     * keystore contains one key - Configured keystore contains valid key
     * Expected results: - Partner that does not encrypt assertions: - Keystore
     * doesn't come into play; SAML token should be successfully processed by
     * JAX-RS. - Partner that encrypts assertions: - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_notSpecified_keystoreKeyPassword_matchesKeystore_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_notSpecified_keystoreKeyPassword_matchesKeystore_singleKeyInKeystore_noKeyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Not specified - keyPassword (keystore):
     * Matches the keystore password - keyAlias: References invalid key -
     * Configured keystore contains multiple keys - Configured keystore contains
     * valid key Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - 401 when
     * invoking JAX-RS - CWWKS5007E message in RS log saying an internal error
     * occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_notSpecified_keystoreKeyPassword_matchesKeystore_multipleKeysInKeystore_keyAlias_invalidKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_notSpecified_keystoreKeyPassword_matchesKeystore_multipleKeysInKeystore_keyAlias_invalidKey";

        // TODO also get open source errors for decrypting the key
        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Not specified - keyPassword (keystore):
     * Matches the keystore password - keyAlias: References valid key -
     * Configured keystore contains multiple keys - Configured keystore contains
     * valid key Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - The SAML token
     * should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_notSpecified_keystoreKeyPassword_matchesKeystore_multipleKeysInKeystore_keyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_notSpecified_keystoreKeyPassword_matchesKeystore_multipleKeysInKeystore_keyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): "" - keyPassword (keystore): Not
     * specified - keyAlias: Not specified - Configured keystore contains one
     * key - Configured keystore contains valid key Expected results: - Partner
     * that does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - Runtime should behave as if the keyPassword attribute
     * wasn't specified - The SAML token should be successfully processed by
     * JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_empty_keystoreKeyPassword_notSpecified_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_empty_keystoreKeyPassword_notSpecified_singleKeyInKeystore_noKeyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): "" - keyPassword (keystore): "" -
     * keyAlias: Not specified - Configured keystore contains one key -
     * Configured keystore contains valid key Expected results: - Partner that
     * does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWPKI0812E message in RS log
     * saying there was an error getting the key - CWWKS5007E message in RS log
     * saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_empty_keystoreKeyPassword_empty_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_empty_keystoreKeyPassword_empty_singleKeyInKeystore_noKeyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): "" - keyPassword (keystore): "" -
     * keyAlias: References valid key - Configured keystore contains multiple
     * keys - Configured keystore contains valid key Expected results: - Partner
     * that does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWPKI0812E message in RS log
     * saying there was an error getting the key - CWWKS5007E message in RS log
     * saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_empty_keystoreKeyPassword_empty_multipleKeysInKeystore_keyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_empty_keystoreKeyPassword_empty_multipleKeysInKeystore_keyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): "" - keyPassword (keystore): Non-empty
     * value - keyAlias: References valid key - Configured keystore contains
     * multiple keys - Configured keystore contains valid key Expected results:
     * - Partner that does not encrypt assertions: - Keystore doesn't come into
     * play; SAML token should be successfully processed by JAX-RS. - Partner
     * that encrypts assertions: - 401 when invoking JAX-RS - CWPKI0812E message
     * in RS log saying there was an error getting the key - CWWKS5007E message
     * in RS log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_empty_keystoreKeyPassword_nonEmpty_multipleKeysInKeystore_keyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_empty_keystoreKeyPassword_nonEmpty_multipleKeysInKeystore_keyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): "" - keyPassword (keystore): Matches
     * the keystore password - keyAlias: Not specified - Configured keystore
     * contains one key - Configured keystore contains valid key Expected
     * results: - Partner that does not encrypt assertions: - Keystore doesn't
     * come into play; SAML token should be successfully processed by JAX-RS. -
     * Partner that encrypts assertions: - Runtime should behave as if the
     * keyPassword attribute wasn't specified - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_empty_keystoreKeyPassword_matchesKeystore_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_empty_keystoreKeyPassword_matchesKeystore_singleKeyInKeystore_noKeyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): "" - keyPassword (keystore): Matches
     * the keystore password - keyAlias: References valid key - Configured
     * keystore contains multiple keys - Configured keystore contains valid key
     * Expected results: - Partner that does not encrypt assertions: - Keystore
     * doesn't come into play; SAML token should be successfully processed by
     * JAX-RS. - Partner that encrypts assertions: - Runtime should behave as if
     * the keyPassword attribute wasn't specified - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_empty_keystoreKeyPassword_matchesKeystore_multipleKeysInKeystore_keyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_empty_keystoreKeyPassword_matchesKeystore_multipleKeysInKeystore_keyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Non-empty value - keyPassword
     * (keystore): Not specified - keyAlias: Not specified - Configured keystore
     * contains one key - Configured keystore contains valid key Expected
     * results: - Partner that does not encrypt assertions: - Keystore doesn't
     * come into play; SAML token should be successfully processed by JAX-RS. -
     * Partner that encrypts assertions: - CWPKI0812E message in RS log saying
     * there was an error getting the key - Runtime will fall back to trying the
     * only key in the keystore. In this case, that key is valid. - The SAML
     * token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_nonEmpty_keystoreKeyPassword_notSpecified_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_nonEmpty_keystoreKeyPassword_notSpecified_singleKeyInKeystore_noKeyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        successfulKeyStoreTest(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Non-empty value - keyPassword
     * (keystore): "" - keyAlias: Not specified - Configured keystore contains
     * one key - Configured keystore contains valid key Expected results: -
     * Partner that does not encrypt assertions: - Keystore doesn't come into
     * play; SAML token should be successfully processed by JAX-RS. - Partner
     * that encrypts assertions: - 401 when invoking JAX-RS - CWPKI0812E message
     * in RS log saying there was an error getting the key - CWWKS5007E message
     * in RS log saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_nonEmpty_keystoreKeyPassword_empty_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_nonEmpty_keystoreKeyPassword_empty_singleKeyInKeystore_noKeyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Non-empty value - keyPassword
     * (keystore): Non-empty value (matches RS keyPassword value) - keyAlias:
     * Not specified - Configured keystore contains one key - Configured
     * keystore contains valid key Expected results: - Partner that does not
     * encrypt assertions: - Keystore doesn't come into play; SAML token should
     * be successfully processed by JAX-RS. - Partner that encrypts assertions:
     * -
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_singleKeyInKeystore_noKeyAlias_validKey";

        // TODO - I would expect this to pass but it does not. Instead I get a
        // 401 + CWPKI0812E + CWWKS5007E
        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Non-empty value - keyPassword
     * (keystore): Non-empty value (matches RS keyPassword value) - keyAlias:
     * Not specified - Configured keystore contains multiple keys - Configured
     * keystore contains valid key Expected results: - Partner that does not
     * encrypt assertions: - Keystore doesn't come into play; SAML token should
     * be successfully processed by JAX-RS. - Partner that encrypts assertions:
     * -
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_multipleKeysInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_multipleKeysInKeystore_noKeyAlias_validKey";

        // TODO - I would expect this to pass but it does not. Instead I get a
        // 401 + CWPKI0812E + CWWKS5073E + CWWKS5007E
        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not get message saying private key could not be found.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Non-empty value - keyPassword
     * (keystore): Non-empty value (matches RS keyPassword value) - keyAlias:
     * References non-existent key - Configured keystore contains multiple keys
     * - Configured keystore contains valid key Expected results: - Partner that
     * does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWWKS5007E message in RS log
     * saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.cert.CertificateException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_multipleKeysInKeystore_keyAlias_nonExistentKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_multipleKeysInKeystore_keyAlias_nonExistentKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Non-empty value - keyPassword
     * (keystore): Non-empty value (matches RS keyPassword value) - keyAlias:
     * References invalid key - Configured keystore contains multiple keys -
     * Configured keystore contains valid key Expected results: - Partner that
     * does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: - 401 when invoking JAX-RS - CWPKI0812E message in RS log
     * saying there was an error getting the key - CWWKS5007E message in RS log
     * saying an internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_multipleKeysInKeystore_keyAlias_invalidKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_multipleKeysInKeystore_keyAlias_invalidKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Non-empty value - keyPassword
     * (keystore): Non-empty value (matches RS keyPassword value) - keyAlias:
     * References valid key - Configured keystore contains multiple keys -
     * Configured keystore contains valid key Expected results: - Partner that
     * does not encrypt assertions: - Keystore doesn't come into play; SAML
     * token should be successfully processed by JAX-RS. - Partner that encrypts
     * assertions: -
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "java.security.KeyStoreException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_multipleKeysInKeystore_keyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_nonEmpty_keystoreKeyPassword_nonEmptyMatches_multipleKeysInKeystore_keyAlias_validKey";

        // TODO - I would expect this to pass but it does not. Instead I get a
        // 401 + CWPKI0812E + CWWKS5007E
        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Non-empty value - keyPassword
     * (keystore): Matches the keystore password - keyAlias: Not specified -
     * Configured keystore contains multiple keys - Configured keystore contains
     * valid key Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - 401 when
     * invoking JAX-RS - CWPKI0812E message in RS log saying there was an error
     * getting the key - CWWKS5007E message in RS log saying an internal error
     * occurred - CWWKS5073E message in RS log saying service provider couldn't
     * find the private key in the keystore
     *
     * @throws Exception
     */
    @ExpectedFFDC("com.ibm.ws.security.saml.error.SamlException")
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_nonEmpty_keystoreKeyPassword_matchesKeystore_multipleKeysInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_nonEmpty_keystoreKeyPassword_matchesKeystore_multipleKeysInKeystore_noKeyAlias_validKey";

        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");
        errorMsgs.put(SAMLMessageConstants.CWPKI0812E_ERROR_GETTING_KEY, "Did not get message saying there was an error getting the key.");
        errorMsgs.put(SAMLMessageConstants.CWWKS5073E_CANNOT_FIND_PRIVATE_KEY, "Did not get message saying private key could not be found.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): Not specified - keyAlias: Not specified -
     * Configured keystore contains one key - Configured keystore contains valid
     * key Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - The SAML token
     * should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_notSpecified_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_notSpecified_singleKeyInKeystore_noKeyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): Not specified - keyAlias: Not specified -
     * Configured keystore contains multiple keys - Configured keystore contains
     * valid key Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - The SAML token
     * should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_notSpecified_multipleKeysInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_notSpecified_multipleKeysInKeystore_noKeyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): Not specified - keyAlias: References invalid key
     * - Configured keystore contains multiple keys - Configured keystore
     * contains valid key Expected results: - Partner that does not encrypt
     * assertions: - Keystore doesn't come into play; SAML token should be
     * successfully processed by JAX-RS. - Partner that encrypts assertions: -
     * 401 when invoking JAX-RS - CWWKS5007E message in RS log saying an
     * internal error occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_notSpecified_multipleKeysInKeystore_keyAlias_invalidKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_notSpecified_multipleKeysInKeystore_keyAlias_invalidKey";

        // TODO also get open source errors for decrypting the key
        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): Not specified - keyAlias: References valid key -
     * Configured keystore contains multiple keys - Configured keystore contains
     * valid key Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - The SAML token
     * should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_notSpecified_multipleKeysInKeystore_keyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_notSpecified_multipleKeysInKeystore_keyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): "" - keyAlias: Not specified - Configured
     * keystore contains one key - Configured keystore contains valid key
     * Expected results: - Partner that does not encrypt assertions: - Keystore
     * doesn't come into play; SAML token should be successfully processed by
     * JAX-RS. - Partner that encrypts assertions: - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_empty_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_empty_singleKeyInKeystore_noKeyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): "" - keyAlias: Not specified - Configured
     * keystore contains multiple keys - Configured keystore contains valid key
     * Expected results: - Partner that does not encrypt assertions: - Keystore
     * doesn't come into play; SAML token should be successfully processed by
     * JAX-RS. - Partner that encrypts assertions: - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_empty_multipleKeysInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_empty_multipleKeysInKeystore_noKeyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): "" - keyAlias: References invalid key -
     * Configured keystore contains multiple keys - Configured keystore contains
     * valid key Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - 401 when
     * invoking JAX-RS - CWWKS5007E message in RS log saying an internal error
     * occurred
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.xml.encryption.DecryptionException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_empty_multipleKeysInKeystore_keyAlias_invalidKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_empty_multipleKeysInKeystore_keyAlias_invalidKey";

        // TODO also get open source errors for decrypting the key
        Map<String, String> errorMsgs = new HashMap<String, String>();
        errorMsgs.put(SAMLMessageConstants.CWWKS5007E_INTERNAL_SERVER_ERROR, "Did not get internal error message.");

        runKeyStoreTestExpecting401(appExtension, errorMsgs);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): "" - keyAlias: References valid key - Configured
     * keystore contains multiple keys - Configured keystore contains valid key
     * Expected results: - Partner that does not encrypt assertions: - Keystore
     * doesn't come into play; SAML token should be successfully processed by
     * JAX-RS. - Partner that encrypts assertions: - The SAML token should be
     * successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_empty_multipleKeysInKeystore_keyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_empty_multipleKeysInKeystore_keyAlias_validKey";

        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): Non-empty value - keyAlias: Not specified -
     * Configured keystore contains one key - Configured keystore contains valid
     * key Expected results: - Partner that does not encrypt assertions: -
     * Keystore doesn't come into play; SAML token should be successfully
     * processed by JAX-RS. - Partner that encrypts assertions: - The SAML token
     * should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_nonEmpty_singleKeyInKeystore_noKeyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_nonEmpty_singleKeyInKeystore_noKeyAlias_validKey";

        // TODO - I would expect this to fail
        successfulKeyStoreTest(appExtension);
    }

    /**
     * Test purpose: - keyPassword (RS): Matches the keystore password -
     * keyPassword (keystore): Non-empty value - keyAlias: References valid key
     * - Configured keystore contains multiple keys - Configured keystore
     * contains valid key Expected results: - Partner that does not encrypt
     * assertions: - Keystore doesn't come into play; SAML token should be
     * successfully processed by JAX-RS. - Partner that encrypts assertions: -
     * The SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_rsKeyPassword_matchesKeystore_keystoreKeyPassword_nonEmpty_multipleKeysInKeystore_keyAlias_validKey() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(APP_SERVER_ORIG_CONFIG), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        String appExtension = "keyPassword_matchesKeystore_keystoreKeyPassword_nonEmpty_multipleKeysInKeystore_keyAlias_validKey";

        // TODO - I would expect this to fail
        successfulKeyStoreTest(appExtension);
    }

    /**************************************
     * pkixTrustEngine
     **************************************/

    // Covered in com.ibm.ws.security.saml.fat.jaxrs.common.RSSamlPkixTests

}
