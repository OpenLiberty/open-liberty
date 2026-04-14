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
package com.ibm.ws.security.social.fat.commonTests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.SignatureEncryptionUserinfoUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

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
 * The Social client config will specify the test tooling app instead of the standard token endpoint.
 * The test tooling app will return the saved JWT token as the access_token and id_token.
 *
 * This allows us to test that the Social client can handle a token signed with signature algorithms that
 * our OP does not support.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Social_SignatureAlgTests extends SocialCommonTest {

    public static Class<?> thisClass = Social_SignatureAlgTests.class;

    protected static SignatureEncryptionUserinfoUtils signingUtils = null;
    public static boolean isTestingOidc = false; // subclasses may set this to true

    @Rule
    public static final SkipJavaSemeruWithFipsEnabled skipJavaSemeruWithFipsEnabled = new SkipJavaSemeruWithFipsEnabled("com.ibm.ws.security.social_fat.LibertyOP.opWithStub");

    public void genericSigAlgTest(String sigAlgForBuilder, String sigAlgForSocialClient) throws Exception {
        genericSigAlgTest(sigAlgForBuilder, sigAlgForSocialClient, null);
    }

    /**
     * All of the test cases in this class perform the same steps - the only real differences are:
     * 1) which builder will be used to create a JWT Token,
     * 2) which test app they'll use (the test app dictates which Social client config will be used),
     * 3) and whether to expect a success or failure (failure indicated by a mis-mismatch in the signature algorithm used by the
     * builder and Social client
     * Passing in the builder and Social client signature algorithms can tell a common method all it needs to know.
     *
     * This method invokes the test tooling app "TokenEndpointServlet/saveToken" to create the JWT token using the builder
     * specified.
     * It then sets up the expectations for the current instance - if the build and Social client signature algorithms match,
     * expectations are set to validate token content, if they do not match expectations are set for a 401 status code and server
     * side error messages indicating the mis-match.
     * Finally, this method tries to invoke the test app protected by the Social client. The Social client config will end up
     * using the test tooling app's token endpoint and use the "JWT Builder" generated token.
     *
     * @param sigAlgForBuilder
     *            - the signature algorithm that the builder will use - the builder configs are named such that they start with
     *            the signature algorithm (ie: HS256Builder)
     * @param sigAlgForSocialClient
     *            - the signature algorithm that the Social client will use - the test app names match filters in the Social
     *            client config that cause
     *            the Social client config to be used with the specified signature algorithm (ie: formlogin/simple/HS256)
     * @throws Exception
     */
    public void genericSigAlgTest(String sigAlgForBuilder, String sigAlgForSocialClient, List<String> allowedSigAlgs) throws Exception {

        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "******** Testing with Jwt builder using signature algorithm: " + sigAlgForBuilder + " and Social client using signature algorithm: " + sigAlgForSocialClient + 
        (allowedSigAlgs != null ? " with allowed signature algorithms: " + allowedSigAlgs : "") + " ********");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        WebClient wc = getAndSaveWebClient();
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();

        String urlPath = sigAlgForSocialClient;
        if (Constants.SIGALG_FROMHEADER.equals(adjustSigAlg(sigAlgForSocialClient)) && allowedSigAlgs != null) {
            // When using FROM_HEADER with a defined allowedSignatureAlgorithms list
            // We 'box' the algorithm list with underscores and separate them with dashes (e.g., _HS256-HS384_).
            // This specifies the correct auth filter, removing ambiguity
            urlPath += "_" + String.join("-", allowedSigAlgs) + "_";
        }
        updatedSocialTestSettings.setProtectedResource(updatedSocialTestSettings.getProtectedResource() + urlPath);

        updatedSocialTestSettings.setSignatureAlg(adjustSigAlg(sigAlgForBuilder));

        List<validationData> expectations = setBasicSigningExpectations(sigAlgForBuilder, sigAlgForSocialClient, allowedSigAlgs, updatedSocialTestSettings);

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "builderId", sigAlgForBuilder + "Builder");

        // Invoke the test TokenEndpoint stub.  It will invoke the Jwt Builder to create a JWT Token (using the builder specified in the builderId passed in via parms
        // The TokenEndpoint stub will save that token and it will be returned when the Social client uses it's TokenEnpdointUrl specified in it's config
        //  (That url is:  http://localhost:${bvt.prop.security_1_HTTP_default}/TokenEndpointServlet/getToken)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedSocialTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);

        // we created and saved a jwt for our test tooling token endpoint to return to the Social client - let's invoke
        // the protected resource.  The Social client will get the auth token, but, instead of getting a jwt from the OP, it will use a
        // token endpoint pointing to the test tooling app that will return the jwt previously obtained using a builder
        genericSocial(_testName, wc, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    private String adjustSigAlg(String rawAlg) {

        if (rawAlg != null) {
            return rawAlg.replace("short_", "").replace("diff_", "");
        }
        return rawAlg;
    }

    public List<validationData> setBasicSigningExpectations(String sigAlgForBuilder, String sigAlgForSocialClient, SocialTestSettings settings) throws Exception {
        return setBasicSigningExpectations(sigAlgForBuilder, sigAlgForSocialClient,null, settings);
    }

    public List<validationData> setBasicSigningExpectations(String sigAlgForBuilder, String sigAlgForSocialClient, List<String> allowedSigAlgs, SocialTestSettings settings) throws Exception {

        String test_finalAction = SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN;

        List<validationData> expectations = null;
        // if the signature alg in the build matches what's in the RP, the test should succeed - validate status codes and token content
        if (sigAlgForBuilder.equals(sigAlgForSocialClient) || (sigAlgForSocialClient.contains("FROM_HEADER") && (allowedSigAlgs == null || allowedSigAlgs.contains(sigAlgForBuilder)))) {
            expectations = setGoodSocialExpectations(settings, doNotAddJWTTokenValidation);
            expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
            expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_finalAction, settings);
        } else {
            // validate that we get the correct error message(s) for tests that use the same sig alg, but have mis-matched keys
            if (sigAlgForBuilder.contains(sigAlgForSocialClient) || allowedSigAlgsStringContainBuilder(allowedSigAlgs, sigAlgForBuilder)) {
                expectations = vData.addSuccessStatusCodesForActions(test_finalAction, invoke_social_login_actions);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, test_finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1756E_OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR + ".*client01.*" + (sigAlgForSocialClient.equals("FROM_HEADER") ? allowedSigAlgs : sigAlgForSocialClient) + ".*");
            } else {
                // create negative expectations when signature algorithms don't match
                expectations = vData.addSuccessStatusCodesForActions(test_finalAction, invoke_social_login_actions);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, test_finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1761E_SIG_ALG_MISMATCH + ".*client01.*" + (sigAlgForSocialClient.equals("FROM_HEADER") ? allowedSigAlgs : sigAlgForSocialClient) + ".*");
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, test_finalAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
            }
        }

        return expectations;
    }

    private boolean allowedSigAlgsStringContainBuilder(List<String> allowedSigAlgs, String sigAlgForBuilder) {
        if (allowedSigAlgs != null) {
            for (String allowedAlg : allowedSigAlgs) {
                if (allowedAlg.contains(sigAlgForBuilder)) {
                    return true;
                }
            }
        }
        return false;
    }

    /******************************* tests *******************************/
    /************** jwt builder/Social client using the same algorithm **************/
    /**
     * Test shows that the Social client can consume a JWT signed with HS256
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenHS256_SocialVerifyHS256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS256, Constants.SIGALG_HS256);

    }

    /**
     * Test shows that the Social client can consume a JWT signed with HS384
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_SignatureAlgTests_SignTokenHS384_SocialVerifyHS384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS384, Constants.SIGALG_HS384);

    }

    /**
     * Test shows that the Social client can consume a JWT signed with HS512
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenHS512_SocialVerifyHS512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_HS512, Constants.SIGALG_HS512);

    }

    /**
     * Test shows that the Social client can consume a JWT signed with RS256
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRS256_SocialVerifyRS256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the Social client can consume a JWT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRS384_SocialVerifyRS384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the Social client can consume a JWT signed with RS512
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRS512_SocialVerifyRS512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the Social client can consume a JWT signed with ES256
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_SignatureAlgTests_SignTokenES256_SocialVerifyES256() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES256, Constants.SIGALG_ES256);

    }

    /**
     * Test shows that the Social client can consume a JWT signed with ES384
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenES384_SocialVerifyES384() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES384, Constants.SIGALG_ES384);

    }

    /**
     * Test shows that the Social client can consume a JWT signed with ES512
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenES512_SocialVerifyES512() throws Exception {

        genericSigAlgTest(Constants.SIGALG_ES512, Constants.SIGALG_ES512);

    }

    /*********** jwt builder/Social client using the different algorithm ************/
    /* Show that we can't validate the token if the signature algorithms */
    /* don't match */
    /*********************************************************************/
    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with HS256
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithHS256_SocialVerifyHS256() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_HS256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with HS384
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithHS384_SocialVerifyHS384() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_HS384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with HS512
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithHS512_SocialVerifyHS512() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_HS512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with RS256
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithRS256_SocialVerifyRS256() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_RS256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with RS384
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithRS384_SocialVerifyRS384() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_RS384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with RS512
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithRS512_SocialVerifyRS512() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_RS512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with ES256
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithES256_SocialVerifyES256() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_ES256;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with ES384
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithES384_SocialVerifyES384() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_ES384;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with ES512
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithES512_SocialVerifyES512() throws Exception {

        String SocialClientSigAlg = Constants.SIGALG_ES512;
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!SocialClientSigAlg.equals(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, SocialClientSigAlg);
            }
        }

    }

    /*********** jwt builder/Social client using the different same alg, but different keys ************/
    /* Show that we can't validate the token if the signature algorithms match, but either */
    /* the shared key or the public/private keys don't match */
    /****************************************************************************************/
    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg HS256, but a different key
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_SignatureAlgTests_SignTokenHS256_SocialVerifyHS256_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS256, Constants.SIGALG_HS256);

    }

    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg HS384, but a different key
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenHS384_SocialVerifyHS384_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS384, Constants.SIGALG_HS384);

    }

    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg HS512, but a different key
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenHS512_SocialVerifyHS512_keyMismatch() throws Exception {

        genericSigAlgTest("diff_" + Constants.SIGALG_HS512, Constants.SIGALG_HS512);

    }

    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg RS256, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void Social_SignatureAlgTests_SignTokenRS256_SocialVerifyRS256_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS256, Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg RS384, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void Social_SignatureAlgTests_SignTokenRS384_SocialVerifyRS384_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS384, Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg RS512, but a different key
     * Skip test on FIPS 140-3 because it requires RSA keys to be at least 2048 bit
     *
     * @throws Exception
     */
    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void Social_SignatureAlgTests_SignTokenRS512_SocialVerifyRS512_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_RS512, Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg ES256, but a different key
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_SignatureAlgTests_SignTokenES256_SocialVerifyES256_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES256, Constants.SIGALG_ES256);

    }

    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg ES384, but a different key
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenES384_SocialVerifyES384_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES384, Constants.SIGALG_ES384);

    }

    /**
     * Test shows that the Social client can not consume a JWT signed with sigAlg ES512, but a different key
     *
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenES512_SocialVerifyES512_keyMismatch() throws Exception {

        genericSigAlgTest("short_" + Constants.SIGALG_ES512, Constants.SIGALG_ES512);

    }

    /************** jwt builder using set algorithm / Social Client is using from_header option allowing various algorithms **************/
    /* Show that a Social Client configured with from_header can ONLY verify JWTs signed with the allowed algorithms
    /****************************************************************************************/

    /**
     * Test shows that the Social client can consume a JWT signed with HS256 when configured with FROM_HEADER and allowing HS256
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenHS256_SocialVerifyFromHeader_AllowSignHS256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        
        genericSigAlgTest(Constants.SIGALG_HS256, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with HS384 when configured with FROM_HEADER and allowing HS384
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenHS384_SocialVerifyFromHeader_AllowSignHS384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS384);
        
        genericSigAlgTest(Constants.SIGALG_HS384, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with HS512 when configured with FROM_HEADER and allowing HS512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenHS512_SocialVerifyFromHeader_AllowSignHS512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS512);
        
        genericSigAlgTest(Constants.SIGALG_HS512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with RS256 when configured with FROM_HEADER and allowing RS256
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRS256_SocialVerifyFromHeader_AllowSignRS256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS256);
        
        genericSigAlgTest(Constants.SIGALG_RS256, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with RS384 when configured with FROM_HEADER and allowing RS384
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRS384_SocialVerifyFromHeader_AllowSignRS384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS384);
        
        genericSigAlgTest(Constants.SIGALG_RS384, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with RS512 when configured with FROM_HEADER and allowing RS512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRS512_SocialVerifyFromHeader_AllowSignRS512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS512);
        
        genericSigAlgTest(Constants.SIGALG_RS512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with ES256 when configured with FROM_HEADER and allowing ES256
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenES256_SocialVerifyFromHeader_AllowSignES256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES256);
        
        genericSigAlgTest(Constants.SIGALG_ES256, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with ES384 when configured with FROM_HEADER and allowing ES384
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenES384_SocialVerifyFromHeader_AllowSignES384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES384);
        
        genericSigAlgTest(Constants.SIGALG_ES384, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with ES512 when configured with FROM_HEADER and allowing ES512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenES512_SocialVerifyFromHeader_AllowSignES512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        genericSigAlgTest(Constants.SIGALG_ES512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with HS256 when configured with FROM_HEADER and allowing HS256
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithHS256_SocialVerifyFromHeader_AllowSignHS256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with HS384 when configured with FROM_HEADER and allowing HS384
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithHS384_SocialVerifyFromHeader_AllowSignHS384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS384);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with HS512 when configured with FROM_HEADER and allowing HS512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithHS512_SocialVerifyFromHeader_AllowSignHS512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with RS256 when configured with FROM_HEADER and allowing RS256
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithRS256_SocialVerifyFromHeader_AllowSignRS256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS256);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with RS384 when configured with FROM_HEADER and allowing RS384
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithRS384_SocialVerifyFromHeader_AllowSignRS384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS384);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with RS512 when configured with FROM_HEADER and allowing RS512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithRS512_SocialVerifyFromHeader_AllowSignRS512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with ES256 when configured with FROM_HEADER and allowing ES256
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithES256_SocialVerifyFromHeader_AllowSignES256() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES256);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with ES384 when configured with FROM_HEADER and allowing ES384
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithES384_SocialVerifyFromHeader_AllowSignES384() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES384);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with ES512 when configured with FROM_HEADER and allowing ES512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithES512_SocialVerifyFromHeader_AllowSignES512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        for (String builderSigAlg : Constants.ALL_TEST_SIGALGS) {
            if (!allowedAlgs.contains(builderSigAlg)) {
                genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
            }
        }
    }
    
    /**
     * Test shows that the Social client can consume a JWT signed with HS256, HS384 and HS512 when configured with FROM_HEADER and allowing HS256, HS384, HS512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenHSAlgs_SocialVerifyFromHeader_AllowSignHSAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        allowedAlgs.add(Constants.SIGALG_HS384);
        allowedAlgs.add(Constants.SIGALG_HS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_HSSIGALGS) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the Social client can consume a JWT signed with RS256, RS384 and RS512 when configured with FROM_HEADER and allowing RS256, RS384, RS512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRSAlgs_SocialVerifyFromHeader_AllowSignRSAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_RS256);
        allowedAlgs.add(Constants.SIGALG_RS384);
        allowedAlgs.add(Constants.SIGALG_RS512);
        
        for (String builderSigAlg : Constants.ALL_TEST_RSSIGALGS) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the Social client can consume a JWT signed with ES256, ES384 and ES512 when configured with FROM_HEADER and allowing ES256, ES384, ES512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenESAlgs_SocialVerifyFromHeader_AllowSignESAlgs() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_ES256);
        allowedAlgs.add(Constants.SIGALG_ES384);
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        for (String builderSigAlg : Constants.ALL_TEST_ESSIGALGS) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with HS256, HS384 and HS512 when configured with FROM_HEADER and allowing HS256, HS384, HS512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithHSAlgs_SocialVerifyFromHeader_AllowSignHSAlgs() throws Exception {
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
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with RS256, RS384 and RS512 when configured with FROM_HEADER and allowing RS256, RS384, RS512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithRSAlgs_SocialVerifyFromHeader_AllowSignRSAlgs() throws Exception {
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
     * Test shows that the Social client can NOT consume a JWT that is NOT signed with ES256, ES384 and ES512 when configured with FROM_HEADER and allowing ES256, ES384, ES512
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenNotWithESAlgs_SocialVerifyFromHeader_AllowSignESAlgs() throws Exception {
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
     * Test shows that the Social client can consume a JWT signed with HS256, RS384 and ES512 when using FROM_HEADER and allowing HS256, RS384, ES512
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_SignatureAlgTests_SignTokenWithAllowedAlgs_SocialVerifyFromHeader_AllowSignHS256RS384ES512() throws Exception {
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add(Constants.SIGALG_HS256);
        allowedAlgs.add(Constants.SIGALG_RS384);
        allowedAlgs.add(Constants.SIGALG_ES512);
        
        for (String builderSigAlg : allowedAlgs) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the Social client can not consume a JWT signed with non-allowed algorithms when using FROM_HEADER and allowing HS256, RS384, ES512
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_SignatureAlgTests_SignTokenWithDisallowedAlgs_SocialVerifyFromHeader_AllowHS256RS384ES512() throws Exception {
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
     * Test shows that the Social client can consume a JWT signed with all supported signature algorithms when using FROM_HEADER without defining an allowedSignatureAlgorithms
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenAllAlgs_SocialVerifyFromHeader() throws Exception {
        
        for (String builderSigAlg : Constants.ALL_TEST_HSSIGALGS) {
            genericSigAlgTest(builderSigAlg, "only_" + Constants.SIGALG_FROMHEADER);
        }
    }

    /**
     * Test shows that the Social client can consume JWTs signed with RS256, RS384, and RS512 using the trustedAlias when configured with FROM_HEADER and allowing RS256, RS384 and RS512
     * The configured trust store does not contain algorithm prefixed keys, so the configured alias (altrs256) is used for signature verification of each token
     * The builders all sign with the same altrs256 private key, so all three tokens can be verified by the Social client.
     * 
     * Note: For RS-based algorithms (RS256, RS384, RS512), the same RSA key can be used for signing with each algorithm.
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRSAlgs_SocialVerifyFromHeader_AllowSignRSAlgs_useTrustAlias() throws Exception {
    
        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add("diff_" + Constants.SIGALG_RS256);
        allowedAlgs.add("diff_" + Constants.SIGALG_RS384);
        allowedAlgs.add("diff_" + Constants.SIGALG_RS512);

        for (String builderSigAlg : allowedAlgs) {
            genericSigAlgTest(builderSigAlg, Constants.SIGALG_FROMHEADER, allowedAlgs);
        }
    }

    /**
     * Test shows that the Social client can consume a JWT signed with RS256, but not RS384 and RS512 using the trustedAlias when configured with FROM_HEADER and allowing RS256, RS384 and RS512
     * The configured trust store does not contain algorithm prefixed keys, so the configured fallback alias (altrs256) is used for signature verification of each token
     * The RS256 builder signs using the altrs256 private key, which the Social client can successfully verify
     * The RS384 and RS512 builder sign with their standard keys and signature verification fails on the Social client due to a key mismatch
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenRSAlgs_SocialVerifyFromHeader_AllowSignRSAlgs_useTrustAlias_keyMismatch() throws Exception {

        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add("diff_" + Constants.SIGALG_RS256);
        allowedAlgs.add("diff_" + Constants.SIGALG_RS384);
        allowedAlgs.add("diff_" + Constants.SIGALG_RS512);

        genericSigAlgTest("diff_" + Constants.SIGALG_RS256, Constants.SIGALG_FROMHEADER, allowedAlgs);
        genericSigAlgTest(Constants.SIGALG_RS384, Constants.SIGALG_FROMHEADER, allowedAlgs);
        genericSigAlgTest(Constants.SIGALG_RS512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }

    /**
     * Test shows that the Social client can consume a JWT signed with ES256, but not ES384 and ES512 using the trustedAlias when configured with FROM_HEADER and allowing ES256, ES384 and ES512
     * The configured trust store does not contain algorithm prefixed keys, so the configured fallback alias (altes256) is used for signature verification of each token
     * The ES256 builder signs using the altes256 (secp256r1) private key, which the Social client can successfully verify
     * The ES384 and ES512 builder sign with their standard keys (secp384r1 and secp521r1) and signature verification fails on the Social client due to a key mismatch
     * 
     * Note: ES-based algorithms (ES256, ES384, ES512) each require algorithm-specific elliptic curve keys and cannot share the same key:
     * - ES256 requires P-256 (secp256r1) curve
     * - ES384 requires P-384 (secp384r1) curve
     * - ES512 requires P-521 (secp521r1) curve
     * 
     * @throws Exception
     */
    @Test
    public void Social_SignatureAlgTests_SignTokenESAlgs_SocialVerifyFromHeader_AllowSignESAlgs_useTrustAlias_keyMismatch() throws Exception {

        List<String> allowedAlgs = new ArrayList<>();
        allowedAlgs.add("diff_" + Constants.SIGALG_ES256);
        allowedAlgs.add("diff_" + Constants.SIGALG_ES384);
        allowedAlgs.add("diff_" + Constants.SIGALG_ES512);

        genericSigAlgTest("diff_" + Constants.SIGALG_ES256, Constants.SIGALG_FROMHEADER, allowedAlgs);
        genericSigAlgTest(Constants.SIGALG_ES384, Constants.SIGALG_FROMHEADER, allowedAlgs);
        genericSigAlgTest(Constants.SIGALG_ES512, Constants.SIGALG_FROMHEADER, allowedAlgs);
    }
}
