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
import java.util.Set;

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

    private RememberMeInterceptor interceptor;
    private HttpMessageContext httpMessageContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Principal principal;
    private CDIService cdiService;
    private CDIHelperTestWrapper cdiHelperTestWrapper;
    private RememberMeIdentityStore rememberMeIdentityStore;

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

        principal = null;

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
        InvocationContext ic = createInvocationContext("validateRequest", TestHttpAuthenticationMechanismWithDefaultRememberMe.class);
        withCookies(null).doesNotNotifyContainerAboutLogin().invokesNextInterceptor(ic, AuthenticationStatus.SEND_FAILURE).doesNotGenerateLoginToken().doesNotCreateCookie();

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SEND_FAILURE.", AuthenticationStatus.SEND_FAILURE, status);
    }

    @Test
    public void testInterceptValidateRequestWithInvalidRemembeMeCookieAndAuthenticationFailure() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest", TestHttpAuthenticationMechanismWithDefaultRememberMe.class);
        Cookie[] cookies = createCookies("JREMEMBERMEID", "123");
        withCookies(cookies).validatesCookie("123", CredentialValidationResult.INVALID_RESULT);
        doesNotNotifyContainerAboutLogin().removesCookie("JREMEMBERMEID");
        invokesNextInterceptor(ic, AuthenticationStatus.SEND_FAILURE).doesNotGenerateLoginToken().doesNotCreateCookie();

        AuthenticationStatus status = (AuthenticationStatus) interceptor.intercept(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SEND_FAILURE.", AuthenticationStatus.SEND_FAILURE, status);
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

    private Cookie[] createCookies(String cookieName, String value) {
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(cookieName, value);
        return cookies;
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
                one(httpMessageContext).getResponse();
                will(returnValue(response));
                one(response).isCommitted();
                will(returnValue(false));
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
                            matches = cookieName.equals(cookie.getName()) && "".equals(cookie.getValue()) && cookie.getMaxAge() == 0;
                        }
                        return matches;
                    }
                }));
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

    private RememberMeInterceptorTest doesNotCreateCookie() {
        mockery.checking(new Expectations() {
            {
                never(response).addCookie(with(aNonNull(Cookie.class)));
            }
        });
        return this;
    }

    //
//    @Test
//    public void testInterceptValidateRequestWithNoPrincipalAndFailure() throws Exception {
//        InvocationContext ic = createInvocationContext("validateRequest");
//        AuthenticationStatus nextInterceptorStatus = AuthenticationStatus.SEND_FAILURE;
//        withPrincipal(null).invokesNextInterceptor(ic, nextInterceptorStatus);
//
//        AuthenticationStatus status = interceptor.interceptValidateRequest(ic);
//
//        assertEquals("The AuthenticationStatus must be as returned from the next interceptor.", nextInterceptorStatus, status);
//    }
//
//    @Test
//    public void testInterceptValidateRequestWithPrincipal() throws Exception {
//        InvocationContext ic = createInvocationContext("validateRequest");
//        principal = new CallerPrincipal("user1");
//        withPrincipal(principal);
//
//        AuthenticationStatus status = interceptor.interceptValidateRequest(ic);
//
//        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
//    }
//
    @Test
    public void testSecureResponseNotIntercepted() throws Exception {
        doesNotIntercept("secureResponse", TestHttpAuthenticationMechanismWithDefaultRememberMe.class);
    }

//    @Test
//    public void testCleanSubjectNotIntercepted() throws Exception {
//        doesNotIntercept("cleanSubject");
//    }

    private InvocationContext createInvocationContext(final String methodName, Class<?> mechanism) throws Exception {
        final InvocationContext ic = mockery.mock(InvocationContext.class);
        final Method method = getMethod(methodName, mechanism);

        mockery.checking(new Expectations() {
            {
                allowing(ic).getMethod();
                will(returnValue(method));
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

    @RememberMe
    private class TestHttpAuthenticationMechanismWithDefaultRememberMe implements HttpAuthenticationMechanism {
        @Override
        public AuthenticationStatus validateRequest(HttpServletRequest arg0, HttpServletResponse arg1, HttpMessageContext arg2) throws AuthenticationException {
            // TODO Auto-generated method stub
            return null;
        }
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

    private Method getMethod(String methodName, Class<?> mechanism) throws Exception {
        return mechanism.getMethod(methodName, HttpServletRequest.class, HttpServletResponse.class,
                                   HttpMessageContext.class);
    }

    private void doesNotIntercept(String methodName, Class<?> mechanism) throws Exception {
        InvocationContext ic = createInvocationContext(methodName, mechanism);
        Object nextInterceptorReturn = Void.TYPE;
        invokesNextInterceptor(ic, nextInterceptorReturn);

        Object returnObject = interceptor.intercept(ic);

        assertEquals("The return Object must be as returned from the next interceptor.", nextInterceptorReturn, returnObject);
    }

}
