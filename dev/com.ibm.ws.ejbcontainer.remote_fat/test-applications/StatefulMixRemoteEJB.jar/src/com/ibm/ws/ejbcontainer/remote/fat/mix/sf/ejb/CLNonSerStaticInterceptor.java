/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class CLNonSerStaticInterceptor {
    private final static String CLASSNAME = CLNonSerStaticInterceptor.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static String staticStrValue = "Default";

    // This method sets the local string value to whatever value was passed in on the invocation
    @AroundInvoke
    private Object aroundInvoke(InvocationContext inv) throws Exception {
        Method m = inv.getMethod();
        svLogger.info("CLNonSerStaticInterceptor.aroundInvoke: " + m.getName());
        if (m.getName().equals("interceptorStaticStart")) {
            Object[] parms = inv.getParameters();
            if (parms.length > 0) {
                PassivationTracker.addMessage("CLNonSerStaticInterceptor.aroundInvoke:" + staticStrValue);
                staticStrValue = (String) parms[0];
            }
        } else if (m.getName().equals("interceptorStaticEnd")) {
            PassivationTracker.addMessage("CLNonSerStaticInterceptor.aroundInvoke:" + staticStrValue);
            staticStrValue = "Default"; // Reset the static value
        }

        Object rv = inv.proceed();
        return rv;
    }
}
