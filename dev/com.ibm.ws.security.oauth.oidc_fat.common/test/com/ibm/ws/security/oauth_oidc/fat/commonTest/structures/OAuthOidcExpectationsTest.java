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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.test.common.CommonExpectationTestClass;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.test.common.ExpectedExpectation;

import test.common.SharedOutputManager;

public class OAuthOidcExpectationsTest extends CommonExpectationTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.tooling.*=all");

    TestSettings settings = mockery.mock(TestSettings.class);

    OAuthOidcExpectations expectations = new OAuthOidcExpectations(ACTIONS);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        expectations = new OAuthOidcExpectations(ACTIONS);
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

    /**
     * Tests:
     * - Empty argument constructor
     * Expects:
     * - Actions list with all of the default OAuth test actions
     * - Empty expectations list
     */
    @Test
    public void test_constructor_noArg() {
        try {
            expectations = new OAuthOidcExpectations();

            verifyEmptyExpectationsList(expectations);

            String[] defaultActions = getDefaultOAuthOidcTestActions();
            List<String> actionsList = expectations.getActionsList();
            assertEquals("Actions list should have contained OAuth/OIDC actions by default, but the size did not match.", defaultActions.length, actionsList.size());
            for (String action : defaultActions) {
                assertTrue("Actions list did not contain expected test action [" + action + "].", actionsList.contains(action));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Constructor given a null argument
     * Expects:
     * - Empty actions list
     * - Empty expectations list
     */
    @Test
    public void test_constructor_nullArg() {
        try {
            String[] testActions = null;
            expectations = new OAuthOidcExpectations(testActions);

            verifyEmptyActionsList(expectations);
            verifyEmptyExpectationsList(expectations);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Constructor given an empty array
     * Expects:
     * - Empty actions list
     * - Empty expectations list
     */
    @Test
    public void test_constructor_emptyActionList() {
        try {
            String[] testActions = new String[0];
            expectations = new OAuthOidcExpectations(testActions);

            verifyEmptyActionsList(expectations);
            verifyEmptyExpectationsList(expectations);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Constructor given an array with one entry
     * Expects:
     * - Actions list with one entry that matches the entry in the array passed to the constructor
     * - Empty expectations list
     */
    @Test
    public void test_constructor_singleEntryActionList() {
        try {
            String[] testActions = new String[] { ACTION1 };
            expectations = new OAuthOidcExpectations(testActions);

            List<String> actionsList = expectations.getActionsList();
            assertEquals("Actions list size did not match expected value. Actions list was: " + actionsList, testActions.length, actionsList.size());
            assertEquals("Test action did not match the action provided to the constructor.", ACTION1, actionsList.get(0));
            verifyEmptyExpectationsList(expectations);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Constructor given an array with multiple entries
     * Expects:
     * - Actions list with entries that match the entries in the array passed to the constructor
     * - Empty expectations list
     */
    @Test
    public void test_constructor_multipleEntryActionList() {
        try {
            String[] testActions = new String[] { ACTION1, ACTION2, ACTION3 };
            expectations = new OAuthOidcExpectations(testActions);

            List<String> actionsList = expectations.getActionsList();
            assertEquals("Actions list size did not match expected value. Actions list was: " + actionsList, testActions.length, actionsList.size());
            assertEquals("First test action did not match the action provided to the constructor.", ACTION1, actionsList.get(0));
            assertEquals("Second test action did not match the action provided to the constructor.", ACTION2, actionsList.get(1));
            assertEquals("Third test action did not match the action provided to the constructor.", ACTION3, actionsList.get(2));
            verifyEmptyExpectationsList(expectations);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createSuccessfulStatusExpectations **************************************/

    /**
     * Tests:
     * - Static method
     * Expects:
     * - Actions list with all of the default OAuth test actions
     * - Expectations list with an entry for each default action where the expected response status is 200
     */
    @Test
    public void test_createSuccessfulStatusExpectations() {
        try {
            expectations = OAuthOidcExpectations.createSuccessfulStatusExpectations();

            String[] defaultActions = getDefaultOAuthOidcTestActions();

            List<String> actionsList = expectations.getActionsList();
            assertEquals("Actions list length should have matched the number of default test actions. Actions list was: " + actionsList, defaultActions.length, actionsList.size());
            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list length should have matched the number of default test actions. Expectations list was: " + expList, defaultActions.length,
                         expList.size());
            for (Expectation exp : expList) {
                verifySuccessfulResponseStatusExpectation(exp, exp.getAction());
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addNoTokensInResponseExpectations **************************************/

    /**
     * Tests:
     * - Null action argument
     * Expects:
     * - Empty actions list
     * - Expectations list with two entries:
     * 1. Expects full response not to contain Constants.ACCESS_TOKEN_KEY; test action is null
     * 2. Expects full response not to contain Constants.ID_TOKEN_KEY; test action is null
     */
    @Test
    public void test_addNoTokensInResponseExpectations_nullArg() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = null;
            expectations.addNoTokensInResponseExpectations(action);

            verifyEmptyActionsList(expectations);
            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list should have contained exactly two expectations (access token and ID token checks) but did not. Expectation list was: " + expList, 2,
                         expList.size());
            for (Expectation exp : expList) {
                verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, DO_NOT_CHECK, DO_NOT_CHECK);
                assertTrue("Expectation validation value must be for " + Constants.ACCESS_TOKEN_KEY + " or " + Constants.ID_TOKEN_KEY + " but was [" + exp.getValidationValue()
                           + "].",
                           exp.getValidationValue().equals(Constants.ACCESS_TOKEN_KEY) || exp.getValidationValue().equals(Constants.ID_TOKEN_KEY));
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Non-null action argument
     * Expects:
     * - Empty actions list
     * - Expectations list with two entries:
     * 1. Expects full response not to contain Constants.ACCESS_TOKEN_KEY; test action matches action provided to method
     * 2. Expects full response not to contain Constants.ID_TOKEN_KEY; test action matches action provided to method
     */
    @Test
    public void test_addNoTokensInResponseExpectations() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = ACTION1;
            expectations.addNoTokensInResponseExpectations(action);

            verifyEmptyActionsList(expectations);
            List<Expectation> expList = expectations.getExpectations();
            assertEquals("Expectations list should have contained exactly two expectations (access token and ID token checks) but did not. Expectation list was: " + expList, 2,
                         expList.size());
            for (Expectation exp : expList) {
                verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, DO_NOT_CHECK, DO_NOT_CHECK);
                assertTrue("Expectation validation value must be for " + Constants.ACCESS_TOKEN_KEY + " or " + Constants.ID_TOKEN_KEY + " but was [" + exp.getValidationValue()
                           + "].",
                           exp.getValidationValue().equals(Constants.ACCESS_TOKEN_KEY) || exp.getValidationValue().equals(Constants.ID_TOKEN_KEY));
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addNoTokenInResponseExpectation **************************************/

    /**
     * Tests:
     * - Null action argument
     * - Null token argument
     * Expects:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response not to contain specified token string
     */
    @Test
    public void test_addNoTokenInResponseExpectation_nullArgs() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = null;
            String token = null;
            expectations.addNoTokenInResponseExpectation(action, token);

            verifyResult_addNoTokenInResponseExpectation(expectations, action, token);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Null action argument
     * - Non-null token argument
     * Expects:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response not to contain specified token string
     */
    @Test
    public void test_addNoTokenInResponseExpectation_nullAction() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = null;
            String token = "Some token";
            expectations.addNoTokenInResponseExpectation(action, token);

            verifyResult_addNoTokenInResponseExpectation(expectations, action, token);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Non-null action argument
     * - Null token argument
     * Expects:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response not to contain specified token string
     */
    @Test
    public void test_addNoTokenInResponseExpectation_nullToken() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = ACTION1;
            String token = null;
            expectations.addNoTokenInResponseExpectation(action, token);

            verifyResult_addNoTokenInResponseExpectation(expectations, action, token);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Non-null action argument
     * - Non-null token argument
     * Expects:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response not to contain specified token string
     */
    @Test
    public void test_addNoTokenInResponseExpectation() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = ACTION1;
            String token = "Some token value";
            expectations.addNoTokenInResponseExpectation(action, token);

            verifyResult_addNoTokenInResponseExpectation(expectations, action, token);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addTokenInResponseExpectation **************************************/

    /**
     * Tests:
     * - Null action argument
     * - Null token argument
     * Expects:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response to contain specified token string
     */
    @Test
    public void test_addTokenInResponseExpectation_nullArgs() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = null;
            String token = null;
            expectations.addTokenInResponseExpectation(action, token);

            verifyResult_addTokenInResponseExpectation(expectations, action, token);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Null action argument
     * - Non-null token argument
     * Expects:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response to contain specified token string
     */
    @Test
    public void test_addTokenInResponseExpectation_nullAction() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = null;
            String token = "Some+token+value.";
            expectations.addTokenInResponseExpectation(action, token);

            verifyResult_addTokenInResponseExpectation(expectations, action, token);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Non-null action argument
     * - Null token argument
     * Expects:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response to contain specified token string
     */
    @Test
    public void test_addTokenInResponseExpectation_nullToken() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = ACTION1 + "+" + ACTION2;
            String token = null;
            expectations.addTokenInResponseExpectation(action, token);

            verifyResult_addTokenInResponseExpectation(expectations, action, token);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Non-null action argument
     * - Non-null token argument
     * Expects:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response to contain specified token string
     */
    @Test
    public void test_addTokenInResponseExpectation() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String action = ACTION1;
            String token = "Some+token+value";
            expectations.addTokenInResponseExpectation(action, token);

            verifyResult_addTokenInResponseExpectation(expectations, action, token);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addDefaultGeneralResponseExpectations **************************************/

    /**
     * Tests:
     * - Null provider type argument
     * - Null action argument
     * - Null test settings argument
     * Expects:
     * - Empty action list
     * - Empty expectations list
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_nullArgs() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = null;
            String action = null;
            TestSettings settings = null;
            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyEmptyActionsList(expectations);
            // Null settings object should result in no expectations being set
            verifyEmptyExpectationsList(expectations);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Null provider type argument
     * - Null action argument
     * - Test settings specifies null scope
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. General response check (no validation key or value)
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_nullProvider_nullAction_scopeNull() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = null;
            String action = null;
            final String scope = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Null provider type argument
     * - Null action argument
     * - Test settings specifies scope that doesn't include "openid" with that exact casing
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. General response check (no validation key or value)
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_nullProvider_nullAction_scopeDoesNotContainOpenid() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = null;
            String action = null;
            // Make sure that case matters and that "openid" does not appears
            final String scope = "some Openid list openId of OpenID scopes";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Null provider type argument
     * - Null action argument
     * - Test settings specifies scope that includes "openid"
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. Look for Constants.OIDC_OP in general response
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_nullProvider_nullAction_scopeContainsOpenid() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = null;
            String action = null;
            final String scope = "some list of openid scopes";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withOpenidScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - OAuth OP provider type argument
     * - Non-null action argument
     * - Test settings specifies null scope
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. General response check (no validation key or value)
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_oauthProvider_scopeNull() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Constants.OAUTH_OP;
            String action = ACTION1;
            final String scope = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - OAuth OP provider type argument
     * - Non-null action argument
     * - Test settings specifies scope that doesn't include "openid" with that exact casing
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. General response check (no validation key or value)
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_oauthProvider_scopeDoesNotContainOpenid() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Constants.OAUTH_OP;
            String action = ACTION1;
            // Make sure that case matters and that "openid" does not appears
            final String scope = "some Openid list openId of OpenID scopes";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - OAuth OP provider type argument
     * - Non-null action argument
     * - Test settings specifies scope that includes "openid"
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. General response check (no validation key or value)
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_oauthProvider_scopeContainsOpenid() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Constants.OAUTH_OP;
            String action = ACTION1;
            final String scope = "some list of openid scopes";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - OIDC OP provider type argument
     * - Non-null action argument
     * - Test settings specifies null scope
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. General response check (no validation key or value)
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_oidcProvider_scopeNull() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Constants.OIDC_OP;
            String action = ACTION1;
            final String scope = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - OIDC OP provider type argument
     * - Non-null action argument
     * - Test settings specifies scope that doesn't include "openid" with that exact casing
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. General response check (no validation key or value)
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_oidcProvider_scopeDoesNotContainOpenid() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Constants.OIDC_OP;
            String action = ACTION1;
            // Make sure that case matters and that "openid" does not appears
            final String scope = "some Openid list openId of OpenID scopes";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - OIDC OP provider type argument
     * - Non-null action argument
     * - Test settings specifies scope that includes "openid"
     * Expects:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. Look for Constants.OIDC_OP in general response
     * 2. Scope check in general response
     */
    @Test
    public void test_addDefaultGeneralResponseExpectations_oidcProvider_scopeContainsOpenid() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Constants.OIDC_OP;
            String action = ACTION1;
            final String scope = "some list of openid scopes";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultGeneralResponseExpectations(providerType, action, settings);

            verifyResult_addDefaultGeneralResponseExpectations_withOpenidScope(expectations, action, scope);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** addDefaultIdTokenExpectations **************************************/

    /**
     * Tests:
     * - Null provider type argument
     * - Null action argument
     * - Null test settings argument
     * Expects:
     * - Empty action list
     * - One expectation:
     * 1. Make sure ID token key is not found in the ID token response
     */
    @Test
    public void test_addDefaultIdTokenExpectations_nullArgs() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = null;
            String action = null;

            expectations.addDefaultIdTokenExpectations(providerType, action, null);

            verifyResult_addDefaultIdTokenExpectations_nonOAuthProviderOrMissingOpenidScope(expectations, action);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - OAuth OP provider type argument
     * - Null or non-null action argument (doesn't matter here)
     * - Non-null test settings argument
     * Expects:
     * - Empty action list
     * - One expectation:
     * 1. Make sure ID token key is not found in the ID token response
     */
    @Test
    public void test_addDefaultIdTokenExpectations_oauthProvider() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Constants.OAUTH_OP;
            String action = Utils.getRandomSelection(null, ACTION1);

            expectations.addDefaultIdTokenExpectations(providerType, action, settings);

            verifyResult_addDefaultIdTokenExpectations_nonOAuthProviderOrMissingOpenidScope(expectations, action);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provider type argument that isn't Constants.OAUTH_OP
     * - Null or non-null action argument (doesn't matter here)
     * - Test settings specifies null scope
     * Expects:
     * - Empty action list
     * - One expectation:
     * 1. Make sure ID token key is not found in the ID token response
     */
    @Test
    public void test_addDefaultIdTokenExpectations_nonOAuthProvider_scopeNull() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Utils.getRandomSelection(null, "not" + Constants.OAUTH_OP, Constants.OIDC_OP);
            String action = Utils.getRandomSelection(null, ACTION1);
            final String scope = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultIdTokenExpectations(providerType, action, settings);

            verifyResult_addDefaultIdTokenExpectations_nonOAuthProviderOrMissingOpenidScope(expectations, action);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provider type argument that isn't Constants.OAUTH_OP
     * - Null or non-null action argument (doesn't matter here)
     * - Test settings specifies scope that doesn't include "openid" with that exact casing
     * Expects:
     * - Empty action list
     * - One expectation:
     * 1. Make sure ID token key is not found in the ID token response
     */
    @Test
    public void test_addDefaultIdTokenExpectations_nonOAuthProvider_scopeDoesNotContainOpenid() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Utils.getRandomSelection(null, "not" + Constants.OAUTH_OP, Constants.OIDC_OP);
            String action = Utils.getRandomSelection(null, ACTION1);
            // Make sure that case matters and that "openid" does not appears
            final String scope = "some Openid list openId of OpenID scopes";
            mockery.checking(new org.jmock.Expectations() {
                {
                    allowing(settings).getScope();
                    will(returnValue(scope));
                }
            });

            expectations.addDefaultIdTokenExpectations(providerType, action, settings);

            verifyResult_addDefaultIdTokenExpectations_nonOAuthProviderOrMissingOpenidScope(expectations, action);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provider type argument that isn't Constants.OAUTH_OP
     * - Null or non-null action argument (doesn't matter here)
     * - Test settings specifies scope that includes "openid"
     * Expects:
     * - Empty action list
     * - Expectations list that has appropriate checks for ID token format, client ID, admin user, realm, and nonce
     */
    @Test
    public void test_addDefaultIdTokenExpectations_nonOAuthProvider_scopeContainsOpenid() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Utils.getRandomSelection(null, "not" + Constants.OAUTH_OP, Constants.OIDC_OP);
            String action = Utils.getRandomSelection(null, ACTION1);
            final String scope = "some list of openid scopes";
            final String clientId = "clientId";
            final String adminUser = "adminUser";
            final String realm = "realm";
            final String nonce = "nonce";
            mockery.checking(new org.jmock.Expectations() {
                {
                    allowing(settings).getScope();
                    will(returnValue(scope));
                    allowing(settings).getClientID();
                    will(returnValue(clientId));
                    allowing(settings).getAdminUser();
                    will(returnValue(adminUser));
                    allowing(settings).getRealm();
                    will(returnValue(realm));
                    allowing(settings).getNonce();
                    will(returnValue(nonce));
                }
            });

            expectations.addDefaultIdTokenExpectations(providerType, action, settings);

            verifyResult_addDefaultIdTokenExpectations(expectations, action, clientId, adminUser, realm, nonce);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provider type argument that isn't Constants.OAUTH_OP
     * - Null or non-null action argument (doesn't matter here)
     * - Test settings specifies scope that includes "openid"
     * - Test settings do not specify a realm name or a nonce value
     * Expects:
     * - Empty action list
     * - Expectations list that has appropriate checks for ID token format, client ID, and admin user
     */
    @Test
    public void test_addDefaultIdTokenExpectations_nonOAuthProvider_scopeContainsOpenid_missingRealmAndNonce() {
        try {
            expectations = new OAuthOidcExpectations(null);

            String providerType = Utils.getRandomSelection(null, "not" + Constants.OAUTH_OP, Constants.OIDC_OP);
            String action = Utils.getRandomSelection(null, ACTION1);
            final String scope = "some list of openid scopes";
            final String clientId = "clientId";
            final String adminUser = "adminUser";
            final String realm = null;
            final String nonce = Utils.getRandomSelection(null, "");
            mockery.checking(new org.jmock.Expectations() {
                {
                    allowing(settings).getScope();
                    will(returnValue(scope));
                    allowing(settings).getClientID();
                    will(returnValue(clientId));
                    allowing(settings).getAdminUser();
                    will(returnValue(adminUser));
                    allowing(settings).getRealm();
                    will(returnValue(realm));
                    allowing(settings).getNonce();
                    will(returnValue(nonce));
                }
            });

            expectations.addDefaultIdTokenExpectations(providerType, action, settings);

            verifyResult_addDefaultIdTokenExpectations(expectations, action, clientId, adminUser, realm, nonce);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    String[] getDefaultOAuthOidcTestActions() {
        List<String> actions = new ArrayList<String>();
        actions.addAll(Arrays.asList(Constants.OP_TEST_ACTIONS));
        actions.addAll(Arrays.asList(Constants.RP_TEST_ACTIONS));
        actions.addAll(Arrays.asList(Constants.BUILDER_TEST_ACTIONS));
        return actions.toArray(new String[actions.size()]);
    }

    /**
     * Verifies:
     * - Empty action list
     * - Expectations list with one entry:
     * 1. Expects full response not to contain specified token string
     */
    void verifyResult_addNoTokenInResponseExpectation(Expectations expectations, final String action, final String token) throws Exception {
        verifyEmptyActionsList(expectations);

        List<Expectation> expList = expectations.getExpectations();
        assertEquals("Expectations list should have contained only one expectation for the token check but did not. Expectation list was: " + expList, 1, expList.size());
        Expectation exp = expList.get(0);
        verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, token, DO_NOT_CHECK);
    }

    /**
     * Verifies:
     * - Empty actions list
     * - Expectations list with one entry:
     * 1. Expects full response to contain specified token string
     */
    void verifyResult_addTokenInResponseExpectation(Expectations expectations, final String action, final String token) throws Exception {
        verifyEmptyActionsList(expectations);

        List<Expectation> expList = expectations.getExpectations();
        assertEquals("Expectations list should have contained only one expectation for the token check but did not. Expectation list was: " + expList, 1, expList.size());
        Expectation exp = expList.get(0);
        verifyExpectationValues(exp, action, null, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, token, DO_NOT_CHECK);
    }

    /**
     * Verifies:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. General response check (no validation key or value)
     * 2. Scope check in general response
     */
    void verifyResult_addDefaultGeneralResponseExpectations_withScope(Expectations exps, String action, String scope) throws Exception {
        verifyResult_addDefaultGeneralResponseExpectations(exps, action, scope);

        List<Expectation> expList = exps.getExpectations();

        // First expectation should be a general response check
        Expectation e1 = expList.get(0);
        verifyExpectationValues(e1, action, DO_NOT_CHECK, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS, null, null, DO_NOT_CHECK);
    }

    /**
     * Verifies:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. Look for Constants.OIDC_OP in general response
     * 2. Scope check in general response
     */
    void verifyResult_addDefaultGeneralResponseExpectations_withOpenidScope(Expectations exps, String action, String scope) throws Exception {
        verifyResult_addDefaultGeneralResponseExpectations(exps, action, scope);

        List<Expectation> expList = exps.getExpectations();

        // First expectation should check for Constants.OIDC_OP in the response
        Expectation e1 = expList.get(0);
        verifyExpectationValues(e1, action, DO_NOT_CHECK, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS, null, Constants.OIDC_OP, DO_NOT_CHECK);
    }

    /**
     * Verifies:
     * - Empty action list
     * - Expectations list with two entries:
     * 1. Not verified by this method
     * 2. Scope check in general response
     */
    void verifyResult_addDefaultGeneralResponseExpectations(Expectations exps, String action, String scope) throws Exception {
        verifyEmptyActionsList(expectations);

        List<Expectation> expList = exps.getExpectations();
        assertEquals("Expectations list should have exactly two expectations but was: " + expList, 2, expList.size());

        // Second expectation should check the scope in the response
        Expectation e2 = expList.get(1);
        verifyExpectationValues(e2, action, DO_NOT_CHECK, Constants.RESPONSE_GENERAL, Constants.STRING_CONTAINS, "scope", scope, DO_NOT_CHECK);
    }

    /**
     * Verifies:
     * - Empty action list
     * - Expectations list with one entry:
     * 1. Make sure ID token key is not found in the ID token response
     */
    void verifyResult_addDefaultIdTokenExpectations_nonOAuthProviderOrMissingOpenidScope(Expectations exps, String testAction) throws Exception {
        verifyEmptyActionsList(exps);

        List<Expectation> expList = exps.getExpectations();
        assertEquals("Expectations list should have exactly one expectation but was: " + expList, 1, expList.size());

        // Expectation should check that an ID token key was not found
        Expectation e1 = expList.get(0);
        verifyExpectationValues(e1, testAction, DO_NOT_CHECK, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, Constants.ID_TOKEN_KEY, Constants.NOT_FOUND, DO_NOT_CHECK);
    }

    /**
     * Verifies:
     * - Empty action list
     * - Expectations list contains checks for the following:
     * 1. ID token format
     * 2. Token aud claim
     * 3. Token sub claim
     * 4. Token unique security name
     * 5. Token realm
     * 6. Token nonce (if one was specified)
     */
    void verifyResult_addDefaultIdTokenExpectations(Expectations exps, String action, String clientId, String adminUser, String realm, String nonce) throws Exception {
        verifyEmptyActionsList(exps);

        List<ExpectedExpectation> expExpectations = new ArrayList<ExpectedExpectation>();
        expExpectations.add(new ExpectedExpectation(action, DO_NOT_CHECK, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, null, null, DO_NOT_CHECK));
        expExpectations.add(new ExpectedExpectation(action, DO_NOT_CHECK, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_AUDIENCE_KEY, clientId, DO_NOT_CHECK));
        expExpectations.add(new ExpectedExpectation(action, DO_NOT_CHECK, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_SUBJECT_KEY, adminUser, DO_NOT_CHECK));
        expExpectations.add(new ExpectedExpectation(action, DO_NOT_CHECK, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS, Constants.IDTOK_UNIQ_SEC_NAME_KEY, adminUser, DO_NOT_CHECK));
        if (realm != null) {
            expExpectations.add(new ExpectedExpectation(action, DO_NOT_CHECK, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_REALM_KEY, realm, DO_NOT_CHECK));
        } else {
            expExpectations.add(new ExpectedExpectation(action, DO_NOT_CHECK, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_REALM_KEY, Constants.BASIC_REALM, DO_NOT_CHECK));
        }
        if (nonce != null && !nonce.isEmpty()) {
            expExpectations.add(new ExpectedExpectation(action, DO_NOT_CHECK, Constants.RESPONSE_ID_TOKEN, Constants.STRING_MATCHES, Constants.IDTOK_NONCE_KEY, nonce, DO_NOT_CHECK));
        }

        verifyAllAnticipatedExpectationsArePresent(exps, expExpectations);
    }

}
