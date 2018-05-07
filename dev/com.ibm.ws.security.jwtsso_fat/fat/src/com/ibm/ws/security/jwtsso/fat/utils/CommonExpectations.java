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
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.jwtsso.fat.expectations.CookieExpectation;
import com.ibm.ws.security.jwtsso.fat.expectations.JwtExpectation;

public class CommonExpectations {

    protected static Class<?> thisClass = CommonExpectations.class;

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>200 status code in the response for the specified test action
     * <li>Response URL is equivalent to provided URL
     * </ol>
     */
    public static Expectations successfullyReachedUrl(String testAction, String url) {
        Expectations expectations = new Expectations();
        expectations.addSuccessStatusCodesForActions(new String[] { testAction });
        expectations.addExpectation(new ResponseUrlExpectation(testAction, JwtFatConstants.STRING_EQUALS, url, "Did not reach the expected URL."));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>200 status code in the response for the specified test action
     * <li>Response title is equivalent to expected login page title
     * </ol>
     */
    public static Expectations successfullyReachedLoginPage(String testAction) {
        Expectations expectations = new Expectations();
        expectations.addSuccessStatusCodesForActions(new String[] { testAction });
        expectations.addExpectation(new ResponseTitleExpectation(testAction, JwtFatConstants.STRING_EQUALS, "login.jsp", "Title of page returned during test step " + testAction
                                                                                                                         + " did not match expected value."));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>Successfully reached the specified URL
     * <li>JWT SSO cookie with the default name is present in the WebClient
     * <li>Response text includes JWT cookie and principal information (no LTPA cookie information)
     * </ol>
     */
    public static Expectations successfullyReachedProtectedResourceWithJwtCookie(String testAction, WebClient webClient, String protectedUrl, String username) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(successfullyReachedUrl(testAction, protectedUrl));
        expectations.addExpectations(jwtCookieExists(testAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(getResponseTextExpectationsForJwtCookie(testAction, JwtFatConstants.JWT_COOKIE_NAME, username, JwtFatConstants.BASIC_REALM));
        expectations.addExpectations(getJwtPrincipalExpectations(testAction, username, JwtFatConstants.DEFAULT_ISS_REGEX));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>Successfully reached the specified URL
     * <li>Response headers do not include JWT cookie information
     * <li>Response text includes LTPA cookie and principal information (no JWT cookie information)
     * </ol>
     */
    public static Expectations successfullyReachedProtectedResourceWithLtpaCookie(String testAction, WebClient webClient, String protectedUrl, String username) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(successfullyReachedUrl(testAction, protectedUrl));
        expectations.addExpectations(ltpaCookieExists(testAction, webClient));
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
    public static Expectations getResponseTextExpectationsForJwtCookie(String testAction, String jwtCookieName, String username, String accessIdRealmRegex) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(responseTextIncludesCookie(testAction, jwtCookieName));
        expectations.addExpectations(responseTextIncludesExpectedRemoteUser(testAction, username));
        expectations.addExpectations(responseTextIncludesJwtPrincipal(testAction));
        expectations.addExpectations(responseTextIncludesExpectedAccessId(testAction, accessIdRealmRegex, username));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>Response text includes an LTPA cookie
     * <li>Response text includes expected remote user
     * <li>Subject principal specifies the expected user
     * <li>Subject public credentials include appropriate access ID
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
    public static Expectations getJwtPrincipalExpectations(String testAction) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(getJwtPrincipalExpectations(testAction, JwtFatConstants.TESTUSER, "https?://" + "[^/]+" + JwtFatConstants.DEFAULT_ISS_CONTEXT));
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
