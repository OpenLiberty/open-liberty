/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.backChannelLogout.fat.CommonTests;

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

/**
 * This is the test class that contains
 **/

public class LogoutTokenTests extends BackChannelLogoutCommonTests {

    public static Class<?> thisClass = LogoutTokenTests.class;
    public static BackChannelLogoutCommonTests logoutTokenUtils = new BackChannelLogoutCommonTests();

    @Override
    public String buildBackChannelLogoutUri(String client) throws Exception {

        String part2 = (Constants.OIDC_CLIENT_DEFAULT_CONTEXT_ROOT + Constants.OIDC_BACK_CHANNEL_LOGOUT_ROOT + client).replace("//", "/");
        String uri = testRPServer.getHttpsString() + part2;
        Log.info(thisClass, "_testName", "backChannelLogouturi: " + uri);

        return uri;
    }

    public void genericMissingRequiredClaimTest(String claimToOmit) throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParms_removingOne(claimToOmit);

        List<validationData> expectations = setMissingBackChannelLogoutRequestClaim(claimToOmit);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    public void genericInvalidRequiredClaimTest(String claim, Object value, String errorMsg) throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParms_addOrOverrideOne(claim, value);

        List<validationData> expectations = setInvalidBackChannelLogoutRequestClaim(errorMsg);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>
     * </OL>
     */

    @Test
    public void LogoutTokenTests_validTokenContent() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParms();

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    @Test
    public void LogoutTokenTests_missing_required_iss() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_ISSUER);

    }

    @Test
    public void LogoutTokenTests_invalid_iss() throws Exception {

        genericInvalidRequiredClaimTest(Constants.PAYLOAD_ISSUER, "someBadValue", MessageConstants.CWWKS1751E_INVALID_ISSUER);

    }

    @Test
    public void LogoutTokenTests_missing_required_aud() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_AUDIENCE);

    }

    @Test
    public void LogoutTokenTests_invalid_aud() throws Exception {

        genericInvalidRequiredClaimTest(Constants.PAYLOAD_AUDIENCE, "someBadValue", "");

    }

    @Test
    public void LogoutTokenTests_missing_required_iat() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);

    }

    @Test
    public void LogoutTokenTests_invalid_iat() throws Exception {

        // set an invalid iat (future date - 2/21/2035)
        genericInvalidRequiredClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, 2055696852, MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

    }

    @Test
    public void LogoutTokenTests_invalid_iat_future_beyond_clockSkew() throws Exception {

        long clockSkew = 5;
        long tomorrow = System.currentTimeMillis() / 1000 + minutesToMilliseconds(clockSkew + 5);

        genericInvalidRequiredClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, tomorrow, MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

    }

    @Test
    public void LogoutTokenTests_invalid_iat_tomorrow() throws Exception {

        long tomorrow = System.currentTimeMillis() / 1000 + hoursToMilliseconds(24);

        genericInvalidRequiredClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, tomorrow, MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

    }

    public long minutesToMilliseconds(long minutes) throws Exception {
        return minutes * 60;
    }

    public long hoursToMilliseconds(long hours) throws Exception {
        return hours * 60 * 60;
    }

    @Test
    public void LogoutTokenTests_invalid_iat_yesterday() throws Exception {

        long yesterday = System.currentTimeMillis() / 1000 - hoursToMilliseconds(24);

        genericInvalidRequiredClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, yesterday, MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

    }

    @Test
    public void LogoutTokenTests_invalid_iat_1971() throws Exception {

        // 02/21/1971
        genericInvalidRequiredClaimTest(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, 36010452, MessageConstants.CWWKS1773E_TOKEN_EXPIRED);

    }

    @Test
    public void LogoutTokenTests_missing_required_jti() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_JWTID);

    }

    @Test
    public void LogoutTokenTests_missing_required_events() throws Exception {

        genericMissingRequiredClaimTest(Constants.PAYLOAD_EVENTS);

    }

    // test really is the same as LogoutTokenTests_validTokenContent
    @Test
    public void LogoutTokenTests_include_optional_sub() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParms();

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    @Test
    public void LogoutTokenTests_missing_optional_sub_and_sid() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParms_removingOne(Constants.PAYLOAD_SUBJECT);

        List<validationData> expectations = setInvalidBackChannelLogoutRequest();
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the sub and sid claims are both missing.", MessageConstants.CWWKS1546E_BACK_CHANNEL_LOGOUT_MISSING_SUB_AND_SID_CLAIMS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    @Test
    public void LogoutTokenTests_missing_optional_sub_with_sid() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParms_removingOne_addOrOverrideOne(Constants.PAYLOAD_SUBJECT, Constants.PAYLOAD_SESSION_ID, "xx");

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    @Test
    public void LogoutTokenTests_include_optional_sid() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParms_addOrOverrideOne(Constants.PAYLOAD_SESSION_ID, "xx");

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    @Test
    public void LogoutTokenTests_missing_optional_sid() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParms_removingOne(Constants.PAYLOAD_SESSION_ID);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    @Test
    public void LogoutTokenTests_include_prohibited_nonce() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createLogoutParms_addOrOverrideOne(Constants.PAYLOAD_NONCE, "xx");

        List<validationData> expectations = setInvalidBackChannelLogoutRequest();
        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the token contained a dis-allowed nonce claim.", MessageConstants.CWWKS1549E_BACK_CHANNEL_LOGOUT_NONCE_CLAIM);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }
}
