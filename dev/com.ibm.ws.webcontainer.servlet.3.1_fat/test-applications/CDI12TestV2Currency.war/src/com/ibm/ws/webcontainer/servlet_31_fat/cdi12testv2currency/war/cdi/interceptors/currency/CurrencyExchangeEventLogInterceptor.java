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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2currency.war.cdi.interceptors.currency;

import java.io.Serializable;
import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Type used to log currency events.
 */
@CurrencyExchangeEvent
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class CurrencyExchangeEventLogInterceptor implements Serializable {
    //
    public static final String LOG_CLASS_NAME = "CurrencyExchangeEventLogInterceptor";

    //
    private static final long serialVersionUID = 1L;

    //

    public CurrencyExchangeEventLogInterceptor() {
        // EMPTY
    }

    //

    @Inject
    private CurrencyExchangeEventLog interceptorLogger;

    //

    @AroundInvoke
    public Object logCurrencyExchangeEvent(InvocationContext invocationContext) throws Exception {
        String logMethodName = "logCurrencyExchangeEvent";

        Method invocationMethod = invocationContext.getMethod();
        Object invocationTarget = invocationContext.getTarget();

        // @formatter:off
        String invocationText =
            "Method [ " + invocationMethod.getDeclaringClass().getName() + "." + invocationMethod.getName() + " ] " +
            " invoked on [ " + invocationTarget.getClass().getName() + " ]";
        // @formatter:on
        interceptorLogger.log(LOG_CLASS_NAME, logMethodName, invocationText);

        return invocationContext.proceed();
    }
}
