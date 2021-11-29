/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat.actions;

import static org.junit.Assert.assertNotNull;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

public class JwtFatActions extends TestActions {

    protected TestValidationUtils validationUtils = new TestValidationUtils();

    /**
     * Accesses the protected resource and logs in successfully, ensuring that a JWT SSO cookie is included in the result.
     */
    public Cookie logInAndObtainJwtCookie(String testName, String protectedUrl, String username, String password) throws Exception {
        return logInAndObtainJwtCookie(testName, new WebClient(), protectedUrl, username, password);
    }

    /**
     * Accesses the protected resource and logs in successfully, ensuring that a JWT SSO cookie is included in the result.
     */
    public Cookie logInAndObtainJwtCookie(String testName, WebClient webClient, String protectedUrl, String username, String password) throws Exception {
        return logInAndObtainJwtCookie(testName, webClient, protectedUrl, username, password, JwtFatConstants.DEFAULT_ISS_REGEX);
    }

    /**
     * Accesses the protected resource and logs in successfully, ensuring that a JWT SSO cookie is included in the result.
     */
    public Cookie logInAndObtainJwtCookie(String testName, WebClient webClient, String protectedUrl, String username, String password, String issuerRegex) throws Exception {
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(TestActions.ACTION_INVOKE_PROTECTED_RESOURCE));
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, protectedUrl, username,
                                                                                                          issuerRegex));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, JwtFatConstants.LTPA_COOKIE_NAME));

        return logInAndObtainCookie(testName, webClient, protectedUrl, username, password, JwtFatConstants.JWT_COOKIE_NAME, expectations);
    }

    /**
     * Accesses the protected resource and logs in successfully, ensuring that an LTPA cookie is included in the result.
     */
    public Cookie logInAndObtainLtpaCookie(String testName, String protectedUrl, String username, String password) throws Exception {
        WebClient webClient = new WebClient();
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(TestActions.ACTION_INVOKE_PROTECTED_RESOURCE));
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithLtpaCookie(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, webClient, protectedUrl,
                                                                                                           username));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, JwtFatConstants.JWT_COOKIE_NAME));

        return logInAndObtainCookie(testName, webClient, protectedUrl, username, password, JwtFatConstants.LTPA_COOKIE_NAME, expectations);
    }

    /**
     * Accesses the protected resource and logs in successfully, ensuring that the expected cookie is included in the result.
     */
    private Cookie logInAndObtainCookie(String testName, WebClient webClient, String protectedUrl, String username, String password, String cookieName,
                                        Expectations expectations) throws Exception {
        Page response = invokeProtectedUrlAndValidateResponse(testName, webClient, protectedUrl, expectations);
        doFormLoginAndValidateResponse(response, username, password, expectations);

        Cookie cookie = webClient.getCookieManager().getCookie(cookieName);
        assertNotNull("Cookie [" + cookieName + "] was null but should not have been.", cookie);
        return cookie;
    }

    private Page invokeProtectedUrlAndValidateResponse(String testName, WebClient webClient, String protectedUrl, Expectations expectations) throws Exception {
        Page response = invokeUrl(testName, webClient, protectedUrl);
        validationUtils.validateResult(response, TestActions.ACTION_INVOKE_PROTECTED_RESOURCE, expectations);
        return response;
    }

    private Page doFormLoginAndValidateResponse(Page loginPage, String username, String password, Expectations expectations) throws Exception {
        Page response = doFormLogin(loginPage, username, password);
        validationUtils.validateResult(response, TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS, expectations);
        return response;
    }

}
