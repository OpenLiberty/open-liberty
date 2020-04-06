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
package com.ibm.ws.security.saml.sso20.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;

public class SpCookieRetriverTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final IExtendedRequest extRequest = mockery.mock(IExtendedRequest.class, "extRequest");
    private static final SsoRequest samlRequest = common.getSsoRequest();
    private static final AuthCacheService authCacheService = mockery.mock(AuthCacheService.class, "authCacheService");

    private static final String PROVIDER_NAME = "sp";
    private static byte[] cookieValueBytes = { 'i', 'd', 'p', '_', 'i', 'n', 'i', 't', 'i', 'a', 'l', '_' };
    private static SpCookieRetriver cookieRetriver;

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        mockery.checking(new Expectations() {
            {
                allowing(samlRequest).getProviderName();
                will(returnValue(PROVIDER_NAME));
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testGetSubjectFromSpCookie() {
        final String SP_COOKIE_NAME = "SpCookieName";
        final Subject subject = new Subject();
        cookieRetriver = new SpCookieRetriver(authCacheService, extRequest, samlRequest);

        mockery.checking(new Expectations() {
            {
                one(samlRequest).getSpCookieName();
                will(returnValue(SP_COOKIE_NAME));
                one(extRequest).getCookieValueAsBytes(SP_COOKIE_NAME);
                will(returnValue(cookieValueBytes));
                one(authCacheService).getSubject(with(any(Object.class)));
                will(returnValue(subject));
            }
        });

        Subject result = cookieRetriver.getSubjectFromSpCookie();
        assertEquals("Expected to receive the correct Subject object but it was not received.",
                     subject, result);
    }

    @Test
    public void testGetSubjectFromSpCookie_NullAuthCacheService() {
        cookieRetriver = new SpCookieRetriver(null, extRequest, samlRequest);

        Subject result = cookieRetriver.getSubjectFromSpCookie();
        assertNull("Expected to receive a null value but was received " + result, result);
    }
}
