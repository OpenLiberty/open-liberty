/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import static org.junit.Assert.assertEquals;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.security.saml2.Saml20Attribute;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;

/**
 * Unit test {@link com.ibm.ws.security.saml.sso20.internal.Authenticator20} class.
 */
public class AuthenticatorTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final States states = mockery.states("first");
    private static final Saml20Attribute samlAttribute = mockery.mock(Saml20Attribute.class, "samlAttribute");
    private static final WebProviderAuthenticatorHelper authHelper = mockery.mock(WebProviderAuthenticatorHelper.class);
    private static final HttpServletRequest request = mockery.mock(IExtendedRequest.class);
    private static final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private static final SsoSamlService ssoService = mockery.mock(SsoSamlService.class);
    private static final SsoConfig ssoConfig = mockery.mock(SsoConfig.class);
    private static final Saml20Token sso20Token = mockery.mock(Saml20Token.class);
    private static final SsoRequest ssoRequest = mockery.mock(SsoRequest.class);
    private static final Cache cache = mockery.mock(Cache.class);
    private static final UserData useData = mockery.mock(UserData.class);
    private static final WebAppSecurityConfig webAppSecConfig = mockery.mock(WebAppSecurityConfig.class, "webappsecconfig");
    private static final AuthenticationResult authResult = mockery.mock(AuthenticationResult.class, "authResult");
    private static final Principal principal = mockery.mock(Principal.class, "principal");
    private static final Subject subject = new Subject();

    private static final String PROVIDER_ID = "providerId";
    @SuppressWarnings("unchecked")
    private static final ConcurrentServiceReferenceMap<String, SsoSamlService> ssoServiceRefMap = mockery.mock(ConcurrentServiceReferenceMap.class, "ssoServiceRefMap");

    private static final String CACHE_ID = "cacheID";
    private static final String SAML_NAME_ID = "samlnameid";
    private static final String SAML_ISSUER_NAME = "issuerName";
    private static final String USER = "user";
    private static final String USER_ID = "urn:oid:1.3.6.1.4.1.5923.1.1.1.1";
    private static final String GROUP_ID = "groupid";
    private static final String REALM_ID = "urn:oid:1.3.6.1.4.1.5923.1.1.1.1";
    private static final String ATTRIBUTE_NAME = "urn:oid:1.3.6.1.4.1.5923.1.1.1.1";
    private static final String ATTRIBUTE_NAME_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
    private static final String SAML_STR = "saml string";

    private static final List<Saml20Attribute> samlAttributes = new ArrayList<Saml20Attribute>();
    private static final List<String> valuesStrings = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        samlAttributes.add(samlAttribute);
        valuesStrings.add(USER);
        subject.getPrincipals().add(principal);
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);

        mockery.checking(new Expectations() {
            {
                //Expectations for Sso20Token mock object.
                atMost(3).of(sso20Token).getSAMLNameID();
                will(returnValue(SAML_NAME_ID));
                atMost(2).of(sso20Token).getSAMLIssuerName();
                will(returnValue(SAML_ISSUER_NAME));
                allowing(sso20Token).getSAMLAttributes();
                will(returnValue(samlAttributes));
                allowing(sso20Token).getSAMLAsString();
                will(returnValue(SAML_STR));

                //Expectations for UserData mock object.
                allowing(useData).getSamlToken();
                will(returnValue(sso20Token));

                //Expectations for Cache mock object.
                atMost(4).of(cache).get(CACHE_ID);
                will(returnValue(useData));
                atMost(3).of(cache).remove(CACHE_ID);

                //Expectations for SsoConfig mock object.
                atMost(2).of(ssoConfig).getMapToUserRegistry();
                will(returnValue(Constants.MapToUserRegistry.No));

                atMost(3).of(ssoConfig).getUserIdentifier();
                will(returnValue(USER_ID));
                atMost(2).of(ssoConfig).getRealmName();
                will(returnValue(null));
                atMost(2).of(ssoConfig).getRealmIdentifier();
                will(returnValue(REALM_ID));
                atMost(2).of(ssoConfig).getUserUniqueIdentifier();
                will(returnValue(USER_ID));
                atMost(2).of(ssoConfig).getGroupIdentifier();
                will(returnValue(GROUP_ID));
                allowing(ssoConfig).getSessionNotOnOrAfter();
                will(returnValue(7200000L)); // 2 hours
                atMost(3).of(ssoConfig).isIncludeTokenInSubject();
                will(returnValue(true));

                //Expectations for WebAppSecurityConfig mock object.
                atMost(3).of(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(true));
                atMost(3).of(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                //Expectations for HttpServletRequest mock object.
                atMost(4).of((IExtendedRequest) request).getCookieValueAsBytes(Constants.COOKIE_NAME_WAS_SAML_ACS + SamlUtil.hash(PROVIDER_ID));
                will(returnValue(CACHE_ID.getBytes()));
                one((IExtendedRequest) request).setAttribute(with(any(String.class)), with(any(String.class)));
                allowing((IExtendedRequest) request).getAttribute(with(any(String.class)));
                will(returnValue(ssoRequest));

                //
                allowing(ssoRequest).isDisableLtpaCookie();
                will(returnValue(true));
                allowing(ssoRequest).setSpCookieValue(with(any(String.class)));
                allowing(ssoRequest).createSpCookieIfDisableLtpa(request, response);
                //Expectations for WebProviderAuthenticationHelper mock object.
                atMost(3).of(authHelper).loginWithUserName(with(any(HttpServletRequest.class)),
                                                           with(any(HttpServletResponse.class)),
                                                           with(any(String.class)), with(any(Subject.class)),
                                                           with(any(Hashtable.class)), with(any(Boolean.class)));
                will(returnValue(authResult));

                //Expectations for AuthenticationResult mock object.
                atMost(3).of(authResult).getStatus();
                will(returnValue(AuthResult.SUCCESS));
                atMost(3).of(authResult).getSubject();
                will(returnValue(subject));

                //Expectations for principal mock object.
                atMost(3).of(principal).getName();
                will(returnValue(USER));

                //Expectations for SsoService mock object.
                one(ssoService).getAuthHelper();
                will(returnValue(authHelper));
                atMost(2).of(ssoService).getProviderId();
                will(returnValue(PROVIDER_ID));
                one(ssoService).getConfig();
                will(returnValue(ssoConfig));
                atMost(4).of(ssoService).getAcsCookieCache(PROVIDER_ID);
                will(returnValue(cache));

                //Expectations for SamlAttribute mock object.
                allowing(samlAttribute).getName();
                will(returnValue(ATTRIBUTE_NAME));
                allowing(samlAttribute).getNameFormat();
                will(returnValue(ATTRIBUTE_NAME_FORMAT));
                allowing(samlAttribute).getValuesAsString();
                will(returnValue(valuesStrings));

                allowing(ssoConfig).isAllowCustomCacheKey();
                will(returnValue(false));

                //Expectations for ssoServiceRefMap mock object.
                one(ssoServiceRefMap).getService(PROVIDER_ID);
                will(returnValue(ssoService));

                //Expectations for HttpServletResponse mock object.
                atMost(3).of(response).addCookie(with(any(Cookie.class)));

            }
        });
    }

    @AfterClass
    public static void tearDown() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(null);
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testDeleteMe() {
        // TODO - Delete me
    }

    //@Test
    public void testAuthenticate() throws Exception {
        Authenticator autehnticator = new Authenticator(ssoService, useData);
        TAIResult taiResult = autehnticator.authenticate(request, response);
        assertEquals("First: The status code should be " + HttpServletResponse.SC_OK + " and got " + taiResult.getStatus() + ".",
                     HttpServletResponse.SC_OK, taiResult.getStatus());

        states.become("second");
        mockery.checking(new Expectations() {
            {
                atMost(2).of(ssoConfig).getMapToUserRegistry();
                will(returnValue(Constants.MapToUserRegistry.User));
            }
        });
        taiResult = autehnticator.authenticate(request, response);
        assertEquals("Second: The status code should be " + HttpServletResponse.SC_OK + " and got " + taiResult.getStatus() + ".",
                     HttpServletResponse.SC_OK, taiResult.getStatus());

        states.become("third");
        mockery.checking(new Expectations() {
            {
                atMost(2).of(ssoConfig).getMapToUserRegistry();
                will(returnValue(Constants.MapToUserRegistry.Group));
            }
        });
        taiResult = autehnticator.authenticate(request, response);
        assertEquals("Third: The status code should be " + HttpServletResponse.SC_OK + " and got " + taiResult.getStatus() + ".",
                     HttpServletResponse.SC_OK, taiResult.getStatus());
    }

}
