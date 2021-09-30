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

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.actions.JwtTokenActions;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.client.fat.utils.SignatureEncryptionUserinfoUtils;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run tests to verify the correct behavior when
 * the userinfo endpoint returns data in the form of
 * json
 * JWS
 * JWE
 *
 * The Liberty OP's userinfo endpoint only creates JSON responses. We'll create a
 * test tool userinfo endpoint. When called with POST or GET, it will return whatever
 * response it has. The test cases will save the response that it wants userinfo to return.
 *
 * This allows us to test that the RP can handle userinfo responses that our OP can't create
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcClientConsumeUserinfoTests extends CommonTest {

    public static Class<?> thisClass = OidcClientConsumeUserinfoTests.class;
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
    private static final JwtTokenActions actions = new JwtTokenActions();
    private static final SignatureEncryptionUserinfoUtils userinfoUtils = new SignatureEncryptionUserinfoUtils();

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        // apps are taking too long to start up for the normal app check, but, we need to be sure that they're ready before we try to use them.
        List<String> extraMsgs = new ArrayList<String>() {
            {
                add("CWWKZ0001I.*" + JwtConstants.JWT_SIMPLE_BUILDER_SERVLET);
                add("CWWKZ0001I.*" + Constants.USERINFO_ENDPOINT_SERVLET);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.opWithStub", "op_server_userinfo.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);
        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_withOpStub_consumeUserinfo.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);
        // even though, we're not testing the RP handling the response from the tokenendpoint, the same config that could consume that
        // response will be used to consume the userinfo response, so, ... we need to make sure that the tokenendpoint returns an appropriate response
        testSettings.setTokenEndpt(testSettings.getTokenEndpt().replace("oidc/endpoint/OidcConfigSample/token", "TokenEndpointServlet").replace("oidc/providers/OidcConfigSample/token", "TokenEndpointServlet") + "/saveToken");
        testSettings.setUserinfoEndpt(testSettings.getUserinfoEndpt().replace("oidc/endpoint/OidcConfigSample/userinfo", "UserinfoEndpointServlet").replace("oidc/providers/OidcConfigSample/userinfo", "UserinfoEndpointServlet") + "/saveToken");

        testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);
    }

    /**
     * Invoke common steps to test userinfo returning a JWS token as it's response.
     * This method is used to test cases where the OP, RP and userinfo response are
     * all configured to match (same sig alg, client id, ...)
     *
     * @param alg
     *            - the signature alg that will be used (used as the id to use with the jwt builder, it's also used to specify
     *            which app to invoke (which in turn selects the correct RP config)
     * @throws Exception
     */
    public void genericConsumeJWSUserinfoTest(String alg) throws Exception {
        genericConsumeJWTUserinfoTest(alg, setJWSBuilderName(alg), setJWSBuilderName(alg), alg);
    }

    public void genericImplicitConsumeJWSUserinfoTest(String alg) throws Exception {
        genericConsumeJWTUserinfoTest(alg, setJWSBuilderName(alg), setJWSBuilderName(alg), "implicit" + alg);
    }

    /**
     * Invoke common steps to test userinfo returning a JWE token as it's response.
     * This method is used to test cases where the OP, RP and userinfo response are
     * all configured to match (same sig alg, same encryption alg, client id, ...)
     *
     * @param alg
     *            - the signature alg that will be used (used as the id to use with the jwt builder, it's also used to specify
     *            which app to invoke (which in turn selects the correct RP config)
     * @throws Exception
     */
    public void genericConsumeJWEUserinfoTest(String alg) throws Exception {
        genericConsumeJWTUserinfoTest(alg, setJWEBuilderName(alg, alg), setJWEBuilderName(alg, alg), setJWEAppName(alg, alg));
    }

    /**
     * The common method that will:
     * 1) build a JWS or JWE token using the builder specified to be returned by the token endpoint
     * 2) build a JWS or JWE token using the builder specified adding extra claims to be returned by the userinfo endpoint
     * 3) Create expectations to validate that the claims from the token returned by userinfo are found in the subject printed by
     * the test app (when the token is valid for the request - and validate that those claims are NOT found in the subject when
     * the token is NOT valid)
     * 4) attempt to access the protected app with a config that will use the test token and userinfo endpoints (that return our
     * created tokens)
     * 5) validate the expectations that we've created
     *
     * @param alg
     *            - the signature algorithm used by the OP
     * @param tokenEndpointBuilderId
     *            - the builderId to use when creating the JWT used/returned by the token endpoint
     * @param userinfoBuilderId
     *            - the builderId to use when creating the JWT used/returned by the userinfo endpoint
     * @param appName
     *            - the name of the app to invoke to test access
     * @throws Exception
     */
    public void genericConsumeJWTUserinfoTest(String alg, String tokenEndpointBuilderId, String userinfoBuilderId, String appName) throws Exception {

        boolean isImplicit = appName.contains("implicit");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "******** Testing with a JWT userinfo response - token endpoint using builder: " + tokenEndpointBuilderId + " and userinfo using builder: " + userinfoBuilderId + "********");
        Log.info(thisClass, _testName, "********************************************************************************************************************");

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));
        updatedTestSettings.setSignatureAlg(alg);
        updatedTestSettings.setDecryptKey(JwtKeyTools.getComplexPrivateKeyForSigAlg(testOPServer.getServer(), alg));
        List<validationData> expectations = userinfoUtils.setBasicSigningExpectations(alg, alg, updatedTestSettings, isImplicit);

        if (isImplicit) {
            updatedTestSettings.setState(".+");
        } else {
            // for implicit flow, we don't use the token endpoint
            List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "builderId", tokenEndpointBuilderId);

            // Invoke the test TokenEndpoint stub.  It will invoke the Jwt Builder to create a JWT Token (using the builder specified in the builderId passed in via parms
            // The TokenEndpoint stub will save that token and it will be returned when the RP uses it's TokenEnpdointUrl specified in it's config
            //  (That url is:  http://localhost:${bvt.prop.security_1_HTTP_default}/TokenEndpointServlet/getToken)
            genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);
        }
        // let's create a similar JWT token, but make sure that it has an additional claim - we'll check to make sure that the extra claim
        // shows up in the subject that our protected test app sees
        // 3rd parm tells createTokenWithSubjectAndExpectations whether the userinfo token will or will not match the config so it can determine whether to set
        //  expectations for things that should be found, or things that should NOT be found
        // 4th parm tells createTokenWithSubjectAndExpectations whether we're using JWS or JWE tokens (messages we're searching for will be different) - the check
        //  in the call determines token type because the test app names match the signature alg for JWS tests - for JWE, the appNames are more complex.
        String token = createTokenWithSubjectAndExpectations(userinfoBuilderId, expectations, tokenEndpointBuilderId.equals(userinfoBuilderId), appName.replace("implicit", "").equals(alg));
        // save the new token in the test userinfo endpoint so that the rp will have that returned instead of the standard json response
        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", token);
        //  (That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);

        Log.info(thisClass, "genericConsumeJWSUserinfoTest", String.valueOf(expectations.size()));

        // we created and saved a jwt for our test tooling token endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting a jwt from the OP, it will use a
        // token endpoint pointing to the test tooling app that will return the jwt previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Create builder name for a JWS builder (the builder name is simply the alg + Builder)
     *
     * @param alg
     * @return the builder name to use
     */
    public String setJWSBuilderName(String alg) {
        return alg + "Builder";
    }

    /**
     * Create builder name for a JWE builder
     *
     * @param sigAlg
     *            - signature alg
     * @param encryptAlg
     *            - encryption alg
     * @return the builder name to use
     */
    public String setJWEBuilderName(String sigAlg, String encryptAlg) {
        return "Sign" + sigAlg + "Encrypt" + encryptAlg + "Builder";
    }

    /**
     * Create app name to use with JWE tokens
     *
     * @param sigAlg
     *            - the sig alg to use to build the appname to call (this will in turn result in the appropriate RP config to be
     *            used to verify and decrypt the token)
     * @param decryptAlg
     *            - the decrypt alg to use to build the appname to call (this will in turn result in the appropriate RP config to
     *            be used to verify and decrypt the token)
     * @return - the appname to call
     */
    public String setJWEAppName(String sigAlg, String decryptAlg) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "************************ Testing with RP - Verifying with " + sigAlg + " and decrypting using: " + decryptAlg + " ************************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return "Sign" + sigAlg + "Encrypt" + decryptAlg;
    }

    /**
     * Calls createTokenWithSubjectAndExpectations and include a null list of extra parms to add
     *
     * @param builderId
     *            - the builder id to use to create the token
     * @param expectations
     *            - alrady set expectations - we'll add to these
     * @param userinfoWillBeUsed
     *            - flag indicating if we should set expectations that make sure the claims are in or are not in the subject
     * @param isJWS
     *            - flag indicating if the test usins JWS token (false indicates JWE)
     * @return the created token
     * @throws Exception
     */
    private String createTokenWithSubjectAndExpectations(String builderId, List<validationData> expectations, boolean userinfoWillBeUsed, boolean isJWS) throws Exception {
        return createTokenWithSubjectAndExpectations(builderId, null, expectations, userinfoWillBeUsed, isJWS);

    }

    /**
     * Create a token from the specified build and add a few specific parms that we'll check for in the subject.
     * Set expectations for the new parms - if the test expects this token to be valid case (the rp config matches this tokens
     * contents), we'll set expectations that validate the extra claims are in the subject, if it won't be valid, we'll set
     * expectations that validate that the extra claims are NOT in the subject
     *
     * @param builderId
     *            - the builder id to use to create the token
     * @param parms
     *            - extra parms that the caller wants to add
     * @param expectations
     *            - alrady set expectations - we'll add to these
     * @param userinfoWillBeUsed
     *            - flag indicating if we should set expectations that make sure the claims are in or are not in the subject
     * @param isJWS
     *            - flag indicating if the test usins JWS token (false indicates JWE)
     * @return the created token
     * @throws Exception
     */
    private String createTokenWithSubjectAndExpectations(String builderId, List<NameValuePair> parms, List<validationData> expectations, boolean userinfoWillBeUsed, boolean isJWS) throws Exception {

        if (parms == null) {
            parms = new ArrayList<NameValuePair>();
        }
        parms.add(new NameValuePair("sub", "testuser"));
        parms.add(new NameValuePair("defaultExtraClaim", "someValue"));
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, testOPServer.getServer(), builderId, parms);
        Log.info(thisClass, _testName, jwtToken);
        if (expectations != null) {
            for (NameValuePair parm : parms) {
                if (parm.getName().equals("sub") || userinfoWillBeUsed) {
                    expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the claim " + parm.getName() + " in the response.", null, "\"" + parm.getName() + "\":\"" + parm.getValue() + "\"");
                } else {
                    expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_MATCH, "Did not see the claim " + parm.getName() + " in the response.", null, "\"" + parm.getName() + "\":\"" + parm.getValue() + "\"");
                }
            }
        }
        if (!userinfoWillBeUsed) {
            if (isJWS) {
                // TODO - update message checked once (issue 18256) is resolved
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1761E_SIG_ALG_MISMATCH);
            } else {
                // TODO - update log checked once (issue 18256) is resolved
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.TRACE_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
            }
        }

        return jwtToken;
    }
    /******************************* tests *******************************/

    /************************** JSON Response ***************************/
    // All of the other oidc fat tests run with userinfo returning json
    //  responses, no need to add any additional tests here
    /*********************************************************************/

    /************************** JWS Response ****************************/
    /****** userinfo returns responses that match the RP config **********/
    /**
     * Test shows that the RP can handle a JWS response signed with RS256 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with RS256 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256_implicitFlow() throws Exception {

        genericImplicitConsumeJWSUserinfoTest(Constants.SIGALG_RS256);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with RS384 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS384() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_RS384);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with RS512 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS512() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_RS512);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with HS256 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedHS256() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_HS256);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with HS256 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedHS256_implicitFlow() throws Exception {

        genericImplicitConsumeJWSUserinfoTest(Constants.SIGALG_HS256);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with HS384 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedHS384() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_HS384);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with HS512 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedHS512() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_HS512);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with ES256 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedES256() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_ES256);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with ES384 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedES384() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_ES384);

    }

    /**
     * Test shows that the RP can handle a JWS response signed with ES512 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedES512() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_ES512);

    }

    /****** userinfo returns responses that do NOT match the RP config **********/
    /**
     * Test shows that the RP will fail to be able to consume the JWS token returned by the userinfo
     * endpoint. This will result in an error message being logged to the RP server logs. Processing will
     * continue and we will be granted access to the test app. The JWS returned from the userinfo endpoint
     * contained extra claims that were not in the token returned from the token endpoint. We will verify
     * that none of those extra claims show up in the subject that the protected test app logs. This shows
     * that the userinfo JWS claims were not applied to the subject.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256_userinfoMismatch() throws Exception {

        for (String alg : Constants.ALL_TEST_SIGALGS) {
            if (!alg.equals(Constants.SIGALG_RS256)) {
                genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(alg), Constants.SIGALG_RS256);
            }
        }
    }

    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256_implcicitFlow_userinfoMismatch() throws Exception {

        for (String alg : Constants.ALL_TEST_SIGALGS) {
            if (!alg.equals(Constants.SIGALG_RS256)) {
                genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(alg), "implicit" + Constants.SIGALG_RS256);
            }
        }
    }

    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedHS256_implcicitFlow_userinfoMismatch() throws Exception {

        for (String alg : Constants.ALL_TEST_SIGALGS) {
            if (!alg.equals(Constants.SIGALG_HS256)) {
                genericConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS256), setJWSBuilderName(alg), "implicit" + Constants.SIGALG_HS256);
            }
        }
    }

    // TODO - do we really need to test all variations

    /************************** JWE Response ****************************/

    /************** jwt builder/rp using the same encryption algorithm **************/
    /**
     * Test shows that the RP can handle a JWE response signed and encrypted with RS256 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWEResponse_encryptedRS256() throws Exception {

        genericConsumeJWEUserinfoTest(Constants.SIGALG_RS256);

    }

    /************** jwt builder/rp using the same encryption algorithm **************/
    /**
     * Test shows that the RP can handle a JWE response signed and encrypted with RS384 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientConsumeUserinfoTests_JWEResponse_encryptedRS384() throws Exception {

        genericConsumeJWEUserinfoTest(Constants.SIGALG_RS384);

    }

    /************** jwt builder/rp using the same encryption algorithm **************/
    /**
     * Test shows that the RP can handle a JWE response signed and encrypted with RS512 from the userinfo endpoint
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWEResponse_encryptedRS512() throws Exception {

        genericConsumeJWEUserinfoTest(Constants.SIGALG_RS512);

    }

    // TODO - ES encryption not working properly in the builder
    //    /************** jwt builder/rp using the same encryption algorithm **************/
    //    /**
    //     * Test shows that the RP can handle a JWE response signed and encrypted with ES256 from the userinfo endpoint
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void OidcClientConsumeUserinfoTests_JWEResponse_encryptedES256() throws Exception {
    //
    //        genericConsumeJWTUserinfoTest(Constants.SIGALG_ES256, setBuilderName(Constants.SIGALG_ES256, Constants.SIGALG_ES256), Constants.SIGALG_ES256, setBuilderName(Constants.SIGALG_ES256, Constants.SIGALG_ES256), setAppName(Constants.SIGALG_ES256, Constants.SIGALG_ES256), null, null);
    //      genericConsumeJWEUserinfoTest(Constants.SIGALG_ES256);
    //
    //    }
    //
    //    /************** jwt builder/rp using the same encryption algorithm **************/
    //    /**
    //     * Test shows that the RP can handle a JWE response signed and encrypted with ES384 from the userinfo endpoint
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void OidcClientConsumeUserinfoTests_JWEResponse_encryptedES384() throws Exception {
    //
    //        genericConsumeJWTUserinfoTest(Constants.SIGALG_ES384, setBuilderName(Constants.SIGALG_ES384, Constants.SIGALG_ES384), Constants.SIGALG_ES384, setBuilderName(Constants.SIGALG_ES384, Constants.SIGALG_ES384), setAppName(Constants.SIGALG_ES384, Constants.SIGALG_ES384), null, null);
    //      genericConsumeJWEUserinfoTest(Constants.SIGALG_ES384);
    //
    //    }
    //
    //    /************** jwt builder/rp using the same encryption algorithm **************/
    //    /**
    //     * Test shows that the RP can handle a JWE response signed and encrypted with ES512 from the userinfo endpoint
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void OidcClientConsumeUserinfoTests_JWEResponse_encryptedES512() throws Exception {
    //
    //        genericConsumeJWTUserinfoTest(Constants.SIGALG_ES512, setBuilderName(Constants.SIGALG_ES512, Constants.SIGALG_ES512), Constants.SIGALG_ES512, setBuilderName(Constants.SIGALG_ES512, Constants.SIGALG_ES512), setAppName(Constants.SIGALG_ES512, Constants.SIGALG_ES512), null, null);
    //      genericConsumeJWEUserinfoTest(Constants.SIGALG_ES512);
    //
    //    }

    /****** userinfo returns responses that do NOT match the RP config **********/
    /**
     * Test shows that the RP will fail to be able to consume the JWS token returned by the userinfo
     * endpoint. This will result in an error message being logged to the RP server logs. Processing will
     * continue and we will be granted access to the test app. The JWS returned from the userinfo endpoint
     * contained extra claims that were not in the token returned from the token endpoint. We will verify
     * that none of those extra claims show up in the subject that the protected test app logs. This shows
     * that the userinfo JWS claims were not applied to the subject.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWEResponse_signedRS256_userinfoMismatch() throws Exception {

        for (String alg : Constants.ALL_TEST_ENCRYPTALGS) {
            // TODO skip ES Algs for encryption until (issue 17485) is resolved
            if (!(alg.equals(Constants.SIGALG_RS256) || alg.startsWith("ES"))) {
                genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWEBuilderName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), setJWEBuilderName(alg, alg), setJWEAppName(Constants.SIGALG_RS256, Constants.SIGALG_RS256));
            }
        }
    }
}
