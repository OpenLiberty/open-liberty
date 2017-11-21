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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.el.ELProcessor;
import javax.interceptor.InvocationContext;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.RememberMe;
import javax.security.enterprise.credential.RememberMeCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.RememberMeIdentityStore;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.security.javaeesec.CDIHelperTestWrapper;

public class RememberMeInterceptorTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String DEFAULT_COOKIE_NAME = "JREMEMBERMEID";
    private static final String COOKIE_VALUE = "123";

    private RememberMeInterceptor interceptor;
    private HttpMessageContext httpMessageContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private CallerPrincipal callerPrincipal;
    private Set<String> groups;
    private CDIService cdiService;
    private CDIHelperTestWrapper cdiHelperTestWrapper;
    private RememberMeIdentityStore rememberMeIdentityStore;
    private CredentialValidationResult validResult;

    private final TestHttpAuthenticationMechanismWithDefaultRememberMe mechanismWithDefaultRememberMe = new TestHttpAuthenticationMechanismWithDefaultRememberMe();

    @Before
    public void setUp() throws Exception {
        interceptor = new RememberMeInterceptor();
        httpMessageContext = mockery.mock(HttpMessageContext.class);
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        cdiService = mockery.mock(CDIService.class);
        rememberMeIdentityStore = mockery.mock(RememberMeIdentityStore.class);
        cdiHelperTestWrapper = new CDIHelperTestWrapper(mockery, rememberMeIdentityStore);
        cdiHelperTestWrapper.setCDIService(cdiService);

        callerPrincipal = new CallerPrincipal("user1");
        groups = new HashSet<String>();
        validResult = new CredentialValidationResult(callerPrincipal, groups);

        mockery.checking(new Expectations() {
            {
                allowing(httpMessageContext).getRequest();
                will(returnValue(request));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        cdiHelperTestWrapper.unsetCDIService(cdiService);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testInterceptValidateRequestWithNoRemembeMeCookieAndAuthenticationFailure() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest", mechanismWithDefaultRememberMe);
        withCookies(null).doesNotNotifyContainerAboutLogin().invokesNextInterceptor(ic, AuthenticationStatus.SEND_FAILURE).doesNotGenerateLoginToken().doesNotCreateCookie();

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SEND_FAILURE.", AuthenticationStatus.SEND_FAILURE, status);
    }

    @Test
    public void testInterceptValidateRequestWithNoRemembeMeCookieAndAuthenticationSuccess() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest", mechanismWithDefaultRememberMe);
        withCookies(null).doesNotNotifyContainerAboutLogin();
        invokesNextInterceptor(ic, AuthenticationStatus.SUCCESS).withSecureRequest(true).generatesLoginToken().createsCookie(DEFAULT_COOKIE_NAME, COOKIE_VALUE, 86400, true, true);

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testInterceptValidateRequestWithNoRemembeMeCookieWithAuthenticationSuccessAndInsecureHttpRequest() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest", mechanismWithDefaultRememberMe);
        withCookies(null).doesNotNotifyContainerAboutLogin();
        invokesNextInterceptor(ic, AuthenticationStatus.SUCCESS).withSecureRequest(false).doesNotGenerateLoginToken().doesNotCreateCookie();

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testInterceptValidateRequestWithNoRemembeMeCookieWithAuthenticationSuccessAndCookieSecureOnlyFalse() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest", new TestHttpAuthenticationMechanismWithRememberMeInsecure());
        withCookies(null).doesNotNotifyContainerAboutLogin();
        invokesNextInterceptor(ic, AuthenticationStatus.SUCCESS);
        generatesLoginToken().createsCookie(DEFAULT_COOKIE_NAME, COOKIE_VALUE, 86400, false, false);

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testInterceptValidateRequestWithInvalidRemembeMeCookieAndAuthenticationFailure() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest", mechanismWithDefaultRememberMe);
        withCookies(DEFAULT_COOKIE_NAME, COOKIE_VALUE, CredentialValidationResult.INVALID_RESULT);
        doesNotNotifyContainerAboutLogin().removesCookie(DEFAULT_COOKIE_NAME);
        invokesNextInterceptor(ic, AuthenticationStatus.SEND_FAILURE).doesNotGenerateLoginToken().doesNotCreateCookie();

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SEND_FAILURE.", AuthenticationStatus.SEND_FAILURE, status);
    }

    @Test
    public void testInterceptValidateRequestWithInvalidRemembeMeCookieAndAuthenticationSuccess() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest", mechanismWithDefaultRememberMe);
        withCookies(DEFAULT_COOKIE_NAME, COOKIE_VALUE, CredentialValidationResult.INVALID_RESULT);
        doesNotNotifyContainerAboutLogin().removesCookie(DEFAULT_COOKIE_NAME);
        invokesNextInterceptor(ic, AuthenticationStatus.SUCCESS).withSecureRequest(true).generatesLoginToken().createsCookie(DEFAULT_COOKIE_NAME, COOKIE_VALUE, 86400, true, true);

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testInterceptValidateRequestWithValidRemembeMeCookieNotifiesContainerAboutLogin() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest", mechanismWithDefaultRememberMe);
        withCookies(DEFAULT_COOKIE_NAME, COOKIE_VALUE, validResult);
        notifiesContainerAboutLogin(validResult).doesNotInvokeNextInterceptor(ic).doesNotGenerateLoginToken().doesNotCreateCookie();

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void testSecureResponseNotIntercepted() throws Exception {
        doesNotIntercept("secureResponse", mechanismWithDefaultRememberMe);
    }

    @Test
    public void testInterceptCleanSubjectWithNoRemembeMeCookie() throws Exception {
        InvocationContext ic = createInvocationContext("cleanSubject", mechanismWithDefaultRememberMe);
        withCookies(null).doesNotRemoveLoginToken();

        interceptor.intercept(ic);
    }

    @Test
    public void testInterceptCleanSubjectWithRemembeMeCookie() throws Exception {
        InvocationContext ic = createInvocationContext("cleanSubject", mechanismWithDefaultRememberMe);
        Cookie[] cookies = createCookies(DEFAULT_COOKIE_NAME, COOKIE_VALUE);
        withCookies(cookies).removesCookie(DEFAULT_COOKIE_NAME).removesLoginToken(COOKIE_VALUE);

        interceptor.intercept(ic);
    }

    @Test
    public void testRememberMeELExpressions() throws Exception {
        final ELProcessor elProcessor = mockery.mock(ELProcessor.class);
        interceptor = new RememberMeInterceptor() {
            @Override
            protected ELProcessor getELProcessorWithAppModuleBeanManagerELResolver() {
                return elProcessor;
            }
        };

        final HttpAuthenticationMechanism mechanismWithELExpresssion = new TestHttpAuthenticationMechanismWithRememberMeELExpressions();
        final RememberMe rememberMe = mechanismWithELExpresssion.getClass().getAnnotation(RememberMe.class);

        final String isRememberMeExpression = rememberMe.isRememberMeExpression();
        final String cookieSecureOnlyExpression = rememberMe.cookieSecureOnlyExpression();

        mockery.checking(new Expectations() {
            {
                one(elProcessor).defineBean("httpMessageContext", httpMessageContext);
                one(elProcessor).defineBean("self", mechanismWithELExpresssion);
                one(elProcessor).eval(isRememberMeExpression.substring(2, isRememberMeExpression.length() - 1));
                will(returnValue(Boolean.TRUE));
                exactly(2).of(elProcessor).eval(cookieSecureOnlyExpression.substring(2, cookieSecureOnlyExpression.length() - 1));
                will(returnValue(Boolean.TRUE));
                one(elProcessor).eval(rememberMe.cookieMaxAgeSecondsExpression());
                will(returnValue(Integer.valueOf(600)));
                one(elProcessor).eval(rememberMe.cookieHttpOnlyExpression());
                will(returnValue(Boolean.TRUE));
            }
        });

        InvocationContext ic = createInvocationContext("validateRequest", mechanismWithELExpresssion);
        withCookies(null).doesNotNotifyContainerAboutLogin();
        invokesNextInterceptor(ic, AuthenticationStatus.SUCCESS).withSecureRequest(true).generatesLoginToken().createsCookie(DEFAULT_COOKIE_NAME, COOKIE_VALUE, 600, true, true);

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
    }

    private InvocationContext createInvocationContext(final String methodName, final HttpAuthenticationMechanism mechanism) throws Exception {
        final InvocationContext ic = mockery.mock(InvocationContext.class);
        final Method method = getMethod(methodName, mechanism.getClass());

        mockery.checking(new Expectations() {
            {
                allowing(ic).getMethod();
                will(returnValue(method));
                allowing(ic).getTarget();
                will(returnValue(mechanism));
            }
        });

        if ("validateRequest".equals(methodName) || "cleanSubject".equals(methodName)) {
            final Object[] parameters = new Object[3];
            parameters[0] = request;
            parameters[1] = response;
            parameters[2] = httpMessageContext;
            mockery.checking(new Expectations() {
                {
                    allowing(ic).getParameters();
                    will(returnValue(parameters));
                }
            });
        }

        return ic;
    }

    private Cookie[] createCookies(String cookieName, String value) {
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(cookieName, value);
        return cookies;
    }

    private void withCookies(String cookieName, String cookieValue, CredentialValidationResult result) {
        Cookie[] cookies = createCookies(cookieName, cookieValue);
        withCookies(cookies).validatesCookie(cookieValue, result);
    }

    private RememberMeInterceptorTest withCookies(final Cookie[] cookies) {
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(cookies));
            }
        });
        return this;
    }

    private RememberMeInterceptorTest validatesCookie(final String rememberMeCookeValue, final CredentialValidationResult result) {
        mockery.checking(new Expectations() {
            {
                one(rememberMeIdentityStore).validate(with(new Matcher<RememberMeCredential>() {

                    @Override
                    public void describeTo(Description description) {}

                    @Override
                    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {}

                    @Override
                    public boolean matches(Object param) {
                        boolean matches = false;
                        if (param instanceof RememberMeCredential) {
                            matches = rememberMeCookeValue.equals(((RememberMeCredential) param).getToken());
                        }
                        return matches;
                    }
                }));
                will(returnValue(result));
            }
        });
        return this;
    }

    private RememberMeInterceptorTest notifiesContainerAboutLogin(final CredentialValidationResult result) {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).notifyContainerAboutLogin(result.getCallerPrincipal(), result.getCallerGroups());
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private RememberMeInterceptorTest doesNotNotifyContainerAboutLogin() {
        mockery.checking(new Expectations() {
            {
                never(httpMessageContext).notifyContainerAboutLogin(with(aNonNull(CallerPrincipal.class)), with(any(Set.class)));
            }
        });
        return this;
    }

    private RememberMeInterceptorTest removesCookie(final String cookieName) {
        mockery.checking(new Expectations() {
            {
                one(response).isCommitted();
                will(returnValue(false));
            }
        });
        createsCookie(cookieName, "", 0, true, true);
        return this;
    }

    private RememberMeInterceptorTest withSecureRequest(final boolean secure) {
        mockery.checking(new Expectations() {
            {
                one(request).isSecure();
                will(returnValue(secure));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private RememberMeInterceptorTest generatesLoginToken() {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).getCallerPrincipal();
                will(returnValue(callerPrincipal));
                one(httpMessageContext).getGroups();
                will(returnValue(groups));
                one(rememberMeIdentityStore).generateLoginToken(callerPrincipal, groups);
                will(returnValue(COOKIE_VALUE));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private RememberMeInterceptorTest doesNotGenerateLoginToken() {
        mockery.checking(new Expectations() {
            {
                never(rememberMeIdentityStore).generateLoginToken(with(aNonNull(CallerPrincipal.class)), with(any(Set.class)));
            }
        });
        return this;
    }

    private RememberMeInterceptorTest createsCookie(final String cookieName, final String cookieValue, final int maxAge, final boolean secure, final boolean httpOnly) {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).getResponse();
                will(returnValue(response));
                one(response).addCookie(with(new Matcher<Cookie>() {

                    @Override
                    public void describeTo(Description description) {}

                    @Override
                    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {}

                    @Override
                    public boolean matches(Object param) {
                        boolean matches = false;
                        if (param instanceof Cookie) {
                            Cookie cookie = (Cookie) param;
                            matches = cookieName.equals(cookie.getName()) &&
                                      cookieValue.equals(cookie.getValue()) &&
                                      maxAge == cookie.getMaxAge() &&
                                      (cookie.getSecure() == secure) &&
                                      (cookie.isHttpOnly() == httpOnly);
                        }
                        return matches;
                    }
                }));
            }
        });
        return this;
    }

    private RememberMeInterceptorTest doesNotCreateCookie() {
        mockery.checking(new Expectations() {
            {
                never(response).addCookie(with(aNonNull(Cookie.class)));
            }
        });
        return this;
    }

    private RememberMeInterceptorTest withPrincipal(final Principal principal) {
        mockery.checking(new Expectations() {
            {
                one(request).getUserPrincipal();
                will(returnValue(principal));
            }
        });
        return this;
    }

    private RememberMeInterceptorTest invokesNextInterceptor(final InvocationContext invocationContext, final Object status) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(invocationContext).proceed();
                will(returnValue(status));
            }
        });
        return this;
    }

    private RememberMeInterceptorTest doesNotInvokeNextInterceptor(final InvocationContext invocationContext) throws Exception {
        mockery.checking(new Expectations() {
            {
                never(invocationContext).proceed();
            }
        });
        return this;
    }

    private Method getMethod(String methodName, Class<?> mechanism) throws Exception {
        return mechanism.getMethod(methodName, HttpServletRequest.class, HttpServletResponse.class,
                                   HttpMessageContext.class);
    }

    private void doesNotIntercept(String methodName, HttpAuthenticationMechanism mechanism) throws Exception {
        InvocationContext ic = createInvocationContext(methodName, mechanism);
        Object nextInterceptorReturn = Void.TYPE;
        invokesNextInterceptor(ic, nextInterceptorReturn);

        Object returnObject = interceptor.intercept(ic);

        assertEquals("The return Object must be as returned from the next interceptor.", nextInterceptorReturn, returnObject);
    }

    private RememberMeInterceptorTest removesLoginToken(final String cookieValue) {
        mockery.checking(new Expectations() {
            {
                one(rememberMeIdentityStore).removeLoginToken(cookieValue);
            }
        });
        return this;
    }

    private RememberMeInterceptorTest doesNotRemoveLoginToken() {
        mockery.checking(new Expectations() {
            {
                never(rememberMeIdentityStore).removeLoginToken(with(any(String.class)));
            }
        });
        return this;
    }

    @RememberMe
    private class TestHttpAuthenticationMechanismWithDefaultRememberMe implements HttpAuthenticationMechanism {
        @Override
        public AuthenticationStatus validateRequest(HttpServletRequest arg0, HttpServletResponse arg1, HttpMessageContext arg2) throws AuthenticationException {
            return null;
        }
    }

    @RememberMe(cookieSecureOnly = false, cookieHttpOnly = false)
    private class TestHttpAuthenticationMechanismWithRememberMeInsecure implements HttpAuthenticationMechanism {
        @Override
        public AuthenticationStatus validateRequest(HttpServletRequest arg0, HttpServletResponse arg1, HttpMessageContext arg2) throws AuthenticationException {
            return null;
        }
    }

    @RememberMe(cookieHttpOnlyExpression = "mybean.httpOnly", cookieMaxAgeSecondsExpression = "mybean.maxAge",
                cookieSecureOnlyExpression = "#{mybean.secureOnly}", isRememberMeExpression = "${mybean.rememberMe}")
    private class TestHttpAuthenticationMechanismWithRememberMeELExpressions implements HttpAuthenticationMechanism {
        @Override
        public AuthenticationStatus validateRequest(HttpServletRequest arg0, HttpServletResponse arg1, HttpMessageContext arg2) throws AuthenticationException {
            return null;
        }
    }

}
