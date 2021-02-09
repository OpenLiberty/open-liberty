/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.wsspi.security.registry.RegistryHelper;

import test.common.SharedOutputManager;

/**
 *
 */
public class OAuth20EndpointServicesTest {

    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class, "req");
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class, "resp");
    private final ServletContext servletContext = mock.mock(ServletContext.class);
    private final OAuth20EnhancedTokenCache cache = mock.mock(OAuth20EnhancedTokenCache.class, "cache");
    private final OAuth20Provider provider = mock.mock(OAuth20Provider.class, "provider");
    private final OAuth20Token accessToken = mock.mock(OAuth20Token.class, "accessToken");
    private final PrintWriter writer = mock.mock(PrintWriter.class, "writer");
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<WSSecurityService> wsSecurityServiceRef = mock.mock(ServiceReference.class);
    private final WSSecurityService wsSecurityService = mock.mock(WSSecurityService.class);
    private final UserRegistry registry = mock.mock(UserRegistry.class);
    private final OidcOAuth20ClientProvider clientprovider = mock.mock(OidcOAuth20ClientProvider.class);
    private final OidcBaseClient client = mock.mock(OidcBaseClient.class);
    private final ClientAuthnData clientAuthnData = mock.mock(ClientAuthnData.class);
    private final HttpSession session = mock.mock(HttpSession.class);

    private RegistryHelper registryHelper;

    private final String ACCESS_TOKEN_STRING = "RVNnQ0BKKjVxOnlKz7Be";
    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String AUTHORIZATION_CODE_GRANT = "authorization_code";
    private final String CLIENT_SECRET = "secret";
    private final String CLIENT01 = "client01";
    private final String CLIENT02 = "client02";
    private final String scopes[] = { "scope", "scope2" };
    private final String clientSecretArray[] = { CLIENT_SECRET };
    private final String client01Array[] = { CLIENT01 };
    protected int length = 40;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(WSSecurityService.KEY_WS_SECURITY_SERVICE, wsSecurityServiceRef);
                will(returnValue(wsSecurityService));
            }
        });
        registryHelper = new RegistryHelper();
        registryHelper.setWsSecurityService(wsSecurityServiceRef);
        registryHelper.activate(cc);
    }

    @After
    public void tearDown() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    /**
     * Test expired token
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.introspect()
     */
    @Test
    public void testIntrospectExpired() {
        final String methodName = "testIntrospectExpired";
        final long now = System.currentTimeMillis();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecretArray));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(client01Array));
                    allowing(req).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(provider).getClientProvider();
                    will(returnValue(clientprovider));
                    allowing(clientprovider).get(with(any(String.class)));
                    will(returnValue(client));
                    allowing(clientAuthnData).getUserName();
                    will(returnValue("bob"));
                    allowing(client).isIntrospectTokens();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    allowing(accessToken).getClientId();
                    will(returnValue(CLIENT01));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(0));
                    allowing(accessToken).getGrantType();
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_OK);
                    allowing(resp).sendError(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(resp).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.introspect(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testURLEncodeParams() {
        OAuth20EndpointServices oas = new OAuth20EndpointServices();
        String in = "https://www.somewhere.com?item=s巧";
        String expected = "https://www.somewhere.com?item=s%E5%B7%A7";
        String actual = oas.URLEncodeParams(in);
        assertTrue("url encode not as expected: " + actual, actual.equals(expected));

        in = "https://www.somewhere.com";
        assertTrue("url encode 2 not as expected", oas.URLEncodeParams(in).equals(in));

        in = "https://www.somewhere.com?item=s%4";
        assertTrue("url containing % should not have been encoded again", oas.URLEncodeParams(in).equals(in));

    }

    /**
     * Test good token
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.introspect()
     */
    @Test
    public void testIntrospectGood() {
        final String methodName = "testIntrospectGood";
        final long now = System.currentTimeMillis();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecretArray));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(client01Array));
                    allowing(req).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(req).getServerName();
                    will(returnValue("localhost"));
                    allowing(req).getScheme();
                    will(returnValue("https"));
                    allowing(req).getLocalPort();
                    will(returnValue(443));
                    allowing(req).getRequestURI();
                    will(returnValue("/oidc"));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(provider).getClientProvider();
                    will(returnValue(clientprovider));
                    allowing(clientprovider).get(with(any(String.class)));
                    will(returnValue(client));
                    allowing(clientAuthnData).getUserName();
                    will(returnValue("bob"));
                    allowing(client).isIntrospectTokens();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    allowing(accessToken).getClientId();
                    will(returnValue(CLIENT01));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1000));
                    one(provider).getID();
                    will(returnValue("myOAuthProvider"));
                    allowing(accessToken).getUsername();
                    will(returnValue("bob"));
                    allowing(accessToken).getScope();
                    will(returnValue(scopes));
                    allowing(accessToken).getRedirectUri();
                    will(returnValue("https://somehost:port/blah/blah/blah"));
                    allowing(accessToken).getGrantType();
                    will(returnValue(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
                    allowing(accessToken).getUsedBy();
                    will(returnValue(null));
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_OK);
                    allowing(resp).sendError(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(resp).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                    allowing(wsSecurityService).getUserRegistry(null);
                    will(returnValue(registry));
                    allowing(registry).getUniqueUserId("bob");
                    will(returnValue("user:realm/bob"));
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.introspect(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test no token parameter
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.introspect()
     */
    @Test
    public void testIntrospectNoTokenParm() {
        final String methodName = "testIntrospectNoTokenParm";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(null));
                    allowing(req).getRequestURI();
                    will(returnValue("junit/dummy/uri"));
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    allowing(resp).sendError(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.introspect(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test wrong token type
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.introspect()
     */
    @Test
    public void testIntrospectWrongTokenType() {
        final String methodName = "testIntrospectWrongTokenType";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecretArray));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(client01Array));
                    allowing(req).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.SUBTYPE_REFRESH_TOKEN));
                    allowing(accessToken).getGrantType();
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_OK);
                    allowing(resp).sendError(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(resp).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.introspect(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test token with client different than request param client
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.introspect()
     */
    @Test
    public void testIntrospectWrongTokenClient() {
        final String methodName = "testIntrospectWrongTokenClient";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecretArray));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(client01Array));
                    allowing(req).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.SUBTYPE_REFRESH_TOKEN));
                    allowing(accessToken).getClientId();
                    will(returnValue(CLIENT02));
                    allowing(accessToken).getGrantType();
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_OK);
                    allowing(resp).sendError(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(resp).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.introspect(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test with no token param
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.revoke()
     */
    @Test
    public void testRevokeNoTokenParm() {
        final String methodName = "testRevokeNoTokenParm";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(null));
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    allowing(resp).sendError(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.revoke(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test with token param that is not in the cache
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.revoke()
     */
    @Test
    public void testRevokeNoTokenFound() {
        final String methodName = "testRevokeNoTokenFound";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(null));
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.revoke(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test with valid token
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.revoke()
     */
    @Test
    public void testRevokeGood() {
        final String methodName = "testRevokeGood";
        final long now = System.currentTimeMillis();
        System.out.println("*** testRevokeGood");
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getContextPath();
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecretArray));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(client01Array));
                    allowing(req).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(cache).remove(ACCESS_TOKEN_STRING);
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    allowing(accessToken).getSubType();
                    will(returnValue(OAuth20Constants.SUBTYPE_REFRESH_TOKEN));
                    allowing(accessToken).getClientId();
                    will(returnValue(CLIENT01));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1000));
                    allowing(accessToken).getUsername();
                    will(returnValue("bob"));
                    allowing(accessToken).getGrantType();
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_OK);
                    allowing(resp).sendError(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.revoke(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test with token type authorization_grant
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.revoke()
     */
    @Test
    public void testRevokeGood01() {
        final String methodName = "testRevokeGood01";
        final long now = System.currentTimeMillis();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getContextPath();
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecretArray));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(client01Array));
                    allowing(req).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(cache).remove(ACCESS_TOKEN_STRING);
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT));
                    allowing(accessToken).getSubType();
                    will(returnValue(OAuth20Constants.SUBTYPE_REFRESH_TOKEN));
                    allowing(accessToken).getClientId();
                    will(returnValue(CLIENT01));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1000));
                    allowing(accessToken).getUsername();
                    will(returnValue("bob"));
                    allowing(accessToken).getGrantType();
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_OK);
                    allowing(resp).sendError(HttpServletResponse.SC_OK);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.revoke(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test with unsupported token type
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.revoke()
     */
    @Test
    public void testRevokeWrongTokenType() {
        final String methodName = "testRevokeWrongTokenType";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecretArray));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(client01Array));
                    allowing(req).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getClientId();
                    will(returnValue(CLIENT01));
                    allowing(accessToken).getType();
                    will(returnValue("bogus_token_type"));
                    allowing(accessToken).getGrantType();
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.revoke(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * token with client different than request param client
     *
     * Test method for com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet.revoke()
     */
    @Test
    public void testRevokeWrongClient() {
        final String methodName = "testRevokeWrongClient";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(req).getRequestURI();
                    will(returnValue("/oidc"));
                    allowing(req).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_SECRET);
                    will(returnValue(clientSecretArray));
                    allowing(req).getParameterValues(OAuth20Constants.CLIENT_ID);
                    will(returnValue(client01Array));
                    allowing(req).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getGrantType();
                    will(returnValue(AUTHORIZATION_CODE_GRANT));
                    allowing(accessToken).getClientId();
                    will(returnValue(CLIENT02));
                    allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    allowing(resp).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
            assertNotNull("There must be an OAuth20EndpointServices", oauth20EndpointServices);
            oauth20EndpointServices.revoke(provider, req, resp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleOAuthRequestNoOAuth20Provider() throws Exception {
        createOAuth20RequestExpectations();
        mock.checking(new Expectations() {
            {
                // Only one sendError method call is expected. Do not change to "allowing".
                one(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        });

        OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
        oauth20EndpointServices.handleOAuthRequest(req, resp, servletContext);
    }

    private void createOAuth20RequestExpectations() {
        final OAuth20Request oauth20Request = new OAuth20Request("providerThatDoesNotExist", EndpointType.authorize, req);
        mock.checking(new Expectations() {
            {
                one(req).getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
                will(returnValue(oauth20Request));
            }
        });
    }

    @Test
    public void testIsAfterLoginTrue() {
        mock.checking(new Expectations() {
            {
                one(req).getSession(false);
                will(returnValue(session));
                one(session).getAttribute(com.ibm.ws.security.oauth20.api.Constants.ATTR_AFTERLOGIN);
                will(returnValue("value"));
                one(session).removeAttribute(com.ibm.ws.security.oauth20.api.Constants.ATTR_AFTERLOGIN);
            }
        });

        OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
        assertTrue(oauth20EndpointServices.isAfterLogin(req));
    }

    @Test
    public void testIsAfterLoginFalse() {
        mock.checking(new Expectations() {
            {
                one(req).getSession(false);
                will(returnValue(session));
                one(session).getAttribute(com.ibm.ws.security.oauth20.api.Constants.ATTR_AFTERLOGIN);
                will(returnValue(null));
                never(session).removeAttribute(com.ibm.ws.security.oauth20.api.Constants.ATTR_AFTERLOGIN);
            }
        });

        OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
        assertFalse(oauth20EndpointServices.isAfterLogin(req));
    }

    @Test
    public void testIsAfterLoginNoSession() {
        mock.checking(new Expectations() {
            {
                one(req).getSession(false);
                will(returnValue(null));
            }
        });

        OAuth20EndpointServices oauth20EndpointServices = new OAuth20EndpointServices();
        assertFalse(oauth20EndpointServices.isAfterLogin(req));
    }

}
