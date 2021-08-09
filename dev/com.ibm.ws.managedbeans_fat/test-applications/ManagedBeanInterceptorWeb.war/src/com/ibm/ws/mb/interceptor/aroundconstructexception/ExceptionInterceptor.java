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
package com.ibm.ws.mb.interceptor.aroundconstructexception;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

import com.ibm.ws.mb.interceptor.injection.InjectedManagedBean;

/**
 *
 */
public class ExceptionInterceptor {

    private static final String CLASS_NAME = ExceptionInterceptor.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static enum ExceptionTestType {
        RECOVER, THROW, CATCHANDRETHROW, THROWNEW, CONSTRUCTTHENTHROW
    };

    private static ExceptionTestType svTestType = ExceptionTestType.RECOVER;

    public static void setTestType(ExceptionTestType pvTestType) {
        svTestType = pvTestType;
    }

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) throws ConstructorException, InterceptorException {
        try {
            Object o = null;
            ExceptionManagedBean bean;

            switch (svTestType) {
                case CONSTRUCTTHENTHROW:
                    ExceptionManagedBean.setThrowException(false);
                    o = inv.proceed();

                    throw new InterceptorException();
                case THROWNEW:
                    try {
                        ExceptionManagedBean.setThrowException(true);
                        o = inv.proceed();
                        fail("InvocationContext.proceed() did not return with expected exception from constructor");
                    } catch (ConstructorException e) {
                        throw new InterceptorException();
                    }
                    break;
                case CATCHANDRETHROW:
                    try {
                        ExceptionManagedBean.setThrowException(true);
                        o = inv.proceed();
                        fail("InvocationContext.proceed() did not return with expected exception from constructor");
                    } catch (ConstructorException e) {
                        throw e;
                    }
                    break;
                case THROW:

                    ExceptionManagedBean.setThrowException(true);
                    o = inv.proceed();
                    fail("InvocationContext.proceed() did not return with expected exception from constructor");

                    break;
                case RECOVER:
                default:
                    /*
                     * Make sure that even though constructor throws exception first time, catching it and
                     * re-calling proceed will try to create the bean again
                     */
                    try {
                        /*
                         * Make sure even though first proceed fails, bean created in .setParameters() before proceed is still
                         * passed to the constructor
                         */
                        InjectedManagedBean injection = new InjectedManagedBean();
                        injection.setID(-1);

                        Object[] parameters = new Object[1];
                        parameters[0] = injection;

                        inv.setParameters(parameters);

                        ExceptionManagedBean.setThrowException(true);
                        o = inv.proceed();
                        fail("InvocationContext.proceed() did not return with expected exception from constructor");
                    } catch (ConstructorException e) {
                        svLogger.log(Level.INFO, "Expected Exception", e);

                        //make sure .getTarget() is null after first .proceed() failed
                        assertNull("InvocationContext.getTarget() is not null after first .proceed() failed", inv.getTarget());

                        ExceptionManagedBean.setThrowException(false);
                        o = inv.proceed();
                    }
                    break;
            }

            bean = (ExceptionManagedBean) inv.getTarget();
            bean.setAroundConstructCalled(true);

            return o;

        } catch (ConstructorException ce) {
            throw ce;
        } catch (InterceptorException ie) {
            throw ie;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }

}
