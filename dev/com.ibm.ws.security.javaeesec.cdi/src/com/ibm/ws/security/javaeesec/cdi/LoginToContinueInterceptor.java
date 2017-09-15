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
package com.ibm.ws.security.javaeesec.cdi;

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
@LoginToContinue
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 220)
public class LoginToContinueInterceptor {
    private static final TraceComponent tc = Tr.register(LoginToContinueInterceptor.class);
    private BeanManager beanManager = null;

    public LoginToContinueInterceptor() {
        beanManager = CDI.current().getBeanManager();
    }

    @AroundInvoke
    public Object processFormLogin(InvocationContext ic) throws Exception {
        Method method = ic.getMethod();
        if ("validateRequest".equals(method.getName())) {
        }
        Object result = ic.proceed();
        return result;
    }
}
