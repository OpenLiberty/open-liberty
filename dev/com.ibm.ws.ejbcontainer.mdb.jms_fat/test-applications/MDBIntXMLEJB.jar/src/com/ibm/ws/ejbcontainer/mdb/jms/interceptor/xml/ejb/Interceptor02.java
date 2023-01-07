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
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.xml.ejb;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;

public class Interceptor02 implements Serializable {
    private static final String LOGGER_CLASS_NAME = Interceptor02.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);

    private static final long serialVersionUID = 7354759478072013626L;

    // @AroundInvoke, defined in xml
    protected Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "Interceptor02.aroundInvoke", this);
        svLogger.info("Interceptor02.aroundInvoke: this=" + this);
        return invCtx.proceed();
    }

    // @PostConstruct, defined in xml
    void postConstruct(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "Interceptor02.postConstruct", this);
        svLogger.info("Interceptor02.postConstruct: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }

    // @PreDestroy, defined in xml
    public void preDestroy(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "Interceptor02.preDestroy", this);
        svLogger.info("Interceptor02.preDestroy: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }
}