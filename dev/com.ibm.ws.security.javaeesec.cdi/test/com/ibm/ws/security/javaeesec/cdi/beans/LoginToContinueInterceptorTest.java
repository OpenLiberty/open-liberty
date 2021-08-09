/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.Properties;

import javax.el.ELProcessor;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.interceptor.InvocationContext;
import javax.security.auth.message.MessageInfo;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.Credential;
import javax.servlet.http.Cookie;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;

import test.common.SharedOutputManager;

public class LoginToContinueInterceptorTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("rawtypes")

    private InvocationContext ici, icm;
    private ModulePropertiesProvider mpp;
    private Instance<ModulePropertiesProvider> mppi;

    private LoginToContinueInterceptor ltci;
    private ReferrerURLCookieHandler ruh;
    private WebAppSecurityConfig wasc;
    private HttpMessageContext hmc;
    private AuthenticationParameters ap;
    private MessageInfo mi;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private WebRequest wr;
    private SecurityMetadata smd;
    private RequestDispatcher rd;
    private ELProcessor elp, elpi, elpm;
    private CDI cdi;
    private Credential cred;
    private Principal principal;
    private Cookie sessionCookie, wasReqUrlCookie;

    private boolean isInterceptedMethod = false;
    private Class hamClass = null;
    private final Class CUSTOM_FORM_CLASS = CustomFormAuthenticationMechanism.class;
    private final Class FORM_CLASS = FormAuthenticationMechanism.class;
    private final Class CUSTOM_HAM_CLASS = String.class;

    private final String LOGIN_PAGE = "/login.jsp";
    private final String ERROR_PAGE = "/error.jsp";
    private final String EL_LOGIN_PAGE_RESOLVED = "/login_el_resolved.jsp";
    private final String EL_ERROR_PAGE_RESOLVED = "/error_el_resolved.jsp";
    private final String EL_ERROR_PAGE = "someBean.errorPage";
    private final String EL_LOGIN_PAGE = "someBean.loginPage";
    private final String EL_IS_FORWARD = "someBean.isForward";
    private final String EL_IMMEDIATE_ERROR_PAGE = "${" + EL_ERROR_PAGE + "}";
    private final String EL_IMMEDIATE_LOGIN_PAGE = "${" + EL_LOGIN_PAGE + "}";
    private final String EL_IMMEDIATE_IS_FORWARD = "${" + EL_IS_FORWARD + "}";
    private final String EL_DEFERRED_ERROR_PAGE = "#{" + EL_ERROR_PAGE + "}";
    private final String EL_DEFERRED_LOGIN_PAGE = "#{" + EL_LOGIN_PAGE + "}";
    private final String EL_DEFERRED_IS_FORWARD = "#{" + EL_IS_FORWARD + "}";
    private final String SESSION_COOKIE = "jaspicSession";
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
        ici = mockery.mock(InvocationContext.class, "ici");
        icm = mockery.mock(InvocationContext.class, "icm");
        mpp = mockery.mock(ModulePropertiesProvider.class);
        mppi = mockery.mock(Instance.class, "mppi");
        wasc = mockery.mock(WebAppSecurityConfig.class);
        ruh = mockery.mock(ReferrerURLCookieHandler.class);
        hmc = mockery.mock(HttpMessageContext.class);
        ap = mockery.mock(AuthenticationParameters.class);
        mi = mockery.mock(MessageInfo.class);
        wr = mockery.mock(WebRequest.class);
        smd = mockery.mock(SecurityMetadata.class);
        rd = mockery.mock(RequestDispatcher.class);
        req = mockery.mock(HttpServletRequest.class);
        res = mockery.mock(HttpServletResponse.class);
        elpi = mockery.mock(ELProcessor.class, "elpi");
        elpm = mockery.mock(ELProcessor.class, "elpm");
        cdi = mockery.mock(CDI.class);
        cred = mockery.mock(Credential.class, "cred1");
        principal = mockery.mock(Principal.class, "principal1");
        sessionCookie = mockery.mock(Cookie.class, "session");
        wasReqUrlCookie = mockery.mock(Cookie.class, "wasrequrl");

        ltci = new LoginToContinueInterceptor() {
            @Override
            protected boolean isMethodToIntercept(InvocationContext ic) {
                return isInterceptedMethod;
            }

            @Override
            protected Class getClass(InvocationContext ic) {
                // anything would be fine since this value is not used.
                return hamClass;
            }

            @Override
            protected Class getTargetClass(InvocationContext ic) {
                return hamClass;
            }

            @Override
            protected WebAppSecurityConfig getWebAppSecurityConfig() {
                return wasc;
            }

            @Override
            protected ELProcessor getELProcessorWithAppModuleBeanManagerELResolver() {
                return elp;
            }

            @Override
            protected CDI getCDI() {
                return cdi;
            }

            @Override
            protected SecurityMetadata getSecurityMetadata() {
                return smd;
            }

        };
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    /**
     * initialize with no EL.
     * make sure that resolved is set true, isForward is set as specified, elForward is null;
     */
    @Test
    public void testInitializeNoEL() throws Exception {
        ltci.setMPP(mpp);
        hamClass = FORM_CLASS;
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.FALSE);
        withInitProps(props);
        ltci.initialize(ici);

        assertEquals("_errorPage field should be set as specified.", ERROR_PAGE, ltci.getErrorPage());
        assertEquals("_loginPage field should be set as specified.", LOGIN_PAGE, ltci.getLoginPage());
        assertFalse("_isForward field should be set as false.", ltci.getIsForward());

    }

    /**
     * initialize with immediate EL with Form HAM.
     * make sure that resolved is set true, isForward is set as specified, elForward is the unrwrapped EL value;
     */
    @Test
    public void testInitializeELImmediateWithForm() throws Exception {
        ltci.setMPP(mpp);
        hamClass = FORM_CLASS;
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.FALSE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, EL_IMMEDIATE_LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, EL_IMMEDIATE_ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, EL_IMMEDIATE_IS_FORWARD);
        withInitProps(props).wlthELBoolean(elpi, EL_IS_FORWARD, Boolean.TRUE);
        wlthELString(elpi, EL_ERROR_PAGE, EL_ERROR_PAGE_RESOLVED);
        wlthELString(elpi, EL_LOGIN_PAGE, EL_LOGIN_PAGE_RESOLVED);
        ltci.initialize(ici);

        assertEquals("_errorPage field should be set as specified.", EL_ERROR_PAGE_RESOLVED, ltci.getErrorPage());
        assertEquals("_loginPage field should be set as specified.", EL_LOGIN_PAGE_RESOLVED, ltci.getLoginPage());
        assertTrue("_isForward field should be set as true.", ltci.getIsForward());

    }

    /**
     * initialize with immediate EL with CustomForm HAM.
     * in this case, even it is emmediate EL, it's treated as deferred.
     * make sure that resolved is set false, isForward is unknown, elForward is the unrwrapped EL value;
     */
    @Test
    public void testInitializeELImmediateWithCustomForm() throws Exception {
        ltci.setMPP(mpp);
        hamClass = CUSTOM_HAM_CLASS;
        withNoELP();

        assertNull("_errorPage field should be null.", ltci.getErrorPage());
        assertNull("_loginPage field should be null.", ltci.getLoginPage());
        assertNull("_isForward field should be null.", ltci.getIsForward());
    }

    /**
     * initialize with deferred EL with Form HAM.
     * make sure that resolved is set true, isForward is unknown, elForward is the unrwrapped EL value;
     */
    @Test
    public void testInitializeELDeferredWithForm() throws Exception {
        ltci.setMPP(mpp);
        hamClass = FORM_CLASS;
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.FALSE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, EL_DEFERRED_LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, EL_DEFERRED_ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, EL_DEFERRED_IS_FORWARD);
        withInitProps(props).withNoELP();
        ltci.initialize(ici);

        assertNull("_errorPage field should be null.", ltci.getErrorPage());
        assertNull("_loginPage field should be null.", ltci.getLoginPage());
        assertNull("_isForward field should be null.", ltci.getIsForward());
    }

    /**
     * initialize with deferred EL with CustomForm HAM.
     * make sure that resolved is set true, isForward is unknown, elForward is the unrwrapped EL value;
     */
    @Test
    public void testInitializeELDeferredWithCustomForm() throws Exception {
        ltci.setMPP(mpp);
        hamClass = CUSTOM_HAM_CLASS;
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.FALSE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, EL_DEFERRED_LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, EL_DEFERRED_ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, EL_DEFERRED_IS_FORWARD);
        withProps(props).withNoELP();
        ltci.initialize(ici);

        assertNull("_errorPage field should be null.", ltci.getErrorPage());
        assertNull("_loginPage field should be null.", ltci.getLoginPage());
        assertNull("_isForward field should be null.", ltci.getIsForward());
    }

    /**
     * valid method. valid objects.
     * use immediate EL. Make sure that el resolution happens during initiallization, and does not happen
     * while intercepting the request.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptContinueFormELImmediate() throws Exception {
        isInterceptedMethod = true;
        hamClass = FORM_CLASS;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.FALSE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, EL_IMMEDIATE_LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, EL_IMMEDIATE_ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, EL_IMMEDIATE_IS_FORWARD);
        Object expect = AuthenticationStatus.SEND_CONTINUE;
        withProps(props).withLoginConfig().withParams().withReferrer().withSetCookies().withRedirect(EL_LOGIN_PAGE_RESOLVED).withAuthParams(null);
        withJSecurityCheck("contextRoot/original.html").withSessionCookie(null).withProtected(true);
        wlthELBoolean(elpi, EL_IS_FORWARD, Boolean.FALSE).withNoELP(elpm);
        wlthELString(elpi, EL_ERROR_PAGE, EL_ERROR_PAGE_RESOLVED);
        wlthELString(elpi, EL_LOGIN_PAGE, EL_LOGIN_PAGE_RESOLVED);
        ltci.initialize(ici);
        assertEquals("The SEND_CONTINUE should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. valid objects.
     * use deferred EL. Make sure that el resolution happens every interception.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptContinueFormELDeferred() throws Exception {
        isInterceptedMethod = true;
        hamClass = FORM_CLASS;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, EL_DEFERRED_LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, EL_DEFERRED_ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION, EL_DEFERRED_IS_FORWARD);
        Object expect = AuthenticationStatus.SEND_CONTINUE;
        withProps(props).withLoginConfig().withParams().withReferrer().withSetCookies().withRedirect(EL_LOGIN_PAGE_RESOLVED).withAuthParams(null);
        withJSecurityCheck("contextRoot/original.html").withSessionCookie(null).withProtected(true);
        wlthELBoolean(elpm, EL_IS_FORWARD, Boolean.FALSE).withNoELP(elpi);
        wlthELString(elpm, EL_ERROR_PAGE, EL_ERROR_PAGE_RESOLVED);
        wlthELString(elpm, EL_LOGIN_PAGE, EL_LOGIN_PAGE_RESOLVED);
        ltci.initialize(ici);
        assertEquals("The SEND_CONTINUE should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. valid objects.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptContinueFormRedirect() throws Exception {
        hamClass = FORM_CLASS;
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.FALSE);
        Object expect = AuthenticationStatus.SEND_CONTINUE;
        withProps(props).withLoginConfig().withParams().withReferrer().withSetCookies().withRedirect(LOGIN_PAGE).withNoELP().withAuthParams(null);
        withJSecurityCheck("contextRoot/original.html").withSessionCookie(null).withProtected(true);

        ltci.initialize(ici);
        assertEquals("The SEND_CONTINUE should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. valid objects.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptContinueFormForward() throws Exception {
        hamClass = CUSTOM_FORM_CLASS;
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, Boolean.TRUE);
        Object expect = AuthenticationStatus.SEND_CONTINUE;
        withProps(props).withLoginConfig().withParams().withReferrer().withSetCookies().withForward(LOGIN_PAGE).withNoELP().withAuthParams(null);
        withJSecurityCheck("contextRoot/original.html").withSessionCookie(null).withProtected(true);

        ltci.initialize(ici);
        assertEquals("The SEND_CONTINUE should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. valid objects.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptContinueFormDefault() throws Exception {
        hamClass = FORM_CLASS;
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        Object expect = AuthenticationStatus.SEND_CONTINUE;
        withProps(props).withLoginConfig().withParams().withReferrer().withSetCookies().withForward(LOGIN_PAGE).withNoELP().withAuthParams(null);
        withJSecurityCheck("contextRoot/original.html").withSessionCookie(null).withProtected(true);
        ltci.initialize(ici);
        assertEquals("The SEND_CONTINUE should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. valid objects.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptSuccessCustomForm() throws Exception {
        isInterceptedMethod = true;
        hamClass = CUSTOM_FORM_CLASS;
        ltci.setMPP(mpp);
        Object expect = AuthenticationStatus.SUCCESS;
        Properties props = new Properties();
        String storedReq = "http://localhost:80/contextRoot/original.html";
        String requestUrl = "http://localhost:80/contextRoot/request.html";
        withInvocationContext(expect).withParams().withReferrer().withGetURL(storedReq, requestUrl).withAuthParams(cred);
        withJSecurityCheck("contextRoot/original.html");

        assertEquals("The SUCCESS should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. valid objects.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptSuccessForm() throws Exception {
        isInterceptedMethod = true;
        hamClass = FORM_CLASS;
        ltci.setMPP(mpp);
        Object expect = AuthenticationStatus.SUCCESS;
        Properties props = new Properties();
        String storedReq = "http://localhost:80/contextRoot/original.html";
        String requestUrl = "http://localhost:80/contextRoot/request.html";
        withInvocationContext(expect).withParams().withReferrer().withGetURL(storedReq, requestUrl).withAuthParams(null);
        withJSecurityCheck("/j_security_check");

        assertEquals("The SUCCESS should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. valid objects.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptSessionCookieExist() throws Exception {
        hamClass = FORM_CLASS;
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        Object expect = AuthenticationStatus.SUCCESS;
        withProps(props).withParams().withNoELP().withAuthParams(null);
        withJSecurityCheck("contextRoot/original.html").withSessionCookie(principal).withWasReqUrlCookie(false);
        ltci.initialize(ici);
        assertEquals("The SUCCESS should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. valid objects.
     * Make sure that AuthenticationStatus.SUCCESS is returned along with redirection to the original url.
     */
    @Test
    public void testInterceptSessionCookieExistOnOriginalURLAfterAuthenticate() throws Exception {
        hamClass = FORM_CLASS;
        isInterceptedMethod = true;
        ltci.setMPP(mpp);
        Properties props = new Properties();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, LOGIN_PAGE);
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, ERROR_PAGE);
        final Cookie[] cookies = {sessionCookie};
        Object expect = AuthenticationStatus.SUCCESS;
        withProps(props).withParams().withNoELP().withAuthParams(null).withReferrer();
        withJSecurityCheck("contextRoot/original.html").withSessionCookie(principal).withWasReqUrlCookie(true).withRemoveWasReqUrlCookie();

        ltci.initialize(ici);
        assertEquals("The SUCCESS should be returned.", expect, ltci.intercept(icm));
    }

    /**
     * valid method. No ModulePropertiesProvider object.
     * Make sure that AuthenticationStatus.SEND_FAILURE is returned along with the error message in the log file.
     */
    @Test
    public void testInterceptNoMpp() throws Exception {
        isInterceptedMethod = true;
        ltci.setMPP(null);
        hamClass = FORM_CLASS;
        assertEquals("The SEND_FAILURE should be returned.", AuthenticationStatus.SEND_FAILURE, ltci.intercept(icm));
        assertTrue("CWWKS1926E  message was not logged", outputMgr.checkForStandardErr("CWWKS1926E:"));
    }

    /**
     * Unrelated method invocation, make sure that the target method is invoked.
     */
    @Test
    public void testInterceptDifferentMethod() throws Exception {
        isInterceptedMethod = false;
        Object expect = new Object();
        withInvocationContext(expect);
        assertEquals("The request should not be intercepted.", expect, ltci.intercept(icm));
    }

    /*************** support methods **************/
    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withInvocationContext(final Object result) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(icm).proceed();
                will(returnValue(result));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withParams() throws Exception {
        final Object[] params = { req, res, hmc };
        mockery.checking(new Expectations() {
            {
                one(icm).getParameters();
                will(returnValue(params));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withInitProps(final Properties props) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(mppi));
                one(mppi).get();
                will(returnValue(mpp));
                one(mpp).getAuthMechProperties(with(any(Class.class)));
                will(returnValue(props));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest wlthELBoolean(final ELProcessor elp, final String elValue, final Boolean output) {
        this.elp = elp;
        mockery.checking(new Expectations() {
            {
                one(elp).eval(elValue);
                will(returnValue(output));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest wlthELString(final ELProcessor elp, final String elValue, final String output) {
        this.elp = elp;
        mockery.checking(new Expectations() {
            {
                one(elp).eval(elValue);
                will(returnValue(output));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withNoELP() {
        mockery.checking(new Expectations() {
            {
                never(elpi).eval(with(any(String.class)));
                never(elpm).eval(with(any(String.class)));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withNoELP(final ELProcessor elp) {
        mockery.checking(new Expectations() {
            {
                never(elp).eval(with(any(String.class)));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withProps(final Properties props) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(mppi));
                one(mppi).get();
                will(returnValue(mpp));
                // when trace is enabled, number of invocation would change, therefore it is set as 1 to 3.
                between(1, 3).of(mpp).getAuthMechProperties(with(any(Class.class)));
                will(returnValue(props));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withLoginConfig() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(smd).setLoginConfiguration(with(any(LoginConfiguration.class)));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withAuthParams(final Credential cred) {
        mockery.checking(new Expectations() {
            {
                between(1, 2).of(hmc).getAuthParameters();
                will(returnValue(ap));
                one(ap).isNewAuthentication();
                will(returnValue(false));
                between(0, 1).of(ap).getCredential();
                will(returnValue(cred));
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
                exactly(2).of(req).getRequestURL();
                will(returnValue(sb));
                one(res).encodeURL(storedReq);
                will(returnValue(storedReq));
                one(res).setHeader("Location", storedReq);
                one(res).setStatus(HttpServletResponse.SC_FOUND);
                one(wasc).getWASReqURLRedirectDomainNames();
                will(returnValue(null));
                one(req).setAttribute("com.ibm.ws.security.javaeesec.donePostLoginProcess", Boolean.TRUE);
            }
        });

        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withRemoveWasReqUrlCookie() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(ruh).invalidateReferrerURLCookie(req, res, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
            }
        });

        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withJSecurityCheck(final String uri) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(req).getRequestURI();
                will(returnValue(uri));
            }
        });
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
                between(1, 2).of(req).getMethod();
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

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withSessionCookie(final Principal principal) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(hmc).getRequest();
                will(returnValue(req));
                one(req).getUserPrincipal();
                will(returnValue(principal));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withProtected(final boolean value) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(hmc).isProtected();
                will(returnValue(value));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private LoginToContinueInterceptorTest withWasReqUrlCookie(boolean isExist) throws Exception {
        if (isExist) {
            final Cookie [] cookies = {wasReqUrlCookie};
            mockery.checking(new Expectations() {
                {
                    one(req).getCookies();
                    will(returnValue(cookies));
                    one(wasReqUrlCookie).getName();
                    will(returnValue(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME));
                    one(wasReqUrlCookie).getValue();
                    will(returnValue("value"));
                }
            });
        } else {
            mockery.checking(new Expectations() {
                {
                    one(req).getCookies();
                    will(returnValue(null));
                }
            });
        }
        return this;
    }
}
