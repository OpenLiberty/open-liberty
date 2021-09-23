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
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.actions.JwtTokenActions;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.client.fat.utils.SignatureEncryptionUserinfoUtils;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run tests to verify the correct behavior when
 * the userinfo endpoint returns data in the form of
 * json (we won't bother adding tests for this type as ALL other existing tests use responses in json format as the Liberty OP can
 * only create this type of response)
 * JWS
 * JWE
 *
 * The Liberty OP's userinfo endpoint only creates JSON responses. We'll create a
 * test tool userinfo endpoint. The test cases will use PUT to save a userinfo response (in the form
 * of json, jws or jwe) When called with POST or GET, the test userinfo endpoint will return whatever
 * response was previously saved.
 *
 * This allows us to test that the RP can handle userinfo responses that our OP can't create (but that
 * other providers OPs userinfo endpoint can and do return)
 *
 * We'll test that we can create an opaque or jwt(jws) token (using a normal flow between RP and OP for RS256 and HS256).
 * For all other signature algorithms and encryption, we'll use a jwt build to create a jwt access_token. We'll pass
 * that access token to the RS.
 * The RS will have configs that validates with introspect (and userinfo) or just userinfo.
 * Only the setups where we can use the OP to create the access_token will use introspect (The builder created
 * tokens can't be validated with introspect as the OP does not know of that token)
 * In the cases that can validate with introspect, we'll also call userinfo to gather extra claims. In cases
 * where the signature algorithms in the userinfo response are the same as the config, we'll validate that extra claims
 * from userinfo will show up in the subject printed by the test app. If the signature algorithm in the token returned from
 * userinfo does not match, we'll make sure that we do NOT get a 401 (since we go down the "getUserInfoIfPossible"
 * path and that we do NOT get the extra claims.
 *
 * For signature algorithms and encryption that our OP does not currently support, we'll bypass the OP and RP
 * and create a JWS or JWE with the JWT builder. We'll use userinfo only for validation (introspect won't work in
 * this case). We'll need to use new flags requireJwtAccessTokenRemoteValidation and/or allowJwtAccessTokenRemoteValidation
 * to allow use of the validation endpoints (the runtime only does local only validation if the
 * Bearer token is JWT if we don't use requireJwtAccessTokenRemoteValidation)
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcPropagationConsumeUserinfoTests extends CommonTest {

    public static Class<?> thisClass = OidcPropagationConsumeUserinfoTests.class;

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

        List<String> rs_apps = new ArrayList<String>() {
            {
                add(Constants.HELLOWORLD_APP);
            }
        };

        testSettings = new TestSettings();

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_userinfo.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true);
        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_consumeUserinfo.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true);

        //Start the OIDC RS server and setup default values
        genericTestServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rs", "rs_server_withOpStub_consumeUserinfo.xml", Constants.GENERIC_SERVER, rs_apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true);

        testSettings.setFlowType(Constants.RP_FLOW);

        testSettings.setUserinfoEndpt(testSettings.getUserinfoEndpt().replace("oidc/endpoint/OidcConfigSample/userinfo", "UserinfoEndpointServlet").replace("oidc/providers/OidcConfigSample/userinfo", "UserinfoEndpointServlet") + "/saveToken");

        testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);
    }

    /**
     * NOTE: use of this method should be replaced with calls to genericUseOPToken_ConsumeJWTUserinfoTest if/when we add support
     * to our OP
     * for additional signatures algorithms and encryption. This is a workaround to allow us to test userinfo
     *
     * The common method that allows us to test with signature algorithms and encryption that our OP does NOT support.
     * This method will:
     * 1) build a JWS or JWE token using the builder specified (using builderId)
     * 2) build a JWS or JWE token using the builder specified adding extra claims to be returned by the userinfo endpoint (using
     * userinfoBuilderId)
     * 3) Create expectations to validate that the claims from the token returned by userinfo are found in the subject printed by
     * the test app (when the token is valid for the request - and validate that those claims are NOT found in the subject when
     * the token is NOT valid) (will also add expectations for error messages being logged in the case that the userinfo token is
     * NOT valid)
     * 4) attempt to access the protected app with a config that will use the userinfo endpoint (that return our created token)
     * 5) validate the expectations that we've created
     *
     * @param alg
     *            - the signature algorithm used by the RS
     * @param builderId
     *            - the builderId to use when creating the JWT to be passed on the app request in the RS
     * @param userinfoBuilderId
     *            - the builderId to use when creating the JWT used/returned by the userinfo endpoint
     * @param appName
     *            - the name of the app to invoke (in the RS) to test access
     * @throws Exception
     */
    public void genericUseStubbedToken_ConsumeJWTUserinfoTest(String alg, String builderId, String userinfoBuilderId, String appName) throws Exception {

        boolean isJws = !appName.contains("Encrypt");
        String exactTokenType = (isJws) ? "JWS" : "JWE";
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "******** Testing with a JWT (" + exactTokenType + ") userinfo response - base token created using builder: " + builderId + " and userinfo using builder: " + userinfoBuilderId + "********");
        Log.info(thisClass, _testName, "********************************************************************************************************************");

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + appName);
        Log.info(thisClass, _testName, "RS Protected App: " + updatedTestSettings.getRSProtectedResource());
        updatedTestSettings.setSignatureAlg(alg);

        // Create a token to use for access
        String access_token = createTokenWithSubject(builderId);

        // create and save an updated token to be used as the response from the userinfo endpoint
        List<validationData> expectations = userinfoUtils.setBasicSigningExpectations(alg, alg, updatedTestSettings);
        List<NameValuePair> userinfoParms = createExtraParms(null, userinfoBuilderId);
        String token = createTokenWithSubjectPlus(userinfoBuilderId, userinfoParms);
        expectations = createRSExpectations(expectations, userinfoParms, builderId.equals(userinfoBuilderId), isJws, appName.contains(Constants.USERINFO_ENDPOINT));

        // save the new token in the test userinfo endpoint so that the rp will have that returned instead of the standard json response
        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", token);
        //  (That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);

        // use the original token and and RS config that will include the
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, expectations);

    }

    /**
     * The common method that allows us to test with signature algorithms that our OP does support.
     * This method will:
     * 1) invoke a protected app on an RP - thus creating an access token (either opaque or jwt)
     * 2) build a JWS or JWE token using the builder specified adding extra claims to be returned by the userinfo endpoint (using
     * userinfoBuilderId)
     * 3) Create expectations to validate that the claims from the token returned by userinfo are found in the subject printed by
     * the test app (when the token is valid for the request - and validate that those claims are NOT found in the subject when
     * the token is NOT valid) (will also add expectations for error messages being logged in the case that the userinfo token is
     * NOT valid)
     * 4) attempt to access the protected app with a config that will use the userinfo endpoint (that return our created token)
     * 5) validate the expectations that we've created
     *
     * @param alg
     *            - the signature algorithm used by the RS
     * @param userinfoBuilderId
     *            - the builderId to use when creating the JWT used/returned by the userinfo endpoint
     * @param rpAppName
     *            - the name of the app to invoke (in the RP) to test
     * @param rsAppName
     *            - the name of the app to invoke (in the RS) to test access
     * @throws Exception
     */
    public void genericUseOPToken_ConsumeJWTUserinfoTest(String alg, String userinfoBuilderId, String rpAppName, String rsAppName) throws Exception {

        boolean isJws = !rsAppName.contains("Encrypt");
        String exactTokenType = (isJws) ? "JWS" : "JWE";
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "******************* Testing with a JWT (" + exactTokenType + ") userinfo response - userinfo using builder: " + userinfoBuilderId + "******************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + rpAppName));
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + rsAppName);
        Log.info(thisClass, _testName, "RS Protected App: " + updatedTestSettings.getRSProtectedResource());
        updatedTestSettings.setSignatureAlg(alg);

        List<validationData> expectations = userinfoUtils.setBasicSigningExpectations(alg, alg, updatedTestSettings);

        WebResponse response = genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
        String access_token = validationTools.getTokenFromOutput("access_token=", response);

        List<NameValuePair> userinfoParms = createExtraParms(null, userinfoBuilderId);
        String token = createTokenWithSubjectPlus(userinfoBuilderId, userinfoParms);
        List<validationData> expectations2 = createRSExpectations(expectations, userinfoParms, userinfoBuilderId.startsWith(alg), isJws, rsAppName.contains(Constants.USERINFO_ENDPOINT));

        // save the new token in the test userinfo endpoint so that the rp will have that returned instead of the standard json response
        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", token);
        //  (That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations2);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, expectations2);
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
     * @param alg
     *            - signature and encryption alg
     * @return the builder name to use
     */
    public String setJWEBuilderName(String alg) {
        return setJWEBuilderName(alg, alg);
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
     * Build the name of the app to use on the RP server.
     * The name of the app will result in the proper oidc config to be chosen
     *
     * @param alg
     *            - the signature algorithm to use for signing
     * @param tokenFormat
     *            - the type of access_token to use (opaque/jwt)
     * @return
     */
    public String setRP_JWSAppName(String alg, String tokenFormat) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "********************************** Testing with RP - Verifying with " + alg + " **********************************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return "simple/" + alg + tokenFormat;
    }

    /**
     * Build the name of the app to use on the RS server when using a JWS token
     * The name of the app will result in the proper oidc config to be chosen
     *
     * @param alg
     *            - the algorithm to use for signing
     * @param validationType
     *            - the validation type (introspect/userinfo)
     * @param tokenFormat
     *            - the type of access_token to use (opaque/jwt)
     * @return
     */
    public String setRS_JWSAppName(String alg, String validationType, String tokenFormat) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "********************************** Testing with RS - Verifying with " + alg + " **********************************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return Constants.HELLOWORLD_PROTECTED_RESOURCE + alg + validationType + "_" + tokenFormat;
    }

    /**
     * Build the name of the app to use on the RS server when using a JWE token
     * The name of the app will result in the proper oidc config to be chosen
     *
     * @param alg
     *            - the algorithm to use for signing and encryption
     * @param validationType
     *            - the validation type (introspect/userinfo)
     * @param tokenFormat
     *            - the type of access_token to use (opaque/jwt)
     * @return - the appname to call
     */
    public String setRS_JWEAppName(String alg, String validationType, String tokenFormat) {
        return setRS_JWEAppName(alg, alg, validationType, tokenFormat);
    }

    /**
     * Build the name of the app to use on the RS server when using a JWE token
     * The name of the app will result in the proper oidc config to be chosen
     *
     * @param sigAlg
     *            - the algorithm to use for signing
     * @param decryptAlg
     *            - the algorithm to use for encrypting
     * @param validationType
     *            - the validation type (introspect/userinfo)
     * @param tokenFormat
     *            - the type of access_token to use (opaque/jwt)
     * @return
     */
    public String setRS_JWEAppName(String sigAlg, String decryptAlg, String validationType, String tokenFormat) {
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        Log.info(thisClass, _testName, "************************ Testing with RS - Verifying with " + sigAlg + " and decrypting using: " + decryptAlg + " ************************");
        Log.info(thisClass, _testName, "********************************************************************************************************************");
        return Constants.HELLOWORLD_PROTECTED_RESOURCE + "Sign" + sigAlg + "Encrypt" + decryptAlg + validationType + "_" + tokenFormat;
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
    protected List<validationData> createRSExpectations(List<validationData> expectations, List<NameValuePair> parms, boolean userinfoWillBeUsed, boolean isJWS, boolean usesUserinfoOnly) throws Exception {

        if (usesUserinfoOnly && !userinfoWillBeUsed) {
            Log.info(thisClass, "createRSExpectations", "Expecting failure validating using just userinfo endpoint");
            expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Can not extract the JSON token from the response", MessageConstants.CWWKS1533E_SIGNATURE_MISMATCH);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the auth endpoint is not set.", MessageConstants.CWWKS1534E_MISSING_AUTH_ENDPOINT);
            if (isJWS) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a signature mismatch.", MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there a problem validating the access token.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there a problem extracting JWS from JWE.", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
            }
        } else {
            if (expectations == null) {
                expectations = vData.addSuccessStatusCodes(null);
            }
            for (NameValuePair parm : parms) {
                if (parm.getName().equals("sub") || userinfoWillBeUsed) {
                    if (parm.getName().equals(JwtConstants.PARAM_KEY_MGMT_ALG) || parm.getName().equals((JwtConstants.PARAM_ENCRYPT_KEY))) {
                        Log.info(thisClass, "createRSExpectations", "Not adding a check for parm: " + parm + " - attr is in the JWE header and not part of the payload.");
                    } else {
                        Log.info(thisClass, "createRSExpectations", "adding extra claim check");
                        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the claim " + parm.getName() + " in the response.", null, "\"" + parm.getName() + "\":\"" + parm.getValue() + "\"");
                    }
                } else {
                    expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_MATCH, "Did not see the claim " + parm.getName() + " in the response.", null, "\"" + parm.getName() + "\":\"" + parm.getValue() + "\"");
                }
            }

            if (!userinfoWillBeUsed) {
                if (isJWS) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.TRACE_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
                } else {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.TRACE_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
                }
            }

        }

        return expectations;
    }

    /**
     * Create a jwt token with extra parms - this will be used to generate the the token saved and used by the test "userinfo"
     * endpoint.
     * The extra parm "defaultExtraClaim" will be used to show that the runtime is picking up info from userinfo.
     *
     * @param parms
     *            - current list of parms to add values to
     * @param builderId
     *            - the builderId that will be used (passed to method for use in determining if extra EC parms are needed)
     * @return - updated list of parms to add to token
     * @throws Exception
     */
    protected List<NameValuePair> createExtraParms(List<NameValuePair> parms, String builderId) throws Exception {
        if (parms == null) {
            parms = new ArrayList<NameValuePair>();
        }
        parms.add(new NameValuePair("sub", "testuser"));
        parms.add(new NameValuePair("defaultExtraClaim", "someValue"));
        parms = setParmsForECWorkaround(parms, builderId);
        return parms;

    }

    /**
     * Creates a token using the passed builder along with any parms passed
     *
     * @param builderId
     *            - the builder id to use to build the token
     * @param parms
     *            - extra parms if any
     * @return - a token built using the builder id and extra parms passed
     * @throws Exception
     */
    protected String createTokenWithSubjectPlus(String builderId, List<NameValuePair> parms) throws Exception {

        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, testOPServer.getServer(), builderId, parms);
        Log.info(thisClass, _testName, jwtToken);

        return jwtToken;
    }

    /**
     * Creates a token using the passed builder along with the sub claim
     *
     * @param builderId
     *            - the builder id to use to build the token
     * @return - a token built using the builder id and sub claim
     * @throws Exception
     */
    protected String createTokenWithSubject(String builderId) throws Exception {

        List<NameValuePair> parms = new ArrayList<NameValuePair>();

        parms.add(new NameValuePair("sub", "testuser"));
        parms = setParmsForECWorkaround(parms, builderId);
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, testOPServer.getServer(), builderId, parms);
        Log.info(thisClass, _testName, jwtToken);

        return jwtToken;
    }

    /**
     * Create extra parms to work around issue 17485 (where we can't just use the jwt builder to create a token encrypted with an
     * EC alg)
     *
     * @param parms
     *            - current list of parms (ad EC parms to this list)
     * @param builderId
     *            - the builder that we'll be using (for the tests in this class, we can create the alg name based on the builder
     *            name)
     * @return - an updated list of parms
     * @throws Exception
     */
    public List<NameValuePair> setParmsForECWorkaround(List<NameValuePair> parms, String builderId) throws Exception {

        if (builderId.contains("EncryptES")) {
            String alg = builderId.split("Encrypt")[1].replace("Builder", "");
            testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);
            parms.add(new NameValuePair(JwtConstants.PARAM_KEY_MGMT_ALG, JwtConstants.KEY_MGMT_KEY_ALG_ES));
            parms.add(new NameValuePair(JwtConstants.PARAM_ENCRYPT_KEY, JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), alg)));
        }
        return parms;
    }

    /******************************* tests *******************************/

    /********************** Userinfo JSON Response ***********************/
    // All of the other oidc fat tests run with userinfo returning json
    //  responses, no need to add any additional tests here
    /*********************************************************************/

    /********************** Userinfo JWS Response ************************/
    /******************* userinfo returns JWS responses ******************/

    /**
     * Test with an opaque token signed with RS256.
     * The RS config specifies validation using introspect. userInfoEndpointEnabled is set to true, so we'll use introspect, but
     * also try to add any additional info by using the userinfo endpoint. In this case,
     * userinfo will return a JWS Token signed with RS256. The userinfo response will have an extra claim - the test will validate
     * that the extra claim is in the subject printed by the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_opaqueToken_SignedRS256_validateInstropectWithUserinfo() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setRP_JWSAppName(Constants.SIGALG_RS256, Constants.OPAQUE_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_RS256, Constants.INTROSPECTION_ENDPOINT, Constants.OPAQUE_TOKEN_FORMAT));

    }

    /**
     * Test with an opaque token signed with RS256.
     * The RS config specifies validation using userinfo. In this case,
     * userinfo will return a JWS Token signed with RS256. The userinfo response will have an extra claim - the test will validate
     * that the extra claim is in the subject printed by the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_opaqueToken_SignedRS256_validateUserinfoOnly() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setRP_JWSAppName(Constants.SIGALG_RS256, Constants.OPAQUE_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_RS256, Constants.USERINFO_ENDPOINT, Constants.OPAQUE_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with RS256.
     * The RS config specifies validation using introspect. userInfoEndpointEnabled is set to true, so we'll use introspect, but
     * also try to add any additional info by using the userinfo endpoint. In this case,
     * userinfo will return a JWS Token signed with RS256. The userinfo response will have an extra claim - the test will validate
     * that the extra claim is in the subject printed by the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedRS256_validateInstropectWithUserinfo() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setRP_JWSAppName(Constants.SIGALG_RS256, Constants.JWT_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_RS256, Constants.INTROSPECTION_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with RS256.
     * The RS config specifies validation using userinfo. In this case,
     * userinfo will return a JWS Token signed with RS256. The userinfo response will have an extra claim - the test will validate
     * that the extra claim is in the subject printed by the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedRS256_validateUserinfoOnly() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setRP_JWSAppName(Constants.SIGALG_RS256, Constants.JWT_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_RS256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with an opaque token signed with RS256.
     * The RS config specifies validation using introspect. userInfoEndpointEnabled is set to true, so we'll use introspect, but
     * also try to add any additional info by using the userinfo endpoint. In this case,
     * userinfo will return a JWS Token signed with RS384. Instrospect will succeed, but userinfo will fail because of the alg
     * mismatch. We will validate that the extra claim is NOT in the subject and that we get a warning message in the server log.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_opaqueToken_SignedRS256_validateInstropectWithUserinfo_userinfoMismatchRS384() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS384), setRP_JWSAppName(Constants.SIGALG_RS256, Constants.OPAQUE_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_RS256, Constants.INTROSPECTION_ENDPOINT, Constants.OPAQUE_TOKEN_FORMAT));

    }

    /**
     * Test with a opaque token signed with RS256.
     * The RS config specifies validation using userinfo. In this case,
     * userinfo will return a JWS Token signed with RS384. Validation of the userinfo response will fail because of the alg
     * mistmatch. This will result in the access_token being seen as invalid and we will receive a 401 as well as error messages
     * in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_opaqueToken_SignedRS256_validateUserinfoOnly_userinfoMismatchRS384() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS384), setRP_JWSAppName(Constants.SIGALG_RS256, Constants.OPAQUE_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_RS256, Constants.USERINFO_ENDPOINT, Constants.OPAQUE_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with RS256.
     * The RS config specifies validation using introspect. userInfoEndpointEnabled is set to true, so we'll use introspect, but
     * also try to add any additional info by using the userinfo endpoint. In this case,
     * userinfo will return a JWS Token signed with RS384. Instrospect will succeed, but userinfo will fail because of the alg
     * mismatch. We will validate that the extra claim is NOT in the subject and that we get a warning message in the server log.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedRS256_validateInstropectWithUserinfo_userinfoMismatchRS384() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS384), setRP_JWSAppName(Constants.SIGALG_RS256, Constants.JWT_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_RS256, Constants.INTROSPECTION_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a opaque token signed with RS256.
     * The RS config specifies validation using userinfo. In this case,
     * userinfo will return a JWS Token signed with RS384. Validation of the userinfo response will fail because of the alg
     * mistmatch. This will result in the access_token being seen as invalid and we will receive a 401 as well as error messages
     * in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedRS256_validateUserinfoOnly_userinfoMismatchRS384() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS384), setRP_JWSAppName(Constants.SIGALG_RS256, Constants.JWT_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_RS256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    // NOTE: tests that use signature algorithms other than RS256 and HS256 can use only use userinfo as a validation endpoint.  Our OP can NOT produce an access token (opaque or jws) using any other signature.
    //         Our OP can not produce an access token in JWE format for any alg.  Therefore, we have to use the jwt builder to create an access_token to pass on the request - since the token is not created by the
    //         OP, we can't use introspect to validate it - we can only test using userinfo to validate - that means for any mismatch cases, we'll need to expect a failure, not just missing extra info.
    /**
     * Test with a jwt token signed with RS384 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with RS384.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedRS384_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS384, setJWSBuilderName(Constants.SIGALG_RS384), setJWSBuilderName(Constants.SIGALG_RS384), setRS_JWSAppName(Constants.SIGALG_RS384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with RS384 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with ES256.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedRS384_validateUserinfoOnly_userinfoMismatchES256() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS384, setJWSBuilderName(Constants.SIGALG_RS384), setJWSBuilderName(Constants.SIGALG_ES256), setRS_JWSAppName(Constants.SIGALG_RS384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with RS512 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with RS512.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedRS512_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS512, setJWSBuilderName(Constants.SIGALG_RS512), setJWSBuilderName(Constants.SIGALG_RS512), setRS_JWSAppName(Constants.SIGALG_RS512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with RS512 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with ES512.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedRS512_validateUserinfoOnly_userinfoMismatchES512() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS512, setJWSBuilderName(Constants.SIGALG_RS512), setJWSBuilderName(Constants.SIGALG_ES512), setRS_JWSAppName(Constants.SIGALG_RS512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with ES256 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with ES256.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedES256_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES256, setJWSBuilderName(Constants.SIGALG_ES256), setJWSBuilderName(Constants.SIGALG_ES256), setRS_JWSAppName(Constants.SIGALG_ES256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with ES256 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with RS512.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedES256_validateUserinfoOnly_userinfoMismatchRS512() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES256, setJWSBuilderName(Constants.SIGALG_ES256), setJWSBuilderName(Constants.SIGALG_RS512), setRS_JWSAppName(Constants.SIGALG_ES256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with ES384 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with ES384.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedES384_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES384, setJWSBuilderName(Constants.SIGALG_ES384), setJWSBuilderName(Constants.SIGALG_ES384), setRS_JWSAppName(Constants.SIGALG_ES384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with ES384 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with RS256.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedES384_validateUserinfoOnly_userinfoMismatchRS256() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES384, setJWSBuilderName(Constants.SIGALG_ES384), setJWSBuilderName(Constants.SIGALG_RS256), setRS_JWSAppName(Constants.SIGALG_ES384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with ES512 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with ES512.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedES512_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES512, setJWSBuilderName(Constants.SIGALG_ES512), setJWSBuilderName(Constants.SIGALG_ES512), setRS_JWSAppName(Constants.SIGALG_ES512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with ES512 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with RS384.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedES512_validateUserinfoOnly_userinfoMismatchRS384() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES512, setJWSBuilderName(Constants.SIGALG_ES512), setJWSBuilderName(Constants.SIGALG_RS384), setRS_JWSAppName(Constants.SIGALG_ES512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with an opaque token signed with HS256.
     * The RS config specifies validation using introspect. userInfoEndpointEnabled is set to true, so we'll use introspect, but
     * also try to add any additional info by using the userinfo endpoint. In this case,
     * userinfo will return a JWS Token signed with HS256. The userinfo response will have an extra claim - the test will validate
     * that the extra claim is in the subject printed by the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_opaqueToken_SignedHS256_validateInstropectWithUserinfo() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS256), setRP_JWSAppName(Constants.SIGALG_HS256, Constants.OPAQUE_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_HS256, Constants.INTROSPECTION_ENDPOINT, Constants.OPAQUE_TOKEN_FORMAT));

    }

    /**
     * Test with an opaque token signed with HS256.
     * The RS config specifies validation using userinfo. In this case,
     * userinfo will return a JWS Token signed with HS256. The userinfo response will have an extra claim - the test will validate
     * that the extra claim is in the subject printed by the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_opaqueToken_SignedHS256_validateUserinfoOnly() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS256), setRP_JWSAppName(Constants.SIGALG_HS256, Constants.OPAQUE_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_HS256, Constants.USERINFO_ENDPOINT, Constants.OPAQUE_TOKEN_FORMAT));

    }

    /**
     * Test with an jwt token signed with HS256.
     * The RS config specifies validation using introspect. userInfoEndpointEnabled is set to true, so we'll use introspect, but
     * also try to add any additional info by using the userinfo endpoint. In this case,
     * userinfo will return a JWS Token signed with HS256. The userinfo response will have an extra claim - the test will validate
     * that the extra claim is in the subject printed by the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedHS256_validateInstropectWithUserinfo() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS256), setRP_JWSAppName(Constants.SIGALG_HS256, Constants.JWT_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_HS256, Constants.INTROSPECTION_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with an jwt token signed with HS256.
     * The RS config specifies validation using userinfo. In this case,
     * userinfo will return a JWS Token signed with HS256. The userinfo response will have an extra claim - the test will validate
     * that the extra claim is in the subject printed by the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedHS256_validateUserinfoOnly() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS256), setRP_JWSAppName(Constants.SIGALG_HS256, Constants.JWT_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_HS256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with an opaque token signed with HS256.
     * The RS config specifies validation using introspect. userInfoEndpointEnabled is set to true, so we'll use introspect, but
     * also try to add any additional info by using the userinfo endpoint. In this case,
     * userinfo will return a JWS Token signed with HS384. Instrospect will succeed, but userinfo will fail because of the alg
     * mismatch. We will validate that the extra claim is NOT in the subject and that we get a warning message in the server log.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_opaqueToken_SignedHS256_validateInstropectWithUserinfo_userinfoMismatchHS384() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS384), setRP_JWSAppName(Constants.SIGALG_HS256, Constants.OPAQUE_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_HS256, Constants.INTROSPECTION_ENDPOINT, Constants.OPAQUE_TOKEN_FORMAT));

    }

    /**
     * Test with a opaque token signed with HS256.
     * The RS config specifies validation using userinfo. In this case,
     * userinfo will return a JWS Token signed with HS384. Validation of the userinfo response will fail because of the alg
     * mistmatch. This will result in the access_token being seen as invalid and we will receive a 401 as well as error messages
     * in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_opaqueToken_SignedHS256_validateUserinfoOnly_userinfoMismatchHS384() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS384), setRP_JWSAppName(Constants.SIGALG_HS256, Constants.OPAQUE_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_HS256, Constants.USERINFO_ENDPOINT, Constants.OPAQUE_TOKEN_FORMAT));

    }

    /**
     * Test with an jwt token signed with HS256.
     * The RS config specifies validation using introspect. userInfoEndpointEnabled is set to true, so we'll use introspect, but
     * also try to add any additional info by using the userinfo endpoint. In this case,
     * userinfo will return a JWS Token signed with HS384. Instrospect will succeed, but userinfo will fail because of the alg
     * mismatch. We will validate that the extra claim is NOT in the subject and that we get a warning message in the server log.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedHS256_validateInstropectWithUserinfo_userinfoMismatchHS384() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS384), setRP_JWSAppName(Constants.SIGALG_HS256, Constants.JWT_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_HS256, Constants.INTROSPECTION_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with HS256.
     * The RS config specifies validation using userinfo. In this case,
     * userinfo will return a JWS Token signed with HS384. Validation of the userinfo response will fail because of the alg
     * mistmatch. This will result in the access_token being seen as invalid and we will receive a 401 as well as error messages
     * in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedHS256_validateUserinfoOnly_userinfoMismatchHS384() throws Exception {

        genericUseOPToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS384), setRP_JWSAppName(Constants.SIGALG_HS256, Constants.JWT_TOKEN_FORMAT), setRS_JWSAppName(Constants.SIGALG_HS256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    // NOTE: tests that use signature algorithms other than RS256 and HS256 can use introspect as a validation endpoint.  Our OP can NOT produce an access token (opaque or jws) using any other signature.
    //         Our OP can not produce an access token in JWE format for any alg.  Therefore, we have to hack together the original access token (and can not test with introspect and fill in data with userinfo)
    /**
     * Test with a jwt token signed with HS384 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with HS384.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedHS384_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS384, setJWSBuilderName(Constants.SIGALG_HS384), setJWSBuilderName(Constants.SIGALG_HS384), setRS_JWSAppName(Constants.SIGALG_HS384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with HS384 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with ES256.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedHS384_validateUserinfoOnly_userinfoMismatchES256() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS384, setJWSBuilderName(Constants.SIGALG_HS384), setJWSBuilderName(Constants.SIGALG_ES256), setRS_JWSAppName(Constants.SIGALG_HS384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with HS512 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with HS512.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedHS512_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS512, setJWSBuilderName(Constants.SIGALG_HS512), setJWSBuilderName(Constants.SIGALG_HS512), setRS_JWSAppName(Constants.SIGALG_HS512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed with HS512 (created using the jwt builder as opposed to the RP requesting the token from the
     * OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed with ES512.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jwsToken_SignedHS512_validateUserinfoOnly_userinfoMismatchES512() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_HS512, setJWSBuilderName(Constants.SIGALG_HS512), setJWSBuilderName(Constants.SIGALG_ES512), setRS_JWSAppName(Constants.SIGALG_HS512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /********************** Userinfo JWE Response ************************/

    /************** jwt builder/rp using the same encryption algorithm **************/
    /**
     * Test with a jwt token signed and encrypted with RS256 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * RS256.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedRS256_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWEBuilderName(Constants.SIGALG_RS256), setJWEBuilderName(Constants.SIGALG_RS256), setRS_JWEAppName(Constants.SIGALG_RS256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with RS256 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * ES384.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedRS256_validateUserinfoOnly_userinfoMismatchES384() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWEBuilderName(Constants.SIGALG_RS256), setJWEBuilderName(Constants.SIGALG_ES384), setRS_JWEAppName(Constants.SIGALG_RS256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with RS384 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * RS384.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedRS384_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS384, setJWEBuilderName(Constants.SIGALG_RS384), setJWEBuilderName(Constants.SIGALG_RS384), setRS_JWEAppName(Constants.SIGALG_RS384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with RS384 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * RS512.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedRS384_validateUserinfoOnly_userinfoMismatchRS512() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS384, setJWEBuilderName(Constants.SIGALG_RS384), setJWEBuilderName(Constants.SIGALG_RS512), setRS_JWEAppName(Constants.SIGALG_RS384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with RS512 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * RS512.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedRS512_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS512, setJWEBuilderName(Constants.SIGALG_RS512), setJWEBuilderName(Constants.SIGALG_RS512), setRS_JWEAppName(Constants.SIGALG_RS512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with RS512 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * ES384.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedRS512_validateUserinfoOnly_userinfoMismatchES384() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_RS512, setJWEBuilderName(Constants.SIGALG_RS512), setJWEBuilderName(Constants.SIGALG_ES384), setRS_JWEAppName(Constants.SIGALG_RS512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with ES256 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * ES256.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedES256_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES256, setJWEBuilderName(Constants.SIGALG_ES256), setJWEBuilderName(Constants.SIGALG_ES256), setRS_JWEAppName(Constants.SIGALG_ES256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with ES256 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * RS384.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedES256_validateUserinfoOnly_userinfoMismatchRS384() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES256, setJWEBuilderName(Constants.SIGALG_ES256), setJWEBuilderName(Constants.SIGALG_RS384), setRS_JWEAppName(Constants.SIGALG_ES256, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with ES384 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * ES384.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedES384_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES384, setJWEBuilderName(Constants.SIGALG_ES384), setJWEBuilderName(Constants.SIGALG_ES384), setRS_JWEAppName(Constants.SIGALG_ES384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with ES384 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * RS512.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedES384_validateUserinfoOnly_userinfoMismatchRS512() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES384, setJWEBuilderName(Constants.SIGALG_ES384), setJWEBuilderName(Constants.SIGALG_RS512), setRS_JWEAppName(Constants.SIGALG_ES384, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with ES512 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * ES512.
     * The userinfo response will have an extra claim - the test will validate that the extra claim is in the subject printed by
     * the test app.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedES512_validateUserinfoOnly() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES512, setJWEBuilderName(Constants.SIGALG_ES512), setJWEBuilderName(Constants.SIGALG_ES512), setRS_JWEAppName(Constants.SIGALG_ES512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }

    /**
     * Test with a jwt token signed and encrypted with ES512 (created using the jwt builder as opposed to the RP requesting the
     * token from the OP).
     * The RS config specifies validation using userinfo. In this case, userinfo will return a JWS Token signed and encrypted with
     * RS384.
     * Validation of the userinfo response will fail because of the alg mistmatch. This will result in the access_token being seen
     * as invalid and we will receive a 401 as well as error messages in the server log.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException", "java.lang.Exception" })
    @Test
    public void OidcClientConsumeUserinfoTests_jweToken_EncryptedES512_validateUserinfoOnly_userinfoMismatchRS384() throws Exception {

        genericUseStubbedToken_ConsumeJWTUserinfoTest(Constants.SIGALG_ES512, setJWEBuilderName(Constants.SIGALG_ES512), setJWEBuilderName(Constants.SIGALG_RS384), setRS_JWEAppName(Constants.SIGALG_ES512, Constants.USERINFO_ENDPOINT, Constants.JWT_TOKEN_FORMAT));

    }
}
