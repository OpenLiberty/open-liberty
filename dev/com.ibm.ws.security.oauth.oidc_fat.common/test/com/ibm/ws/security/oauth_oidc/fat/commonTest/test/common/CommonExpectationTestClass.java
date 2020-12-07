/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.test.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.TestServer;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.test.common.CommonTestClass;

public class CommonExpectationTestClass extends CommonTestClass {

    public static final String ACTION1 = "action1";
    public static final String ACTION2 = "action2";
    public static final String ACTION3 = "action3";

    public static final String[] ACTIONS = new String[] { ACTION1, ACTION2, ACTION3 };

    public static final String DO_NOT_CHECK = "DoNotCheckThisValue";

    /************************************** Helper methods **************************************/

    protected boolean shouldCheckValue(String value) {
        return (value == null || !DO_NOT_CHECK.equals(value));
    }

    protected void verifyEmptyActionsList(Expectations testExps) {
        List<String> actionsList = testExps.getActionsList();
        assertTrue("Actions list should have been empty but was " + actionsList, actionsList.isEmpty());
    }

    protected void verifyEmptyExpectationsList(Expectations testExps) {
        List<Expectation> expList = testExps.getExpectations();
        assertTrue("Expectations list should have been empty but was " + expList, expList.isEmpty());
    }

    /**
     * Verifies that each of the anticipated expectations is found within the list of expectations produced by the test. If the
     * test produced an expectation that is not specified in expExpectations, this method will cause a failure.
     */
    protected void verifyAllAnticipatedExpectationsArePresent(Expectations exps, List<ExpectedExpectation> expExpectations) throws Exception {
        List<Expectation> expListCopy = new ArrayList<Expectation>(exps.getExpectations());
        for (ExpectedExpectation ee : expExpectations) {
            int i = 0;
            while (i < expListCopy.size()) {
                Expectation checkExpectation = expListCopy.get(i);
                try {
                    // Check if the current test expectation matches the current expected expectation
                    verifyExpectationValues(checkExpectation, ee.expectedAction, ee.expectedServerXml, ee.expectedSearchLocation, ee.expectedCheckType,
                                            ee.expectedSearchKey, ee.expectedSearchValue, ee.expectedFailureMsg);

                    // This expectation matches the expected expectation; remove from the list so we know it's been handled
                    expListCopy.remove(i);
                } catch (AssertionError e) {
                    // Keep this expectation in the list to check later
                    i++;
                }
            }
        }
        if (expListCopy.size() > 0) {
            fail("There was at least one expectation created by the method under test that was not accounted for: " + expListCopy);
        }
    }

    protected void verifyExpectationValues(Expectation testExp, String expAction, String expServerXml, String expSearchLocation, String expCheckType, String expSearchKey,
                                           String expSearchValue, String expFailureMsg) throws Exception {
        if (shouldCheckValue(expAction)) {
            assertEquals("Expectation's test action did not match expected value.", expAction, testExp.getAction());
        }
        if (shouldCheckValue(expSearchLocation)) {
            assertEquals("Expectation's search location did not match expected value.", expSearchLocation, testExp.getSearchLocation());
        }
        if (shouldCheckValue(expCheckType)) {
            assertEquals("Expectation's check type did not match expected value.", expCheckType, testExp.getCheckType());
        }
        if (shouldCheckValue(expSearchKey)) {
            assertEquals("Expectation's validation key did not match expected value.", expSearchKey, testExp.getValidationKey());
        }
        if (shouldCheckValue(expSearchValue)) {
            assertEquals("Expectation's validation value did not match expected value.", expSearchValue, testExp.getValidationValue());
        }
        if (shouldCheckValue(expFailureMsg)) {
            assertEquals("Expectation's failure message did not match expected value.", expFailureMsg, testExp.getFailureMsg());
        }
        assertFalse("New expectation should not be considered handled, but was.", testExp.isExpectationHandled());

        if (shouldCheckValue(expServerXml)) {
            verifyExpectationTestServerValues(testExp, expServerXml);
        }
    }

    protected void verifyExpectationTestServerValues(Expectation testExp, String expServerXml) throws Exception {
        TestServer serverResult = testExp.getServerRef();
        if (expServerXml != null) {
            assertEquals("Server config did not match expected value.", expServerXml, (serverResult == null ? null : serverResult.getServerXml()));
        } else {
            assertNull("Server reference should have been null but was [" + serverResult + "].", serverResult);
        }
    }

    protected void verifySuccessfulResponseStatusExpectation(Expectation expectation, String testAction) {
        String expectedStatus = Integer.toString(Constants.OK_STATUS);
        String defaultMsg = "Did not receive the expected status code [" + expectedStatus + "] during test action [" + testAction + "].";

        assertEquals("Expectation action did not match expected value. Expectation was: " + expectation, testAction, expectation.getAction());
        assertEquals("Expectation server reference wasn't null but should have been.", null, expectation.getServerRef());
        assertEquals("Expectation search location did not match expected value. Expectation was: " + expectation, Constants.RESPONSE_STATUS, expectation.getSearchLocation());
        assertEquals("Expectation check type did not match expected value. Expectation was: " + expectation, Constants.STRING_EQUALS, expectation.getCheckType());
        assertEquals("Key to search for wasn't null but should have been.", null, expectation.getValidationKey());
        assertEquals("Value to search for did not match expected value. Expectation was: " + expectation, expectedStatus, expectation.getValidationValue());
        assertEquals("Expectation failure message did not match expected value. Expectation was: " + expectation, defaultMsg, expectation.getFailureMsg());
    }

}
