/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.ann.ejb;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

/**
 * Bean implementation class for Enterprise Bean: BasicStatelessLocal
 **/
@Stateless
@Local({ BasicStatelessLocal.class, BasicStatelessLocal2.class })
@Asynchronous
public class BasicStatelessLocalBean {
    public final static String CLASSNAME = BasicStatelessLocalBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static volatile boolean asyncWorkDone = false;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static volatile long beanThreadId = 0;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static final CountDownLatch svBeanLatch = new CountDownLatch(1);

    @Resource
    private SessionContext ivContext;

    public void test_fireAndForget() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndForget");
        }

        // save threadId value to static variable for verification method executed on different thread
        BasicStatelessLocalBean.beanThreadId = Thread.currentThread().getId();

        svLogger.info("threadId: " + BasicStatelessLocalBean.beanThreadId);

        // set static variable for work completed to true
        BasicStatelessLocalBean.asyncWorkDone = true;

        svBeanLatch.countDown();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndForget");
        }

        return;
    }

    @AroundInvoke
    private Object aroundInvoke(InvocationContext ic) throws Exception {
        String method = ic.getMethod().getName();
        Object[] parameters = ic.getParameters();
        if ("test_interceptor".equals(method) && parameters.length == 1) {
            @SuppressWarnings("unchecked")
            List<String> value = (List<String>) parameters[0];
            value.add("bean");
        }
        return ic.proceed();
    }

    @Interceptors(BasicStatelessLocalInterceptor.class)
    public Future<Void> test_interceptor(List<String> value) {
        value.add("method");
        return null;
    }

    public Future<Class<?>> test_getInvokedBusinessInterface() {
        return new AsyncResult<Class<?>>(ivContext.getInvokedBusinessInterface());
    }
}