/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml;

import static org.junit.Assert.assertTrue;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class SsoRequestTest {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final HttpServletRequest request = common.getServletRequest();
    private static final HttpServletResponse response = common.getServletResponse();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final UserData userData = common.getUserData();
    @SuppressWarnings("unchecked")
    private static final AtomicServiceReference<WsLocationAdmin> locationAdminRef = mockery.mock(AtomicServiceReference.class, "locationAdminRef");
    private static final WebAppSecurityConfig webAppSecConfig = common.getWebAppSecConfig();

    private static SsoRequest ssoRequest = new SsoRequest(null, null, null, null);

    @BeforeClass
    public static void setUp() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
    }

    @AfterClass
    public static void tearDown() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(null);
        mockery.assertIsSatisfied();
    }

    //@Test
    public void testSetSpCookie() {
        final String SP_COOKIE_NAME = "spCookieName";
        final String SP_COOKIE_VALUE = "spCookieValue";

        mockery.checking(new Expectations() {
            {
                one(ssoService).getConfig();
                will(returnValue(ssoConfig));

                one(ssoService).isInboundPropagation();
                will(returnValue(false));

                one(ssoConfig).isDisableLtpaCookie();
                will(returnValue(true));
                one(ssoConfig).getSpCookieName(null);
                will(returnValue(SP_COOKIE_NAME));

                one(locationAdminRef).getService();
                will(returnValue(null));

                one(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(with(any(Boolean.class))));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(with(any(Boolean.class))));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));

                one(response).addCookie(with(any(Cookie.class)));
            }
        });

        ssoRequest.setSsoSamlService(ssoService);
        ssoRequest.setLocationAdminRef(locationAdminRef);
        ssoRequest.setSpCookieValue(SP_COOKIE_VALUE);

        ssoRequest.createSpCookieIfDisableLtpa(request, response);
    }

    @Test
    public void testToString() {
        final String PROVIDER_NAME = "providerName";
        final SsoRequest ssoRequest = new SsoRequest(PROVIDER_NAME, null, request, null);
        ssoRequest.setType(Constants.EndpointType.ACS);
        ssoRequest.setUserData(userData);

        String result = ssoRequest.toString();

        assertTrue("The request does not contain the correct provider name '" + PROVIDER_NAME + "'.", result.contains(ssoRequest.getProviderName()));
        assertTrue("The request does not contain the correct endpoint type '" + Constants.EndpointType.ACS + "'.", result.contains(ssoRequest.getType().toString()));
        assertTrue("The request does not contain the correct request '" + request.toString() + "'.", result.contains(ssoRequest.getRequest().toString()));
        assertTrue("The request does not contain the correct value for user data.", result.contains(((Boolean) (ssoRequest.getUserData() != null)).toString()));

    }
}
