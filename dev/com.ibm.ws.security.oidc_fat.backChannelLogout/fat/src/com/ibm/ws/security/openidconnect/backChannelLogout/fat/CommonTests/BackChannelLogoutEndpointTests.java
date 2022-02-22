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
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains
 **/

public class BackChannelLogoutEndpointTests extends BackChannelLogoutCommonTests {

    public static Class<?> thisClass = BackChannelLogoutEndpointTests.class;

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

    @Mode(TestMode.LITE)
    @Test
    public void BackChannelLogoutEndpointTests_Valid_Http_Method() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParms();

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.OK_STATUS);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.POSTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

        // TODO - if Trace should NOT work, should it be listed by options?
        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.OPTIONSMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

        // TODO - should Trace work or not - listed by options, but gets a 403
        //        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
        //                Constants.TRACEMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations, testSettings);

    }

    @Mode(TestMode.LITE)
    @Test
    public void BackChannelLogoutEndpointTests_Invalid_Http_Methods() throws Exception {

        String logutOutEndpoint = buildBackChannelLogoutUri("client01");

        WebClient webClient = getAndSaveWebClient(true);

        List<endpointSettings> parms = createDefaultLogoutParms();

        // not allowed status
        List<validationData> expectations405 = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.NOT_ALLOWED_STATUS);
        expectations405 = vData.addExpectation(expectations405, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.METHOD_NOT_ALLOWED + "\" was not found in the reponse message", null, Constants.METHOD_NOT_ALLOWED);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.GETMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations405, testSettings);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.PUTMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations405, testSettings);

        testRPServer.addIgnoredServerException(MessageConstants.SRVE8094W_CANNOT_SET_HEADER_RESPONSE_COMMITTED);
        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.HEADMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations405, testSettings);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.DELETEMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations405, testSettings);

        // not implemented status
        List<validationData> expectations501 = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.NOT_IMPLEMENTED_STATUS);
        expectations501 = vData.addExpectation(expectations501, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.METHOD_NOT_IMPLEMENTED + "\" was not found in the reponse message", null, Constants.METHOD_NOT_IMPLEMENTED);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.PATCHMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations501, testSettings);

        // not implemented status
        List<validationData> expectations403 = vData.addResponseStatusExpectation(null, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.FORBIDDEN_STATUS);
        expectations403 = vData.addExpectation(expectations403, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "\"" + Constants.METHOD_NOT_IMPLEMENTED + "\" was not found in the reponse message", null, Constants.FORBIDDEN);

        genericInvokeEndpoint(_testName, webClient, null, logutOutEndpoint,
                Constants.TRACEMETHOD, Constants.INVOKE_BACK_CHANNEL_LOGOUT_ENDPOINT, parms, null, expectations403, testSettings);

    }

}
