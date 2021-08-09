/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors;

import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.ejb.apps.aroundconstruct.AroundConstructLogger;

@Interceptor
@InterceptorOneBinding
public class InterceptorOne {

    @Inject
    AroundConstructLogger logger;

    @AroundConstruct
    public Object intercept(InvocationContext context) throws Exception {
        logger.addConstructorInterceptor(this.getClass());
        return context.proceed();
    }
}
