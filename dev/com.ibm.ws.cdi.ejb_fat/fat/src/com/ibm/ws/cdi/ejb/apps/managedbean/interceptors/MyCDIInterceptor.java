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
package com.ibm.ws.cdi.ejb.apps.managedbean.interceptors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.ejb.apps.managedbean.CounterUtil;
import com.ibm.ws.cdi.ejb.apps.managedbean.MyEJBBeanLocal;

/**
 *
 */

@MyCDIInterceptorBinding
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class MyCDIInterceptor {

    @Inject
    MyEJBBeanLocal ejbBean;
    @Resource(name = "myStr")
    String myStr;

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        ejbBean.addToMsgList(this.getClass().getSimpleName() + ":AroundInvoke called" + " injectedStr:" + myStr);
        return context.proceed();
    }

    @AroundConstruct
    private Object construct(InvocationContext context) {
        CounterUtil.addToMsgList(this.getClass().getSimpleName() + ":AroundConstruct called" + " injectedStr:" + myStr);
        try {
            return context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    private void postConstruct(InvocationContext context) {
        CounterUtil.addToMsgList(this.getClass().getSimpleName() + ":PostConstruct called" + " injectedStr:" + myStr);
        try {
            context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void destroy(InvocationContext context) {
        System.out.println("@PreDestory called " + this.getClass().getSimpleName());

        try {
            context.proceed();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
