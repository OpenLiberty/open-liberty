/*******************************************************************************
 * Copyright (c) 2016, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
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
    private final SSOCookieHelperImpl ssoCookieHelper = mock.mock(SSOCookieHelperImpl.class);
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
                allowing(webAppSecConfig).createSSOCookieHelper();
                will(returnValue(ssoCookieHelper));

                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));

                allowing(webAppSecConfig).getSSODomainList();
                will(returnValue(null));
                allowing(webAppSecConfig).getSSOUseDomainFromURL();
                will(returnValue(false));

                allowing(ssoCookieHelper).getSSODomainName(with(any(javax.servlet.http.HttpServletRequest.class)), with(any(List.class)), with(any(Boolean.class)));
                will(returnValue(null));
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
                one(referCookieHandler).createCookie("fred", "", request);
                allowing(response).addCookie(with(any(Cookie.class)));
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
                will(returnValue("secret1234"));
            }
        });
        final String expectedNonceCookieName = OidcStorageUtils.getNonceStorageKey("client01", state);
        final String expectedNonceCookieValue = OidcStorageUtils.createNonceStorageValue(nonceValue, state, "secret1234");

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
                one(referCookieHandler).createCookie(expectedNonceCookieName, "", request);
                allowing(response).addCookie(with(any(Cookie.class)));
            }
        });
        boolean validNonceCookie = OidcUtil.verifyNonce(convClientRequest, nonceValue, convClientConfig, state);
        assertTrue("The created nonce cookie is not considered valid but should have been.", validNonceCookie);
    }

}
