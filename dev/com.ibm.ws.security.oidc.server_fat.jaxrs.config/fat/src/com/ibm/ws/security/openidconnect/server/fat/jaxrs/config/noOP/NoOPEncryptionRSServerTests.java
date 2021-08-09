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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.actions.JwtTokenActions;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MangleJWTTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Class for RS Encryption tests.
 * Test class starts a server called OPServer, but that server does provide OP services.
 * It is provided to act as a builder to create JWT's to provide to the RS.
 *
 * @author chrisc
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class NoOPEncryptionRSServerTests extends MangleJWTTestTools {

    private static final Class<?> thisClass = NoOPEncryptionRSServerTests.class;
    protected static final String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static final String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";
    protected static final String badTokenSegment = "1234567890123456789";

    private static final JwtTokenActions actions = new JwtTokenActions();
    public static final JwtTokenBuilderUtils tokenBuilderHelpers = new JwtTokenBuilderUtils();

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add("jwtbuilder");

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps2 = new ArrayList<String>();

        TestServer.addTestApp(extraApps2, null, Constants.OP_TAI_APP, Constants.OIDC_OP);

        // set token and cert types - hard code JWT and then select the token type to use...
        String tokenType = Constants.JWT_TOKEN;
        //		String certType = rsTools.chooseCertType(tokenType) ;
        String certType = Constants.X509_CERT;

        testSettings = new TestSettings();

        testOPServer = commonSetUp(OPServerName, "server_encryption.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "ECDH-ES");
        testOPServer.setRestoreServerBetweenTests(false);

        genericTestServer = commonSetUp(RSServerName, "server_encryption.xml", Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, extraMsgs2, null, Constants.OIDC_OP, true, true, tokenType, certType);

        // We use a variable insert for the validationMethod config attribute which the config evaluator will think is invalid
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "validationMethod");
        SecurityFatHttpUtils.saveServerPorts(genericTestServer.getServer(), Constants.BVT_SERVER_3_PORT_NAME_ROOT);
        genericTestServer.setRestoreServerBetweenTests(false);

        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        flowType = Constants.WEB_CLIENT_FLOW;
        goodActions = RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS;

        // set RS protected resource to point to second server.
        testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/oauth2tai/snoop");
        testSettings = helpers.fixProviderInUrls(testSettings, "providers", "endpoint");
    }

    /**
     * Create a JWT using the builder specified
     *
     * @param builderId
     *            - builder to use to create a JWT
     * @return - built JWT
     * @throws Exception
     */
    private String createTokenWithSubject(String builderId) throws Exception {
        return createTokenWithSubject(builderId, null);
    }

    /**
     * Create a JWT using the builder and extra parms specified. Include passing the claim sub
     *
     * @param builderId
     *            - builder to use to create a JWT
     * @param parms
     *            - additional parameters to pass to the builder
     * @return - built JWT
     * @throws Exception
     */
    private String createTokenWithSubject(String builderId, List<NameValuePair> parms) throws Exception {

        if (parms == null) {
            parms = new ArrayList<NameValuePair>();
        }
        parms.add(new NameValuePair("token_src", "product builder"));
        parms.add(new NameValuePair("sub", "testuser"));
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, testOPServer.getServer(), builderId, parms);
        Log.info(thisClass, _testName, jwtToken);

        return jwtToken;
    }

    /**
     * TODO when issue 17485 is completed, remove setting/passing parms and update the builder configs that encrypt with ES algs
     * with keyManagementKeyAlgorithm set to ECDH-ES
     *
     * Update the keyManagementKeyAlgorithm to be used in creating the JWT. This allows us to create Elliptic Curve encrypted
     * tokens
     *
     * @param alg
     *            - the algorithm to set
     * @return - parms to pass to the builder servlet
     * @throws Exception
     */
    public List<NameValuePair> setParmsForECWorkaround(String alg) throws Exception {

        testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "ECDH-ES");

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair(JwtConstants.PARAM_KEY_MGMT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_ES));
        parms.add(new NameValuePair(JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), alg)));
        return parms;
    }

    /**
     * Create the standard error message list for cases where token is encrypted, but the RS config does not specify config
     * information for decrypting
     *
     * @return - a list of messages to search for in the server side log
     * @throws Exception
     */
    public String[] getMissingDecryptionSettingsMessages() throws Exception {

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE, MessageConstants.CWWKS6066E_JWE_DECRYPTION_KEY_MISSING };
        return msgs;
    }

    /**
     * Generic method to create a JWT and use it to try to access a protected app
     * (no extra messages need to be searched for in the server side logs)
     * (build the builderId based on the passed encryptAlgForBuilder)
     * (build the aooName based on the passed decryptAlgForRS)
     * (no extra parms need to be passed)
     *
     * @param encryptAlgForBuilder
     *            - the encryption algorithm used by the builder
     * @param decryptAlgForRS
     *            - the encryption algorithm used by the RS server
     * @throws Exception
     */
    public void genericEncryptTest(String encryptAlgForBuilder, String decryptAlgForRS) throws Exception {
        genericEncryptTest(encryptAlgForBuilder, setBuilderName(encryptAlgForBuilder), decryptAlgForRS, setAppName(decryptAlgForRS), null);
    }

    /**
     * Generic method to create a JWT and use it to try to access a protected app
     * (no extra messages need to be searched for in the server side logs)
     * (build the builderId based on the passed encryptAlgForBuilder)
     * (build the aooName based on the passed decryptAlgForRS)
     *
     * @param encryptAlgForBuilder
     *            - the encryption algorithm used by the builder
     * @param decryptAlgForRS
     *            - the encryption algorithm used by the RS server
     * @param parms
     *            - additional parms to use when building the token
     * @throws Exception
     */
    public void genericEncryptTest(String encryptAlgForBuilder, String decryptAlgForRS, List<NameValuePair> parms) throws Exception {
        genericEncryptTest(encryptAlgForBuilder, setBuilderName(encryptAlgForBuilder), decryptAlgForRS, setAppName(decryptAlgForRS), null, parms);
    }

    /**
     * Generic method to create a JWT and use it to try to access a protected app
     * (no extra parms are needed)
     *
     * @param encryptAlgForBuilder
     *            - the encryption algorithm used by the builder
     * @param builderId
     *            - the builderId to use to build the JWT
     * @param decryptAlgForRS
     *            - the encryption algorithm used by the RS server
     * @param appName
     *            - the app to invoke
     * @param msgs
     *            - error messages that should searched for in the server side logs
     * @throws Exception
     */
    public void genericEncryptTest(String encryptAlgForBuilder, String builderId, String decryptAlgForRS, String appName, String[] msgs) throws Exception {

        genericEncryptTest(encryptAlgForBuilder, builderId, decryptAlgForRS, appName, msgs, null);
    }

    /**
     * Generic method to create a JWT and use it to try to access a protected app
     *
     * @param encryptAlgForBuilder
     *            - the encryption algorithm used by the builder
     * @param builderId
     *            - the builderId to use to build the JWT
     * @param decryptAlgForRS
     *            - the encryption algorithm used by the RS server
     * @param appName
     *            - the app to invoke
     * @param msgs
     *            - error messages that should searched for in the server side logs
     * @param parms
     *            - additional parms to use when building the token
     * @throws Exception
     */
    public void genericEncryptTest(String encryptAlgForBuilder, String builderId, String decryptAlgForRS, String appName, String[] msgs,
            List<NameValuePair> parms) throws Exception {

        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "******** Testing with Jwt builder using encryption algorithm: " + encryptAlgForBuilder + " and RS using encryption algorithm: " + decryptAlgForRS + " ********");
        Log.info(thisClass, _testName, "********************************************************************************************************************");

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, appName);
        String jwtToken = createTokenWithSubject(builderId, parms);

        if (msgs == null) {
            if (encryptAlgForBuilder.equals(decryptAlgForRS) || Constants.SIGALG_NONE.equals(encryptAlgForBuilder)) {
                positiveTest(updatedTestSettings, jwtToken);
            } else {
                negativeTest(updatedTestSettings, jwtToken, new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE });
            }
        } else {
            negativeTest(updatedTestSettings, jwtToken, msgs);
        }

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
     *            - the alg to use to build the appname to call (this will in turn result in the appropriate RS config to be used
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
     *            - the sig alg to use to build the appname to call (this will in turn result in the appropriate RS config to be
     *            used to verify and decrypt the token)
     * @param decryptAlg
     *            - the decrypt alg to use to build the appname to call (this will in turn result in the appropriate RS config to
     *            be used to verify and decrypt the token)
     * @return - the appname to call
     */
    public String setAppName(String sigAlg, String decryptAlg) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "************************ Testing with RS - Verifying with " + sigAlg + " and decrypting using: " + decryptAlg + " ************************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return "snoop/Sign" + sigAlg + "Encrypt" + decryptAlg;
    }

    public String createTokenWithBadElement(int badPart) throws Exception {

        String thisMethod = "createTokenWithBadElement";
        String jwtToken = createTokenWithSubject("SignRS256EncryptRS256Builder");
        Log.info(thisClass, thisMethod, jwtToken);
        String[] jwtTokenArray = jwtToken.split("\\.");
        Log.info(thisClass, thisMethod, "size: " + jwtTokenArray.length);
        String badJweToken = "";

        for (int i = 0; i < 5; i++) {
            Log.info(thisClass, thisMethod, "i=" + i);
            Log.info(thisClass, thisMethod, "badJweToken: " + badJweToken);
            Log.info(thisClass, thisMethod, "subString: " + jwtTokenArray[i]);
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
     * Test shows that the RS can consume a JWE encrypted with RS256
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenRS256_RSDecryptRS256() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RS can consume a JWE encrypted with RS384
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenRS384_RSDecryptRS384() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the RS can consume a JWE encrypted with RS512
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenRS512_RSDecryptRS512() throws Exception {

        genericEncryptTest(Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the RS can consume a JWE encrypted with ES256
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenES256_RSDecryptES256() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(Constants.SIGALG_ES256, Constants.SIGALG_ES256, setParmsForECWorkaround(JwtConstants.SIGALG_ES256));

    }

    /**
     * Test shows that the RS can consume a JWE encrypted with ES384
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenES384_RSDecryptES384() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(Constants.SIGALG_ES384, Constants.SIGALG_ES384, setParmsForECWorkaround(Constants.SIGALG_ES384));

    }

    /**
     * Test shows that the RS can consume a JWE encrypted with ES512
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenES512_RSDecryptES512() throws Exception {

        // TODO when issue 17485 is completed, remove setting/passing parms
        genericEncryptTest(Constants.SIGALG_ES512, Constants.SIGALG_ES512, setParmsForECWorkaround(Constants.SIGALG_ES512));

    }

    /*********** jwt builder/rp using the different algorithm ************/
    /* Show that we can't validate the token if the encryption algorithms */
    /* don't match */
    /*********************************************************************/
    /**
     * Test shows that the RS can NOT consume a JWT that is NOT encrypted with RS256
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenNotWithRS256_RSDecryptRS256() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_RS256;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RS specifies original alg for sign, but RS256 for decrypt
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
     * Test shows that the RS can NOT consume a JWT that is NOT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenNotWithRS384_RSDecryptRS384() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_RS384;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RS specifies original alg for sign, but RS384 for decrypt
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
     * Test shows that the RS can NOT consume a JWT that is NOT signed with RS512
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenNotWithRS512_RSDecryptRS512() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_RS512;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RS specifies original alg for sign, but RS512 for decrypt
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
     * Test shows that the RS can NOT consume a JWT that is NOT encrypted with RS256
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenNotWithES256_RSDecryptES256() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_ES256;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RS specifies original alg for sign, but ES256 for decrypt
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
     * Test shows that the RS can NOT consume a JWT that is NOT signed with ES384
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenNotWithES384_RSDecryptES384() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_ES384;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RS specifies original alg for sign, but ES384 for decrypt
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
     * Test shows that the RS can NOT consume a JWT that is NOT signed with ES512
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_EncryptTokenNotWithES512_RSDecryptES512() throws Exception {

        String rpDecryptAlg = Constants.SIGALG_ES512;
        for (String builderEncryptAlg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!rpDecryptAlg.equals(builderEncryptAlg)) {
                //sign and encrypt with the same alg, RS specifies original alg for sign, but ES512 for decrypt
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
     * Test shows that the RS can consume a JWT that is encrypted/decrypted with RS256 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with RS256, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_SignWithVariousAlgs_EncryptWithRS256_DecryptWithRS256() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_RS256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null);
            }
        }

    }

    /**
     * Test shows that the RS can consume a JWT that is encrypted/decrypted with RS384 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with RS384, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_SignWithVariousAlgs_EncryptWithRS384_DecryptWithRS384() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_RS384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null);
            }
        }
    }

    /**
     * Test shows that the RS can consume a JWT that is encrypted/decrypted with RS512 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with RS512, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_SignWithVariousAlgs_EncryptWithRS512_DecryptWithRS512() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_RS512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null);
            }
        }
    }

    /**
     * Test shows that the RS can consume a JWT that is encrypted/decrypted with ES256 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with ES256, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_SignWithVariousAlgs_EncryptWithES256_DecryptWithES256() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_ES256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null, setParmsForECWorkaround(encryptDecryptAlg));
            }
        }

    }

    /**
     * Test shows that the RS can consume a JWT that is encrypted/decrypted with ES384 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with ES384, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_SignWithVariousAlgs_EncryptWithES384_DecryptWithES384() throws Exception {

        String encryptDecryptAlg = Constants.SIGALG_ES384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!encryptDecryptAlg.equals(builderSigAlg)) { // base tests already tested with same sign & encrypt
                genericEncryptTest(encryptDecryptAlg, setBuilderName(builderSigAlg, encryptDecryptAlg), encryptDecryptAlg, setAppName(builderSigAlg, encryptDecryptAlg), null, setParmsForECWorkaround(encryptDecryptAlg));
            }
        }
    }

    /**
     * Test shows that the RS can consume a JWT that is encrypted/decrypted with ES512 and signed with all other supported
     * signature algorithms.
     * This test encrypts/decrypts with ES512, but signs/verifies with all other supported signature algorithms.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_SignWithVariousAlgs_EncryptWithES512_DecryptWithES512() throws Exception {

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
    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptValid_DecryptInvalidKeyManagementKeyAlias() throws Exception {
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE };
        genericEncryptTest(rpEncryptAlg, setBuilderName(rpEncryptAlg), rpDecryptAlg, "snoop/InvalidKeyManagementKeyAlias", msgs);
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptValid_DecryptNonExistantKeyManagementKeyAlias() throws Exception {
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE };
        genericEncryptTest(rpEncryptAlg, setBuilderName(rpEncryptAlg), rpDecryptAlg, "snoop/NonExistantKeyManagementKeyAlias", msgs);
    }

    // This test is the same as all of the NoOPEncryption1ServerTests_SignWithValidAlg_EncryptWith<*>_DoNotDecrypt tests
    //    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptValid_DecryptOmittedKeyManagementKeyAlias() throws Exception {

    /************** Don't encrypt the token, RS decrypts *************/
    /* Don't encrypt the token, but the RS config "expects" and */
    /* encrypted token - show that we will still consume it */
    /*****************************************************************/
    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS256() throws Exception {
        String signAlg = Constants.SIGALG_RS256;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS256;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS384() throws Exception {
        String signAlg = Constants.SIGALG_RS384;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS384;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_DoNotEncrypt_DecryptWithRS512() throws Exception {
        String signAlg = Constants.SIGALG_RS512;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_RS512;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES256() throws Exception {
        String signAlg = Constants.SIGALG_ES256;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_ES256;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES384() throws Exception {
        String signAlg = Constants.SIGALG_ES384;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_ES384;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_DoNotEncrypt_DecryptWithES512() throws Exception {
        String signAlg = Constants.SIGALG_ES512;
        String rpEncryptAlg = Constants.SIGALG_NONE;
        String rpDecryptAlg = Constants.SIGALG_ES512;

        // If the access/ID tokens aren't encrypted, we won't try to decrypt them even if the keyManagementKeyAlias is set
        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), null);
    }

    /************** Encrypt the token, RS does not decrypt *************/

    /* Encrypt the token, but the RS config does not have decryption */
    /* enabled - show that we fail with the appropriate errors */
    /*******************************************************************/
    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptWithRS256_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS256;
        String rpEncryptAlg = Constants.SIGALG_RS256;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsMessages());

    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptWithRS384_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS384;
        String rpEncryptAlg = Constants.SIGALG_RS384;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsMessages());

    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptWithRS512_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_RS512;
        String rpEncryptAlg = Constants.SIGALG_RS512;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsMessages());
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptWithES256_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_ES256;
        String rpEncryptAlg = Constants.SIGALG_ES256;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsMessages(), setParmsForECWorkaround(rpEncryptAlg));
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptWithES384_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_ES384;
        String rpEncryptAlg = Constants.SIGALG_ES384;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsMessages(), setParmsForECWorkaround(rpEncryptAlg));
    }

    @Test
    public void NoOPEncryption1ServerTests_SignWithValidAlg_EncryptWithES512_DoNotDecrypt() throws Exception {
        String signAlg = Constants.SIGALG_ES512;
        String rpEncryptAlg = Constants.SIGALG_ES512;
        String rpDecryptAlg = Constants.SIGALG_NONE;

        genericEncryptTest(rpEncryptAlg, setBuilderName(signAlg, rpEncryptAlg), rpDecryptAlg, setAppName(signAlg, rpDecryptAlg), getMissingDecryptionSettingsMessages(), setParmsForECWorkaround(rpEncryptAlg));
    }

    /*************** Various JWE header content tests ***************/
    /* Show that we can handle non-default values where allowed and */
    /* fail appropriately when we do not allow non0default values */
    /****************************************************************/
    /**
     * The RS server.xml has a config that specifies a key management key alias using an RS256 Cert - this test ensures that
     * after building a jwt that is encrypted with the matching public key, but using "A192GCM" as the contentEncryptionAlg,
     * the RS can consume the token with the matching private key.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_consumeTokenThatWasEncryptedUsingOtherContentEncryptionAlg() throws Exception {

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair(JwtConstants.PARAM_CONTENT_ENCRYPT_ALG, JwtConstants.CONTENT_ENCRYPT_ALG_192));
        parms.add(new NameValuePair(JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), JwtConstants.SIGALG_RS256)));

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
    }

    /**
     * Show that the RS can accept a token built specifying RSA_OAEP_256 (instead of RSA-OAEP) in the KeyManagementKeyAlgorithm
     * key.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_consumeTokenThatWasEncryptedUsingOtherKeyManagementKeyAlg() throws Exception {

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair(JwtConstants.PARAM_KEY_MGMT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_256));
        parms.add(new NameValuePair(JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), JwtConstants.SIGALG_RS256)));

        genericEncryptTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256, parms);
    }

    /**
     * Show that the RS can accept a token with a JWE header "typ" that does not contain JOSE.
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_JWETypeNotJose() throws Exception {

        // We're going to use a test JWT token builder to create a token that has "notJOSE" in the JWE header type field
        // the Liberty builder won't allow us to update that field, so, we need to peice a token together
        JWTTokenBuilder builder = tokenBuilderHelpers.populateAlternateJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)));
        builder.setIssuer("client01");
        builder.setAlorithmHeaderValue(Constants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setClaim("token_src", "testcase builder");
        // calling buildJWE will override the header contents
        String jwtToken = builder.buildJWE("notJOSE", "jwt");

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));
        positiveTest(updatedTestSettings, jwtToken);

    }

    @Test
    public void NoOPEncryption1ServerTests_JWEContentTypeNotJwt() throws Exception {

        // We're going to use a test JWT token builder to create a token that has "not_jwt" in the JWE header content type field
        // the Liberty builder won't allow us to update that field, so, we need to peice a token together
        JWTTokenBuilder builder = tokenBuilderHelpers.populateAlternateJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)));
        builder.setIssuer("client01");
        builder.setAlorithmHeaderValue(Constants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setClaim("token_src", "testcase builder");
        // calling buildJWE will override the header contents
        String jwtToken = builder.buildJWE("JOSE", "not_jwt");

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));
        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE, MessageConstants.CWWKS6057E_CTY_NOT_JWT_FOR_NESTED_JWS };

        negativeTest(updatedTestSettings, jwtToken, msgs);

    }

    /*************** Misc tests ***************/
    /**
     * Include a simple Json payload instead of a JWS - make sure that we fail with an appropriate message
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_simpleJsonPayload() throws Exception {

        List<NameValuePair> extraparms = new ArrayList<NameValuePair>();
        extraparms.add(new NameValuePair("token_src", "alternate JWE builder"));

        // build a jwt token whose payload contains only json data - make sure that we do not allow this format (it's not supported at this time)
        String jwtToken = tokenBuilderHelpers.buildAlternatePayloadJWEToken(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)), extraparms);

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));
        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6065E_NESTED_JWS_REQUIRED_BUT_NOT_FOUND };

        negativeTest(updatedTestSettings, jwtToken, msgs);
    }

    /**
     * The RS should not allow a key shorter than 2048 in the config
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_RSUsesShortPrivateKey() throws Exception {

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE };
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "/snoop/SignRS256EncryptShortRS256", msgs);

    }

    /**
     * The RS should not use a public key to decrypt - should use the private key
     * The RS fails to find the key - there is a key with a name that matches the keyManagementKeyAlias, but the key is
     * a public key, not a private key
     */
    @Test
    public void NoOPEncryption1ServerTests_RSUsesPublicKey() throws Exception {

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE };
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "/snoop/SignRS256EncryptPublicRS256", msgs);

    }

    // test case for RS client config omitting the sslRef and oidc client uses the server-wide sslconfig is in its own unique class as SSL reconfigs can be problematic
    // All of the other tests in this class have a server-wide ssl config that does NOT contain the signing and encrypting keys.
    // The sslRef in the RS clients provide the key and trust that the tests need
    // the test in the xxx class does not specify an sslRef in the RS client config, but the server-wide ssl config will provide the key and trust stores that
    /**
     * Test that the RS will fall back to using the server-wide ssl config if an sslRef is missing from the openidConnectClient
     * config
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_RSMissingSSLRef_serverWideSSLConfigDoesNotHaveKeyMgmtKeyAlias() throws Exception {

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, "not present" };
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "/snoop/RS_sslRefOmitted", msgs);

    }

    // product code will fail on the missing keyManagementKeyAlias before hitting an issue with the missing trustStoreRef
    //public void NoOPEncryption1ServerTests_RSMissingSSLRef_trustStoreRefAlsoOmitted() throws Exception {

    /**
     * Test that the RS will fall back to using the key and trust stores in the sslRef if the trustStoreRef is missing from the
     * openidConnectClient config
     *
     * @throws Exception
     */
    @Test
    public void NoOPEncryption1ServerTests_RStrustStoreRefOmitted() throws Exception {

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND };
        genericEncryptTest(Constants.SIGALG_RS256, setBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, "/snoop/RS_trustStoreRefOmitted", msgs);

    }

    /**
     * Test that the RS detects that the JWE is invalid as it has too many parts (6) (one of which is completely invalid)
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWETooManyParts() throws Exception {

        String jwtToken = createTokenWithSubject("SignRS256EncryptRS256Builder") + "." + badTokenSegment;

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        negativeTest(updatedTestSettings, jwtToken, msgs);

    }

    /**
     * Test that the RS detects that the JWE is invalid as it has too few parts - the token only has 4 parts.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWETooFewParts() throws Exception {

        String jwtToken = createTokenWithSubject("SignRS256EncryptRS256Builder");
        String badJweToken = jwtToken.substring(0, jwtToken.lastIndexOf(".") - 1);

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE };
        negativeTest(updatedTestSettings, badJweToken, msgs);

    }

    /**
     * Test that the RS detects that the JWE is invalid - Part 1 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Part1_isInvalid() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));

        String[] msgs = new String[] { MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED };
        negativeTest(updatedTestSettings, createTokenWithBadElement(1), msgs);

    }

    /**
     * Test that the RS detects that the JWE is invalid - Part 2 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Part2_isInvalid() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE };
        negativeTest(updatedTestSettings, createTokenWithBadElement(2), msgs);

    }

    /**
     * Test that the RS detects that the JWE is invalid - Part 3 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Par3_isInvalid() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE };
        negativeTest(updatedTestSettings, createTokenWithBadElement(3), msgs);

    }

    /**
     * Test that the RS detects that the JWE is invalid - Part 4 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Part4_isInvalid() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE };
        negativeTest(updatedTestSettings, createTokenWithBadElement(4), msgs);

    }

    /**
     * Test that the RS detects that the JWE is invalid - Part 5 is not valid
     *
     * @throws Exception
     */
    @Test
    public void OidcClientEncryptionTests_JWE_Part5_isInvalid() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, setAppName(Constants.SIGALG_RS256));

        String[] msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE };
        negativeTest(updatedTestSettings, createTokenWithBadElement(5), msgs);

    }

}
