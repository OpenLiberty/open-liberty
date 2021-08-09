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
package com.ibm.ws.cdi.client.fat.counting.impl;

import static javax.interceptor.Interceptor.Priority.APPLICATION;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.client.fat.counting.CountBean;
import com.ibm.ws.cdi.client.fat.counting.Counted;

/**
 *
 */
@Interceptor
@Counted
@Priority(APPLICATION)
public class CountingInterceptor {

    @Inject
    private CountBean counter;

    @AroundInvoke
    public Object methodCalled(InvocationContext context) throws Exception {
        Object result;
        try {
            result = context.proceed();
        } finally {
            counter.add(1);
        }
        return result;
    }

}
