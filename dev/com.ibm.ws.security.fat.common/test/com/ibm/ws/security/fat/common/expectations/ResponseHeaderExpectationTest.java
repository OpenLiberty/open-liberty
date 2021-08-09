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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.test.UnitTestUtils;

import test.common.SharedOutputManager;

public class ResponseHeaderExpectationTest extends CommonSpecificExpectationTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

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

            ResponseHeaderExpectation exp = new ResponseHeaderExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_HEADER, checkType, null, searchForVal, failureMsg);

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

            ResponseHeaderExpectation exp = new ResponseHeaderExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_HEADER, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    @Test
    public void test_validate_unknownCheckType() {
        try {
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, "check type", SEARCH_FOR_VAL, FAILURE_MESSAGE);
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
            Object content = new Integer(0);

            runNegativeValidateTestForUnsupportedResponseType(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullAction() {
        try {
            Expectation exp = new ResponseHeaderExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "Some " + SEARCH_FOR_VAL + " content";

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullResponseHeaders() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = htmlunitWebResponse;
            setValidateTestExpectations(content, null);

            runNegativeValidateTestForNullContent(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_emptyResponseHeaders() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = htmlunitHtmlPage;
            List<NameValuePair> headers = getHeaderList(0);
            setValidateTestExpectations(content, headers);

            runNegativeValidateTestForCheckType_contains(exp, content, buildHeaderOutputRegex(headers));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_fails() {
        try {
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "missing search value";

            runNegativeValidateTestForCheckType_contains(exp, content, content.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_passes() {
        try {
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            List<NameValuePair> headers = getHeaderList(0);
            headers.add(new NameValuePair(SEARCH_FOR_VAL, "found value"));
            setValidateTestExpectations(content, headers);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_fails() {
        try {
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitWebResponse;
            List<NameValuePair> headers = getHeaderList(0);
            headers.add(new NameValuePair(SEARCH_FOR_VAL, "found value"));
            setValidateTestExpectations(content, headers);

            runNegativeValidateTestForCheckType_doesNotContain(exp, content, buildHeaderOutputRegex(headers));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_passes() {
        try {
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            List<NameValuePair> headers = getHeaderList(5);
            setValidateTestExpectations(content, headers);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_fails() {
        try {
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_MATCHES, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            List<NameValuePair> headers = getHeaderList(5);
            setValidateTestExpectations(content, headers);

            runNegativeValidateTestForCheckType_matches(exp, content, buildHeaderOutputRegex(headers));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_matchSpansMultipleLines() {
        try {
            String searchForRegex = "search.+me";
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            List<NameValuePair> headers = getHeaderList(0);
            headers.add(new NameValuePair(SEARCH_FOR_VAL, "search \n\r for \n\r me"));
            setValidateTestExpectations(content, headers);

            runNegativeValidateTestForCheckType_matches(exp, content, buildHeaderOutputRegex(headers));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_passes() {
        try {
            String searchForRegex = "search.+me";
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitWebResponse;
            List<NameValuePair> headers = getHeaderList(0);
            headers.add(new NameValuePair(SEARCH_FOR_VAL, "search for me"));
            setValidateTestExpectations(content, headers);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_fails() {
        try {
            String searchForRegex = "search.+me";
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            List<NameValuePair> headers = getHeaderList(3);
            headers.add(new NameValuePair(SEARCH_FOR_VAL, "search for me"));
            setValidateTestExpectations(content, headers);

            runNegativeValidateTestForCheckType_doesNotMatch(exp, content, buildHeaderOutputRegex(headers));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_passes() {
        try {
            Expectation exp = new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            List<NameValuePair> headers = getHeaderList(3);
            setValidateTestExpectations(content, headers);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseHeadersString **************************************/

    @Test
    public void test_getResponseHeadersString_nullContent() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            Object content = null;
            String result = exp.getResponseHeaderString(content);
            assertNull("Result should have been null but was: [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getResponseHeadersString_unsupportedContentType() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            Object content = Boolean.TRUE;
            try {
                String result = exp.getResponseHeaderString(content);
                fail("Should have thrown an exception for an unknown response type but got result: [" + result + "].");
            } catch (Exception e) {
                verifyException(e, String.format(UnitTestUtils.ERR_UNKNOWN_RESPONSE_TYPE, content.getClass().getName()));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getResponseHeadersString_nullHeaders() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            final List<NameValuePair> headers = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String result = exp.getResponseHeaderString(htmlunitHtmlPage);

            assertNull("Result should have been null but was: [" + result + "].", result);
            assertStringInTrace(outputMgr, "No headers found");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getResponseHeadersString_emptyHeaderList() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String result = exp.getResponseHeaderString(htmlunitTextPage);

            assertEquals("Result should have been an empty string but wasn't.", "", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getResponseHeadersString_nonEmptyHeaderList() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair("name1", "value1"));
            headers.add(new NameValuePair("name2", "value2"));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String result = exp.getResponseHeaderString(htmlunitXmlPage);

            String expectedString = "name1" + ": " + "value1" + " " + ResponseHeaderExpectation.HEADER_DELIMITER + " ";
            expectedString += "name2" + ": " + "value2";
            assertEquals("Result did not match the expected value", expectedString, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** buildHeadersString **************************************/

    @Test
    public void test_buildHeadersString_nullList() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            List<NameValuePair> headers = null;

            String result = exp.buildHeadersString(headers);

            assertNull("Result should have been null but was: [" + result + "].", result);
            assertStringInTrace(outputMgr, "No headers found");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildHeadersString_emptyList() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            List<NameValuePair> headers = new ArrayList<NameValuePair>();

            String result = exp.buildHeadersString(headers);

            assertEquals("Result should have been an empty string but wasn't.", "", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildHeadersString_oneHeader() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair("name", "value"));

            String result = exp.buildHeadersString(headers);

            String expectedString = "name" + ": " + "value";
            assertEquals("Result did not match the expected value", expectedString, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildHeadersString_multipleHeaders() {
        try {
            ResponseHeaderExpectation exp = (ResponseHeaderExpectation) createBasicExpectation();
            List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair("emptyValue", ""));
            headers.add(new NameValuePair("", "empty name"));
            headers.add(new NameValuePair("`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?", "special characters"));

            String result = exp.buildHeadersString(headers);

            String expectedString = "emptyValue" + ": " + " " + ResponseHeaderExpectation.HEADER_DELIMITER + " ";
            expectedString += ": " + "empty name" + " " + ResponseHeaderExpectation.HEADER_DELIMITER + " ";
            expectedString += "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?: " + "special characters";
            assertEquals("Result did not match the expected value", expectedString, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Override
    protected Expectation createBasicExpectation() {
        return new ResponseHeaderExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    @Override
    protected Expectation createBasicExpectationWithNoAction() {
        return new ResponseHeaderExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    protected void setValidateTestExpectations(Object responseObject, final Object content) {
        setWebResponseExpectation(responseObject);
        mockery.checking(new org.jmock.Expectations() {
            {
                one(htmlunitWebResponse).getResponseHeaders();
                will(returnValue((List<NameValuePair>) content));
            }
        });
    }

    private List<NameValuePair> getHeaderList(int numberOfHeaders) {
        List<NameValuePair> headers = new ArrayList<NameValuePair>();
        for (int i = 1; i <= numberOfHeaders; i++) {
            headers.add(new NameValuePair("header " + i + " name", "header " + i + " value"));
        }
        return headers;
    }

    private String buildHeaderOutputRegex(List<NameValuePair> headers) {
        String regex = "";
        for (NameValuePair header : headers) {
            if (!regex.isEmpty()) {
                regex += ".*?" + Pattern.quote(ResponseHeaderExpectation.HEADER_DELIMITER) + ".*?";
            }
            regex += Pattern.quote(header.getName()) + ".+?" + Pattern.quote(header.getValue());
        }
        return regex;
    }

}
