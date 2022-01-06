/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat;

import java.security.Provider;
import java.security.Security;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.actions.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.CommonJwtssoFat;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test jwtSso with builders that create tokens using encryption. Also use
 * mpJwt configs to consume tokens created with encryption.
 * We test all possible encryption algorithms in the jwtBuilder and mpJwt FATS. Will not
 * test all possible encryption algorithms in this FAT since that's already done.
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EncryptionTests extends CommonJwtssoFat {

    protected static Class<?> thisClass = EncryptionTests.class;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private final JwtFatActions actions = new JwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();
    private static JwtFatUtils fatUtils = new JwtFatUtils();

    String protectedUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + JwtFatConstants.SIMPLE_SERVLET_PATH;
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {

        fatUtils.updateFeatureFileForEE9(server);
        transformApps(server);

        server.addInstalledAppForValidation(JwtFatConstants.APP_FORMLOGIN);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration("server_withFeature.xml", CommonWaitForAppChecks.getSSLChannelReadyMsgs());

        for (Provider p : Security.getProviders()) {
            Log.info(thisClass, "setUp", "provider: " + p.getName());
        }
    }

    @Test
    public void EncryptionTests_signWithES384_encryptWithRS256() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_signES384EncryptRS256.xml");

        WebClient webClient = actions.createWebClient();

        String builderId = "sigAlgES384EncryptRS256Builder";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        String expectedIssuer = "https://[^/]+/jwt/" + builderId;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, expectedIssuer));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME, JwtFatConstants.NOT_SECURE,
                                                                        JwtFatConstants.HTTPONLY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        verifyJwtHeaderContainsKey(jwtCookie.getValue(), "kid");
        verifyJwtHeaderContainsKeyAndValue(jwtCookie.getValue(), "alg", JwtFatConstants.SIGALG_RS384);
        actions.destroyWebClient(webClient);

    }

    @Test
    public void EncryptionTests_signWithRS512_encryptWithES256() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_signRS512EncryptES256.xml");

        WebClient webClient = actions.createWebClient();

        String builderId = "sigAlgRS512EncryptES256Builder";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        String expectedIssuer = "https://[^/]+/jwt/" + builderId;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, expectedIssuer));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME, JwtFatConstants.NOT_SECURE,
                                                                        JwtFatConstants.HTTPONLY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        verifyJwtHeaderContainsKey(jwtCookie.getValue(), "kid");
        verifyJwtHeaderContainsKeyAndValue(jwtCookie.getValue(), "alg", JwtFatConstants.SIGALG_RS384);
        actions.destroyWebClient(webClient);

    }
}
