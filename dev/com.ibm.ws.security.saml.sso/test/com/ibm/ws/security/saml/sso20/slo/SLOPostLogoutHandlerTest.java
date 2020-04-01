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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
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

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.utils.HttpRequestInfo;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class SLOPostLogoutHandlerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.saml.*=all");

    @Rule
    public TestRule managerRule = outputMgr;

    SLOPostLogoutHandler handler = null;

    final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    final PrintWriter writer = mockery.mock(PrintWriter.class);
    final BasicMessageContext<?, ?, ?> msgCtx = mockery.mock(BasicMessageContext.class);
    final SsoConfig config = mockery.mock(SsoConfig.class);
    final SLOPostLogoutPageBuilder postLogoutPageBuilder = mockery.mock(SLOPostLogoutPageBuilder.class);
    final Status sloResponseStatus = mockery.mock(Status.class);
    final StatusCode sloStatusCode = mockery.mock(StatusCode.class);
    final HttpRequestInfo httpInfo = mockery.mock(HttpRequestInfo.class);

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        boolean isValidPostLogoutRedirectUrlConfigured();

        void redirectToCustomPostLogoutPage();

        void generateDefaultPostLogoutPage();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        handler = new SLOPostLogoutHandler(request, config, msgCtx);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    /************************************** sendToPostLogoutPage **************************************/

    /**
     * Tests:
     * - Custom post logout URL configured
     * Expects:
     * - Should redirect to custom post logout page
     */
    @Test
    public void test_sendToPostLogoutPage_customPostLogoutPage() throws Exception {
        handler = new SLOPostLogoutHandler(request, config, msgCtx) {
            @Override
            boolean isValidPostLogoutRedirectUrlConfigured() {
                return mockInterface.isValidPostLogoutRedirectUrlConfigured();
            }

            @Override
            void redirectToCustomPostLogoutPage(HttpServletResponse response) {
                mockInterface.redirectToCustomPostLogoutPage();
            }
        };
        mockery.checking(new Expectations() {
            {
                one(mockInterface).isValidPostLogoutRedirectUrlConfigured();
                will(returnValue(true));
                one(mockInterface).redirectToCustomPostLogoutPage();
            }
        });
        handler.sendToPostLogoutPage(response);
    }

    /**
     * Tests:
     * - Custom post logout URL not configured
     * Expects:
     * - Should redirect to default post logout page
     */
    @Test
    public void test_sendToPostLogoutPage_defaultPostLogoutPage() throws Exception {
        handler = new SLOPostLogoutHandler(request, config, msgCtx) {
            @Override
            boolean isValidPostLogoutRedirectUrlConfigured() {
                return mockInterface.isValidPostLogoutRedirectUrlConfigured();
            }

            @Override
            void generateDefaultPostLogoutPage(HttpServletResponse response) {
                mockInterface.generateDefaultPostLogoutPage();
            }
        };
        mockery.checking(new Expectations() {
            {
                one(mockInterface).isValidPostLogoutRedirectUrlConfigured();
                will(returnValue(false));
                one(mockInterface).generateDefaultPostLogoutPage();
            }
        });
        handler.sendToPostLogoutPage(response);
    }

    /************************************** isValidPostLogoutRedirectUrlConfigured **************************************/

    /**
     * Tests:
     * - URL not configured
     * Expects:
     * - Result: false
     */
    @Test
    public void test_isValidPostLogoutRedirectUrlConfigured_notConfigured() {
        final String url = null;
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getCachedRequestInfo();
                will(returnValue(httpInfo));
                one(httpInfo).getFormLogoutExitPage();
                will(returnValue(null));

                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
            }
        });
        assertFalse("Config with missing post logout URL should not be considered to have the URL configured.", handler.isValidPostLogoutRedirectUrlConfigured());
    }

    /**
     * Tests:
     * - URL: ""
     * Expects:
     * - Result: false
     */
    @Test
    public void test_isValidPostLogoutRedirectUrlConfigured_emptyUrlString() {
        final String url = "";
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getCachedRequestInfo();
                will(returnValue(httpInfo));
                one(httpInfo).getFormLogoutExitPage();
                will(returnValue(null));

                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
                allowing(config).getProviderId();
            }
        });
        assertFalse("Config with empty post logout URL should not be considered to have the URL configured.", handler.isValidPostLogoutRedirectUrlConfigured());
    }

    /**
     * Tests:
     * - URL: Some invalid URL
     * Expects:
     * - Result: false
     */
    @Test
    public void test_isValidPostLogoutRedirectUrlConfigured_invalidUrlString() {
        final String url = "not a valid URL";
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getCachedRequestInfo();
                will(returnValue(httpInfo));
                one(httpInfo).getFormLogoutExitPage();
                will(returnValue(null));

                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
                allowing(config).getProviderId();
            }
        });
        assertFalse("Config with empty post logout URL should not be considered to have the URL configured.", handler.isValidPostLogoutRedirectUrlConfigured());
    }

    /**
     * Tests:
     * - URL: Does not start with HTTP or HTTPS
     * Expects:
     * - Result: false
     */
    @Test
    public void test_isValidPostLogoutRedirectUrlConfigured_nonHttpUrl() {
        final String url = "ftp://localhost";
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getCachedRequestInfo();
                will(returnValue(httpInfo));
                one(httpInfo).getFormLogoutExitPage();
                will(returnValue(null));

                one(config).getPostLogoutRedirectUrl();
                allowing(config).getProviderId();
                will(returnValue(url));
            }
        });
        assertFalse("Config with non-HTTP post logout URL should not be considered to have the URL configured.", handler.isValidPostLogoutRedirectUrlConfigured());
    }

    /**
     * Tests:
     * - URL: Valid URL using HTTP
     * Expects:
     * - Result: true
     */
    @Test
    public void test_isValidPostLogoutRedirectUrlConfigured_validHttpUrl() {
        final String url = "http://localhost";
        mockery.checking(new Expectations() {
            {
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
            }
        });
        assertTrue("Config with valid post logout URL should be considered to have the URL configured.", handler.isValidPostLogoutRedirectUrlConfigured());
    }

    /**
     * Tests:
     * - URL: Complex valid URL
     * Expects:
     * - Result: true
     */
    @Test
    public void test_isValidPostLogoutRedirectUrlConfigured_complexUrl() {
        final String url = "https://test.example.com:9443/context/@folder-1/%20/_!@$&'()*+,;=";
        mockery.checking(new Expectations() {
            {
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
            }
        });
        assertTrue("Config with valid post logout URL should be considered to have the URL configured.", handler.isValidPostLogoutRedirectUrlConfigured());
    }

    /************************************** redirectToCustomPostLogoutPage **************************************/

    /**
     * Tests:
     * - Somehow we started the process to redirect to a valid post logout page, but now the post logout URL is not valid
     * Expects:
     * - Should redirect to default post logout page
     */
    @Test
    public void test_redirectToCustomPostLogoutPage_invalidPostLogoutUrlConfiguredRedirectsToDefaultPage() throws Exception {
        handler = new SLOPostLogoutHandler(request, config, msgCtx) {
            @Override
            void generateDefaultPostLogoutPage(HttpServletResponse response) {
                mockInterface.generateDefaultPostLogoutPage();
            }
        };
        final String url = "not a valid URL";
        mockery.checking(new Expectations() {
            {
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
                one(mockInterface).generateDefaultPostLogoutPage();
                allowing(config).getProviderId();
                one(msgCtx).getCachedRequestInfo();
                will(returnValue(httpInfo));
                one(httpInfo).getFormLogoutExitPage();
                will(returnValue(null));
            }
        });
        handler.redirectToCustomPostLogoutPage(response);
    }

    /**
     * Tests:
     * - Valid redirect URL is configured
     * - Unknown logout status
     * Expects:
     * - Should redirect response to the URL with the logout status param set to the UNKNOWN status string
     */
    @Test
    public void test_redirectToCustomPostLogoutPage_validPostLogoutUrlConfigured_unknownLogoutStatus() throws Exception {
        final String url = "https://test.example.com:9443/context/";
        final String logoutStatus = null;
        setSloResponseStatusExpectations(logoutStatus);
        mockery.checking(new Expectations() {
            {
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
                one(response).sendRedirect(url + "?" + SLOPostLogoutHandler.PARAM_LOGOUT_STATUS + "=" + SLOMessageContextUtils.STATUS_UNKNOWN);
            }
        });
        handler.redirectToCustomPostLogoutPage(response);
    }

    /**
     * Tests:
     * - Valid redirect URL is configured
     * - Non-empty, known logout status
     * Expects:
     * - Should redirect response to the URL with the logout status param set to the encoded status string
     */
    @Test
    public void test_redirectToCustomPostLogoutPage_validPostLogoutUrlConfigured_knownLogoutStatus() throws Exception {
        final String url = "http://localhost/context/";
        final String logoutStatus = "Some non-empty status";
        setSloResponseStatusExpectations(logoutStatus);
        mockery.checking(new Expectations() {
            {
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
                one(response).sendRedirect(url + "?" + SLOPostLogoutHandler.PARAM_LOGOUT_STATUS + "=" + URLEncoder.encode(logoutStatus, "UTF-8"));
            }
        });
        handler.redirectToCustomPostLogoutPage(response);
    }

    /************************************** getAndValidatePostLogoutRedirectUrl **************************************/

    /**
     * Tests:
     * - Redirect URL: Null
     * Expects:
     * - Returns null because URL is not valid
     */
    @Test
    public void test_getAndValidatePostLogoutRedirectUrl_urlNotConfigured() {
        final String url = null;
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getCachedRequestInfo();
                will(returnValue(httpInfo));
                one(httpInfo).getFormLogoutExitPage();
                will(returnValue(null));

                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
            }
        });
        String result = handler.getAndValidatePostLogoutRedirectUrl();
        assertNull("Result should have been null because the URL [" + url + "] is not considered valid, but return URL [" + result + "].", result);
    }

    /**
     * Tests:
     * - Redirect URL: ""
     * Expects:
     * - Returns null because URL is not valid
     */
    @Test
    public void test_getAndValidatePostLogoutRedirectUrl_emptyUrl() {
        final String url = "";
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getCachedRequestInfo();
                will(returnValue(httpInfo));
                one(httpInfo).getFormLogoutExitPage();
                will(returnValue(null));

                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
                allowing(config).getProviderId();
            }
        });
        String result = handler.getAndValidatePostLogoutRedirectUrl();
        assertNull("Result should have been null because the URL [" + url + "] is not considered valid, but return URL [" + result + "].", result);
    }

    /**
     * Tests:
     * - Redirect URL: Not a valid URL
     * Expects:
     * - Returns null because URL is not valid
     */
    @Test
    public void test_getAndValidatePostLogoutRedirectUrl_urlNotFormattedCorrectly() {
        final String url = "not a valid URL";
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getCachedRequestInfo();
                will(returnValue(httpInfo));
                one(httpInfo).getFormLogoutExitPage();
                will(returnValue(null));
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
                allowing(config).getProviderId();
            }
        });
        String result = handler.getAndValidatePostLogoutRedirectUrl();
        assertNull("Result should have been null because the URL [" + url + "] is not considered valid, but return URL [" + result + "].", result);
    }

    /**
     * Tests:
     * - Redirect URL: URL that does not use the HTTP or HTTPS protocol
     * Expects:
     * - Returns null because URL is not valid
     */
    //@Test bt: the code to do this is definitely not present.
    public void test_getAndValidatePostLogoutRedirectUrl_nonHttpUrl() {
        final String url = "ftp://localhost";
        mockery.checking(new Expectations() {
            {
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
            }
        });
        String result = handler.getAndValidatePostLogoutRedirectUrl();
        assertNull("Result should have been null because the URL [" + url + "] is not considered valid, but return URL [" + result + "].", result);
    }

    /**
     * Tests:
     * - Redirect URL: Simple URL (not port, context path, etc.)
     * Expects:
     * - Returns the configured URL
     */
    @Test
    public void test_getAndValidatePostLogoutRedirectUrl_simpleValidUrl() {
        final String url = "http://localhost";
        mockery.checking(new Expectations() {
            {
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
            }
        });
        String result = handler.getAndValidatePostLogoutRedirectUrl();
        assertEquals("Returned URL did not match the expected value.", url, result);
    }

    /**
     * Tests:
     * - Redirect URL: Complex URL (contains port, context path, unusual but valid path characters)
     * Expects:
     * - Returns the configured URL
     */
    @Test
    public void test_getAndValidatePostLogoutRedirectUrl_complexValidUrl() {
        final String url = "https://test.example.com:9443/context/@folder-1/%20/_!@$&'()*+,;=";
        mockery.checking(new Expectations() {
            {
                one(config).getPostLogoutRedirectUrl();
                will(returnValue(url));
            }
        });
        String result = handler.getAndValidatePostLogoutRedirectUrl();
        assertEquals("Returned URL did not match the expected value.", url, result);
    }

    /************************************** getCustomPostLogoutQueryString **************************************/

    /**
     * Tests:
     * - Logout status cannot be determined, so results in the UNKNOWN status string
     * Expects:
     * - Should get the usual logout status parameter name with its value set to the UNKNOWN status string
     */
    @Test
    public void test_getCustomPostLogoutQueryString_unknownLogoutStatus() {
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getSLOResponseStatus();
                will(returnValue(null));
            }
        });
        String result = handler.getCustomPostLogoutQueryString();
        String expectedString = SLOPostLogoutHandler.PARAM_LOGOUT_STATUS + "=" + SLOMessageContextUtils.STATUS_UNKNOWN;
        assertEquals("Query string value did not match expected value.", expectedString, result);
    }

    /**
     * Tests:
     * - Successful status value returned
     * Expects:
     * - Should get the usual logout status parameter name with its value set to the status string
     */
    @Test
    public void test_getCustomPostLogoutQueryString_successfulLogoutStatusString() throws Exception {
        final String statusValue = SLOMessageContextUtils.LOGOUT_STATUS_CODE_SUCCESS;
        setSloResponseStatusExpectations(statusValue);
        String result = handler.getCustomPostLogoutQueryString();
        String expectedString = SLOPostLogoutHandler.PARAM_LOGOUT_STATUS + "=" + URLEncoder.encode(statusValue, "UTF-8");
        assertEquals("Query string value did not match expected value.", expectedString, result);
    }

    /************************************** getStatusCodeForQueryString **************************************/

    /**
     * Tests:
     * - Logout status cannot be determined, so results in the UNKNOWN status string
     * Expects:
     * - Should return the UNKNOWN status string
     */
    @Test
    public void test_getStatusCodeForQueryString_unknownLogoutStatus() {
        mockery.checking(new Expectations() {
            {
                one(msgCtx).getSLOResponseStatus();
                will(returnValue(null));
            }
        });
        String result = handler.getStatusCodeForQueryString();
        String expectedString = SLOMessageContextUtils.STATUS_UNKNOWN;
        assertEquals("Status code value did not match expected value.", expectedString, result);
    }

    /**
     * Tests:
     * - No logout status value found in message context
     * Expects:
     * - Should return the UNKNOWN status string
     */
    @Test
    public void test_getStatusCodeForQueryString_nullLogoutStatusString() {
        final String statusValue = null;
        setSloResponseStatusExpectations(statusValue);
        String result = handler.getStatusCodeForQueryString();
        String expectedString = SLOMessageContextUtils.STATUS_UNKNOWN;
        assertEquals("Status code value did not match expected value.", expectedString, result);
    }

    /**
     * Tests:
     * - Empty logout status value returned
     * Expects:
     * - Should return the UNKNOWN status string
     */
    @Test
    public void test_getStatusCodeForQueryString_emptyLogoutStatusString() {
        final String statusValue = "";
        setSloResponseStatusExpectations(statusValue);
        String result = handler.getStatusCodeForQueryString();
        String expectedString = SLOMessageContextUtils.STATUS_UNKNOWN;
        assertEquals("Status code value did not match expected value.", expectedString, result);
    }

    /**
     * Tests:
     * - Non-empty logout status value returned
     * Expects:
     * - Should return the status value as-is
     */
    @Test
    public void test_getStatusCodeForQueryString_nonEmptyLogoutStatusString() {
        final String statusValue = "200 status";
        setSloResponseStatusExpectations(statusValue);
        String result = handler.getStatusCodeForQueryString();
        assertEquals("Status code value did not match expected value.", statusValue, result);
    }

    /**
     * Tests:
     * - Successful logout status value returned
     * Expects:
     * - Should return the status value as-is
     */
    @Test
    public void test_getStatusCodeForQueryString_successfulLogoutStatusString() {
        final String statusValue = SLOMessageContextUtils.LOGOUT_STATUS_CODE_SUCCESS;
        setSloResponseStatusExpectations(statusValue);
        String result = handler.getStatusCodeForQueryString();
        assertEquals("Status code value did not match expected value.", statusValue, result);
    }

    /************************************** generateDefaultPostLogoutPage **************************************/

    /**
     * Tests:
     * - Exception is thrown when writing the default logout page
     * Expects:
     * - The exception should be re-thrown as-is
     */
    @Test
    public void test_generateDefaultPostLogoutPage_exceptionThrown() throws Exception {
        handler = new SLOPostLogoutHandler(request, config, msgCtx) {
            @Override
            SLOPostLogoutPageBuilder getPostLogoutPageBuilder() {
                return postLogoutPageBuilder;
            }
        };
        mockery.checking(new Expectations() {
            {
                one(postLogoutPageBuilder).writeDefaultLogoutPage(response);
                will(throwException(new IOException(defaultExceptionMsg)));
            }
        });
        try {
            handler.generateDefaultPostLogoutPage(response);
            fail("Should have thrown an exception but did not.");
        } catch (IOException e) {
            verifyException(e, Pattern.quote(defaultExceptionMsg));
        }
    }

    /**
     * Tests:
     * - Writing the default page HTML is successful
     */
    @Test
    public void test_generateDefaultPostLogoutPage() throws Exception {
        handler = new SLOPostLogoutHandler(request, config, msgCtx) {
            @Override
            SLOPostLogoutPageBuilder getPostLogoutPageBuilder() {
                return postLogoutPageBuilder;
            }
        };
        mockery.checking(new Expectations() {
            {
                one(postLogoutPageBuilder).writeDefaultLogoutPage(response);
            }
        });
        handler.generateDefaultPostLogoutPage(response);
    }

    /************************************** getPostLogoutPageBuilder **************************************/

    /**
     * Tests:
     * - Instantiate a post logout page builder object
     * Expects:
     * - Returned object should not be null
     */
    @Test
    public void test_getPostLogoutPageBuilder() {
        SLOPostLogoutPageBuilder result = handler.getPostLogoutPageBuilder();
        assertNotNull("Result should not have been null but was.", result);
    }

    /************************************** Helper methods **************************************/

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
