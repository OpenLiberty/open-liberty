/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors;

import static com.ibm.ws.cdi.ejb.utils.Utils.id;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.ejb.apps.aroundconstruct.StatelessAroundConstructLogger;
import com.ibm.ws.cdi.ejb.utils.StatelessIntercepted;

@Interceptor
@StatelessIntercepted
@Priority(Interceptor.Priority.APPLICATION)
public class StatelessConstructInterceptor {

    @Inject
    StatelessAroundConstructLogger statelessLogger;

    @AroundConstruct
    public Object intercept(InvocationContext context) throws Exception {
        Class<?> declaringClass = context.getConstructor().getDeclaringClass();

        statelessLogger.setInterceptedBean(declaringClass);
        context.proceed();
        return null;
    }
}
