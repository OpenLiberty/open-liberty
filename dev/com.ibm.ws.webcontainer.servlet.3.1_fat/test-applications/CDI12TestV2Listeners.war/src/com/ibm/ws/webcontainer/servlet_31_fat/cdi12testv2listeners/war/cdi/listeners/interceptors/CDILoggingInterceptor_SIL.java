/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.interceptors;

import java.io.Serializable;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log.ApplicationLog;

/**
 * Any method annotated with @CDILogInterceptorAnnotation_SIL would be intercepted by logMethodEntry
 */
@CDILogInterceptorAnnotation_SIL
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class CDILoggingInterceptor_SIL implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LOG_CLASS_NAME = "CDILoggingInterceptor_SIL";

    // Common application log ...
    @Inject
    ApplicationLog applicationLog;

    public CDILoggingInterceptor_SIL() {

    }

    @AroundInvoke
    public Object logMethodEntry(InvocationContext ctx) throws Exception {
        String methodName = "logMethodEntry";
        applicationLog.log(LOG_CLASS_NAME, methodName, "Entering method: [ " + ctx.getMethod().getName() + " ] from Class: [ " + ctx.getMethod().getDeclaringClass().getName()
                                                       + " ]");
        return ctx.proceed();
    }
}
