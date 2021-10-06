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
package com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;

import javax.interceptor.InvocationContext;

//@Stateless
//@Interceptors(BasicInterceptor.class)
public class StatelessBean {

    public String name;
    protected String beanName = StatelessBean.class.getSimpleName();
    private boolean postConstructCalled;
    protected static boolean intPostConstructCalled;
    protected static boolean intAroundInvokeCalled;
    protected static boolean intPreDestroyCalled;
    protected static boolean intAroundConstructCalled;
    protected static boolean multiArgConstructCalled;
    protected static boolean injectIntoInterceptor;

    public StatelessBean() {}

    public String getBeanName() {
        return beanName;
    }

    //@PostConstruct
    public void postConstruct() {
        assertTrue(getBeanName() + "interceptor's postConstruct() not called.", intPostConstructCalled);
        postConstructCalled = true;
    }

    public void verifyPostConstruct() {
        assertTrue(getBeanName() + "postConstruct was not called.", postConstructCalled);
    }

    public void verifyInterceptorAroundInvoke() {
        // This is all that's needed because by calling this method itself, aroundInvoke is triggered.
        assertTrue(getBeanName() + "aroundInvoke not called.", intAroundInvokeCalled);
    }

    public void verifyInterceptorAroundConstruct() {
        assertTrue(getBeanName() + "aroundConstruct not called.", intAroundConstructCalled);
    }

    public void verifyInjectIntoInterceptor() {
        assertFalse(getBeanName() + " was injected into Interceptor with aroundConstruct.", injectIntoInterceptor);
    }

    //@AroundInvoke
    public Object aroundInvoke(InvocationContext ctx) throws Exception {
        System.out.println("AI1");
        assertFalse(StatelessBean.intAroundInvokeCalled);
        StatelessBean.intAroundInvokeCalled = true;
        Constructor<?> con = ctx.getConstructor();
        assertNull("InvocationContext.getConstructor() returned not null for AroundInvoke", con);
        Object proceed = null;
        
        try {
            proceed = ctx.proceed();
        } catch (Exception e) {
        }
        
        StatelessBean.intAroundInvokeCalled = false;
        return proceed;
    }
}
