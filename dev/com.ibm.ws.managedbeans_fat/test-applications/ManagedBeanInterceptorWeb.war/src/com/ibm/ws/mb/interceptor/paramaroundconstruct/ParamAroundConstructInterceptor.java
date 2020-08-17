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
package com.ibm.ws.mb.interceptor.paramaroundconstruct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

import com.ibm.ws.mb.interceptor.injection.InjectedManagedBean;

public class ParamAroundConstructInterceptor {

    private static final String CLASS_NAME = ParamAroundConstructInterceptor.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static enum ParamTestType {
        DEFAULT, GETCONSTRUCT, GETPARAMS, SETPARAMS
    };

    private static ParamTestType svTestType = ParamTestType.DEFAULT;

    public static void setTestType(ParamTestType pvTestType) {
        svTestType = pvTestType;
    }

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) {
        try {
            Object o;
            ParamAroundConstructManagedBean bean;

            switch (svTestType) {
                case SETPARAMS:
                    svLogger.info("ParamConstructor .setParamters() Test");

                    setParamsTest(inv, "before");

                    o = inv.proceed();

                    setParamsTest(inv, "after");

                    //Make sure .setParameters() after proceed doesn't change parameters bean was constructed with (checked in servlet)
                    InjectedManagedBean paramBean = new InjectedManagedBean();
                    paramBean.setID(-2);

                    Object[] parameters = new Object[1];
                    parameters[0] = paramBean;

                    inv.setParameters(parameters);

                    break;
                case GETPARAMS:
                    svLogger.info("ParamConstructor .getParamters() Test");

                    getParamsTest(inv, "before");

                    o = inv.proceed();

                    getParamsTest(inv, "after");

                    break;
                case GETCONSTRUCT:
                    svLogger.info("GetConstructor Test");

                    getConstructorTest(inv, "before");

                    o = inv.proceed();

                    getConstructorTest(inv, "after");

                    break;
                case DEFAULT:
                default:
                    //Test that AroundConstruct interceptor works
                    svLogger.info("Default Test");

                    //make sure .getTarget() is null before .proceed();
                    assertNull("InvocationContext.getTarget() is not null before InvocationContext.proceed()", inv.getTarget());

                    o = inv.proceed();

                    //make sure .getTarget() after proceed returns correct object
                    assertEquals("InvocationContext.getTarget() did not return correct object", ParamAroundConstructManagedBean.class, inv.getTarget().getClass());

                    break;
            }
            bean = (ParamAroundConstructManagedBean) inv.getTarget();
            bean.setAroundConstructCalled(true);

            return o;

        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }

    private void setParamsTest(InvocationContext inv, String beforeOrAfter) {

        //Create a bean to add to parameters
        InjectedManagedBean paramBean = new InjectedManagedBean();
        paramBean.setID(-1);

        assertNotSame("newly created bean is equal to .getParameters() bean", paramBean, inv.getParameters()[0]);

        Object[] parameters = new Object[1];
        parameters[0] = paramBean;

        inv.setParameters(parameters);

        //Make sure .setParameters works / Make sure .getParameters returns what was set in .setParameters
        assertEquals("InvocationContext.setParameters() " + beforeOrAfter + " proceed did not set to correct parameter", paramBean, inv.getParameters()[0]);

        //.setParameters with mismatched types should fail
        try {
            Object[] wrongTypeParameters = new Object[1];
            wrongTypeParameters[0] = new String("Illegal Type Parameter");

            inv.setParameters(wrongTypeParameters);
            fail("InvocationContext.setParameters() " + beforeOrAfter + " proceed with wrong type of constructor arguments did not fail");
        } catch (IllegalArgumentException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
        }

        //.setParameters with mismatched numOfArgs should fail
        try {
            Object[] wrongNumParameters = new Object[2];
            wrongNumParameters[0] = paramBean;
            wrongNumParameters[1] = new String("Illegal Type Parameter");

            inv.setParameters(wrongNumParameters);
            fail("InvocationContext.setParameters() " + beforeOrAfter + " proceed with wrong number of constructor arguments did not fail");
        } catch (IllegalArgumentException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
        }

        //.setParameters(null) shouldn't work
        try {
            inv.setParameters(null);
            fail("InvocationContext.setParameters(null) " + beforeOrAfter + " proceed did not fail");
        } catch (IllegalArgumentException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
        }

        /*
         * Modifying the parameter values does not affect the determination of the method or the
         * constructor that is invoked on the target class.
         *
         * So setParameters() with parameters that match an alternate constructor not invoked should still fail
         */
        try {
            Object[] differentConstructorParams = new Object[1];
            differentConstructorParams[0] = 1;
            inv.setParameters(differentConstructorParams);
            fail("InvocationContext.setParameters() " + beforeOrAfter + " proceed different constructor params did not fail");
        } catch (IllegalArgumentException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
        }

    }

    private void getParamsTest(InvocationContext inv, String beforeOrAfter) {
        assertNotNull("InvocationContext.getParemeters() " + beforeOrAfter + " proceed was null", inv.getParameters());
        assertEquals("InvocationContext.getParameters() " + beforeOrAfter + " proceed returned incorrect number of parameters", 1, inv.getParameters().length);
        assertEquals("InvocationContext.getParameters() " + beforeOrAfter + " proceed returned incorrect parameter type", InjectedManagedBean.class,
                     inv.getParameters()[0].getClass());
    }

    private void getConstructorTest(InvocationContext inv, String beforeOrAfter) throws Exception {
        assertNotNull("InvocationContext.getConstructor() " + beforeOrAfter + " proceed was null", inv.getConstructor());

        @SuppressWarnings("unchecked")
        Constructor<ParamAroundConstructManagedBean> constructor = (Constructor<ParamAroundConstructManagedBean>) inv.getConstructor();

        assertEquals("InvocationTarget.getConstructor() " + beforeOrAfter + " proceed did not return correct constructor",
                     ParamAroundConstructManagedBean.class.getConstructor(InjectedManagedBean.class), constructor);
    }

}
