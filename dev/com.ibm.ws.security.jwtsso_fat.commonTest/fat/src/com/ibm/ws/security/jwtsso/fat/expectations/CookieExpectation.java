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
package com.ibm.ws.security.jwtsso.fat.expectations;

import static org.junit.Assert.assertEquals;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

public class CookieExpectation extends Expectation {

    protected static Class<?> thisClass = CookieExpectation.class;

    public static final String DEFAULT_FAILURE_MSG_COOKIE_FOUND = "The [%s] cookie was found but should not have been. Found cookies: %s";
    public static final String DEFAULT_FAILURE_MSG_COOKIE_NOT_FOUND = "The [%s] cookie was not found but should have been. Found cookies: %s";
    public static final String SEARCH_LOCATION_COOKIES = "cookies";

    private TestValidationUtils validationUtils = new TestValidationUtils();

    private WebClient webClient = null;
    private Boolean isSecure = null;
    private Boolean isHttpOnly = null;

    public CookieExpectation(String testAction, WebClient webClient, String cookieName, String expectedValue) {
        this(testAction, webClient, cookieName, expectedValue, null);
    }

    public CookieExpectation(String testAction, WebClient webClient, String cookieName, String expectedValue, boolean isSecure, boolean isHttpOnly) {
        this(testAction, webClient, cookieName, expectedValue, null);
        this.isSecure = isSecure;
        this.isHttpOnly = isHttpOnly;
    }

    public CookieExpectation(String testAction, WebClient webClient, String cookieName, String expectedValue, String failureMsg) {
        super(testAction, SEARCH_LOCATION_COOKIES, (expectedValue == null) ? Constants.STRING_NULL : Constants.STRING_MATCHES, cookieName, expectedValue, failureMsg);
        this.webClient = webClient;
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        Cookie cookie = webClient.getCookieManager().getCookie(validationKey);
        Log.info(thisClass, "validate", "Validating cookie: " + cookie);
        validateCookieValue(cookie);
        if (cookie != null) {
            validateCookieSettings(cookie);
        }
    }

    private void validateCookieValue(Cookie cookie) throws Exception {
        setFailureMessageForCookieValue(validationKey, validationValue, webClient);
        String valueToValidate = (cookie == null) ? null : cookie.getValue();
        try {
            validationUtils.validateStringContent(this, valueToValidate);
        } catch (Throwable t) {
            throw new Exception(failureMsg + " " + t);
        }
    }

    private void setFailureMessageForCookieValue(String cookieName, String expectedValue, WebClient webClient) {
        if (failureMsg != null) {
            return;
        }
        if (expectedValue == null) {
            failureMsg = String.format(DEFAULT_FAILURE_MSG_COOKIE_FOUND, cookieName, webClient.getCookieManager().getCookies());
        } else {
            failureMsg = String.format(DEFAULT_FAILURE_MSG_COOKIE_NOT_FOUND, cookieName, webClient.getCookieManager().getCookies());
        }
    }

    private void validateCookieSettings(Cookie cookie) throws Exception {
        if (isSecure != null) {
            assertEquals("Secure flag for cookie [" + cookie.getName() + "] did not match the expected value.", isSecure, cookie.isSecure());
        }
        if (isHttpOnly != null) {
            assertEquals("HttpOnly flag for cookie [" + cookie.getName() + "] did not match the expected value.", isHttpOnly, cookie.isHttpOnly());
        }
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" [ Secure: %b | HttpOnly %b ]", isSecure, isHttpOnly);
    }

}
