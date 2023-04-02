/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the class that contains tests that validate the proper behavior when using an issuer to filter/select
 * an OIDC client when using inboundPropagation.
 **/

public class JaxRSClientIssuerClaimAsFilterTests extends CommonTest {

    public static Class<?> thisClass = JaxRSClientIssuerClaimAsFilterTests.class;
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    public static final boolean clientTrulyRandomlyChosen = true;

    /**
     * Create expectations to validate that a test has invoked an app on the RP properly (good status code is returned and we've
     * landed on the test app)
     *
     * @param settings
     *            current testSettings (used to set the app and issuer to search for in the response)
     * @return expectations
     * @throws Exception
     */
    public List<validationData> setDefaultRPAppExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();

        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on helloworld in the RP", null, settings.getTestURL());
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the JWT Token printed in the app output", null, ".*\"iss\":\"" + settings.getIssuer() + "\".*");

        return expectations;
    }

    /**
     * Create expectations to validate that a test has invoked an app on the RS properly (good status code is returned and we've
     * landed on the test app)
     *
     * @param settings
     *            current testSettings (used to set the app to search for in the response)
     * @return expectations
     * @throws Exception
     */
    public List<validationData> setRSAppGoodExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on helloworld in the RPS", null, settings.getRSProtectedResource());

        return expectations;

    }

    /**
     * Create expectations to validate that a test has used the correct config in RS and has received a signature algorithm
     * mismatch failure
     *
     * @param settings
     *            current testSettings (used to search for the clientId in the messages)
     * @return expectations
     * @throws Exception
     */
    public List<validationData> setRSAppSigAlgMismatchExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH + ".*" + settings.getSignatureAlg() + ".*");
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a problem validating the JWT", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE + ".*" + settings.getClientID() + ".*");

        return expectations;

    }

    /**
     * Create expectations to validate that a test has used a client where we expect the isser to NOT match what's in the token
     * This method will be called from tests that "know/expect" a certain client to process he request or from tests that will
     * randomly select a client.
     * When we expect a specific client to be used, we'll search for that clientId in the error messages, otherwise, we'll just
     * search for the message number and a call to "selectNonFiltered" method (in the trace) to show that we did choose the app
     * randomly.
     *
     * @param settings
     *            current testSettings (used to search for the clientId in the messages)
     * @param randomClient
     *            flag indicating if we expect a random or specific client
     * @return expectations
     * @throws Exception
     */
    public List<validationData> setRSAppIssuerMismatchExpectations(TestSettings settings, boolean randomClient) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);
        // if the issuer won't match because we don't know which client will handle the request, make the check more generic
        if (randomClient) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a problem validating the JWT", MessageConstants.CWWKS1781E_TOKEN_ISSUER_NOT_TRUSTED + ".*");
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a problem validating the JWT", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE + ".*");
            // check for call to method that will "randomly" choose a config
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.TRACE_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a entry noting entry/exit of the \"selectNonFiltered\" method.", "selectNonFiltered");
        } else {
            // based on the request made and what's in the config (header args, filter) we "know" which client will handle the request, so he checks can be more specific
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a problem validating the JWT", MessageConstants.CWWKS1781E_TOKEN_ISSUER_NOT_TRUSTED + ".*" + settings.getIssuer() + ".*");
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a problem validating the JWT", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE + ".*" + settings.getClientID() + ".*");
        }
        return expectations;

    }

    /**
     * Create expectations to validate that a test has received an exception mentioning a 401 status - this will occur when the
     * oidcAuthnHint or oidc_client passed in the request header does NOT match any of the oidc client ids in the config
     *
     * @return expectations
     * @throws Exception
     */
    public List<validationData> setRSHintMismatchExpectations() throws Exception {

        // just get a 401, no error messages in the server side
        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS, "Did NOT get expected 401 exception", null, "HTTP response code: 401");

        return expectations;

    }

    /**
     * Create expectations to validate that a test has NOT used an authFilter or the issuer as the filter - it does this by
     * checking for method "selectNonFiltered" being called and logged in the RS server trace.log.
     *
     * @return expectations
     * @throws Exception
     */
    public List<validationData> setNonJWTAccessTokenExpectations() throws Exception {

        // we don't know which config will be chosen - most likely it won't be client20, but occasionally, it might be, so don't check the status
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_RS_PROTECTED_RESOURCE);
        // check for call to method that will "randomly" choose a config
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.TRACE_LOG, Constants.STRING_CONTAINS, "Client trace.log should contain a entry noting entry/exit of the \"selectNonFiltered\" method.", "selectNonFiltered");

        return expectations;
    }

    /**
     * Creates a copy of the testSettings and updates settings based on the values passed in.
     *
     * @param settings
     *            The current test settings - that will be copied and the copy will be updated and returned
     * @param providerOrIssuer
     *            The provider or issuer to use to update the issuer value in test settings
     * @param client
     *            The client to update the clientId value in test settings
     * @param appNames
     *            The name of the app to use to build and update the RP and RS test app names
     * @return an updated copy of testSettings
     * @throws Exception
     */
    public TestSettings updateTestSettings(TestSettings settings, String providerOrIssuer, String client, String appNames) throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID(client);
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/helloworld/rest/" + appNames);
        if (providerOrIssuer.startsWith("Provider")) {
            updatedTestSettings.setIssuer(testOPServer.getHttpString() + "/oidc/endpoint/" + providerOrIssuer);
        } else {
            updatedTestSettings.setIssuer(providerOrIssuer);
        }

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/" + appNames);

        return updatedTestSettings;
    }

    /**
     * Build and return a hashmap of header key/value pairs
     * 1) authorization header (with BEARER access_token)
     * 2) oidcAuthnHint or oidc_client and the clientId that we intend to use
     * Log a "sub-test" message in the test and server logs to put a mark between separate attempts within one test
     *
     * @param settings
     *            the current test settings used to provider the header name to use for the access_token
     * @param access_token
     *            the access_token to pass
     * @param hintKey
     *            the key to use to pass the clientId
     * @param client
     *            the clientId to pass
     * @return the hashmap of header values
     * @throws Exception
     */
    public HashMap<String, String[]> buildParms(TestSettings settings, String access_token, String hintKey, String client) throws Exception {

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + hintKey + ":" + client, "Starting TEST ");

        HashMap<String, String[]> headers = new HashMap<String, String[]>();
        headers.put(settings.getHeaderName(), new String[] { Constants.BEARER + " " + access_token });
        headers.put(hintKey, new String[] { client });

        return headers;

    }

    /**
     * Tests that are validating the behavior with oidcAuthnHint or oidc_client in the header use this common method to attempt
     * access using the specified header key. Tests attempt access with the passed client as well as a client that does not exist
     * The caller passes in the expectations that should validate the responses when this method uses the passed client and builds
     * its own excpecations in the case where it uses a client that does not exist in the config.
     *
     * @param settings
     *            the testSettings to use to make the request
     * @param access_token
     *            the access_token to use to access a proected app
     * @param rsClient
     *            the id of the RS client to use with the hint key
     * @param headerMatchesExpectations
     *            the expectations to use when the header hint key uses the client passed (not an invalid clientId)
     * @throws Exception
     */
    public void invokeRSProtectedResourceWithExtraHeaders(TestSettings settings, String access_token, String rsClient, List<validationData> headerMatchesExpectations) throws Exception {

        HashMap<String, String[]> headers;
        // use oidcAuthnHint with the proper client within the RS config in the request header
        headers = buildParms(settings, access_token, Constants.PROVIDER_HINT, rsClient);
        helpers.invokeRSProtectedResource(_testName, new WebConversation(), Constants.POSTMETHOD, headers, null, settings, headerMatchesExpectations);

        // use oidcAuthnHint with some unknown client (not in) the RS config in the request header
        headers = buildParms(settings, access_token, Constants.PROVIDER_HINT, "ClientThatDoesNotExist");
        helpers.invokeRSProtectedResource(_testName, new WebConversation(), Constants.POSTMETHOD, headers, null, settings, setRSHintMismatchExpectations());

        // use oidc_client with the proper client within the RS config in the request header
        headers = buildParms(settings, access_token, Constants.OIDC_CLIENT, rsClient);
        helpers.invokeRSProtectedResource(_testName, new WebConversation(), Constants.POSTMETHOD, headers, null, settings, headerMatchesExpectations);

        // use oidc_client with some unknown client (not in) the RS config in the request header
        headers = buildParms(settings, access_token, Constants.OIDC_CLIENT, "ClientThatDoesNotExist");
        helpers.invokeRSProtectedResource(_testName, new WebConversation(), Constants.POSTMETHOD, headers, null, settings, setRSHintMismatchExpectations());

    }

    /**
     * Obtains an access_token by accessing an OIDC protected app on the RP server. It returns the access_token that it finds in
     * the app response.
     *
     * @param settings
     *            the testSettings to use to invoke the RS protecte dapp
     * @return the access_token created to access the RP protected app
     * @throws Exception
     */
    public String getAccessToken(TestSettings settings) throws Exception {

        WebConversation wc = new WebConversation();

        List<validationData> expectations = setDefaultRPAppExpectations((settings));

        WebResponse response = genericRP(_testName, wc, settings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

        String access_token = validationTools.getTokenFromResponse(response, "Access Token:");
        Log.info(thisClass, _testName, "access_token: " + access_token);

        return access_token;
    }

    /**
     * Tests that we can access a protected app after the RS server determines which openidConnectClient to use to grant access.
     * The openidConnectClient is selected based on the issuerIdentifier matching what's in the access_token - the config does not
     * use a filter
     */
    @Mode(TestMode.LITE)
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_BasicTest() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider01", "client01", "helloworld_useClient01");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * Test is very similar to JaxRSClientIssuerClaimAsFilterTests_BasicTest, it just uses a different provider/config - shows
     * that with multiple configs we select the correct one
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_BasicTestAnother() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider02", "client02", "helloworld_useClient02");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * This is another test using issuer as filter in a config with multiple clients using issuer as filter.
     * The difference in this test is that the client that should match the issuer uses a different signature algorithm so the
     * test
     * expects a failure.
     * We're doing this to prove that we really used the correct client.
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_BasicTestAnotherButSigAlgMismatch() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider03", "client03", "helloworld_useClient03");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppSigAlgMismatchExpectations(updatedTestSettings));

    }

    /**
     * Test is very similar to JaxRSClientIssuerClaimAsFilterTests_BasicTest, it uses a different provider/config, but this config
     * does NOT specify an issuerIdentifer, it uses the runtime generated default value
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_IssuerMatchesClientsDefaultIssuer() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider04", "client04", "helloworld_useClient04");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * Test shows that we can have multiple clients using tokens from one provider
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_MultipleClient_oneUsingFilter_oneUsingIssuerAsFilter() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider05", "client05", "helloworld_useClient05");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        // TODO - his call could end up using RSclient05_authFilter or RSclient05_issuerAsFilter - This part of the test may need to be removed
        // (can't add a filter to select based on NotContain as it'll catch all other requests...)
        msgUtils.printMethodName(_testName + " Step/Sub-Test " + "RSclient05_authFilter", "Starting TEST ");
        // invoke the app that should cause the client using an authFilter to process the request
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppGoodExpectations(updatedTestSettings));

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_issuer_useClient05");

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + "RSclient05_issuerAsFilter", "Starting TEST ");
        // invoke the app that should cause the client NOT using an auth filter to process the request - this client has a sig alg mismatch (on purpose)
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppSigAlgMismatchExpectations(updatedTestSettings));

    }

    /**
     * Test shows that we'll randomly choose an oidcConnectClient config (RSClient06 does NOT exist) in the RS.
     * Do a general check for an issuer mismatch failure as we don't know which config we'll use, we can't be too specific with
     * the error message that we look for
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_NoClientMatchesIssuerOrFilter() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider06", "client06", "helloworld_useClient06");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        // no filter catches request, so, we'll get a "random" client - because of that, the issuer should not match
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppIssuerMismatchExpectations(updatedTestSettings, clientTrulyRandomlyChosen));

    }

    /**
     * Test shows that the runtime will match the configured issuer when the configured authFilter doesn't match the app (typo in
     * the urlPattern of the filter)
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_FilterContentTypo_issuerMatches() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider07", "client07", "helloworld_useClient07");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * Test shows that the runtime will match the configured issuer when the configured authFilter doesn't exist
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_FilterRefTypo_issuerMatches() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider09", "client09", "helloworld_useClient09");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * Test when using mulitple oidcConnectClients that all use the same provider - that provider specifies a non-default issuer
     * (default would be something like: (http://localhost:8010/oidc/endpoint/Provider08").
     * Test uses oidcClientConnect clients that specify:
     * RSclient08a - filter matches request, config does NOT specify an issuer, so, the "default" will be used - call receives a
     * 401 status due to the incorrect issuer
     * RSclient08b - filter matches request, config specifies an issuer with a value that is not what the OP uses - call receives
     * a 401 status due to the incorrect issuer
     * RSclient08c - filter matches request, config specifies an issuer with the same value that the OP uses - access to the app
     * will be granted
     * RSclient08d - no filter and the config specifies an issuer with the same value that the OP uses - access to the app will be
     * granted
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_NonDefaultIssuer() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "uniqueIssuer08", "client08", "helloworld_useClient08");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        List<validationData> goodExpectations = setRSAppGoodExpectations(updatedTestSettings);
        List<validationData> badIssuerExpectations = setRSAppIssuerMismatchExpectations(updatedTestSettings, !clientTrulyRandomlyChosen);

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + "RSclient08a", "Starting TEST ");
        // fail default issuer doesn't match non-default OP issuer
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08a");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, badIssuerExpectations);

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + "RSclient08b", "Starting TEST ");
        // fail bad specified issuer
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08b");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, badIssuerExpectations);

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + "RSclient08c", "Starting TEST ");
        // pass - good filter and issuer - client RSclient08c should handle
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08c");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, goodExpectations);

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + "RSclient08d", "Starting TEST ");
        //pass filter on good issuer (no filter)  - client RSclient08c or RSclient08d could actually be selected to handle this
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08d");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, goodExpectations);

    }

    /**
     * These tests make requests to access a protected app including either oidcAuthnHint or oidc_client in the header with either
     * the oidcConnectClient id of the config that we want to use or an id that does not exist.
     * This test verifies the correct response/behavior with the issuer as filter updates - we need to ensure no change in
     * existing behavior (basically, with the header values, we fail if we can't find what they specify and not match on the
     * issuer)
     *
     */

    /**
     * oidcConnectClient does not use an authFilter and does not specify an issuer (default matches what the OP provides)
     *
     * When the oidcAuthnHint or oidc_client value matches the config: Expect access to the app
     * When the oidcAuthnHint or oidc_client value does NOT match the config: Expect an exception with a 401 msg
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_oidcAuthnHintAndOidcClientVariations_noAuthFilter_defaultIssuerMatches() throws Exception {
        // client04

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider04", "client04", "helloworld_useClient04");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        // try to access an app on the RS using the access_token we just created - we'll use the oidcAuthnHint and oidc_client header args with the RS client value,
        // or some unknown client to test the interaction of the header args and the issuer as filter code
        invokeRSProtectedResourceWithExtraHeaders(updatedTestSettings, access_token, "RSClient04", setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * oidcConnectClient does not use an authFilter and specifies an issuer that the OP does NOT provide.
     *
     * When the oidcAuthnHint or oidc_client value matches the config: Expect an issuer mismatch (401)
     * When the oidcAuthnHint or oidc_client value does NOT match the config: Expect an exception with a 401 msg
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_oidcAuthnHintAndOidcClientVariations_noAuthFilter_defaultIssuerDoesNotMatch() throws Exception {

        // client8e

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "uniqueIssuer08", "client08", "helloworld_useClient08");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        // Override RS protected app - RS will use one of multiple configs that use Client08 in the OP
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08e");

        // try to access an app on the RS using the access_token we just created - we'll use the oidcAuthnHint and oidc_client header args with the RS client value,
        // or some unknown client to test the interaction of the header args and the issuer as filter code
        invokeRSProtectedResourceWithExtraHeaders(updatedTestSettings, access_token, "RSClient08e", setRSAppIssuerMismatchExpectations(updatedTestSettings, !clientTrulyRandomlyChosen));

    }

    /**
     * oidcConnectClient does not use an authFilter and specifies the issuer that the OP provides.
     *
     * When the oidcAuthnHint or oidc_client value matches the config: Expect access to the app
     * When the oidcAuthnHint or oidc_client value does NOT match the config: Expect an exception with a 401 msg
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_oidcAuthnHintAndOidcClientVariations_noAuthFilter_specifiedIssuerMatches() throws Exception {
        // client8d

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "uniqueIssuer08", "client08", "helloworld_useClient08");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08d");

        // try to access an app on the RS using the access_token we just created - we'll use the oidcAuthnHint and oidc_client header args with the RS client value,
        // or some unknown client to test the interaction of the header args and the issuer as filter code
        invokeRSProtectedResourceWithExtraHeaders(updatedTestSettings, access_token, "RSClient08d", setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * oidcConnectClient does not use an authFilter and specifies an issuer that the OP provide does not provide
     *
     * When the oidcAuthnHint or oidc_client value matches the config: Expect an issuer mismatch (401)
     * When the oidcAuthnHint or oidc_client value does NOT match the config: Expect an exception with a 401 msg
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_oidcAuthnHintAndOidcClientVariations_noAuthFilter_specifiedIssuerDoesNotMatch() throws Exception {
        // client8f

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "uniqueIssuer08", "client08", "helloworld_useClient08");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08f");

        // try to access an app on the RS using the access_token we just created - we'll use the oidcAuthnHint and oidc_client header args with the RS client value,
        // or some unknown client to test the interaction of the header args and the issuer as filter code
        invokeRSProtectedResourceWithExtraHeaders(updatedTestSettings, access_token, "RSClient08f", setRSAppIssuerMismatchExpectations(updatedTestSettings, !clientTrulyRandomlyChosen));

    }

    /**
     * oidcConnectClient uses an authFilter and does not specify an issuer but, the default is what the OP provides.
     *
     * When the oidcAuthnHint or oidc_client value matches the config: Expect access to the app
     * When the oidcAuthnHint or oidc_client value does NOT match the config: Expect an exception with a 401 msg
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_oidcAuthnHintAndOidcClientVariations_authFilterMatches_defaultIssuerMatches() throws Exception {
        // client05

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider05", "client05", "helloworld_useClient05");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        // try to access an app on the RS using the access_token we just created - we'll use the oidcAuthnHint and oidc_client header args with the RS client value,
        // or some unknown client to test the interaction of the header args and the issuer as filter code
        invokeRSProtectedResourceWithExtraHeaders(updatedTestSettings, access_token, "RSclient05_authFilter", setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * oidcConnectClient uses an authFilter and specifies an issuer that the OP provide does not provide
     *
     * When the oidcAuthnHint or oidc_client value matches the config: Expect an issuer mismatch (401)
     * When the oidcAuthnHint or oidc_client value does NOT match the config: Expect an exception with a 401 msg
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_oidcAuthnHintAndOidcClientVariations_authFilterMatches_defaultIssuerDoesNotMatch() throws Exception {
        // client8a

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "uniqueIssuer08", "client08", "helloworld_useClient08");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08a");

        // try to access an app on the RS using the access_token we just created - we'll use the oidcAuthnHint and oidc_client header args with the RS client value,
        // or some unknown client to test the interaction of the header args and the issuer as filter code
        invokeRSProtectedResourceWithExtraHeaders(updatedTestSettings, access_token, "RSclient08a", setRSAppIssuerMismatchExpectations(updatedTestSettings, !clientTrulyRandomlyChosen));

    }

    /**
     * oidcConnectClient used an authFilter and specifies the non-default issuer that the OP provides.
     *
     * When the oidcAuthnHint or oidc_client value matches the config: Expect access to the app
     * When the oidcAuthnHint or oidc_client value does NOT match the config: Expect an exception with a 401 msg
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_oidcAuthnHintAndOidcClientVariations_authFilterMatches_specifiedIssuerMatches() throws Exception {
        // client8c

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "uniqueIssuer08", "client08", "helloworld_useClient08");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08c");

        // try to access an app on the RS using the access_token we just created - we'll use the oidcAuthnHint and oidc_client header args with the RS client value,
        // or some unknown client to test the interaction of the header args and the issuer as filter code
        invokeRSProtectedResourceWithExtraHeaders(updatedTestSettings, access_token, "RSclient08c", setRSAppGoodExpectations(updatedTestSettings));

    }

    /**
     * oidcConnectClient uses an authFilter and specifies an issuer that the OP provide does not provide
     *
     * When the oidcAuthnHint or oidc_client value matches the config: Expect an issuer mismatch (401)
     * When the oidcAuthnHint or oidc_client value does NOT match the config: Expect an exception with a 401 msg
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_oidcAuthnHintAndOidcClientVariations_authFilterMatches_specifiedIssuerDoesNotMatch() throws Exception {
        // client8b

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "uniqueIssuer08", "client08", "helloworld_useClient08");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_useClient08b");

        // try to access an app on the RS using the access_token we just created - we'll use the oidcAuthnHint and oidc_client header args with the RS client value,
        // or some unknown client to test the interaction of the header args and the issuer as filter code
        invokeRSProtectedResourceWithExtraHeaders(updatedTestSettings, access_token, "RSclient08b", setRSAppIssuerMismatchExpectations(updatedTestSettings, !clientTrulyRandomlyChosen));

    }

    /**
     * Use an opaque access_token and show that we don't choose the RS config based upon the issuer - check for a call to
     * selectNonFiltered in the RS server trace.
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_OpaqueToken_NoFilter() throws Exception {

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider20", "client20", "helloworld_useClient20");

        // log in to access a protected app on the RP - grab the access_token from the response (we'll use the access_token to test RS issuer as filter behavior)
        String access_token = getAccessToken(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), access_token, updatedTestSettings, setNonJWTAccessTokenExpectations());

    }

    /**
     * Use an JWE access_token and show that we don't choose the RS config based upon the issuer - check for a call to
     * selectNonFiltered in the RS server trace.
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientIssuerClaimAsFilterTests_JWEToken_NoFilter() throws Exception {

        String jwe_token = "eyJraWQiOiJsWVF4Q2dMNi1uN3Vzbl94bTVqdFlfRHNONFYyRDByMGxxcWhjRTZxYVZFIiwiYWxnIjoiUlNBLU9BRVAiLCJlbmMiOiJBMjU2R0NNIiwidHlwIjoiSk9TRSIsImN0eSI6Imp3dCJ9.Jltt7Ox6Jw8QNg_KheafzvLjaxQWKI8Br6pDbJ7vaczNkYy7IR69f0tYLPYUPQwWeKiuw1R1Hmv0UwWJFM0skOps6A6fO_H_9J4ZCRsponZYurCqZ-r7SE21MsPPDnxyci6nhl39HkizvfGB4FHr5U0hValoDUaf0ona9RGbJ7Rk_VcYSxpPfxtWLGLnWOunCfqQ-LI0eoQvhkJAFwPfcIpJGmC4gTIsUHXYRv1XzDzl_30-qzL4W1oALkoY_lhlwPm4uK54Fi4lHLvFH_7zG5lNnadsKUbumTLPxKaT5nllmkx3BNHSu8J4ifCwBbMQUhLE5wv478-i_ucd0aBg5ovbXVgvSyyMNiNQE4a0npIMz-SjGFWJN4QVWjSTCj2jgbN3E98oXtCmV8i2QcgR5iH3FBO30wrgQ7R9mz6IDhI3AfWvdA7UAYz0SpyKQVcC7x4OWxjQeMAd_mdxinGrWjHu5TlCyq1lQ6lg_1CYBUgMVZKA6P9pyW5yvg9OWKN6rYJ3xBaUwuqu0G86bzfY_Le5Lzue6ovil4jq6a0Pb1-FyqcINxEDGf2i7XjFR3i3iEqEMneB_iUQb-AWQRF9cidqrPZapmpW_6mkntGZRLmKKUq6WRE4rQI63fwdIxvw5Jln97gYKTq2Z64o71ksJ2RbQJCRm2FY9JkiRH-7mG4.FLJ9z5wL2AP-9TKN.dBMWIbJ4QpQVVEyQlAzyAW66QJuufORqcsG1B_EFOyqIleUuclynSFz1e981Eh5cSMS24GBULwicwkumWmBdg6hiUcM358Tg2sBzDEBYWgbHnIB8DyRdaOovwaJ9-fsJPMZmSIbpwCCTCS69QYggmmcFMxBVkzPCRYHIJC7RlKKePdH-kZy_jtg5shqepipCrXvz-GhM_LUcZz8PaiGb4fu3zjyTApkL6ji7643DnVthzQ4btdlZDPkO-KhA5wsUiG263ckX-U-TE_ZdWoo-ZzmiY57p9_ClEkhf6leuPGNtl6YFHpsRJZSdfgAAej8qW-8lPHyeAv-Nzy_vFUB_HBL2IcwlCLAMPsvYbbQCiX6Z_kS-5yPTKxPeco6B31S5MYYNmP9pmY6jxJGZTl1t_HL5-fUxwFF1lfRk12xa4AkOcXP3Yp7mVzsCN1oJcwYReCDQIDkgM-DFz5ZgPzmPJwwQJ_trjz67FcsES9iOksxHn9BuDRqEfwk5vG0fH0bQNIztsXXkBxGsApLwlmnnuWu2xDhgwJAlkTn3yNzO-UqH87_h63igjvBtXuOFNCaozEjQpbS1ieOxWr7kEtBzNQIlIWvhP34GIvinUOUczbIBrbN9F0li58g6fjRmzwcHZuZfTZP15k-r8WHx9Jcq3Q27LT5mPSJyIlY1scQ-6J1l_JTWITp7addj0MqZOkZknacxWwX1so5EcKK3IKWquC_FzuLKpqK0Os0mMTio_symEsHtB2LFpuvNYgj_P8RRYqjcntzIdUlBafls6wZ8oD0btSTZMhgAHERZe2k6UZ9pJtupkHyBOo-EyKJx2S1e1s4Emxsj0ZeZxlUK_UVInlKRDGGDJh75vL2dubWamk7wlE7Dgwj2Vl99ndNHff_hiwA6g0Xcj6QjSFFHSrTDnQ6Ikmbt6EBrURWkf4q8XWtDPZuFCIO43ofsmidl_MjfxK1cPQX2sTndNIuPbHogcI5bGJ-XRiUBN3yJYj5M5hTIJGwuwgwv9fOgVMKgosb1Dz8NC0r9KNTQag3PjlXXL87nYXwDxwT1Emsw-48LSxHoKqtDVCqctwb0qO0CWr2w5UtA7iVteudbTWTMgBuP1NWZB2bx86I_tPLV-IkCZtEl5g5AI99VZDhePM4nKSZmP2ld2iDP1OqIELfE4g1RTLeY7TBJmStVLfSdXk1HiLVjPriwkFC4AeOvxqMWiWU2vGwnfn4HDjM-DgOY4dEEx-Po8RbAo5Q6PThyqFtZJ39nQi-UmpQfQr3J8Fp4T2y-uAFrXjuGs2sZ3lX2KYo_-YehjauphdQd6svqg5EzdXfjFHpq8GgnFxsEFktso9CxgtcTuHkjK4l5QM8ddWkeKlxe5bDozHXspFoBoGoNyRkGnS8Kn1GbWwMIIzkGm8ktCIzKSOjrJY1aT1oEediLh-n7cv8VdekRzxHy4TpPu79UeDqUceaseSnj0h1OBYAUSLvkRxkI_spDbylrbtsSeYQxQcMvjsbkw5L5q_QoFzHDm5zFfp6xslf8G3JYbKKHtqHmdTN27487iq21z2_sevOe7aoFS1To9k5OtvdhCgIv-RT89HVHrD6HkfFPBaMGQxs3h04KfTz28i3enGE.uwimEzN5f9zDDG5qNQ7i9w";

        TestSettings updatedTestSettings = updateTestSettings(testSettings, "Provider21", "client21", "helloworld_useClient21");

        genericTestServer.addIgnoredServerException(MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), jwe_token, updatedTestSettings, setNonJWTAccessTokenExpectations());

    }

}
