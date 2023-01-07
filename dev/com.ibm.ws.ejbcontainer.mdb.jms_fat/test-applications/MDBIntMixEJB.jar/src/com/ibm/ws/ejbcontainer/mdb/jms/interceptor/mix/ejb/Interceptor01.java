/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.mix.ejb;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class Interceptor01 implements Serializable {
    private static final String LOGGER_CLASS_NAME = Interceptor01.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);

    private static final long serialVersionUID = 9027385894354125019L;

    @Resource(name = "int1Injection")
    private Integer injectedEnvVar = 99;
    @Resource(name = "ctxInjection")
    private MessageDrivenContext injContext;

    @AroundInvoke
    private Object aroundInvoke(InvocationContext invCtx) throws Exception {
        Object injInteger = injContext.lookup("int1Injection");
        CheckInvocation.getInstance().recordInjectionInfo("InjectInterceptor", "Interceptor01 = " + injInteger, this);
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "Interceptor01.aroundInvoke", this);
        svLogger.info("Interceptor01.aroundInvoke: this=" + this);
        return invCtx.proceed();
    }

    @PostConstruct
    protected void postConstruct(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "Interceptor01.postConstruct", this);
        svLogger.info("Interceptor01.postConstruct: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }

    @PreDestroy
    void preDestroy(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "Interceptor01.preDestroy", this);
        svLogger.info("Interceptor01.preDestroy: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }
}