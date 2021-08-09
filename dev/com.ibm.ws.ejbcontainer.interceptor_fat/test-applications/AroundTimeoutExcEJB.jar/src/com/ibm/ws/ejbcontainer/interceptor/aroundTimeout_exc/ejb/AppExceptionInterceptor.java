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
package com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb;

import java.util.logging.Logger;

import javax.ejb.Timer;
import javax.interceptor.InvocationContext;

public class AppExceptionInterceptor {
    private static final String CLASS_NAME = AppExceptionInterceptor.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public Object aroundTimeout(InvocationContext invCtx) throws MyException {
        svLogger.info("--> Entered " + CLASS_NAME + ".aroundTimeout");
        try {
            Timer t = (Timer) invCtx.getTimer();
            svLogger.info("--> Timer t = " + t);
            String eventTag = "::" + this + ".aroundTimeout:" + invCtx.getMethod() + "," + t.getInfo();
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            Object o = null;
            try {
                o = invCtx.proceed();
            } catch (Exception e) {
                throw new MyException();
            }

            return o;
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".aroundTimeout");
        }
    }
}
