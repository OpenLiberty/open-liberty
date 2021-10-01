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

import static org.junit.Assert.assertTrue;

import javax.annotation.PostConstruct;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;

import com.ibm.ws.ejbcontainer.fat.beaninterfaceholderlib.BasicInterceptorLocal;

@Stateful
@Interceptors(BasicInterceptor.class)
public class BasicInterceptorStatefulBean implements BasicInterceptorLocal {

    private boolean postConstructCalled;

    // Checking interceptor's method
    private static boolean intPostConstructCalled;
    private static boolean intAroundInvokeCalled;
    private static boolean intPreDestroyCalled;

    /** {@inheritDoc} */
    @Override
    public String getSimpleBeanName() {
        return BasicInterceptorStatefulBean.class.getSimpleName();
    }

    @PostConstruct
    public void postConstruct() {
        assertTrue(getSimpleBeanName() + "interceptor's postConstruct() not called.", getIntPostConstructCalled());
        postConstructCalled = true;
    }

    /** {@inheritDoc} */
    @Override
    public void verifyPostConstruct() {
        assertTrue(getSimpleBeanName() + "postConstruct was not called.", postConstructCalled);
    }

    /** {@inheritDoc} */
    @Override
    public void verifyInterceptorAroundInvoke() {
        // This is all that's needed because by calling this method itself, aroundInvoke is triggered.
        assertTrue(getSimpleBeanName() + "aroundInvoke not called.", getIntAroundInvokeCalled());
    }

    /** {@inheritDoc} */
    @Remove
    @Override
    public void remove() {
        // Don't need to actually do anything, the bean should be destroyed after method exits.
    }

    /**
     * @return the intPostConstructCalled
     */
    public static boolean getIntPostConstructCalled() {
        return intPostConstructCalled;
    }

    /**
     * @param intPostConstructCalled the intPostConstructCalled to set
     */
    public static void setIntPostConstructCalled(boolean intPostConstructCalled) {
        BasicInterceptorStatefulBean.intPostConstructCalled = intPostConstructCalled;
    }

    /**
     * @return the intAroundInvokeCalled
     */
    public static boolean getIntAroundInvokeCalled() {
        return intAroundInvokeCalled;
    }

    /**
     * @param intAroundInvokeCalled the intAroundInvokeCalled to set
     */
    public static void setIntAroundInvokeCalled(boolean intAroundInvokeCalled) {
        BasicInterceptorStatefulBean.intAroundInvokeCalled = intAroundInvokeCalled;
    }

    /**
     * @return the intPreDestroyCalled
     */
    public static boolean getIntPreDestroyCalled() {
        return intPreDestroyCalled;
    }

    /**
     * @param intPreDestroyCalled the intPreDestroyCalled to set
     */
    public static void setIntPreDestroyCalled(boolean intPreDestroyCalled) {
        BasicInterceptorStatefulBean.intPreDestroyCalled = intPreDestroyCalled;
    }
}
