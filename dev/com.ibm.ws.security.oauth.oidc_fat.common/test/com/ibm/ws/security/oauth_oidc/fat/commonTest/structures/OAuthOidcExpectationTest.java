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
package com.ibm.ws.security.oauth_oidc.fat.commonTest.structures;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.test.common.CommonExpectationTestClass;

import test.common.SharedOutputManager;

public class OAuthOidcExpectationTest extends CommonExpectationTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.tooling.*=all");

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

    /************************************** createNoAccessTokenInResponseExpectation **************************************/

    @Test
    public void test_createNoAccessTokenInResponseExpectation_nullArg() {
        try {
            Expectation exp = OAuthOidcExpectation.createNoAccessTokenInResponseExpectation(null);

            verifyExpectationValues(exp, null, null, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, Constants.ACCESS_TOKEN_KEY,
                                    "Found [" + Constants.ACCESS_TOKEN_KEY + "] in the response and should not have.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createNoAccessTokenInResponseExpectation() {
        try {
            Expectation exp = OAuthOidcExpectation.createNoAccessTokenInResponseExpectation(ACTION1);

            verifyExpectationValues(exp, ACTION1, null, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, Constants.ACCESS_TOKEN_KEY,
                                    "Found [" + Constants.ACCESS_TOKEN_KEY + "] in the response and should not have.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createNoIdTokenInResponseExpectation **************************************/

    @Test
    public void test_createNoIdTokenInResponseExpectation_nullArg() {
        try {
            String action = null;
            Expectation exp = OAuthOidcExpectation.createNoIdTokenInResponseExpectation(action);

            verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, Constants.ID_TOKEN_KEY,
                                    "Found [" + Constants.ID_TOKEN_KEY + "] in the response and should not have.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createNoIdTokenInResponseExpectation() {
        try {
            String action = ACTION1;
            Expectation exp = OAuthOidcExpectation.createNoIdTokenInResponseExpectation(action);

            verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, Constants.ID_TOKEN_KEY,
                                    "Found [" + Constants.ID_TOKEN_KEY + "] in the response and should not have.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addTokenInResponseExpectation **************************************/

    @Test
    public void test_addTokenInResponseExpectation_nullArgs() {
        try {
            String action = null;
            String token = null;
            Expectation exp = OAuthOidcExpectation.addTokenInResponseExpectation(action, token);

            verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, token, "Did not find [" + token + "] in the response.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addTokenInResponseExpectation_nullAction() {
        try {
            String action = null;
            String token = "some token";
            Expectation exp = OAuthOidcExpectation.addTokenInResponseExpectation(action, token);

            verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, token, "Did not find [" + token + "] in the response.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addTokenInResponseExpectation_nullToken() {
        try {
            String action = ACTION1;
            String token = null;
            Expectation exp = OAuthOidcExpectation.addTokenInResponseExpectation(action, token);

            verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, token, "Did not find [" + token + "] in the response.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_addTokenInResponseExpectation() {
        try {
            String action = ACTION1;
            String token = "Some token";
            Expectation exp = OAuthOidcExpectation.addTokenInResponseExpectation(action, token);

            verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, token, "Did not find [" + token + "] in the response.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
