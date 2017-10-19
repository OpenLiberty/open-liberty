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
        RememberMe rememberMe = getRememberMe(ic);
        HttpMessageContext httpMessageContext = getHttpMessageContext(ic);
        HttpServletRequest request = httpMessageContext.getRequest();
        String rememberMeCookieValue = getRememberMeCookieValue(request, rememberMe.cookieName());
        CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;
        RememberMeIdentityStore rememberMeIdentityStore = getRememberMeIdentityStore();
        ELProcessor elProcessor = getELProcessorIfNeeded(ic, rememberMe);

        if (rememberMeCookieValue != null) {
            RememberMeCredential credential = new RememberMeCredential(rememberMeCookieValue);
            result = rememberMeIdentityStore.validate(credential);

            if (CredentialValidationResult.Status.VALID.equals(result.getStatus())) {
                httpMessageContext.notifyContainerAboutLogin(result.getCallerPrincipal(), result.getCallerGroups());
            } else {
                removeCookie(request, httpMessageContext.getResponse(), rememberMe, elProcessor);
            }
        }

        if (CredentialValidationResult.Status.VALID.equals(result.getStatus()) == false) {
            status = (AuthenticationStatus) ic.proceed();

            if (AuthenticationStatus.SUCCESS.equals(status)) {
                if (isRememberMe(rememberMe, elProcessor)) {
                    if ((isSecure(rememberMe, elProcessor) && !request.isSecure()) == false) {
                        rememberMeCookieValue = rememberMeIdentityStore.generateLoginToken((CallerPrincipal) httpMessageContext.getCallerPrincipal(),
                                                                                           httpMessageContext.getGroups());
                        setRememberMeCookieInResponse(rememberMeCookieValue, httpMessageContext.getResponse(), rememberMe, elProcessor);
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

    private RememberMe getRememberMe(InvocationContext ic) {
        return ic.getTarget().getClass().getAnnotation(RememberMe.class);
    }

    private HttpMessageContext getHttpMessageContext(InvocationContext ic) {
        Object[] params = ic.getParameters();
        return (HttpMessageContext) params[2];
    }

    private Void interceptCleanSubject(InvocationContext ic) {
        RememberMe rememberMe = getRememberMe(ic);
        HttpMessageContext httpMessageContext = getHttpMessageContext(ic);
        HttpServletRequest request = httpMessageContext.getRequest();
        String rememberMeCookie = getRememberMeCookieValue(request, rememberMe.cookieName());

        if (rememberMeCookie != null) {
            removeCookie(request, httpMessageContext.getResponse(), rememberMe, getELProcessorIfNeeded(ic, rememberMe));
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

    private void removeCookie(HttpServletRequest request, HttpServletResponse response, RememberMe rememberMe, ELProcessor elProcessor) {
        String cookieName = rememberMe.cookieName();
        if (!response.isCommitted() && response instanceof IExtendedResponse) {
            ((IExtendedResponse) response).removeCookie(cookieName);
        }

        Cookie rememberMeInvalidationCookie = createCookie(rememberMe, elProcessor, "", 0);
        response.addCookie(rememberMeInvalidationCookie);
    }

    private boolean isRememberMe(RememberMe rememberMe, ELProcessor elProcessor) {
        String isRememberMeExpression = rememberMe.isRememberMeExpression();

        if (isRememberMeExpression.isEmpty()) {
            return rememberMe.isRememberMe();
        } else {
            return (Boolean) elProcessor.eval(isRememberMeExpression);
        }
    }

    private void setRememberMeCookieInResponse(@Sensitive String rememberMeCookieValue, HttpServletResponse response, RememberMe rememberMe, ELProcessor elProcessor) {
        Cookie rememberMeCookie = createCookie(rememberMe, elProcessor, rememberMeCookieValue, getCookieMaxAgeInSeconds(rememberMe, elProcessor));
        response.addCookie(rememberMeCookie);
    }

    private int getCookieMaxAgeInSeconds(RememberMe rememberMe, ELProcessor elProcessor) {
        String cookieMaxAgeSecondsExpression = rememberMe.cookieMaxAgeSecondsExpression();

        if (cookieMaxAgeSecondsExpression.isEmpty()) {
            return rememberMe.cookieMaxAgeSeconds();
        } else {
            return (Integer) elProcessor.eval(cookieMaxAgeSecondsExpression);
        }
    }

    private Cookie createCookie(RememberMe rememberMe, ELProcessor elProcessor, @Sensitive String value, int maxAge) {
        Cookie cookie = new Cookie(rememberMe.cookieName(), value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setSecure(isSecure(rememberMe, elProcessor));
        cookie.setHttpOnly(isHttpOnly(rememberMe, elProcessor));
        return cookie;
    }

    private boolean isSecure(RememberMe rememberMe, ELProcessor elProcessor) {
        String cookieSecureOnlyExpression = rememberMe.cookieSecureOnlyExpression();

        if (cookieSecureOnlyExpression.isEmpty()) {
            return rememberMe.cookieSecureOnly();
        } else {
            return (Boolean) elProcessor.eval(cookieSecureOnlyExpression);
        }
    }

    private boolean isHttpOnly(RememberMe rememberMe, ELProcessor elProcessor) {
        String cookieHttpOnlyExpression = rememberMe.cookieHttpOnlyExpression();

        if (cookieHttpOnlyExpression.isEmpty()) {
            return rememberMe.cookieHttpOnly();
        } else {
            return (Boolean) elProcessor.eval(cookieHttpOnlyExpression);
        }
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
        return !(rememberMe.isRememberMeExpression().isEmpty() &&
                 rememberMe.cookieSecureOnlyExpression().isEmpty() &&
                 rememberMe.cookieMaxAgeSecondsExpression().isEmpty() &&
                 rememberMe.cookieHttpOnlyExpression().isEmpty());
    }

    protected ELProcessor getELProcessorWithAppModuleBeanManagerELResolver() {
        return CDIHelper.getELProcessor();
    }

}
