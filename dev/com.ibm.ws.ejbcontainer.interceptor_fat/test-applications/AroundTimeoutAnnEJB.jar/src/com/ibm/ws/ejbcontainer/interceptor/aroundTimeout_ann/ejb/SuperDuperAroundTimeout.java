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

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.ejb.Schedule;
import javax.ejb.Timer;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

public class SuperDuperAroundTimeout {
    private static final String CLASS_NAME = SuperDuperAroundTimeout.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final CountDownLatch svAutoTimerLatch = new CountDownLatch(1);

    public static final String SUPER_DUPER_AUTO_TIMER_INFO = "superDuperAutoTimer";

    public static CountDownLatch getSuperDuperAutoTimerLatch() {
        return svAutoTimerLatch;
    }

    @AroundTimeout
    public Object superDuperATO(InvocationContext invCtx) throws Exception {
        svLogger.info("--> Entered " + CLASS_NAME + ".superDuperATO");
        try {
            Timer t = (Timer) invCtx.getTimer();
            svLogger.info("--> Timer t = " + t);
            String eventTag = "::" + this + ".superDuperATO:" + invCtx.getMethod() + "," + t.getInfo();
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            return invCtx.proceed();
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".superDuperATO");
        }
    }

    @Interceptors({ ML1Interceptor.class, ML2Interceptor.class })
    @Schedule(second = "*", minute = "*", hour = "*", info = SUPER_DUPER_AUTO_TIMER_INFO, persistent = false)
    public void superDuperAutoTimerMethod(Timer t) {
        svLogger.info("--> Entered " + CLASS_NAME + ".superDuperAutoTimerMethod");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);

            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".superDuperAutoTimerMethod:" + t.getInfo();
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            td.setIsFired(true);
            svAutoTimerLatch.countDown();
        } finally {
            svLogger.info("--> Cancelling timer...");
            t.cancel();
            svLogger.info("<-- Exiting " + CLASS_NAME + ".autoTimeoutMethod");
        }
    }
}
