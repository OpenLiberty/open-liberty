/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb_int;

import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * Basic EJB Interceptor that appends to a call stack.
 **/
public class EJBInterceptor {
    private static final String CLASS_NAME = EJBInterceptor.class.getName();
    private static final String SIMPLE_NAME = EJBInterceptor.class.getSimpleName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @PostConstruct
    void postConstruct(InvocationContext inv) {
        svLogger.info("> " + SIMPLE_NAME + ".postConstruct()");

        InterceptorAccess bean = (InterceptorAccess) inv.getTarget();
        List<String> postConstructStack = bean.getPostConstructStack();
        postConstructStack.add(SIMPLE_NAME + ".postConstruct");

        try {
            inv.proceed();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new EJBException("unexpected Exception", e);
        }

        svLogger.info("< " + SIMPLE_NAME + ".postConstruct()");
    }

    @AroundInvoke
    Object aroundInvoke(InvocationContext inv) throws Exception {
        svLogger.info("> " + SIMPLE_NAME + ".aroundInvoke()");

        Object[] parms = inv.getParameters();

        @SuppressWarnings("unchecked")
        List<String> callStack = (List<String>) parms[0];

        callStack.add(SIMPLE_NAME + ".aroundInvoke");

        Object rv = inv.proceed();

        svLogger.info("< " + SIMPLE_NAME + ".aroundInvoke()");
        return rv;
    }
}
