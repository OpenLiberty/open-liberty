/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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

import com.ibm.ws.cdi.ejb.apps.aroundconstruct.AroundConstructLogger;
import com.ibm.ws.cdi.ejb.utils.Intercepted;

@Interceptor
@Intercepted
@Priority(Interceptor.Priority.APPLICATION)
public class ConstructInterceptor {

    @Inject
    AroundConstructLogger logger;

    @AroundConstruct
    public Object intercept(InvocationContext context) throws Exception {
        logger.setConstructor(context.getConstructor());
        logger.addConstructorInterceptor(this.getClass());
        context.proceed();
        logger.setTarget(context.getTarget());
        return null;
    }
}
