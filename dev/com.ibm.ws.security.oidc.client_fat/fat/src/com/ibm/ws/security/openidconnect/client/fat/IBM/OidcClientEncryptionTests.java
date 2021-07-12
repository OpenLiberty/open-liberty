/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

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
 * The RP config will specify the test tooling app instead of the standard token endpoint.
 * The test tooling app will return the saved JWT token as the access_token and id_token.
 *
 * This allows us to test that the RP can handle a token signed with signature algorithms that
 * our OP does not support.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcClientEncryptionTests extends CommonTest {

    public static Class<?> thisClass = OidcClientEncryptionTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.LOGIN_USER;
    protected static String hostName = "localhost";
    public static final String MSG_USER_NOT_IN_REG = "CWWKS1106A";
    public static final JwtTokenBuilderUtils tokenBuilderHelpers = new JwtTokenBuilderUtils();
    public static final String badTokenSegment = "1234567890123456789";

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

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
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.opWithStub", "op_server_encrypt.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_withOpStub_encrypt.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);
        testSettings.setTokenEndpt(testSettings.getTokenEndpt().replace("oidc/endpoint/OidcConfigSample/token", "TokenEndpointServlet").replace("oidc/providers/OidcConfigSample/token", "TokenEndpointServlet") + "/saveToken");

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
     * It then sets up the expectations for the current instance - if the builder and RP signature and encryption algorithms
     * match, expectations
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

        genericEncryptTest(encryptAlgForBuilder, builderId, decryptAlgForRP, appName, expectations, null);
    }

    public void genericEncryptTest(String encryptAlgForBuilder, String builderId, String decryptAlgForRP, String appName, List<validationData> expectations,
            List<endpointSettings> parms) throws Exception {

        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "******** Testing with Jwt builder using encryption algorithm: " + encryptAlgForBuilder + " and RP using encryption algorithm: " + decryptAlgForRP + " ********");
        Log.info(thisClass, _testName, "********************************************************************************************************************");

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        //updatedTestSettings.addRequestParms();
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        updatedTestSettings.setSignatureAlg(encryptAlgForBuilder);

        if (expectations == null) {
            // if the encryption alg in the build matches what's in the RP, the test should succeed - validate status codes and token content
            if (encryptAlgForBuilder.equals(decryptAlgForRP) || Constants.SIGALG_NONE.equals(encryptAlgForBuilder)) {
                expectations = getSuccessfulLoginExpectations(updatedTestSettings);
            } else {
                // create negative expectations when signature algorithms don't match
                expectations = validationTools.add401Responses(Constants.LOGIN_USER);
                if (encryptAlgForBuilder.startsWith("ES") || (decryptAlgForRP.startsWith("ES"))) {
                    if (encryptAlgForBuilder.startsWith("ES") && (decryptAlgForRP.startsWith("ES"))) {
                        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "epk is invalid for");
                    } else {
                        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "InvalidKeyException");
                    }
                } else {
                    expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "AEADBadTagException");
                }
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
     *            - the alg to use to build the appname to call (this will in turn result in the appropriate RP config to be used
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
     *            - the sig alg to use to build the appname to call (this will in turn result in the appropriate RP config to be
     *            used to verify and decrypt the token)
     * @param decryptAlg
     *            - the decrypt alg to use to build the appname to call (this will in turn result in the appropriate RP config to
     *            be used to verify and decrypt the token)
     * @return - the appname to call
     */
    public String setAppName(String sigAlg, String decryptAlg) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "************************ Testing with RP - Verifying with " + sigAlg + " and decrypting using: " + decryptAlg + " ************************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return "Sign" + sigAlg + "Encrypt" + decryptAlg;
    }

    public List<validationData> getSuccessfulLoginExpectations(TestSettings updatedTestSettings) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        //        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        return expectations;
    }

    public List<validationData> getMissingDecryptionSettingsExpectations() throws Exception {
        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + MessageConstants.CWWKS6066E_JWE_DECRYPTION_KEY_MISSING);
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
        JWTTokenBuilder builder = tokenBuilderHelpers.populateAlternateJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)));
        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setAlorithmHeaderValue(Constants.SIGALG_RS256);
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
    /************** jwt builder/rp using the same encryption algorithm **************/
    /**
     * Test shows that the RP can consume a JWE encrypted with RS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenRS256_RPDecryptRS256() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RP can consume a JWE encrypted with RS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenRS384_RPDecryptRS384() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the RP can consume a JWE encrypted with RS512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenRS512_RPDecryptRS512() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the RP can consume a JWE encrypted with ES256
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientEncryptionTests_EncryptTokenES256_RPDecryptES256() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(Constants.SIGALG_ES256, Constants.SIGALG_ES256, setParmsForECWorkaround(JwtConstants.SIGALG_ES256));

    }

    /**
     * Test shows that the RP can consume a JWE encrypted with ES384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenES384_RPDecryptES384() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(Constants.SIGALG_ES384, Constants.SIGALG_ES384, setParmsForECWorkaround(Constants.SIGALG_ES384));

    }

    /**
     * Test shows that the RP can consume a JWE encrypted with ES512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenES512_RPDecryptES512() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(Constants.SIGALG_ES512, Constants.SIGALG_ES512, setParmsForECWorkaround(Constants.SIGALG_ES512));

    }

    /*********** jwt builder/rp using the different algorithm ************/
    /* Show that we can't validate the token if the encryption algorithms */
    /* don't match */
    /*********************************************************************/
    /**
     * Test shows that the RP can NOT consume a JWT that is NOT encrypted with RS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenNotWithRS256_RPDecryptRS256() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_RS256;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but RS256 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
                }
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
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but RS384 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
                }
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
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but RS512 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
                }
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT encrypted with RS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenNotWithES256_RPDecryptES256() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_ES256;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but ES256 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
                }
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenNotWithES384_RPDecryptES384() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_ES384;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but ES384 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
                }
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_EncryptTokenNotWithES512_RPDecryptES512() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_ES512;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RP specifies original alg for sign, but ES512 for decrypt
                // TODO when issue 17485 is completed, remove setting/passing parms
                if (builderEncryptAlg.startsWith("ES")) {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null, setParmsForECWorkaround(builderEncryptAlg));
                } else {
                    genericEncryptTest(builderEncryptAlg, setBuilderName(builderEncryptAlg), rpDecryptAlg, setAppName(builderEncryptAlg, rpDecryptAlg), null);
                }
            }
        }

    }

    /*********** jwt builder/rp mix signature algs with differnt encryption algorithms ************/
    /* Show that we can't validate the token if the signature and encryption are not the same */
    /**********************************************************************************************/
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
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null);
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
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null);
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
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null);
            }
        }
    }

    /**
     * Test shows that the RP can consume a JWT that is encrypted/decrypted with ES256 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with ES256, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_SignWithVariousAlgs_EncryptWithES256_DecryptWithES256() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_ES256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null, setParmsForECWorkaround(encryptDecryptAlg));
            }
        }

    }

    /**
     * Test shows that the RP can consume a JWT that is encrypted/decrypted with ES384 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with ES384, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_SignWithVariousAlgs_EncryptWithES384_DecryptWithES384() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_ES384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null, setParmsForECWorkaround(encryptDecryptAlg));
            }
        }
    }

    /**
     * Test shows that the RP can consume a JWT that is encrypted/decrypted with ES512 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with ES512, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_SignWithVariousAlgs_EncryptWithES512_DecryptWithES512() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_ES512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
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
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptValid_DecryptInvalidKeyManagementKeyAlias() throws Exception {
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "ClassCastException");
        genericEncryptTest(rpEncryptAlg, setBuilderName(rpEncryptAlg), rpDecryptAlg, "InvalidKeyManagementKeyAlias", expectations);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptValid_DecryptNonExistantKeyManagementKeyAlias() throws Exception {
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "nonExistantKeyManagementKeyAlias" + ".*" + "not present");
        genericEncryptTest(rpEncryptAlg, setBuilderName(rpEncryptAlg), rpDecryptAlg, "NonExistantKeyManagementKeyAlias", expectations);
    }

    //    This test is the same as all of the OidcClientEncryptionTests_SignWithValidAlg_EncryptWith<*>_DoNotDecrypt tests
    //    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptValid_DecryptOmittedKeyManagementKeyAlias() throws Exception {

    /************** Don't encrypt the token, RP decrypts *************/
    /* Don't encrypt the token, but the RP config "expects" and */
    /* encrypted token - show that we will still consume it */
    /*****************************************************************/
    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS256() throws Exception {
        String signAlg = Constants.SIGALG_RS256;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS384() throws Exception {
        String signAlg = Constants.SIGALG_RS384;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS384;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS512() throws Exception {
        String signAlg = Constants.SIGALG_RS512;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS512;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES256() throws Exception {
        String signAlg = Constants.SIGALG_ES256;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_ES256;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES384() throws Exception {
        String signAlg = Constants.SIGALG_ES384;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_ES384;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES512() throws Exception {
        String signAlg = Constants.SIGALG_ES512;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_ES512;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    /************** Encrypt the token, RP does not decrypt *************/
    /* Encrypt the token, but the RP config does not have decryption */
    /* enabled - show that we fail with the appropriate errors */
    /*******************************************************************/
    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithRS256_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS256;
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsExpectations());

    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithRS384_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS384;
        String rpEncryptAlg = Constants.SIGALG_RS384;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsExpectations());

    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithRS512_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS512;
        String rpEncryptAlg = Constants.SIGALG_RS512;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsExpectations());
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithES256_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_ES256;
        String rpEncryptAlg = Constants.SIGALG_ES256;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsExpectations(), setParmsForECWorkaround(rpEncryptAlg));
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithES384_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_ES384;
        String rpEncryptAlg = Constants.SIGALG_ES384;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsExpectations(), setParmsForECWorkaround(rpEncryptAlg));
    }

    @Test
    public void OidcClientEncryptionTests_SignWithValidAlg_EncryptWithES512_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_ES512;
        String rpEncryptAlg = Constants.SIGALG_ES512;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsExpectations(), setParmsForECWorkaround(rpEncryptAlg));
    }

    /*************** Various JWE header content tests ***************/
    /* Show that we can handle non-default values where allowed and */
    /* fail appropriately when we do not allow non0default values */
    /****************************************************************/
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
        parms = eSettings.addEndpointSettingsIfNotNull(parms, JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), JwtConstants.SIGALG_RS256));

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
    }

    /**
     * Show that the RP can accept a token built specifying RSA_OAEP_256 (instead of RSA-OAEP) in the KeyManagementKeyAlgorithm
     * key.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_consumeTokenThatWasEncryptedUsingOtherKeyManagementKeyAlg() throws Exception {

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, JwtConstants.PARAM_KEY_MGMT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_256);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), JwtConstants.SIGALG_RS256));

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
    }

    /**
     * Show that the RP can accept a token with a JWE header "typ" that does not contain JOSE.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWETypeNotJose() throws Exception {

        // We're going to use a test JWT token builder to create a token that has "notJOSE" in the JWE header type field
        // the Liberty builder won't allow us to update that field, so, we need to peice a token together
        JWTTokenBuilder builder = tokenBuilderHelpers.populateAlternateJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)));
        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setAlorithmHeaderValue(Constants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setClaim("token_src", "testcase builder");
        // calling buildJWE will override the header contents
        String jwtToken = builder.buildJWE("notJOSE", "jwt");

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), null, parms);
    }

    @Test
    public void OidcClientEncryptionTests_JWEContentTypeNotJwt() throws Exception {

        // We're going to use a test JWT token builder to create a token that has "not_jwt" in the JWE header content type field
        // the Liberty builder won't allow us to update that field, so, we need to peice a token together
        JWTTokenBuilder builder = tokenBuilderHelpers.populateAlternateJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)));
        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setAlorithmHeaderValue(Constants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setClaim("token_src", "testcase builder");
        // calling buildJWE will override the header contents
        String jwtToken = builder.buildJWE("JOSE", "not_jwt");

        // the built token will be pass to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs for a bad 'cty' in the ID token.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + MessageConstants.CWWKS6057E_CTY_NOT_JWT_FOR_NESTED_JWS);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    /*************** Misc tests ***************/
    /**
     * Include a simple Json payload instead of a JWS - make sure that we fail with an appropriate message
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_simpleJsonPayload() throws Exception {

        // build a jwt token whose payload contains only json data - make sure that we do not allow this format (it's not supported at this time)
        String jwtToken = tokenBuilderHelpers.buildAlternatePayloadJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)));

        // the built token will be pass to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs saying the payload of the JWE ID token wasn't a JWS.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6065E_NESTED_JWS_REQUIRED_BUT_NOT_FOUND);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    /**
     * The RP should not allow a key shorter than 2048 in the config
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_RPUsesShortPrivateKey() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs saying the JWS couldn't be extracted because the key used a key size that was too small.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "2048 bits or larger");
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "SignRS256EncryptShortRS256", expectations);

    }

    /**
     * The RP should not use a public key to decrypt - should use the private key
     * The RP fails to find the key - there is a key with a name that matches the keyManagementKeyAlias, but the key is
     * a public key, not a private key
     */
    @Test
    public void OidcClientEncryptionTests_RPUsesPublicKey() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs saying the JWS couldn't be extracted because a key was missing.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE + ".*" + "rs256" + ".*" + "not present");
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "SignRS256EncryptPublicRS256", expectations);

    }

    // test case for RP client config omitting the sslRef and oidc client uses the server-wide sslconfig is in its own unique class as SSL reconfigs can be problematic
    // All of the other tests in this class have a server-wide ssl config that does NOT contain the signing and encrypting keys.
    // The sslRef in the RP clients provide the key and trust that the tests need
    // the test in the xxx class does not specify an sslRef in the RP client config, but the server-wide ssl config will provide the key and trust stores that
    /**
     * Test that the RP will fall back to using the server-wide ssl config if an sslRef is missing from the openidConnectClient
     * config
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_RPMissingSSLRef_serverWideSSLConfigDoesNotHaveKeyMgmtKeyAlias() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs saying the JWS couldn't be extracted because a key was missing.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*" + "not present");
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "RP_sslRefOmitted", expectations);

    }

    // product code will fail on the missing keyManagementKeyAlias before hitting an issue with the missing trustStoreRef
    //public void OidcClientEncryptionTests_RPMissingSSLRef_trustStoreRefAlsoOmitted() throws Exception {

    /**
     * Test that the RP will fall back to using the key and trust stores in the sslRef if the trustStoreRef is missing from the
     * openidConnectClient config
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_RPtrustStoreRefOmitted() throws Exception {

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs saying a signing key was not found.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs for failure to validate the ID token.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "RP_trustStoreRefOmitted", expectations);

    }

    /**
     * Test that the RP detects that the JWE is invalid as it has too many parts (6) (one of which is completely invalid)
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWETooManyParts() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createGenericRS256JWE() + "." + badTokenSegment);

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*but was 6.*");
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    /**
     * Test that the RP detects that the JWE is invalid as it has too few parts - the token only has 4 parts.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWETooFewParts() throws Exception {

        String jwtToken = createGenericRS256JWE();

        String badJweToken = jwtToken.substring(0, jwtToken.lastIndexOf(".") - 1);
        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", badJweToken);

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN + ".*but was 4.*");
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    /**
     * Test that the RP detects that the JWE is invalid - Part 1 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Part1_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(1));

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    /**
     * Test that the RP detects that the JWE is invalid - Part 2 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Part2_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(2));

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    /**
     * Test that the RP detects that the JWE is invalid - Part 3 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Par3_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(3));

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    /**
     * Test that the RP detects that the JWE is invalid - Part 4 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Part4_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(4));

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }

    /**
     * Test that the RP detects that the JWE is invalid - Part 5 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Part5_isInvalid() throws Exception {

        // the built token will be passed to the test app via the overrideToken parm
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", createTokenWithBadElement(5));

        List<validationData> expectations = validationTools.add401Responses(Constants.LOGIN_USER);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Didn't find expected error message in the RP logs saying that the passed JWE had the wrong number of parts.", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, setAppName(Constants.SIGALG_RS256), expectations, parms);

    }
}
