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
package com.ibm.ws.security.fat.common.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class WebResponseUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private final com.gargoylesoftware.htmlunit.WebResponse htmlunitWebResponse = mockery.mock(com.gargoylesoftware.htmlunit.WebResponse.class);
    private final com.gargoylesoftware.htmlunit.html.HtmlPage htmlunitHtmlPage = mockery.mock(com.gargoylesoftware.htmlunit.html.HtmlPage.class);
    private final com.gargoylesoftware.htmlunit.TextPage htmlunitTextPage = mockery.mock(com.gargoylesoftware.htmlunit.TextPage.class);
    private final com.gargoylesoftware.htmlunit.xml.XmlPage htmlunitXmlPage = mockery.mock(com.gargoylesoftware.htmlunit.xml.XmlPage.class);
    private final com.gargoylesoftware.htmlunit.UnexpectedPage htmlunitUnexpectedPage = mockery.mock(com.gargoylesoftware.htmlunit.UnexpectedPage.class);
    private final com.gargoylesoftware.htmlunit.WebClient htmlunitWebClient = mockery.mock(com.gargoylesoftware.htmlunit.WebClient.class);
    private final com.gargoylesoftware.htmlunit.CookieManager htmlunitCookieManager = mockery.mock(com.gargoylesoftware.htmlunit.CookieManager.class);

    private final String HELLO_WORLD = "Hello, world!";
    private final String URL_STRING = "http://localhost:8010";
    private final String HEADER_NAME_1 = "headerName1";
    private final String HEADER_NAME_2 = "headerName2";
    private final String HEADER_NAME_3 = "headerName3";
    private final String HEADER_VALUE_1 = "headerValue1";
    private final String HEADER_VALUE_2 = "headerValue2";
    private final String HEADER_VALUE_3 = "headerValue3";

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

    /************************************** getResponseText **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseText_nullArgument() {
        try {
            Object pageOrResponse = null;

            String result = WebResponseUtils.getResponseText(pageOrResponse);

            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseText_unknownObject() {
        try {
            Object pageOrResponse = 12345;
            try {
                String result = WebResponseUtils.getResponseText(pageOrResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: " + Pattern.quote(pageOrResponse.getClass().getName()));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: String
     * Expects:
     * - Result match the content
     */
    @Test
    public void test_getResponseText_string() {
        try {
            Object pageOrResponse = "some string";
            String result = WebResponseUtils.getResponseText(pageOrResponse);
            assertEquals("Result did not match expected value.", pageOrResponse, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - Returned content is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseText_htmlunitWebResponse_nullContent() {
        try {
            final String content = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitWebResponse);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - Returned content is not null
     * Expects:
     * - Result match the content
     */
    @Test
    public void test_getResponseText_htmlunitWebResponse_nonNullContent() {
        try {
            final String content = HELLO_WORLD;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitWebResponse);
            assertEquals("Result did not match expected value.", content, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Returned WebResponse object is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseText_htmlunitHtmlPage_nullWebResponse() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(null));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitHtmlPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Returned content is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseText_htmlunitHtmlPage_nullContent() {
        try {
            final String content = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitHtmlPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Returned content is not null
     * Expects:
     * - Result match the content
     */
    @Test
    public void test_getResponseText_htmlunitHtmlPage_nonNullContent() {
        try {
            final String content = HELLO_WORLD;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitHtmlPage);
            assertEquals("Result did not match expected value.", content, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * - Returned content is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseText_htmlunitTextPage_nullContent() {
        try {
            final String content = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitTextPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * - Returned content is not null
     * Expects:
     * - Result match the content
     */
    @Test
    public void test_getResponseText_htmlunitTextPage_nonNullContent() {
        try {
            final String content = HELLO_WORLD;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitTextPage);
            assertEquals("Result did not match expected value.", content, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * - Returned content is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseText_htmlunitXmlPage_nullContent() {
        try {
            final String content = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitXmlPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * - Returned content is not null
     * Expects:
     * - Result match the content
     */
    @Test
    public void test_getResponseText_htmlunitXmlPage_nonNullContent() {
        try {
            final String content = HELLO_WORLD;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitXmlPage);
            assertEquals("Result did not match expected value.", content, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * - Returned content is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseText_htmlunitUnexpectedPage_nullContent() {
        try {
            final String content = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitUnexpectedPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * - Returned content is not null
     * Expects:
     * - Result match the content
     */
    @Test
    public void test_getResponseText_htmlunitUnexpectedPage_nonNullContent() {
        try {
            final String content = HELLO_WORLD;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseText(htmlunitUnexpectedPage);
            assertEquals("Result did not match expected value.", content, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseStatusCode **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be a bogus status code
     */
    @Test
    public void test_getResponseStatusCode_nullArgument() {
        try {
            Object pageOrResponse = null;

            int result = WebResponseUtils.getResponseStatusCode(pageOrResponse);

            assertEquals("Result did not match the expected status code for a null argument.", -1, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseStatusCode_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            try {
                int result = WebResponseUtils.getResponseStatusCode(pageOrResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * Expects:
     * - Result should match expected status code
     */
    @Test
    public void test_getResponseStatusCode_htmlunitWebResponse() {
        try {
            final int statusCode = 200;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                }
            });
            int result = WebResponseUtils.getResponseStatusCode(htmlunitWebResponse);
            assertEquals("Result did not match the expected status code.", statusCode, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * Expects:
     * - Result should match expected status code
     */
    @Test
    public void test_getResponseStatusCode_htmlunitHtmlPage() {
        try {
            final int statusCode = 200;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                }
            });
            int result = WebResponseUtils.getResponseStatusCode(htmlunitHtmlPage);
            assertEquals("Result did not match the expected status code.", statusCode, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * - Returned WebResponse object is null
     * Expects:
     * - Result should match expected status code
     */
    @Test
    public void test_getResponseStatusCode_htmlunitTextPage_nullWebResponse() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(null));
                }
            });
            int result = WebResponseUtils.getResponseStatusCode(htmlunitTextPage);
            assertEquals("Result did not match the expected status code for an unsuccessful result.", -1, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * Expects:
     * - Result should match expected status code
     */
    @Test
    public void test_getResponseStatusCode_htmlunitTextPage() {
        try {
            final int statusCode = 200;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                }
            });
            int result = WebResponseUtils.getResponseStatusCode(htmlunitTextPage);
            assertEquals("Result did not match the expected status code.", statusCode, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * Expects:
     * - Result should match expected status code
     */
    @Test
    public void test_getResponseStatusCode_htmlunitXmlPage() {
        try {
            final int statusCode = 200;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                }
            });
            int result = WebResponseUtils.getResponseStatusCode(htmlunitXmlPage);
            assertEquals("Result did not match the expected status code.", statusCode, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * Expects:
     * - Result should match expected status code
     */
    @Test
    public void test_getResponseStatusCode_htmlunitUnexpectedPage() {
        try {
            final int statusCode = 500;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                }
            });
            int result = WebResponseUtils.getResponseStatusCode(htmlunitUnexpectedPage);
            assertEquals("Result did not match the expected status code.", statusCode, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseTitle **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseTitle_nullArgument() {
        try {
            Object pageOrResponse = null;

            String result = WebResponseUtils.getResponseTitle(pageOrResponse);

            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseTitle_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            try {
                String result = WebResponseUtils.getResponseTitle(pageOrResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * Expects:
     * - Exception should be thrown since getting the title isn't supported with this object
     */
    @Test
    public void test_getResponseTitle_htmlunitWebResponse() {
        try {
            try {
                String result = WebResponseUtils.getResponseTitle(htmlunitWebResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "not supported.+" + "com.gargoylesoftware.htmlunit.WebResponse");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Returned content is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseTitle_htmlunitHtmlPage_nullContent() {
        try {
            final String content = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getTitleText();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseTitle(htmlunitHtmlPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Returned content is not null
     * Expects:
     * - Result match the content
     */
    @Test
    public void test_getResponseTitle_htmlunitHtmlPage_nonNullContent() {
        try {
            final String content = HELLO_WORLD;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getTitleText();
                    will(returnValue(content));
                }
            });
            String result = WebResponseUtils.getResponseTitle(htmlunitHtmlPage);
            assertEquals("Result did not match expected value.", content, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * Expects:
     * - Result should be "TextPage has no title"
     */
    @Test
    public void test_getResponseTitle_htmlunitTextPage() {
        try {
            String result = WebResponseUtils.getResponseTitle(htmlunitTextPage);
            assertEquals("Result did not match expected value.", htmlunitTextPage.getClass().getName() + " has no title", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * Expects:
     * - Result should be "XmlPage has no title"
     */
    @Test
    public void test_getResponseTitle_htmlunitXmlPage() {
        try {
            String result = WebResponseUtils.getResponseTitle(htmlunitXmlPage);
            assertEquals("Result did not match expected value.", htmlunitXmlPage.getClass().getName() + " has no title", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * Expects:
     * - Result should be "UnexpectedPage has no title"
     */
    @Test
    public void test_getResponseTitle_htmlunitUnexpectedPage() {
        try {
            String result = WebResponseUtils.getResponseTitle(htmlunitUnexpectedPage);
            assertEquals("Result did not match expected value.", htmlunitUnexpectedPage.getClass().getName() + " has no title", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseUrl **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseUrl_nullArgument() {
        try {
            Object pageOrResponse = null;

            String result = WebResponseUtils.getResponseUrl(pageOrResponse);

            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseUrl_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            try {
                String result = WebResponseUtils.getResponseUrl(pageOrResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * Expects:
     * - Exception should be thrown since getting the title isn't supported with this object
     */
    @Test
    public void test_getResponseUrl_htmlunitWebResponse() {
        try {
            try {
                String result = WebResponseUtils.getResponseUrl(htmlunitWebResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "not supported.+" + "com.gargoylesoftware.htmlunit.WebResponse");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Returned URL object is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseUrl_htmlunitHtmlPage_nullUrl() {
        try {
            final URL url = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getUrl();
                    will(returnValue(url));
                }
            });
            String result = WebResponseUtils.getResponseUrl(htmlunitHtmlPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * Expects:
     * - Result should match the expected URL string
     */
    @Test
    public void test_getResponseUrl_htmlunitHtmlPage() {
        try {
            final URL url = new URL(URL_STRING);
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getUrl();
                    will(returnValue(url));
                }
            });
            String result = WebResponseUtils.getResponseUrl(htmlunitHtmlPage);
            assertEquals("Result did not match expected value.", URL_STRING, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * Expects:
     * - Result should match the expected URL string
     */
    @Test
    public void test_getResponseUrl_htmlunitTextPage() {
        try {
            final URL url = new URL(URL_STRING);
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getUrl();
                    will(returnValue(url));
                }
            });
            String result = WebResponseUtils.getResponseUrl(htmlunitTextPage);
            assertEquals("Result did not match expected value.", URL_STRING, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * Expects:
     * - Result should match the expected URL string
     */
    @Test
    public void test_getResponseUrl_htmlunitXmlPage() {
        try {
            final URL url = new URL(URL_STRING);
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getUrl();
                    will(returnValue(url));
                }
            });
            String result = WebResponseUtils.getResponseUrl(htmlunitXmlPage);
            assertEquals("Result did not match expected value.", URL_STRING, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * Expects:
     * - Result should match the expected URL string
     */
    @Test
    public void test_getResponseUrl_htmlunitUnexpectedPage() {
        try {
            final URL url = new URL(URL_STRING);
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).getUrl();
                    will(returnValue(url));
                }
            });
            String result = WebResponseUtils.getResponseUrl(htmlunitUnexpectedPage);
            assertEquals("Result did not match expected value.", URL_STRING, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseMessage **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseMessage_nullArgument() {
        try {
            Object pageOrResponse = null;

            String result = WebResponseUtils.getResponseMessage(pageOrResponse);

            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseMessage_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            try {
                String result = WebResponseUtils.getResponseMessage(pageOrResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - Status message is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseMessage_htmlunitWebResponse_nullMessage() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(null));
                }
            });
            String result = WebResponseUtils.getResponseMessage(htmlunitWebResponse);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * Expects:
     * - Result should match the expected response string
     */
    @Test
    public void test_getResponseMessage_htmlunitWebResponse() {
        try {
            final String message = "This is the response message.";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(message));
                }
            });
            String result = WebResponseUtils.getResponseMessage(htmlunitWebResponse);
            assertEquals("Result did not match expected value.", message, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * Expects:
     * - Result should match the expected response string
     */
    @Test
    public void test_getResponseMessage_htmlunitHtmlPage() {
        try {
            final String message = "This is the response message.";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(message));
                }
            });
            String result = WebResponseUtils.getResponseMessage(htmlunitHtmlPage);
            assertEquals("Result did not match expected value.", message, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * Expects:
     * - Result should match the expected response string
     */
    @Test
    public void test_getResponseMessage_htmlunitTextPage() {
        try {
            final String message = "This is the response message.";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(message));
                }
            });
            String result = WebResponseUtils.getResponseMessage(htmlunitTextPage);
            assertEquals("Result did not match expected value.", message, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * - WebResponse object is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseMessage_htmlunitXmlPage_nullWebResponse() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(null));
                }
            });
            String result = WebResponseUtils.getResponseMessage(htmlunitXmlPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * Expects:
     * - Result should match the expected response string
     */
    @Test
    public void test_getResponseMessage_htmlunitXmlPage() {
        try {
            final String message = "This is the response message.";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(message));
                }
            });
            String result = WebResponseUtils.getResponseMessage(htmlunitXmlPage);
            assertEquals("Result did not match expected value.", message, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * Expects:
     * - Result should match the expected response string
     */
    @Test
    public void test_getResponseMessage_htmlunitUnexpectedPage() {
        try {
            final String message = "This is the response message.";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(message));
                }
            });
            String result = WebResponseUtils.getResponseMessage(htmlunitUnexpectedPage);
            assertEquals("Result did not match expected value.", message, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseHeaderNames **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseHeaderNames_nullArgument() {
        try {
            Object pageOrResponse = null;

            String[] result = WebResponseUtils.getResponseHeaderNames(pageOrResponse);

            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseHeaderNames_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            try {
                String[] result = WebResponseUtils.getResponseHeaderNames(pageOrResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - List of headers is null
     * Expects:
     * - Result should be an empty array
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitWebResponse_nullHeaders() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(null));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitWebResponse);
            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result should have been an empty array but was not.", Arrays.toString(new String[0]), Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - List of headers is empty
     * Expects:
     * - Result should be an empty array
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitWebResponse_noHeaders() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitWebResponse);
            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result should have been an empty array but was not.", Arrays.toString(new String[0]), Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - List of headers contains one header
     * Expects:
     * - Result should be array containing just the name of the header
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitWebResponse_oneHeader() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitWebResponse);
            assertNotNull("Result should not have been null but was.", result);

            String expectedResult = Arrays.toString(new String[] { HEADER_NAME_1 });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - List of headers contains multiple headers
     * Expects:
     * - Result should be array containing the names of each header
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitWebResponse_multipleHeaders() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            headers.add(new NameValuePair(HEADER_NAME_3, HEADER_VALUE_3));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitWebResponse);
            assertNotNull("Result should not have been null but was.", result);

            String expectedResult = Arrays.toString(new String[] { HEADER_NAME_1, HEADER_NAME_2, HEADER_NAME_3 });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - WebResponse object is null
     * Expects:
     * - Result should be an empty array
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitHtmlPage_nullWebResponse() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(null));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitHtmlPage);
            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result should have been an empty array but was not.", Arrays.toString(new String[0]), Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * Expects:
     * - Result should be array containing the names of each header
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitHtmlPage() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            headers.add(new NameValuePair(HEADER_NAME_3, HEADER_VALUE_3));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitHtmlPage);
            assertNotNull("Result should not have been null but was.", result);

            String expectedResult = Arrays.toString(new String[] { HEADER_NAME_1, HEADER_NAME_2, HEADER_NAME_3 });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * Expects:
     * - Result should be array containing the names of each header
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitTextPage() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            headers.add(new NameValuePair(HEADER_NAME_3, HEADER_VALUE_3));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitTextPage);
            assertNotNull("Result should not have been null but was.", result);

            String expectedResult = Arrays.toString(new String[] { HEADER_NAME_1, HEADER_NAME_2, HEADER_NAME_3 });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * Expects:
     * - Result should be array containing the names of each header
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitXmlPage() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            headers.add(new NameValuePair(HEADER_NAME_3, HEADER_VALUE_3));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitXmlPage);
            assertNotNull("Result should not have been null but was.", result);

            String expectedResult = Arrays.toString(new String[] { HEADER_NAME_1, HEADER_NAME_2, HEADER_NAME_3 });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * Expects:
     * - Result should be array containing the names of each header
     */
    @Test
    public void test_getResponseHeaderNames_htmlunitUnexpectedPage() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            String[] result = WebResponseUtils.getResponseHeaderNames(htmlunitUnexpectedPage);
            assertNotNull("Result should not have been null but was.", result);

            String expectedResult = Arrays.toString(new String[] { HEADER_NAME_1 });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseHeaderList **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseHeaderList_nullArgument() {
        try {
            Object pageOrResponse = null;

            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(pageOrResponse);

            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseHeaderList_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            try {
                List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(pageOrResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - List of headers is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseHeaderList_htmlunitWebResponse_nullHeaders() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(null));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitWebResponse);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - List of headers is empty
     * Expects:
     * - Result should be an empty list
     */
    @Test
    public void test_getResponseHeaderList_htmlunitWebResponse_noHeaders() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitWebResponse);
            assertEquals("Result should have been an empty list but was not.", headers, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - List of headers contains one header
     * Expects:
     * - Result should match the list of headers
     */
    @Test
    public void test_getResponseHeaderList_htmlunitWebResponse_oneHeader() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitWebResponse);
            assertEquals("Result did not match the expected list of headers.", headers, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - List of headers contains multiple headers
     * Expects:
     * - Result should match the list of headers
     */
    @Test
    public void test_getResponseHeaderList_htmlunitWebResponse_multipleHeaders() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            headers.add(new NameValuePair(HEADER_NAME_3, HEADER_VALUE_3));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitWebResponse);
            assertEquals("Result did not match the expected list of headers.", headers, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - WebResponse object is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseHeaderList_htmlunitHtmlPage_nullWebResponse() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(null));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitHtmlPage);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * Expects:
     * - Result should match the list of headers
     */
    @Test
    public void test_getResponseHeaderList_htmlunitHtmlPage() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            headers.add(new NameValuePair(HEADER_NAME_3, HEADER_VALUE_3));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitHtmlPage);
            assertEquals("Result did not match the expected list of headers.", headers, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * Expects:
     * - Result should match the list of headers
     */
    @Test
    public void test_getResponseHeaderList_htmlunitTextPage() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            headers.add(new NameValuePair(HEADER_NAME_3, HEADER_VALUE_3));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitTextPage);
            assertEquals("Result did not match the expected list of headers.", headers, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * Expects:
     * - Result should be array containing the names of each header
     */
    @Test
    public void test_getResponseHeaderList_htmlunitXmlPage() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            headers.add(new NameValuePair(HEADER_NAME_3, HEADER_VALUE_3));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitXmlPage);
            assertEquals("Result did not match the expected list of headers.", headers, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * Expects:
     * - Result should be array containing the names of each header
     */
    @Test
    public void test_getResponseHeaderList_htmlunitUnexpectedPage() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME_2, HEADER_VALUE_2));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                }
            });
            List<NameValuePair> result = WebResponseUtils.getResponseHeaderList(htmlunitUnexpectedPage);
            assertEquals("Result did not match the expected list of headers.", headers, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** convertHeaderListToNameArray **************************************/

    /**
     * Tests:
     * - List: null
     * Expects:
     * - Result should be an empty array
     */
    @Test
    public void test_convertHeaderListToNameArray_nullList() {
        try {
            final List<NameValuePair> headers = null;

            String[] result = WebResponseUtils.convertHeaderListToNameArray(headers);

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result should have been an empty array but was not.", Arrays.toString(new String[0]), Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - List: Empty
     * Expects:
     * - Result should be an empty array
     */
    @Test
    public void test_convertHeaderListToNameArray_emptyList() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();

            String[] result = WebResponseUtils.convertHeaderListToNameArray(headers);

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result should have been an empty array but was not.", Arrays.toString(new String[0]), Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - List: One entry with a null name
     * Expects:
     * - Result should be an array with a single null entry
     */
    @Test
    public void test_convertHeaderListToNameArray_singleEntry_nullName() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(null, HEADER_VALUE_1));

            String[] result = WebResponseUtils.convertHeaderListToNameArray(headers);

            assertNotNull("Result should not have been null but was.", result);
            String expectedResult = Arrays.toString(new String[] { null });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - List: One entry with an empty name
     * Expects:
     * - Result should be an array with a single empty string entry
     */
    @Test
    public void test_convertHeaderListToNameArray_singleEntry_emptyName() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair("", HEADER_VALUE_1));

            String[] result = WebResponseUtils.convertHeaderListToNameArray(headers);

            assertNotNull("Result should not have been null but was.", result);
            String expectedResult = Arrays.toString(new String[] { "" });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - List: Multiple entries
     * Expects:
     * - Result should be an array with the corresponding names of each entry
     */
    @Test
    public void test_convertHeaderListToNameArray_multipleEntries() {
        try {
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(null, "nullNameValue"));
            headers.add(new NameValuePair("", "emptyNameValue"));
            headers.add(new NameValuePair(HEADER_NAME_1, HEADER_VALUE_1));
            headers.add(new NameValuePair("`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? ", "special char name value"));

            String[] result = WebResponseUtils.convertHeaderListToNameArray(headers);

            assertNotNull("Result should not have been null but was.", result);
            String expectedResult = Arrays.toString(new String[] { null, "", HEADER_NAME_1, "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? " });
            assertEquals("Result did not match the list of expected header names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseHeaderField **************************************/

    /**
     * Tests:
     * - Response argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseHeaderField_nullArgument() {
        try {
            Object pageOrResponse = null;
            String headerName = null;

            String result = WebResponseUtils.getResponseHeaderField(pageOrResponse, headerName);

            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Response argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseHeaderField_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            String headerName = null;
            try {
                String result = WebResponseUtils.getResponseHeaderField(pageOrResponse, headerName);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - Header name is null
     * - Header value is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseHeaderField_htmlunitWebResponse_nullHeaderName_nullValue() {
        try {
            final String headerName = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaderValue(headerName);
                    will(returnValue(null));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitWebResponse, headerName);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - Header name is null
     * - Header value is non-null
     * Expects:
     * - Result should match expected header value
     */
    @Test
    public void test_getResponseHeaderField_htmlunitWebResponse_nullHeaderName_nonNullValue() {
        try {
            final String headerName = null;
            final String headerValue = HEADER_VALUE_1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaderValue(headerName);
                    will(returnValue(headerValue));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitWebResponse, headerName);
            assertEquals("Returned header value did not match expected value.", headerValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - Header name is empty
     * - Header value is non-null
     * Expects:
     * - Result should match expected header value
     */
    @Test
    public void test_getResponseHeaderField_htmlunitWebResponse_emptyHeaderName() {
        try {
            final String headerName = "";
            final String headerValue = HEADER_VALUE_1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaderValue(headerName);
                    will(returnValue(headerValue));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitWebResponse, headerName);
            assertEquals("Returned header value did not match expected value.", headerValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * - Header name is non-null
     * - Header value is non-null
     * Expects:
     * - Result should match expected header value
     */
    @Test
    public void test_getResponseHeaderField_htmlunitWebResponse() {
        try {
            final String headerName = HEADER_NAME_1;
            final String headerValue = HEADER_VALUE_1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getResponseHeaderValue(headerName);
                    will(returnValue(headerValue));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitWebResponse, headerName);
            assertEquals("Returned header value did not match expected value.", headerValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - WebResponse object is null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseHeaderField_htmlunitHtmlPage_nullWebResponse() {
        try {
            final String headerName = HEADER_NAME_1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(null));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitHtmlPage, headerName);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * Expects:
     * - Result should match expected header value
     */
    @Test
    public void test_getResponseHeaderField_htmlunitHtmlPage() {
        try {
            final String headerName = HEADER_NAME_1;
            final String headerValue = HEADER_VALUE_1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaderValue(headerName);
                    will(returnValue(headerValue));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitHtmlPage, headerName);
            assertEquals("Returned header value did not match expected value.", headerValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * Expects:
     * - Result should match expected header value
     */
    @Test
    public void test_getResponseHeaderField_htmlunitTextPage() {
        try {
            final String headerName = HEADER_NAME_1;
            final String headerValue = HEADER_VALUE_1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaderValue(headerName);
                    will(returnValue(headerValue));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitTextPage, headerName);
            assertEquals("Returned header value did not match expected value.", headerValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * Expects:
     * - Result should match expected header value
     */
    @Test
    public void test_getResponseHeaderField_htmlunitXmlPage() {
        try {
            final String headerName = HEADER_NAME_1;
            final String headerValue = HEADER_VALUE_1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaderValue(headerName);
                    will(returnValue(headerValue));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitXmlPage, headerName);
            assertEquals("Returned header value did not match expected value.", headerValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * Expects:
     * - Result should match expected header value
     */
    @Test
    public void test_getResponseHeaderField_htmlunitUnexpectedPage() {
        try {
            final String headerName = HEADER_NAME_1;
            final String headerValue = HEADER_VALUE_1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getResponseHeaderValue(headerName);
                    will(returnValue(headerValue));
                }
            });
            String result = WebResponseUtils.getResponseHeaderField(htmlunitUnexpectedPage, headerName);
            assertEquals("Returned header value did not match expected value.", headerValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseCookieNames **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseCookieNames_nullArgument() {
        try {
            Object pageOrResponse = null;

            String[] result = WebResponseUtils.getResponseCookieNames(pageOrResponse);

            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseCookieNames_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            try {
                String[] result = WebResponseUtils.getResponseCookieNames(pageOrResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "not supported.+" + "java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseCookieNames_htmlunitWebResponse() {
        try {
            try {
                String[] result = WebResponseUtils.getResponseCookieNames(htmlunitWebResponse);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "not supported.+" + "com.gargoylesoftware.htmlunit.WebResponse");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * Expects:
     * - Result should match the expected list of cookie names
     */
    @Test
    public void test_getResponseCookieNames_htmlunitHtmlPage() {
        try {
            final Set<Cookie> cookies = new HashSet<Cookie>();
            cookies.add(new Cookie("/", "name", "value"));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebClient();
                    will(returnValue(htmlunitWebClient));
                    one(htmlunitWebClient).getCookieManager();
                    will(returnValue(htmlunitCookieManager));
                    one(htmlunitCookieManager).getCookies();
                    will(returnValue(cookies));
                }
            });
            String[] result = WebResponseUtils.getResponseCookieNames(htmlunitHtmlPage);

            assertNotNull("Result should not have been null but was.", result);
            String expectedResult = Arrays.toString(new String[] { "name" });
            assertEquals("Result did not match the expected list of cookie names.", expectedResult, Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseCookieNames_htmlunitTextPage() {
        try {
            try {
                String[] result = WebResponseUtils.getResponseCookieNames(htmlunitTextPage);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "not supported.+" + "com.gargoylesoftware.htmlunit.TextPage");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseCookieNames_htmlunitXmlPage() {
        try {
            try {
                String[] result = WebResponseUtils.getResponseCookieNames(htmlunitXmlPage);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "not supported.+" + "com.gargoylesoftware.htmlunit.xml.XmlPage");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseCookieNames_htmlunitUnexpectedPage() {
        try {
            try {
                String[] result = WebResponseUtils.getResponseCookieNames(htmlunitUnexpectedPage);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "not supported.+" + "com.gargoylesoftware.htmlunit.UnexpectedPage");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getCookieNamesFromHtmlPage **************************************/

    /**
     * Tests:
     * - WebClient is null
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getCookieNamesFromHtmlPage_nullWebClient() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebClient();
                    will(returnValue(null));
                }
            });
            try {
                String[] result = WebResponseUtils.getCookieNamesFromHtmlPage(htmlunitHtmlPage);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Cannot get cookies.+web client cannot be found");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - CookieManager is null
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getCookieNamesFromHtmlPage_nullCookieManager() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebClient();
                    will(returnValue(htmlunitWebClient));
                    one(htmlunitWebClient).getCookieManager();
                    will(returnValue(null));
                }
            });
            try {
                String[] result = WebResponseUtils.getCookieNamesFromHtmlPage(htmlunitHtmlPage);
                fail("Should have thrown an exception but got result [" + result + "].");
            } catch (Exception e) {
                verifyException(e, "Cannot get cookies.+cookie manager cannot be found");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie set is null
     * Expects:
     * - Result should be an empty array
     */
    @Test
    public void test_getCookieNamesFromHtmlPage_nullCookieSet() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebClient();
                    will(returnValue(htmlunitWebClient));
                    one(htmlunitWebClient).getCookieManager();
                    will(returnValue(htmlunitCookieManager));
                    one(htmlunitCookieManager).getCookies();
                    will(returnValue(null));
                }
            });
            String[] result = WebResponseUtils.getCookieNamesFromHtmlPage(htmlunitHtmlPage);
            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result should have been an empty array but was not.", Arrays.toString(new String[0]), Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie set is empty
     * Expects:
     * - Result should be an empty array
     */
    @Test
    public void test_getCookieNamesFromHtmlPage_emptyCookieSet() {
        try {
            final Set<Cookie> cookies = new HashSet<Cookie>();
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebClient();
                    will(returnValue(htmlunitWebClient));
                    one(htmlunitWebClient).getCookieManager();
                    will(returnValue(htmlunitCookieManager));
                    one(htmlunitCookieManager).getCookies();
                    will(returnValue(cookies));
                }
            });
            String[] result = WebResponseUtils.getCookieNamesFromHtmlPage(htmlunitHtmlPage);
            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result should have been an empty array but was not.", Arrays.toString(new String[0]), Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie set includes null cookie
     * Expects:
     * - Result should be an empty array
     */
    @Test
    public void test_getCookieNamesFromHtmlPage_cookieSetWithNullCookie() {
        try {
            final Set<Cookie> cookies = new HashSet<Cookie>();
            cookies.add(null);
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebClient();
                    will(returnValue(htmlunitWebClient));
                    one(htmlunitWebClient).getCookieManager();
                    will(returnValue(htmlunitCookieManager));
                    one(htmlunitCookieManager).getCookies();
                    will(returnValue(cookies));
                }
            });
            String[] result = WebResponseUtils.getCookieNamesFromHtmlPage(htmlunitHtmlPage);
            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result should have been an empty array but was not.", Arrays.toString(new String[0]), Arrays.toString(result));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie set includes multiple cookies
     * Expects:
     * - Result should be array containing the names of each cookie
     */
    @Test
    public void test_getCookieNamesFromHtmlPage_multipleCookies() {
        try {
            final Set<Cookie> cookies = new HashSet<Cookie>();
            cookies.add(new Cookie("/", "", "emptyNameValue"));
            cookies.add(new Cookie("/", "name1", "value1"));
            cookies.add(new Cookie("/", "`~!@#$%^&*() -_=+[{]}\\|;:'\",<.>/?", "special char name value"));
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebClient();
                    will(returnValue(htmlunitWebClient));
                    one(htmlunitWebClient).getCookieManager();
                    will(returnValue(htmlunitCookieManager));
                    one(htmlunitCookieManager).getCookies();
                    will(returnValue(cookies));
                }
            });
            String[] result = WebResponseUtils.getCookieNamesFromHtmlPage(htmlunitHtmlPage);

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result did not contain the expected number of entries. Result was " + Arrays.toString(result), 3, result.length);
            List<String> resultAsList = Arrays.asList(result);
            assertTrue("Result did not contain expected empty cookie name.", resultAsList.contains(""));
            assertTrue("Result did not contain expected cookie name [" + "name1" + "].", resultAsList.contains("name1"));
            assertTrue("Result did not contain expected cookie name [" + "`~!@#$%^&*() -_=+[{]}\\|;:'\",<.>/?" + "].", resultAsList.contains("`~!@#$%^&*() -_=+[{]}\\|;:'\",<.>/?"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getResponseIsHtml **************************************/

    /**
     * Tests:
     * - Argument: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_getResponseIsHtml_nullArgument() {
        try {
            Object pageOrResponse = null;

            boolean result = WebResponseUtils.getResponseIsHtml(pageOrResponse);

            assertFalse("Response should not have been considered HTML but was.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: Unknown/unsupported object type
     * Expects:
     * - Exception should be thrown
     */
    @Test
    public void test_getResponseIsHtml_unknownObject() {
        try {
            Object pageOrResponse = "some unknown object";
            try {
                boolean result = WebResponseUtils.getResponseIsHtml(pageOrResponse);
                fail("Should have thrown an exception, but result was " + ((result ? "" : "not ")) + "considered HTML.");
            } catch (Exception e) {
                verifyException(e, "Unknown response type: java.lang.String");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.WebResponse
     * Expects:
     * - Result should be true
     */
    @Test
    public void test_getResponseIsHtml_htmlunitWebResponse() {
        try {
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitWebResponse);
            assertTrue("A com.gargoylesoftware.htmlunit.WebResponse object should be considered HTML but was not.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Object is mocked to return false
     * Expects:
     * - Result should match expected value
     */
    @Test
    public void test_getResponseIsHtml_htmlunitHtmlPage_false() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).isHtmlPage();
                    will(returnValue(false));
                }
            });
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitHtmlPage);
            assertFalse("Assertion did not match the expected value (false).", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Object is mocked to return true
     * Expects:
     * - Result should match expected value
     */
    @Test
    public void test_getResponseIsHtml_htmlunitHtmlPage_true() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).isHtmlPage();
                    will(returnValue(true));
                }
            });
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitHtmlPage);
            assertTrue("Assertion did not match the expected value (true).", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * - Object is mocked to return false
     * Expects:
     * - Result should match expected value
     */
    @Test
    public void test_getResponseIsHtml_htmlunitTextPage_false() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).isHtmlPage();
                    will(returnValue(false));
                }
            });
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitTextPage);
            assertFalse("Assertion did not match the expected value (false).", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.TextPage
     * - Object is mocked to return true
     * Expects:
     * - Result should match expected value
     */
    @Test
    public void test_getResponseIsHtml_htmlunitTextPage_true() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).isHtmlPage();
                    will(returnValue(true));
                }
            });
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitTextPage);
            assertTrue("Assertion did not match the expected value (true).", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * - Object is mocked to return false
     * Expects:
     * - Result should match expected value
     */
    @Test
    public void test_getResponseIsHtml_htmlunitXmlPage_false() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).isHtmlPage();
                    will(returnValue(false));
                }
            });
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitXmlPage);
            assertFalse("Assertion did not match the expected value (false).", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.xml.XmlPage
     * - Object is mocked to return true
     * Expects:
     * - Result should match expected value
     */
    @Test
    public void test_getResponseIsHtml_htmlunitXmlPage_true() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).isHtmlPage();
                    will(returnValue(true));
                }
            });
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitXmlPage);
            assertTrue("Assertion did not match the expected value (true).", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * - Object is mocked to return false
     * Expects:
     * - Result should match expected value
     */
    @Test
    public void test_getResponseIsHtml_htmlunitUnexpectedPage_false() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).isHtmlPage();
                    will(returnValue(false));
                }
            });
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitUnexpectedPage);
            assertFalse("Assertion did not match the expected value (false).", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Argument: com.gargoylesoftware.htmlunit.UnexpectedPage
     * - Object is mocked to return true
     * Expects:
     * - Result should match expected value
     */
    @Test
    public void test_getResponseIsHtml_htmlunitUnexpectedPage_true() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitUnexpectedPage).isHtmlPage();
                    will(returnValue(true));
                }
            });
            boolean result = WebResponseUtils.getResponseIsHtml(htmlunitUnexpectedPage);
            assertTrue("Assertion did not match the expected value (true).", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
