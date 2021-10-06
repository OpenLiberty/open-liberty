/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.ejbinwarpackaging.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.junit.Assert;

/**
 * Interceptor class that implements postConstruct, aroundInvoke, and preDestroy.
 */
public class BasicInterceptor {
    static final String CLASS_NAME = BasicInterceptor.class.getName();

    private static Object proceed(InvocationContext ic) {
        try {
            return ic.proceed();
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @PostConstruct
    public void postConstruct(InvocationContext ic) {
        Assert.assertFalse(BasicInterceptorStatefulBean.getIntPostConstructCalled());
        BasicInterceptorStatefulBean.setIntPostConstructCalled(true);

        proceed(ic);

        BasicInterceptorStatefulBean.setIntPostConstructCalled(false);

    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ic) throws Exception {
        Assert.assertFalse(BasicInterceptorStatefulBean.getIntAroundInvokeCalled());
        BasicInterceptorStatefulBean.setIntAroundInvokeCalled(true);

        Object result = proceed(ic);

        BasicInterceptorStatefulBean.setIntAroundInvokeCalled(false);

        return result;
    }

    @PreDestroy
    public void preDestroy(InvocationContext ic) {
        Assert.assertFalse(BasicInterceptorStatefulBean.getIntPreDestroyCalled());
        BasicInterceptorStatefulBean.setIntPreDestroyCalled(true);

        proceed(ic);
    }
}
