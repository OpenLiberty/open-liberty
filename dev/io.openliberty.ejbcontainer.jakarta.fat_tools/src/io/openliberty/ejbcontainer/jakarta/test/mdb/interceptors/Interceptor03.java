/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.mdb.interceptors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJBException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class Interceptor03 implements Serializable {

    private static final String CLASS_NAME = Interceptor03.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final long serialVersionUID = 5375172574787612684L;

    @AroundInvoke
    Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "Interceptor03.aroundInvoke", this);
        svLogger.info("Interceptor03.aroundInvoke: this=" + this);
        for (Iterator<Entry<String, Object>> it = invCtx.getContextData().entrySet().iterator(); it.hasNext();) {
            Entry<String, Object> entry = it.next();
            svLogger.info("Interceptor03.aroundInvoke: ctxData.key=" + entry.getKey() + "; ctxData.value=" + (String) entry.getValue());
            CheckInvocation.getInstance().recordCallInfo(entry.getKey(), (String) entry.getValue(), this);
        }
        String targetStr = invCtx.getTarget().toString();
        String methodStr = invCtx.getMethod().toString();
        String parameterStr = Arrays.toString(invCtx.getParameters());
        svLogger.info("Interceptor03.aroundInvoke: getTarget=" + targetStr);
        svLogger.info("Interceptor03.aroundInvoke: getMethod=" + methodStr);
        svLogger.info("Interceptor03.aroundInvoke: getParameters=" + parameterStr);
        CheckInvocation.getInstance().recordCallInfo("Target", invCtx.getTarget().toString(), this);
        CheckInvocation.getInstance().recordCallInfo("Method", invCtx.getMethod().toString(), this);
        CheckInvocation.getInstance().recordCallInfo("Parameters", Arrays.toString(invCtx.getParameters()), this);
        return invCtx.proceed();
    }

    @PostConstruct
    public void postConstruct(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "Interceptor03.postConstruct", this);
        svLogger.info("Interceptor03.postConstruct: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }

    @PreDestroy
    private void preDestroy(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "Interceptor03.preDestroy", this);
        svLogger.info("Interceptor03.preDestroy: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }
}
