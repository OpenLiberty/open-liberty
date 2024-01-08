/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.commonTests;

import static org.junit.Assert.fail;

import java.util.Map;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerTraceExpectation;

import componenttest.topology.impl.LibertyServer;

public class CommonLogoutAndRefreshTests extends CommonAnnotatedSecurityTests {
    protected static String baseAppName = null;

    protected static final boolean TokenWasRefreshed = true;
    protected static final boolean TokenWasNotRefreshed = false;

    protected static final boolean NotifyProvider = true;
    protected static final boolean DoNotNotifyProvider = false;
    protected static final boolean IDTokenHonorExpiry = true;
    protected static final boolean IDTokenDoNotHonorExpiry = false;
    protected static final boolean AccessTokenHonorExpiry = true;
    protected static final boolean AccessTokenDoNotHonorExpiry = false;
    protected static final boolean PromptLogin = true;
    protected static final boolean DoNotPromptLogin = false;

    protected static final String goodRedirectUri = "goodRedirectUri";
    protected static final String goodRedirectExtraParmsUri = "goodRedirectExtraParmsUri";
    protected static final String badRedirectUri = "badRedirectUri";
    protected static final String emptyRedirectUri = "emptyRedirectUri";

    protected static final int sleepTimeInSeconds = 35;

    /**
     * Try to access a protected app, then try to access it again after the tokens expire.
     * Callers of this method:
     * - The token isn't allowed to be refreshed, or can't be refreshed tokenAutoRefresh=false, or is true, but the provider doesn't include a refresh token
     * - One of the tokens (id or access) is expired.
     * - notifyProvider is false
     * - redirectUri is empty
     * Callers using this method expect that we will successfully access the app the second time.
     * If the promptType is setup to expect a login page, the second access will need to complete the login form again.
     * Otherwise, the OP can immediately redirect to the callback.
     * This method will make sure that we access the protected app 2 times and that the id_token, access_token and refresh_token are all different in each access.
     *
     * Also make sure that we get a new token because we've gone down the re-auth path (do that by making sure that the re-auth message is in the trace)
     *
     * @param rpServer - the server whose log should be checked for the re-auth message
     * @param appName - the name of the app to invoke (the specific app will contain properties to set the @@OpenIdAuthenticationMechanismDefinition properly.
     * @param provider - the provider that the test should use - this info will be used to validate the token created
     * @param providerAllowsRefresh - flag indicating if a refresh token should be validated
     * @param promptLogin - flag indicating if the promptType is setup to expect the login page
     * @throws Exception
     */
    public void genericReAuthn(LibertyServer rpServer, String appName, String provider, boolean providerAllowsRefresh, boolean promptLogin) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);
        Page response1 = runGoodEndToEndTest(webClient, appName, baseAppName);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(sleepTimeInSeconds);
        String url = rpHttpsBase + "/" + appName + "/" + baseAppName;

        Page response2 = null;
        if (promptLogin) {
            response2 = invokeAppReturnLoginPage(webClient, url);
            response2 = processLogin(response2, Constants.TESTUSER, Constants.TESTUSERPWD, baseAppName);
        } else {
            response2 = invokeAppGetToApp(webClient, url);
        }

        if (tokensAreDifferent(response1, response2, providerAllowsRefresh, TokenWasNotRefreshed)) {
            Log.info(thisClass, _testName, "Test tokens are different");
        } else {
            fail("Test tokens were NOT different");
        }
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerTraceExpectation(rpServer, "Redirect to the OpenID Connect Provider Authentication endpoint for re-authentication", "The request did not result in a re-auth request."));
        validationUtils.validateResult(response2, expectations);

    }

    /**
     * Try to access a protected app, then try to access it again after the tokens expire.
     * Callers of this method:
     * - The token isn't allowed to be refreshed, or can't be refreshed tokenAutoRefresh=false, or is true, but the provider doesn't include a refresh token
     * - One of the tokens (id or access) is expired.
     * Callers using this method expect that we will log out (landing on the logout page)
     *
     * After the logout, make sure that we will have to log in before accessing the app again.
     *
     * @param appName - the name of the app to invoke (the specific app will contain properties to set the @@OpenIdAuthenticationMechanismDefinition properly.
     * @param provider - the provider that the test should use - this info will be used to validate the token created
     * @throws Exception
     */
    public void genericGoodLogoutTest(String appName, String provider) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);
        runGoodEndToEndTest(webClient, appName, baseAppName);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(sleepTimeInSeconds);
        String url = rpHttpsBase + "/" + appName + "/" + baseAppName;
        invokeAppReturnLogoutPage(webClient, url);

        // even though we validated that we landed on the logut successful page,
        // make sure that we need to log in again.
        invokeAppReturnLoginPage(webClient, url);

    }

    public void genericGoodLogoutAndRedirectTest(String appName, String provider, Map<String, String> extraParms) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);
        runGoodEndToEndTest(webClient, appName, baseAppName);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(35);
        String url = rpHttpsBase + "/" + appName + "/" + baseAppName;
        invokeAppReturnPostLogoutPage(webClient, url, extraParms);

        // even though we validated that we landed on the logut successful page,
        // make sure that we need to log in again.
        invokeAppReturnLoginPage(webClient, url);

    }

    public void genericGoodRedirectWithoutLogoutTest(String appName, String provider, Map<String, String> extraParms) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);
        Page response1 = runGoodEndToEndTest(webClient, appName, baseAppName);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(35);
        String url = rpHttpsBase + "/" + appName + "/" + baseAppName;
        invokeAppReturnPostLogoutPage(webClient, url, extraParms);

        Page response2 = invokeAppGetToApp(webClient, url);

        if (!accessTokensAreDifferent(response1, response2)) {
            fail("access token should have been different");
        }
        if (!idTokensAreDifferent(response1, response2)) {
            fail("id token should have been different");
        }

    }

    /**
     * Try to access a protected app, then try to access it again after the tokens expire.
     * Callers of this method:
     * - The token isn't allowed to be refreshed, or can't be refreshed tokenAutoRefresh=false, or is true, but the provider doesn't include a refresh token
     * - the token is either not expired, or the expiry for that token is not enabled
     * Callers using this method expect that we will successfully access the app the second time.
     * This method will make sure that we access the protected app 2 times and that the id_token, access_token and refresh_token are all the same for each access.
     *
     * Also make sure that we don't get a new token.
     *
     * @param rpServer - the server whose log should be checked for the re-auth message
     * @param appName - the name of the app to invoke (the specific app will contain properties to set the @@OpenIdAuthenticationMechanismDefinition properly.
     * @param provider - the provider that the test should use - this info will be used to validate the token created
     * @param providerAllowsRefresh - flag indicating if a refresh token should be validated
     * @throws Exception
     */
    public void genericGoodEndToEndReAccessTest(String appName, String provider, boolean providerAllowsRefresh) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);
        Page response1 = runGoodEndToEndTest(webClient, appName, baseAppName);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(sleepTimeInSeconds);
        String url = rpHttpsBase + "/" + appName + "/" + baseAppName;
        Page response2 = invokeAppGetToApp(webClient, url);

        if (tokensAreTheSame(response1, response2, providerAllowsRefresh)) {
            Log.info(thisClass, _testName, "Test tokens are the same");
        } else {
            fail("Test tokens were different");
        }

    }

    /**
     * Try to access a protected app, then try to access it again after the tokens expire.
     * Callers of this method:
     * - The token isn't allowed to be refreshed, or can't be refreshed tokenAutoRefresh=false, or is true, but the provider doesn't include a refresh token
     * - the token is expired
     * - notifyProvider is false
     * - a redirectURI is specified, but it is the open-liberty root
     * Callers using this method expect that we will land on the Open-Liberty splash page when the app is invoked the second time.
     *
     * @param appName - the name of the app to invoke (the specific app will contain properties to set the @@OpenIdAuthenticationMechanismDefinition properly.
     * @param provider - the provider that the test should use - this info will be used to validate the token created
     * @throws Exception
     */
    public void genericGoodSplashPage(String appName, String provider) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/" + provider);
        runGoodEndToEndTest(webClient, appName, baseAppName);

        // now logged in - wait for token to expire
        actions.testLogAndSleep(sleepTimeInSeconds);
        String url = rpHttpsBase + "/" + appName + "/" + baseAppName;
        webClient.getOptions().setJavaScriptEnabled(false);
        invokeAppGetToSplashPage(webClient, url);

    }

}
