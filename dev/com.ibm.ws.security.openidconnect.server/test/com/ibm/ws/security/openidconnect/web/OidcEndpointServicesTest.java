/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.oauth20.client.OAuth20ClientProvider;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.openidconnect.server.internal.HashUtils;
import com.ibm.ws.security.openidconnect.server.internal.OidcServerConfigImpl;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.ws.webcontainer.security.internal.StringUtil;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.Root;

import test.common.SharedOutputManager;

public class OidcEndpointServicesTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;
    private final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletResponse response = context.mock(HttpServletResponse.class);
    private final HttpServletRequest request = context.mock(HttpServletRequest.class);
    private final ServletContext servletContext = context.mock(ServletContext.class);
    private final OAuth20EnhancedTokenCache cache = context.mock(OAuth20EnhancedTokenCache.class, "cache");
    private final OAuth20Token accessToken = context.mock(OAuth20Token.class, "accessToken");
    private final PrintWriter writer = context.mock(PrintWriter.class, "writer");
    private final ComponentContext cc = context.mock(ComponentContext.class);
    private final Root root = context.mock(Root.class);
    private final PersonAccount person = context.mock(PersonAccount.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WSSecurityService> wsSecurityServiceReference = context.mock(ServiceReference.class, "wsSecurityServiceRef");
    private final WSSecurityService wsSecurityService = context.mock(WSSecurityService.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<OidcServerConfig> oidcSCServiceReference = context.mock(ServiceReference.class, "oidcSCServiceReference");
    private final OidcServerConfig oidcServerConfig = context.mock(OidcServerConfig.class);
    private final HashMap<String, OidcServerConfig> mockOidcMap = context.mock(HashMap.class, "mockOidcMap");
    @SuppressWarnings("unchecked")
    private final ServiceReference<VMMService> vmmServiceRef = context.mock(ServiceReference.class, "vmmServiceRef");
    private final VMMService vmmService = context.mock(VMMService.class);

    private final UserRegistry registry = context.mock(UserRegistry.class);
    //private final ConfigUtils mockConfigUtils = context.mock(ConfigUtils.class, "mockConfigUtils");

    private RegistryHelper registryHelper;
    private ConfigUtils configUtils;
    private final List<Entity> entities = new ArrayList<Entity>();
    private final OAuth20Provider oauth20Provider = context.mock(OAuth20Provider.class);
    private final OAuth20ClientProvider oauth20clientprovider = context.mock(OAuth20ClientProvider.class);
    private final OAuth20EnhancedTokenCache oauth20tokencache = context.mock(OAuth20EnhancedTokenCache.class);
    private final OAuth20TokenImpl idtokenimpl = context.mock(OAuth20TokenImpl.class);
    private final OAuth20Token refreshtoken = context.mock(OAuth20Token.class);
    private final Principal principal = context.mock(Principal.class);
    private final OidcOAuth20ClientProvider oidcoauth20clientprovider = context.mock(OidcOAuth20ClientProvider.class);
//    private final BaseClient baseclient = context.mock(BaseClient.class);

    private static final String KEY_OIDC_SERVER_CONFIG_SERVICE = "oidcServerConfig";
    private static final String KEY_VMM_SERVICE = "vmmService";
    private static final String TEST_URI = "test URI";
    private static final StringBuffer TEST_URL = new StringBuffer("http://test_URL");
    private static final String TEST_QUERY = "name=stupid&color=garbage";
    private final String ACCESS_TOKEN_STRING = "RVNnQ0BKKjVxOnlKz7Be";
    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String TEST_PROVIDER = "testprovider";
    private final String ISSUER = "http://abc.com/tokenissuer";
    private final String TEST_OAUTH_PROVIDER_NAME = "testOAuthProviderName";
    private final String scopes[] = { "openid", "scope1", "scope2" };
    private final Properties scopeToClaims = new Properties();
    private final Properties claimsToVMMProps = new Properties();
    private final long oidcProviderServiceId = 123;
    private final Vector<String> paramNames = new Vector<String>();
    protected int length = 40;

    @Before
    public void setUp() {
        paramNames.add(OAuth20Constants.ACCESS_TOKEN);
    }

    @After
    public void tearDown() {
        if (configUtils != null) {
            configUtils.deactivate(cc);
        }
        context.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    /*
     * test processEndSession method
     * Input: LtpaToken Cookie exists. id_token_hint exists, post_logout_redirect_uri is set, refresh token exists.
     * expect result : cookie is removed. refresh token is removed. 302 http response with speicfied redirect uri is returned.
     */
    @Test
    public void testProcessEndSession_LtpaToken_IdTokenHint_RedirectUri_RefreshToken() {

        final byte[] cookieBytes = StringUtil.getBytes("123");
        final String cookieValue = new String(Base64.encodeBase64(cookieBytes));
        final String cookieName = "LTPAToken2";
        final Cookie cookie = new Cookie(cookieName, cookieValue);
        final String redirectUri = "http://localhost:80/index.html";
        final String idTokenHint = "id_token_hint";
        final String refreshTokenKey = "refreshtokenkey";
        final String refreshTokenString = "refreshtokenString";
        final String username = "user1";
        final String clientId = "client01";

        final OidcBaseClient oidcbaseclient = new OidcBaseClient(clientId, "secret", null, "clientName", "componentId", true);
        oidcbaseclient.setPostLogoutRedirectUris(OidcOAuth20Util.initJsonArray(new String[] { redirectUri, "http://redirect" }));
        MockServletRequest req = new MockServletRequest();
        Cookie[] cookies = new Cookie[] { cookie };
        req.setCookies(cookies);
        req.setParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI, redirectUri);
        req.setParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT, idTokenHint);
        req.setUserPrincipal(principal);
        AttributeList ops = new AttributeList();
        String[] values = { "HS256" };
        ops.setAttribute(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, OAuth20Constants.ATTRTYPE_REQUEST, values);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(idtokenimpl));
                    one(oauth20tokencache).get(with(refreshTokenKey));
                    will(returnValue(refreshtoken));
                    one(idtokenimpl).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    one(idtokenimpl).getUsername();
                    will(returnValue(username));
                    one(idtokenimpl).getClientId();
                    will(returnValue(clientId));
                    one(idtokenimpl).getRefreshTokenKey();
                    will(returnValue(refreshTokenKey));
                    one(refreshtoken).getTokenString();
                    will(returnValue(refreshTokenString));
                    one(principal).getName();
                    will(returnValue(username));

                    one(oauth20tokencache).remove(with(refreshTokenString));
                    one(oauth20Provider).getClientProvider();
                    will(returnValue(oidcoauth20clientprovider));
                    one(oidcoauth20clientprovider).get(with(clientId));
                    will(returnValue(oidcbaseclient));
                    one(oauth20Provider).isTrackRelyingParties();
                    will(returnValue(false));

                    one(response).sendRedirect(with(redirectUri));
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        OidcEndpointServices oes = new OidcEndpointServices();
        try {
            oes.processEndSession(oauth20Provider, oidcServerConfig, req, response);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("An exception is caught");
        }
    }

    /*
     * test processEndSession method
     * Input: LtpaToken Cookie exists. id_token_hint exists, post_logout_redirect_uri is set, refresh token exists. username mismatch
     * expect result : cookie is removed. refresh token is removed. 302 http response with speicfied redirect uri is returned.
     */
    @Test
    public void testProcessEndSession_LtpaToken_IdTokenHint_RedirectUri_RefreshToken_usernameMismatch() {

        final byte[] cookieBytes = StringUtil.getBytes("123");
        final String cookieValue = new String(Base64.encodeBase64(cookieBytes));
        final String cookieName = "LTPAToken2";
        final Cookie cookie = new Cookie(cookieName, cookieValue);
        final String redirectUri = "http://localhost:80/index.html";
        final String idTokenHint = "id_token_hint";
        final String refreshTokenKey = "refreshtokenkey";
        final String refreshTokenString = "refreshtokenString";
        final String username1 = "user1";
        final String username2 = "user2";
        final String clientId = "client01";

        final OidcBaseClient oidcbaseclient = new OidcBaseClient(clientId, "secret", null, "clientName", "componentId", true);
        oidcbaseclient.setPostLogoutRedirectUris(OidcOAuth20Util.initJsonArray(new String[] { redirectUri, "http://redirect" }));
        MockServletRequest req = new MockServletRequest();
        Cookie[] cookies = new Cookie[] { cookie };
        req.setCookies(cookies);
        req.setParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI, redirectUri);
        req.setParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT, idTokenHint);
        req.setUserPrincipal(principal);
        AttributeList ops = new AttributeList();
        String[] values = { "HS256" };
        ops.setAttribute(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, OAuth20Constants.ATTRTYPE_REQUEST, values);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(idtokenimpl));
                    one(idtokenimpl).getUsername();
                    will(returnValue(username1));
                    one(idtokenimpl).getClientId();
                    will(returnValue(clientId));
                    one(principal).getName();
                    will(returnValue(username2));
                    one(oauth20Provider).isTrackRelyingParties();
                    will(returnValue(false));

                    one(response).sendRedirect(with("/end_session_error.html"));
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        OidcEndpointServices oes = new OidcEndpointServices();
        try {
            oes.processEndSession(oauth20Provider, oidcServerConfig, req, response);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("An exception is caught");
        }
    }

    /*
     * test processEndSession method
     * Input: LtpaToken Cookie exists. id_token_hint exists, post_logout_redirect_uri is set, refresh token exists, redirectUri mismatch
     * expect result : cookie is removed. refresh token is removed. 302 http response with default logout page
     */
    @Test
    public void testProcessEndSession_LtpaToken_IdTokenHint_RedirectUri_RefreshToken_UriMismatch() {

        final byte[] cookieBytes = StringUtil.getBytes("123");
        final String cookieValue = new String(Base64.encodeBase64(cookieBytes));
        final String cookieName = "LTPAToken2";
        final Cookie cookie = new Cookie(cookieName, cookieValue);
        final String redirectUri = "http://localhost:80/index.html";
        final String idTokenHint = "id_token_hint";
        final String refreshTokenKey = "refreshtokenkey";
        final String refreshTokenString = "refreshtokenString";
        final String username = "user1";
        final String clientId = "client01";

        final OidcBaseClient oidcbaseclient = new OidcBaseClient(clientId, "secret", null, "clientName", "componentId", true);
        oidcbaseclient.setPostLogoutRedirectUris(OidcOAuth20Util.initJsonArray(new String[] { "https://localhost:80/index.html", "http://redirect" }));
        MockServletRequest req = new MockServletRequest();
        Cookie[] cookies = new Cookie[] { cookie };
        req.setCookies(cookies);
        req.setParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI, redirectUri);
        req.setParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT, idTokenHint);
        req.setUserPrincipal(principal);
        AttributeList ops = new AttributeList();
        String[] values = { "HS256" };
        ops.setAttribute(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, OAuth20Constants.ATTRTYPE_REQUEST, values);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(idtokenimpl));
                    one(oauth20tokencache).get(with(refreshTokenKey));
                    will(returnValue(refreshtoken));
                    one(idtokenimpl).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    one(idtokenimpl).getUsername();
                    will(returnValue(username));
                    one(idtokenimpl).getClientId();
                    will(returnValue(clientId));
                    one(idtokenimpl).getRefreshTokenKey();
                    will(returnValue(refreshTokenKey));
                    one(refreshtoken).getTokenString();
                    will(returnValue(refreshTokenString));
                    one(principal).getName();
                    will(returnValue(username));

                    one(oauth20tokencache).remove(with(refreshTokenString));
                    one(oauth20Provider).getClientProvider();
                    will(returnValue(oidcoauth20clientprovider));
                    one(oidcoauth20clientprovider).get(with(clientId));
                    will(returnValue(oidcbaseclient));
                    one(oauth20Provider).isTrackRelyingParties();
                    will(returnValue(false));
                    allowing(response).sendRedirect(with("/end_session_logout.html"));
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        OidcEndpointServices oes = new OidcEndpointServices();
        try {
            oes.processEndSession(oauth20Provider, oidcServerConfig, req, response);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("An exception is caught");
        }
    }

    /*
     * test processEndSession method
     * Input: LtpaToken Cookie exists. id_token_hint exists, post_logout_redirect_uri is not set, refresh token doesn't exist.
     * expect result : cookie is removed. refresh token is removed. 302 http response which redirects to error page.
     */
    @Test
    public void testProcessEndSession_LtpaToken_IdTokenHint_NoRedirectUri_NoRefreshToken() {

        final byte[] cookieBytes = StringUtil.getBytes("123");
        final String cookieValue = new String(Base64.encodeBase64(cookieBytes));
        final String cookieName = "LTPAToken2";
        final Cookie cookie = new Cookie(cookieName, cookieValue);
        final String idTokenHint = IDTokenUtil.createIdTokenString();
        MockServletRequest req = new MockServletRequest();
        Cookie[] cookies = new Cookie[] { cookie };
        req.setCookies(cookies);
        req.setParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT, idTokenHint);
        req.setUserPrincipal(principal);
        AttributeList ops = new AttributeList();
        String[] values = { "HS256" };
        ops.setAttribute(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, OAuth20Constants.ATTRTYPE_REQUEST, values);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20Provider).getClientProvider();
                    will(returnValue(oauth20clientprovider));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(null));
                    allowing(principal).getName();
                    will(returnValue(IDTokenUtil.KEY_STRING));
                    one(oauth20Provider).isTrackRelyingParties();
                    will(returnValue(false));

                    allowing(response).sendRedirect(with("/end_session_error.html"));
                }
            });
        } catch (IOException e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        OidcEndpointServices oes = new OidcEndpointServices();
        try {
            oes.processEndSession(oauth20Provider, oidcServerConfig, req, response);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("An exception is caught");
        }
    }

    /*
     * test processEndSession method
     * Input: LtpaToken Cookie exists. id_token_hint doesn't exist, post_logout_redirect_uri is set.
     * expect result : cookie is removed. 302 http response with specified redirect uri is returned.
     */
    @Test
    public void testProcessEndSession_LtpaToken_NoIdTokenHint_RedirectUri() {

        final byte[] cookieBytes = StringUtil.getBytes("123");
        final String cookieValue = new String(Base64.encodeBase64(cookieBytes));
        final String cookieName = "LTPAToken2";
        final Cookie cookie = new Cookie(cookieName, cookieValue);
        final Cookie matchCookie = new Cookie(cookieName, "");
        matchCookie.setMaxAge(0);

        MockServletRequest req = new MockServletRequest();
        Cookie[] cookies = new Cookie[] { cookie };
        req.setCookies(cookies);
        final String redirectUri = "http://localhost:80/index.html";
        req.setParameter(OIDCConstants.OIDC_LOGOUT_REDIRECT_URI, redirectUri);

        AttributeList ops = new AttributeList();
        String[] values = { "HS256" };
        ops.setAttribute(OidcServerConfigImpl.CFG_KEY_SIGNATURE_ALGORITHM, OAuth20Constants.ATTRTYPE_REQUEST, values);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).isTrackRelyingParties();
                    will(returnValue(false));
                    one(response).sendRedirect(with("/end_session_logout.html"));
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        OidcEndpointServices oes = new OidcEndpointServices();
        try {
            oes.processEndSession(oauth20Provider, oidcServerConfig, req, response);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("An exception is caught");
        }
    }

    /**
     * Test good access token passed via authorization header
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServlet.userinfo()
     */
    @Test
    public void testUserinfoGoodAuthzHeader() {
        final String methodName = "testUserinfoGoodAuthzHeader";
        final long now = System.currentTimeMillis();

        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(null));
                    allowing(request).getParameterNames();
                    will(returnValue(null));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue("Bearer " + ACCESS_TOKEN_STRING));
                    allowing(oauth20Provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(oauth20Provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(oauth20Provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1000));
                    allowing(accessToken).getUsername();
                    will(returnValue("bob"));
                    allowing(accessToken).getScope();
                    will(returnValue(scopes));
                    allowing(oidcServerConfig).isOpenidScopeRequiredForUserInfo();
                    will(returnValue(true));
                    allowing(accessToken).getGrantType();
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_OK);
                    allowing(response).getWriter();
                    will(returnValue(writer));
                    allowing(response).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                    allowing(wsSecurityService).getUserRegistry(null);
                    will(returnValue(registry));
                    allowing(registry).getUniqueUserId("bob");
                    will(returnValue("user:realm/bob"));
                    allowing(oidcServerConfig).getIssuerIdentifier();
                    will(returnValue(ISSUER));
                    allowing(oidcServerConfig).getProviderId();
                    will(returnValue(TEST_PROVIDER));
                    allowing(oidcServerConfig).getScopeToClaimMap();
                    will(returnValue(scopeToClaims));
                    allowing(oidcServerConfig).getClaimToUserRegistryMap();
                    will(returnValue(claimsToVMMProps));
                    allowing(vmmService).get(with(any(Root.class)));
                    will(returnValue(root));
                    allowing(root).getEntities();
                    will(returnValue(entities));
                    allowing(person).get("cn");
                    will(returnValue("bob"));
                    allowing(person).get("name");
                    will(returnValue("bob schloblodnik"));
                    allowing(request).getRequestURI();
                }
            });
            registryHelper = new RegistryHelper();
            registryHelper.setWsSecurityService(wsSecurityServiceReference);
            registryHelper.activate(cc);
            setupConfigUtils();
            scopeToClaims.put("scope1", new String[] { "name" });
            claimsToVMMProps.put("name", new String[] { "cn" });
            entities.add(person);
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test good access token passed via access_token parameter
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServlet.userinfo()
     */
    @Test
    public void testUserinfoGoodParmToken() {
        final String methodName = "testUserinfoGoodParmToken";
        final long now = System.currentTimeMillis();

        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(request).getParameterNames();
                    will(returnValue(paramNames.elements()));
                    allowing(oauth20Provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(oauth20Provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(oauth20Provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(oauth20Provider).getID();
                    will(returnValue(TEST_PROVIDER));
                    allowing(oidcServerConfig).getIssuerIdentifier();
                    will(returnValue(ISSUER));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1000));
                    allowing(accessToken).getUsername();
                    will(returnValue("bob"));
                    allowing(accessToken).getScope();
                    will(returnValue(scopes));
                    allowing(oidcServerConfig).isOpenidScopeRequiredForUserInfo();
                    will(returnValue(true));
                    allowing(accessToken).getGrantType();
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_OK);
                    allowing(response).getWriter();
                    will(returnValue(writer));
                    allowing(response).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                    allowing(wsSecurityService).getUserRegistry(null);
                    will(returnValue(registry));
                    allowing(registry).getUniqueUserId("bob");
                    will(returnValue("user:realm/bob"));
                    allowing(oidcServerConfig).getProviderId();
                    will(returnValue(TEST_PROVIDER));
                    allowing(oidcServerConfig).getScopeToClaimMap();
                    will(returnValue(scopeToClaims));
                    allowing(oidcServerConfig).getClaimToUserRegistryMap();
                    will(returnValue(claimsToVMMProps));
                    allowing(vmmService).get(with(any(Root.class)));
                    will(returnValue(root));
                    allowing(root).getEntities();
                    will(returnValue(entities));
                    allowing(person).get("cn");
                    will(returnValue("bob"));
                    allowing(request).getRequestURI();
                    will(returnValue(TEST_URI));
                }
            });
            setupConfigUtils();
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test good access token passed via access_token parameter that is missing the 'openid' scope.
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServlet.userinfo()
     */
    @Test
    public void testUserinfoGoodParmToken_missingOpenidScope_openidScopeRequired() {
        final String methodName = "testUserinfoGoodParmToken";
        final long now = System.currentTimeMillis();

        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(request).getParameterNames();
                    will(returnValue(paramNames.elements()));
                    allowing(oauth20Provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(oauth20Provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(oauth20Provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(oauth20Provider).getID();
                    will(returnValue(TEST_PROVIDER));
                    allowing(oidcServerConfig).getIssuerIdentifier();
                    will(returnValue(ISSUER));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1000));
                    allowing(accessToken).getUsername();
                    will(returnValue("bob"));
                    allowing(oidcServerConfig).getScopeToClaimMap();
                    will(returnValue(scopeToClaims));
                    allowing(accessToken).getScope();
                    will(returnValue(new String[] { "scope1", "scope2" }));
                    allowing(oidcServerConfig).isOpenidScopeRequiredForUserInfo();
                    will(returnValue(true));
                    allowing(accessToken).getGrantType();
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    allowing(request).getRequestURI();
                    will(returnValue(TEST_URI));
                }
            });
            setupConfigUtils();
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
            outputMgr.checkForMessages("CWWKS1619E" + ".*" + Pattern.quote(TEST_URI));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test good access token passed via access_token parameter that is missing the 'openid' scope, but the 'openid' scope is not required.
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServlet.userinfo()
     */
    @Test
    public void testUserinfoGoodParmToken_missingOpenidScope_openidScopeNotRequired() {
        final String methodName = "testUserinfoGoodParmToken";
        final long now = System.currentTimeMillis();

        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(request).getParameterNames();
                    will(returnValue(paramNames.elements()));
                    allowing(oauth20Provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(oauth20Provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(oauth20Provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(oauth20Provider).getID();
                    will(returnValue(TEST_PROVIDER));
                    allowing(oidcServerConfig).getIssuerIdentifier();
                    will(returnValue(ISSUER));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1000));
                    allowing(accessToken).getUsername();
                    will(returnValue("bob"));
                    allowing(accessToken).getScope();
                    will(returnValue(new String[] { "scope1", "scope2" }));
                    allowing(oidcServerConfig).isOpenidScopeRequiredForUserInfo();
                    will(returnValue(false));
                    allowing(accessToken).getGrantType();
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_OK);
                    allowing(response).getWriter();
                    will(returnValue(writer));
                    allowing(response).getHeader(with(any(String.class)));
                    will(returnValue(null));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                    allowing(wsSecurityService).getUserRegistry(null);
                    will(returnValue(registry));
                    allowing(registry).getUniqueUserId("bob");
                    will(returnValue("user:realm/bob"));
                    allowing(oidcServerConfig).getProviderId();
                    will(returnValue(TEST_PROVIDER));
                    allowing(oidcServerConfig).getScopeToClaimMap();
                    will(returnValue(scopeToClaims));
                    allowing(oidcServerConfig).getClaimToUserRegistryMap();
                    will(returnValue(claimsToVMMProps));
                    allowing(vmmService).get(with(any(Root.class)));
                    will(returnValue(root));
                    allowing(root).getEntities();
                    will(returnValue(entities));
                    allowing(person).get("cn");
                    will(returnValue("bob"));
                    allowing(request).getRequestURI();
                    will(returnValue(TEST_URI));
                }
            });
            setupConfigUtils();
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test userinfo with no access token
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServices.userinfo()
     */
    @Test
    public void testUserinfoNoToken() {
        final String methodName = "testUserinfoNoToken";
        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(null));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(request).getRequestURI();
                    will(returnValue(TEST_URI));
                    allowing(request).getParameterNames();
                    will(returnValue(null));
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    allowing(response).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            setupConfigUtils();
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test userinfo with two access tokens supplied
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServlet.userinfo()
     */
    @Test
    public void testUserinfoDuplicateToken() {
        final String methodName = "testUserinfoDuplicateToken";
        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue("Bearer " + ACCESS_TOKEN_STRING));
                    allowing(request).getRequestURI();
                    will(returnValue(TEST_URI));
                    allowing(request).getParameterNames();
                    will(returnValue(paramNames.elements()));
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    allowing(response).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            setupConfigUtils();
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test userinfo with an unknown access token string
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServlet.userinfo()
     */
    @Test
    public void testUserinfoUnknownToken() {
        final String methodName = "testUserinfoUnknownToken";
        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(request).getParameterNames();
                    will(returnValue(paramNames.elements()));
                    allowing(oauth20Provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(oauth20Provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(oauth20Provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(oauth20Provider).getID();
                    will(returnValue(TEST_PROVIDER));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(null));
                    allowing(request).getRequestURI();
                    will(returnValue(TEST_URI));
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    allowing(response).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            setupConfigUtils();
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test userinfo with token that is not an access token
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServlet.userinfo()
     */
    @Test
    public void testUserinfoNotAccessToken() {
        final String methodName = "testUserinfoNotAccessToken";
        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(request).getParameterNames();
                    will(returnValue(paramNames.elements()));
                    allowing(oauth20Provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(oauth20Provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(oauth20Provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(oauth20Provider).getID();
                    will(returnValue(TEST_PROVIDER));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT));
                    allowing(request).getRequestURI();
                    will(returnValue(TEST_URI));
                    allowing(accessToken).getGrantType();
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    allowing(response).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            setupConfigUtils();
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test userinfo with token that is expired
     *
     * Test method for com.ibm.ws.security.openidconnect.web.OidcEndpointServlet.userinfo()
     */
    @Test
    public void testUserinfoExpiredToken() {
        final String methodName = "testUserinfoExpiredToken";
        final long now = System.currentTimeMillis();
        try {
            context.checking(new Expectations() {
                {
                    allowing(request).getParameter(com.ibm.ws.security.oauth20.util.UtilConstants.ACCESS_TOKEN);
                    will(returnValue(ACCESS_TOKEN_STRING));
                    allowing(request).getHeader(AUTHORIZATION_HEADER);
                    will(returnValue(null));
                    allowing(request).getParameterNames();
                    will(returnValue(paramNames.elements()));
                    allowing(oauth20Provider).getTokenCache();
                    will(returnValue(cache));
                    allowing(oauth20Provider).getAccessTokenLength();
                    will(returnValue(length));
                    allowing(oauth20Provider).isLocalStoreUsed();
                    will(returnValue(true));
                    allowing(oauth20Provider).getID();
                    will(returnValue(TEST_PROVIDER));
                    allowing(cache).get(ACCESS_TOKEN_STRING);
                    will(returnValue(accessToken));
                    allowing(accessToken).getType();
                    will(returnValue(OAuth20Constants.TOKENTYPE_ACCESS_TOKEN));
                    allowing(accessToken).getCreatedAt();
                    will(returnValue(now - 10000));
                    allowing(accessToken).getLifetimeSeconds();
                    will(returnValue(1));
                    allowing(request).getRequestURI();
                    will(returnValue(TEST_URI));
                    allowing(accessToken).getGrantType();
                    allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                    allowing(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    allowing(response).getWriter();
                    will(returnValue(writer));
                    allowing(writer).write(with(any(String.class)));
                    allowing(writer).flush();
                }
            });
            setupConfigUtils();
            OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
            assertNotNull("There must be an OidcEndpointServlet", oidcEndpointServices);
            oidcEndpointServices.userinfo(oauth20Provider, oidcServerConfig, request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleOidcRequestNoOidcRequestAttribute() throws Exception {
        createOidcServerConfigRefExpectations();
        createOidcRequestExpectations(null);

        context.checking(new Expectations() {
            {
                allowing(request).getRequestURI();
                will(returnValue(TEST_URI));
                // Only one sendError method call is expected. Do not change to "allowing".
                one(response).sendError(with(HttpServletResponse.SC_NOT_FOUND), with(any(String.class)));
                allowing(request).getRequestURL(); //for trace
                will(returnValue(TEST_URL));
                //always passing in a query string for trace so that we don't have to
                //pass in a parameter map.  Tests that are using the parameters may
                //screw up if we hardcode the parameter map
                allowing(request).getQueryString(); //for trace
                will(returnValue(TEST_QUERY));
            }
        });

        invokeHandleOidcRequest(true);
    }

    @Test
    public void testHandleOidcRequestNoOidcServerConfig() throws Exception {
        createOidcServerConfigRefExpectations();
        final OidcRequest oidcRequest = new OidcRequest(TEST_PROVIDER, EndpointType.authorize, request);
        createOidcRequestExpectations(oidcRequest);

        context.checking(new Expectations() {
            {
                allowing(cc).locateService(KEY_OIDC_SERVER_CONFIG_SERVICE, oidcSCServiceReference);
                will(returnValue(null));

                allowing(mockOidcMap).get(with(any(String.class)));
                will(returnValue(null));

                // Only one sendError method call is expected. Do not change to "allowing".
                one(response).sendError(with(HttpServletResponse.SC_NOT_FOUND), with(any(String.class)));
            }
        });

        invokeHandleOidcRequest(true);
    }

    @Test
    public void testHandleOidcRequestNoOAuth20ProviderName() throws Exception {
        createOidcServerConfigRefExpectations();
        createComponentContextExpectations();
        final OidcRequest oidcRequest = new OidcRequest(TEST_PROVIDER, EndpointType.authorize, request);
        createOidcRequestExpectations(oidcRequest);

        context.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getProviderId();
                will(returnValue(TEST_PROVIDER));
                allowing(oidcServerConfig).getOauthProviderName();
                will(returnValue(null));
                // Only one sendError method call is expected. Do not change to "allowing".
                one(response).sendError(HttpServletResponse.SC_NOT_FOUND,
                                        "CWWKS1632E: The OAuth provider name referenced by the OpenID Connect provider " + TEST_PROVIDER + " was not found.");
                allowing(request).getRequestURL(); //for trace
                will(returnValue(TEST_URL));
                //always passing in a query string for trace so that we don't have to
                //pass in a parameter map.  Tests that are using the parameters may
                //screw up if we hardcode the parameter map
                allowing(request).getQueryString(); //for trace
                will(returnValue(TEST_QUERY));
            }
        });

        invokeHandleOidcRequest(true);
    }

    @Test
    public void testHandleOidcRequestNoOAuth20Provider() throws Exception {
        createOidcServerConfigRefExpectations();
        createComponentContextExpectations();
        final OidcRequest oidcRequest = new OidcRequest(TEST_PROVIDER, EndpointType.authorize, request);
        createOidcRequestExpectations(oidcRequest);

        context.checking(new Expectations() {
            {
                allowing(oidcServerConfig).getProviderId();
                will(returnValue(TEST_PROVIDER));
                allowing(oidcServerConfig).getOauthProviderName();
                will(returnValue(TEST_OAUTH_PROVIDER_NAME));
                // Only one sendError method call is expected. Do not change to "allowing".
                one(response).sendError(HttpServletResponse.SC_NOT_FOUND, "CWWKS1630E: OAuth20Provider object is null for the OpenID Connect provider " + TEST_PROVIDER + ".");
                allowing(request).getRequestURL(); //for trace
                will(returnValue(TEST_URL));
                //always passing in a query string for trace so that we don't have to
                //pass in a parameter map.  Tests that are using the parameters may
                //screw up if we hardcode the parameter map
                allowing(request).getQueryString(); //for trace
                will(returnValue(TEST_QUERY));
            }
        });

        invokeHandleOidcRequest(true);
    }

    @Test
    public void testHandleIdTokenHintGood() {

        final String idTokenHint = "id_token_hint";
        final String username = "user1";
        final String clientId = "client1";

        AttributeList attrs = new AttributeList();
        String[] values = { idTokenHint };
        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT, OAuth20Constants.ATTRTYPE_REQUEST, values);

        context.checking(new Expectations() {
            {
                one(oauth20Provider).getTokenCache();
                will(returnValue(oauth20tokencache));
                one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                will(returnValue(idtokenimpl));
                one(idtokenimpl).getUsername();
                will(returnValue(username));
                one(idtokenimpl).getClientId();
                will(returnValue(clientId));
                allowing(request).getRequestURL(); //for trace
                will(returnValue(TEST_URL));
                //always passing in a query string for trace so that we don't have to
                //pass in a parameter map.  Tests that are using the parameters may
                //screw up if we hardcode the parameter map
                allowing(request).getQueryString(); //for trace
                will(returnValue(TEST_QUERY));
            }
        });

        OidcEndpointServices oes = new OidcEndpointServices();
        oes.handleIdTokenHint(oauth20Provider, oidcServerConfig, attrs);
        assertEquals(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_SUCCESS, attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS));
        assertEquals(username, attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME));
        assertEquals(clientId, attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID));
    }

    @Test
    public void testHandleIdTokenHintNullHash() {

        final String idTokenHint = "";
        final String username = "user1";
        final String clientId = "client1";

        AttributeList attrs = new AttributeList();
        String[] values = { idTokenHint };
        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT, OAuth20Constants.ATTRTYPE_REQUEST, values);

        context.checking(new Expectations() {
            {
                one(oauth20Provider).getTokenCache();
                will(returnValue(oauth20tokencache));
            }
        });

        OidcEndpointServices oes = new OidcEndpointServices();
        oes.handleIdTokenHint(oauth20Provider, oidcServerConfig, attrs);
        assertEquals(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN, attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS));
    }

    @Test
    public void testHandleIdTokenHintCacheMissValidTokenButExpiredGood() {
        final String methodName = "testHandleIdTokenHintCacheMissValidTokenButExpiredGood";
        //final String idTokenHint = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3lhbW1lcmxwOjgwMjAvb2lkYy9lbmRwb2ludC9vaWRjT3BDb25maWdTYW1wbGUiLCJub25jZSI6ImRlZmF1bHRfbm9uY2UiLCJpYXQiOjE0MDc4NzUyNjksInN1YiI6InVzZXIxIiwiZXhwIjoxNDA3ODgyNDY5LCJhdWQiOiJjbGllbnQwMSIsInJlYWxtTmFtZSI6IkJhc2ljUmVhbG0iLCJ1bmlxdWVTZWN1cml0eU5hbWUiOiJ1c2VyMSIsImF0X2hhc2giOiJSNHVPdl9JbTc5dDgteWRFZDl1TGVBIn0.E20YvyWVhZ2X6Cx46PHjlHZCQZaQqks_6sxfF9_cmDk"; //this is valid but expired.
        final String idTokenHint = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3lhbW1lcmxwOjgwMjAvb2lkYy9lbmRwb2ludC9vaWRjT3BDb25maWdTYW1wbGUiLCJub25jZSI6ImRlZmF1bHRfbm9uY2UiLCJpYXQiOjE0MDc4NzUyNjksInN1YiI6InVzZXIxIiwiZXhwIjoxNDA3ODgyNDY5LCJhdWQiOiJjbGllbnQwMSIsInJlYWxtTmFtZSI6IkJhc2ljUmVhbG0iLCJ1bmlxdWVTZWN1cml0eU5hbWUiOiJ1c2VyMSIsImF0X2hhc2giOiJSNHVPdl9JbTc5dDgteWRFZDl1TGVBIn0.Gg6V8Qslmf4F0Xhj87cHx_nY86ILp7T8HDqC1465YgY";

        final String username = "user1";
        final String clientId = "client01";
        //final String secret = "secret";  //jose4j needs longer key
        final String secret = "secretsecretsecretsecretsecretsecret";

        AttributeList attrs = new AttributeList();
        String[] values = { idTokenHint };
        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT, OAuth20Constants.ATTRTYPE_REQUEST, values);
        outputMgr.trace("com.ibm.ws.security.openidconnect.web.*=all");

        final OidcBaseClient oidcbaseclient = new OidcBaseClient(clientId, secret, null, "clientName", "componentId", true);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(null));
                    one(oauth20Provider).getClientProvider();
                    will(returnValue(oidcoauth20clientprovider));
                    one(oidcoauth20clientprovider).get(with(clientId));
                    will(returnValue(oidcbaseclient));

                    one(oidcServerConfig).getSignatureAlgorithm();
                    will(returnValue("HS256"));

                }
            });

            OidcEndpointServices oes = new OidcEndpointServices();
            oes.handleIdTokenHint(oauth20Provider, oidcServerConfig, attrs);
            assertEquals(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_SUCCESS, attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS));
            assertEquals(username, attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_USERNAME));
            assertEquals(clientId, attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_CLIENTID));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleIdTokenHintCacheMissInvalidSecret() {
        final String methodName = "testHandleIdTokenHintCacheMissValidTokenButExpiredGood";
        final String idTokenHint = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3lhbW1lcmxwOjgwMjAvb2lkYy9lbmRwb2ludC9vaWRjT3BDb25maWdTYW1wbGUiLCJub25jZSI6ImRlZmF1bHRfbm9uY2UiLCJpYXQiOjE0MDc4NzUyNjksInN1YiI6InVzZXIxIiwiZXhwIjoxNDA3ODgyNDY5LCJhdWQiOiJjbGllbnQwMSIsInJlYWxtTmFtZSI6IkJhc2ljUmVhbG0iLCJ1bmlxdWVTZWN1cml0eU5hbWUiOiJ1c2VyMSIsImF0X2hhc2giOiJSNHVPdl9JbTc5dDgteWRFZDl1TGVBIn0.E20YvyWVhZ2X6Cx46PHjlHZCQZaQqks_6sxfF9_cmDk"; //this is valid but expired.
        final String username = "user1";
        final String clientId = "client01";
        final String secret = "bad_secret";

        AttributeList attrs = new AttributeList();
        String[] values = { idTokenHint };
        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT, OAuth20Constants.ATTRTYPE_REQUEST, values);

        final OidcBaseClient oidcbaseclient = new OidcBaseClient(clientId, secret, null, "clientName", "componentId", true);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(null));
                    one(oauth20Provider).getClientProvider();
                    will(returnValue(oidcoauth20clientprovider));
                    one(oidcoauth20clientprovider).get(with(clientId));
                    will(returnValue(oidcbaseclient));

                    one(oidcServerConfig).getSignatureAlgorithm();
                    will(returnValue("HS256"));

                }
            });

            OidcEndpointServices oes = new OidcEndpointServices();
            oes.handleIdTokenHint(oauth20Provider, oidcServerConfig, attrs);
            assertEquals(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN,
                         attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleIdTokenHintCacheMissInvalidHint() {
        final String methodName = "testHandleIdTokenHintCacheMissInvalidHint";
        final String idTokenHint = "eyJhbGciOiJIUzI1NJ9.eyJpc3MiOiJodHRwczovL3lhbW1lcmxwOjgwMjAvb2lkYy9lbmRwb2ludC9vaWRjT3BDb25maWdTYW1wbGUiLCJub25jZSI6ImRlZmF1bHRfbm9uY2UiLCJpYXQiOjE0MDc4NzUyNjksInN1YiI6InVzZXIxIiwiZXhwIjoxNDA3ODgyNDY5LCJhdWQiOiJjbGllbnQwMSIsInJlYWxtTmFtZSI6IkJhc2ljUmVhbG0iLCJ1bmlxdWVTZWN1cml0eU5hbWUiOiJ1c2VyMSIsImF0X2hhc2giOiJSNHVPdl9JbTc5dDgteWRFZDl1TGVBIn0.E20YvyWVhZ2X6Cx46PHjlHZCQZaQqks_6sxfF9_cmDk"; //this is not valid

        AttributeList attrs = new AttributeList();
        String[] values = { idTokenHint };
        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT, OAuth20Constants.ATTRTYPE_REQUEST, values);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(null));
                }
            });

            OidcEndpointServices oes = new OidcEndpointServices();
            oes.handleIdTokenHint(oauth20Provider, oidcServerConfig, attrs);
            assertEquals(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN,
                         attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHandleIdTokenHintCacheMissIncorrectFormatHint() {
        final String methodName = "testHandleIdTokenHintCacheMissIncorrectFormatHint";
        final String idTokenHint = "part1.part2";

        AttributeList attrs = new AttributeList();
        String[] values = { idTokenHint };
        attrs.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT, OAuth20Constants.ATTRTYPE_REQUEST, values);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(null));
                }
            });

            OidcEndpointServices oes = new OidcEndpointServices();
            oes.handleIdTokenHint(oauth20Provider, oidcServerConfig, attrs);
            assertEquals(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS_FAIL_INVALID_ID_TOKEN,
                         attrs.getAttributeValueByName(OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT_STATUS));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private void createOidcServerConfigRefExpectations() {
        context.checking(new Expectations() {
            {
                allowing(oidcSCServiceReference).getProperty("id");
                will(returnValue(TEST_PROVIDER));
                allowing(oidcSCServiceReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(oidcProviderServiceId));
                allowing(oidcSCServiceReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });
    }

    private void createComponentContextExpectations() {
        context.checking(new Expectations() {
            {
                allowing(cc).locateService(WSSecurityService.KEY_WS_SECURITY_SERVICE, wsSecurityServiceReference);
                will(returnValue(wsSecurityService));
                allowing(cc).locateService(KEY_OIDC_SERVER_CONFIG_SERVICE, oidcSCServiceReference);
                will(returnValue(oidcServerConfig));
                allowing(mockOidcMap).get(with(any(String.class)));
                will(returnValue(oidcServerConfig));
                allowing(cc).locateService(KEY_VMM_SERVICE, vmmServiceRef);
                will(returnValue(vmmService));
            }
        });
    }

    private void createOidcRequestExpectations(final OidcRequest oidcRequest) {
        context.checking(new Expectations() {
            {
                allowing(request).getRequestURL(); //for trace
                will(returnValue(TEST_URL));
                //always passing in a query string for trace so that we don't have to
                //pass in a parameter map.  Tests that are using the parameters may
                //screw up if we hardcode the parameter map
                allowing(request).getQueryString(); //for trace
                will(returnValue(TEST_QUERY));
                allowing(request).getAttribute("OidcRequest");
                will(returnValue(oidcRequest));
            }
        });
    }

    private void invokeHandleOidcRequest(boolean mock) throws Exception {

        OidcEndpointServices oidcEndpointServices = new OidcEndpointServices();
        if (mock) {
            oidcEndpointServices.configUtils = new mockConfigUtils();
        }
        oidcEndpointServices.setOidcServerConfig(oidcSCServiceReference);
        oidcEndpointServices.activate(cc);
        oidcEndpointServices.handleOidcRequest(request, response, servletContext);
    }

    /**
     * Set up the configUtils object
     */
    private void setupConfigUtils() {
        createOidcServerConfigRefExpectations();
        createComponentContextExpectations();

        configUtils = new mockConfigUtils();
        configUtils.setOidcServerConfig(oidcSCServiceReference);
        configUtils.setVmmService(vmmServiceRef);
        configUtils.activate(cc);
    }

    public void mockServices() {
        createOidcServerConfigRefExpectations();
        createComponentContextExpectations();
    }

    class mockConfigUtils extends ConfigUtils {
        @Override
        public HashMap<String, OidcServerConfig> checkDuplicateOAuthProvider(ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef) {
            return mockOidcMap;
        };
    }
}