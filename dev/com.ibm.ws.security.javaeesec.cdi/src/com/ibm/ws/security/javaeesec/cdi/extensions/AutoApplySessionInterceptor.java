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
import java.security.Principal;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@AutoApplySession
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class AutoApplySessionInterceptor {

    @SuppressWarnings("unchecked")
    @AroundInvoke
    public Object interceptValidateRequest(InvocationContext ic) throws Exception {
        AuthenticationStatus status;
        Method method = ic.getMethod();
        if ("validateRequest".equals(method.getName())) {
            Object[] params = ic.getParameters();
            HttpMessageContext httpMessageContext = (HttpMessageContext) params[2];
            HttpServletRequest request = httpMessageContext.getRequest();
            Principal principal = request.getUserPrincipal();
            if (principal == null) {
                status = (AuthenticationStatus) ic.proceed();
                if (AuthenticationStatus.SUCCESS.equals(status)) {
                    httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.registerSession", Boolean.TRUE.toString());
                }
                return status;
            } else {
                Callback[] callbacks = new Callback[1];
                callbacks[0] = new CallerPrincipalCallback(httpMessageContext.getClientSubject(), principal);
                httpMessageContext.getHandler().handle(callbacks);
                return AuthenticationStatus.SUCCESS;
            }
        } else {
            status = (AuthenticationStatus) ic.proceed();
            return status;
        }
    }

}
