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
import java.util.Map;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJBException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class Interceptor04 implements Serializable {

    private static final long serialVersionUID = -6129979656467552909L;

    private static final String CLASS_NAME = Interceptor04.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundInvoke
    public Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "Interceptor04.aroundInvoke", this);
        svLogger.info("Interceptor04.aroundInvoke: this=" + this);
        String targetStr = invCtx.getTarget().toString();
        String methodStr = invCtx.getMethod().toString();
        String parameterStr = Arrays.toString(invCtx.getParameters());
        svLogger.info("Interceptor04.aroundInvoke: getTarget=" + targetStr);
        svLogger.info("Interceptor04.aroundInvoke: getMethod=" + methodStr);
        svLogger.info("Interceptor04.aroundInvoke: getParameters=" + parameterStr);
        Map<String, Object> ctxData = invCtx.getContextData();
        ctxData.put("Target", targetStr);
        ctxData.put("Method", methodStr);
        ctxData.put("Parameters", parameterStr);
        return invCtx.proceed();
    }

    @PostConstruct
    private void postConstruct(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "Interceptor04.postConstruct", this);
        svLogger.info("Interceptor04.postConstruct: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }

    @PreDestroy
    protected void preDestroy(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "Interceptor04.preDestroy", this);
        svLogger.info("Interceptor04.preDestroy: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }
}
