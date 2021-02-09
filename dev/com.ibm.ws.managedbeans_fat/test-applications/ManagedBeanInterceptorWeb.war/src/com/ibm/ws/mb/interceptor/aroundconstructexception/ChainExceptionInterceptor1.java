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

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

import com.ibm.ws.mb.interceptor.aroundconstructexception.ChainExceptionManagedBean.ChainExceptionTestType;

/**
 *
 */
public class ChainExceptionInterceptor1 {

    private static final String CLASS_NAME = ChainExceptionInterceptor1.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) throws ConstructorException, InterceptorException {
        try {
            ChainExceptionTestType testType = ChainExceptionManagedBean.getTestType();

            Object o = null;
            ChainExceptionManagedBean bean;

            svLogger.info("Chain 1 before proceed");

            switch (testType) {
                case CHAIN3THROWNEW:
                    ChainExceptionManagedBean.setThrowException(true);
                    try {
                        o = inv.proceed();
                        fail("Constructor did not throw exception");
                    } catch (ConstructorException e) {
                        fail("Chain1 was thrown a ConstructorException when it should have been an InterceptorException");
                    } catch (InterceptorException e) {
                        ChainExceptionManagedBean.setThrowException(false);
                        o = inv.proceed();
                    }
                    break;
                case CHAIN1RECOVER:
                default:
                    ChainExceptionManagedBean.setThrowException(true);
                    try {
                        o = inv.proceed();
                        fail("Constructor did not throw exception");
                    } catch (ConstructorException e) {
                        ChainExceptionManagedBean.setThrowException(false);
                        o = inv.proceed();
                    }
                    break;
            }

            svLogger.info("Chain 1 after proceed");

            bean = (ChainExceptionManagedBean) inv.getTarget();
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
