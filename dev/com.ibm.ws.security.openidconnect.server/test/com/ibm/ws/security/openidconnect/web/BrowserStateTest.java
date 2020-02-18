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
import static org.junit.Assert.assertNull;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.server.plugins.OIDCBrowserStateUtil;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

/*
 *
 */

public class BrowserStateTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;
    private final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletResponse response = context.mock(HttpServletResponse.class);
    private final WebAppSecurityConfig webAppSecConfig = context.mock(WebAppSecurityConfig.class);

    @Before
    public void setUp() {}

    /*
     * test getOriginalBrowserState method with no cookie.
     * expect result is null.
     */
    @Test
    public void testGetOriginalBrowserStateNull() {

        MockServletRequest req = new MockServletRequest();

        BrowserState browserState = new BrowserState();
        assertNull("return value shall be null.", browserState.getOriginalBrowserState(req));
    }

    /*
     * test getOriginalBrowserState method with valid cookie.
     * expect result is same cookie value as the original one.
     */
    @Test
    public void testGetOriginalBrowserStateValid() {

        MockServletRequest req = new MockServletRequest();
        String value = "browser_state_value";
        Cookie[] cookies = new Cookie[] { new Cookie(OIDCConstants.OIDC_BROWSER_STATE_COOKIE, value) };
        req.setCookies(cookies);

        BrowserState browserState = new BrowserState();
        assertEquals(value, browserState.getOriginalBrowserState(req));
    }

    /*
     * test processSession method
     * Input: no Cookie.
     * expect result : adding a new cookie.
     */
    @Test
    public void testProcessSessionNoCookie() {

        MockServletRequest req = new MockServletRequest();
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);

        context.checking(new Expectations() {
            {
                allowing(webAppSecConfig).isIncludePathInWASReqURL();
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));
                one(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                one(response).addCookie(with(any(Cookie.class)));
                allowing(webAppSecConfig).getSameSiteCookie();
                will(returnValue("Disabled"));
            }
        });
        BrowserState browserState = new BrowserState();
        browserState.processSession(req, response);
    }

    /*
     * test processSession method
     * Input: Cookie exists.
     * expect result : no action.
     */
    @Test
    public void testProcessSession() {

        MockServletRequest req = new MockServletRequest();
        String value = OIDCBrowserStateUtil.generateOIDCBrowserState(false);
        Cookie[] cookies = new Cookie[] { new Cookie(OIDCConstants.OIDC_BROWSER_STATE_COOKIE, value) };
        req.setCookies(cookies);
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);

        context.checking(new Expectations() {
            {
                never(response).addCookie(with(any(Cookie.class)));
            }
        });

        BrowserState browserState = new BrowserState();
        browserState.processSession(req, response);
    }

    /*
     * test generateState method
     * Input: valid input of browser session, client id.
     * expect result : session state is generated. (the generated value varies)
     */
    @Test
    public void testGenerateStateWithVaildValues() {

        MockServletRequest req = new MockServletRequest();
        req.setParameter(OAuth20Constants.CLIENT_ID, "client01");
        AttributeList al = new AttributeList();

        BrowserState browserState = new BrowserState();
        browserState.generateState(req, al);
        String[] retValue = al.getAttributeValuesByNameAndType(OIDCConstants.OIDC_SESSION_STATE, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE);
        assertNotNull(retValue);
        assertEquals(1, retValue.length);
    }
}