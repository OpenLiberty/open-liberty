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

import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;

@Singleton()
@Local(LocalInterface.class)
public class InheritedTimerCallbackBean extends SuperTimerCallback {
    private static final String CLASS_NAME = InheritedTimerCallbackBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundTimeout
    Object aroundTimeout(InvocationContext c) throws Exception {
        svLogger.info("--> Entered " + CLASS_NAME + ".aroundTimeout");
        try {
            Timer t = (Timer) c.getTimer();
            svLogger.info("--> Timer t = " + t);
            String eventTag = "::" + this + ".aroundTimeout:" + c.getMethod() + "," + t.getInfo();

            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            return c.proceed();
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".aroundTimeout");
        }
    }
}
