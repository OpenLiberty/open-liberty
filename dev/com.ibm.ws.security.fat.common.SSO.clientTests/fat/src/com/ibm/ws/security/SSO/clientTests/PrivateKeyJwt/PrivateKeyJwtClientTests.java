/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.security.SSO.clientTests.PrivateKeyJwt;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.SSO.clientTests.commonTools.PKCEPrivateKeyJwtCommonTooling;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run Private Key Jwt client tests - These are tests that use either an openidConnectClient or
 * Social oidcLogin and an OP to provide authorization functionality. The tests stub out the token endpoint which will just log
 * the request used to invoke it. The tests will then search for the different request parameters recorded in the log. They will
 * validate that the proper parms were passed and that the content of those parms is what was expected (based on the config used
 * by that test). This includes parsing the private key jwt and making sure that all of the required claims are included and
 * are correct.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class PrivateKeyJwtClientTests extends PKCEPrivateKeyJwtCommonTooling {

    protected static boolean usingSocialClient = false;

    private static boolean usingJava8() {
        boolean usingJava8 = false;
        if (System.getProperty("java.specification.version").matches("1\\.[789]")) {
            usingJava8 = true;
        }
        Log.info(thisClass, "usingJava8", "Skip for Java 8: " + Boolean.toString(usingJava8));
        return usingJava8;

    }

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    public static class SkipIfSocialClient extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            Log.info(thisClass, "callSpecificCheck", Boolean.toString(usingSocialClient));
            return usingSocialClient;

        }
    }

    public static class SkipIfJava8 extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            boolean skipIt = usingJava8();
            Log.info(thisClass, "callSpecificCheck", "Skip for Java 8: " + Boolean.toString(skipIt));
            return skipIt;

        }
    }

    public static class SkipIfJava8OrSocial extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {

            boolean skipIt = usingJava8() || usingSocialClient;
            Log.info(thisClass, "callSpecificCheck", "Skip for Java 8 or Social Client: " + Boolean.toString(skipIt));
            return skipIt;

        }
    }

    public static Class<?> thisClass = PrivateKeyJwtClientTests.class;

    public static void allowPrivateKeyJwtErrorMessages() throws Exception {
        clientServer.addIgnoredServerExceptions(MessageConstants.SRVE8094W_CANNOT_SET_HEADER_RESPONSE_COMMITTED, MessageConstants.SRVE8115W_CANNOT_SET_HEADER_RESPONSE_COMMITTED, MessageConstants.CWPKI0033E_KEYSTORE_DOES_NOT_EXIST, MessageConstants.CWPKI0809W_FAILURE_LOADING_KEYSTORE);
    }

    /**
     * Process a positive test case flow when a private key should be included
     *
     * @param app
     *            - the app to invoke
     * @param challenge
     *            - the type of challenge (S256 or plain)
     * @throws Exception
     */
    public void positiveTestWithPrivateKey(TestSettings updatedTestSettings) throws Exception {
        positiveTest(updatedTestSettings, AuthMethod.PRIVATE_KEY_JWT, null);
    }

    /**
     * Process a positive test case flow - expected behavior based on the authMethod
     *
     * @param updatedTestSettings
     *            - the test setting values to use for this test instance
     * @param authMethod
     *            - the auth method used by the client
     * @throws Exception
     */
    public void positiveTest(TestSettings updatedTestSettings, AuthMethod authMethod, String originHeader) throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations();

        genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

        validateTokenRequest(updatedTestSettings, authMethod, originHeader);
    }

    /**
     * Process a negative test case flow - calling test passes in what to expect/check
     *
     * @param updatedTestSettings
     *            - the test setting values to use for this test instance
     * @param expectations
     *            - the expectations to validate (set by the calling test)
     * @throws Exception
     */
    public void negativeTestWithPrivateKey(TestSettings updatedTestSettings, List<validationData> expectations)
            throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

    /******************************* Tests *********************************/
    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="rs256"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesRS256() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_RS256_RS256");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS384
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS384"
     * keyAliasName="rs384"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesRS384() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS384, "client_RS256_RS384");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS512
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS512"
     * keyAliasName="rs512"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesRS512() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS512, "client_RS256_RS512");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with ES256
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="ES256"
     * keyAliasName="es256"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesES256() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_ES256, "client_RS256_ES256");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with ES384
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="ES384"
     * keyAliasName="es384"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesES384() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_ES384, "client_RS256_ES384");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with ES512
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="ES512"
     * keyAliasName="es512"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesES512() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_ES512, "client_RS256_ES512");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of HS256, and a private key jwt configured with RS256
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="rs256"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesHS256_privateKeyJwtUsesRS256() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_HS256", Constants.SIGALG_RS256, "client_HS256_RS256");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of HS256, and a private key jwt configured with RS384
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS384"
     * keyAliasName="rs384"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesHS256_privateKeyJwtUsesRS384() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_HS256", Constants.SIGALG_RS384, "client_HS256_RS384");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of HS256, and a private key jwt configured with RS512
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS512"
     * keyAliasName="rs512"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesHS256_privateKeyJwtUsesRS512() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_HS256", Constants.SIGALG_RS512, "client_HS256_RS512");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of HS256, and a private key jwt configured with ES256
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="ES256"
     * keyAliasName="es256"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesHS256_privateKeyJwtUsesES256() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_HS256", Constants.SIGALG_ES256, "client_HS256_ES256");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of HS256, and a private key jwt configured with ES384
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="ES384"
     * keyAliasName="es384"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesHS256_privateKeyJwtUsesES384() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_HS256", Constants.SIGALG_ES384, "client_HS256_ES384");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of HS256, and a private key jwt configured with ES512
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="ES512"
     * keyAliasName="es512"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesHS256_privateKeyJwtUsesES512() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_HS256", Constants.SIGALG_ES512, "client_HS256_ES512");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256, but
     * we'll specify a keyAliasName that doesn't exist
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="badKeyAlias"
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException", "java.security.cert.CertificateException" })
    @Test
    public void PrivateKeyJwtClientTests_badKeyAliasName() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_RS256_badKeyAlias");

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the token endpoint did not return an id_token.", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER + ".*" + MessageConstants.CWWKS2432E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR + ".*" + MessageConstants.CWWKS2430E_FAILED_TO_BUILD_TOKEN_FOR_CLIENT_AUTH + ".*" + MessageConstants.CWWKS2435E_PRIVATE_KEY_JWT_ERROR_GETTING_PRIVATE_KEY);

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256, but
     * we'll omit the keyAliasName
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     */
    @AllowedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException" })
    @Mode(TestMode.LITE)
    @Test
    public void PrivateKeyJwtClientTests_ommittedKeyAliasName() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_RS256_omittedKeyAlias");

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the token endpoint did not return an id_token.", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER + ".*" + MessageConstants.CWWKS2432E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR + ".+" + MessageConstants.CWWKS2433E_PRIVATE_KEY_JWT_MISSING_KEY_ALIAS_NAME);

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured omitting
     * tokenEndpointAuthSigningAlgorithm - we'll use a keyAliasName compatible with RS256 which tokenEndpointAuthSigningAlgorithm
     * will default to.
     * tokenEndpointAuthMethod="private_key_jwt"
     * keyAliasName="rs256"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_omittedAuthSingingAlg() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_RS256_omittedAuthSingingAlg");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256.
     * We'll omit tokenEndpointAuthMethod, but that will default to client_secret_post. Make sure that the assertion parms are
     * omitted and that client_id and client_secret are included.
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="rs256"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_omitAuthMethodButIncludeOtherPrivateKeyJwtParms() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_RS256_authMethodPost");

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_DOES_NOT_CONTAIN, "Message log contained " + getParmString(Constants.CLIENT_ASSERTION_TYPE) + " and should not have.", getParmString(Constants.CLIENT_ASSERTION_TYPE));
        addToAllowableTimeoutCount(1);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_DOES_NOT_CONTAIN, "Message log contained " + getParmString(Constants.CLIENT_ASSERTION) + " and should not have.", getParmString(Constants.CLIENT_ASSERTION));
        addToAllowableTimeoutCount(1);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain " + getParmString(Constants.JWT_CLIENT_SECRET) + " and should have.", getParmString(Constants.JWT_CLIENT_SECRET));

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256
     * Specify a keyAliasName that is actually for an rs384 key - the token will be built successfully since both the
     * sigAlg and key are RS types.
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="rs384"
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_sigAlgMistmatchKeyAlias_RsTypes() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_private_key_mismatch2");

        positiveTestWithPrivateKey(updatedTestSettings);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256
     * Specify a keyAliasName that is actually for an es384 key - the request will fail because the key is of the wrong type
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="es384"
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException" })
    @Test
    public void PrivateKeyJwtClientTests_sigAlgMistmatchKeyAlias_RsVsEsTypes() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_private_key_mismatch1");

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the JWT could not be build for client authentication.", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER + ".*" + MessageConstants.CWWKS2432E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR + ".+" + MessageConstants.CWWKS2430E_FAILED_TO_BUILD_TOKEN_FOR_CLIENT_AUTH);

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256.
     * This client specifies the real token endpoint in the OP. The Liberty OP does not currently support private key jwt, so
     * the OP will not recognize the client_assertion and will fail because the client_id and client_secret are mossing.
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="rs256"
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.http.BadPostRequestException" })
    @Test
    public void PrivateKeyJwtClientTests_useRealTokenEndpoint() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_withRealTokenEndpoint");

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations(Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "The response message did not contain " + Constants.BAD_REQUEST, null, Constants.BAD_REQUEST);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The response id not get a message about an invalid request", null, MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);

        if (testSettings.getFlowType().equals(SocialConstants.SOCIAL)) {
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The response id not get a message about an invalid request", null, MessageConstants.CWWKS5489E_AUTH_ERROR);
        }

        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the token request had an invalid client credential.", MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the required parm client_id was missing.", MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256.
     * This client does not specify an SSLRef. Show that the server wide SSL config is NOT used currently. The request
     * will currently fail - the runtime may be updated in the future to fall back to the serverwide config.
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="rs256"
     */
    @AllowedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException", "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_noClientSSLRef() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_noClientSSLRef");

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that there was no keystore in the config.", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER + ".*" + MessageConstants.CWWKS2432E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR + ".+" + MessageConstants.CWWKS2430E_FAILED_TO_BUILD_TOKEN_FOR_CLIENT_AUTH + ".+" + MessageConstants.CWWKS2434E_PRIVATE_KEY_JWT_MISSING_KEYSTORE_REF);

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="altrs256"
     * The certificate's alias name used is coming from the keystore specified by the sslRef.
     * The trustStoreRef specified does NOT contain a cert with that same alias, but the truststore that is
     * specified in the sslRef does and that is where we'll find the cert to verify the content.
     * The Overall request will fail because the Liberty OP doesn't support private key jwt.
     */
    /*
     * Skip this test when using a social client - it does not have a trustStoreRef, so, all cases use the sslRef
     */
    @AllowedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException", "java.security.cert.CertificateException", "io.openliberty.security.oidcclientcore.http.BadPostRequestException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfJava8OrSocial.class)
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesRS256_mismatchedKeyAliasNames_trustRefSslRefMatch() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_alt_RS256_RS256_match");

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations(Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The response id not get a message about an invalid request", null, MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the token request had an invalid client credential.", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER + ".*" + MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the required parm client_id was missing.", MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="altrs256"
     * The client's alias name used with the keystore is different than the alias in the trust store
     * The sslRef specified uses the key and trust store that contain the alias used to create the private key - we should fail to
     * get access to the app because the trustStoreRef should override the trust store from the sslRef and the trustStoreRef
     * points to a trust store that uses a different alias name
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException", "java.security.cert.CertificateException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfJava8.class) // Java 8 has trouble with the trust store we're using.
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesRS256_mismatchedKeyAliasNames_trustRefSslRefMisMatch() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_alt_RS256_RS256_mismatch");

        List<validationData> expectations = setPrivateKeyJwtCommonExpectations();
        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the corresponding alias could not be found in the trust store.", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER + ".*" + MessageConstants.CWWKS2432E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR + ".+" + MessageConstants.CWWKS2430E_FAILED_TO_BUILD_TOKEN_FOR_CLIENT_AUTH + ".+" + MessageConstants.CWWKS2436E_CANNOT_RETRIEVE_PUBLIC_KEY_FROM_TRUSTSTORE);

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    /**
     * Test with the client config using the signature algorithm of RS256, and a private key jwt configured with RS256
     * tokenEndpointAuthMethod="private_key_jwt"
     * tokenEndpointAuthSigningAlgorithm="RS256"
     * keyAliasName="altrs256"
     * The client's alias name used with the keystore is the same at that in the trust store. The cert using that alias name in
     * the keystore is a different cert.
     * We should not have access to the protected app.
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfJava8.class) // Java 8 has trouble with the trust store we're using.
    @AllowedFFDC({ "io.openliberty.security.oidcclientcore.http.BadPostRequestException", "sun.security.validator.ValidatorException", "javax.net.ssl.SSLHandshakeException", "java.security.cert.CertPathValidatorException" })
    @Test
    public void PrivateKeyJwtClientTests_accessTokenUsesRS256_privateKeyJwtUsesRS256_sameAliasDiffCert() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_alt_RS256_RS256_diffTrust");

        List<validationData> expectations = null;
        if (testSettings.getFlowType().equals(SocialConstants.SOCIAL)) {
            expectations = setPrivateKeyJwtCommonExpectations(Constants.UNAUTHORIZED_STATUS);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "The response message did not contain " + Constants.UNAUTHORIZED_MESSAGE, null, Constants.UNAUTHORIZED_MESSAGE);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The response id not get a message about an invalid request", null, MessageConstants.CWWKS5489E_AUTH_ERROR);
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "The response id not get a message about a SSL Handshake exception", MessageConstants.CWPKI0823E_SSL_HANDSHAKE_FAILURE);
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "The response id not get a message stating that the client was unable to contact the provider", MessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER);
        } else {
            expectations = setPrivateKeyJwtCommonExpectations(Constants.BAD_REQUEST_STATUS);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "The response message did not contain " + Constants.BAD_REQUEST, null, Constants.BAD_REQUEST);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The response id not get a message about an invalid request", null, MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
            expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the token request had an invalid client credential.", MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message stating that the required parm client_id was missing.", MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);
        }

        negativeTestWithPrivateKey(updatedTestSettings, expectations);

    }

    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_tokenRequestOriginHeader_private_key_jwt() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_originHeader_valid");

        positiveTest(updatedTestSettings, AuthMethod.PRIVATE_KEY_JWT, clientServer.getHttpString());

    }

    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void PrivateKeyJwtClientTests_tokenRequestOriginHeader_post() throws Exception {

        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_post_originHeader_valid");

        positiveTest(updatedTestSettings, AuthMethod.CLIENT_SECRET_POST, clientServer.getHttpString());

    }

    @Test
    public void PrivateKeyJwtClientTests_tokenRequestOriginHeader_post_endToEnd() throws Exception {

        // Most of the tests use a fake/test token endpoint that logs the headers/params - using that prevents us from going end
        // to end (getting access to the test app) This test uses the real token endpoint and shows that with the origin header included we can still access the protected app
        TestSettings updatedTestSettings = updateTestCaseSettings("client_RS256", Constants.SIGALG_RS256, "client_post_noStubs_originHeader_valid");

        WebClient webClient = getAndSaveWebClient(true);

        List<validationData> expectations = vData.addSuccessStatusCodes();

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Should have gotten to the protected app", "", "Servlet");

        genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

}
