/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.client.fat.CommonTests.GenericOidcClientTests;
import com.meterware.httpunit.WebConversation;

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
 * The RP config will specify the test tooling app instead of the standard token endpoint.
 * The test tooling app will return the saved JWT token as the access_token and id_token.
 *
 * This allows us to test that the RP can handle a token signed with signature algorithms that
 * our OP does not support.
 *
 **/

@Mode(TestMode.FULL)
public class OidcClientEncryptionTests extends CommonTest {

    public static Class<?> thisClass = GenericOidcClientTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.LOGIN_USER;
    protected static String hostName = "localhost";
    public static final String MSG_USER_NOT_IN_REG = "CWWKS1106A";
    public static final JwtTokenBuilderUtils tokenBuilderHelpers = new JwtTokenBuilderUtils();

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcClientEncryptionTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.opWithStub", "op_server_encrypt.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_withOpStub_encrypt.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                                   Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);
        testSettings.setTokenEndpt(testSettings.getTokenEndpt()
                        .replace("oidc/endpoint/OidcConfigSample/token", "TokenEndpointServlet")
                        .replace("oidc/providers/OidcConfigSample/token", "TokenEndpointServlet") + "/saveToken");

    }

    /**
     * All of the test cases in this class perform the same steps - the only real differences are:
     * 1) which builder will be used to create a JWT Token,
     * 2) which test app they'll use (the test app dictates which RP config will be used),
     * 3) and whether to expect a success or failure (failure indicated by a mis-mismatch in the algorithm used by the
     * builder and RP
     * Passing in the builder and RP signature and encryption algorithms can tell a common method all it needs to know.
     *
     * This method invokes the test tooling app "TokenEndpointServlet/saveToken" to create the JWT token using the builder
     * specified.
     * It then sets up the expectations for the current instance - if the builder and RP signature and encryption algorithms match, expectations
     * are set to validate token content, if they do not match expectations are set for a 401 status code and server side error
     * messages indicating the mis-match.
     * Finally, this method tries to invoke the test app protected by the RP. The RP config will end up using the test tooling
     * app's token endpoint and use the "JWT Builder" generated token.
     *
     * @param encryptAlgForBuilder
     *            - the signature algorithm that the builder will use - the builder configs are named such that they start with
     *            the signature algorithm (ie: HS256Builder)
     * @param decryptAlgForRP
     *            - the signature algorithm that the RP will use - the test app names match filters in the RP config that cause
     *            the RP config to be used with the specified signature algorithm (ie: formlogin/simple/HS256)
     * @throws Exception
     */
    //    public void genericEncryptTest(String sigAlgForBuilder, String sigAlgForRP, String encryptAlgForBuilder, String encryptAlgForRP) throws Exception {
    public void genericEncryptTest(String encryptAlgForBuilder, String decryptAlgForRP) throws Exception {
        genericEncryptTest(encryptAlgForBuilder, setBuilderName(encryptAlgForBuilder), decryptAlgForRP, setAppName(decryptAlgForRP), null);
    }

    public void genericEncryptTest(String encryptAlgForBuilder, String decryptAlgForRP, List<endpointSettings> parms) throws Exception {
        genericEncryptTest(encryptAlgForBuilder, setBuilderName(encryptAlgForBuilder), decryptAlgForRP, setAppName(decryptAlgForRP), null, parms);
    }

    public void genericEncryptTest(String encryptAlgForBuilder, String builderId, String decryptAlgForRP, String appName, List<validationData> expectations) throws Exception {

        genericEncryptTest(encryptAlgForBuilder, builderId, decryptAlgForRP, appName, null, null);
    }

    public void genericEncryptTest(String encryptAlgForBuilder, String builderId, String decryptAlgForRP, String appName, List<validationData> expectations,
                                   List<endpointSettings> parms) throws Exception {

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        //updatedTestSettings.addRequestParms();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        updatedTestSettings.setSignatureAlg(encryptAlgForBuilder);

        if (expectations == null) {
            // if the signature alg in the build matches what's in the RP, the test should succeed - validate status codes and token content
            if (encryptAlgForBuilder.equals(decryptAlgForRP)) {
                expectations = vData.addSuccessStatusCodes(null);
                expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
                expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                    "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
                expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
                expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
                expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
            } else {
                // create negative expectations when signature algorithms don't match
                expectations = validationTools.add401Responses(Constants.LOGIN_USER);
                // TODO - update for encrypt/decrypt failures.
                //            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch", null, MessageConstants.CWWKS1761E_SIG_ALG_MISMATCH + ".*client01.*" + sigAlgForRP + ".*" + sigAlgForBuilder + ".*");
                //            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a signature mismatch", null, MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
            }
        }

        parms = eSettings.addEndpointSettingsIfNotNull(parms, "builderId", builderId);

        // Invoke the test TokenEndpoint stub.  It will invoke the Jwt Builder to create a JWT Token (using the builder specified in the builderId passed in via parms
        // The TokenEndpoint stub will save that token and it will be returned when the RP uses it's TokenEnpdointUrl specified in it's config
        //  (That url is:  http://localhost:${bvt.prop.security_1_HTTP_default}/TokenEndpointServlet/getToken)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);

        // we created and saved a jwt for our test tooling token endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting a jwt from the OP, it will use a
        // token endpoint pointing to the test tooling app that will return the jwt previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    public List<validationData> setMisMatchSignatureExpectations() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // add specific error messages to search for
        return expectations;

    }

    /******************************* tests *******************************/
    /************** jwt builder/rp using the same algorithm **************/

    /**
     * Test shows that the RP can consume a JWT signed with RS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenRS256_RPDecryptRS256() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RP can consume a JWT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenRS384_RPDecryptRS384() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the RP can consume a JWT signed with RS512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenRS512_RPDecryptRS512() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /*********** jwt builder/rp using the different algorithm ************/
    /* Show that we can't validate the token if the signature algorithms */
    /* don't match */
    /*********************************************************************/
    /**
     * Test shows that the RP can NOT consume a JWT that is NOT encrypted with RS256
     *
     * @throws Exception
     */
    // TODO - update method called or maybe just exception
    @Test
    public void OidcClientEncryptionTests_EncryptTokenNotWithRS256_RPDecryptRS256() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_RS256;
        for (String builderEncryptAlg : Constants.ALL_TEST_RSSIGALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but RS256 for decrypt
                genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenNotWithRS384_RPDecryptRS384() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_RS384;
        for (String builderEncryptAlg : Constants.ALL_TEST_RSSIGALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but RS384 for decrypt
                genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenNotWithRS512_RPDecryptRS512() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_RS512;
        for (String builderEncryptAlg : Constants.ALL_TEST_RSSIGALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but RS512 for decrypt
                genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
            }
        }

    }

    /*********** jwt builder/rp mix signature algs with differnt encryption algorithms ************/
    /* Show that we can't validate the token if the signature algorithms */
    /* don't match */
    /*********************************************************************/
    // create builder name with the same alg for both signing and encrypting
    public String setBuilderName(String alg) {
        return setBuilderName(alg, alg);
    }

    public String setBuilderName(String sigAlg, String encryptAlg) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName,
                 "********************* Testing with Jwt builder - Signing with " + sigAlg + " and encrypting using: " + encryptAlg + " ********************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return "Sign" + sigAlg + "Encrypt" + encryptAlg + "Builder";
    }

    // create app name with the same alg for both verifying and decrypting
    public String setAppName(String alg) {
        return setAppName(alg, alg);
    }

    public String setAppName(String sigAlg, String encryptAlg) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName,
                 "************************ Testing with RP - Verifying with " + sigAlg + " and decrypting using: " + encryptAlg + " ************************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return "Sign" + sigAlg + "Encrypt" + encryptAlg;
    }

    /**
     * Test shows that the RP can consume a JWT that is encrypted/decrypted with RS256 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with RS256, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_SignWithVariousAlgs_EncryptWithRS256_DecryptWithRS256() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_RS256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg),
                                   setMisMatchSignatureExpectations());
            }
        }

    }

    /**
     * Test shows that the RP can consume a JWT that is encrypted/decrypted with RS384 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with RS384, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_SignWithVariousAlgs_EncryptWithRS384_DecryptWithRS384() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_RS384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg),
                                   setMisMatchSignatureExpectations());
            }
        }
    }

    /**
     * Test shows that the RP can consume a JWT that is encrypted/decrypted with RS512 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with RS512, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_SignWithVariousAlgs_EncryptWithRS512_DecryptWithRS512() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_RS512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg),
                                   setMisMatchSignatureExpectations());
            }
        }
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptValid_DecryptInvalidKeyManagementKeyAlias() throws Exception {
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        genericEncryptTest(rpEncryptAlg, setBuilderName(rpEncryptAlg), rpDecryptAlg, "InvalidKeyManagementKeyAlias", expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptValid_DecryptNonExistantKeyManagementKeyAlias() throws Exception {
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        genericEncryptTest(rpEncryptAlg, setBuilderName(rpEncryptAlg), rpDecryptAlg, "NonExistantKeyManagementKeyAlias", expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptValid_DecryptOmittedKeyManagementKeyAlias() throws Exception {
        String rpEncryptAlg = Constants.SIGALG_RS256;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        genericEncryptTest(rpEncryptAlg, setBuilderName(rpEncryptAlg), null, "OmittedKeyManagementKeyAlias", expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS256() throws Exception {
        String signAlg = Constants.SIGALG_RS256;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        // sign with RS256, but do not encrypt - RP has sign RS256 and decrypt RS256
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS384() throws Exception {
        String signAlg = Constants.SIGALG_RS384;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS384;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        // sign with RS384, but do not encrypt - RP has sign RS384 and decrypt RS384
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS512() throws Exception {
        String signAlg = Constants.SIGALG_RS512;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS512;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        // sign with RS512, but do not encrypt - RP has sign RS152 and decrypt RS512
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithRS256_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS256;
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        // sign and encrypt with RS256, but do not decrypt
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithRS384_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS384;
        String rpEncryptAlg = Constants.SIGALG_RS384;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        // sign and encrypt with RS384, but do not decrypt
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithRS512_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS512;
        String rpEncryptAlg = Constants.SIGALG_RS512;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        // TODO - probably need to build specific error messages to check for
        // sign and encrypt with RS512, but do not decrypt
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), expectations);
    }

    /**
     * The RP server.xml has a config that specifies a key management key alias using an RS256 Cert - this test ensures that
     * after building a jwt that is encrypted with the matching public key, but using "A192GCM" as the contentEncryptionAlg,
     * the RP can consume the token with the matching private key.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_consumeTokenThatWasEncryptedUsingOtherContentEncryptionAlg() throws Exception {

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, JwtConstants.PARAM_CONTENT_ENCRYPT_ALG, JwtConstants.CONTENT_ENCRYPT_ALG_192);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, JwtConstants.PARAM_ENCRYPT_KEY,
                                                       JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), JwtConstants.SIGALG_RS256));

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
    }

    /**
     * Show that the RP can accept a token built specyfying RSA_OAEP_256 (instead of RSA-OAEP) in the KeyManagementKeyAlgorithm key.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_consumeTokenThatWasEncryptedUsingOtherKeyManagementKeyAlg() throws Exception {

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, JwtConstants.PARAM_KEY_MGMT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_256);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, JwtConstants.PARAM_ENCRYPT_KEY,
                                                       JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), JwtConstants.SIGALG_RS256));

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
    }

    @Test
    public void OidcClientEncryptionTests_TypeNotJose() throws Exception {

        // We're going to use a test JWT token builder to create a token that has "notJOSE" in the JWE header type field
        // the Liberty builder won't allow us to update that field
        String jwtToken = tokenBuilderHelpers
                        .buildJWETokenWithAltHeader(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)),
                                                    "notJOSE", "jwt");

        // the built token will be pass to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
//        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
//        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    @Test
    public void OidcClientEncryptionTests_JWEContentTypeNotJwt() throws Exception {

        // build a jwe token that has "cty" set to "not_jwt" instead of "jwt".  The token will be encrypted with RS256 and signed with HS256.
        String jwtToken = tokenBuilderHelpers
                        .buildJWETokenWithAltHeader(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)),
                                                    "JOSE", "not_jwt");

        // the built token will be pass to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
//        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
//        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    @Test
    public void OidcClientEncryptionTests_simpleJsonPayload() throws Exception {

        // build a jwt token whose payload contains only json data - make sure that we do not allow this format (it's not supported at this time)
        String jwtToken = tokenBuilderHelpers
                        .buildAlternatePayloadJWEToken(JwtKeyTools
                                        .getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)));

        // the built token will be pass to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
//        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
//        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    @Test
    public void OidcClientEncryptionTests_RPUsesShortPrivateKey() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "SignRS256EncryptShortRS256", expectations);

    }

    @Test
    public void OidcClientEncryptionTests_RPUsesPublicKey() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "SignRS256EncryptPublicRS256", expectations);

    }

    // test case for RP client config omitting the sslRef and oidc client uses the server-wide sslconfig is in its own unique class as SSL reconfigs can be problematic
    // All of the other tests in this class have a server-wide ssl config that does NOT contain the signing and encrypting keys.
    // The sslRef in the RP clients provide the key and trust that the tests need
    // the test in the xxx class does not specify an sslRef in the RP client config, but the server-wide ssl config will provide the key and trust stores that
    /**
     * Test that the RP will fall back to using the server-wide ssl config if an sslRef is missing from the openidConnectClient config
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_RPMissingSSLRef_serverWideSSLConfigDoesNotHaveKeyMgmtKeyAlias() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "RP_sslRefOmitted", expectations);

    }

    // product code will fail on the missing keyManagementKeyAlias before hitting an issue with the missing trustStoreRef
    //public void OidcClientEncryptionTests_RPMissingSSLRef_trustStoreRefAlsoOmitted() throws Exception {

    /**
     * Test that the RP will fall back to using the key and trust stores in the sslRef if the trustStoreRef is missing from the openidConnectClient config
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_RPtrustStoreRefOmitted() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "RP_trustStoreRefOmitted", expectations);

    }
}
