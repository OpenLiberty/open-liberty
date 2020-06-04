/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oauth20.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.oauth20.web.EndpointUtils;
import com.ibm.ws.security.oauth20.web.OAuth20Request;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import test.common.SharedOutputManager;

@SuppressWarnings("restriction")
public class OAuthSupportedHttpMethodHandlerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.oauth.*=all:com.ibm.ws.security.oauth*=all");

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final EndpointUtils endpointUtils = mockery.mock(EndpointUtils.class);
    private final OAuth20Request oauth20Request = mockery.mock(OAuth20Request.class);

    private OAuthSupportedHttpMethodHandler handler;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        handler = new OAuthSupportedHttpMethodHandler(request, response);
        handler.endpointUtils = endpointUtils;
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_isValidHttpMethodForRequest_missingOAuth20RequestInfo() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
                one(request).getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
            }
        });
        boolean result = handler.isValidHttpMethodForRequest(HttpMethod.GET);
        assertFalse("Should not have been a valid HTTP method for a request missing OAuth20Request information.", result);
    }

    @Test
    public void test_isValidHttpMethodForRequest_unknownEndpoint() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
                one(oauth20Request).getType();
                will(returnValue(null));
            }
        });
        boolean result = handler.isValidHttpMethodForRequest(HttpMethod.GET);
        assertFalse("Should not have been a valid HTTP method for an unknown endpoint.", result);
    }

    @Test
    public void test_isValidHttpMethodForRequest_knownEndpoint_unsupportedHttpMethod() throws IOException {
        final EndpointType endpoint = EndpointType.introspect;
        final HttpMethod requestMethod = HttpMethod.GET;
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
                one(oauth20Request).getType();
                will(returnValue(endpoint));
            }
        });
        boolean result = handler.isValidHttpMethodForRequest(requestMethod);
        assertFalse("HTTP method [" + requestMethod + "] should not have been supported for endpoint [" + endpoint + "].", result);
    }

    @Test
    public void test_isValidHttpMethodForRequest_knownEndpoint_ssupportedHttpMethod() throws IOException {
        final EndpointType endpoint = EndpointType.authorize;
        final HttpMethod requestMethod = HttpMethod.GET;
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
                one(oauth20Request).getType();
                will(returnValue(endpoint));
            }
        });
        boolean result = handler.isValidHttpMethodForRequest(requestMethod);
        assertTrue("HTTP method [" + requestMethod + "] should have been supported for endpoint [" + endpoint + "].", result);
    }

    @Test
    public void test_sendHttpOptionsResponse_missingOAuth20RequestInfo() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
                one(request).getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
                one(response).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        });
        handler.sendHttpOptionsResponse();
    }

    @Test
    public void test_sendHttpOptionsResponse_unknownEndpoint() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
                one(oauth20Request).getType();
                will(returnValue(null));
                one(response).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        });
        handler.sendHttpOptionsResponse();
    }

    @Test
    public void test_sendHttpOptionsResponse_knownEndpoint() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
                one(oauth20Request).getType();
                will(returnValue(EndpointType.introspect));
                // There are multiple supported methods, so we can't know in which order they'll be output
                one(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setStatus(HttpServletResponse.SC_OK);
            }
        });
        handler.sendHttpOptionsResponse();
    }

    @Test
    public void test_getEndpointType_missingOAuthRequestInfo() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
                one(request).getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
            }
        });
        EndpointType endpoint = handler.getEndpointType();
        assertNull("Expected endpoint to be null but was [" + endpoint + "].", endpoint);
    }

    @Test
    public void test_getEndpointType_nullEndpointType() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
                one(oauth20Request).getType();
                will(returnValue(null));
            }
        });
        EndpointType endpoint = handler.getEndpointType();
        assertNull("Expected endpoint to be null but was [" + endpoint + "].", endpoint);
    }

    @Test
    public void test_getEndpointType() throws IOException {
        final EndpointType expectedEndpoint = EndpointType.introspect;
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
                one(oauth20Request).getType();
                will(returnValue(expectedEndpoint));
            }
        });
        EndpointType endpoint = handler.getEndpointType();
        assertEquals("Returned endpoint did not match expected value.", expectedEndpoint, endpoint);
    }

    @Test
    public void test_getSupportedMethodsForEndpoint_authorize() throws IOException {
        Set<HttpMethod> supportedMethods = handler.getSupportedMethodsForEndpoint(EndpointType.authorize);
        assertNotNull("Set of supported methods should not have been null but was.", supportedMethods);
        HttpMethod[] expectedMethods = new HttpMethod[] { HttpMethod.OPTIONS, HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST };
        assertEquals("Did not find the expected number of supported HTTP methods.", expectedMethods.length, supportedMethods.size());
        for (HttpMethod expectedMethod : expectedMethods) {
            assertTrue("Did not find " + expectedMethod + " in the set of supported HTTP methods.", supportedMethods.contains(expectedMethod));
        }
    }

    @Test
    public void test_getSupportedMethodsForEndpoint_token() throws IOException {
        Set<HttpMethod> supportedMethods = handler.getSupportedMethodsForEndpoint(EndpointType.token);
        assertNotNull("Set of supported methods should not have been null but was.", supportedMethods);
        HttpMethod[] expectedMethods = new HttpMethod[] { HttpMethod.OPTIONS, HttpMethod.POST };
        assertEquals("Did not find the expected number of supported HTTP methods.", expectedMethods.length, supportedMethods.size());
        for (HttpMethod expectedMethod : expectedMethods) {
            assertTrue("Did not find " + expectedMethod + " in the set of supported HTTP methods.", supportedMethods.contains(expectedMethod));
        }
    }

}
