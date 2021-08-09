/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;

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
        expectations.addExpectation(new ResponseUrlExpectation(testAction, Constants.STRING_EQUALS, url, "Did not reach the expected URL."));
        return expectations;
    }

    /**
     * Set success for current action only
     * 
     * @param url
     * @return
     */
    public static Expectations successfullyReachedUrl(String url) {
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_EQUALS, url, "Did not reach the expected URL."));
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
        expectations.addExpectation(new ResponseTitleExpectation(testAction, Constants.STRING_EQUALS, Constants.FORM_LOGIN_TITLE, "Title of page returned during test step " + testAction
                + " did not match expected value."));
        return expectations;
    }

    public static Expectations successfullyReachedLoginPage() {
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseTitleExpectation(Constants.STRING_EQUALS, Constants.FORM_LOGIN_TITLE, "Title of page returned did not match expected value."));
        return expectations;
    }

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>200 status code in the response for the specified test action
     * <li>Response title is equivalent to expected login page title
     * </ol>
     */
    public static Expectations successfullyReachedFormLoginPage(String testAction) {
        Expectations expectations = successfullyReachedLoginPage(testAction);
        expectations.addExpectation(new ResponseFullExpectation(testAction, Constants.STRING_CONTAINS, Constants.FORM_LOGIN_HEADING, "Page returned during test step " + testAction + " did not match expected value."));
        return expectations;
    }

    public static Expectations successfullyReachedFormLoginPage() {
        Expectations expectations = successfullyReachedLoginPage();
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, Constants.FORM_LOGIN_HEADING, "Page returned did not match expected value."));
        return expectations;
    }
}
