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

import static org.junit.Assert.fail;

import java.net.URL;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;

import test.common.SharedOutputManager;

public class ResponseUrlExpectationTest extends CommonSpecificExpectationTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private final String url = "http://localhost:8010/context/path";

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
            String checkType = null;
            String searchForVal = null;
            String failureMsg = null;

            ResponseUrlExpectation exp = new ResponseUrlExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_URL, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor() {
        try {
            String testAction = "testAction";
            String checkType = "checkType";
            String searchForVal = "searchForVal";
            String failureMsg = "failureMsg";

            ResponseUrlExpectation exp = new ResponseUrlExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_URL, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    @Test
    public void test_validate_unknownCheckType() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, "check type", SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "Some content";

            runNegativeValidateTestForCheckType_unknown(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_unsupportedResponseType() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = new URL(url);

            runNegativeValidateTestForUnsupportedResponseType(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullAction() {
        try {
            Expectation exp = new ResponseUrlExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "Some " + SEARCH_FOR_VAL + " content";

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullResponseUrl() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = htmlunitHtmlPage;
            setValidateTestExpectations(content, null);

            runNegativeValidateTestForNullContent(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_contentIsHtmlUnitWebResponse() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = htmlunitWebResponse;
            try {
                exp.validate(content);
                fail("Should have thrown an error because the URL cannot be obtained from this response object, but did not.");
            } catch (Exception e) {
                verifyException(e, Pattern.quote(FAILURE_MESSAGE) + ".+not supported.+" + Pattern.quote(content.getClass().getName()));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_fails() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "missing search value";

            runNegativeValidateTestForCheckType_contains(exp, content, content.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_passes() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            URL testUrl = new URL(url + SEARCH_FOR_VAL + "/other");
            setValidateTestExpectations(content, testUrl);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_fails() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            URL testUrl = new URL(url + SEARCH_FOR_VAL + "/other");
            setValidateTestExpectations(content, testUrl);

            runNegativeValidateTestForCheckType_doesNotContain(exp, content, testUrl.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_passes() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            URL testUrl = new URL(url);
            setValidateTestExpectations(content, testUrl);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_matchSpansMultipleLines() {
        try {
            String searchForRegex = "line1.+line2";
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            URL testUrl = new URL("http://line1\n\rline2");
            setValidateTestExpectations(content, testUrl);

            runNegativeValidateTestForCheckType_matches(exp, content, testUrl.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_fails() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_MATCHES, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            URL testUrl = new URL(url);
            setValidateTestExpectations(content, testUrl);

            runNegativeValidateTestForCheckType_matches(exp, content, testUrl.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_passes() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_MATCHES, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            URL testUrl = new URL(url + SEARCH_FOR_VAL + "/");
            setValidateTestExpectations(content, testUrl);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_fails() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            URL testUrl = new URL(url + SEARCH_FOR_VAL);
            setValidateTestExpectations(content, testUrl);

            runNegativeValidateTestForCheckType_doesNotMatch(exp, content, testUrl.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_passes() {
        try {
            Expectation exp = new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            URL testUrl = new URL(url);
            setValidateTestExpectations(content, testUrl);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Override
    protected Expectation createBasicExpectation() {
        return new ResponseUrlExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    @Override
    protected Expectation createBasicExpectationWithNoAction() {
        return new ResponseUrlExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    protected void setValidateTestExpectations(Object responseObject, final Object content) {
        if (responseObject == htmlunitHtmlPage) {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getUrl();
                    will(returnValue((URL) content));
                }
            });
        } else if (responseObject == htmlunitTextPage) {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getUrl();
                    will(returnValue((URL) content));
                }
            });
        } else if (responseObject == htmlunitXmlPage) {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getUrl();
                    will(returnValue((URL) content));
                }
            });
        }
    }

}
