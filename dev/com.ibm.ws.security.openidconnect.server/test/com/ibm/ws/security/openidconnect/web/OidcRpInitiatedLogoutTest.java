/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.web;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.Principal;

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

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.oauth20.client.OAuth20ClientProvider;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.openidconnect.server.internal.HashUtils;
import com.ibm.ws.security.openidconnect.server.internal.OidcServerConfigImpl;
import com.ibm.ws.webcontainer.security.internal.StringUtil;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import test.common.SharedOutputManager;

public class OidcRpInitiatedLogoutTest {

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
    private final OidcServerConfig oidcServerConfig = context.mock(OidcServerConfig.class);

    private final OidcEndpointServices endpointServices = context.mock(OidcEndpointServices.class);
    private final OAuth20Provider oauth20Provider = context.mock(OAuth20Provider.class);
    private final OAuth20ClientProvider oauth20clientprovider = context.mock(OAuth20ClientProvider.class);
    private final OAuth20EnhancedTokenCache oauth20tokencache = context.mock(OAuth20EnhancedTokenCache.class);
    private final OAuth20TokenImpl idtokenimpl = context.mock(OAuth20TokenImpl.class);
    private final OAuth20Token refreshtoken = context.mock(OAuth20Token.class);
    private final Principal principal = context.mock(Principal.class);
    private final OidcOAuth20ClientProvider oidcoauth20clientprovider = context.mock(OidcOAuth20ClientProvider.class);

    OidcRpInitiatedLogout logout;

    @Before
    public void setUp() {
        System.setProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY, "false");

        logout = new OidcRpInitiatedLogout(endpointServices, oauth20Provider, oidcServerConfig, request, response);
    }

    @After
    public void tearDown() {
        System.clearProperty(ProductInfo.BETA_EDITION_JVM_PROPERTY);
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

        logout = new OidcRpInitiatedLogout(endpointServices, oauth20Provider, oidcServerConfig, req, response);

        try {
            context.checking(new Expectations() {
                {
                    allowing(oauth20Provider).getTokenCache();
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
                    one(oauth20Provider).isTrackOAuthClients();
                    will(returnValue(false));

                    one(response).sendRedirect(with(redirectUri));
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        try {
            logout.processEndSession();
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

        logout = new OidcRpInitiatedLogout(endpointServices, oauth20Provider, oidcServerConfig, req, response);

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
                    one(oauth20Provider).isTrackOAuthClients();
                    will(returnValue(false));

                    one(response).sendRedirect(with("/end_session_error.html"));
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        try {
            logout.processEndSession();
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

        logout = new OidcRpInitiatedLogout(endpointServices, oauth20Provider, oidcServerConfig, req, response);

        try {
            context.checking(new Expectations() {
                {
                    allowing(oauth20Provider).getTokenCache();
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
                    one(oauth20Provider).isTrackOAuthClients();
                    will(returnValue(false));
                    allowing(response).sendRedirect(with("/end_session_logout.html"));
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        try {
            logout.processEndSession();
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

        logout = new OidcRpInitiatedLogout(new OidcEndpointServices(), oauth20Provider, oidcServerConfig, req, response);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).getTokenCache();
                    will(returnValue(oauth20tokencache));
                    one(oauth20Provider).getClientProvider();
                    will(returnValue(oauth20clientprovider));
                    one(oauth20tokencache).get(with(HashUtils.digest(idTokenHint)));
                    will(returnValue(null));
                    one(oidcServerConfig).getIdTokenSigningAlgValuesSupported();
                    will(returnValue("HS256"));
                    allowing(principal).getName();
                    will(returnValue(IDTokenUtil.KEY_STRING));
                    one(oauth20Provider).isTrackOAuthClients();
                    will(returnValue(false));

                    allowing(response).sendRedirect(with("/end_session_error.html"));
                }
            });
        } catch (IOException e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        try {
            logout.processEndSession();
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

        logout = new OidcRpInitiatedLogout(endpointServices, oauth20Provider, oidcServerConfig, req, response);

        try {
            context.checking(new Expectations() {
                {
                    one(oauth20Provider).isTrackOAuthClients();
                    will(returnValue(false));
                    one(response).sendRedirect(with("/end_session_logout.html"));
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace(System.out);
            fail("An exception is caught");

        }

        try {
            logout.processEndSession();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("An exception is caught");
        }
    }

}