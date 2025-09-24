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
package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.actions.JwtTokenActions;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.SignatureEncryptionUserinfoUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.ExpectedFFDC;
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

    private static final JwtTokenActions actions = new JwtTokenActions();
    protected static SignatureEncryptionUserinfoUtils userinfoUtils = new SignatureEncryptionUserinfoUtils();
    //private final String jwtContentType = "application/jwt";
    private final String jsonContentType = "application/json";

    protected enum ExpectedBehavior {
        USE_USERINFO, DO_NO_USE_USERINFO, CONTENT_TYPE_MISMATCH_JWT_NOT_JSON, CONTENT_TYPE_MISMATCH_JSON_NOT_JWT, SUBJECT_MISMATCH, SIGN_MISMATCH, ENCRYPT_MISMATCH
    }

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        // apps are taking too long to start up for the normal app check, but, we need to be sure that they're ready before we try to use them.
        List<String> opExtraMsgs = new ArrayList<String>() {
            {
                add("CWWKZ0001I.*" + JwtConstants.JWT_SIMPLE_BUILDER_SERVLET);
                add("CWWKZ0001I.*" + Constants.TOKEN_ENDPOINT_SERVLET);
                add("CWWKZ0001I.*" + Constants.USERINFO_ENDPOINT_SERVLET);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.opWithStub", "op_server_userinfo.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, opExtraMsgs, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);
        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_withOpStub_consumeUserinfo.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

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
        genericConsumeJWTUserinfoTest(alg, setJWSBuilderName(alg), setJWSBuilderName(alg), alg, ExpectedBehavior.USE_USERINFO);
    }

    public void genericImplicitConsumeJWSUserinfoTest(String alg) throws Exception {
        genericConsumeJWTUserinfoTest(alg, setJWSBuilderName(alg), setJWSBuilderName(alg), "implicit" + alg, ExpectedBehavior.USE_USERINFO);
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
        genericConsumeJWTUserinfoTest(alg, setJWEBuilderName(alg, alg), setJWEBuilderName(alg, alg), setJWEAppName(alg, alg), ExpectedBehavior.USE_USERINFO);
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
    public void genericConsumeJWTUserinfoTest(String alg, String tokenEndpointBuilderId, String userinfoBuilderId, String appName, ExpectedBehavior expectedBehavior) throws Exception {
        genericConsumeJWTUserinfoTest(alg, tokenEndpointBuilderId, userinfoBuilderId, appName, null, null, expectedBehavior);

    }

    public void genericConsumeJWTUserinfoTest(String alg, String tokenEndpointBuilderId, String userinfoBuilderId, String appName, List<NameValuePair> overrideParms, String contentType, ExpectedBehavior expectedBehavior) throws Exception {

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
        List<validationData> expectations = userinfoUtils.setBasicSigningExpectations(alg, alg, updatedTestSettings, Constants.LOGIN_USER, isImplicit);

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
        // let's create a similar JWT token (that userinfo will return), but make sure that it has an additional claim - we'll check to make sure that the extra claim
        // shows up in the subject that our protected test app sees (when the userinfo reponse is "valid" and make sure that the extra claims DO NOT show up
        // when the userinfo response is "invalid"
        List<NameValuePair> userninfoTokenParms = createUserinfoTokenParms(overrideParms);
        String userinfoToken = actions.getJwtTokenUsingBuilder(_testName, testOPServer.getServer(), userinfoBuilderId, userninfoTokenParms);

        // create the proper expectations - expect extra claims to be or not to be in the subject as well as possible extra error messages in the logs
        expectations = updateExpectationsForTest(expectations, expectedBehavior, userninfoTokenParms);

        // save the new token in the test userinfo endpoint so that the rp will have that returned instead of the standard json response
        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", userinfoToken);
        if (contentType != null) {
            userinfParms = eSettings.addEndpointSettings(userinfParms, "contentType", contentType);
        }
        //  (That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);

        Log.info(thisClass, "genericConsumeJWSUserinfoTest", String.valueOf(expectations.size()));

        // we created and saved a jwt for our test tooling token endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting a jwt from the OP, it will use a
        // token endpoint pointing to the test tooling app that will return the jwt previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

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

    private List<NameValuePair> createUserinfoTokenParms(List<NameValuePair> parms) throws Exception {

        if (parms == null) {
            parms = new ArrayList<NameValuePair>();
            parms.add(new NameValuePair("sub", "testuser"));
            parms.add(new NameValuePair("defaultExtraClaim", "someValue"));
        }

        return parms;
    }

    /**
     * Create expectations for unique parms contained in the userinfo response. We will confirm that the unique claims in the
     * userinfo response are NOT contained in the subject
     * in the cases where the userinfo response is not considered valid and should NOT be used.
     * "sub" is a special case - when we do use the userinfo response, we would expect a different value for sub - in the case
     * where we do NOT use the userinfo response, we
     * should check for the value that the ID token provided - currenlty "testuser", so add an expectation to validate that
     * "testuser" is the sub.
     *
     * @param expectations
     * @param parms
     * @return
     * @throws Exception
     */
    private List<validationData> setNotUsedUserinfoData(List<validationData> expectations, List<NameValuePair> parms) throws Exception {
        for (NameValuePair parm : parms) {
            if (parm.getName().equals("sub")) {
                if (parm.getValue().equals("testuser")) {
                    expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the claim " + parm.getName() + " in the response.", null, "\"" + parm.getName() + "\":\"" + parm.getValue() + "\"");
                } else {
                    expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_MATCH, "Found claim " + parm.getName() + " in the response and it should not be there.", null, "\"" + parm.getName() + "\":\"" + parm.getValue() + "\"");
                }
            }
        }
        return expectations;
    }

    /**
     * Set expectations for the expected behavior of each specific test case. The test case specifies what type of behavior to
     * expect.
     *
     * @param expectations
     *            - already set expectations
     * @param expectedBehavior
     *            - the type of behavior the current test case expects
     * @param parms
     *            - the parms that were used to create claims in the userinfo response token - we'll either expect them to be in
     *            the app output or NOT be in the app output
     * @return - an updated list of expectations
     * @throws Exception
     */
    private List<validationData> updateExpectationsForTest(List<validationData> expectations, ExpectedBehavior expectedBehavior, List<NameValuePair> parms) throws Exception {

        switch (expectedBehavior) {
        case DO_NO_USE_USERINFO:
            expectations = setNotUsedUserinfoData(expectations, parms);
            break;
        // The signature algorithm in the userinfo response does not match the ID token/openidconnect client config (look for error message and for extra claims to NOT exist in the app output)
        case SIGN_MISMATCH:
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a signature mismatch.", MessageConstants.CWWKS1761E_SIG_ALG_MISMATCH);
            expectations = setNotUsedUserinfoData(expectations, parms);
            break;
        // The encryption of the userinfo response does not match the ID token/openidconnect client config (look for error message and for extra claims to NOT exist in the app output)
        case ENCRYPT_MISMATCH:
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a problem retrieving info from the Userinfo endpoint.", MessageConstants.CWWKS1540E_CANNOT_RETRIEVE_DATA_FROM_USERINFO);
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a problem extracting the payload.", MessageConstants.CWWKS6056E_ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE);
            expectations = setNotUsedUserinfoData(expectations, parms);
            break;
        // The sub in the userinfo response does not match the ID token/openidconnect client config (look for error message and for extra claims to NOT exist in the app output)
        case SUBJECT_MISMATCH:
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the subject in the userinfo response did NOT match the subject in the ID token.", MessageConstants.CWWKS1749E_SUB_DID_NOT_MATCH_ID_TOKEN);
            expectations = setNotUsedUserinfoData(expectations, parms);
            break;
        // The content type of the response does not match the format of the actual response look for error message and for extra claims to NOT exist in the app output)
        case CONTENT_TYPE_MISMATCH_JWT_NOT_JSON:
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the subject in the userinfo response did NOT match the subject in the ID token.", MessageConstants.CWWKS1538E_CONTENT_NOT_JSON);
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the subject in the userinfo response did NOT match the subject in the ID token.", MessageConstants.CWWKS1749E_SUB_DID_NOT_MATCH_ID_TOKEN);
            expectations = setNotUsedUserinfoData(expectations, parms);
            break;
        case CONTENT_TYPE_MISMATCH_JSON_NOT_JWT:
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a problem retrieving info from the Userinfo endpoint.", MessageConstants.CWWKS1540E_CANNOT_RETRIEVE_DATA_FROM_USERINFO);
            expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the subject in the userinfo response did NOT match the subject in the ID token.", MessageConstants.CWWKS1539E_CONTENT_NOT_JWT);
            expectations = setNotUsedUserinfoData(expectations, parms);
            break;
        // The userinfo response is valid (for the ID token/openidconnect client) - its content should be used and the subject in the response should reflect the userinfo claims
        case USE_USERINFO:
        default:
            for (NameValuePair parm : parms) {
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the claim " + parm.getName() + " in the response.", null, "\"" + parm.getName() + "\":\"" + parm.getValue() + "\"");
            }
            break;
        }

        return expectations;
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

    /**
     * Test shows that the RP can handle a JWS response that is NOT signed from the userinfo endpoint
     *
     * @throws Exception
     */
    //@Test - enable when 19028 is resolved
    public void OidcClientConsumeUserinfoTests_JWSResponse_notSigned() throws Exception {

        genericConsumeJWSUserinfoTest(Constants.SIGALG_NONE);

    }

    /****** userinfo returns responses that do NOT match the RP config **********/
    // TODO - when 19028 is resolved, update the Constant ALL_TEST_SIGALG to include NONE - need to see where it is used and may need a client list of suppoeted algs and a server list since our OP probably won't support none.
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
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.clients.common.UserInfoException" })
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256_userinfoMismatch() throws Exception {

        for (String alg : Constants.ALL_TEST_SIGALGS) {
            if (!alg.equals(Constants.SIGALG_RS256)) {
                genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(alg), Constants.SIGALG_RS256, ExpectedBehavior.SIGN_MISMATCH);
            }
        }
    }

    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.clients.common.UserInfoException" })
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256_implcicitFlow_userinfoMismatch() throws Exception {

        for (String alg : Constants.ALL_TEST_SIGALGS) {
            if (!alg.equals(Constants.SIGALG_RS256)) {
                genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(alg), "implicit" + Constants.SIGALG_RS256, ExpectedBehavior.SIGN_MISMATCH);
            }
        }
    }

    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.clients.common.UserInfoException" })
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedHS256_implcicitFlow_userinfoMismatch() throws Exception {

        for (String alg : Constants.ALL_TEST_SIGALGS) {
            if (!alg.equals(Constants.SIGALG_HS256)) {
                genericConsumeJWTUserinfoTest(Constants.SIGALG_HS256, setJWSBuilderName(Constants.SIGALG_HS256), setJWSBuilderName(alg), "implicit" + Constants.SIGALG_HS256, ExpectedBehavior.SIGN_MISMATCH);
            }
        }
    }

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
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.clients.common.UserInfoException" })
    @Test
    public void OidcClientConsumeUserinfoTests_JWEResponse_signedRS256_userinfoMismatch() throws Exception {

        for (String alg : Constants.ALL_TEST_ENCRYPTALGS) {
            if (!(alg.equals(Constants.SIGALG_RS256))) {
                genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWEBuilderName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), setJWEBuilderName(alg, alg), setJWEAppName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), ExpectedBehavior.ENCRYPT_MISMATCH);
            }
        }
    }

    /**
     * Test to ensure that we'll accept tokens that are NOT signed, but are encrypted with RS256
     *
     * @throws Exception
     */
    //@Test - enable when 19028 is resolved (and we support none as a valid signing alg
    public void OidcClientConsumeUserinfoTests_JWEResponse_notSigned_encryptRS256() throws Exception {

        genericConsumeJWTUserinfoTest(Constants.SIGALG_NONE, setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_RS256), setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_RS256), setJWEAppName(Constants.SIGALG_NONE, Constants.SIGALG_RS256), ExpectedBehavior.USE_USERINFO);

    }

    /**
     * Test to ensure that we'll accept tokens that are NOT signed, but are encrypted with RS384
     *
     * @throws Exception
     */
    //@Test - enable when 19028 is resolved (and we support none as a valid signing alg
    public void OidcClientConsumeUserinfoTests_JWEResponse_notSigned_encryptRS384() throws Exception {

        genericConsumeJWTUserinfoTest(Constants.SIGALG_NONE, setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_RS384), setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_RS384), setJWEAppName(Constants.SIGALG_NONE, Constants.SIGALG_RS384), ExpectedBehavior.USE_USERINFO);

    }

    /**
     * Test to ensure that we'll accept tokens that are NOT signed, but are encrypted with RS512
     *
     * @throws Exception
     */
    //@Test - enable when 19028 is resolved (and we support none as a valid signing alg
    public void OidcClientConsumeUserinfoTests_JWEResponse_notSigned_encryptRS512() throws Exception {

        genericConsumeJWTUserinfoTest(Constants.SIGALG_NONE, setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_RS512), setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_RS512), setJWEAppName(Constants.SIGALG_NONE, Constants.SIGALG_RS512), ExpectedBehavior.USE_USERINFO);

    }

    /**
     * Test to ensure that we'll accept tokens that are NOT signed, but are encrypted with ES256
     *
     * @throws Exception
     */
    //@Test - enable when 19028 is resolved (and we support none as a valid signing alg (And ES encryption is working properly)
    public void OidcClientConsumeUserinfoTests_JWEResponse_notSigned_encryptES256() throws Exception {

        genericConsumeJWTUserinfoTest(Constants.SIGALG_NONE, setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_ES256), setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_ES256), setJWEAppName(Constants.SIGALG_NONE, Constants.SIGALG_ES256), ExpectedBehavior.USE_USERINFO);

    }

    /**
     * Test to ensure that we'll accept tokens that are NOT signed, but are encrypted with ES384
     *
     * @throws Exception
     */
    //@Test - enable when 19028 is resolved (and we support none as a valid signing alg (And ES encryption is working properly)
    public void OidcClientConsumeUserinfoTests_JWEResponse_notSigned_encryptES384() throws Exception {

        genericConsumeJWTUserinfoTest(Constants.SIGALG_NONE, setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_ES384), setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_ES384), setJWEAppName(Constants.SIGALG_NONE, Constants.SIGALG_ES384), ExpectedBehavior.USE_USERINFO);

    }

    /**
     * Test to ensure that we'll accept tokens that are NOT signed, but are encrypted with ES512
     *
     * @throws Exception
     */
    //@Test - enable when 19028 is resolved (and we support none as a valid signing alg (And ES encryption is working properly)
    public void OidcClientConsumeUserinfoTests_JWEResponse_notSigned_encryptES512() throws Exception {

        genericConsumeJWTUserinfoTest(Constants.SIGALG_NONE, setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_ES512), setJWEBuilderName(Constants.SIGALG_NONE, Constants.SIGALG_ES512), setJWEAppName(Constants.SIGALG_NONE, Constants.SIGALG_ES512), ExpectedBehavior.USE_USERINFO);

    }

    /**
     * Test to ensure that userinfo data that is returned that contains a different sub value is NOT used to update the
     * subject. We should issue a message indicating that the sub doesn't match, give access to the protected app,
     * but not update the subject with the claim data in the userinfo response.
     * Userinfo is returned in the form of a JWS.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256_misMatchSubInUserinfo() throws Exception {

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair("sub", "bob"));
        parms.add(new NameValuePair("defaultExtraClaim", "someValue"));

        genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, parms, null, ExpectedBehavior.SUBJECT_MISMATCH);

    }

    /**
     * Test to ensure that userinfo data that is returned that contains a different sub value is NOT used to update the
     * subject. We should issue a message indicating that the sub doesn't match, give access to the protected app,
     * but not update the subject with the claim data in the userinfo response.
     * Userinfo is returned in the form of a JWE.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWEResponse_signedRS256_misMatchSubInUserinfo() throws Exception {

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair("sub", "bob"));
        parms.add(new NameValuePair("defaultExtraClaim", "someValue"));

        genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWEBuilderName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), setJWEBuilderName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), setJWEAppName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), parms, null, ExpectedBehavior.SUBJECT_MISMATCH);

    }

    /**
     * Test to ensure that userinfo data that is returned that does not contain a sub value is NOT used to update the
     * subject. We should issue a message indicating that the sub doesn't match, give access to the protected app,
     * but not update the subject with the claim data in the userinfo response.
     * Userinfo is returned in the form of a JWS.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256_missingSubInUserinfo() throws Exception {

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair("defaultExtraClaim", "someValue"));

        genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, parms, null, ExpectedBehavior.SUBJECT_MISMATCH);

    }

    /**
     * Test to ensure that userinfo data that is returned that does not contain a sub value is NOT used to update the
     * subject. We should issue a message indicating that the sub doesn't match, give access to the protected app,
     * but not update the subject with the claim data in the userinfo response.
     * Userinfo is returned in the form of a JWE.
     *
     * @throws Exception
     */
    @Test
    public void OidcClientConsumeUserinfoTests_JWEResponse_signedRS256_missingSubInUserinfo() throws Exception {

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair("defaultExtraClaim", "someValue"));

        genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWEBuilderName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), setJWEBuilderName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), setJWEAppName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), parms, null, ExpectedBehavior.SUBJECT_MISMATCH);

    }

    /**
     * Test that we will not accept userinfo data that is contained in a response with content-type set to json when it actually
     * contains a JWS.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void OidcClientConsumeUserinfoTests_JWSResponse_signedRS256_contentTypeJson() throws Exception {

        genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), Constants.SIGALG_RS256, null, jsonContentType, ExpectedBehavior.CONTENT_TYPE_MISMATCH_JWT_NOT_JSON);

    }

    /**
     * Test that we will not accept userinfo data that is contained in a response with content-type set to json when it actually
     * contains a JWS.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "java.io.IOException" })
    @Test
    public void OidcClientConsumeUserinfoTests_JWEResponse_signedRS256_contentTypeJson() throws Exception {

        genericConsumeJWTUserinfoTest(Constants.SIGALG_RS256, setJWEBuilderName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), setJWEBuilderName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), setJWEAppName(Constants.SIGALG_RS256, Constants.SIGALG_RS256), null, jsonContentType, ExpectedBehavior.CONTENT_TYPE_MISMATCH_JWT_NOT_JSON);

    }

    /**
     * Test that we will not accept userinfo data that is contained in a response with content-type set to jwt when it actually
     * contains a json response.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.openidconnect.clients.common.UserInfoException" })
    @Test
    public void OidcClientConsumeUserinfoTests_JSONResponse_signedRS256_contentTypeJwt() throws Exception {

        // unlike all of the other tests, we can use the normal token endpoint in this test - we just need to make the "userinfo" endpoint return json data with a content-type of jwt
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/NormalTokenEndpointRS256"));
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_RS256);
        List<validationData> expectations = userinfoUtils.setBasicSigningExpectations(Constants.SIGALG_RS256, Constants.SIGALG_RS256, updatedTestSettings, Constants.LOGIN_USER, false);

        List<NameValuePair> userinfoTokenParms = new ArrayList<NameValuePair>();
        userinfoTokenParms.add(new NameValuePair("sub", "bob"));
        userinfoTokenParms.add(new NameValuePair("defaultExtraClaim", "someValue"));

        JSONObject jsonData = new JSONObject();
        jsonData.put("sub", "bob");
        jsonData.put("defaultExtraClaim", "someValue");
        String userinfoToken = jsonData.toString();

        // create the proper expectations - expect extra claims to not to be in the subject as well as an extra error message in the logs
        expectations = updateExpectationsForTest(expectations, ExpectedBehavior.CONTENT_TYPE_MISMATCH_JSON_NOT_JWT, userinfoTokenParms);

        // save the new token (json data) in the test userinfo endpoint so that the rp will have that returned instead of the standard json response
        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", userinfoToken);

        //  (That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);

        Log.info(thisClass, "genericConsumeJWSUserinfoTest", String.valueOf(expectations.size()));

        // we created and saved a jwt for our test tooling userinfo endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, will get a json response with a content-type set to jwt
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

}
