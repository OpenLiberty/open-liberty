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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;

import test.common.SharedOutputManager;

public class ExpectationsTest extends CommonExpectationTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    Expectations expectations = new Expectations(ACTIONS);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        expectations = new Expectations(ACTIONS);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** Constructor **************************************/

    @Test
    public void test_constructor_nullArg() {
        try {
            expectations = new Expectations(null);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was " + expList, expList.isEmpty());
            List<String> actionsList = expectations.getActionsList();
            assertTrue("Actions list should have been empty but was " + actionsList, actionsList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_emptyArray() {
        try {
            expectations = new Expectations(new String[0]);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was " + expList, expList.isEmpty());
            List<String> actionsList = expectations.getActionsList();
            assertTrue("Actions list should have been empty but was " + actionsList, actionsList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_singleEntryArray() {
        try {
            expectations = new Expectations(new String[] { ACTION1 });

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was " + expList, expList.isEmpty());
            List<String> actionsList = expectations.getActionsList();
            assertEquals("Actions list should have one entry (" + ACTION1 + ") but was " + actionsList, 1, actionsList.size());
            assertEquals("Actions list entry did not match expected action. Action list was: " + actionsList, ACTION1, actionsList.get(0));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_multipleEntryArray() {
        try {
            expectations = new Expectations(ACTIONS);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was " + expList, expList.isEmpty());
            List<String> actionsList = expectations.getActionsList();
            assertEquals("Actions list did not match expected value.", Arrays.asList(ACTIONS), actionsList);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addExpectation **************************************/

    @Test
    public void test_addExpectation_nullArg() {
        try {
            Expectation e = null;
            expectations.addExpectation(e);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectation_addOne() {
        try {
            String testAction = ACTION1;
            Expectation e = new ResponseFullExpectation(testAction, null, null, null);
            expectations.addExpectation(e);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            assertEquals("Expectation action did not match expected value. Expectation was: " + compareE, testAction, compareE.getAction());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectation_addDuplicate() {
        try {
            String testAction = ACTION1;
            Expectation e = new ResponseHeaderExpectation(testAction, null, null, null);
            expectations.addExpectation(e);
            expectations.addExpectation(e);
            expectations.addExpectation(e);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 3, expList.size());
            Expectation compareE = expList.get(0);
            assertEquals("Expectation action did not match expected value. Expectation was: " + compareE, testAction, compareE.getAction());
            compareE = expList.get(1);
            assertEquals("Expectation action did not match expected value. Expectation was: " + compareE, testAction, compareE.getAction());
            compareE = expList.get(2);
            assertEquals("Expectation action did not match expected value. Expectation was: " + compareE, testAction, compareE.getAction());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectation_addMultiple() {
        try {
            Expectation e1 = new ResponseHeaderExpectation(ACTION1, null, null, null);
            Expectation e2 = new ResponseUrlExpectation(ACTION2, null, null, null);
            Expectation e3 = null;
            Expectation e4 = new ResponseFullExpectation(ACTION3, null, null, null);
            expectations.addExpectation(e1);
            expectations.addExpectation(e2);
            expectations.addExpectation(e3);
            expectations.addExpectation(e4);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, ACTIONS.length, expList.size());
            Expectation compareE = expList.get(0);
            assertEquals("First expectation action did not match expected value. Expectation was: " + compareE, ACTION1, compareE.getAction());
            compareE = expList.get(1);
            assertEquals("Second expectation action did not match expected value. Expectation was: " + compareE, ACTION2, compareE.getAction());
            compareE = expList.get(2);
            assertEquals("Third expectation action did not match expected value. Expectation was: " + compareE, ACTION3, compareE.getAction());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addExpectations **************************************/

    @Test
    public void test_addExpectations_noKnownExpectations_nullArg() {
        try {
            Expectations exps = null;

            expectations.addExpectations(exps);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectations_noKnownExpectations_addEmptyNewExpectations() {
        try {
            Expectations exps = new Expectations();

            expectations.addExpectations(exps);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectations_noKnownExpectations_addSingleNewExpectation() {
        try {
            Expectations exps = new Expectations();
            Expectation e1 = new ResponseTitleExpectation(ACTION1, null, null, null);
            exps.addExpectation(e1);

            expectations.addExpectations(exps);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            assertEquals("First expectation action did not match expected value. Expectation was: " + compareE, ACTION1, compareE.getAction());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectations_noKnownExpectations_addMultipleNewExpectations() {
        try {
            Expectations exps = new Expectations();
            Expectation e1 = new ResponseTitleExpectation(ACTION1, null, null, null);
            Expectation e2 = new ResponseFullExpectation(null, null, null, null);
            Expectation e3 = null;
            Expectation e4 = new ResponseFullExpectation(ACTION3, null, null, null);
            exps.addExpectation(e1);
            exps.addExpectation(e2);
            exps.addExpectation(e3);
            exps.addExpectation(e4);

            expectations.addExpectations(exps);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 3, expList.size());
            Expectation compareE = expList.get(0);
            assertEquals("First expectation action did not match expected value. Expectation was: " + compareE, ACTION1, compareE.getAction());
            compareE = expList.get(1);
            assertNull("Second expectation action should have been null but was not. Expectation was: " + compareE, compareE.getAction());
            compareE = expList.get(2);
            assertEquals("Third expectation action did not match expected value. Expectation was: " + compareE, ACTION3, compareE.getAction());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectations_oneKnownExpectation_nullArg() {
        try {
            // Add one known expectation
            Expectation e1 = new ResponseFullExpectation(ACTION1, null, null, null);
            expectations.addExpectation(e1);

            Expectations exps = null;
            expectations.addExpectations(exps);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            assertEquals("First expectation action did not match expected value. Expectation was: " + compareE, ACTION1, compareE.getAction());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectations_oneKnownExpectation_addEmptyNewExpectations() {
        try {
            // Add one known expectation
            Expectation e1 = new ResponseStatusExpectation(ACTION1, null, null, null);
            expectations.addExpectation(e1);

            Expectations exps = new Expectations();
            expectations.addExpectations(exps);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            assertEquals("First expectation action did not match expected value. Expectation was: " + compareE, ACTION1, compareE.getAction());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectations_oneKnownExpectation_addOneNewExpectations() {
        try {
            // Add one known expectation
            expectations.addExpectation(new ResponseMessageExpectation(ACTION1, null, null, null));

            // Action for the new expectation shouldn't matter - it should be added as a new expectation
            String testAction = ACTION2;
            Expectations exps = new Expectations();
            exps.addExpectation(new ResponseFullExpectation(testAction, "checkType", "searchFor", "failureMsg"));

            expectations.addExpectations(exps);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 2, expList.size());
            // First expectation should match the original known expectation
            Expectation compareE = expList.get(0);
            assertEquals("First expectation action did not match expected value. Expectation was: " + compareE, ACTION1, compareE.getAction());
            assertEquals("First expectation search location did not match expected value. Expectation was: " + compareE, Constants.RESPONSE_MESSAGE, compareE.getSearchLocation());
            // Second expectation should match the new expectation that was added
            compareE = expList.get(1);
            assertEquals("Second expectation action did not match expected value. Expectation was: " + compareE, testAction, compareE.getAction());
            assertEquals("Second expectation search location did not match expected value. Expectation was: " + compareE, Constants.RESPONSE_FULL, compareE.getSearchLocation());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addExpectations_multipleKnownExpectation_addMultipleNewExpectations() {
        try {
            // Add some known expectations
            expectations.addExpectation(new ResponseTitleExpectation(ACTION1, "e1-checkType", "e1-searchFor", "e1-failureMsg"));
            expectations.addExpectation(new ResponseFullExpectation(ACTION2, "e2-checkType", "e2-searchFor", "e2-failureMsg"));
            expectations.addExpectation(new ResponseFullExpectation(null, "e2-checkType", "e2-searchFor-2", "e2-failureMsg-2"));

            // Create some new expectations
            Expectations exps = new Expectations();
            exps.addExpectation(new ResponseFullExpectation(ACTION1, "ne1-checkType", "ne1-searchFor", "ne1-failureMsg"));
            exps.addExpectation(new ResponseStatusExpectation(ACTION3, "ne3-checkType", "ne3-searchFor", "ne3-failureMsg"));

            expectations.addExpectations(exps);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 5, expList.size());
            // First expectations should match the original known expectations
            Expectation compareE = expList.get(0);
            assertEquals("First known expectation action did not match expected value. Expectation was: " + compareE, ACTION1, compareE.getAction());
            assertEquals("First known expectation search location did not match expected value. Expectation was: " + compareE, Constants.RESPONSE_TITLE, compareE.getSearchLocation());
            compareE = expList.get(1);
            assertEquals("Second known expectation action did not match expected value. Expectation was: " + compareE, ACTION2, compareE.getAction());
            assertEquals("Second known expectation search location did not match expected value. Expectation was: " + compareE, Constants.RESPONSE_FULL, compareE.getSearchLocation());
            assertEquals("Second known expectation search location did not match expected value. Expectation was: " + compareE, "e2-searchFor", compareE.getValidationValue());
            compareE = expList.get(2);
            assertNull("Third known expectation action should have been null but wasn't. Expectation was: " + compareE, compareE.getAction());
            assertEquals("Third known expectation search location did not match expected value. Expectation was: " + compareE, Constants.RESPONSE_FULL, compareE.getSearchLocation());
            assertEquals("Third known expectation search location did not match expected value. Expectation was: " + compareE, "e2-searchFor-2", compareE.getValidationValue());
            // Other expectations should match the new expectations that were added
            compareE = expList.get(3);
            assertEquals("First new expectation action did not match expected value. Expectation was: " + compareE, ACTION1, compareE.getAction());
            assertEquals("First new expectation search location did not match expected value. Expectation was: " + compareE, Constants.RESPONSE_FULL, compareE.getSearchLocation());
            compareE = expList.get(4);
            assertEquals("Second new expectation action did not match expected value. Expectation was: " + compareE, ACTION3, compareE.getAction());
            assertEquals("Second new expectation search location did not match expected value. Expectation was: " + compareE, Constants.RESPONSE_STATUS, compareE.getSearchLocation());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addSuccessStatusCodes **************************************/

    @Test
    public void test_addSuccessStatusCodes_noTestActions() {
        try {
            expectations = new Expectations(null);

            expectations.addSuccessStatusCodes();

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_emptyTestActions() {
        try {
            expectations = new Expectations(new String[0]);

            expectations.addSuccessStatusCodes();

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_singleTestAction() {
        try {
            expectations = new Expectations(new String[] { ACTION1 });

            expectations.addSuccessStatusCodes();

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            verifySuccessfulResponseStatusExpectation(compareE, ACTION1);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_singleNullTestAction() {
        try {
            expectations = new Expectations(new String[] { null });

            expectations.addSuccessStatusCodes();

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            verifySuccessfulResponseStatusExpectation(compareE, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_multipleTestActions() {
        try {
            expectations.addSuccessStatusCodes();

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, ACTIONS.length, expList.size());
            verifySuccessfulResponseStatusExpectation(expList.get(0), ACTION1);
            verifySuccessfulResponseStatusExpectation(expList.get(1), ACTION2);
            verifySuccessfulResponseStatusExpectation(expList.get(2), ACTION3);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_exceptAction_noTestActions() {
        try {
            expectations = new Expectations(null);

            expectations.addSuccessStatusCodes(ACTION1);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_exceptAction_emptyTestActions() {
        try {
            expectations = new Expectations(new String[0]);

            expectations.addSuccessStatusCodes(ACTION1);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_exceptAction_singleNullTestAction() {
        try {
            expectations = new Expectations(new String[] { null });

            expectations.addSuccessStatusCodes(ACTION1);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            verifySuccessfulResponseStatusExpectation(compareE, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_exceptAction_singleTestAction_actionMatches() {
        try {
            expectations = new Expectations(new String[] { ACTION1 });

            expectations.addSuccessStatusCodes(ACTION1);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_exceptAction_singleTestAction_actionDoesNotMatch() {
        try {
            expectations = new Expectations(new String[] { ACTION1 });

            // "Except action" doesn't match the test action
            expectations.addSuccessStatusCodes(ACTION2);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            verifySuccessfulResponseStatusExpectation(compareE, ACTION1);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_exceptAction_singleTestAction_actionIsSubstring() {
        try {
            final String action = ACTION1 + "extra";
            expectations = new Expectations(new String[] { action });

            // "Except action" is a substring of one of the test actions
            expectations.addSuccessStatusCodes(ACTION1);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            verifySuccessfulResponseStatusExpectation(compareE, action);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_exceptAction_multipleTestActions_exceptActionNotIncluded() {
        try {
            // Give some action that isn't in the default list of test actions
            expectations.addSuccessStatusCodes("some other action");

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, ACTIONS.length, expList.size());
            verifySuccessfulResponseStatusExpectation(expList.get(0), ACTION1);
            verifySuccessfulResponseStatusExpectation(expList.get(1), ACTION2);
            verifySuccessfulResponseStatusExpectation(expList.get(2), ACTION3);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodes_exceptAction_multipleTestActions() {
        try {
            expectations.addSuccessStatusCodes(ACTION2);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, ACTIONS.length - 1, expList.size());
            verifySuccessfulResponseStatusExpectation(expList.get(0), ACTION1);
            verifySuccessfulResponseStatusExpectation(expList.get(1), ACTION3);
            // There should not be an expectation for ACTION2

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addSuccessStatusCodesForActions **************************************/

    @Test
    public void test_addSuccessStatusCodesForActions_nullArg() {
        try {
            expectations.addSuccessStatusCodesForActions(null);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_emptyArray() {
        try {
            expectations.addSuccessStatusCodesForActions(new String[0]);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_singleEntryArray() {
        try {
            expectations.addSuccessStatusCodesForActions(new String[] { ACTION1 });

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            verifySuccessfulResponseStatusExpectation(compareE, ACTION1);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_multipleTestActions() {
        try {
            expectations.addSuccessStatusCodesForActions(ACTIONS);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, ACTIONS.length, expList.size());
            verifySuccessfulResponseStatusExpectation(expList.get(0), ACTION1);
            verifySuccessfulResponseStatusExpectation(expList.get(1), ACTION2);
            verifySuccessfulResponseStatusExpectation(expList.get(2), ACTION3);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_noTestActions() {
        try {
            expectations.addSuccessStatusCodesForActions(ACTION1, (String[]) null);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_emptyTestActions() {
        try {
            expectations.addSuccessStatusCodesForActions(ACTION1, new String[0]);

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_singleTestAction_actionMatches() {
        try {
            expectations.addSuccessStatusCodesForActions(ACTION1, new String[] { ACTION1 });

            List<Expectation> expList = expectations.getExpectations();
            assertTrue("Expectations list should have been empty but was: " + expList, expList.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_singleTestAction_actionDoesNotMatch() {
        try {
            // "Except action" doesn't match the test action
            expectations.addSuccessStatusCodesForActions(ACTION2, new String[] { ACTION1 });

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            verifySuccessfulResponseStatusExpectation(compareE, ACTION1);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_singleTestAction_actionIsSubstring() {
        try {
            final String action = ACTION1 + "extra";

            // "Except action" is a substring of one of the test actions
            expectations.addSuccessStatusCodesForActions(ACTION1, new String[] { action });

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 1, expList.size());
            Expectation compareE = expList.get(0);
            verifySuccessfulResponseStatusExpectation(compareE, action);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_multipleTestActions_exceptActionNotIncluded() {
        try {
            // Give some action that isn't in the default list of test actions
            expectations.addSuccessStatusCodesForActions("some other action", ACTIONS);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, ACTIONS.length, expList.size());
            verifySuccessfulResponseStatusExpectation(expList.get(0), ACTION1);
            verifySuccessfulResponseStatusExpectation(expList.get(1), ACTION2);
            verifySuccessfulResponseStatusExpectation(expList.get(2), ACTION3);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_duplicateTestActions_exceptActionNotIncluded() {
        try {
            // Give some action that isn't in the list of test actions
            expectations.addSuccessStatusCodesForActions("some other action", new String[] { ACTION1, ACTION2, ACTION1, ACTION2 });

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 4, expList.size());
            verifySuccessfulResponseStatusExpectation(expList.get(0), ACTION1);
            verifySuccessfulResponseStatusExpectation(expList.get(1), ACTION2);
            verifySuccessfulResponseStatusExpectation(expList.get(2), ACTION1);
            verifySuccessfulResponseStatusExpectation(expList.get(3), ACTION2);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_duplicateTestActions() {
        try {
            expectations.addSuccessStatusCodesForActions(ACTION1, new String[] { ACTION1, ACTION2, ACTION1, ACTION2 });

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, 2, expList.size());
            verifySuccessfulResponseStatusExpectation(expList.get(0), ACTION2);
            verifySuccessfulResponseStatusExpectation(expList.get(1), ACTION2);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addSuccessStatusCodesForActions_exceptAction_multipleTestActions() {
        try {
            expectations.addSuccessStatusCodesForActions(ACTION2, ACTIONS);

            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list size did not match expected value. Expectations list was: " + expList, ACTIONS.length - 1, expList.size());
            verifySuccessfulResponseStatusExpectation(expList.get(0), ACTION1);
            verifySuccessfulResponseStatusExpectation(expList.get(1), ACTION3);
            // There should not be an expectation for ACTION2

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
