/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that tests the JwtHeaderInjecter api and the underlying code that adds
 * a JWT to the subject and then passes that in the next request.
 * In general, the tests, ...
 * <OL>
 * <LI>Access a test servlet that is protected via an oidc RP config
 * <LI>The RP config specifies:
 * <OL>
 * <LI>JWT reference/config (if it it exists) can specify:
 * <OL>
 * <LI>builder - the builder may/may not exist
 * <LI>claims - may specify claims - the claims may or may not be something that exists.
 * </OL>
 * </OL>
 * <LI>The RP servlet:
 * <OL>
 * <LI>For most of the tests will invoke one of JwtHeaderInjecter(), JwtHeaderInjecter(headerName), or
 * JwtHeaderInjecter(headerName, builder)
 * <LI>HelloWorld servlet on the RS server
 * </OL>
 * </OL>
 *
 */

public class JaxRSClientRequestFilterTests extends CommonTest {

    public static Class<?> thisClass = JaxRSClientRequestFilterTests.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String test_FinalAction = Constants.LOGIN_USER;
    private static final Boolean contextWillBeSet = true;
    private static final Boolean RP_RS_should_use_same_JWT = true;
    private static final Boolean RP_RS_should_NOT_use_same_JWT = false;
    private static final Map<String, String> NoExtraClaims = null;
    private static final String defaultHeaderName = "Authorization";
    public static final String OP_As_Issuer = "http.*://.*/oidc/endpoint/OidcConfigSample";
    public static String Default_Issuer = null;
    public static final String Default_Jwt_Issuer = "http.*://.*/jwt/jwtInjecter";
    public static final String Jwt_Issuer2 = "http.*://.*/jwt/jwtInjecter2";
    public static final String Extra_Jwt_Issuer = "http.*://.*/jwt/extraInjecter";
    public static Boolean OP_Issues_JWT = false;
    private Boolean jwtBuilderRef = true;

    /**
     *
     * JwtHeaderInjecter1 --> JwtHeaderInjecter()
     * JwtHeaderInjecter2 --> JwtHeaderInjecter(headerToInject)
     * JwtHeaderInjecter3 --> JwtHeaderInjecter(headerToInject, jwtBuilder)
     *
     */

    String errMsg0x704 = "CertPathBuilderException";

    /**
     * Add additional checks for output from the other new API's
     *
     */
    private List<validationData> setRSOauthExpectationsWithAPIChecks(String testCase, String finalAction, TestSettings settings, Boolean contextWillBeSet) throws Exception {
        return setRSOauthExpectationsWithAPIChecks(false, testCase, finalAction, settings, contextWillBeSet, true);
    }

    private List<validationData> setRSOauthExpectationsWithAPIChecks(String testCase, String finalAction, TestSettings settings, Boolean contextWillBeSet, Boolean RP_RS_sameJWT) throws Exception {
        return setRSOauthExpectationsWithAPIChecks(true, testCase, finalAction, settings, contextWillBeSet, RP_RS_sameJWT);
    }

    private List<validationData> setRSOauthExpectationsWithAPIChecks(Boolean usesInjection, String testCase, String finalAction, TestSettings settings, Boolean contextWillBeSet, Boolean RP_RS_sameJWT) throws Exception {

        String bearer, scopes = null;
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        if (contextWillBeSet) {
            expectations = validationTools.addDefaultRSOAuthExpectations(expectations, testCase, finalAction, settings);
            bearer = "Bearer";
            scopes = settings.getScope();
            if (scopes.contains("openid")) {
                expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Did not see a valid ID Token in the ouptut", null, "JaxRSClient-getIdToken: null");
            } else {
                expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Received an ID Token and should NOT have", null, "JaxRSClient-getIdToken: null");
            }
        } else {
            bearer = "null";
            scopes = "null";
            expectations = vData.addExpectation(expectations, finalAction, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not see a message that all of the subject values were null", null, "All values in subject are null as they should be");
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Got to the target App on the RS server and should not have...", null, "formlogin/SimpleServlet");
        }
        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token Type set to Bearer printed in the app output", null, "JaxRSClient-getAccessTokenType: " + bearer);
        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token printed in the app output", null, "JaxRSClient-getAccessToken: ");
        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token Scopes printed in the app output", null, "JaxRSClient-getScopes: " + scopes);
        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token IDToken printed in the app output", null, "JaxRSClient-getIdToken: ");

        if (usesInjection) {
            // make sure that the injecter's don't update the value in the subject
            //expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "RP JWT check (before/after injecter) failed (tokens were NOT the same)", null, "RP's JWT in subject check: passed");
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "RP JWT check (before/after injecter) failed (tokens were NOT the same)", null, "RP's Issued JWT in subject check: passed");
            // make sure that the RP and RS use the correct jwt
            if (RP_RS_sameJWT) {
                //if (jwtBuilderRef)
                expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "RP and RS did NOT use the same JWT and they should have", null, "RP and RS Issued JWT's match");
            } else {
                expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "RP and RS DID use the same JWT and they should NOT have", null, "RP and RS Issued JWT's DO NOT match");
            }
        } else {
            if (settings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
                if (RP_RS_sameJWT) {
                    expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "RP and RS did NOT use the same JWT and they should have", null, "RP and RS Issued JWT's DO NOT match");
                } else {
                    expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "RP and RS DID use the same JWT and they should NOT have", null, "RP and RS Issued JWT's DO NOT match");
                }
            } else {
                expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "The response contains JsonWebToken and injection is NOT being used", null, "JsonWebToken");
            }
        }

        return expectations;
    }

    /**
     * Add check for the headername
     *
     * @param expectations
     *            - the current expectations (we'll add to them)
     * @param headerName
     *            - the headername value to validate
     * @return - updated expectations
     * @throws Exception
     */
    private List<validationData> setHeaderNameExpectations(List<validationData> expectations, String headerName) throws Exception {

        expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "RS did Not find headerName " + headerName, null, "Header key: .*" + headerName);

        return expectations;
    }

    /**
     * Add claim expectations
     *
     * @param settings
     *            - the current test settings
     * @param expectations
     *            - the current expectations (we'll add to them)
     * @param audience
     *            - the audience value to check
     * @param iss
     *            - the iss value to check
     * @param claims
     *            - map of extra claims to check
     * @param excludedClaims
     *            - map of claims that we should NOT find in the token
     * @return - updated expectations
     * @throws Exception
     */
    private List<validationData> setClaimExpectations(TestSettings settings, List<validationData> expectations, String audience, String iss, Map<String, String> claims, Map<String, String> excludedClaims) throws Exception {

        // set default claim expectations
        expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find claim [aud] with value [" + audience + "]", null, Constants.JWT_STR_START + ".*\"aud\":.*" + audience);
        expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find claim [iss] with value [" + Constants.JWT_STR_START + ".*iss=" + iss + "]", null, Constants.JWT_STR_START + ".*\"iss\":.*" + iss);
        expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find claim [token_type] with value [Bearer]", null, Constants.JWT_STR_START + ".*\"token_type\":\"Bearer\"");
        expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find claim [sub] with value [" + settings.getAdminUser() + "]", null, Constants.JWT_STR_START + ".*\"sub\":.*" + settings.getAdminUser());
        expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find claim [exp]", null, Constants.JWT_STR_START + ".*\"exp\":.*");
        expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find claim [iat]", null, Constants.JWT_STR_START + ".*\"iat\":.*");
        // set expectations for test specific claims
        if (claims != null) {
            for (String key : claims.keySet()) {
                expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find claim [" + key + "] with value [" + claims.get(key) + "]", null, Constants.JWT_STR_START + ".*\"" + key + "\":.*" + claims.get(key));
            }
        }
        if (excludedClaims != null) {
            for (String key : excludedClaims.keySet()) {
                expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_MATCH, "Found claim [" + key + "] and should NOT have", null, Constants.JWT_STR_START + ".*" + key);
            }
        }

        return expectations;

    }

    /**
     * Add parm to the parm map in the test settings (these will be passed on the servlet invocation)
     *
     * @param settings
     *            - the current test settings
     * @param theKey
     *            - the parm name to add to the map
     * @param theValue
     *            - the value of the parm to add
     * @return - updated settings
     * @throws Exception
     */
    private TestSettings updateMap(TestSettings settings, String theKey, String theValue) throws Exception {

        Map<String, String> currentMap = settings.getRequestParms();
        if (currentMap == null) {
            currentMap = new HashMap<String, String>();
        }
        Log.info(thisClass, "updateMap", "Processing Key: " + theKey + " Value: " + theValue);
        currentMap.put(theKey, theValue);
        settings.setRequestParms(currentMap);

        return settings;
    }

    /**
     * Add expectations for 401 status code
     *
     * @return - updated Expectations
     * @throws Exception
     */
    private List<validationData> setAccessDeniedExpectations() throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, test_FinalAction);
        expectations = vData.addResponseStatusExpectation(expectations, test_FinalAction, Constants.INTERNAL_SERVER_ERROR_STATUS);
        expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see response code 401 in the output", null, Constants.HTTP_UNAUTHORIZED_EXCEPTION);
        expectations = vData.addExpectation(expectations, test_FinalAction, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not see response code 401 in the output", null, Constants.HTTP_UNAUTHORIZED_EXCEPTION);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, test_FinalAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not see message stating that propagation token is missing", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);
        return expectations;
    }

    /**
     * Add missing builder expectations
     *
     * @param failureMsgChecks
     *            - list of messages that we should look for
     * @return - updated expectations
     * @throws Exception
     */
    private List<validationData> missingJwtBuilderConfigExpectations(String[] failureMsgChecks) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null, test_FinalAction);
        expectations = vData.addResponseStatusExpectation(expectations, test_FinalAction, Constants.INTERNAL_SERVER_ERROR_STATUS);

        for (String msg : failureMsgChecks) {
            expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "RP did NOT log " + msg + "in the message of the response", null, msg);
        }
        return expectations;
    }

    /**
     * create a map of the default claims that should be found in the built token
     *
     * @param settings
     *            - current settings
     * @return - the map of the default claims
     * @throws Exception
     */
    private Map<String, String> setDefaultExtraClaimMap(TestSettings settings) throws Exception {

        Map<String, String> extraClaims = new HashMap<String, String>();
        extraClaims.put(Constants.JWT_REALM_NAME, settings.getRealm());
        extraClaims.put(Constants.ACCTOK_UNIQ_SEC_NAME_KEY, settings.getAdminUser());
        return extraClaims;

    }

    /*********************************************************************************************/

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject but, no JWT is passed in the header of the helloworld request
     */
    @Test
    public void JaxRSClientRequestFilterTests_noInection_jwtBuilderInRPConfig() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet);
        if (updatedTestSettings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            String regex = Constants.JWT_STR_START + ".*\"iss\":\"" + OP_As_Issuer;
            expectations = vData.addExpectation(expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not find claim [iss] with value [" + regex + "]", null, regex);
        }

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked
     */
    @Mode(TestMode.LITE)
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter1_jwtBuilderInRPConfig() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_1");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter() api fails appropriately when invoked.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter1_noJwtBuilderInRPConfig() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        // use RP config with NO JWT builder reference
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");

        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_1");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = null;
        if (OP_Issues_JWT) {
            expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
            expectations = setHeaderNameExpectations(expectations, defaultHeaderName);
        } else {
            expectations = missingJwtBuilderConfigExpectations(new String[] { MessageConstants.CWWKS6048E_JWT_MISSING_FROM_SUBJECT });
        }

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked. The request to helloworld fails appropriately
     * as the RS config expects some other headerName
     */
    //@Mode(TestMode.LITE)
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter1_RP_RS_headerNameMismatch() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_1");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setAccessDeniedExpectations();

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter(defaultHeaderName) api is invoked
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_jwtBuilderInRPConfig_default() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter(defaultHeaderName) api fails appropriately when invoked.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_noJwtBuilderInRPConfig_default() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = null;
        if (OP_Issues_JWT) {
            expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
            expectations = setHeaderNameExpectations(expectations, defaultHeaderName);
        } else {
            expectations = missingJwtBuilderConfigExpectations(new String[] { MessageConstants.CWWKS6048E_JWT_MISSING_FROM_SUBJECT });
        }

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * <uniqueHeaderName>. The JwtHeaderInjecter(uniqueHeaderName) api is invoked. The RS server config
     * specifies headerName as <uniqueHeaderName>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_jwtBuilderInRPConfig_uniqueHeaderName() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter(uniqueHeaderName) api fails appropriately when invoked.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_noJwtBuilderInRPConfig_uniqueHeaderName() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = null;
        if (OP_Issues_JWT) {
            expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
            expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");
        } else {
            expectations = missingJwtBuilderConfigExpectations(new String[] { MessageConstants.CWWKS6048E_JWT_MISSING_FROM_SUBJECT });
        }

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter("Authorization", builderFromCfg) api is invoked.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_defaultHeaderName_defaultBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");

        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter(defaultHeaderName, builderFromCfg) api fails appropriately when invoked.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_noJwtBuilderInRPConfig_defaultHeaderName_defaultBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter("Authorization", uniqueBuilder) api is invoked.
     */
    @Mode(TestMode.LITE)
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_defaultHeaderName_uniqueBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_anotherExtraBuilder");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter2");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Jwt_Issuer2, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter(defaultHeaderName, uniqueBuilder) api fails appropriately when invoked.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_noJwtBuilderInRPConfig_defaultHeaderName_uniqueBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_anotherExtraBuilder");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter2");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Jwt_Issuer2, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * <uniqueHeaderName>. The JwtHeaderInjecter(uniqueHeaderName, builderFromCfg) api is invoked. The RS server config
     * specifies headerName as <uniqueHeaderName>
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_uniqueHeaderName_defaultBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter(uniqueHeaderName, builderFromCfg) api fails appropriately when invoked.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_noJwtBuilderInRPConfig_uniquetHeaderName_defaultBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * <uniqueHeaderName>. The JwtHeaderInjecter(uniqueHeaderName, uniqueBuilder) api is invoked. The RS server config
     * specifies headerName as <uniqueHeaderName>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_uniqueHeaderName_uniqueBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_extraBuilder");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "extraInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client02", Extra_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter(uniqueHeaderName, uniqueBuilder) api fails appropriately when invoked.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_noJwtBuilderInRPConfig_uniqueHeaderName_uniqueBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");
        updatedTestSettings.setClientID("client01_noJWT");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_extraBuilder");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "extraInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client02", Extra_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter(uniqueHeaderName, missingBuilder) api fails appropriately when invoked.
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidBuilderException" })
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_uniqueHeaderName_missingBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "missingBuilder");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = missingJwtBuilderConfigExpectations(new String[] { MessageConstants.CWWKS6008E_BAD_CONFIG_ID });

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that without a JWT config reference in the RP config, we do see a JWT in
     * the subject. The JwtHeaderInjecter(uniqueHeaderName, missingBuilder) api fails appropriately when invoked.
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidBuilderException" })
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_noJwtBuilderInRPConfig_uniqueHeaderName_missingBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");
        updatedTestSettings.setClientID("client01_noJWT");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "missingBuilder");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = missingJwtBuilderConfigExpectations(new String[] { MessageConstants.CWWKS6008E_BAD_CONFIG_ID });

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked
     * The claims attribute is omitted from the jwt config in the RP - no extra claims should be included
     */
    @Mode(TestMode.LITE)
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter1_jwtBuilderWithNoClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_noClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_1");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        if (OP_Issues_JWT) {
            //expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        } else {
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        }
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter(defaultHeaderName) api is invoked
     * The claims attribute is omitted from the jwt config in the RP - no extra claims should be included
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_jwtBuilderWithNoClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_noClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        if (OP_Issues_JWT) {
            //expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        } else {
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        }
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter("Authorization", builderFromCfg) api is invoked.
     * The claims attribute is omitted from the jwt config in the RP - no extra claims should be included
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderWithNoClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_noClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked
     * The claims attribute in the jwt config in the RP is set to "" - no extra claims should be included
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter1_jwtBuilderWithEmptyClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_emptyClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_1");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        if (OP_Issues_JWT) {
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        } else {
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        }
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter(defaultHeaderName) api is invoked
     * The claims attribute is omitted from the jwt config in the RP - no extra claims should be included
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_jwtBuilderWithEmptyClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_emptyClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        if (OP_Issues_JWT) {
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        } else {
            expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        }
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked
     * The claims attribute is omitted from the jwt config in the RP - no extra claims should be included
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderWithEmptyClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_emptyClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked
     * The claims attribute in the jwt config in the RP contains some made up names - the default
     * claims and recognized claims will be included, but NOT the made up claim
     */
    @Mode(TestMode.LITE)
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter1_jwtBuilderWithMadeUpClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_madeUpClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_1");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        Map<String, String> omittedClaims = new HashMap<String, String>();
        omittedClaims.put("madeUpClaim", "somevalue");
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), omittedClaims);
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter(defaultHeaderName) api is invoked
     * The claims attribute in the jwt config in the RP contains some made up names - the default
     * claims and recognized claims will be included, but NOT the made up claim
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_jwtBuilderWithMadeUpClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_madeUpClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        Map<String, String> omittedClaims = new HashMap<String, String>();
        omittedClaims.put("madeUpClaim", "somevalue");
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), omittedClaims);
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked
     * The claims attribute in the jwt config in the RP contains some made up names - the default
     * claims and recognized claims will be included, but NOT the made up claim
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderWithMadeUpClaims() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_madeUpClaims");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        Map<String, String> extraClaims = setDefaultExtraClaimMap(updatedTestSettings);
        extraClaims.put("madeUpClaim", "somevalue");
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, extraClaims);
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked
     * Set the headerName attribute in the RP config - show that this value is NOT used to set the
     * headername on the request to the RS
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter1_rpHeaderName() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_rpHeaderName");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_1");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        Map<String, String> omittedClaims = new HashMap<String, String>();
        omittedClaims.put("madeUpClaim", "somevalue");
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), omittedClaims);
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter(defaultHeaderName) api is invoked
     * Set the headerName attribute in the RP config - show that this value is NOT used to set the
     * headername on the request to the RS
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_rpHeaderName() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_rpHeaderName");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        Map<String, String> omittedClaims = new HashMap<String, String>();
        omittedClaims.put("madeUpClaim", "somevalue");
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), omittedClaims);
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter() api is invoked
     * Set the headerName attribute in the RP config - show that this value is NOT used to set the
     * headername on the request to the RS
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_rpHeaderName() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/claims/JaxRSClient_Injecter_rpHeaderName");

        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_uniqueHeaderName");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "myJwtHeaderName");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        Map<String, String> extraClaims = setDefaultExtraClaimMap(updatedTestSettings);
        extraClaims.put("madeUpClaim", "somevalue");
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, extraClaims);
        expectations = setHeaderNameExpectations(expectations, "myJwtHeaderName");

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter("") api is invoked
     * The default headerName "Authorization" is used.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_jwtBuilderInRPConfig_emptyString() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "empty");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter(null) api is invoked
     * The default headerName "Authorization" is used.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter2_jwtBuilderInRPConfig_nullString() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");
        updateMap(updatedTestSettings, Constants.WHERE, "Injection_2");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "null");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, setDefaultExtraClaimMap(updatedTestSettings), NoExtraClaims);
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter("", builderFromCfg) api is invoked.
     * The default headerName "Authorization" is used.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_emptyHeaderName_defaultBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");

        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "empty");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a JWT config reference in the RP config, we do see a JWT in
     * the subject and a JWT is passed in the header of the helloworld request using the headerName of
     * Authorization. The JwtHeaderInjecter(null, builderFromCfg) api is invoked.
     * The default headerName "Authorization" is used.
     */
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_nullHeaderName_defaultBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");

        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "jwtInjecter");
        updateMap(updatedTestSettings, Constants.HEADER_NAME, "null");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet, RP_RS_should_NOT_use_same_JWT);
        expectations = setClaimExpectations(updatedTestSettings, expectations, "client01", Default_Jwt_Issuer, NoExtraClaims, setDefaultExtraClaimMap(updatedTestSettings));
        expectations = setHeaderNameExpectations(expectations, defaultHeaderName);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with an empty builder reference in the api, the JwtHeaderInjecter(uniqueHeaderName, "") api fails
     * appropriately when invoked.
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidBuilderException" })
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_defaultHeaderName_emptyBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");

        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "empty");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = missingJwtBuilderConfigExpectations(new String[] { MessageConstants.CWWKS6008E_BAD_CONFIG_ID });

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <P>
     * Test is showing that with a null builder reference in the api, the JwtHeaderInjecter(uniqueHeaderName, null) api fails
     * appropriately when invoked.
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidBuilderException" })
    @Test
    public void JaxRSClientRequestFilterTests_JwtHeaderInjecter3_jwtBuilderInRPConfig_defaultHeaderName_nullBuilder() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld_basicConfig");

        updateMap(updatedTestSettings, Constants.WHERE, "Injection_3");
        updateMap(updatedTestSettings, Constants.JWT_BUILDER_NAME, "null");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);

        List<validationData> expectations = missingJwtBuilderConfigExpectations(new String[] { MessageConstants.CWWKS6008E_BAD_CONFIG_ID });

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

}
