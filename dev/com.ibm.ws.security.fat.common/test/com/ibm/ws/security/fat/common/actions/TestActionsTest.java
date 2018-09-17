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
package com.ibm.ws.security.fat.common.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.exceptions.TestActionException;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;
import com.ibm.ws.security.fat.common.web.WebFormUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class TestActionsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private final CommonFatLoggingUtils loggingUtils = mockery.mock(CommonFatLoggingUtils.class);
    private final WebClient webClient = mockery.mock(WebClient.class);
    private final WebFormUtils webFormUtils = mockery.mock(WebFormUtils.class);
    private final WebRequest request = mockery.mock(WebRequest.class);
    private final HtmlPage htmlPage = mockery.mock(HtmlPage.class);
    private final Page page = mockery.mock(Page.class);
    private final Cookie cookie = mockery.mock(Cookie.class);

    private final String url = "http://localhost:8010";
    private final String username = "testuser";
    private final String password = "mypassword";

    TestActions actions = new TestActions();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        actions = new TestActions() {
            @Override
            public WebClient createWebClient() {
                return webClient;
            }

            @Override
            public WebRequest createGetRequest(String url) throws MalformedURLException {
                return request;
            }
        };
        actions.loggingUtils = loggingUtils;
        actions.webFormUtils = webFormUtils;
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

    /************************************** invokeUrl **************************************/

    /**
     * Tests:
     * - Do not provide a WebClient object
     * - Invoke with a valid URL string
     * Expects:
     * - Response object should be returned
     */
    @Test
    public void test_invokeUrl_withoutWebClient_withUrl() {
        try {
            printMethodNameExpectation();
            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webClient).getPage(request);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.invokeUrl(testName.getMethodName(), url);
            assertEquals("Resulting page object did not point to expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL string is null
     * Expects:
     * - Exception should be thrown saying the web request couldn't be created because the URL is malformed
     */
    @Test
    public void test_invokeUrl_nullUrl() {
        try {
            actions = new TestActions();
            actions.loggingUtils = loggingUtils;

            String url = null;

            printMethodNameExpectation();
            try {
                actions.invokeUrl(testName.getMethodName(), webClient, url);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, ".*error occurred invoking the URL.*MalformedURLException");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL string is not a valid URL
     * Expects:
     * - Exception should be thrown saying the web request couldn't be created because the URL is malformed
     */
    @Test
    public void test_invokeUrl_invalidUrl() {
        try {
            actions = new TestActions();
            actions.loggingUtils = loggingUtils;

            String url = "some invalid string";

            printMethodNameExpectation();
            try {
                actions.invokeUrl(testName.getMethodName(), webClient, url);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, ".*error occurred invoking the URL.*MalformedURLException");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL string is a valid URL
     * - Provided WebClient is null
     * Expects:
     * - New web client will be created for us
     * - Response object should be returned
     */
    @Test
    public void test_invokeUrl_validUrl_nullWebClient() {
        try {
            printMethodNameExpectation();
            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webClient).getPage(request);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.invokeUrl(testName.getMethodName(), null, url);
            assertEquals("Resulting page object did not point to expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL string is a valid URL
     * Expects:
     * - Response object should be returned
     */
    @Test
    public void test_invokeUrl_validUrl() {
        try {
            printMethodNameExpectation();
            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webClient).getPage(request);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.invokeUrl(testName.getMethodName(), webClient, url);
            assertEquals("Resulting page object did not point to expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** invokeUrlWithCookie **************************************/

    /**
     * Tests:
     * - Cookie is null
     * Expects:
     * - Exception should be thrown because of the null cookie
     */
    @Test
    public void test_invokeUrlWithCookie_nullCookie() {
        try {
            printMethodNameExpectation();
            try {
                Page result = actions.invokeUrlWithCookie(testName.getMethodName(), url, null);
                fail("Should have thrown an exception but got a page result: " + WebResponseUtils.getResponseText(result));
            } catch (Exception e) {
                verifyException(e, "error occurred invoking the URL.*null cookie");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL string is not a valid URL
     * Expects:
     * - Exception should be thrown saying the web request couldn't be created because the URL is malformed
     */
    @Test
    public void test_invokeUrlWithCookie_invalidUrl() {
        try {
            actions = new TestActions();
            actions.loggingUtils = loggingUtils;

            String url = "some invalid string";

            printMethodNameExpectation();
            try {
                Page result = actions.invokeUrlWithCookie(testName.getMethodName(), url, cookie);
                fail("Should have thrown an exception but got a page result: " + WebResponseUtils.getResponseText(result));
            } catch (Exception e) {
                verifyException(e, ".*error occurred invoking the URL.*MalformedURLException");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL string is a valid URL
     * - Cookie is valid
     * Expects:
     * - Response object should be returned
     */
    @Test
    public void test_invokeUrlWithCookie_validUrl() {
        try {
            final String cookieName = "myCookie";
            final String cookieValue = "cookieValue";
            printMethodNameExpectation();
            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(cookie).getName();
                    will(returnValue(cookieName));
                    one(cookie).getValue();
                    will(returnValue(cookieValue));
                    one(request).setAdditionalHeader("Cookie", cookieName + "=" + cookieValue);
                    one(webClient).getPage(request);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.invokeUrlWithCookie(testName.getMethodName(), url, cookie);
            assertEquals("Resulting page object did not point to expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** invokeUrlWithParameters **************************************/

    /**
     * Tests:
     * - Parameters list is null
     * Expects:
     * - Response object should be returned
     */
    @Test
    public void test_invokeUrlWithParameters_nullParametersList() {
        try {
            printMethodNameExpectation();
            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webClient).getPage(request);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.invokeUrlWithParameters(testName.getMethodName(), webClient, url, null);
            assertEquals("Resulting page object did not point to expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL string is not a valid URL
     * Expects:
     * - Exception should be thrown saying the web request couldn't be created because the URL is malformed
     */
    @Test
    public void test_invokeUrlWithParameters_invalidUrl() {
        try {
            actions = new TestActions();
            actions.loggingUtils = loggingUtils;

            final List<NameValuePair> params = new ArrayList<NameValuePair>();
            String url = "some invalid string";

            printMethodNameExpectation();
            try {
                Page result = actions.invokeUrlWithParameters(testName.getMethodName(), webClient, url, params);
                fail("Should have thrown an exception but got a page result: " + WebResponseUtils.getResponseText(result));
            } catch (Exception e) {
                verifyException(e, ".*error occurred invoking the URL.*MalformedURLException");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL string is a valid URL
     * - Cookie is valid
     * Expects:
     * - Response object should be returned
     */
    @Test
    public void test_invokeUrlWithParameters_withParams() {
        try {
            final List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new NameValuePair("name", "value"));

            printMethodNameExpectation();
            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(request).setRequestParameters(params);
                    one(webClient).getPage(request);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.invokeUrlWithParameters(testName.getMethodName(), webClient, url, params);
            assertEquals("Resulting page object did not point to expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** submitRequest **************************************/

    /**
     * Tests:
     * - Do not provide a WebClient object
     * - Invoke with a valid WebRequest
     * Expects:
     * - Response object should be returned
     */
    @Test
    public void test_submitRequest_withoutWebClient_withWebRequest() {
        try {
            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webClient).getPage(request);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.submitRequest(testName.getMethodName(), request);
            assertEquals("Resulting page object did not point to expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - WebRequest object is null
     * Expects:
     * - Exception should be thrown saying the WebRequest object is null
     */
    @Test
    public void test_submitRequest_nullWebRequest() {
        try {
            WebClient wc = null;
            WebRequest request = null;

            printMethodNameExpectation();
            try {
                actions.submitRequest(testName.getMethodName(), wc, request);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "Cannot invoke the URL.* WebRequest.* is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - WebClient object is null
     * - Exception thrown while getting response
     * Expects:
     * - TestActionException should be thrown saying an error occurred while invoking the URL
     */
    @Test
    public void test_submitRequest_nullWebClient_exceptionThrownGettingResponse() {
        try {
            WebClient wc = null;

            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webClient).getPage(request);
                    will(throwException(new IOException(defaultExceptionMsg)));
                    one(request).getUrl();
                    will(returnValue(new URL(url)));
                }
            });
            try {
                actions.submitRequest(testName.getMethodName(), wc, request);
                fail("Should have thrown an exception but did not.");
            } catch (TestActionException e) {
                verifyException(e, ".*error occurred while submitting a request to \\[" + url + "\\].*" + Pattern.quote(defaultExceptionMsg));
                assertStringInTrace(outputMgr, "Exception occurred");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL invocation succeeds
     * Expects:
     * - Response object should be returned
     */
    @Test
    public void test_submitRequest_successful() {
        try {
            final String testName = null;

            printMethodNameExpectation();
            printRequestPartsExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webClient).getPage(request);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.submitRequest(testName, webClient, request);
            assertEquals("Resulting page object did not point to expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** doFormLogin **************************************/

    /**
     * Tests:
     * - Page object is null
     * Expects:
     * - Exception should be thrown saying the page object is null
     */
    @Test
    public void test_doFormLogin_page_nullLoginPage() {
        try {
            Page loginPage = null;

            printMethodNameExpectation();
            try {
                actions.doFormLogin(loginPage, username, password);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "Cannot perform login.* page.* is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Page object is not an instance of HtmlPage
     * Expects:
     * - Exception should be thrown saying the page object is not of the correct type
     */
    @Test
    public void test_doFormLogin_page_notHtmlPage() {
        try {
            printMethodNameExpectation();
            try {
                actions.doFormLogin(page, username, password);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "Cannot perform login.* page.* not a .*HtmlPage.* instance");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - HtmlPage object is null
     * Expects:
     * - Exception should be thrown saying the page object is null
     */
    @Test
    public void test_doFormLogin_htmlPage_nullLoginPage() {
        try {
            HtmlPage loginPage = null;

            printMethodNameExpectation();
            try {
                actions.doFormLogin(loginPage, username, password);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "Cannot perform login.* page.* is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Exception thrown when submitting the login form
     * Expects:
     * - TestActionException should be thrown saying an error occurred while performing the login
     */
    @Test
    public void test_doFormLogin_htmlPage_exceptionThrownSubmittingForm() {
        try {
            printMethodNameExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webFormUtils).getAndSubmitLoginForm(htmlPage, username, password);
                    will(throwException(new Exception(defaultExceptionMsg)));
                }
            });
            try {
                actions.doFormLogin(htmlPage, username, password);
                fail("Should have thrown an exception but did not.");
            } catch (TestActionException e) {
                verifyException(e, ".*error occurred while performing form login." + ".*" + Pattern.quote(defaultExceptionMsg));
                assertStringInTrace(outputMgr, "Exception occurred");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form filled and submitted successfully
     * Expects:
     * - No errors should be logged and the resulting page should be returned
     */
    @Test
    public void test_doFormLogin_htmlPage_successful() {
        try {
            printMethodNameExpectation();
            mockery.checking(new Expectations() {
                {
                    one(webFormUtils).getAndSubmitLoginForm(htmlPage, username, password);
                    will(returnValue(page));
                }
            });
            printResponsePartsExpectation();

            Page result = actions.doFormLogin(htmlPage, username, password);
            assertEquals("Resulting page did not point to expected object.", page, result);

            assertStringNotInTrace(outputMgr, "Exception");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private void printMethodNameExpectation() {
        mockery.checking(new Expectations() {
            {
                one(loggingUtils).printMethodName(with(any(String.class)));
            }
        });
    }

    private void printRequestPartsExpectation() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(loggingUtils).printRequestParts(with(any(WebClient.class)), with(any(WebRequest.class)), with(any(String.class)));
            }
        });
    }

    private void printResponsePartsExpectation() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(loggingUtils).printResponseParts(with(any(Object.class)), with(any(String.class)), with(any(String.class)));
            }
        });
    }

}
