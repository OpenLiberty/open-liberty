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

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class BaseCLInterceptor {
    private final static String CLASSNAME = BaseCLInterceptor.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public String strValue = "Default";

    @PostActivate
    private void postActivate(InvocationContext inv) {
        PassivationTracker.addMessage("BaseCLInterceptor.postActivate:" + strValue);
        svLogger.info("BaseCLInterceptor.postActivate: " + strValue);
        try {
            inv.proceed();
        } catch (Exception e) {
            svLogger.info("prePassivate inv.proceed threw Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PrePassivate
    private void prePassivate(InvocationContext inv) {
        PassivationTracker.addMessage("BaseCLInterceptor.prePassivate:" + strValue);
        svLogger.info("BaseCLInterceptor.prePassivate: " + strValue);
        try {
            inv.proceed();
        } catch (Exception e) {
            svLogger.info("prePassivate inv.proceed threw Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // This method sets the local string value to whatever value was passed in on the invocation
    @AroundInvoke
    private Object aroundInvoke(InvocationContext inv) throws Exception {
        Method m = inv.getMethod();
        svLogger.info("BaseCLInterceptor.aroundInvoke: " + m.getName());
        if (m.getName().equals("interceptorStart")) {
            Object[] parms = inv.getParameters();
            if (parms.length > 0) {
                strValue = (String) parms[0];
                PassivationTracker.addMessage("BaseCLInterceptor.aroundInvoke:" + strValue);
            }
        } else if (m.getName().equals("interceptorEnd")) {
            PassivationTracker.addMessage("BaseCLInterceptor.aroundInvoke:" + strValue);
        }

        Object rv = inv.proceed();
        return rv;
    }
}
