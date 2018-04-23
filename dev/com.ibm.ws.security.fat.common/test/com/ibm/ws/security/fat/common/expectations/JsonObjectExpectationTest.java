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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;

import test.common.SharedOutputManager;

public class JsonObjectExpectationTest extends CommonSpecificExpectationTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private static final String SEARCH_KEY = "searchKey";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
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

    /************************************** Constructors/getters **************************************/

    @Test
    public void test_constructor_nullArgs() {
        try {
            String testAction = null;
            String searchKey = null;
            String searchForVal = null;
            String failureMsg = null;

            JsonObjectExpectation exp = new JsonObjectExpectation(testAction, searchKey, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.JSON_OBJECT, null, searchKey, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor() {
        try {
            String testAction = "testAction";
            String searchKey = "searchKey";
            String searchForVal = "searchForVal";
            String failureMsg = "failureMsg";

            JsonObjectExpectation exp = new JsonObjectExpectation(testAction, searchKey, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.JSON_OBJECT, null, searchKey, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    protected Expectation createBasicExpectation() {
        return new JsonObjectExpectation(TEST_ACTION, SEARCH_KEY, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

}
