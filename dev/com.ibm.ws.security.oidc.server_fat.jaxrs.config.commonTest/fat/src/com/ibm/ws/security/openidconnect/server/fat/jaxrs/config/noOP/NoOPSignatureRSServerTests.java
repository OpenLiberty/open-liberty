/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.actions.JwtTokenActions;
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
import componenttest.rules.SkipJavaSemeruWithFipsEnabled;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled.SkipJavaSemeruWithFipsEnabledRule;

/**
 * Class for RS signature algorithm tests
 * Test class starts a server called OPServer, but that server does provide OP services.
 * It is provided to act as a builder to create JWT's to provide to the RS.
 *
 * @author chrisc
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class NoOPSignatureRSServerTests extends MangleJWTTestTools {

    private static final Class<?> thisClass = NoOPSignatureRSServerTests.class;
    protected static final String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static final String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";

    private static final JwtTokenActions actions = new JwtTokenActions();
    public static final JwtTokenBuilderUtils tokenBuilderHelpers = new JwtTokenBuilderUtils();

    @Rule
    public static final SkipJavaSemeruWithFipsEnabled skipJavaSemeruWithFipsEnabled = new SkipJavaSemeruWithFipsEnabled(OPServerName);

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

        testOPServer = commonSetUp(OPServerName, "server_sigAlg.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "ECDH-ES");
        testOPServer.setRestoreServerBetweenTests(false);

        genericTestServer = commonSetUp(RSServerName, "server_sigAlg.xml", Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, extraMsgs2, null, Constants.OIDC_OP, true, true, tokenType, certType);
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
        parms.add(new NameValuePair("sub", "testuser"));
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, testOPServer.getServer(), builderId, parms);
        Log.info(thisClass, _testName, jwtToken);

        return jwtToken;
    }

    /**
     * Generic method to create a JWT and use it to try to access a protected app
     *
     * @param sigAlgForBuilder
     *            - the builder to use to build a JWT token
     * @param sigAlgForRS
     *            - the signature algorithm to use as the base of the app name that will consume the token
     * @throws Exception
     */
    public void genericSigAlgTest(String sigAlgForBuilder, String sigAlgForRS) throws Exception {

        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName,
                "******** Testing with Jwt builder using signature algorithm: " + sigAlgForBuilder + " and RS using signature algorithm: " + sigAlgForRS + " ********");

        Log.info(thisClass, _testName, "********************************************************************************************************************");

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "snoop/" + sigAlgForRS);

        updatedTestSettings.setSignatureAlg(adjustSigAlg(sigAlgForBuilder));

        String jwtToken = createTokenWithSubject(sigAlgForBuilder + "Builder");

        if (sigAlgForBuilder.equals(sigAlgForRS) || Constants.SIGALG_NONE.equals(sigAlgForBuilder)) {
            positiveTest(updatedTestSettings, jwtToken);
        } else {
            String[] msgs = null;
            // if we're using a completly different differen signature algorithm
            if (sigAlgForBuilder.contains(sigAlgForRS)) {
                msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS1776E_SIGNATURE_VALIDATION };
            } else {
                msgs = new String[] { MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE, MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH };
            }
            negativeTest(updatedTestSettings, jwtToken, msgs);
        }

    }

    /**
     * Some of the tests will use different variations of a builder - most of the builder names are simply the sig alg names.
     * These
     * special builders start with "short_<sigAlg>" or "diff_<sigAlg>". The test case will pass the builder name, but we also need
     * to
     * know what sig alg to put into the the test setting for verification. Instead of passing in an extra parm, we can create the
     * algorithm name
     *
     * @param alg
     *            - the builder name containing the signature algorithm
     * @return
     */
    private String adjustSigAlg(String alg) {

        if (alg != null) {
            return alg.replace("short_", "").replace("diff_", "");
        }
        return alg;
    }

    /******************************* tests *******************************/
    /************** jwt builder/rp using the same algorithm **************/
    /**
     * Test shows that the RS can consume a JWT signed with HS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS256_RSVerifyHS256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS256, Constants.SIGALG_HS256);

    }

    /**
     * Test shows that the RS can consume a JWT signed with HS384
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS384_RSVerifyHS384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS384, Constants.SIGALG_HS384);

    }

    /**
     * Test shows that the RS can consume a JWT signed with HS512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS512_RSVerifyHS512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS512, Constants.SIGALG_HS512);

    }

    /**
     * Test shows that the RS can consume a JWT signed with RS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS256_RSVerifyRS256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RS can consume a JWT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS384_RSVerifyRS384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the RS can consume a JWT signed with RS512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS512_RSVerifyRS512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the RS can consume a JWT signed with ES256
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES256_RSVerifyES256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES256, Constants.SIGALG_ES256);

    }

    /**
     * Test shows that the RS can consume a JWT signed with ES384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES384_RSVerifyES384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES384, Constants.SIGALG_ES384);

    }

    /**
     * Test shows that the RS can consume a JWT signed with ES512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES512_RSVerifyES512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES512, Constants.SIGALG_ES512);

    }

    /*********** jwt builder/rp using the different algorithm ************/
    /* Show that we can't validate the token if the signature algorithms */
    /* don't match */
    /*********************************************************************/
    /**
     * Test shows that the RS can NOT consume a JWT that is NOT signed with HS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS256_RSVerifyHS256() throws Exception {

        String rpSigAlg = Constants.SIGALG_HS256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RS can NOT consume a JWT that is NOT signed with HS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS384_RSVerifyHS384() throws Exception {

        String rpSigAlg = Constants.SIGALG_HS384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RS can NOT consume a JWT that is NOT signed with HS512
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS512_RSVerifyHS512() throws Exception {

        String rpSigAlg = Constants.SIGALG_HS512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RS can NOT consume a JWT that is NOT signed with RS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS256_RSVerifyRS256() throws Exception {

        String rpSigAlg = Constants.SIGALG_RS256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RS can NOT consume a JWT that is NOT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS384_RSVerifyRS384() throws Exception {

        String rpSigAlg = Constants.SIGALG_RS384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
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
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS512_RSVerifyRS512() throws Exception {

        String rpSigAlg = Constants.SIGALG_RS512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RS can NOT consume a JWT that is NOT signed with ES256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES256_RSVerifyES256() throws Exception {

        String rpSigAlg = Constants.SIGALG_ES256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RS can NOT consume a JWT that is NOT signed with ES384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES384_RSVerifyES384() throws Exception {

        String rpSigAlg = Constants.SIGALG_ES384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RS can NOT consume a JWT that is NOT signed with ES512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES512_RSVerifyES512() throws Exception {

        String rpSigAlg = Constants.SIGALG_ES512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /*********** jwt builder/rp using the different same alg, but different keys ************/

    /* Show that we can't validate the token if the signature algorithms match, but either */
    /* the shared key or the public/private keys don't match */
    /****************************************************************************************/
    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg HS256, but a different key
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS256_RSVerifyHS256_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS256, Constants.SIGALG_HS256);

    }

    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg HS384, but a different key
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS384_RSVerifyHS384_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS384, Constants.SIGALG_HS384);

    }

    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg HS512, but a different key
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS512_RSVerifyHS512_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS512, Constants.SIGALG_HS512);

    }

    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg RS256, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void OidcClientSignatureAlgTests_SignTokenRS256_RSVerifyRS256_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg RS384, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void OidcClientSignatureAlgTests_SignTokenRS384_RSVerifyRS384_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg RS512, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void OidcClientSignatureAlgTests_SignTokenRS512_RSVerifyRS512_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg ES256, but a different key
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES256_RSVerifyES256_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES256, Constants.SIGALG_ES256);

    }

    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg ES384, but a different key
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES384_RSVerifyES384_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES384, Constants.SIGALG_ES384);

    }

    /**
     * Test shows that the RS can not consume a JWT signed with sigAlg ES512, but a different key
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES512_RSVerifyES512_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES512, Constants.SIGALG_ES512);

    }

}
