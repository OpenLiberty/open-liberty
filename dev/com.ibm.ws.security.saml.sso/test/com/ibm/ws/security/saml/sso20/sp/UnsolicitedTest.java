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
package com.ibm.ws.security.saml.sso20.sp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.security.tai.TAIResult;

import test.common.SharedOutputManager;

public class UnsolicitedTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final WebAppSecurityConfig webAppSecConfig = common.getWebAppSecConfig();
    static {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
    }

    private static final Cache cache = common.getCache();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final HttpServletRequest request = common.getServletRequest();
    private static final HttpServletResponse response = common.getServletResponse();
    private static final ForwardRequestInfo cachingRequestInfo = common.getRequestInfo();
    private static final PrintWriter out = mockery.mock(PrintWriter.class, "out");

    private static final String LOGIN_PAGE_URL = "http://localhost/loginPageUrl?company=ibm";
    private static final String PROVIDER_ID = "b07b804c";
    private static Unsolicited initiator;

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);

//        mockery.checking(new Expectations() {
//            {
//                atMost(2).of(request).getRequestURL();
//                will(returnValue(new StringBuffer("http://localhost:8010/formlogin")));
//                allowing(request).getQueryString();//
//                will(returnValue(null));//
//                one(request).getMethod();
//                will(returnValue("PUT"));
//
//                atMost(2).of(response).setStatus(with(any(Integer.class)));
//                atMost(2).of(response).addCookie(with(any(Cookie.class)));
//                atMost(2).of(response).setHeader(with(any(String.class)), with(any(String.class)));
//                atMost(2).of(response).setHeader(with(any(String.class)), with(any(String.class)));
//                atMost(2).of(response).setDateHeader(with(any(String.class)), with(any(Integer.class)));
//                atMost(2).of(response).setContentType(with(any(String.class)));
//
//                atMost(2).of(webAppSecConfig).getHttpOnlyCookies();
//                will(returnValue(with(any(Boolean.class))));
//                atMost(2).of(webAppSecConfig).getSSORequiresSSL();
//                will(returnValue(with(any(Boolean.class))));
//                allowing(webAppSecConfig).createReferrerURLCookieHandler();
//                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));
//
//                allowing(ssoService).getProviderId();
//                will(returnValue(PROVIDER_ID));
//                one(ssoService).getAcsCookieCache(PROVIDER_ID);
//                will(returnValue(cache));
//
//                one(cache).put(with(any(String.class)), with(any(ForwardRequestInfo.class)));
//
//                allowing(webAppSecConfig).getSSORequiresSSL();
//                will(returnValue(true));
//
//                allowing(request).setAttribute("SpSLOInProgress", "true");
//            }
//        });
//
//        initiator = new Unsolicited(ssoService);

    }

    @AfterClass
    public static void tearDown() {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(null);
//        mockery.assertIsSatisfied();
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testDeleteMe() {
        // TODO - Delete me
    }

    //@Test
    public void testSendRequestToLoginPageUrl() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(response).getWriter();
                will(returnValue(out));

                one(out).println(with(any(String.class)));
                one(out).flush();
                allowing(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });

        try {
            TAIResult result = initiator.sendRequestToLoginPageUrl(request, response, LOGIN_PAGE_URL);
            assertTrue("The TAIResult must not be null.", result != null);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    //@Test
    public void testRedirectToUserDefinedLoginPageURL() throws IOException {
        final IOException e = new IOException();

        mockery.checking(new Expectations() {
            {
                one(cachingRequestInfo).getFragmentCookieId();
                will(returnValue("MhG22Pgu"));

                one(response).getWriter();
                will(throwException(e));

                allowing(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });

        try {
            initiator.redirectToUserDefinedLoginPageURL(request, response, "targetId", LOGIN_PAGE_URL, cachingRequestInfo);
            fail("WebTrustAssociationFailedException was not thrown");
        } catch (WebTrustAssociationFailedException ex) {
            // the exception is expected
        }
    }
}
