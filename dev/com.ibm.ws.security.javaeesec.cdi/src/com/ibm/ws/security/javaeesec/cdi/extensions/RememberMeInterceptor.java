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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.annotation.Priority;
import javax.el.ELProcessor;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.RememberMe;
import javax.security.enterprise.credential.RememberMeCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.RememberMeIdentityStore;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.javaeesec.CDIHelper;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

@RememberMe
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 210)
public class RememberMeInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        Method method = invocationContext.getMethod();
        if ("validateRequest".equals(method.getName())) {
            return new RememberMeWrapper(invocationContext).interceptValidateRequest();
        } else if ("cleanSubject".equals(method.getName())) {
            return new RememberMeWrapper(invocationContext).interceptCleanSubject();
        } else {
            return invocationContext.proceed();
        }
    }

    private RememberMeIdentityStore getRememberMeIdentityStore() {
        return (RememberMeIdentityStore) CDIHelper.getBeanFromCurrentModule(RememberMeIdentityStore.class);
    }

    protected ELProcessor getELProcessorWithAppModuleBeanManagerELResolver() {
        return CDIHelper.getELProcessor();
    }

    private class RememberMeWrapper {

        private final InvocationContext invocationContext;
        private final RememberMe rememberMe;
        private final ELProcessor elProcessor;
        private final HttpMessageContext httpMessageContext;

        private String cookieName = null;
        private Boolean isSecure = null;

        public RememberMeWrapper(InvocationContext invocationContext) {
            this.invocationContext = invocationContext;
            rememberMe = getRememberMe();
            httpMessageContext = getHttpMessageContext();
            elProcessor = getELProcessorIfNeeded();
            cookieName = getCookieName();
        }

        private RememberMe getRememberMe() {
            return invocationContext.getTarget().getClass().getAnnotation(RememberMe.class);
        }

        private HttpMessageContext getHttpMessageContext() {
            Object[] params = invocationContext.getParameters();
            return (HttpMessageContext) params[2];
        }

        private ELProcessor getELProcessorIfNeeded() {
            ELProcessor elProcessor = null;
            if (isAnyELExpressionSet()) {
                elProcessor = getELProcessorWithAppModuleBeanManagerELResolver();
                elProcessor.defineBean("httpMessageContext", httpMessageContext);
                elProcessor.defineBean("self", invocationContext.getTarget());
            }
            return elProcessor;
        }

        private boolean isAnyELExpressionSet() {
            return rememberMe.isRememberMeExpression().isEmpty() == false ||
                   rememberMe.cookieSecureOnlyExpression().isEmpty() == false ||
                   rememberMe.cookieMaxAgeSecondsExpression().isEmpty() == false ||
                   rememberMe.cookieHttpOnlyExpression().isEmpty() == false ||
                   rememberMe.cookieName().startsWith("${") ||
                   rememberMe.cookieName().startsWith("#{");
        }

        private String getCookieName() {
            String cookieName = rememberMe.cookieName();

            if (cookieName.startsWith("${") || cookieName.startsWith("#{")) {
                cookieName = processExpression(elProcessor, cookieName);
            }
            return cookieName;
        }

        public AuthenticationStatus interceptValidateRequest() throws Exception {
            AuthenticationStatus status = AuthenticationStatus.SUCCESS;
            HttpServletRequest request = httpMessageContext.getRequest();
            ProtectedString rememberMeCookieValue = getRememberMeCookieValue(request);
            CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;
            RememberMeIdentityStore rememberMeIdentityStore = getRememberMeIdentityStore();

            if (rememberMeCookieValue != null) {
                result = authenticateWithRememberMeCookie(request, rememberMeCookieValue, rememberMeIdentityStore);
            }

            if (CredentialValidationResult.Status.VALID.equals(result.getStatus()) == false) {
                status = authenticateAndRemember(rememberMeIdentityStore);
            }
            return status;
        }

        private CredentialValidationResult authenticateWithRememberMeCookie(HttpServletRequest request, ProtectedString rememberMeCookieValue,
                                                                            RememberMeIdentityStore rememberMeIdentityStore) {
            RememberMeCredential credential = new RememberMeCredential(new String(rememberMeCookieValue.getChars()));
            CredentialValidationResult result = rememberMeIdentityStore.validate(credential);

            if (CredentialValidationResult.Status.VALID.equals(result.getStatus())) {
                httpMessageContext.notifyContainerAboutLogin(result.getCallerPrincipal(), result.getCallerGroups());
            } else {
                removeCookie(request, httpMessageContext.getResponse());
            }
            return result;
        }

        private AuthenticationStatus authenticateAndRemember(RememberMeIdentityStore rememberMeIdentityStore) throws Exception {
            AuthenticationStatus status = (AuthenticationStatus) invocationContext.proceed();

            if (AuthenticationStatus.SUCCESS.equals(status)) {
                if (isRememberMe()) {
                    ProtectedString rememberMeCookieValue = new ProtectedString(rememberMeIdentityStore.generateLoginToken((CallerPrincipal) httpMessageContext.getCallerPrincipal(),
                                                                                                                           httpMessageContext.getGroups()).toCharArray());
                    setRememberMeCookieInResponse(rememberMeCookieValue, httpMessageContext.getResponse());
                }
            }
            return status;
        }

        public Void interceptCleanSubject() {
            HttpServletRequest request = httpMessageContext.getRequest();
            ProtectedString rememberMeCookie = getRememberMeCookieValue(request);

            if (rememberMeCookie != null) {
                removeCookie(request, httpMessageContext.getResponse());
                getRememberMeIdentityStore().removeLoginToken(new String(rememberMeCookie.getChars()));
            }
            return null;
        }

        private ProtectedString getRememberMeCookieValue(HttpServletRequest request) {
            ProtectedString rememberMeCookie = null;
            Cookie[] cookies = request.getCookies();
            String[] cookieValues = CookieHelper.getCookieValues(cookies, cookieName);
            if (cookieValues != null) {
                rememberMeCookie = new ProtectedString(cookieValues[0].toCharArray());
            }
            return rememberMeCookie;
        }

        private void removeCookie(HttpServletRequest request, HttpServletResponse response) {
            if (!response.isCommitted() && response instanceof IExtendedResponse) {
                ((IExtendedResponse) response).removeCookie(cookieName);
            }

            Cookie rememberMeInvalidationCookie = createRemovalCookie("", 0);
            response.addCookie(rememberMeInvalidationCookie);
        }

        // A ProtectedString is not needed for the removal cookie. Use a String for better performance.
        private Cookie createRemovalCookie(@Sensitive String value, int maxAge) {
            Cookie cookie = AccessController.doPrivileged(new PrivilegedAction<Cookie>() {

                @Override
                public Cookie run() {
                    return new Cookie(cookieName, value);
                }
            });
            setCommonCookieAttributes(maxAge, cookie);
            return cookie;
        }

        private Cookie createCookie(ProtectedString value, int maxAge) {
            Cookie cookie = AccessController.doPrivileged(new PrivilegedAction<Cookie>() {

                @Override
                public Cookie run() {
                    return new Cookie(cookieName, new String(value.getChars()));
                }
            });
            setCommonCookieAttributes(maxAge, cookie);
            return cookie;
        }

        private void setCommonCookieAttributes(int maxAge, Cookie cookie) {
            cookie.setMaxAge(maxAge);
            cookie.setPath("/");
            cookie.setSecure(isSecure());
            cookie.setHttpOnly(isHttpOnly());
        }

        private boolean isSecure() {
            if (isSecure == null) {
                String cookieSecureOnlyExpression = rememberMe.cookieSecureOnlyExpression();

                if (cookieSecureOnlyExpression.isEmpty()) {
                    isSecure = rememberMe.cookieSecureOnly();
                } else {
                    isSecure = processExpression(elProcessor, cookieSecureOnlyExpression);
                }
            }
            return isSecure;
        }

        private boolean isHttpOnly() {
            String cookieHttpOnlyExpression = rememberMe.cookieHttpOnlyExpression();

            if (cookieHttpOnlyExpression.isEmpty()) {
                return rememberMe.cookieHttpOnly();
            } else {
                return processExpression(elProcessor, cookieHttpOnlyExpression);
            }
        }

        private void setRememberMeCookieInResponse(ProtectedString rememberMeCookieValue, HttpServletResponse response) {
            Cookie rememberMeCookie = createCookie(rememberMeCookieValue, getCookieMaxAgeInSeconds());
            response.addCookie(rememberMeCookie);
        }

        private int getCookieMaxAgeInSeconds() {
            String cookieMaxAgeSecondsExpression = rememberMe.cookieMaxAgeSecondsExpression();

            if (cookieMaxAgeSecondsExpression.isEmpty()) {
                return rememberMe.cookieMaxAgeSeconds();
            } else {
                return processExpression(elProcessor, cookieMaxAgeSecondsExpression);
            }
        }

        private boolean isRememberMe() {
            if (httpMessageContext.isAuthenticationRequest()) {
                AuthenticationParameters authenticationParameters = httpMessageContext.getAuthParameters();
                if (authenticationParameters != null) {
                    return authenticationParameters.isRememberMe();
                }
            }

            String isRememberMeExpression = rememberMe.isRememberMeExpression();

            if (isRememberMeExpression.isEmpty()) {
                return rememberMe.isRememberMe();
            } else {
                return processExpression(elProcessor, isRememberMeExpression);
            }
        }

        @SuppressWarnings("unchecked")
        private <T> T processExpression(ELProcessor elProcessor, String expression) {
            return (T) elProcessor.eval(removeBrackets(expression));
        }

        private String removeBrackets(String expression) {
            if ((expression.startsWith("${") || expression.startsWith("#{")) && expression.endsWith("}")) {
                expression = expression.substring(2, expression.length() - 1);
            }
            return expression;
        }

    }

}
