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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.interceptor.InvocationContext;

import junit.framework.Assert;

public class BasicInterceptor {

    //@PostConstruct
    public void postConstruct(InvocationContext ctx) throws Exception {
        StatelessBean.intPostConstructCalled = true;
        Map<String, Object> map = ctx.getContextData();
        Iterator<?> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> pairs = (Entry<?, ?>) it.next();
            Assert.assertFalse("ContextData added in AroundConstruct exists during PostConstruct.", pairs.getKey().equals("propertyac"));
        }
        assertNull("InvocationContext.getConstructor returned not null in PostConstruct.", ctx.getConstructor());
        ctx.proceed();
        StatelessBean.intPostConstructCalled = false;
    }

    /*
     * Before Proceed:
     * getTarget: null
     * getMethod: null
     * getTimer: null
     * getConstructor: Constructor<?>
     * getParameters: Object[]
     *
     * proceed(): null
     *
     * After Proceed:
     * getTarget: Object (StatelessBean, eg.)
     * getMethod: null
     * getTimer: null
     * getConstructor: Constructor<?>
     * getParameters: Object[]
     */
    //@AroundConstruct
    public Object aroundConstruct(InvocationContext ctx) throws Exception {
        StatelessBean.intAroundConstructCalled = true;
        ctx.getContextData().put("propertyac", "ac1");
        try {
            assertEquals(StatelessBean.class, ctx.getConstructor().getDeclaringClass());
            assertEquals(Collections.emptyList(), Arrays.asList(ctx.getParameters()));
            assertNull("InvocationContext.getMethod returned not null for AroundConstruct.", ctx.getMethod());
            assertNull("InvocationContext.getTimer returned not null for AroundConstruct.", ctx.getTimer());
            assertNull("InvocationContext.getTarget returned not null before calling proceed.", ctx.getTarget());

            Object o = ctx.proceed();

            assertNull("InvocationContext.proceed returned not null for AroundConstruct.", o);
            assertEquals(StatelessBean.class, ctx.getConstructor().getDeclaringClass());
            assertEquals(Collections.emptyList(), Arrays.asList(ctx.getParameters()));
            assertNull("InvocationContext.getMethod returned not null for AroundConstruct.", ctx.getMethod());
            assertNull("InvocationContext.getTimer returned not null for AroundConstruct.", ctx.getTimer());
            assertEquals("InvocationContext.getTarget should return StatelessBean, but returned " + ctx.getTarget().getClass() + ".", StatelessBean.class,
                         ctx.getTarget().getClass());
            return o;
        } catch (Exception e) {
            return null;
        }

    }

}
