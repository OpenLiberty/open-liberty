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
package com.ibm.ws.mb.interceptor.chainaroundconstruct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.logging.Logger;

import javax.interceptor.AroundConstruct;
import javax.interceptor.InvocationContext;

import com.ibm.ws.mb.interceptor.injection.InjectedManagedBean;

/**
 *
 */
public class ChainInterceptor1 {

    private static final String CLASS_NAME = ChainInterceptor1.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) {
        try {
            svLogger.info("ChainInterceptor1 Called");

            //InvocationContext.setParameters() add a bean and see if it is returned in second interceptor
            InjectedManagedBean paramBean = new InjectedManagedBean();
            paramBean.setID(-1);

            Object[] parameters = new Object[1];
            parameters[0] = paramBean;

            inv.setParameters(parameters);

            //make sure .getTarget() is null before .proceed();
            assertNull("InvocationContext.getTarget() is not null before InvocationContext.proceed()", inv.getTarget());

            Object o = inv.proceed();

            //make sure .getTarget() after proceed returns correct object
            assertEquals("InvocationContext.getTarget() did not return correct object", inv.getTarget().getClass(), ChainManagedBean.class);

            return o;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("unexpected Exception", e);
        }
    }

}
