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
package com.ibm.ws.security.jwtsso.fat.utils;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.jwtsso.fat.expectations.CookieExpectation;
import com.ibm.ws.security.jwtsso.fat.expectations.JwtExpectation;

public class CommonExpectations extends com.ibm.ws.security.fat.common.utils.CommonExpectations {

    protected static Class<?> thisClass = CommonExpectations.class;

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>Successfully reached the specified URL
     * <li>Response text includes JWT cookie and principal information
     * </ol>
     */
    public static Expectations successfullyReachedProtectedResourceWithJwtCookie(String testAction, String protectedUrl, String username) {
        return successfullyReachedProtectedResourceWithJwtCookie(testAction, protectedUrl, username, JwtFatConstants.DEFAULT_ISS_REGEX);
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>Successfully reached the specified URL
     * <li>Response text includes JWT cookie and principal information
     * </ol>
     */
    public static Expectations successfullyReachedProtectedResourceWithJwtCookie(String testAction, String protectedUrl, String username, String issuerRegex) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(successfullyReachedUrl(testAction, protectedUrl));
        expectations.addExpectations(getResponseTextExpectationsForJwtCookie(testAction, JwtFatConstants.JWT_COOKIE_NAME, username));
        expectations.addExpectations(getJwtPrincipalExpectations(testAction, username, issuerRegex));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>Successfully reached the specified URL
     * <li>Response text includes LTPA cookie and principal information
     * </ol>
     */
    public static Expectations successfullyReachedProtectedResourceWithLtpaCookie(String testAction, WebClient webClient, String protectedUrl, String username) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(successfullyReachedUrl(testAction, protectedUrl));
        expectations.addExpectations(getResponseTextExpectationsForLtpaCookie(testAction, username));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>The provided WebClient contains a cookie with the default JWT SSO cookie name, its value is a JWT, is NOT marked secure, and is marked HttpOnly
     * </ol>
     */
    public static Expectations jwtCookieExists(String testAction, WebClient webClient, String jwtCookieName) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new CookieExpectation(testAction, webClient, jwtCookieName, JwtFatConstants.JWT_REGEX, JwtFatConstants.NOT_SECURE, JwtFatConstants.HTTPONLY));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>The provided WebClient contains a cookie with the default JWT SSO cookie name, its value is a JWT, is NOT marked secure, and is marked HttpOnly
     * </ol>
     */
    public static Expectations jwtCookieExists(String testAction, WebClient webClient, String jwtCookieName, boolean isSecure, boolean isHttpOnly) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new CookieExpectation(testAction, webClient, jwtCookieName, JwtFatConstants.JWT_REGEX, isSecure, isHttpOnly));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>The provided WebClient contains an LTPA cookie with a non-empty value
     * </ol>
     */
    public static Expectations ltpaCookieExists(String testAction, WebClient webClient) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new CookieExpectation(testAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME, ".+"));
        return expectations;
    }

    public static Expectations cookieDoesNotExist(String testAction, WebClient webClient, String cookieName) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new CookieExpectation(testAction, webClient, cookieName, null));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>Response text includes a JWT SSO cookie
     * <li>Response text includes expected remote user
     * <li>Subject principal includes an appropriate JWT
     * <li>Subject public credentials include appropriate access ID
     * </ol>
     */
    public static Expectations getResponseTextExpectationsForJwtCookie(String testAction, String jwtCookieName, String username) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(responseTextIncludesCookie(testAction, jwtCookieName));
        expectations.addExpectations(responseTextIncludesExpectedRemoteUser(testAction, username));
        expectations.addExpectations(responseTextIncludesJwtPrincipal(testAction));
        expectations.addExpectations(responseTextIncludesExpectedAccessId(testAction, JwtFatConstants.BASIC_REALM, username));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>Response text includes an LTPA cookie
     * <li>Response text includes expected remote user
     * <li>Subject principal is a WSPrincipal with the expected user
     * <li>Subject public credentials include appropriate access ID with registry realm
     * </ol>
     */
    public static Expectations getResponseTextExpectationsForLtpaCookie(String testAction, String username) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(responseTextIncludesCookie(testAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(responseTextIncludesExpectedRemoteUser(testAction, username));
        expectations.addExpectations(responseTextIncludesWSPrincipal(testAction, username));
        expectations.addExpectations(responseTextIncludesExpectedAccessId(testAction, JwtFatConstants.BASIC_REALM, username));
        return expectations;
    }

    public static Expectations responseTextIncludesCookie(String testAction, String cookieName) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "cookie: " + cookieName,
                                                                          "Did not find a " + cookieName + " cookie in the response body, but should have."));
        return expectations;
    }

    public static Expectations responseTextMissingCookie(String testAction, String cookieName) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(Expectation.createResponseMissingValueExpectation(testAction, "cookie: " + cookieName,
                                                                                      "Found a " + cookieName + " cookie in the response body but shouldn't have."));
        return expectations;
    }

    public static Expectations responseTextIncludesExpectedRemoteUser(String testAction, String username) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "getRemoteUser: " + username, "Did not find expected user in the response body."));
        return expectations;
    }

    public static Expectations responseTextIncludesJwtPrincipal(String testAction) {
        Expectations expectations = new Expectations();
        String jwtPrincipalRegex = "getUserPrincipal: (\\{.+\\})";
        expectations.addExpectation(new ResponseFullExpectation(testAction, JwtFatConstants.STRING_MATCHES, jwtPrincipalRegex, "Did not find expected JWT principal regex in response content."));
        return expectations;
    }

    public static Expectations responseTextIncludesWSPrincipal(String testAction, String username) {
        Expectations expectations = new Expectations();
        String wsPrincipalString = "getUserPrincipal: WSPrincipal:" + username;
        expectations.addExpectation(new ResponseFullExpectation(testAction, JwtFatConstants.STRING_CONTAINS, wsPrincipalString, "Did not find expected WSPrincipal value in response content."));
        return expectations;
    }

    public static Expectations responseTextIncludesExpectedAccessId(String testAction, String realm, String user) {
        Expectations expectations = new Expectations();
        String accessId = "accessId=user:" + realm + "/" + user;
        expectations.addExpectation(new ResponseFullExpectation(testAction, JwtFatConstants.STRING_MATCHES, "Public Credential: .+"
                                                                                                            + accessId, "Did not find expected access ID in response content."));
        return expectations;
    }

    /**
     * Sets expectations that will check various claims within the subject JWT.
     */
    public static Expectations getJwtPrincipalExpectations(String testAction, String user, String issuer) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new JwtExpectation(testAction, "token_type", JwtFatConstants.TOKEN_TYPE_BEARER));
        expectations.addExpectation(new JwtExpectation(testAction, "sub", user));
        expectations.addExpectation(new JwtExpectation(testAction, "upn", user));
        expectations.addExpectation(new JwtExpectation(testAction, "iss", issuer));
        return expectations;
    }

}
