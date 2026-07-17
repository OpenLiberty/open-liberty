/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.authentication.filter.internal.AuthenticationFilterImpl;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import test.common.SharedOutputManager;

/**
 * Test class for OidcClientImpl.constructResourceMetadataUrl().
 *
 * The algorithm works from the bottom of the URL upward:
 * 1. Get the auth filter for the original request.
 * 2. Check whether the filter accepts the base URL (no path).
 * (a) If yes, then no path suffix is needed.
 * (b) If no, then add one path segment at a time until the filter accepts;
 * that prefix becomes the resource-identifier path.
 */
public class OidcClientImplConstructResourceMetadataUrlTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private OidcClientImpl oidcClient;
    private SRTServletRequest request;
    private OidcClientConfig oidcClientConfig;
    private AuthenticationFilterImpl authFilter;

    @SuppressWarnings("unchecked")
    private ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        oidcClient = new OidcClientImpl();
        request = mock.mock(SRTServletRequest.class, "request");
        oidcClientConfig = mock.mock(OidcClientConfig.class, "oidcClientConfig");
        authFilter = mock.mock(AuthenticationFilterImpl.class, "authFilter");
        authFilterServiceRef = mock.mock(ConcurrentServiceReferenceMap.class, "authFilterServiceRef");

        // Inject the authFilterServiceRef into oidcClient via reflection
        try {
            java.lang.reflect.Field field = OidcClientImpl.class.getDeclaredField("authFilterServiceRef");
            field.setAccessible(true);
            field.set(oidcClient, authFilterServiceRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set authFilterServiceRef", e);
        }
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Helper method for calling constructResourceMetadataUrl via reflection
     *
     * @param req
     * @param config
     * @return
     * @throws Exception
     */
    private String invokeConstructResourceMetadataUrl(HttpServletRequest req, OidcClientConfig config) throws Exception {
        java.lang.reflect.Method method = OidcClientImpl.class.getDeclaredMethod(
                "constructResourceMetadataUrl",
                HttpServletRequest.class,
                OidcClientConfig.class);
        method.setAccessible(true);
        return (String) method.invoke(oidcClient, req, config);
    }

    /**
     * Helper JMock matcher that accepts any HttpServletRequest whose getRequestURL() starts with the given URL prefix
     *
     * @param expectedUrl
     * @return
     */
    private static BaseMatcher<HttpServletRequest> requestWithUrl(final String expectedUrl) {
        return new BaseMatcher<HttpServletRequest>() {
            @Override
            public boolean matches(Object item) {
                if (!(item instanceof HttpServletRequest)) {
                    return false;
                }
                StringBuffer url = ((HttpServletRequest) item).getRequestURL();
                return url != null && expectedUrl.equals(url.toString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("HttpServletRequest with requestURL=" + expectedUrl);
            }
        };
    }

    /**
     * Test constructResourceMetadataUrl with null request.
     * Should return null.
     */
    @Test
    public void testConstructResourceMetadataUrl_NullRequest() {
        final String methodName = "testConstructResourceMetadataUrl_NullRequest";
        try {
            String result = invokeConstructResourceMetadataUrl(null, oidcClientConfig);
            assertNull("Resource metadata URL should be null for null request", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test constructResourceMetadataUrl with null OidcClientConfig.
     * Fall back to full requestURI as path.
     */
    @Test
    public void testConstructResourceMetadataUrl_NullConfig() {
        final String methodName = "testConstructResourceMetadataUrl_NullConfig";
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    one(request).getRequestURI();
                    will(returnValue("/api/resource"));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, null);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should use full requestURI when config is null",
                    "https://resource.example.com/.well-known/oauth-protected-resource/api/resource", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Configuration has no authFilterRef (null authFilterId).
     * Fall back to full requestURI.
     */
    @Test
    public void testConstructResourceMetadataUrl_NoAuthFilterRef() {
        final String methodName = "testConstructResourceMetadataUrl_NoAuthFilterRef";
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    one(request).getRequestURI();
                    will(returnValue("/api/resource"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(null));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should use full requestURI when authFilterId is null",
                    "https://resource.example.com/.well-known/oauth-protected-resource/api/resource", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Configuration has authFilterRef but no filter is registered for that id.
     * Fall back to full requestURI.
     */
    @Test
    public void testConstructResourceMetadataUrl_AuthFilterNotFound() {
        final String methodName = "testConstructResourceMetadataUrl_AuthFilterNotFound";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    one(request).getRequestURI();
                    will(returnValue("/api/resource"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(null));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should use full requestURI when auth filter service is not found",
                    "https://resource.example.com/.well-known/oauth-protected-resource/api/resource", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Filter accepts the base URL (no path).
     * No path suffix in metadata URL.
     *
     * Request URI: /api/resource
     * isAccepted("https://resource.example.com"): true
     * Expected: https://resource.example.com/.well-known/oauth-protected-resource
     */
    @Test
    public void testConstructResourceMetadataUrl_FilterAcceptsBaseUrl_NoPathNeeded() {
        final String methodName = "testConstructResourceMetadataUrl_FilterAcceptsBaseUrl_NoPathNeeded";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    // getRequestURI() is called to split segments; also called to build basePath fallback
                    allowing(request).getRequestURI();
                    will(returnValue("/api/resource"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    // Filter accepts the base URL (no path) immediately
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com")));
                    will(returnValue(true));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should have no path suffix when filter accepts base URL",
                    "https://resource.example.com/.well-known/oauth-protected-resource", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Filter does not accept base URL but accepts after first path segment.
     *
     * Request URI: /api/resource
     * isAccepted("https://resource.example.com"): false
     * isAccepted("https://resource.example.com/api"): true
     * Expected: https://resource.example.com/.well-known/oauth-protected-resource/api
     */
    @Test
    public void testConstructResourceMetadataUrl_FilterAcceptsAfterOneSegment() {
        final String methodName = "testConstructResourceMetadataUrl_FilterAcceptsAfterOneSegment";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    allowing(request).getRequestURI();
                    will(returnValue("/api/resource"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    // Base URL not accepted
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com")));
                    will(returnValue(false));
                    // First segment accepted
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com/api")));
                    will(returnValue(true));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should use /api as path when filter accepts after first segment",
                    "https://resource.example.com/.well-known/oauth-protected-resource/api", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Filter does not accept base URL or first segment, but accepts after two segments.
     *
     * Request URI: /api/v2/resource
     * isAccepted("https://resource.example.com"): false
     * isAccepted("https://resource.example.com/api"): false
     * isAccepted("https://resource.example.com/api/v2"): true
     * Expected: https://resource.example.com/.well-known/oauth-protected-resource/api/v2
     */
    @Test
    public void testConstructResourceMetadataUrl_FilterAcceptsAfterTwoSegments() {
        final String methodName = "testConstructResourceMetadataUrl_FilterAcceptsAfterTwoSegments";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    allowing(request).getRequestURI();
                    will(returnValue("/api/v2/resource"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    // Base URL not accepted
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com")));
                    will(returnValue(false));
                    // /api not accepted
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com/api")));
                    will(returnValue(false));
                    // /api/v2 accepted
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com/api/v2")));
                    will(returnValue(true));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should use /api/v2 as path when filter accepts after two segments",
                    "https://resource.example.com/.well-known/oauth-protected-resource/api/v2", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Filter never accepts any candidate URL → fall back to the full requestURI.
     *
     * Request URI: /api/resource
     * All isAccepted calls: false
     * Expected: https://resource.example.com/.well-known/oauth-protected-resource/api/resource
     */
    @Test
    public void testConstructResourceMetadataUrl_FilterNeverAccepts_FallbackToRequestUri() {
        final String methodName = "testConstructResourceMetadataUrl_FilterNeverAccepts_FallbackToRequestUri";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    allowing(request).getRequestURI();
                    will(returnValue("/api/resource"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    // None of the candidates accepted
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com")));
                    will(returnValue(false));
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com/api")));
                    will(returnValue(false));
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com/api/resource")));
                    will(returnValue(false));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should fall back to full requestURI when filter never accepts",
                    "https://resource.example.com/.well-known/oauth-protected-resource/api/resource", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Non-standard HTTPS port → port should be included in the URL.
     *
     * Request URI: /protected/resource
     * isAccepted("https://resource.example.com:8443"): true
     * Expected: https://resource.example.com:8443/.well-known/oauth-protected-resource
     */
    @Test
    public void testConstructResourceMetadataUrl_NonStandardPort_IncludedInUrl() {
        final String methodName = "testConstructResourceMetadataUrl_NonStandardPort_IncludedInUrl";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(8443));
                    allowing(request).getRequestURI();
                    will(returnValue("/protected/resource"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    // Filter accepts the base URL (with non-standard port, no path)
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com:8443")));
                    will(returnValue(true));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should include non-standard port in URL",
                    "https://resource.example.com:8443/.well-known/oauth-protected-resource", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Non-standard HTTP port.
     * Port should be included in the URL.
     *
     * Request URI: /api
     * isAccepted("http://resource.example.com:8080"): false
     * isAccepted("http://resource.example.com:8080/api"): true
     * Expected: http://resource.example.com:8080/.well-known/oauth-protected-resource/api
     */
    @Test
    public void testConstructResourceMetadataUrl_NonStandardHttpPort_WithPath() {
        final String methodName = "testConstructResourceMetadataUrl_NonStandardHttpPort_WithPath";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("http"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(8080));
                    allowing(request).getRequestURI();
                    will(returnValue("/api"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    // Base not accepted
                    one(authFilter).isAccepted(with(requestWithUrl("http://resource.example.com:8080")));
                    will(returnValue(false));
                    // /api accepted
                    one(authFilter).isAccepted(with(requestWithUrl("http://resource.example.com:8080/api")));
                    will(returnValue(true));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should include non-standard HTTP port and path",
                    "http://resource.example.com:8080/.well-known/oauth-protected-resource/api", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Standard HTTPS port (443).
     * Port should be omitted from the URL.
     *
     * Request URI: /secure/data
     * isAccepted("https://resource.example.com"): false
     * isAccepted("https://resource.example.com/secure"): true
     * Expected: https://resource.example.com/.well-known/oauth-protected-resource/secure
     */
    @Test
    public void testConstructResourceMetadataUrl_StandardHttpsPort_Omitted() {
        final String methodName = "testConstructResourceMetadataUrl_StandardHttpsPort_Omitted";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    allowing(request).getRequestURI();
                    will(returnValue("/secure/data"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    // Base not accepted
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com")));
                    will(returnValue(false));
                    // /secure accepted
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com/secure")));
                    will(returnValue(true));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Standard port 443 should be omitted; path should be /secure",
                    "https://resource.example.com/.well-known/oauth-protected-resource/secure", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Single-segment request URI and filter accepts the base URL.
     * No path suffix.
     *
     * Request URI: /app
     * isAccepted("https://resource.example.com"): true
     * Expected: https://resource.example.com/.well-known/oauth-protected-resource
     */
    @Test
    public void testConstructResourceMetadataUrl_SingleSegment_FilterAcceptsBase() {
        final String methodName = "testConstructResourceMetadataUrl_SingleSegment_FilterAcceptsBase";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    allowing(request).getRequestURI();
                    will(returnValue("/app"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    // Filter accepts the base URL immediately
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com")));
                    will(returnValue(true));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("No path suffix when filter accepts base URL, even for single-segment URI",
                    "https://resource.example.com/.well-known/oauth-protected-resource", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Single-segment request URI and filter only accepts with that segment.
     * Path is that segment.
     *
     * Request URI: /app
     * isAccepted("https://resource.example.com"): false
     * isAccepted("https://resource.example.com/app"): false (filter never matches)
     * Expected fallback: https://resource.example.com/.well-known/oauth-protected-resource/app
     */
    @Test
    public void testConstructResourceMetadataUrl_SingleSegment_FilterNeverAccepts_FallsBack() {
        final String methodName = "testConstructResourceMetadataUrl_SingleSegment_FilterNeverAccepts_FallsBack";
        try {
            final String authFilterId = "myAuthFilter";

            mock.checking(new Expectations() {
                {
                    one(request).getScheme();
                    will(returnValue("https"));
                    one(request).getServerName();
                    will(returnValue("resource.example.com"));
                    one(request).getServerPort();
                    will(returnValue(443));
                    allowing(request).getRequestURI();
                    will(returnValue("/app"));
                    one(oidcClientConfig).getAuthFilterId();
                    will(returnValue(authFilterId));
                    one(authFilterServiceRef).getService(authFilterId);
                    will(returnValue(authFilter));
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com")));
                    will(returnValue(false));
                    one(authFilter).isAccepted(with(requestWithUrl("https://resource.example.com/app")));
                    will(returnValue(false));
                }
            });

            String result = invokeConstructResourceMetadataUrl(request, oidcClientConfig);

            assertNotNull("Resource metadata URL should not be null", result);
            assertEquals("Should fall back to full requestURI when filter never accepts",
                    "https://resource.example.com/.well-known/oauth-protected-resource/app", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}