/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import test.common.SharedOutputManager;

public class OidcUtilTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);
    private final Cookie cookie = mock.mock(Cookie.class);
    private final Cookie cookie2 = mock.mock(Cookie.class, "cookie2");
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class, "response");
    private final HttpServletRequest request = mock.mock(HttpServletRequest.class, "request");
    private final ReferrerURLCookieHandler referCookieHandler = mock.mock(ReferrerURLCookieHandler.class, "referCookieHandler");
    private final OidcClientRequest convClientRequest = mock.mock(OidcClientRequest.class, "convClientRequest");
    private final ConvergedClientConfig convClientConfig = mock.mock(ConvergedClientConfig.class, "convClientConfig");

    @Before
    public void before() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
        mock.checking(new Expectations() {
            {
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(referCookieHandler));
            }
        });
        OidcClientUtil.setReferrerURLCookieHandler(referCookieHandler);
    }

    @After
    public void after() {
        mock.assertIsSatisfied();
    }

    @Test
    public void testInvalidateReferrerURLCookie() {
        mock.checking(new Expectations() {
            {
                one(referCookieHandler).invalidateCookie(request, response, "fred", true);
            }
        });
        OidcClientUtil.invalidateReferrerURLCookie(request, response, "fred");
    }

    @Test
    public void testEncodeQuery_simple() {
        assertEquals(null, OidcUtil.encodeQuery(null));
        assertEquals("", OidcUtil.encodeQuery(""));
        assertEquals("+", OidcUtil.encodeQuery(" "));
        assertEquals("0123456789abcdefghijklmnoprstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", OidcUtil.encodeQuery("0123456789abcdefghijklmnoprstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        assertEquals("+%21%22%23%24%25" + "&" + "%27%28%29" + "*" + "%2B%2C" + "-." + "%2F", OidcUtil.encodeQuery(" !\"#$%&'()*+,-./"));
    }

    @Test
    public void testEncodeQuery_params() {
        assertEquals("param=value", OidcUtil.encodeQuery("param=value"));
        assertEquals("=value", OidcUtil.encodeQuery("=value"));
        assertEquals("param=", OidcUtil.encodeQuery("param="));
        assertEquals("param=Some+value%3Cscript%3Ealert%28300%29%3C%2Fscript%3E", OidcUtil.encodeQuery("param=Some value<script>alert(300)</script>"));
        assertEquals("param=value&param=value", OidcUtil.encodeQuery("param=value&param=value"));
        assertEquals("param=value+of+param&param2&param3=value", OidcUtil.encodeQuery("param=value of param&param2&param3=value"));
        assertEquals("param=value+of+param&param2=%3Cscript%3Ealert%28300%29%3C%2Fscript%3E&param3=value",
                OidcUtil.encodeQuery("param=value of param&param2=<script>alert(300)</script>&param3=value"));
    }

    @Test
    public void testNonceCookie() {
        String state = "someStateValue";
        String nonceValue = "myNonceValue";

        mock.checking(new Expectations() {
            {
                allowing(convClientConfig).getId();
                will(returnValue("myConfigId"));
                one(convClientConfig).getClientId();
                will(returnValue("client01"));
                allowing(convClientConfig).getClientSecret();
                will(returnValue("secret"));
            }
        });
        final String expectedNonceCookieName = OidcStorageUtils.getNonceStorageKey("client01", state);
        final String expectedNonceCookieValue = OidcStorageUtils.createNonceStorageValue(nonceValue, state, "secret");

        mock.checking(new Expectations() {
            {
                allowing(convClientRequest).getRequest();
                will(returnValue(request));
                allowing(convClientRequest).getResponse();
                will(returnValue(response));
                one(request).getCookies();
                will(returnValue(new Cookie[] { cookie }));
                one(cookie).getName();
                will(returnValue(expectedNonceCookieName));
                one(cookie).getValue();
                will(returnValue(expectedNonceCookieValue));
                one(referCookieHandler).invalidateCookie(request, response, expectedNonceCookieName, true);
            }
        });
        boolean validNonceCookie = OidcUtil.verifyNonce(convClientRequest, nonceValue, convClientConfig, state);
        assertTrue("The created nonce cookie is not considered valid but should have been.", validNonceCookie);
    }

}
