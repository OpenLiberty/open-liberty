/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Timer;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class InvocationContextSFInterceptor {
    private static final Logger svLogger = Logger.getLogger(InvocationContextSFInterceptor.class.getName());

    public enum InterceptorType {
        POST_CONSTRUCT,
        PRE_DESTROY,
        POST_ACTIVATE,
        PRE_PASSIVATE,
        AROUND_INVOKE,
    }

    public static Set<InterceptorType> svFired;
    public static Map<InterceptorType, Timer> svTimers;

    public static void reset() {
        svFired = EnumSet.noneOf(InterceptorType.class);
        svTimers = new EnumMap<InterceptorType, Timer>(InterceptorType.class);
    }

    private void intercept(InterceptorType type, InvocationContext ic) {
        svLogger.info(type + ": " + ic.getTimer());
        svFired.add(type);
        svTimers.put(type, (Timer) ic.getTimer());
    }

    @PostConstruct
    private void postConstruct(InvocationContext ic) {
        intercept(InterceptorType.POST_CONSTRUCT, ic);
    }

    @PreDestroy
    private void preDestroy(InvocationContext ic) {
        intercept(InterceptorType.PRE_DESTROY, ic);
    }

    @PrePassivate
    private void prePassivate(InvocationContext ic) {
        intercept(InterceptorType.PRE_PASSIVATE, ic);
    }

    @PostActivate
    private void postActivate(InvocationContext ic) {
        intercept(InterceptorType.POST_ACTIVATE, ic);
    }

    @AroundInvoke
    private Object aroundInvoke(InvocationContext ic) throws Exception {
        intercept(InterceptorType.AROUND_INVOKE, ic);
        return ic.proceed();
    }
}
