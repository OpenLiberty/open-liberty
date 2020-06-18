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
import java.util.HashSet;
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
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.web.OAuth20Request;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import io.openliberty.security.oauth20.internal.config.OAuthEndpointSettings;
import io.openliberty.security.oauth20.internal.config.SpecificOAuthEndpointSettings;
import test.common.SharedOutputManager;

@SuppressWarnings("restriction")
public class OAuthSupportedHttpMethodHandlerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.oauth.*=all:com.ibm.ws.security.oauth*=all");

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final OAuth20Request oauth20Request = mockery.mock(OAuth20Request.class);
    private final OAuth20Provider oauth20Provider = mockery.mock(OAuth20Provider.class);
    private final OAuthEndpointSettings oauthEndpointSettings = mockery.mock(OAuthEndpointSettings.class);
    private final SpecificOAuthEndpointSettings specificOAuthEndpointSettings = mockery.mock(SpecificOAuthEndpointSettings.class);

    private final String providerName = "MyOAuthProvider";

    private OAuthSupportedHttpMethodHandler handler;

    class TestOAuthSupportedHttpMethodHandler extends OAuthSupportedHttpMethodHandler {
        public TestOAuthSupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }

        @Override
        protected OAuth20Provider getOAuth20Provider() {
            return oauth20Provider;
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
                one(oauth20Request).getProviderName();
                will(returnValue(providerName));
            }
        });
        handler = new TestOAuthSupportedHttpMethodHandler(request, response);
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
            }
        });
        handler = new TestOAuthSupportedHttpMethodHandler(request, response);
        boolean result = handler.isValidHttpMethodForRequest(HttpMethod.GET);
        assertFalse("Should not have been a valid HTTP method for a request missing OAuth20Request information.", result);
    }

    @Test
    public void test_isValidHttpMethodForRequest_unknownEndpoint() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(oauth20Request).getType();
                will(returnValue(null));
            }
        });
        boolean result = handler.isValidHttpMethodForRequest(HttpMethod.GET);
        assertFalse("Should not have been a valid HTTP method for an unknown endpoint.", result);
    }

    @Test
    public void test_isValidHttpMethodForRequest_knownEndpoint_unsupportedHttpMethod() throws IOException {
        final EndpointType endpoint = EndpointType.token;
        final HttpMethod requestMethod = HttpMethod.GET;
        mockery.checking(new Expectations() {
            {
                one(oauth20Request).getType();
                will(returnValue(endpoint));
            }
        });
        setOAuthProviderExpectations(null);

        boolean result = handler.isValidHttpMethodForRequest(requestMethod);
        assertFalse("HTTP method [" + requestMethod + "] should not have been supported for endpoint [" + endpoint + "].", result);
    }

    @Test
    public void test_isValidHttpMethodForRequest_knownEndpoint_supportedHttpMethod() throws IOException {
        final EndpointType endpoint = EndpointType.authorize;
        final HttpMethod requestMethod = HttpMethod.GET;
        mockery.checking(new Expectations() {
            {
                one(oauth20Request).getType();
                will(returnValue(endpoint));
            }
        });
        setOAuthProviderExpectations(null);

        boolean result = handler.isValidHttpMethodForRequest(requestMethod);
        assertTrue("HTTP method [" + requestMethod + "] should have been supported for endpoint [" + endpoint + "].", result);
    }

    @Test
    public void test_sendHttpOptionsResponse_missingOAuth20RequestInfo() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
            }
        });
        handler = new TestOAuthSupportedHttpMethodHandler(request, response);
        mockery.checking(new Expectations() {
            {
                one(response).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        });
        handler.sendHttpOptionsResponse();
    }

    @Test
    public void test_sendHttpOptionsResponse_unknownEndpoint() throws IOException {
        mockery.checking(new Expectations() {
            {
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
                one(oauth20Request).getType();
                will(returnValue(EndpointType.introspect));
                // There are multiple supported methods, so we can't know in which order they'll be output
                one(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setStatus(HttpServletResponse.SC_OK);
            }
        });
        setOAuthProviderExpectations(null);

        handler.sendHttpOptionsResponse();
    }

    @Test
    public void test_getEndpointType_missingOAuthRequestInfo() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
            }
        });
        handler = new TestOAuthSupportedHttpMethodHandler(request, response);
        EndpointType endpoint = handler.getEndpointType();
        assertNull("Expected endpoint to be null but was [" + endpoint + "].", endpoint);
    }

    @Test
    public void test_getEndpointType_nullEndpointType() throws IOException {
        mockery.checking(new Expectations() {
            {
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
                one(oauth20Request).getType();
                will(returnValue(expectedEndpoint));
            }
        });
        EndpointType endpoint = handler.getEndpointType();
        assertEquals("Returned endpoint did not match expected value.", expectedEndpoint, endpoint);
    }

    @Test
    public void test_getSupportedMethodsForEndpoint_nullEndpoint() {
        Set<HttpMethod> supportedMethods = handler.getSupportedMethodsForEndpoint(null);
        assertNull("Set of supported HTTP methods should have been null but was " + supportedMethods, supportedMethods);
    }

    @Test
    public void test_getSupportedMethodsForEndpoint_endpointNotConfiguredToLimitAnyMethods() {
        EndpointType endpoint = EndpointType.app_password;
        setOAuthProviderExpectations(null);
        Set<HttpMethod> supportedMethods = handler.getSupportedMethodsForEndpoint(endpoint);
        assertNotNull("Set of supported methods should not have been null but was.", supportedMethods);
        assertTrue("Did not find " + HttpMethod.GET + " in the set of supported HTTP methods.", supportedMethods.contains(HttpMethod.GET));
        assertTrue("Did not find " + HttpMethod.HEAD + " in the set of supported HTTP methods.", supportedMethods.contains(HttpMethod.HEAD));
        // OPTIONS should always be supported
        assertTrue("Did not find " + HttpMethod.OPTIONS + " in the set of supported HTTP methods.", supportedMethods.contains(HttpMethod.OPTIONS));
    }

    @Test
    public void test_getSupportedMethodsForEndpoint_configurationLimitsMethods() {
        EndpointType endpoint = EndpointType.introspect;
        final Set<HttpMethod> configuredSupportedMethods = new HashSet<HttpMethod>();
        configuredSupportedMethods.add(HttpMethod.POST);

        setOAuthProviderExpectations(oauthEndpointSettings);
        mockery.checking(new Expectations() {
            {
                one(oauthEndpointSettings).getSpecificOAuthEndpointSettings(endpoint);
                will(returnValue(specificOAuthEndpointSettings));
                one(specificOAuthEndpointSettings).getSupportedHttpMethods();
                will(returnValue(configuredSupportedMethods));
            }
        });
        Set<HttpMethod> supportedMethods = handler.getSupportedMethodsForEndpoint(endpoint);
        assertNotNull("Set of supported methods should not have been null but was.", supportedMethods);
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + supportedMethods, 2, supportedMethods.size());
        assertTrue("Did not find " + HttpMethod.POST + " in the set of supported HTTP methods.", supportedMethods.contains(HttpMethod.POST));
        // OPTIONS should always be supported
        assertTrue("Did not find " + HttpMethod.OPTIONS + " in the set of supported HTTP methods.", supportedMethods.contains(HttpMethod.OPTIONS));
    }

    @Test
    public void test_getDefaultSupportedMethodsForEndpoint_authorize() throws IOException {
        Set<HttpMethod> supportedMethods = handler.getDefaultSupportedMethodsForEndpoint(EndpointType.authorize);
        assertNotNull("Set of supported methods should not have been null but was.", supportedMethods);
        HttpMethod[] expectedMethods = new HttpMethod[] { HttpMethod.OPTIONS, HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST };
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + supportedMethods, expectedMethods.length, supportedMethods.size());
        for (HttpMethod expectedMethod : expectedMethods) {
            assertTrue("Did not find " + expectedMethod + " in the set of supported HTTP methods.", supportedMethods.contains(expectedMethod));
        }
    }

    @Test
    public void test_getDefaultSupportedMethodsForEndpoint_token() throws IOException {
        Set<HttpMethod> supportedMethods = handler.getDefaultSupportedMethodsForEndpoint(EndpointType.token);
        assertNotNull("Set of supported methods should not have been null but was.", supportedMethods);
        HttpMethod[] expectedMethods = new HttpMethod[] { HttpMethod.OPTIONS, HttpMethod.POST };
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + supportedMethods, expectedMethods.length, supportedMethods.size());
        for (HttpMethod expectedMethod : expectedMethods) {
            assertTrue("Did not find " + expectedMethod + " in the set of supported HTTP methods.", supportedMethods.contains(expectedMethod));
        }
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint_missingEndpointSettingsConfig() {
        setOAuthProviderExpectations(null);

        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(EndpointType.introspect);
        assertNull("Set of configured supported HTTP methods should have been null but was " + supportedMethods, supportedMethods);
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint_noSpecificSettingsForEndpoint() {
        final EndpointType endpointType = EndpointType.jwk;
        setOAuthProviderExpectations(oauthEndpointSettings);
        mockery.checking(new Expectations() {
            {
                one(oauthEndpointSettings).getSpecificOAuthEndpointSettings(endpointType);
                will(returnValue(null));
            }
        });

        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(endpointType);
        assertNull("Set of configured supported HTTP methods should have been null but was " + supportedMethods, supportedMethods);
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint_supportedMethodsNull() {
        final EndpointType endpointType = EndpointType.token;
        setOAuthProviderExpectations(oauthEndpointSettings);
        mockery.checking(new Expectations() {
            {
                one(oauthEndpointSettings).getSpecificOAuthEndpointSettings(endpointType);
                will(returnValue(specificOAuthEndpointSettings));
                one(specificOAuthEndpointSettings).getSupportedHttpMethods();
                will(returnValue(null));
            }
        });

        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(endpointType);
        assertNull("Set of configured supported HTTP methods should have been null but was " + supportedMethods, supportedMethods);
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint_supportedMethodsEmpty() {
        final EndpointType endpointType = EndpointType.app_password;
        final Set<HttpMethod> specificSupportedMethods = new HashSet<HttpMethod>();
        setOAuthProviderExpectations(oauthEndpointSettings);
        mockery.checking(new Expectations() {
            {
                one(oauthEndpointSettings).getSpecificOAuthEndpointSettings(endpointType);
                will(returnValue(specificOAuthEndpointSettings));
                one(specificOAuthEndpointSettings).getSupportedHttpMethods();
                will(returnValue(specificSupportedMethods));
            }
        });

        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(endpointType);
        assertEquals("Set of supported HTTP methods did not match expected value.", specificSupportedMethods, supportedMethods);
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint_supportedMethodsNonEmpty() {
        final EndpointType endpointType = EndpointType.registration;
        final Set<HttpMethod> specificSupportedMethods = new HashSet<HttpMethod>();
        specificSupportedMethods.add(HttpMethod.GET);
        specificSupportedMethods.add(HttpMethod.POST);
        specificSupportedMethods.add(HttpMethod.DELETE);
        setOAuthProviderExpectations(oauthEndpointSettings);
        mockery.checking(new Expectations() {
            {
                one(oauthEndpointSettings).getSpecificOAuthEndpointSettings(endpointType);
                will(returnValue(specificOAuthEndpointSettings));
                one(specificOAuthEndpointSettings).getSupportedHttpMethods();
                will(returnValue(specificSupportedMethods));
            }
        });

        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(endpointType);
        assertEquals("Set of supported HTTP methods did not match expected value.", specificSupportedMethods, supportedMethods);
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_defaultNull_configuredNull() {
        Set<HttpMethod> defaultSupportedMethods = null;
        Set<HttpMethod> configuredSupportedMethods = null;
        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNull("Adjusted set of supported HTTP methods should have been null but was " + adjustedSupportedMethods, adjustedSupportedMethods);
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_defaultEmpty_configuredEmpty() {
        Set<HttpMethod> defaultSupportedMethods = new HashSet<HttpMethod>();
        Set<HttpMethod> configuredSupportedMethods = new HashSet<HttpMethod>();
        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNotNull("Adjusted set of supported HTTP methods should not have been null.", adjustedSupportedMethods);
        assertTrue("Adjusted set of supported HTTP methods should have been empty but was " + adjustedSupportedMethods, adjustedSupportedMethods.isEmpty());
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_defaultEmpty_configuredNonEmpty() {
        Set<HttpMethod> defaultSupportedMethods = new HashSet<HttpMethod>();
        Set<HttpMethod> configuredSupportedMethods = new HashSet<HttpMethod>();
        configuredSupportedMethods.add(HttpMethod.GET);

        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNotNull("Adjusted set of supported HTTP methods should not have been null.", adjustedSupportedMethods);
        assertTrue("Adjusted set of supported HTTP methods should have been empty but was " + adjustedSupportedMethods, adjustedSupportedMethods.isEmpty());
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_defaultNonEmpty_configuredNull() {
        Set<HttpMethod> defaultSupportedMethods = new HashSet<HttpMethod>();
        defaultSupportedMethods.add(HttpMethod.GET);
        defaultSupportedMethods.add(HttpMethod.HEAD);
        Set<HttpMethod> configuredSupportedMethods = null;

        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNotNull("Adjusted set of supported HTTP methods should not have been null.", adjustedSupportedMethods);
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + adjustedSupportedMethods, defaultSupportedMethods.size() + 1, adjustedSupportedMethods.size());
        for (HttpMethod defaultMethod : defaultSupportedMethods) {
            assertTrue("Adjusted set of supported HTTP methods did not contain expected method [" + defaultMethod + "]. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(defaultMethod));
        }
        assertTrue("Adjusted set of supported HTTP methods should have contained [" + HttpMethod.OPTIONS + "] but didn't. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(HttpMethod.OPTIONS));
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_defaultNonEmpty_configuredEmpty() {
        Set<HttpMethod> defaultSupportedMethods = new HashSet<HttpMethod>();
        defaultSupportedMethods.add(HttpMethod.GET);
        Set<HttpMethod> configuredSupportedMethods = new HashSet<HttpMethod>();

        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNotNull("Adjusted set of supported HTTP methods should not have been null.", adjustedSupportedMethods);
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + adjustedSupportedMethods, 1, adjustedSupportedMethods.size());
        assertTrue("Adjusted set of supported HTTP methods should have contained [" + HttpMethod.OPTIONS + "] but didn't. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(HttpMethod.OPTIONS));
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_disjointSets() {
        Set<HttpMethod> defaultSupportedMethods = new HashSet<HttpMethod>();
        defaultSupportedMethods.add(HttpMethod.GET);
        defaultSupportedMethods.add(HttpMethod.HEAD);
        Set<HttpMethod> configuredSupportedMethods = new HashSet<HttpMethod>();
        configuredSupportedMethods.add(HttpMethod.POST);
        configuredSupportedMethods.add(HttpMethod.PUT);

        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNotNull("Adjusted set of supported HTTP methods should not have been null.", adjustedSupportedMethods);
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + adjustedSupportedMethods, 1, adjustedSupportedMethods.size());
        assertTrue("Adjusted set of supported HTTP methods should have contained [" + HttpMethod.OPTIONS + "] but didn't. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(HttpMethod.OPTIONS));
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_defaultSameAsConfigured() {
        HttpMethod sharedMethod = HttpMethod.DELETE;
        Set<HttpMethod> defaultSupportedMethods = new HashSet<HttpMethod>();
        defaultSupportedMethods.add(sharedMethod);
        Set<HttpMethod> configuredSupportedMethods = new HashSet<HttpMethod>();
        configuredSupportedMethods.add(sharedMethod);

        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNotNull("Adjusted set of supported HTTP methods should not have been null.", adjustedSupportedMethods);
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + adjustedSupportedMethods, defaultSupportedMethods.size() + 1, adjustedSupportedMethods.size());
        assertTrue("Adjusted set of supported HTTP methods did not contain expected method [" + sharedMethod + "]. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(sharedMethod));
        assertTrue("Adjusted set of supported HTTP methods should have contained [" + HttpMethod.OPTIONS + "] but didn't. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(HttpMethod.OPTIONS));
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_oneCommonMethod() {
        HttpMethod sharedMethod = HttpMethod.POST;
        Set<HttpMethod> defaultSupportedMethods = new HashSet<HttpMethod>();
        defaultSupportedMethods.add(sharedMethod);
        defaultSupportedMethods.add(HttpMethod.DELETE);
        defaultSupportedMethods.add(HttpMethod.PUT);
        Set<HttpMethod> configuredSupportedMethods = new HashSet<HttpMethod>();
        configuredSupportedMethods.add(HttpMethod.GET);
        configuredSupportedMethods.add(HttpMethod.HEAD);
        configuredSupportedMethods.add(sharedMethod);

        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNotNull("Adjusted set of supported HTTP methods should not have been null.", adjustedSupportedMethods);
        assertEquals("Adjusted set of supported HTTP methods should have only had one entry. Adjusted set was " + adjustedSupportedMethods, 2, adjustedSupportedMethods.size());
        assertTrue("Adjusted set of supported HTTP methods did not contain expected method [" + sharedMethod + "]. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(sharedMethod));
        assertTrue("Adjusted set of supported HTTP methods should have contained [" + HttpMethod.OPTIONS + "] but didn't. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(HttpMethod.OPTIONS));
    }

    @Test
    public void test_getAdjustedSupportedMethodsForEndpoint_multipleCommonMethods() {
        HttpMethod[] sharedMethods = new HttpMethod[] { HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST };
        Set<HttpMethod> defaultSupportedMethods = new HashSet<HttpMethod>();
        for (HttpMethod sharedMethod : sharedMethods) {
            defaultSupportedMethods.add(sharedMethod);
        }
        defaultSupportedMethods.add(HttpMethod.TRACE);
        Set<HttpMethod> configuredSupportedMethods = new HashSet<HttpMethod>();
        for (HttpMethod sharedMethod : sharedMethods) {
            configuredSupportedMethods.add(sharedMethod);
        }
        configuredSupportedMethods.add(HttpMethod.DELETE);

        Set<HttpMethod> adjustedSupportedMethods = handler.getAdjustedSupportedMethodsForEndpoint(defaultSupportedMethods, configuredSupportedMethods);
        assertNotNull("Adjusted set of supported HTTP methods should not have been null.", adjustedSupportedMethods);
        assertEquals("Adjusted set of supported HTTP methods did not have expected number of entries. Adjusted set was " + adjustedSupportedMethods, sharedMethods.length + 1, adjustedSupportedMethods.size());
        for (HttpMethod sharedMethod : sharedMethods) {
            assertTrue("Adjusted set of supported HTTP methods did not contain expected method [" + sharedMethod + "]. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(sharedMethod));
        }
        assertTrue("Adjusted set of supported HTTP methods should have contained [" + HttpMethod.OPTIONS + "] but didn't. Set was " + adjustedSupportedMethods, adjustedSupportedMethods.contains(HttpMethod.OPTIONS));
    }

    @Test
    public void test_getConfiguredOAuthEndpointSettings_missingOAuthRequestInfo() {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
            }
        });
        handler = new TestOAuthSupportedHttpMethodHandler(request, response);
        OAuthEndpointSettings endpointSettings = handler.getConfiguredOAuthEndpointSettings();
        assertNull("Should not have found endpoint settings, but did: " + endpointSettings, endpointSettings);
    }

    @Test
    public void test_getConfiguredOAuthEndpointSettings() {
        mockery.checking(new Expectations() {
            {
                one(oauth20Provider).getOAuthEndpointSettings();
            }
        });
        OAuthEndpointSettings endpointSettings = handler.getConfiguredOAuthEndpointSettings();
        assertNotNull("Should have found endpoint settings, but did not.", endpointSettings);
    }

    private void setOAuthProviderExpectations(final OAuthEndpointSettings endpointSettings) {
        mockery.checking(new Expectations() {
            {
                one(oauth20Provider).getOAuthEndpointSettings();
                will(returnValue(endpointSettings));
            }
        });
    }

}
