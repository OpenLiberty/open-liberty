/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.social.fat.commonTests;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.MessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run tests to verify the correct behavior with
 * all supported signature algorithms.
 *
 * Since we do not support the additional signature algorithms in the OP, we will need
 * to create a test tool token endpoint.
 * Each test case will invoke a test tooling app that will invoke the jwtBuilder to create a jwt.
 * The test case will specify which builder to use - there is a builder for each signature
 * algorithm. The test app will create the JWT token, then save that token.
 * The Social Client config will specify the test tooling app instead of the standard token endpoint.
 * The test tooling app will return the saved JWT token as the access_token and id_token.
 *
 * This allows us to test that the Social Client can handle a token signed with signature algorithms that
 * our OP does not support.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Social_EncryptionTests extends SocialCommonTest {

    public static Class<?> thisClass = Social_EncryptionTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String test_FinalAction = SocialConstants.PERFORM_IDP_LOGIN;
    public static final JwtTokenBuilderUtils tokenBuilderHelpers = new JwtTokenBuilderUtils();
    public static final String badTokenSegment = "1234567890123456789";

    public static boolean isTestingOidc = false; // subclasses may set this to true

    /**
     * All of the test cases in this class perform the same steps - the only real differences are:
     * 1) which builder will be used to create a JWT Token,
     * 2) which test app they'll use (the test app dictates which Social Client config will be used),
     * 3) and whether to expect a success or failure (failure indicated by a mis-mismatch in the algorithm used by the
     * builder and Social Client
     * Passing in the builder and Social Client signature and encryption algorithms can tell a common method all it needs to know.
     *
     * This method invokes the test tooling app "TokenEndpointServlet/saveToken" to create the JWT token using the builder
     * specified.
     * It then sets up the expectations for the current instance - if the builder and Social Client signature and encryption
     * algorithms match, expectations are set to validate token content, if they do not match expectations are set for a 401
     * status code and server side error messages indicating the mis-match.
     * Finally, this method tries to invoke the test app protected by the Social Client. The Social Client config will end up
     * using the test tooling app's token endpoint and use the "JWT Builder" generated token.
     *
     * @param encryptAlgForBuilder
     *            - the signature algorithm that the builder will use - the builder configs are named such that they start with
     *            the signature algorithm (ie: HS256Builder)
     * @param decryptAlgForSocialClient
     *            - the signature algorithm that the Social Client will use - the test app names match filters in the Social
     *            Client config that cause
     *            the Social Client config to be used with the specified signature algorithm (ie: formlogin/simple/HS256)
     * @throws Exception
     */
    public void genericEncryptTest(String encryptAlgForBuilder, String decryptAlgForSocialClient) throws Exception {
        genericEncryptTest(encryptAlgForBuilder, setBuilderName(encryptAlgForBuilder), decryptAlgForSocialClient, setAppName(decryptAlgForSocialClient), null);
    }

    public void genericEncryptTest(String encryptAlgForBuilder, String decryptAlgForSocialClient, List<endpointSettings> parms) throws Exception {
        genericEncryptTest(encryptAlgForBuilder, setBuilderName(encryptAlgForBuilder), decryptAlgForSocialClient, setAppName(decryptAlgForSocialClient), null, parms);
    }

    public void genericEncryptTest(String encryptAlgForBuilder, String builderId, String decryptAlgForSocialClient, String appName, List<validationData> expectations) throws Exception {

        genericEncryptTest(encryptAlgForBuilder, builderId, decryptAlgForSocialClient, appName, expectations, null);
    }

    public void genericEncryptTest(String encryptAlgForBuilder, String builderId, String decryptAlgForSocialClient, String appName, List<validationData> expectations,
            List<endpointSettings> parms) throws Exception {

        genericEncryptTest(encryptAlgForBuilder, builderId, decryptAlgForSocialClient, appName, decryptAlgForSocialClient, expectations, parms);
    }

    public void genericEncryptTest(String encryptAlgForBuilder, String builderId, String decryptAlgForSocialClient, String appName, String sigAlg, List<validationData> expectations,
            List<endpointSettings> parms) throws Exception {

        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "******** Testing with Jwt builder using encryption algorithm: " + encryptAlgForBuilder + " and Social Client using encryption algorithm: " + decryptAlgForSocialClient + " ********");
        Log.info(thisClass, _testName, "********************************************************************************************************************");

        WebClient wc = getAndSaveWebClient();
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(updatedSocialTestSettings.getProtectedResource() + appName);
        updatedSocialTestSettings.setSignatureAlg(sigAlg);
        updatedSocialTestSettings.setDecryptKey(JwtKeyTools.getComplexPrivateKeyForSigAlg(testOPServer.getServer(), decryptAlgForSocialClient));

        if (expectations == null) {
            // if the encryption alg in the build matches what's in the Social Client, the test should succeed - validate status codes and token content
            if (encryptAlgForBuilder.equals(decryptAlgForSocialClient)) {
                expectations = getSuccessfulLoginExpectations(updatedSocialTestSettings);
            } else {
                // create negative expectations when encrypting algorithms don't match
                expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
                if (SocialConstants.SIGALG_NONE.equals(encryptAlgForBuilder) || SocialConstants.SIGALG_NONE.equals(decryptAlgForSocialClient)) {

                    if (SocialConstants.SIGALG_NONE.equals(encryptAlgForBuilder)) {
                        //                        String[] errorMsgs = new String[] { MessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING, MessageConstants.CWWKS5498E_CANNOT_CREATE_JWT_USING_CONFIG, MessageConstants.CWWKS6064E_TOKEN_IS_NOT_A_JWE, MessageConstants.CWWKS5453E_CANNOT_CREATE_JWT_FROM_ID_TOKEN, };
                        String[] errorMsgs = new String[] { MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN, MessageConstants.CWWKS1537E_JWE_IS_NOT_VALID };
                        for (String msg : errorMsgs) {
                            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Client messages.log should contain a message [" + msg + "] indicating that the JWT was a JWS and the social client expected a JWE.", msg + ".*");
                        }
                    }
                    if (SocialConstants.SIGALG_NONE.equals(decryptAlgForSocialClient)) {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Client messages.log should contain a message indicating that the JWT was a JWE and the social client expected a JWS.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS1536E_TOKEN_IS_NOT_A_JWS);
                    }
                } else {

                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
                }
            }
        }

        parms = eSettings.addEndpointSettingsIfNotNull(parms, "builderId", builderId);

        // Invoke the test TokenEndpoint stub.  It will invoke the Jwt Builder to create a JWT Token (using the builder specified in the builderId passed in via parms
        // The TokenEndpoint stub will save that token and it will be returned when the Social Client uses it's TokenEnpdointUrl specified in it's config
        //  (That url is:  http://localhost:${bvt.prop.security_1_HTTP_default}/TokenEndpointServlet/getToken)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedSocialTestSettings.getTokenEndpt(), SocialConstants.PUTMETHOD, "misc", parms, null, expectations);

        // we created and saved a jwt for our test tooling token endpoint to return to the Social Client - let's invoke
        // the protected resource.  The Social Client will get the auth token, but, instead of getting a jwt from the OP, it will use a
        // token endpoint pointing to the test tooling app that will return the jwt previously obtained using a builder
        genericSocial(_testName, wc, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Create builder name with the same alg for both signing and encrypting
     *
     * @param alg
     * @return the builder name to use
     */
    public String setBuilderName(String alg) {
        return setBuilderName(alg, alg);
    }

    /**
     * Create build name based on the signature algorithm and encryption alg passed in
     *
     * @param sigAlg
     *            - signature alg
     * @param encryptAlg
     *            - encryption alg
     * @return the builder name to use
     */
    public String setBuilderName(String sigAlg, String encryptAlg) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "********************* Testing with Jwt builder - Signing with " + sigAlg + " and encrypting using: " + encryptAlg + " ********************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return "Sign" + sigAlg + "Encrypt" + encryptAlg + "Builder";
    }

    /**
     * Create app name with the same alg for both verifying and decrypting
     *
     * @param alg
     *            - the alg to use to build the appname to call (this will in turn result in the appropriate Social Client config
     *            to be used
     *            to verify and decrypt the token)
     * @return - the appname to call
     */
    public String setAppName(String alg) {
        return setAppName(alg, alg);
    }

    /**
     * Create app name with the same alg for both verifying and decrypting
     *
     * @param sigAlg
     *            - the sig alg to use to build the appname to call (this will in turn result in the appropriate Social Client
     *            config to be
     *            used to verify and decrypt the token)
     * @param decryptAlg
     *            - the decrypt alg to use to build the appname to call (this will in turn result in the appropriate Social Client
     *            config to
     *            be used to verify and decrypt the token)
     * @return - the appname to call
     */
    public String setAppName(String sigAlg, String decryptAlg) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName,
                "************************ Testing with Social Client - Verifying with " + sigAlg + " and decrypting using: " + decryptAlg + " ************************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return "Sign" + sigAlg + "Encrypt" + decryptAlg;
    }

    public List<validationData> getSuccessfulLoginExpectations(SocialTestSettings socialSettings) throws Exception {
        List<validationData> expectations = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, SocialConstants.RESPONSE_FULL, SocialConstants.IDToken_STR);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, SocialConstants.RESPONSE_FULL, SocialConstants.IDToken_STR_START + ".*?\"" + SocialConstants.IDTOK_AUDIENCE_KEY + "\":\"" + socialSettings.getClientID() + "\".*");
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, SocialConstants.RESPONSE_FULL, SocialConstants.IDToken_STR_START + ".*?\"" + SocialConstants.IDTOK_SUBJECT_KEY + "\":\"" + socialSettings.getAdminUser() + "\".*");
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, SocialConstants.RESPONSE_FULL, SocialConstants.IDToken_STR_START + ".*?\"" + SocialConstants.IDTOK_REALM_KEY + "\":\"" + socialSettings.getRealm() + "\".*");
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, SocialConstants.RESPONSE_FULL, SocialConstants.IDToken_STR_START + ".*?\"" + SocialConstants.IDTOK_UNIQ_SEC_NAME_KEY + "\":\"" + socialSettings.getAdminUser() + "\".*");
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, SocialConstants.RESPONSE_FULL, SocialConstants.IDToken_STR_START + ".*?\"" + SocialConstants.IDTOK_ISSUER_KEY + "\":\"http:\\/\\/" + hostName + ":" + testOPServer.getHttpDefaultPort() + "/TokenEndpointServlet\".*");
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, SocialConstants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, socialSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, socialSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, socialSettings);
        return expectations;
    }

    public List<validationData> getMissingDecryptionSettingsExpectations() throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(perform_social_login, invoke_social_login_actions);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the Social Client logs.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS1536E_TOKEN_IS_NOT_A_JWS + ".*");
        return expectations;
    }

    public List<validationData> getInvalidFormatExpectations() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the token was invalid.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the token was invalid.", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
        return expectations;
    }

    public List<validationData> getInvalidNumberOfPartsExpectations() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1537E_JWE_IS_NOT_VALID);
        return expectations;
    }

    /**
     * TODO when issue 17485 is completed, remove setting/passing parms and update the builder configs that encrypt with ES algs
     * with keyManagementKeyAlgorithm set to ECDH-ES
     *
     * @param alg
     * @return
     * @throws Exception
     */
    public List<endpointSettings> setParmsForECWorkaround(String alg) throws Exception {

        testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, JwtConstants.PARAM_KEY_MGMT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_ES);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), alg));
        return parms;
    }

    public String createGenericRS256JWE() throws Exception {
        // We're going to use a test JWT token builder to create a token that has "notJOSE" in the JWE header type field
        // the Liberty builder won't allow us to update that field, so, we need to peice a token together
        JWTTokenBuilder builder = tokenBuilderHelpers.populateAlternateJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), SocialConstants.SIGALG_RS256)));
        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setAlorithmHeaderValue(SocialConstants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setClaim("token_src", "testcase builder");
        // calling buildJWE will override the header contents
        String jwtToken = builder.buildJWE("JOSE", "jwt");

        return jwtToken;
    }

    public String createTokenWithBadElement(int badPart) throws Exception {

        String createTokenWithBadElement = "createTokenWithBadElement";
        String jwtToken = createGenericRS256JWE();
        Log.info(thisClass, createTokenWithBadElement, jwtToken);
        String[] jwtTokenArray = jwtToken.split("\\.");
        Log.info(thisClass, createTokenWithBadElement, "size: " + jwtTokenArray.length);
        String badJweToken = "";

        for (int i = 0; i < 5; i++) {
            Log.info(thisClass, createTokenWithBadElement, "i=" + i);
            Log.info(thisClass, createTokenWithBadElement, "badJweToken: " + badJweToken);
            Log.info(thisClass, createTokenWithBadElement, "subString: " + jwtTokenArray[i]);
            if (!badJweToken.equals("")) {
                badJweToken = badJweToken + ".";
            }
            if (i == (badPart - 1)) {
                badJweToken = badJweToken + badTokenSegment;
            } else {
                badJweToken = badJweToken + jwtTokenArray[i];
            }
        }
        return badJweToken;
    }

    /******************************* tests *******************************/
    /************** jwt builder/Social Client using the same encryption algorithm **************/
    /**
     * Test shows that the Social Client can consume a JWE encrypted with RS256
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_EncryptTokenRS256_SocialClientDecryptRS256() throws Exception {

        genericEncryptTest(SocialConstants.SIGALG_RS256, SocialConstants.SIGALG_RS256);

    }

    /**
     * Test shows that the Social Client can consume a JWE encrypted with RS384
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_EncryptTokenRS384_SocialClientDecryptRS384() throws Exception {

        genericEncryptTest(SocialConstants.SIGALG_RS384, SocialConstants.SIGALG_RS384);

    }

    /**
     * Test shows that the Social Client can consume a JWE encrypted with RS512
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_EncryptTokenRS512_SocialClientDecryptRS512() throws Exception {

        genericEncryptTest(SocialConstants.SIGALG_RS512, SocialConstants.SIGALG_RS512);

    }

    /**
     * Test shows that the Social Client can consume a JWE encrypted with ES256
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    // @Test Using ECDH-ES to encrypt the Content Encryption Key of a JWE not officially supported in jwtBuilder yet (issue 17485)
    public void Social_EncryptionTests_EncryptTokenES256_SocialClientDecryptES256() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(SocialConstants.SIGALG_ES256, SocialConstants.SIGALG_ES256, setParmsForECWorkaround(JwtConstants.SIGALG_ES256));

    }

    /**
     * Test shows that the Social Client can consume a JWE encrypted with ES384
     *
     * @throws Exception
     */
    // @Test Using ECDH-ES to encrypt the Content Encryption Key of a JWE not officially supported in jwtBuilder yet (issue 17485)
    public void Social_EncryptionTests_EncryptTokenES384_SocialClientDecryptES384() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(SocialConstants.SIGALG_ES384, SocialConstants.SIGALG_ES384, setParmsForECWorkaround(SocialConstants.SIGALG_ES384));

    }

    /**
     * Test shows that the Social Client can consume a JWE encrypted with ES512
     *
     * @throws Exception
     */
    // @Test Using ECDH-ES to encrypt the Content Encryption Key of a JWE not officially supported in jwtBuilder yet (issue 17485)
    public void Social_EncryptionTests_EncryptTokenES512_SocialClientDecryptES512() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(SocialConstants.SIGALG_ES512, SocialConstants.SIGALG_ES512, setParmsForECWorkaround(SocialConstants.SIGALG_ES512));

    }

    /*********** jwt builder/Social Client using the different algorithm ************/
    /* Show that we can't validate the token if the encryption algorithms */
    /* don't match */
    /*********************************************************************/
    /**
     * Test shows that the Social Client can NOT consume a JWT that is NOT encrypted with RS256
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_EncryptTokenNotWithRS256_SocialClientDecryptRS256() throws Exception {

        String socialClientDecryptAlg = SocialConstants.SIGALG_RS256;
        for (String builderEncryptAlg : SocialConstants.ALL_TEST_ENCRYPTALGS) {
            if (!socialClientDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, Social Client specifies original alg for sign, but RS256 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                // Testing ECDH-ES for encrypting the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null);
                }
            }
        }

    }

    /**
     * Test shows that the Social Client can NOT consume a JWT that is NOT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_EncryptTokenNotWithRS384_RPDecryptRS384() throws Exception {

        String socialClientDecryptAlg = SocialConstants.SIGALG_RS384;
        for (String builderEncryptAlg : SocialConstants.ALL_TEST_ENCRYPTALGS) {
            if (!socialClientDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but RS384 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                // Testing ECDH-ES for encrypting the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null);
                }
            }
        }

    }

    /**
     * Test shows that the Social Client can NOT consume a JWT that is NOT signed with RS512
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_EncryptTokenNotWithRS512_RPDecryptRS512() throws Exception {

        String socialClientDecryptAlg = SocialConstants.SIGALG_RS512;
        for (String builderEncryptAlg : SocialConstants.ALL_TEST_ENCRYPTALGS) {
            if (!socialClientDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but RS512 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                // Testing ECDH-ES for encrypting the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null);
                }
            }
        }

    }

    /**
     * Test shows that the Social Client can NOT consume a JWT that is NOT encrypted with RS256
     *
     * @throws Exception
     */
    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_EncryptTokenNotWithES256_RPDecryptES256() throws Exception {

        String socialClientDecryptAlg = SocialConstants.SIGALG_ES256;
        for (String builderEncryptAlg : SocialConstants.ALL_TEST_ENCRYPTALGS) {
            if (!socialClientDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but ES256 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null);
                }
            }
        }

    }

    /**
     * Test shows that the Social Client can NOT consume a JWT that is NOT signed with ES384
     *
     * @throws Exception
     */
    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_EncryptTokenNotWithES384_RPDecryptES384() throws Exception {

        String socialClientDecryptAlg = SocialConstants.SIGALG_ES384;
        for (String builderEncryptAlg : SocialConstants.ALL_TEST_ENCRYPTALGS) {
            if (!socialClientDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but ES384 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null);
                }
            }
        }

    }

    /**
     * Test shows that the Social Client can NOT consume a JWT that is NOT signed with ES512
     *
     * @throws Exception
     */
    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_EncryptTokenNotWithES512_RPDecryptES512() throws Exception {

        String socialClientDecryptAlg = SocialConstants.SIGALG_ES512;
        for (String builderEncryptAlg : SocialConstants.ALL_TEST_ENCRYPTALGS) {
            if (!socialClientDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but ES512 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), socialClientDecryptAlg, setAppName(builderEncryptAlg, socialClientDecryptAlg), null);
                }
            }
        }

    }

    /*********** jwt builder/Social Client mix signature algs with differnt encryption algorithms ************/
    /* Show that we can't validate the token if the signature and encryption are not the same */
    /**********************************************************************************************/
    /**
     * Test shows that the Social Client can consume a JWT that is encrypted/decrypted with RS256 and signed with all other
     * supported
     * signature algorithms.
     * This test encrypts/decrypts with RS256, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_SignWithVariousAlgs_EncryptWithRS256_DecryptWithRS256() throws Exception {

        String encryptDecryptAlg = SocialConstants.SIGALG_RS256;
        for (String builderSigAlg : SocialConstants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), builderSigAlg, null, null);
            }
        }

    }

    /**
     * Test shows that the Social Client can consume a JWT that is encrypted/decrypted with RS384 and signed with all other
     * supported
     * signature algorithms.
     * This test encrypts/decrypts with RS384, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_SignWithVariousAlgs_EncryptWithRS384_DecryptWithRS384() throws Exception {

        String encryptDecryptAlg = SocialConstants.SIGALG_RS384;
        for (String builderSigAlg : SocialConstants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), builderSigAlg, null, null);
            }
        }
    }

    /**
     * Test shows that the Social Client can consume a JWT that is encrypted/decrypted with RS512 and signed with all other
     * supported
     * signature algorithms.
     * This test encrypts/decrypts with RS512, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_SignWithVariousAlgs_EncryptWithRS512_DecryptWithRS512() throws Exception {

        String encryptDecryptAlg = SocialConstants.SIGALG_RS512;
        for (String builderSigAlg : SocialConstants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), builderSigAlg, null, null);
            }
        }
    }

    /**
     * Test shows that the Social Client can consume a JWT that is encrypted/decrypted with ES256 and signed with all other
     * supported
     * signature algorithms.
     * This test encrypts/decrypts with ES256, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    // @Test Using ECDH-ES to encrypt the Content Encryption Key of a JWE not officially supported in jwtBuilder yet (issue 17485)
    public void Social_EncryptionTests_SignWithVariousAlgs_EncryptWithES256_DecryptWithES256() throws Exception {

        String encryptDecryptAlg = SocialConstants.SIGALG_ES256;
        for (String builderSigAlg : SocialConstants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null, setParmsForECWorkaround(encryptDecryptAlg));
            }
        }

    }

    /**
     * Test shows that the Social Client can consume a JWT that is encrypted/decrypted with ES384 and signed with all other
     * supported
     * signature algorithms.
     * This test encrypts/decrypts with ES384, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    // @Test Using ECDH-ES to encrypt the Content Encryption Key of a JWE not officially supported in jwtBuilder yet (issue 17485)
    public void Social_EncryptionTests_SignWithVariousAlgs_EncryptWithES384_DecryptWithES384() throws Exception {

        String encryptDecryptAlg = SocialConstants.SIGALG_ES384;
        for (String builderSigAlg : SocialConstants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null, setParmsForECWorkaround(encryptDecryptAlg));
            }
        }
    }

    /**
     * Test shows that the Social Client can consume a JWT that is encrypted/decrypted with ES512 and signed with all other
     * supported
     * signature algorithms.
     * This test encrypts/decrypts with ES512, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    // @Test Using ECDH-ES to encrypt the Content Encryption Key of a JWE not officially supported in jwtBuilder yet (issue 17485)
    public void Social_EncryptionTests_SignWithVariousAlgs_EncryptWithES512_DecryptWithES512() throws Exception {

        String encryptDecryptAlg = SocialConstants.SIGALG_ES512;
        for (String builderSigAlg : SocialConstants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null, setParmsForECWorkaround(encryptDecryptAlg));
            }
        }
    }

    /*************** Various JWE header content tests ***************/
    /* Show that we can handle non-default values where allowed and */
    /* fail appropriately when we do not allow non0default values */
    /****************************************************************/
    @Test
    public void Social_EncryptionTests_SignWithValidAlg_EncryptValid_DecryptInvalidKeyManagementKeyAlias() throws Exception {
        String socialClientEncryptAlg = SocialConstants.SIGALG_RS256;
        String socialClientDecryptAlg = SocialConstants.SIGALG_RS256;

        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the RP logs.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "InvalidKeyException");
        genericEncryptTest(socialClientEncryptAlg, setBuilderName(socialClientEncryptAlg), socialClientDecryptAlg, "InvalidKeyManagementKeyAlias", expectations);
    }

    @Test
    public void Social_EncryptionTests_SignWithValidAlg_EncryptValid_DecryptNonExistantKeyManagementKeyAlias() throws Exception {
        String socialClientEncryptAlg = SocialConstants.SIGALG_RS256;
        String socialClientDecryptAlg = SocialConstants.SIGALG_RS256;

        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the RP logs.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "nonExistantKeyManagementKeyAlias" + ".*" + "not present");
        genericEncryptTest(socialClientEncryptAlg, setBuilderName(socialClientEncryptAlg), socialClientDecryptAlg, "NonExistantKeyManagementKeyAlias", expectations);
    }

    //    This test is the same as all of the Social_EncryptionTests_SignWithValidAlg_EncryptWith<*>_DoNotDecrypt tests
    //    public void Social_EncryptionTests_SignWithValidAlg_EncryptValid_DecryptOmittedKeyManagementKeyAlias() throws Exception {

    /************** Don't encrypt the token, Social Client decrypts *************/
    /* Don't encrypt the token, but the Social Client config "expects" and */
    /* encrypted token - show that we will still consume it */
    /*****************************************************************/
    @Test
    public void Social_EncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS256() throws Exception {
        String signAlg = SocialConstants.SIGALG_RS256;
        String socialClientEncryptAlg = SocialConstants.SIGALG_NONE;
        String socialClientDecryptAlg = SocialConstants.SIGALG_RS256;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), signAlg, null, null);
    }

    @Test
    public void Social_EncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS384() throws Exception {
        String signAlg = SocialConstants.SIGALG_RS384;
        String socialClientEncryptAlg = SocialConstants.SIGALG_NONE;
        String socialClientDecryptAlg = SocialConstants.SIGALG_RS384;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), signAlg, null, null);
    }

    @Test
    public void Social_EncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS512() throws Exception {
        String signAlg = SocialConstants.SIGALG_RS512;
        String socialClientEncryptAlg = SocialConstants.SIGALG_NONE;
        String socialClientDecryptAlg = SocialConstants.SIGALG_RS512;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), signAlg, null, null);
    }

    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES256() throws Exception {
        String signAlg = SocialConstants.SIGALG_ES256;
        String socialClientEncryptAlg = SocialConstants.SIGALG_NONE;
        String socialClientDecryptAlg = SocialConstants.SIGALG_ES256;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), signAlg, null, null);
    }

    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES384() throws Exception {
        String signAlg = SocialConstants.SIGALG_ES384;
        String socialClientEncryptAlg = SocialConstants.SIGALG_NONE;
        String socialClientDecryptAlg = SocialConstants.SIGALG_ES384;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), signAlg, null, null);
    }

    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES512() throws Exception {
        String signAlg = SocialConstants.SIGALG_ES512;
        String socialClientEncryptAlg = SocialConstants.SIGALG_NONE;
        String socialClientDecryptAlg = SocialConstants.SIGALG_ES512;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), signAlg, null, null);
    }

    /************** Encrypt the token, Social Client does not decrypt *************/
    /* Encrypt the token, but the Social Client config does not have decryption */
    /* enabled - show that we fail with the appropriate errors */
    /*******************************************************************/
    @Test
    public void Social_EncryptionTests_SignWithValidAlg_EncryptWithRS256_DoNotDecrypt() throws Exception {
        String signAlg = SocialConstants.SIGALG_RS256;
        String socialClientEncryptAlg = SocialConstants.SIGALG_RS256;
        String socialClientDecryptAlg = SocialConstants.SIGALG_NONE;

        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), getMissingDecryptionSettingsExpectations());

    }

    @Test
    public void Social_EncryptionTests_SignWithValidAlg_EncryptWithRS384_DoNotDecrypt() throws Exception {
        String signAlg = SocialConstants.SIGALG_RS384;
        String socialClientEncryptAlg = SocialConstants.SIGALG_RS384;
        String socialClientDecryptAlg = SocialConstants.SIGALG_NONE;

        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), getMissingDecryptionSettingsExpectations());

    }

    @Test
    public void Social_EncryptionTests_SignWithValidAlg_EncryptWithRS512_DoNotDecrypt() throws Exception {
        String signAlg = SocialConstants.SIGALG_RS512;
        String socialClientEncryptAlg = SocialConstants.SIGALG_RS512;
        String socialClientDecryptAlg = SocialConstants.SIGALG_NONE;

        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), getMissingDecryptionSettingsExpectations());
    }

    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_SignWithValidAlg_EncryptWithES256_DoNotDecrypt() throws Exception {
        String signAlg = SocialConstants.SIGALG_ES256;
        String socialClientEncryptAlg = SocialConstants.SIGALG_ES256;
        String socialClientDecryptAlg = SocialConstants.SIGALG_NONE;

        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), getMissingDecryptionSettingsExpectations(),
                setParmsForECWorkaround(socialClientEncryptAlg));
    }

    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_SignWithValidAlg_EncryptWithES384_DoNotDecrypt() throws Exception {
        String signAlg = SocialConstants.SIGALG_ES384;
        String socialClientEncryptAlg = SocialConstants.SIGALG_ES384;
        String socialClientDecryptAlg = SocialConstants.SIGALG_NONE;

        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), getMissingDecryptionSettingsExpectations(),
                setParmsForECWorkaround(socialClientEncryptAlg));
    }

    @Test // Testing ECDH-ES to encrypt the Content Encryption Key of a JWE, but not officially supported yet (issue 17485)
    public void Social_EncryptionTests_SignWithValidAlg_EncryptWithES512_DoNotDecrypt() throws Exception {
        String signAlg = SocialConstants.SIGALG_ES512;
        String socialClientEncryptAlg = SocialConstants.SIGALG_ES512;
        String socialClientDecryptAlg = SocialConstants.SIGALG_NONE;

        genericEncryptTest(socialClientEncryptAlg, setBuilderName(signAlg, socialClientEncryptAlg), socialClientDecryptAlg, setAppName(signAlg, socialClientDecryptAlg), getMissingDecryptionSettingsExpectations(),
                setParmsForECWorkaround(socialClientEncryptAlg));
    }

    /*************** Various JWE header content tests ***************/
    /* Show that we can handle non-default values where allowed and */
    /* fail appropriately when we do not allow non0default values */
    /****************************************************************/
    /**
     * The Social Client server.xml has a config that specifies a key management key alias using an RS256 Cert - this test ensures
     * that
     * after building a jwt that is encrypted with the matching public key, but using "A192GCM" as the contentEncryptionAlg,
     * the Social Client can consume the token with the matching private key.
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_consumeTokenThatWasEncryptedUsingOtherContentEncryptionAlg() throws Exception {

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, JwtConstants.PARAM_CONTENT_ENCRYPT_ALG, JwtConstants.CONTENT_ENCRYPT_ALG_192);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), JwtConstants.SIGALG_RS256));

        genericEncryptTest(SocialConstants.SIGALG_RS256, SocialConstants.SIGALG_RS256, parms);
    }

    /**
     * Show that the Social Client can accept a token built specifying RSA_OAEP_256 (instead of RSA-OAEP) in the
     * KeyManagementKeyAlgorithm
     * key.
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_consumeTokenThatWasEncryptedUsingOtherKeyManagementKeyAlg() throws Exception {

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, JwtConstants.PARAM_KEY_MGMT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_256);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), JwtConstants.SIGALG_RS256));

        genericEncryptTest(SocialConstants.SIGALG_RS256, SocialConstants.SIGALG_RS256, parms);
    }

    /**
     * Show that the Social Client can accept a token with a JWE header "typ" that does not contain JOSE.
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_JWETypeNotJose() throws Exception {

        // We're going to use a test JWT token builder to create a token that has "notJOSE" in the JWE header type field
        // the Liberty builder won't allow us to update that field, so, we need to peice a token together
        JWTTokenBuilder builder = tokenBuilderHelpers.populateAlternateJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), SocialConstants.SIGALG_RS256)));
        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setAlorithmHeaderValue(SocialConstants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setClaim("token_src", "testcase builder");
        builder.setAudience("client01");
        builder.setIssuedAtToNow();
        builder.setExpirationTimeMinutesIntheFuture(120);
        builder.setClaim(SocialConstants.PAYLOAD_AT_HASH, "dummy_hash_value");
        builder.setClaim(SocialConstants.IDTOK_UNIQ_SEC_NAME_KEY, "testuser");
        // calling buildJWE will override the header contents
        String jwtToken = builder.buildJWE("notJOSE", "jwt");

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), null, parms);
    }

    @Test
    public void Social_EncryptionTests_JWEContentTypeNotJwt() throws Exception {

        // We're going to use a test JWT token builder to create a token that has "not_jwt" in the JWE header content type field
        // the Liberty builder won't allow us to update that field, so, we need to peice a token together
        JWTTokenBuilder builder = tokenBuilderHelpers.populateAlternateJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), SocialConstants.SIGALG_RS256)));
        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setAlorithmHeaderValue(SocialConstants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setClaim("token_src", "testcase builder");
        // calling buildJWE will override the header contents
        String jwtToken = builder.buildJWE("JOSE", "not_jwt");

        // the built token will be pass to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the Social Client logs for a bad 'cty' in the ID token.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + MessageConstants.CWWKS6057E_CTY_NOT_JWT_FOR_NESTED_JWS);
        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), expectations, parms);

    }

    /*************** Misc tests ***************/
    /**
     * Include a simple Json payload instead of a JWS - make sure that we fail with an appropriate message
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_simpleJsonPayload() throws Exception {

        // build a jwt token whose payload contains only json data - make sure that we do not allow this format (it's not supported at this time)
        String jwtToken = tokenBuilderHelpers.buildAlternatePayloadJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), SocialConstants.SIGALG_RS256)));

        // the built token will be pass to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the Social Client logs saying the payload of the JWE ID token wasn't a JWS.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6065E_NESTED_JWS_REQUIRED_BUT_NOT_FOUND);
        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), expectations, parms);

    }

    /**
     * The Social Client should not allow a key shorter than 2048 in the config
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_RPUsesShortPrivateKey() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the Social Client logs saying the JWS couldn't be extracted because the key used a key size that was too small.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "2048 bits or larger");
        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, "SignRS256EncryptShortRS256", expectations);

    }

    /**
     * The Social Client should not use a public key to decrypt - should use the private key
     * The Social Client fails to find the key - there is a key with a name that matches the keyManagementKeyAlias, but the key is
     * a public key, not a private key
     */
    @Test
    public void Social_EncryptionTests_RPUsesPublicKey() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the Social Client logs saying the JWS couldn't be extracted because a key was missing.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "rs256" + ".*" + "not present");
        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, "SignRS256EncryptPublicRS256", expectations);

    }

    // can't disable ssl communication between servers with social, so the social client and the server are using the same ssl config, so, the next couple tests are not valid

    // test case for Social Client client config omitting the sslRef and oidc client uses the server-wide sslconfig is in its own unique class as SSL reconfigs can be problematic
    // All of the other tests in this class have a server-wide ssl config that does NOT contain the signing and encrypting keys.
    // The sslRef in the Social Client clients provide the key and trust that the tests need
    // the test in the xxx class does not specify an sslRef in the Social Client client config, but the server-wide ssl config will provide the key and trust stores that
    //    /**
    //     * Test that the Social Client will fall back to using the server-wide ssl config if an sslRef is missing from the
    //     * openidConnectClient
    //     * config
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Social_EncryptionTests_RPMissingSSLRef_serverWideSSLConfigDoesNotHaveKeyMgmtKeyAlias() throws Exception {
    //
    //        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
    //        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the Social Client logs saying the JWS couldn't be extracted because a key was missing.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + "not present");
    //        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, "RP_sslRefOmitted", expectations);
    //
    //    }
    //
    // product code will fail on the missing keyManagementKeyAlias before hitting an issue with the missing trustStoreRef
    //public void Social_EncryptionTests_RPMissingSSLRef_trustStoreRefAlsoOmitted() throws Exception {
    //
    //    /**
    //     * Test that the Social Client will fall back to using the key and trust stores in the sslRef if the trustStoreRef is missing
    //     * from the
    //     * openidConnectClient config
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void Social_EncryptionTests_RPtrustStoreRefOmitted() throws Exception {
    //
    //        List<validationData> expectations = validationTools.add401Responses(SocialConstants.PERFORM_SOCIAL_LOGIN);
    //        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the Social Client logs saying a signing key was not found.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
    //        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Didn't find expected error message in the Social Client logs for failure to validate the ID token.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
    //        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, "RP_trustStoreRefOmitted", expectations);
    //
    //    }

    /**
     * Test that the Social Client detects that the JWE is invalid as it has too many parts (6) (one of which is completely
     * invalid)
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_JWETooManyParts() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createGenericRS256JWE() + "." + badTokenSegment);

        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), getInvalidNumberOfPartsExpectations(), parms);

    }

    /**
     * Test that the Social Client detects that the JWE is invalid as it has too few parts - the token only has 4 parts.
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_JWETooFewParts() throws Exception {

        String jwtToken = createGenericRS256JWE();

        String badJweToken = jwtToken.substring(0, jwtToken.lastIndexOf(".") - 1);
        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", badJweToken);

        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), getInvalidNumberOfPartsExpectations(), parms);

    }

    /**
     * Test that the Social Client detects that the JWE is invalid - Part 1 is not valid
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_JWE_Part1_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(1));

        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), getInvalidFormatExpectations(), parms);

    }

    /**
     * Test that the Social Client detects that the JWE is invalid - Part 2 is not valid
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_JWE_Part2_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(2));

        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), getInvalidFormatExpectations(), parms);

    }

    /**
     * Test that the Social Client detects that the JWE is invalid - Part 3 is not valid
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_JWE_Par3_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(3));

        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), getInvalidFormatExpectations(), parms);

    }

    /**
     * Test that the Social Client detects that the JWE is invalid - Part 4 is not valid
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_JWE_Part4_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(4));

        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), getInvalidFormatExpectations(), parms);

    }

    /**
     * Test that the Social Client detects that the JWE is invalid - Part 5 is not valid
     *
     * @throws Exception
     */
    @Test
    public void Social_EncryptionTests_JWE_Part5_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(5));

        genericEncryptTest(SocialConstants.SIGALG_RS256, setBuilderName(SocialConstants.SIGALG_RS256), SocialConstants.SIGALG_RS256, setAppName(SocialConstants.SIGALG_RS256), getInvalidFormatExpectations(), parms);

    }
}
