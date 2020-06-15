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
package io.openliberty.security.openidconnect.web;

import static org.junit.Assert.assertEquals;
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
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.openidconnect.web.OidcEndpointServices;
import com.ibm.ws.security.openidconnect.web.OidcRequest;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import io.openliberty.security.openidconnect.server.config.OidcEndpointSettings;
import io.openliberty.security.openidconnect.server.config.SpecificOidcEndpointSettings;
import test.common.SharedOutputManager;

@SuppressWarnings("restriction")
public class OidcSupportedHttpMethodHandlerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.openidconnect.*=all:com.ibm.ws.security.openidconnect*=all");

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final OidcRequest oidcRequest = mockery.mock(OidcRequest.class);
    private final OidcEndpointServices oidcEndpointServices = mockery.mock(OidcEndpointServices.class);
    private final OidcServerConfig oidcServerConfig = mockery.mock(OidcServerConfig.class);
    private final OidcEndpointSettings oidcEndpointSettings = mockery.mock(OidcEndpointSettings.class);
    private final SpecificOidcEndpointSettings specificOidcEndpointSettings = mockery.mock(SpecificOidcEndpointSettings.class);

    private final String providerName = "MyOidcProvider";

    private OidcSupportedHttpMethodHandler handler;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        mockery.checking(new Expectations() {
            {
                allowing(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
                allowing(request).getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oidcRequest));
            }
        });
        handler = new OidcSupportedHttpMethodHandler(request, response);
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
    public void test_getDefaultSupportedMethodsForEndpoint_userinfo() throws IOException {
        Set<HttpMethod> supportedMethods = handler.getDefaultSupportedMethodsForEndpoint(EndpointType.userinfo);
        assertNotNull("Set of supported methods should not have been null but was.", supportedMethods);
        HttpMethod[] expectedMethods = new HttpMethod[] { HttpMethod.OPTIONS, HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST };
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + supportedMethods, expectedMethods.length,
                     supportedMethods.size());
        for (HttpMethod expectedMethod : expectedMethods) {
            assertTrue("Did not find " + expectedMethod + " in the set of supported HTTP methods.", supportedMethods.contains(expectedMethod));
        }
    }

    @Test
    public void test_getDefaultSupportedMethodsForEndpoint_endSession() throws IOException {
        Set<HttpMethod> supportedMethods = handler.getDefaultSupportedMethodsForEndpoint(EndpointType.end_session);
        assertNotNull("Set of supported methods should not have been null but was.", supportedMethods);
        HttpMethod[] expectedMethods = new HttpMethod[] { HttpMethod.OPTIONS, HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST };
        assertEquals("Did not find the expected number of supported HTTP methods. Supported methods found were " + supportedMethods, expectedMethods.length,
                     supportedMethods.size());
        for (HttpMethod expectedMethod : expectedMethods) {
            assertTrue("Did not find " + expectedMethod + " in the set of supported HTTP methods.", supportedMethods.contains(expectedMethod));
        }
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint_noConfiguredSettings() {
        handler = new OidcSupportedHttpMethodHandler(request, response) {
            @Override
            OidcEndpointSettings getConfiguredOidcEndpointSettings() {
                return null;
            }
        };
        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(EndpointType.userinfo);
        assertNull("Set of supported HTTP methods should have been null but was " + supportedMethods, supportedMethods);
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint_noSpecificEndpointSettings() {
        EndpointType endpoint = EndpointType.end_session;
        handler = new OidcSupportedHttpMethodHandler(request, response) {
            @Override
            OidcEndpointSettings getConfiguredOidcEndpointSettings() {
                return oidcEndpointSettings;
            }
        };
        mockery.checking(new Expectations() {
            {
                one(oidcEndpointSettings).getSpecificOidcEndpointSettings(endpoint);
                will(returnValue(null));
            }
        });
        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(endpoint);
        assertNull("Set of supported HTTP methods should have been null but was " + supportedMethods, supportedMethods);
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint_nullEndpointType() {
        EndpointType endpoint = null;
        handler = new OidcSupportedHttpMethodHandler(request, response) {
            @Override
            OidcEndpointSettings getConfiguredOidcEndpointSettings() {
                return oidcEndpointSettings;
            }
        };
        mockery.checking(new Expectations() {
            {
                one(oidcEndpointSettings).getSpecificOidcEndpointSettings(endpoint);
                will(returnValue(null));
            }
        });
        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(endpoint);
        assertNull("Set of supported HTTP methods should have been null but was " + supportedMethods, supportedMethods);
    }

    @Test
    public void test_getConfiguredSupportedMethodsForEndpoint() {
        EndpointType endpoint = EndpointType.jwk;
        Set<HttpMethod> expectedMethods = new HashSet<HttpMethod>();
        expectedMethods.add(HttpMethod.GET);
        expectedMethods.add(HttpMethod.HEAD);
        handler = new OidcSupportedHttpMethodHandler(request, response) {
            @Override
            OidcEndpointSettings getConfiguredOidcEndpointSettings() {
                return oidcEndpointSettings;
            }
        };
        mockery.checking(new Expectations() {
            {
                one(oidcEndpointSettings).getSpecificOidcEndpointSettings(endpoint);
                will(returnValue(specificOidcEndpointSettings));
                one(specificOidcEndpointSettings).getSupportedHttpMethods();
                will(returnValue(expectedMethods));
            }
        });
        Set<HttpMethod> supportedMethods = handler.getConfiguredSupportedMethodsForEndpoint(endpoint);
        assertEquals("Set of supported HTTP methods did not match the expected value.", expectedMethods, supportedMethods);
    }

    @Test
    public void test_getConfiguredOidcEndpointSettings_missingOidcRequestInfo() {
        mockery.checking(new Expectations() {
            {
                allowing(request).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
                allowing(request).getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(null));
            }
        });
        handler = new OidcSupportedHttpMethodHandler(request, response);

        OidcEndpointSettings settings = handler.getConfiguredOidcEndpointSettings();
        assertNull("Should not have found any settings but did.", settings);
    }

    @Test
    public void test_getConfiguredOidcEndpointSettings_missingOidcEndpointServices() {
        OidcEndpointSettings settings = handler.getConfiguredOidcEndpointSettings();
        assertNull("Should not have found any settings but did.", settings);
    }

    @Test
    public void test_getConfiguredOidcEndpointSettings_unknownProvider() throws IOException {
        handler.setOidcEndpointServices(oidcEndpointServices);
        mockery.checking(new Expectations() {
            {
                one(oidcRequest).getProviderName();
                will(returnValue(providerName));
                one(oidcEndpointServices).getOidcServerConfig(response, providerName);
                will(returnValue(null));
            }
        });
        OidcEndpointSettings settings = handler.getConfiguredOidcEndpointSettings();
        assertNull("Should not have found any settings but did.", settings);
    }

    @Test
    public void test_getConfiguredOidcEndpointSettings_noConfiguredSettings() throws IOException {
        handler.setOidcEndpointServices(oidcEndpointServices);
        mockery.checking(new Expectations() {
            {
                one(oidcRequest).getProviderName();
                will(returnValue(providerName));
                one(oidcEndpointServices).getOidcServerConfig(response, providerName);
                will(returnValue(oidcServerConfig));
            }
        });
        OidcEndpointSettings settings = handler.getConfiguredOidcEndpointSettings();
        assertNull("Should not have found any settings but did.", settings);
    }

}
