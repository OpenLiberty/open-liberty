/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.SignatureEncryptionUserinfoUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled.SkipJavaSemeruWithFipsEnabledRule;

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
@SkipJavaSemeruWithFipsEnabledRule
public class OidcClientSignatureAlgTests extends CommonTest {

    public static Class<?> thisClass = OidcClientSignatureAlgTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.LOGIN_USER;
    protected static String hostName = "localhost";
    public static final String MSG_USER_NOT_IN_REG = "CWWKS1106A";
    protected static SignatureEncryptionUserinfoUtils signingUtils = new SignatureEncryptionUserinfoUtils();

    @ClassRule
    public static final SkipJavaSemeruWithFipsEnabled skipJavaSemeruWithFipsEnabled = new SkipJavaSemeruWithFipsEnabled("com.ibm.ws.security.openidconnect.client-1.0_fat.4.opWithStub");

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcClientSignatureAlgTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        // apps are taking too long to start up for the normal app check, but, we need to be sure that they're ready before we try to use them.
        List<String> opExtraMsgs = new ArrayList<String>() {
            {
                add("CWWKZ0001I.*" + Constants.TOKEN_ENDPOINT_SERVLET);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.4.opWithStub", "op_server_sigAlg.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                Constants.DO_NOT_USE_DERBY, opExtraMsgs, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.4.rp", "rp_server_withOpStub_sigAlg.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
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

    public void genericSigAlgTest(String sigAlgForBuilder, String sigAlgForRP) throws Exception {
        genericSigAlgTest(sigAlgForBuilder, sigAlgForRP, null);
    }

    /**
     * All of the test cases in this class perform the same steps - the only real differences are:
     * 1) which builder will be used to create a JWT Token,
     * 2) which test app they'll use (the test app dictates which RP config will be used),
     * 3) and whether to expect a success or failure (failure indicated by a mis-mismatch in the signature algorithm used by the
     * builder and RP
     * Passing in the builder and RP signature algorithms can tell a common method all it needs to know.
     *
     * This method invokes the test tooling app "TokenEndpointServlet/saveToken" to create the JWT token using the builder
     * specified.
     * It then sets up the expectations for the current instance - if the build and RP signature algorithms match, expectations
     * are set to validate token content, if they do not match expectations are set for a 401 status code and server side error
     * messages indicating the mis-match.
     * Finally, this method tries to invoke the test app protected by the RP. The RP config will end up using the test tooling
     * app's token endpoint and use the "JWT Builder" generated token.
     *
     * @param sigAlgForBuilder
     *            - the signature algorithm that the builder will use - the builder configs are named such that they start with
     *            the signature algorithm (ie: HS256Builder)
     * @param sigAlgForRP
     *            - the signature algorithm that the RP will use - the test app names match filters in the RP config that cause
     *            the RP config to be used with the specified signature algorithm (ie: formlogin/simple/HS256)
     * @param allowedSigAlgs
     *            - list of allowed signature algorithms when RP uses FROM_HEADER configuration - when using FROM_HEADER the test app names
     *              match filters in the RP config that cause the RP config to be used with the specified allioed signature algorithms
     * @throws Exception
     */
    public void genericSigAlgTest(String sigAlgForBuilder, String sigAlgForRP, List<String> allowedSigAlgs) throws Exception {
    
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName,
                "******** Testing with Jwt builder using signature algorithm: " + sigAlgForBuilder + 
                " and RP using signature algorithm: " + sigAlgForRP + 
                (allowedSigAlgs != null ? " with allowed signature algorithms: " + allowedSigAlgs : "") + " ********");
        Log.info(thisClass, _testName, "********************************************************************************************************************");        
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        
        String urlPath = sigAlgForRP;
        if (Constants.SIGALG_FROMHEADER.equals(adjustSigAlg(sigAlgForRP)) && allowedSigAlgs != null) {
            // When using FROM_HEADER with a defined allowedSignatureAlgorithms list
            // We 'box' the algorithm list with underscores and separate them with dashes (e.g., _HS256-HS384_).
            // This specifies the correct auth filter, removing ambiguity
            urlPath += "_" + String.join("-", allowedSigAlgs) + "_";
        }
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + urlPath));
        updatedTestSettings.setSignatureAlg(adjustSigAlg(sigAlgForBuilder));
        List<validationData> expectations = signingUtils.setBasicSigningExpectations(sigAlgForBuilder, sigAlgForRP, allowedSigAlgs, updatedTestSettings, Constants.LOGIN_USER);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "builderId", sigAlgForBuilder + "Builder");

        // Invoke the test TokenEndpoint stub.  It will invoke the Jwt Builder to create a JWT Token (using the builder specified in the builderId passed in via parms
        // The TokenEndpoint stub will save that token and it will be returned when the RP uses it's TokenEnpdointUrl specified in it's config
        //  (That url is:  http://localhost:${bvt.prop.security_1_HTTP_default}/TokenEndpointServlet/getToken)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);

        // we created and saved a jwt for our test tooling token endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting a jwt from the OP, it will use a
        // token endpoint pointing to the test tooling app that will return the jwt previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    private String adjustSigAlg(String rawAlg) {

        if (rawAlg != null) {
            return rawAlg.replace("short_", "").replace("diff_", "").replace("only_", "");
        }
        return rawAlg;
    }

    /******************************* tests *******************************/
    /************** jwt builder/rp using the same algorithm **************/
    /**
     * Test shows that the RP can consume a JWT signed with HS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS256_RPVerifyHS256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS256, Constants.SIGALG_HS256);

    }

    /**
     * Test shows that the RP can consume a JWT signed with HS384
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS384_RPVerifyHS384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS384, Constants.SIGALG_HS384);

    }

    /**
     * Test shows that the RP can consume a JWT signed with HS512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS512_RPVerifyHS512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS512, Constants.SIGALG_HS512);

    }

    /**
     * Test shows that the RP can consume a JWT signed with RS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS256_RPVerifyRS256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RP can consume a JWT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS384_RPVerifyRS384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the RP can consume a JWT signed with RS512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS512_RPVerifyRS512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the RP can consume a JWT signed with ES256
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES256_RPVerifyES256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES256, Constants.SIGALG_ES256);

    }

    /**
     * Test shows that the RP can consume a JWT signed with ES384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES384_RPVerifyES384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES384, Constants.SIGALG_ES384);

    }

    /**
     * Test shows that the RP can consume a JWT signed with ES512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES512_RPVerifyES512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES512, Constants.SIGALG_ES512);

    }

    /*********** jwt builder/rp using the different algorithm ************/
    /* Show that we can't validate the token if the signature algorithms */
    /* don't match */
    /*********************************************************************/
    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with HS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS256_RPVerifyHS256() throws Exception {

        String rpSigAlg = Constants.SIGALG_HS256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with HS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS384_RPVerifyHS384() throws Exception {

        String rpSigAlg = Constants.SIGALG_HS384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with HS512
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS512_RPVerifyHS512() throws Exception {

        String rpSigAlg = Constants.SIGALG_HS512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS256_RPVerifyRS256() throws Exception {

        String rpSigAlg = Constants.SIGALG_RS256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS384_RPVerifyRS384() throws Exception {

        String rpSigAlg = Constants.SIGALG_RS384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS512
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS512_RPVerifyRS512() throws Exception {

        String rpSigAlg = Constants.SIGALG_RS512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES256
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES256_RPVerifyES256() throws Exception {

        String rpSigAlg = Constants.SIGALG_ES256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES384
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES384_RPVerifyES384() throws Exception {

        String rpSigAlg = Constants.SIGALG_ES384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!rpSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, rpSigAlg);
            }
        }

    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES512
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES512_RPVerifyES512() throws Exception {

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
     * Test shows that the RP can not consume a JWT signed with sigAlg HS256, but a different key
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS256_RPVerifyHS256_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS256, Constants.SIGALG_HS256);

    }

    /**
     * Test shows that the RP can not consume a JWT signed with sigAlg HS384, but a different key
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS384_RPVerifyHS384_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS384, Constants.SIGALG_HS384);

    }

    /**
     * Test shows that the RP can not consume a JWT signed with sigAlg HS512, but a different key
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS512_RPVerifyHS512_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS512, Constants.SIGALG_HS512);

    }

    /**
     * Test shows that the RP can not consume a JWT signed with sigAlg RS256, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void OidcClientSignatureAlgTests_SignTokenRS256_RPVerifyRS256_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RP can not consume a JWT signed with sigAlg RS384, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void OidcClientSignatureAlgTests_SignTokenRS384_RPVerifyRS384_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the RP can not consume a JWT signed with sigAlg RS512, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void OidcClientSignatureAlgTests_SignTokenRS512_RPVerifyRS512_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the RP can not consume a JWT signed with sigAlg ES256, but a different key
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES256_RPVerifyES256_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES256, Constants.SIGALG_ES256);

    }

    /**
     * Test shows that the RP can not consume a JWT signed with sigAlg ES384, but a different key
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES384_RPVerifyES384_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES384, Constants.SIGALG_ES384);

    }

    /**
     * Test shows that the RP can not consume a JWT signed with sigAlg ES512, but a different key
     *
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES512_RPVerifyES512_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES512, Constants.SIGALG_ES512);

    }

    /************** jwt builder using set algorithm / rp is using from_header option allowing various algorithms **************/
    /* Show that a rp configured with from_header can ONLY verify JWTs signed with the allowed algorithms
    /****************************************************************************************/

    /**
     * Test shows that the RP can consume a JWT signed with HS256 when configured with FROM_HEADER and allowing HS256
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS256_RPVerifyFromHeader_AllowSignHS256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        
        genericSigAlgTest(Constants.SIGALG_HS256, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with HS384 when configured with FROM_HEADER and allowing HS384
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS384_RPVerifyFromHeader_AllowSignHS384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS384);
        
        genericSigAlgTest(Constants.SIGALG_HS384, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with HS512 when configured with FROM_HEADER and allowing HS512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHS512_RPVerifyFromHeader_AllowSignHS512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS512);
        
        genericSigAlgTest(Constants.SIGALG_HS512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with RS256 when configured with FROM_HEADER and allowing RS256
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS256_RPVerifyFromHeader_AllowSignRS256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS256);
        
        genericSigAlgTest(Constants.SIGALG_RS256, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with RS384 when configured with FROM_HEADER and allowing RS384
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS384_RPVerifyFromHeader_AllowSignRS384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS384);
        
        genericSigAlgTest(Constants.SIGALG_RS384, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with RS512 when configured with FROM_HEADER and allowing RS512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRS512_RPVerifyFromHeader_AllowSignRS512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS512);
        
        genericSigAlgTest(Constants.SIGALG_RS512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with ES256 when configured with FROM_HEADER and allowing ES256
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES256_RPVerifyFromHeader_AllowSignES256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES256);
        
        genericSigAlgTest(Constants.SIGALG_ES256, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with ES384 when configured with FROM_HEADER and allowing ES384
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES384_RPVerifyFromHeader_AllowSignES384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES384);
        
        genericSigAlgTest(Constants.SIGALG_ES384, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with ES512 when configured with FROM_HEADER and allowing ES512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenES512_RPVerifyFromHeader_AllowSignES512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        genericSigAlgTest(Constants.SIGALG_ES512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with HS256 when configured with FROM_HEADER and allowing HS256
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS256_RPVerifyFromHeader_AllowSignHS256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with HS384 when configured with FROM_HEADER and allowing HS384
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS384_RPVerifyFromHeader_AllowSignHS384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS384);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with HS512 when configured with FROM_HEADER and allowing HS512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHS512_RPVerifyFromHeader_AllowSignHS512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS256 when configured with FROM_HEADER and allowing RS256
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS256_RPVerifyFromHeader_AllowSignRS256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS256);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS384 when configured with FROM_HEADER and allowing RS384
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS384_RPVerifyFromHeader_AllowSignRS384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS384);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS512 when configured with FROM_HEADER and allowing RS512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRS512_RPVerifyFromHeader_AllowSignRS512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES256 when configured with FROM_HEADER and allowing ES256
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES256_RPVerifyFromHeader_AllowSignES256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES256);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES384 when configured with FROM_HEADER and allowing ES384
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES384_RPVerifyFromHeader_AllowSignES384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES384);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES512 when configured with FROM_HEADER and allowing ES512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithES512_RPVerifyFromHeader_AllowSignES512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can consume a JWT signed with HS256, HS384 and HS512 when configured with FROM_HEADER and allowing HS256, HS384, HS512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenHSAlgs_RPVerifyFromHeader_AllowSignHSAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        allowedAlgs.add(Constants.SIGALG_HS384);
        allowedAlgs.add(Constants.SIGALG_HS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_HSSIGALGS) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the RP can consume a JWT signed with RS256, RS384 and RS512 when configured with FROM_HEADER and allowing RS256, RS384, RS512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRSAlgs_RPVerifyFromHeader_AllowSignRSAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS256);
        allowedAlgs.add(Constants.SIGALG_RS384);
        allowedAlgs.add(Constants.SIGALG_RS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_RSSIGALGS) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the RP can consume a JWT signed with ES256, ES384 and ES512 when configured with FROM_HEADER and allowing ES256, ES384, ES512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenESAlgs_RPVerifyFromHeader_AllowSignESAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES256);
        allowedAlgs.add(Constants.SIGALG_ES384);
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        for (String builderSigAlg : Constants.ALL_TEST_ESSIGALGS) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with HS256, HS384 and HS512 when configured with FROM_HEADER and allowing HS256, HS384, HS512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithHSAlgs_RPVerifyFromHeader_AllowSignHSAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        allowedAlgs.add(Constants.SIGALG_HS384);
        allowedAlgs.add(Constants.SIGALG_HS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with RS256, RS384 and RS512 when configured with FROM_HEADER and allowing RS256, RS384, RS512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithRSAlgs_RPVerifyFromHeader_AllowSignRSAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS256);
        allowedAlgs.add(Constants.SIGALG_RS384);
        allowedAlgs.add(Constants.SIGALG_RS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can NOT consume a JWT that is NOT signed with ES256, ES384 and ES512 when configured with FROM_HEADER and allowing ES256, ES384, ES512
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenNotWithESAlgs_RPVerifyFromHeader_AllowSignESAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES256);
        allowedAlgs.add(Constants.SIGALG_ES384);
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can consume a JWT signed with HS256, RS384 and ES512 when using FROM_HEADER and allowing HS256, RS384, ES512
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenWithAllowedAlgs_RPVerifyFromHeader_AllowSignHS256RS384ES512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        allowedAlgs.add(Constants.SIGALG_RS384);
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        for (String builderSigAlg : allowedAlgs) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the RP can not consume a JWT signed with non-allowed algorithms when using FROM_HEADER and allowing HS256, RS384, ES512
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientSignatureAlgTests_SignTokenWithDisallowedAlgs_RPVerifyFromHeader_AllowHS256RS384ES512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        allowedAlgs.add(Constants.SIGALG_RS384);
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the RP can consume a JWT signed with all supported signature algorithms when using FROM_HEADER without defining an allowedSignatureAlgorithms
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenAllAlgs_RPVerifyFromHeader() throws Exception {
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            genericSigAlgTest(builderSigAlg, "only_" + Constants.SIGALG_FROMHEADER);
        }
    }

    /**
     * Test shows that the RP can consume JWTs signed with RS256, RS384, and RS512 using the trustedAlias when configured with FROM_HEADER and allowing RS256, RS384 and RS512
     * The configured trust store does not contain algorithm prefixed keys, so the configured alias (altrs256) is used for signature verification of each token
     * The builders all sign with the same altrs256 private key, so all three tokens can be verified by the RP.
     * 
     * Note: For RS-based algorithms (RS256, RS384, RS512), the same RSA key can be used for signing with each algorithm.
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRSAlgs_RPVerifyFromHeader_AllowSignRSAlgs_useTrustAlias() throws Exception {
    
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add("diff_" + Constants.SIGALG_RS256);
        allowedAlgs.add("diff_" + Constants.SIGALG_RS384);
        allowedAlgs.add("diff_" + Constants.SIGALG_RS512);

        for (String builderSigAlg : allowedAlgs) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the RP can consume a JWT signed with RS256, but not RS384 and RS512 using the trustedAlias when configured with FROM_HEADER and allowing RS256, RS384 and RS512
     * The configured trust store does not contain algorithm prefixed keys, so the configured fallback alias (altrs256) is used for signature verification of each token
     * The RS256 builder signs using the altrs256 private key, which the RP can successfully verify
     * The RS384 and RS512 builder sign with their standard keys and signature verification fails on the RP due to a key mismatch
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenRSAlgs_RPVerifyFromHeader_AllowSignRSAlgs_useTrustAlias_keyMismatch() throws Exception {

        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add("diff_" + Constants.SIGALG_RS256);
        allowedAlgs.add("diff_" + Constants.SIGALG_RS384);
        allowedAlgs.add("diff_" + Constants.SIGALG_RS512);

        genericSigAlgTest("diff_" + Constants.SIGALG_RS256, Constants.SIGALG_FROMHEADER, allowedAlgs);
        genericSigAlgTest(Constants.SIGALG_RS384, Constants.SIGALG_FROMHEADER, allowedAlgs);
        genericSigAlgTest(Constants.SIGALG_RS512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the RP can consume a JWT signed with ES256, but not ES384 and ES512 using the trustedAlias when configured with FROM_HEADER and allowing ES256, ES384 and ES512
     * The configured trust store does not contain algorithm prefixed keys, so the configured fallback alias (altes256) is used for signature verification of each token
     * The ES256 builder signs using the altes256 (secp256r1) private key, which the RP can successfully verify
     * The ES384 and ES512 builder sign with their standard keys (secp384r1 and secp521r1) and signature verification fails on the RP due to a key mismatch
     * 
     * Note: ES-based algorithms (ES256, ES384, ES512) each require algorithm-specific elliptic curve keys and cannot share the same key:
     * - ES256 requires P-256 (secp256r1) curve
     * - ES384 requires P-384 (secp384r1) curve
     * - ES512 requires P-521 (secp521r1) curve
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientSignatureAlgTests_SignTokenESAlgs_RPVerifyFromHeader_AllowSignESAlgs_useTrustAlias_keyMismatch() throws Exception {

        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add("diff_" + Constants.SIGALG_ES256);
        allowedAlgs.add("diff_" + Constants.SIGALG_ES384);
        allowedAlgs.add("diff_" + Constants.SIGALG_ES512);

        genericSigAlgTest("diff_" + Constants.SIGALG_ES256, Constants.SIGALG_FROMHEADER, allowedAlgs);
        genericSigAlgTest(Constants.SIGALG_ES384, Constants.SIGALG_FROMHEADER, allowedAlgs);
        genericSigAlgTest(Constants.SIGALG_ES512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

}
