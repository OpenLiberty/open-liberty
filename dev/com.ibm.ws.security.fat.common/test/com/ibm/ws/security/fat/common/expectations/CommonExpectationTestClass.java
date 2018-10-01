/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.expectations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.test.common.CommonTestClass;

public class CommonExpectationTestClass extends CommonTestClass {

    public static final String TEST_ACTION = "testAction";
    public static final String SEARCH_FOR_VAL = "searchForVal";
    public static final String FAILURE_MESSAGE = "This is an expectation-defined failure message.";
    public static final String ACTION1 = "action1";
    public static final String ACTION2 = "action2";
    public static final String ACTION3 = "action3";

    public static final String[] ACTIONS = new String[] { ACTION1, ACTION2, ACTION3 };

    /************************************** Helper methods **************************************/

    protected void verifyExpectationValues(Expectation testExp, String expAction, String expSearchLocation, String expCheckType, String expSearchKey, String expSearchValue, String expFailureMsg) {
        assertEquals("Test action did not match expected value.", expAction, testExp.getAction());
        assertEquals("Search location did not match expected value.", expSearchLocation, testExp.getSearchLocation());
        assertEquals("Check type did not match expected value.", expCheckType, testExp.getCheckType());
        assertEquals("Validation key did not match expected value.", expSearchKey, testExp.getValidationKey());
        assertEquals("Validation value did not match expected value.", expSearchValue, testExp.getValidationValue());
        assertEquals("Failure message did not match expected value.", expFailureMsg, testExp.getFailureMsg());
        assertFalse("New expectation should not be considered handled, but was.", testExp.isExpectationHandled());
    }

    protected void verifySuccessfulResponseStatusExpectation(Expectation expectation, String testAction) {
        String expectedStatus = Integer.toString(HttpServletResponse.SC_OK);
        String defaultMsg = "Did not receive the expected status code [" + expectedStatus + "] during test action [" + testAction + "].";

        assertEquals("Expectation action did not match expected value. Expectation was: " + expectation, testAction, expectation.getAction());
        assertEquals("Expectation search location did not match expected value. Expectation was: " + expectation, Constants.RESPONSE_STATUS, expectation.getSearchLocation());
        assertEquals("Expectation check type did not match expected value. Expectation was: " + expectation, Constants.STRING_EQUALS, expectation.getCheckType());
        assertEquals("Key to search for wasn't null but should have been.", null, expectation.getValidationKey());
        assertEquals("Value to search for did not match expected value. Expectation was: " + expectation, expectedStatus, expectation.getValidationValue());
        assertEquals("Expectation failure message did not match expected value. Expectation was: " + expectation, defaultMsg, expectation.getFailureMsg());
    }

}
