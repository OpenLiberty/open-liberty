/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class AuthorizationRequestUtilsTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final WebUtils webUtils = mockery.mock(WebUtils.class);

    private final String testUrl = "http://localhost:8010/formlogin/SimpleServlet";

    private TestAuthorizationRequestUtils requestUtils;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        requestUtils = new TestAuthorizationRequestUtils();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_getRequestUrl_noQueryString() {
        try {
            createReqUrlExpectations(null);
            String strUrl = requestUtils.getRequestUrl(request);

            assertEquals("The URL must not contain a query string.", testUrl, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getRequestUrl_withQueryString() {
        try {
            final String query = "response_type=code";
            createReqUrlExpectations(query);
            String strUrl = requestUtils.getRequestUrl(request);
            String expect = testUrl + "?" + query;

            assertEquals("The URL must contain the query string.", expect, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getRequestUrl_queryWithSpecialCharacters() {
        try {
            String value = "code>\"><script>alert(100)</script>";
            final String query = "response_type=" + value;
            createReqUrlExpectations(query);
            String strUrl = requestUtils.getRequestUrl(request);
            String expect = testUrl + "?response_type=" + value;

            assertEquals("The URL must contain the unencoded query string.", expect, strUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBaseURL_http() {
        try {
            String expectedBaseUrl = "http://localhost:8020/test";
            mockery.checking(new Expectations() {
                {
                    allowing(request).getScheme();
                    will(returnValue("http"));
                    one(request).getServerPort();
                    will(returnValue(8020));
                    one(request).getServerName();
                    will(returnValue("localhost"));
                    one(request).getContextPath();
                    will(returnValue("/test"));
                }
            });

            String baseUrl = requestUtils.getBaseURL(request);

            assertEquals("The base url should use the server port number.", expectedBaseUrl, baseUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBaseURL_redirectPortFromRequestIsNull() {
        try {
            String expectedBaseUrl = "https://localhost:8020/test";
            createBaseUrlExpectations(null);

            String baseUrl = requestUtils.getBaseURL(request);

            assertEquals("The base url should use the server port number.", expectedBaseUrl, baseUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBaseURL_redirectPortFromRequestIsSameAsServerPort() {
        try {
            String expectedBaseUrl = "https://localhost:8020/test";
            createBaseUrlExpectations(new Integer(8020));

            String baseUrl = requestUtils.getBaseURL(request);

            assertEquals("The base url should use th server port number.", expectedBaseUrl, baseUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBaseURL_redirectPortFromRequestIsDifferentFromServerPort() {
        try {
            String expectedBaseUrl = "https://localhost:8010/test";
            createBaseUrlExpectations(new Integer(8010));

            String baseUrl = requestUtils.getBaseURL(request);

            assertEquals("The base url should use the redirect port.", expectedBaseUrl, baseUrl);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void createReqUrlExpectations(final String queryString) {
        mockery.checking(new Expectations() {
            {
                allowing(request).getScheme();
                will(returnValue("http"));
                one(request).getServerPort();
                will(returnValue(8020));
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(testUrl)));
                one(request).getQueryString();
                will(returnValue(queryString));
            }
        });
    }

    private void createBaseUrlExpectations(Integer redirectPort) {
        mockery.checking(new Expectations() {
            {
                one(webUtils).getRedirectPortFromRequest(request);
                will(returnValue(redirectPort));
                allowing(request).getScheme();
                will(returnValue("https"));
                one(request).getServerPort();
                will(returnValue(8020));
                one(request).getServerName();
                will(returnValue("localhost"));
                one(request).getContextPath();
                will(returnValue("/test"));
            }
        });
    }

    private class TestAuthorizationRequestUtils extends AuthorizationRequestUtils {

        @Override
        WebUtils getWebUtils() {
            return webUtils;
        }

    }

}
