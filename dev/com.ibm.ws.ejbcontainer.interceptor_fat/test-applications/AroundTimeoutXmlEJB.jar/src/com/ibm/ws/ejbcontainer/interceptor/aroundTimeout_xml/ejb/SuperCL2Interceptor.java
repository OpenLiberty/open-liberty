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
package com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_xml.ejb;

import java.util.logging.Logger;

import javax.ejb.Timer;
import javax.interceptor.InvocationContext;

public class SuperCL2Interceptor {
    private static final String CLASS_NAME = SuperCL2Interceptor.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public Object superAroundTimeout(InvocationContext invCtx) throws Exception {
        svLogger.info("--> Entered " + CLASS_NAME + ".superAroundTimeout");
        try {
            Timer t = (Timer) invCtx.getTimer();
            svLogger.info("--> Timer t = " + t);
            String eventTag = "::" + this + ".superAroundTimeout:" + invCtx.getMethod() + "," + t.getInfo();
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            return invCtx.proceed();
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".superAroundTimeout");
        }
    }
}
