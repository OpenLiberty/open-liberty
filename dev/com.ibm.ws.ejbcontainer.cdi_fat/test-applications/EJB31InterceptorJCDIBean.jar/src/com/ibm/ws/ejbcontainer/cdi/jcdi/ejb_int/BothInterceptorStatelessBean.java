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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

/**
 * Stateless bean that has an EJB interceptor, and a CDI interceptor.
 **/
@Stateless(name = "BothInterceptorStateless")
@Local(InterceptorLocal.class)
@Remote(InterceptorRemote.class)
@Interceptors(EJBInterceptor.class)
@CDIInterceptorBinding
public class BothInterceptorStatelessBean implements InterceptorAccess {
    private static final String CLASS_NAME = BothInterceptorStatelessBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "BothInterceptorStateless";

    @Resource(name = "EJBContext")
    protected SessionContext ivContext;

    private final List<String> ivPostConstructStack = new ArrayList<String>();

    @PostConstruct
    void initialize() {
        ivPostConstructStack.add(CLASS_NAME + ".initialize");
    }

    /**
     * Returns the current PostConstruct call stack, so an interceptor may
     * add itself to the stack.
     **/
    @Override
    public List<String> getPostConstructStack() {
        return ivPostConstructStack;
    }

    /**
     * Returns the current PreDestroy call stack, so an interceptor may
     * add itself to the stack.
     **/
    @Override
    public List<String> getPreDestroyStack() {
        return null; // not supported for this stateless bean
    }

    /**
     * Returns the current PostActivate call stack, so an interceptor may
     * add itself to the stack.
     **/
    @Override
    public List<String> getPostActivateStack() {
        return null; // not supported for stateless beans
    }

    /**
     * Returns the current PrePassivate call stack, so an interceptor may
     * add itself to the stack.
     **/
    @Override
    public List<String> getPrePassivateStack() {
        return null; // not supported for stateless beans
    }

    /**
     * Verifies that the interceptors were properly called per the
     * configuration of the bean.
     *
     * @param callStack list to be updated by interceptors
     **/
    public void verifyInterceptorCalls(List<String> callStack) {
        svLogger.info("> " + ivEJBName + ".verifyInterceptorCalls()");

        assertEquals("Unexpected PostConstruct interceptor calls : " + ivPostConstructStack,
                     3, ivPostConstructStack.size());

        String stackEntry = ivPostConstructStack.get(0);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     "EJBInterceptor.postConstruct", stackEntry);

        stackEntry = ivPostConstructStack.get(1);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     "CDIInterceptor.postConstruct", stackEntry);

        stackEntry = ivPostConstructStack.get(2);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     CLASS_NAME + ".initialize", stackEntry);

        assertEquals("Unexpected AroundInvoke interceptor calls : " + callStack,
                     2, callStack.size());

        stackEntry = callStack.get(0);
        assertEquals("Unexpected AroundInvoke interceptor calls : " + stackEntry,
                     "EJBInterceptor.aroundInvoke", stackEntry);

        stackEntry = callStack.get(1);
        assertEquals("Unexpected AroundInvoke interceptor calls : " + stackEntry,
                     "CDIInterceptor.aroundInvoke", stackEntry);

        svLogger.info("< " + ivEJBName + ".verifyInterceptorCalls()");
    }
}
