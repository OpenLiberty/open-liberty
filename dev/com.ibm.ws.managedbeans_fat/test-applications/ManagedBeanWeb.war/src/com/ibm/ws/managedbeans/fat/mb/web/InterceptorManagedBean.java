/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.mb.web;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.interceptor.Interceptors;
import javax.transaction.UserTransaction;

/**
 * Simple Managed Bean that has been named, has simple injection,
 * and an AroundInvoke interceptor.
 **/
@ManagedBean("InterceptorManagedBean")
@Interceptors(MBInterceptor.class)
public class InterceptorManagedBean implements InterceptorAccess {
    private static final String CLASS_NAME = InterceptorManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static int svNextID = 1;

    private int ivID;
    private String ivValue = null;

    @Resource
    UserTransaction ivUserTran;

    private final List<String> ivPostConstructStack = new ArrayList<String>();

    public InterceptorManagedBean() {
        // do nothing since a wrapper will also subclass this
    }

    @PostConstruct
    public void initialize() {
        ivPostConstructStack.add(CLASS_NAME + ".initialize");

        // Use a unique id so it is easy to tell which instance is in use.
        synchronized (InterceptorManagedBean.class) {
            svLogger.info("-- web.InterceptorManagedBean.initialize" + svNextID);
            ivID = svNextID++;
        }
        if (ivUserTran != null) {
            ivValue = "InterceptorManagedBean.INITIAL_VALUE";
        } else {
            ivValue = "InterceptorManagedBean.NO_USER_TRAN";
        }
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
     * Returns the unique identifier of this instance.
     */
    public int getIdentifier() {
        svLogger.info("-- getIdentifier : " + this);
        return ivID;
    }

    /**
     * Returns the value.. to verify object is 'stateful'
     */
    public String getValue() {
        svLogger.info("-- getValue : " + this);
        return ivValue;
    }

    /**
     * Sets the value.. to verify object is 'stateful'
     */
    public void setValue(String value) {
        svLogger.info("-- setValue : " + ivValue + "->" + value + " : " + this);
        ivValue = value;
    }

    /**
     * Returns the injected UserTransaction.
     */
    public UserTransaction getUserTransaction() {
        return ivUserTran;
    }

    /**
     * Verifies that the interceptors were properly called per the
     * configuration of the bean.
     *
     * @param callStack list to be updated by interceptors
     **/
    public void verifyInterceptorCalls(List<String> callStack) {
        svLogger.info("> " + CLASS_NAME + ".verifyInterceptorCalls()");

        assertEquals("Unexpected PostConstruct interceptor calls : " + ivPostConstructStack,
                     2, ivPostConstructStack.size());

        String stackEntry = ivPostConstructStack.get(0);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     "MBInterceptor.postConstruct", stackEntry);

        stackEntry = ivPostConstructStack.get(1);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     CLASS_NAME + ".initialize", stackEntry);

        assertEquals("Unexpected AroundInvoke interceptor calls : " + callStack,
                     1, callStack.size());

        stackEntry = callStack.get(0);
        assertEquals("Unexpected AroundInvoke interceptor calls : " + stackEntry,
                     "MBInterceptor.aroundInvoke", stackEntry);

        svLogger.info("< " + CLASS_NAME + ".verifyInterceptorCalls()");
    }

    @Override
    public String toString() {
        return "web.InterceptorManagedBean(ID=" + ivID + "," + ivValue + ")";
    }

}
