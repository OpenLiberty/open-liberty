/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mb.interceptor.aroundconstruct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 *
 */
public class AroundConstructInterceptor {

    private static final String CLASS_NAME = AroundConstructInterceptor.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static enum TestType {
        DEFAULT, NOPROCEED, GETMETHOD, GETTIMER, GETCONSTRUCT, PARAMSTEST, POSTCONSTRUCT, AROUNDINVOKE, PREDESTROY
    }

    private static TestType svTestType = TestType.DEFAULT;

    public static void setTestType(TestType pvTestType) {
        svTestType = pvTestType;
    }

    private static final String CONTEXT_DATA_KEY = "Interceptors";

    private void updateContextData(InvocationContext inv, String interceptor) {
        Map<String, Object> contextData = inv.getContextData();
        String interceptors = (String) contextData.get(CONTEXT_DATA_KEY);
        if (interceptors == null) {
            contextData.put(CONTEXT_DATA_KEY, interceptor);
        } else {
            contextData.put(CONTEXT_DATA_KEY, interceptors + ", " + interceptor);
        }
    }

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) {
        try {
            Object o;
            AroundConstructManagedBean bean;
            updateContextData(inv, "AroundConstruct");

            switch (svTestType) {
                case POSTCONSTRUCT:
                case AROUNDINVOKE:
                case PREDESTROY:
                    return inv.proceed();
                case PARAMSTEST:
                    svLogger.info("ParamsTest Test");

                    DefaultConstructorParamsTest(inv, "before");

                    o = inv.proceed();

                    DefaultConstructorParamsTest(inv, "after");

                    break;
                case GETCONSTRUCT:
                    svLogger.info("GetConstructor Test");

                    GetConstructorTest(inv, "before");

                    o = inv.proceed();

                    GetConstructorTest(inv, "after");

                    break;
                case GETTIMER:
                    //InvocationContext.getTimer() should be null in @AroundConstruct

                    svLogger.info("GetTimer Test");

                    assertNull("InvocationContext.getTimer() before proceed is not null", inv.getTimer());

                    o = inv.proceed();

                    assertNull("InvocationContext.getTimer() after proceed is not null", inv.getTimer());

                    break;
                case GETMETHOD:
                    //InvocationContext.getMethod() should be null in @AroundConstruct

                    svLogger.info("GetMethod Test");

                    assertNull("InvocationContext.getMethod() before proceed is not null", inv.getMethod());

                    o = inv.proceed();

                    assertNull("InvocationContext.getMethod() after proceed is not null", inv.getMethod());

                    break;
                case NOPROCEED:
                    svLogger.info("NoProceed Test");
                    return null;
                case DEFAULT:
                default:
                    //Test that AroundConstruct interceptor works
                    svLogger.info("Default Test");

                    //make sure .getTarget() is null before .proceed();
                    assertNull("InvocationContext.getTarget() is not null before InvocationContext.proceed()", inv.getTarget());

                    o = inv.proceed();

                    //make sure .getTarget() returns correct object
                    Object target = inv.getTarget();
                    assertNotNull("InvocationContext.getTarget() after proceed did not return an object", target);
                    AroundConstructManagedBean.class.isAssignableFrom(target.getClass());
                    assertTrue("InvocationContext.getTarget() did not return correct object : " + target,
                               AroundConstructManagedBean.class.isAssignableFrom(target.getClass()));
                    break;
            }

            bean = (AroundConstructManagedBean) inv.getTarget();
            bean.setAroundConstructCalled();

            return o;

        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }

    private void DefaultConstructorParamsTest(InvocationContext inv, String beforeOrAfter) {
        //Make sure InvocationContext.getParameters() doesn't return null
        assertNotNull("InvocationContext.getParameters() " + beforeOrAfter + " proceed was null", inv.getParameters());

        //InvocationContext.getParameters() should return Object[] with 0 length because default constructor has no parameters
        assertEquals("InvocationContext.getParameters() " + beforeOrAfter + " proceed returned incorrect number of parameters", 0, inv.getParameters().length);

        /*
         * Modifying the parameter values does not affect the determination of the method or the
         * constructor that is invoked on the target class.
         *
         * So setParameters() with parameters that match an alternate constructor not invoked should still fail
         */
        try {

            Object[] array = new Object[1];
            array[0] = new String("IllegalParameter");

            inv.setParameters(array);
            fail("InvocationContext.setParameters() " + beforeOrAfter + " proceed in default constructor did not fail");
        } catch (IllegalArgumentException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
        }

        //Make sure .setParameters(null) and .setParameters(new Object[0]) work
        inv.setParameters(null);

        inv.setParameters(new Object[0]);
    }

    private void GetConstructorTest(InvocationContext inv, String beforeOrAfter) throws Exception {
        //Make sure InvocationContext.getConstructor() does not return null
        assertNotNull("InvocationContext.getConstructor() " + beforeOrAfter + " proceed was null", inv.getConstructor());

        @SuppressWarnings("unchecked")
        Constructor<AroundConstructManagedBean> constructor = (Constructor<AroundConstructManagedBean>) inv.getConstructor();

        //Make sure returned constructor matches the bean class constructor
        assertEquals("InvocationTarget.getConstructor() " + beforeOrAfter + " proceed did not return correct constructor", AroundConstructManagedBean.class.getConstructor(),
                     constructor);
    }

    @PostConstruct
    void postConstruct(InvocationContext inv) {
        updateContextData(inv, "PostConstruct");

        if (svTestType == TestType.POSTCONSTRUCT) {
            svLogger.info("PostConstructCalled");
            assertNull("InvocationContext.getConstructor() did not return null in @PostConstruct", inv.getConstructor());
            ((AroundConstructManagedBean) inv.getTarget()).setPostConstructCalled();
        }

        try {
            inv.proceed();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }

    @PreDestroy
    void preDestroy(InvocationContext inv) {

        updateContextData(inv, "PreDestroy");

        if (svTestType == TestType.PREDESTROY) {
            svLogger.info("PreDestroy Called");
            assertNull("InvocationContext.getConstructor() did not return null in @PreDestroy", inv.getConstructor());
            AroundConstructManagedBean.setPreDestroyCalled();
        }

        try {
            inv.proceed();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }

    @AroundInvoke
    Object aroundInvoke(InvocationContext inv) {

        updateContextData(inv, "AroundInvoke");

        if (svTestType == TestType.AROUNDINVOKE) {
            svLogger.info("AroundInvoke Called");
            assertNull("InvocationContext.getConstructor() did not return null in @AroundInvoke", inv.getConstructor());
            ((AroundConstructManagedBean) inv.getTarget()).setBusinessMethodCalled();
            String interceptors = (String) inv.getContextData().get(CONTEXT_DATA_KEY);
            assertEquals("InvocationContext.getContextData returned populated map:" + interceptors, "AroundInvoke", interceptors);
        }

        try {
            return inv.proceed();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }

}
