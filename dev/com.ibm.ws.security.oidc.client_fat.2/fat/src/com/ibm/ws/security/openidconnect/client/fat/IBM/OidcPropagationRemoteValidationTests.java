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
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.SignatureEncryptionUserinfoUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 * This is the test class that will run tests to verify the correct behavior when
 * we set the jwtAccessTokenRemoteValidation to its allowed values of none, allow, require.
 * We'll test with inboundPropagation set to either supported or requeired (none is not worth testing
 * as we won't try to validate or even look at the provided token). We'll also test with passing both
 * valid and invalid JWTs on the request.
 *
 * We'll use the JWT builder to create a valid JWT (simply uses the same signature alg as is configured
 * in the client (RS) config. We'll also use the JWT builder to create an invalid JWT (simply use a different
 * signature algorithm than what is configured in the client (RS) config.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcPropagationRemoteValidationTests extends CommonTest {

    public static Class<?> thisClass = OidcPropagationRemoteValidationTests.class;

    private static final JwtTokenActions actions = new JwtTokenActions();
    protected static SignatureEncryptionUserinfoUtils userinfoUtils = new SignatureEncryptionUserinfoUtils();

    protected enum ExpectedBehavior {
        GOOD_LOCAL_ONLY, GOOD_REMOTE_ONLY, BAD_LOCAL_ONLY, BAD_USE_REMOTE_AUTH, BAD_LOCAL_USE_REMOTE_VAL
    }

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

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

        // We don't need an RP for these tests since we'll create our own JWTs for testing

        //Start the OIDC RS server and setup default values
        genericTestServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rs", "rs_server_withOpStub_remoteValidation.xml", Constants.GENERIC_SERVER, rs_apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true);

        testSettings.setFlowType(Constants.RP_FLOW);

        testSettings.setUserinfoEndpt(testSettings.getUserinfoEndpt().replace("oidc/endpoint/OidcConfigSample/userinfo", "UserinfoEndpointServlet").replace("oidc/providers/OidcConfigSample/userinfo", "UserinfoEndpointServlet") + "/saveToken");

        testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);
    }

    /**
     *
     * The common method that allows us to consolidate all of the common steps of the tests in this class.
     *
     * This Method will:
     * 1) create a JWT using the specified builderId - this token is what will be passed on the request to the protected app.
     * It may create a valid JWT (a token that uses the same Signature Alg as what is configured in the client config),
     * or an invalid JWT (a token that uses a different Signature Alg as what is configured in the client config)
     * 2) Create the expectations based on the expectedBehavior flag that is passed to this method
     * 3) Set up some extra parms that userinfo will return and will be inserted into the subject - the existence or omission of
     * of these values can tell us whether userinfo was invoked
     * 4) Create a second JWT using the userinfoBuilderId value passed in
     * 5) Save this second JWT into the userinfo endpoint so it can be returned to the product runtime if the endpoint is called
     * 6) Invoke the protected resource and pass the first JWT
     *
     * @param builderId
     *            The id of the builder to use to create the access_token JWT
     * @param userinfoBuilderId
     *            the id of the builder to use to create the JWT to populate the userinfo response
     * @param appName
     *            The protected app that we'll try to access - the app name via the authFilter drives the runtime to use the
     *            correct client config
     * @param expectedBehavior
     *            flag indicating what results to validate (local validation works/fails, we try to invoke the userinfo
     *            endpoint, we need, but can't find the auth endpoint, ... all based on the token passed as well as the config
     *            attributes that we're testing.
     * @throws Exception
     */
    public void genericTestRemoteValidation(String builderId, String userinfoBuilderId, String appName, ExpectedBehavior expectedBehavior) throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + appName);
        Log.info(thisClass, _testName, "RS Protected App: " + updatedTestSettings.getRSProtectedResource());

        // Create a token to use for access
        String access_token = createTokenWithSubject(builderId);

        List<validationData> expectations = new ArrayList<validationData>();
        // create and save an updated token to be used as the response from the userinfo endpoint
        //        List<validationData> expectations = userinfoUtils.setBasicSigningExpectations(alg, alg, updatedTestSettings, Constants.LOGIN_USER);
        List<NameValuePair> userinfoParms = createExtraParms(null);
        String token = actions.getJwtTokenUsingBuilder(_testName, testOPServer.getServer(), userinfoBuilderId, userinfoParms);
        expectations = createRSExpectations(expectations, expectedBehavior, userinfoParms);

        // save the new token in the test userinfo endpoint so that the rp will have that returned instead of the standard json response
        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", token);
        //  (That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);

        // use the original token and and RS config that will include the
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, expectations);

    }

    /**
     * Create builder name (the builder name is simply the alg + Builder)
     *
     * @param alg
     * @return the builder name to use
     */
    public String setJWSBuilderName(String alg) {
        return alg + "Builder";
    }

    /**
     * Build the name of the app to use on the RS server
     * The name of the app will result in the proper oidc config to be chosen
     *
     * @param alg
     *            the algorithm to use for signing
     * @param inboundPropSetting
     *            The inboundPropagation config value (Required || Supported)
     * @param remoteValidationSetting
     *            The jwtAccessTokenRemoteValidation config value (none || allow || require)
     * @return
     */
    public String setRS_AppName(String alg, String inboundPropSetting, String remoteValidationSetting) {

        return Constants.HELLOWORLD_PROTECTED_RESOURCE + "_" + alg + "_" + inboundPropSetting + "_" + remoteValidationSetting;
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
    /**
     * Creates the expectations for each test based on the expectedBehavior passed in.
     *
     * @param expectations
     *            The current expectations to add to
     * @param expectedBehavior
     *            Flag indicating what behavior to expect and what expectations to set
     * @param parms
     *            Extra parms/attributes to search for in the app output (for positive cases)
     * @return Return updated expectations
     * @throws Exception
     */
    protected List<validationData> createRSExpectations(List<validationData> expectations, ExpectedBehavior expectedBehavior, List<NameValuePair> parms) throws Exception {

        // TODO - not setting status codes

        switch (expectedBehavior) {
        // The test should only validate the token locally - the response should only have the standard token content (it should NOT contain the extra attributes that userinfo would add)
        case GOOD_LOCAL_ONLY:
            expectations = setNotUsedUserinfoData(expectations, parms);
            break;
        // The JWT token fails local validation, and the jwtAccessTokenRemoteValidation setting doesn't allow a remote request - inboundPropagation is set to required, so we will not try to authenticate
        case BAD_LOCAL_ONLY:
            expectations = setNotUsedUserinfoData(expectations, parms);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a message indicating that there is a problem validating the JWT", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
            break;
        // The JWT token fails local validation, and the jwtAccessTokenRemoteValidation setting doesn't allow a remote request - inboundPropagation is set to supported, so we will try to authenticate (but the config is missing the auth endpoint setting)
        case BAD_USE_REMOTE_AUTH:
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a message indicating that there is a problem validating the JWT", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there a problem validating the access token.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that the auth endpoint is not set.", MessageConstants.CWWKS1534E_MISSING_AUTH_ENDPOINT);
            break;
        // BAD_LOCAL_USE_REMOTE_VAL - local validation will fail (we need to check those messages), but when remote use the remote validation, that will succeed since we've created a valid response for the userinfo endpoint to return
        // so fall through this case to the "good" response validation.
        case BAD_LOCAL_USE_REMOTE_VAL:
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a message indicating that there is a problem validating the JWT", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
            setUseUserinfoData(expectations, parms);
            break;
        case GOOD_REMOTE_ONLY:
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_DOES_NOT_CONTAIN, "Client trace.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
            addToAllowableTimeoutCount(1);
            setUseUserinfoData(expectations, parms);
            break;
        default:
            setUseUserinfoData(expectations, parms);
            break;
        }

        return expectations;
    }

    /**
     * Create expectations for unique parms that would be contained in the userinfo response. Set expectations to show that the
     * unique values returned by userinfo are NOT in the response (showing that we're not using userinfo). Also check for a
     * message from the userinfo app to show that we didn't call userinfo.
     *
     * @param expectations
     *            The current expectations that we'll add to
     * @param parms
     *            The extra parms that we should validate are NOT in the response
     * @return Updated expectations
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
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_DOES_NOT_CONTAIN, "OP messages.log should NOT contain a message indicating that we've invoked the userinfo endpoint.", "Userinfo Endpoint Returning token");
        addToAllowableTimeoutCount(1);
        return expectations;
    }

    /**
     *
     * Create expectations for unique parms that should be contained in the userinfo response. Set expectations to show that the
     * unique values returned by userinfo are in the response (showing that we're using userinfo).
     *
     * @param expectations
     *            The current expectations that we'll add to
     * @param parms
     *            The extra parms that we should validate are in the response
     * @return Updated expectations
     * @throws Exception
     */
    private List<validationData> setUseUserinfoData(List<validationData> expectations, List<NameValuePair> parms) throws Exception {

        for (NameValuePair parm : parms) {
            Log.info(thisClass, "createRSExpectations", "adding extra claim check");
            expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the claim " + parm.getName() + " in the response.", null, "\"" + parm.getName() + "\":\"" + parm.getValue() + "\"");

        }
        return expectations;
    }

    /**
     * Create extra parms that will be passed to the test userinfo endpoint. They'll be used with the JWT builder to create a
     * response that userinfo will return when the runtime invokes it.
     * The extra parm "defaultExtraClaim" will be used to show that the runtime is picking up info from userinfo.
     *
     * @param parms
     *            - current list of parms to add values to
     * @return - updated list of parms to add to token
     * @throws Exception
     */
    protected List<NameValuePair> createExtraParms(List<NameValuePair> parms) throws Exception {
        if (parms == null) {
            parms = new ArrayList<NameValuePair>();
        }
        parms.add(new NameValuePair("sub", "testuser"));
        parms.add(new NameValuePair("defaultExtraClaim", "someValue"));
        return parms;

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
        String jwtToken = actions.getJwtTokenUsingBuilder(_testName, testOPServer.getServer(), builderId, parms);
        Log.info(thisClass, _testName, jwtToken);

        return jwtToken;
    }

    /******************************* tests *******************************/

    /**
     * Test with:
     * inboundPropagation = supported
     * jwtAccessTokenRemoteValidation = none
     * valid JWT Token
     * Test that the token validates as valid and that the OP server log does NOT have a message indicating that the userinfo
     * endpoint was invoked.
     * Also validate that the response (output from the app) does NOT contain the extra attributes that userinfo would have added.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_supported_jwtAccessTokenRemoteValidation_none_validToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Supported", "None"), ExpectedBehavior.GOOD_LOCAL_ONLY);

    }

    /**
     * Test with:
     * inboundPropagation = supported
     * jwtAccessTokenRemoteValidation = none
     * invalid JWT Token
     * Test that the token validates as invalid and that the OP server log does NOT have a message indicating that the userinfo
     * endpoint was invoked.
     * Validate that we received an error indicating that the auth endpoint was not found - this indicates that we try to
     * authenticate after the token validation failed.
     * Also validate that the response (output from the app) does NOT contain the extra attributes that userinfo would have added.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_supported_jwtAccessTokenRemoteValidation_none_invalidToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS512), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Supported", "None"), ExpectedBehavior.BAD_USE_REMOTE_AUTH);

    }

    /**
     * Test with:
     * inboundPropagation = required
     * jwtAccessTokenRemoteValidation = none
     * valid JWT Token
     * Test that the token validates as valid and that the OP server log does NOT have a message indicating that the userinfo
     * endpoint was invoked.
     * Also validate that the response (output from the app) does NOT contain the extra attributes that userinfo would have added.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_required_jwtAccessTokenRemoteValidation_none_validToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Required", "None"), ExpectedBehavior.GOOD_LOCAL_ONLY);

    }

    /**
     * Test with:
     * inboundPropagation = required
     * jwtAccessTokenRemoteValidation = none
     * invalid JWT Token
     * Test that the token validates as invalid and that the OP server log does NOT have a message indicating that the userinfo
     * endpoint was invoked.
     * Validate that we received errors indicating that the signature of the token didn't match the config - we should only fail
     * the validation and stop
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_required_jwtAccessTokenRemoteValidation_none_invalidToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS512), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Required", "None"), ExpectedBehavior.BAD_LOCAL_ONLY);

    }

    /**
     * Test with:
     * inboundPropagation = supported
     * jwtAccessTokenRemoteValidation = allow
     * valid JWT Token
     * Test that the token validates as valid and that the OP server log does NOT have a message indicating that the userinfo
     * endpoint was invoked.
     * Also validate that the response (output from the app) does NOT contain the extra attributes that userinfo would have added.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_supported_jwtAccessTokenRemoteValidation_allow_validToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Supported", "Allow"), ExpectedBehavior.GOOD_LOCAL_ONLY);

    }

    /**
     * Test with:
     * inboundPropagation = supported
     * jwtAccessTokenRemoteValidation = allow
     * invalid JWT Token
     * Test that the token validates as invalid and that the OP server log does have a message indicating that the userinfo
     * endpoint was invoked. (after the bad local validation)
     * Also validate that the response (output from the app) does contain the extra attributes that userinfo added.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_supported_jwtAccessTokenRemoteValidation_allow_invalidToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS512), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Supported", "Allow"), ExpectedBehavior.BAD_LOCAL_USE_REMOTE_VAL);

    }

    /**
     * Test with:
     * inboundPropagation = required
     * jwtAccessTokenRemoteValidation = allow
     * valid JWT Token
     * Test that the token validates as valid and that the OP server log does NOT have a message indicating that the userinfo
     * endpoint was invoked.
     * Also validate that the response (output from the app) does NOT contain the extra attributes that userinfo would have added.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_required_jwtAccessTokenRemoteValidation_allow_validToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Required", "Allow"), ExpectedBehavior.GOOD_LOCAL_ONLY);

    }

    /**
     * Test with:
     * inboundPropagation = required
     * jwtAccessTokenRemoteValidation = allow
     * invalid JWT Token
     * Test that the token validates as invalid and that the OP server log does have a message indicating that the userinfo
     * endpoint was invoked. (after the bad local validation)
     * Also validate that the response (output from the app) does contain the extra attributes that userinfo added.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_required_jwtAccessTokenRemoteValidation_allow_invalidToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS512), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Required", "Allow"), ExpectedBehavior.BAD_LOCAL_USE_REMOTE_VAL);

    }

    /**
     * Test with:
     * inboundPropagation = supported
     * jwtAccessTokenRemoteValidation = require
     * valid JWT Token
     * Test that userinfo successfully completes.
     * Validate that the response (output from the app) does contain the extra attributes that userinfo added.
     * By invoking userinfo, we know that local validation didn't take place.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_supported_jwtAccessTokenRemoteValidation_require_validToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Supported", "Require"), ExpectedBehavior.GOOD_REMOTE_ONLY);

    }

    /**
     * Test with:
     * inboundPropagation = supported
     * jwtAccessTokenRemoteValidation = require
     * invalid JWT Token
     * Test that userinfo successfully completes.
     * Validate that the response (output from the app) does contain the extra attributes that userinfo added.
     * By invoking userinfo, we know that local validation didn't take place. We also know that local validation didn't happen
     * because we search for the sig alg mismatch message and should not find it
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_supported_jwtAccessTokenRemoteValidation_require_invalidToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS512), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Supported", "Require"), ExpectedBehavior.GOOD_REMOTE_ONLY);

    }

    /**
     * Test with:
     * inboundPropagation = required
     * jwtAccessTokenRemoteValidation = require
     * valid JWT Token
     * Test that userinfo successfully completes.
     * Validate that the response (output from the app) does contain the extra attributes that userinfo added.
     * By invoking userinfo, we know that local validation didn't take place.
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_required_jwtAccessTokenRemoteValidation_require_validToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS256), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Required", "Require"), ExpectedBehavior.GOOD_REMOTE_ONLY);

    }

    /**
     * Test with:
     * inboundPropagation = required
     * jwtAccessTokenRemoteValidation = require
     * invalid JWT Token
     * Test that userinfo successfully completes.
     * Validate that the response (output from the app) does contain the extra attributes that userinfo added.
     * By invoking userinfo, we know that local validation didn't take place. We also know that local validation didn't happen
     * because we search for the sig alg mismatch message and should not find it
     *
     * @throws Exception
     */
    @Test
    public void OidcPropagationRemoteValidationTests_inboundPropagation_required_jwtAccessTokenRemoteValidation_require_invalidToken() throws Exception {

        genericTestRemoteValidation(setJWSBuilderName(Constants.SIGALG_RS512), setJWSBuilderName(Constants.SIGALG_RS256), setRS_AppName(Constants.SIGALG_RS256, "Required", "Require"), ExpectedBehavior.GOOD_REMOTE_ONLY);

    }

}
