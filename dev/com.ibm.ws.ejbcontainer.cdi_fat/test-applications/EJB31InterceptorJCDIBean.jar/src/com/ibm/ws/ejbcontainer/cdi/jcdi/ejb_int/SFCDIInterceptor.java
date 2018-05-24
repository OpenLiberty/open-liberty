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

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Basic CDI Interceptor for Stateful beans that appends to a call stack.
 **/
@Interceptor
@SFCDIInterceptorBinding
public class SFCDIInterceptor implements Serializable // Serializable for use with Stateful
{
    private static final long serialVersionUID = 5784358304223800161L;
    private static final String CLASS_NAME = SFCDIInterceptor.class.getName();
    private static final String SIMPLE_NAME = SFCDIInterceptor.class.getSimpleName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundInvoke
    public Object aroundInvoke(InvocationContext inv) throws Exception {
        svLogger.info("> " + SIMPLE_NAME + ".aroundInvoke()");

        Object[] parms = inv.getParameters();

        @SuppressWarnings("unchecked")
        List<String> callStack = (List<String>) parms[0];

        callStack.add(SIMPLE_NAME + ".aroundInvoke");

        Object rv = inv.proceed();

        svLogger.info("< " + SIMPLE_NAME + ".aroundInvoke()");
        return rv;
    }

    @PrePassivate
    void prePassivate(InvocationContext inv) {
        svLogger.info("> " + SIMPLE_NAME + ".prePassivate()");

        InterceptorAccess bean = (InterceptorAccess) inv.getTarget();
        List<String> prePassivateStack = bean.getPrePassivateStack();
        prePassivateStack.add(SIMPLE_NAME + ".prePassivate");

        try {
            inv.proceed();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new EJBException("unexpected Exception", e);
        }

        svLogger.info("< " + SIMPLE_NAME + ".prePassivate()");
    }

    @PostActivate
    void postActivate(InvocationContext inv) {
        svLogger.info("> " + SIMPLE_NAME + ".postActivate()");

        InterceptorAccess bean = (InterceptorAccess) inv.getTarget();
        List<String> postActivateStack = bean.getPostActivateStack();
        postActivateStack.add(SIMPLE_NAME + ".postActivate");

        try {
            inv.proceed();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new EJBException("unexpected Exception", e);
        }

        svLogger.info("< " + SIMPLE_NAME + ".postActivate()");
    }

    @PreDestroy
    void preDestroy(InvocationContext inv) {
        svLogger.info("> " + SIMPLE_NAME + ".preDestroy()");

        InterceptorAccess bean = (InterceptorAccess) inv.getTarget();
        List<String> preDestroyStack = bean.getPreDestroyStack();
        preDestroyStack.add(SIMPLE_NAME + ".preDestroy");

        try {
            inv.proceed();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new EJBException("unexpected Exception", e);
        }

        svLogger.info("< " + SIMPLE_NAME + ".preDestroy()");
    }

}
