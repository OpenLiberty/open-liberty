/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.backchannelLogout.fat.CommonTests;

import java.util.List;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This is the test class that will run tests to validate the proper behavior when
 * various http methods are used wit the back channel logout enddpoint.
 * THe tests will run with the OIDC and Social back channel logout endpoints
 *
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
public class HttpMethodsTests extends BackChannelLogoutCommonTests {

    @BeforeClass
    public static void setUp() throws Exception {

        currentRepeatAction = RepeatTestFilter.getRepeatActionsAsString();

        testSettings = new TestSettings();

        if (currentRepeatAction.contains(Constants.OIDC)) {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_httpMethod.xml", Constants.OIDC_RP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY,
                    Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
        } else {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", "social_server_httpMethod.xml", Constants.GENERIC_SERVER, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY,
                    Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        }
        testSettings.setFlowType(Constants.RP_FLOW);
        clientServer.addIgnoredServerException(MessageConstants.SRVE8094W_CANNOT_SET_HEADER_RESPONSE_COMMITTED);
        // methods that are not supported that are caught by the security code (instead of web container) will also log CWWKS1541E - we can just ignore that
        clientServer.addIgnoredServerException(MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR);

    }

    /**
     * Creates and populates a JWT Builder using default values. It uses that builder to generate a signed logout token.
     *
     * @return - the "list" of parms that can be added to the logout request - in this case, just one pair { "logout_token",
     *         built_logout_token}
     * @throws Exception
     */
    public List<endpointSettings> createDefaultLogoutParm() throws Exception {

        String thisMethod = "createDefaultLogoutParm";

        JWTTokenBuilder builder = setBuilderClaimsForLogoutToken();

        JWTTokenBuilder b = updateLogoutTokenBuilderWithHSASignatureSettings(builder, AlgorithmIdentifiers.HMAC_SHA256);
        Log.info(thisClass, thisMethod, "claims: " + b.getJsonClaims());

        String logoutToken = b.build();

        Log.info(thisClass, thisMethod, "logout token: " + logoutToken);
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, Constants.LOGOUT_TOKEN, logoutToken);

        return parms;
    }

    /**
     * Create and popluate a JWTBuilder with our test's default claim values.
     *
     * @return - return a default populated builder
     * @throws Exception
     */
    public JWTTokenBuilder setBuilderClaimsForLogoutToken() throws Exception {

        JWTTokenBuilder builder = new JWTTokenBuilder();
        builder.setIssuer("testIssuer"); // required
        builder.setSubject("testuser"); // optional

        builder.setAudience(testClient); // required

        builder.setIssuedAtToNow(); // required
        builder.setExpirationTimeMinutesIntheFuture(2); // required
        builder.setGeneratedJwtId(); // will ensure a unique jti for each test

        JSONObject events = new JSONObject();
        events.put(Constants.logoutEventKey, new JSONObject());
        builder.setClaim("events", events); // required

        return builder;
    }

    /***************************** Tests *****************************/

    @Test
    public void HttpMethodsTests_Valid_Http_Method() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParm();

        List<validationData> expectations200 = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.OPTIONSMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations200, testSettings);

        // Should get a 400 because there isn't a recent login session associated with the information in the logout token
        List<validationData> expectations400 = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        validationTools.addMessageExpectation(clientServer, expectations400, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message in RP log saying a recent session associated with the logout token could be found.", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR + ".*" + MessageConstants.CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations400, testSettings);
    }

    @Test
    public void HttpMethodsTests_Invalid_Http_Methods() throws Exception {

        String logutOutEndpoint = buildBackchannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParm();

        // not allowed status
        List<validationData> expectations405 = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.NOT_ALLOWED_STATUS);
        expectations405 = vData.addExpectation(expectations405, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.METHOD_NOT_ALLOWED + "\" was not found in the reponse message", null, Constants.METHOD_NOT_ALLOWED);
        logTestCaseInServerSideLogs(Constants.GETMETHOD);
        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.GETMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations405, testSettings);

        logTestCaseInServerSideLogs(Constants.PUTMETHOD);
        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.PUTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations405, testSettings);

        logTestCaseInServerSideLogs(Constants.HEADMETHOD);
        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.HEADMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations405, testSettings);

        logTestCaseInServerSideLogs(Constants.DELETEMETHOD);
        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.DELETEMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations405, testSettings);

        // not implemented status
        List<validationData> expectations501 = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.NOT_IMPLEMENTED_STATUS);
        expectations501 = vData.addExpectation(expectations501, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.METHOD_NOT_IMPLEMENTED + "\" was not found in the reponse message", null, Constants.METHOD_NOT_IMPLEMENTED);

        logTestCaseInServerSideLogs(Constants.PATCHMETHOD);
        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.PATCHMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations501, testSettings);

        // forbidden status
        List<validationData> expectations403 = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.FORBIDDEN_STATUS);
        expectations403 = vData.addExpectation(expectations403, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.METHOD_NOT_IMPLEMENTED + "\" was not found in the reponse message", null, Constants.FORBIDDEN);

        logTestCaseInServerSideLogs(Constants.TRACEMETHOD);
        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.TRACEMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations403, testSettings);

    }

}
