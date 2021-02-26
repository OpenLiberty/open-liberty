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

public class ChainInterceptor3 {

    private static final String CLASS_NAME = ChainInterceptor3.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundConstruct
    Object AroundConstruct(InvocationContext inv) {
        try {
            svLogger.info("ChainInterceptor3 Called");

            InjectedManagedBean bean = (InjectedManagedBean) inv.getParameters()[0];

            //Make sure InvocationContext.getParameters() in third interceptor returns what was set in first interceptor
            assertEquals(".getParameters() in Chain3 did not contain bean set in .setParameters() in Chain2", bean.getID(), -2);

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
