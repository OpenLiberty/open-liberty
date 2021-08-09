/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Loggable @Interceptor
public class LoggableInterceptor {

    private static String getLoggerName(Object target) {
        String name = "UNKNONW";
        if (target != null) {
            Class<?> clazz = target.getClass();
            if (Proxy.isProxyClass(clazz)) {
                name = "Proxy for " + target.toString();
            } else {
                name = clazz.getName();
            }
        }
        return name;
    }

    private static String format(String sourceClass, String sourceMethod, String msg, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceClass).append(" ");
        sb.append(sourceMethod).append(" ");
        sb.append(msg);
        if (params != null) {
            for (Object o : params) {
                sb.append(" ").append(o);
            }
        }
        return sb.toString();
    }

    @AroundInvoke
    public Object logInvocation(InvocationContext ctx) throws Exception {
        Logger logger = Logger.getLogger(getLoggerName(ctx.getTarget()));
        Method m = ctx.getMethod();

        String sourceClass = m.getDeclaringClass().getName();
        String sourceMethod = m.getName();
        logger.logp(Level.INFO, sourceClass, sourceMethod,
                    format(sourceClass, sourceMethod, "Entering", ctx.getParameters()));

        Object returnVal = null;
        try {
            returnVal = ctx.proceed();
        } catch (Exception ex) {
            logger.logp(Level.INFO, sourceClass, sourceMethod,
                        format(sourceClass, sourceMethod, "Throwing", ex), ex);
        } finally {
            logger.logp(Level.INFO, sourceClass, sourceMethod,
                        format(sourceClass, sourceMethod, "Exiting", returnVal), returnVal);
        }
        return returnVal;
    }
}
