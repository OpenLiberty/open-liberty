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

import static com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb.ATAppExInterface.AUTO_TIMER_INFO;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

public class AroundTimeoutAppExBean extends SuperAroundTimeout {
    private static final String CLASS_NAME = AroundTimeoutAppExBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static CountDownLatch svTimerLatch;
    private static final CountDownLatch svAutoTimerLatch = new CountDownLatch(1);

    @Resource
    private TimerService ivTimerService;

    public CountDownLatch createSingleActionTimer(String info) {
        svTimerLatch = new CountDownLatch(1);
        TimerConfig config = new TimerConfig(info, false);
        Timer singleAction = ivTimerService.createSingleActionTimer(5, config);
        svLogger.info("Created single action timer with info + " + info + " : " + singleAction);

        return svTimerLatch;
    }

    public CountDownLatch getAutoTimerLatch() {
        return svAutoTimerLatch;
    }

    @SuppressWarnings("unused")
    private Object aroundTimeout(InvocationContext c) throws MyException {
        svLogger.info("--> Entered " + CLASS_NAME + ".aroundTimeout");
        try {
            Timer t = (Timer) c.getTimer();
            svLogger.info("--> Timer t = " + t);
            String eventTag = "::" + this + ".aroundTimeout:" + c.getMethod() + "," + t.getInfo();

            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            Object o = null;
            try {
                o = c.proceed();
            } catch (Exception e) {
                throw new MyException();
            }

            return o;
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".aroundTimeout");
        }
    }

    @Interceptors({ AppExceptionInterceptor.class })
    public void timeoutMethod(Timer t) {
        svLogger.info("--> Entered " + CLASS_NAME + ".timeoutMethod");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);
            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".timeoutMethod:" + t.getInfo();
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            td.setIsFired(true);
            svTimerLatch.countDown();
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".timeoutMethod");
        }
    }

    @Schedule(second = "*", minute = "*", hour = "*", info = AUTO_TIMER_INFO, persistent = false)
    public void autoTimeoutMethod(Timer t) {
        svLogger.info("--> Entered " + CLASS_NAME + ".autoTimeoutMethod");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);

            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".autoTimeoutMethod:" + t.getInfo();
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
