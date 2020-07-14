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

import java.util.logging.Logger;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

import com.ibm.ws.mb.interceptor.aroundconstructexception.ChainExceptionManagedBean.ChainExceptionTestType;

/**
 *
 */
public class ChainExceptionInterceptor2 {

    private static final String CLASS_NAME = ChainExceptionInterceptor2.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) throws ConstructorException, InterceptorException {
        try {
            ChainExceptionTestType testType = ChainExceptionManagedBean.getTestType();

            Object o = null;

            svLogger.info("Chain 2 before proceed");

            switch (testType) {
                case CHAIN1RECOVER:
                case CHAIN3THROWNEW:
                default:
                    inv.proceed();

                    // WELD calls all interceptors on recovery; does not resume where left off like WAS
                    // fail("Chain2 after .proceed() reached after exception was thrown up to Chain1 and it recovered");
                    break;
            }

            svLogger.info("Chain 2 after proceed");

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
