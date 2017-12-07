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

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.el.ELProcessor;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.authentication.mechanism.http.RememberMe;
import javax.security.enterprise.credential.RememberMeCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.RememberMeIdentityStore;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.javaeesec.CDIHelper;
import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

@RememberMe
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 210)
public class RememberMeInterceptor {

    private static final TraceComponent tc = Tr.register(RememberMeInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        Method method = ic.getMethod();
        if ("validateRequest".equals(method.getName())) {
            return interceptValidateRequest(ic);
        } else if ("cleanSubject".equals(method.getName())) {
            return interceptCleanSubject(ic);
        } else {
            return ic.proceed();
        }
    }

    private AuthenticationStatus interceptValidateRequest(InvocationContext ic) throws Exception {
        AuthenticationStatus status = AuthenticationStatus.SUCCESS;
        HttpMessageContext httpMessageContext = getHttpMessageContext(ic);
        HttpServletRequest request = httpMessageContext.getRequest();
        RememberMeWrapper rememberMeWrapper = new RememberMeWrapper(ic);
        String rememberMeCookieValue = getRememberMeCookieValue(request, rememberMeWrapper.getCookieName());
        CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;
        RememberMeIdentityStore rememberMeIdentityStore = getRememberMeIdentityStore();

        if (rememberMeCookieValue != null) {
            RememberMeCredential credential = new RememberMeCredential(rememberMeCookieValue);
            result = rememberMeIdentityStore.validate(credential);

            if (CredentialValidationResult.Status.VALID.equals(result.getStatus())) {
                httpMessageContext.notifyContainerAboutLogin(result.getCallerPrincipal(), result.getCallerGroups());
            } else {
                removeCookie(request, httpMessageContext.getResponse(), rememberMeWrapper);
            }
        }

        if (CredentialValidationResult.Status.VALID.equals(result.getStatus()) == false) {
            status = (AuthenticationStatus) ic.proceed();

            if (AuthenticationStatus.SUCCESS.equals(status)) {
                if (rememberMeWrapper.isRememberMe()) {
                    if ((rememberMeWrapper.isSecure() && !request.isSecure()) == false) {
                        rememberMeCookieValue = rememberMeIdentityStore.generateLoginToken((CallerPrincipal) httpMessageContext.getCallerPrincipal(),
                                                                                           httpMessageContext.getGroups());
                        setRememberMeCookieInResponse(rememberMeCookieValue, httpMessageContext.getResponse(), rememberMeWrapper);
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "The remember me cookie will not be sent back because it must be sent using a secure protocol.");
                        }
                    }
                }
            }
        }
        return status;
    }

    private HttpMessageContext getHttpMessageContext(InvocationContext ic) {
        Object[] params = ic.getParameters();
        return (HttpMessageContext) params[2];
    }

    private Void interceptCleanSubject(InvocationContext ic) {
        HttpMessageContext httpMessageContext = getHttpMessageContext(ic);
        HttpServletRequest request = httpMessageContext.getRequest();
        RememberMeWrapper rememberMeWrapper = new RememberMeWrapper(ic);
        String rememberMeCookie = getRememberMeCookieValue(request, rememberMeWrapper.getCookieName());

        if (rememberMeCookie != null) {
            removeCookie(request, httpMessageContext.getResponse(), rememberMeWrapper);
            getRememberMeIdentityStore().removeLoginToken(rememberMeCookie);
        }
        return null;
    }

    @Sensitive
    private String getRememberMeCookieValue(HttpServletRequest request, String cookieName) {
        String rememberMeCookie = null;
        Cookie[] cookies = request.getCookies();
        String[] cookieValues = CookieHelper.getCookieValues(cookies, cookieName);
        if (cookieValues != null) {
            rememberMeCookie = cookieValues[0];
        }
        return rememberMeCookie;
    }

    private RememberMeIdentityStore getRememberMeIdentityStore() {
        return (RememberMeIdentityStore) CDIHelper.getBeanFromCurrentModule(RememberMeIdentityStore.class);
    }

    private void removeCookie(HttpServletRequest request, HttpServletResponse response, RememberMeWrapper rememberMeWrapper) {
        String cookieName = rememberMeWrapper.getCookieName();
        if (!response.isCommitted() && response instanceof IExtendedResponse) {
            ((IExtendedResponse) response).removeCookie(cookieName);
        }

        Cookie rememberMeInvalidationCookie = createCookie(rememberMeWrapper, "", 0);
        response.addCookie(rememberMeInvalidationCookie);
    }

    private void setRememberMeCookieInResponse(@Sensitive String rememberMeCookieValue, HttpServletResponse response, RememberMeWrapper rememberMeWrapper) {
        Cookie rememberMeCookie = createCookie(rememberMeWrapper, rememberMeCookieValue, rememberMeWrapper.getCookieMaxAgeInSeconds());
        response.addCookie(rememberMeCookie);
    }

    private Cookie createCookie(RememberMeWrapper rememberMeWrapper, @Sensitive String value, int maxAge) {
        Cookie cookie = new Cookie(rememberMeWrapper.getCookieName(), value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setSecure(rememberMeWrapper.isSecure());
        cookie.setHttpOnly(rememberMeWrapper.isHttpOnly());
        return cookie;
    }

    protected ELProcessor getELProcessorWithAppModuleBeanManagerELResolver() {
        return CDIHelper.getELProcessor();
    }

    private class RememberMeWrapper {

        private final RememberMe rememberMe;
        private final ELProcessor elProcessor;

        // Save only these two attributes since they are the only ones that are read repeatedly.
        private String cookieName = null;
        private Boolean isSecure = null;

        public RememberMeWrapper(InvocationContext ic) {
            rememberMe = getRememberMe(ic);
            elProcessor = getELProcessorIfNeeded(ic, rememberMe);
        }

        private RememberMe getRememberMe(InvocationContext ic) {
            return ic.getTarget().getClass().getAnnotation(RememberMe.class);
        }

        private ELProcessor getELProcessorIfNeeded(InvocationContext ic, RememberMe rememberMe) {
            ELProcessor elProcessor = null;
            if (isAnyELExpressionSet(rememberMe)) {
                elProcessor = getELProcessorWithAppModuleBeanManagerELResolver();
                elProcessor.defineBean("httpMessageContext", getHttpMessageContext(ic));
                elProcessor.defineBean("self", ic.getTarget());
            }
            return elProcessor;
        }

        private boolean isAnyELExpressionSet(RememberMe rememberMe) {
            return rememberMe.isRememberMeExpression().isEmpty() == false ||
                   rememberMe.cookieSecureOnlyExpression().isEmpty() == false ||
                   rememberMe.cookieMaxAgeSecondsExpression().isEmpty() == false ||
                   rememberMe.cookieHttpOnlyExpression().isEmpty() == false ||
                   rememberMe.cookieName().startsWith("${") ||
                   rememberMe.cookieName().startsWith("#{");
        }

        public int getCookieMaxAgeInSeconds() {
            String cookieMaxAgeSecondsExpression = rememberMe.cookieMaxAgeSecondsExpression();

            if (cookieMaxAgeSecondsExpression.isEmpty()) {
                return rememberMe.cookieMaxAgeSeconds();
            } else {
                return processExpression(elProcessor, cookieMaxAgeSecondsExpression);
            }
        }

        public String getCookieName() {
            if (cookieName == null) {
                cookieName = rememberMe.cookieName();

                if (cookieName.startsWith("${") || cookieName.startsWith("#{")) {
                    cookieName = processExpression(elProcessor, cookieName);
                }
            }
            return cookieName;
        }

        public boolean isHttpOnly() {
            String cookieHttpOnlyExpression = rememberMe.cookieHttpOnlyExpression();

            if (cookieHttpOnlyExpression.isEmpty()) {
                return rememberMe.cookieHttpOnly();
            } else {
                return processExpression(elProcessor, cookieHttpOnlyExpression);
            }
        }

        public boolean isSecure() {
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

        public boolean isRememberMe() {
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
