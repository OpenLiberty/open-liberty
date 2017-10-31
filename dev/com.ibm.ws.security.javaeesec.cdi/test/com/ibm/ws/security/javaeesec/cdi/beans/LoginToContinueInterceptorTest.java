/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import  java.lang.StringBuffer;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import javax.interceptor.InvocationContext;

import javax.security.auth.message.MessageInfo;

import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.jaspi.JaspiConstants;
import com.ibm.ws.security.javaeesec.properties.ModuleProperties;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;

import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class LoginToContinueInterceptorTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("rawtypes")
    
    private InvocationContext ic;
    private ModulePropertiesProvider mpp;
    private LoginToContinueInterceptor ltci;
    private ReferrerURLCookieHandler ruh;
    private WebAppSecurityConfig wasc;
    private HttpMessageContext hmc;
    private MessageInfo mi;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private WebRequest wr;
    private SecurityMetadata smd;
    private RequestDispatcher rd;
    private CDI cdi;

    private boolean isInterceptedMethod = false;
    private boolean isCustomClass = false;
    
    private final String LOGIN_PAGE = "/login.jsp";
    private final String ERROR_PAGE = "/error.jsp";

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.javaeesec.*=all");

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        ic = mockery.mock(InvocationContext.class);
        mpp = mockery.mock(ModulePropertiesProvider.class);
        wasc = mockery.mock(WebAppSecurityConfig.class);
        ruh = mockery.mock(ReferrerURLCookieHandler.class);
        hmc = mockery.mock(HttpMessageContext.class);
        mi = mockery.mock(MessageInfo.class);
        wr = mockery.mock(WebRequest.class);
        smd = mockery.mock(SecurityMetadata.class);
        rd = mockery.mock(RequestDispatcher.class);
        req = mockery.mock(HttpServletRequest.class);
        res = mockery.mock(HttpServletResponse.class);
        cdi = mockery.mock(CDI.class);

        ltci = new LoginToContinueInterceptor() {
            @Override
            protected boolean isMethodToIntercept(InvocationContext ic) {
                return isInterceptedMethod;
            }

            @Override
            protected boolean isCustomForm(InvocationContext ic) {
                return isCustomClass;
            }

            @Override
             protected WebAppSecurityConfig getWebSAppSeurityConfig() {
                 return wasc;
             }

            @Override
            protected CDI getCDI() {
                return cdi;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    /**
     *  valid method. valid objects.
     *  Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptContinueFormRedirect() throws Exception {
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.FALSE);
        Object expect = AuthenticationStatus.SEND_CONTINUE;
        String storedReq = "http://localhost:80/contextRoot/original.html";
        String requestUrl ="http://localhost:80/contextRoot/request.html";
        withInvocationContext(expect).withProps(props).withParams().withReferrer().withSetCookies().withRedirect(LOGIN_PAGE);

        assertEquals("The SEND_CONTINUE should be returned.", expect, ltci.intercept(ic));
    }

    /**
     *  valid method. valid objects.
     *  Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptContinueFormForward() throws Exception {
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.TRUE);
        Object expect = AuthenticationStatus.SEND_CONTINUE;
        String storedReq = "http://localhost:80/contextRoot/original.html";
        String requestUrl ="http://localhost:80/contextRoot/request.html";
        withInvocationContext(expect).withProps(props).withParams().withReferrer().withSetCookies().withForward(LOGIN_PAGE);

        assertEquals("The SEND_CONTINUE should be returned.", expect, ltci.intercept(ic));
    }

    /**
     *  valid method. valid objects.
     *  Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptContinueFormDefault() throws Exception {
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        Object expect = AuthenticationStatus.SEND_CONTINUE;
        String storedReq = "http://localhost:80/contextRoot/original.html";
        String requestUrl ="http://localhost:80/contextRoot/request.html";
        withInvocationContext(expect).withProps(props).withParams().withReferrer().withSetCookies().withForward(LOGIN_PAGE);

        assertEquals("The SEND_CONTINUE should be returned.", expect, ltci.intercept(ic));
    }

    /**
     *  valid method. valid objects.
     *  Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptSuccessCustomForm() throws Exception {
        isInterceptedMethod = true;
        isCustomClass = true;
        ltci.setMPP(mpp);
        Object expect = AuthenticationStatus.SUCCESS;
        Properties props = new Properties();
        String storedReq = "http://localhost:80/contextRoot/original.html";
        String requestUrl ="http://localhost:80/contextRoot/request.html";
        withInvocationContext(expect).withParams().withReferrer().withGetURL(storedReq, requestUrl);

        assertEquals("The SUCCESS should be returned.", expect, ltci.intercept(ic));
    }

    /**
     *  valid method. valid objects.
     *  Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptSuccessForm() throws Exception {
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Object expect = AuthenticationStatus.SUCCESS;
        Properties props = new Properties();
        String storedReq = "http://localhost:80/contextRoot/original.html";
        String requestUrl ="http://localhost:80/contextRoot/request.html";
        withInvocationContext(expect).withParams().withReferrer().withGetURL(storedReq, requestUrl);

        assertEquals("The SUCCESS should be returned.", expect, ltci.intercept(ic));
    }

    /**
     *  valid method. No ModulePropertiesProvider object.
     *  Make sure that AuthenticationStatus.SEND_FAILURE is returned along with the error message in the log file.
     */
    @Test
    public void testInterceptNoMpp() throws Exception {
        isInterceptedMethod = true;
        ltci.setMPP(null);
        assertEquals("The SEND_FAILURE should be returned.", AuthenticationStatus.SEND_FAILURE, ltci.intercept(ic));
        assertTrue("CWWKS1926E  message was not logged", outputMgr.checkForStandardErr("CWWKS1926E:"));
    }

    /**
     *  Unrelated method invocation, make sure that the target method is invoked.
     */
    @Test
    public void testInterceptDifferentMethod() throws Exception {
        isInterceptedMethod = false;
        Object expect = new Object();
        withInvocationContext(expect);
        assertEquals("The request should not be intercepted.", expect, ltci.intercept(ic));
    }

    /*************** support methods **************/
    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withInvocationContext(final Object result) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(ic).proceed();
                will(returnValue(result));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withParams() throws Exception {
        final Object[] params = {req, res, hmc};
        mockery.checking(new Expectations() {
            {
                one(ic).getParameters();
                will(returnValue(params));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withProps(final Properties props) throws Exception {
        final HashMap map = new HashMap();
        map.put(JaspiConstants.SECURITY_WEB_REQUEST, wr);
        final Object target = new String();
        mockery.checking(new Expectations() {
            {
                one(ic).getTarget();
                will(returnValue(target));
                one(mpp).getAuthMechProperties(with(any(Class.class)));
                will(returnValue(props));
                one(hmc).getMessageInfo();
                will(returnValue(mi));
                one(mi).getMap();
                will(returnValue(map));
                one(wr).getSecurityMetadata();
                will(returnValue(smd));
                one(smd).setLoginConfiguration(with(any(LoginConfiguration.class)));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withReferrer() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(wasc).createReferrerURLCookieHandler();
                will(returnValue(ruh));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withGetURL(final String storedReq, final String requestUrl) throws Exception {
        final StringBuffer sb = new StringBuffer(requestUrl);
        mockery.checking(new Expectations() {
            {
                one(ruh).getReferrerURLFromCookies(req, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
                will(returnValue(storedReq));
                one(req).getRequestURL();
                will(returnValue(sb));
                one(res).encodeURL(storedReq);
                will(returnValue(storedReq));
                one(res).setHeader("Location", storedReq);
                one(res).setStatus(HttpServletResponse.SC_FOUND);
                one(wasc).getWASReqURLRedirectDomainNames();
                will(returnValue(null));
            }
        });
        if (isCustomClass) {
            mockery.checking(new Expectations() {
                {
                    one(ruh).invalidateReferrerURLCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
                }
            });
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withSetCookies() throws Exception {
        final String ORIGINAL_URL = "http://localhost/original.html";
        final StringBuffer sb = new StringBuffer(ORIGINAL_URL);
        mockery.checking(new Expectations() {
            {
                one(req).getQueryString();
                will(returnValue(null));
                one(req).getRequestURL();
                will(returnValue(sb));
                one(req).isSecure();
                will(returnValue(true));
                one(wasc).getSSORequiresSSL();
                will(returnValue(false));
                one(req).getMethod();
                will(returnValue("GET"));
                one(ruh).setReferrerURLCookie(with(any(HttpServletRequest.class)), with(any(AuthenticationResult.class)), with(any(String.class)));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withForward(final String loginPage) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(req).getRequestDispatcher(loginPage);
                will(returnValue(rd));
                one(rd).forward(req, res);
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withRedirect(final String loginPage) throws Exception {
        final String CONTEXT_ROOT = "/context_root";
        final String ORIGINAL_URL = "http://localhost" + CONTEXT_ROOT + "/original.html";
        final String LOGIN_URL = "http://localhost" + CONTEXT_ROOT + loginPage;
        final StringBuffer sb = new StringBuffer(ORIGINAL_URL);
        mockery.checking(new Expectations() {
            {
                one(res).setStatus(HttpServletResponse.SC_FOUND);
                one(req).getRequestURL();
                will(returnValue(sb));
                one(req).getContextPath();
                will(returnValue(CONTEXT_ROOT));
                one(res).sendRedirect(LOGIN_URL);
                one(res).encodeURL(LOGIN_URL);
                will(returnValue(LOGIN_URL));
            }
        });
        return this;
    }

}
