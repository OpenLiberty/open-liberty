/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.slo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;

import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class SLOPostLogoutPageBuilderTest extends CommonTestClass {

    public static final String LOGOUT_MESSAGE_SUCCESS = "You successfully logged out.";
    public static final String LOGOUT_MESSAGE_FAILURE = "You might not be completely logged out. Close your browser to completely end your session.";

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.saml.*=all");

    @Rule
    public TestRule managerRule = outputMgr;

    static final String CWWKS5213E_POST_LOGOUT_PAGE_MISSING_MESSAGE_CONTEXT = "CWWKS5213E";

    SLOPostLogoutPageBuilder builder = null;

    final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    final PrintWriter writer = mockery.mock(PrintWriter.class);
    final BasicMessageContext<?, ?, ?> msgCtx = mockery.mock(BasicMessageContext.class);
    final Status sloResponseStatus = mockery.mock(Status.class);
    final StatusCode sloStatusCode = mockery.mock(StatusCode.class);

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        builder = new SLOPostLogoutPageBuilder(request, msgCtx);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    /************************************** writeDefaultLogoutPage **************************************/

    /**
     * Tests:
     * - Exception is thrown when writing the default logout page
     * Expects:
     * - The exception should be re-thrown as-is
     */
    @Test
    public void test_writeDefaultLogoutPage_exceptionThrown() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(response).getWriter();
                will(throwException(new IOException(defaultExceptionMsg)));
            }
        });
        try {
            builder.writeDefaultLogoutPage(response);
            fail("Should have thrown an exception but did not.");
        } catch (IOException e) {
            verifyException(e, Pattern.quote(defaultExceptionMsg));
        }
    }

    /**
     * Tests:
     * - Successful execution
     * Expects:
     * - Page HTML content should be written to response
     */
    @Test
    public void test_writeDefaultLogoutPage() throws Exception {
        final String content = "<html></html>";
        builder = new SLOPostLogoutPageBuilder(request, msgCtx) {
            @Override
            String getPostLogoutPageHtml() {
                return content;
            }
        };
        mockery.checking(new Expectations() {
            {
                one(response).getWriter();
                will(returnValue(writer));
                one(writer).println(content);
                one(writer).close();
            }
        });
        builder.writeDefaultLogoutPage(response);
    }

    /************************************** getPostLogoutPageHtml **************************************/

    /**
     * Tests:
     * - Normal execution
     * Expects:
     * - HTML result will match expected HTML document pattern and include <head> and <body> sections
     */
    @Test
    public void test_getPostLogoutPageHtml() {
        final String htmlLang = "lang=\"en\"";
        final String htmlHead = "<head>\n</head>";
        final String htmlBody = "<body>\n</body>";
        builder = new SLOPostLogoutPageBuilder(request, msgCtx) {
            @Override
            String getHtmlLang() {
                return htmlLang;
            }

            @Override
            String createHtmlHead() {
                return htmlHead;
            }

            @Override
            String createHtmlBody() {
                return htmlBody;
            }
        };

        String result = builder.getPostLogoutPageHtml();

        Pattern htmlPattern = Pattern.compile("<!DOCTYPE html>\\s*<html " + htmlLang + ">.+</html>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        verifyPatternMatches(result, htmlPattern, "Result did not conform to the expected HTML document pattern.");

        Pattern headPattern = Pattern.compile("<head>.+</head>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        verifyPatternExists(result, headPattern, "Result did not contain the expected HTML head text.");

        Pattern bodyPattern = Pattern.compile("<body>.+</body>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        verifyPatternExists(result, bodyPattern, "Result did not contain the expected HTML body text.");
    }

    /************************************** getHtmlLang **************************************/

    /**
     * Tests:
     * - Handler instantiated with null request object
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_getHtmlLang_nullRequest() {
        builder = new SLOPostLogoutPageBuilder(null, msgCtx);

        String result = builder.getHtmlLang();
        assertEquals("Result should have been an empty string.", "", result);
    }

    /**
     * Tests:
     * - Request locale is English
     * Expects:
     * - Result should be "lang" attribute set to "en"
     */
    @Test
    public void test_getHtmlLang() {
        final String localeString = "en";
        setLocaleExpectations(localeString);

        String result = builder.getHtmlLang();
        assertEquals("Result did not match the expected value.", "lang=\"" + localeString + "\"", result);
    }

    /************************************** createHtmlHead **************************************/

    /**
     * Tests:
     * - Request locale is English
     * Expects:
     * - Result should be enclosed in HTML <head> attributes
     * - Result should contain a <meta> tag and a <title> value
     */
    @Test
    public void test_createHtmlHead() {
        final String localeString = "en";
        setLocaleExpectations(localeString);

        String result = builder.createHtmlHead();

        Pattern headPattern = Pattern.compile("<head>.+</head>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        verifyPatternMatches(result, headPattern, "Result did not conform to the expected <head> section pattern.");

        String metaRegex = "<meta\\s*([^>]+)?>";
        verifyPattern(result, metaRegex, "Result did not contain a meta tag in the expected format.");
        String titleRegex = "<title>.+</title>";
        verifyPattern(result, titleRegex, "Result did not contain a title tag in the expected format.");
    }

    /************************************** getHtmlTitle **************************************/

    /**
     * Tests:
     * - Normal execution
     * Expects:
     * - Should have the appropriate translated title value
     */
    @Test
    public void test_getHtmlTitle() {
        final String localeString = "en";
        setLocaleExpectations(localeString);

        String result = builder.getHtmlTitle();

        String expectedTitle = "SAML Single Logout (SLO) Post-Logout";
        assertEquals("Title did not match expected value.", expectedTitle, result);
    }

    /************************************** createHtmlBody **************************************/

    /**
     * Tests:
     * - Normal execution
     * Expects:
     * - Result should be enclosed in HTML <body> attributes
     * - Result should contain the expected status string
     */
    @Test
    public void test_createHtmlBody() {
        final String localeString = "en";
        final String status = SLOMessageContextUtils.LOGOUT_STATUS_CODE_SUCCESS;
        setLocaleExpectations(localeString);
        setSloResponseStatusExpectations(status);

        String result = builder.createHtmlBody();

        Pattern headPattern = Pattern.compile("<body>.+</body>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        verifyPatternMatches(result, headPattern, "Result did not conform to the expected <body> section pattern.");

        assertTrue("Result did not contain the expected successful logout message. Result was [" + result + "].", result.contains(LOGOUT_MESSAGE_SUCCESS));
    }

    /************************************** getBodyText **************************************/

    /**
     * Tests:
     * - Handler instantiated with null message context
     * Expects:
     * - Result should be the unsuccessful logout message
     */
    @Test
    public void test_getBodyText_nullMessageContext() {
        builder = new SLOPostLogoutPageBuilder(request, null);
        final String localeString = "en";
        setLocaleExpectations(localeString);

        String result = builder.getBodyText();
        assertEquals("Body text did not match the expected value.", LOGOUT_MESSAGE_FAILURE, result);
    }

    /**
     * Tests:
     * - SLO response status is null
     * Expects:
     * - Result should be the unsuccessful logout message
     */
    @Test
    public void test_getBodyText_unknownResponseStatus() {
        final String localeString = "en";
        setLocaleExpectations(localeString);
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getSLOResponseStatus();
                will(returnValue(null));
            }
        });

        String result = builder.getBodyText();
        assertEquals("Body text did not match the expected value.", LOGOUT_MESSAGE_FAILURE, result);
    }

    /**
     * Tests:
     * - SLO response status code value is some non-null string
     * Expects:
     * - Result should be the unsuccessful logout message
     */
    @Test
    public void test_getBodyText_nonNullStatusValue() {
        final String localeString = "en";
        final String statusValue = "200 status";
        setLocaleExpectations(localeString);
        setSloResponseStatusExpectations(statusValue);

        String result = builder.getBodyText();
        assertEquals("Body text did not match the expected value.", LOGOUT_MESSAGE_FAILURE, result);
    }

    /**
     * Tests:
     * - SLO response status code value is successful status string
     * Expects:
     * - Result should be the successful logout message
     */
    @Test
    public void test_getBodyText_successfulStatusValue() {
        final String localeString = "en";
        final String statusValue = SLOMessageContextUtils.LOGOUT_STATUS_CODE_SUCCESS;
        setLocaleExpectations(localeString);
        setSloResponseStatusExpectations(statusValue);

        String result = builder.getBodyText();
        assertEquals("Body text did not match the expected value.", LOGOUT_MESSAGE_SUCCESS, result);
    }

    /************************************** Helper methods **************************************/

    private void setLocaleExpectations(final String localeString) {
        final Locale locale = new Locale(localeString);
        final Vector<Locale> localeV = new Vector<Locale>();
        localeV.add(locale);

        mockery.checking(new Expectations() {
            {
                allowing(request).getLocale();
                will(returnValue(locale));
                allowing(request).getLocales();
                will(returnValue(localeV.elements()));
            }
        });
    }

    private void setSloResponseStatusExpectations(final String status) {
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getSLOResponseStatus();
                will(returnValue(sloResponseStatus));
                one(sloResponseStatus).getStatusCode();
                will(returnValue(sloStatusCode));
                one(sloStatusCode).getValue();
                will(returnValue(status));
            }
        });
    }

}
