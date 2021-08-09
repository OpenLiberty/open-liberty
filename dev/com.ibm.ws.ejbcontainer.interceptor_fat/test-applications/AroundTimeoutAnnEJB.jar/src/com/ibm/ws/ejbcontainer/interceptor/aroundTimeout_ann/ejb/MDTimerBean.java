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

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "destinationType",
                                                              propertyValue = "javax.jms.Queue"),
                                    @ActivationConfigProperty(propertyName = "acknowledgeMode",
                                                              propertyValue = "Auto-acknowledge"),
                                    @ActivationConfigProperty(propertyName = "destination",
                                                              propertyValue = "InterceptorMDBReqQueue")
})
@Interceptors(ATOInterceptor.class)
public class MDTimerBean implements MessageListener {
    private static final String CLASS_NAME = MDTimerBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static final String PERSISTENT_AUTO_TIMER_INFO = "MDTimerBean-Schedule-1";
    public static final String NON_PERSISTENT_AUTO_TIMER_INFO = "MDTimerBean-Schedule-2";

    private static CountDownLatch svTimerLatch;
    private static final CountDownLatch svAutoTimerLatch = new CountDownLatch(2);

    public static void setTimerLatch(CountDownLatch timerLatch) {
        svTimerLatch = timerLatch;
    }

    public static CountDownLatch getAutoTimerLatch() {
        return svAutoTimerLatch;
    }

    @Resource
    private TimerService ivTimerService;

    @AroundTimeout
    private Object aroundTimeout(InvocationContext c) throws Exception {
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

    @Timeout
    private void ejbTimeout(Timer t) {
        svLogger.info("--> Entered " + CLASS_NAME + ".ejbTimeout()");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);
            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".ejbTimeout:" + t.getInfo() + ":fired";
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            td.setIsFired(true);
            if (svTimerLatch != null) {
                svTimerLatch.countDown();
            }
        } finally {
            svLogger.info("--> Cancelling timer...");
            t.cancel();
            svLogger.info("<-- Exiting " + CLASS_NAME + ".ejbTimeout()");
        }
    }

    @Schedules({ @Schedule(hour = "*", minute = "*", second = "*", info = PERSISTENT_AUTO_TIMER_INFO),
                 @Schedule(hour = "*", minute = "*", second = "*", info = NON_PERSISTENT_AUTO_TIMER_INFO,
                           persistent = false) })
    private void ejbSchedule(Timer t) {
        svLogger.info("--> Entered " + CLASS_NAME + ".ejbSchedule()");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);
            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".ejbSchedule:" + t.getInfo() + ":fired";
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            td.setIsFired(true);
            svAutoTimerLatch.countDown();
        } finally {
            svLogger.info("--> Cancelling timer...");
            t.cancel();
            svLogger.info("<-- Exiting " + CLASS_NAME + ".ejbSchedule()");
        }
    }

    @Override
    public void onMessage(Message msg) {
        svLogger.info("--> Entered " + CLASS_NAME + ".onMessage");
        try {
            String rcvMsg = ((TextMessage) msg).getText();
            svLogger.info("    onMessage() text received: " + rcvMsg);

            boolean persistent = false;
            if (rcvMsg.endsWith("persistent")) {
                persistent = true;
            }

            svLogger.info("    creating single action timer...");
            TimerConfig config = new TimerConfig(rcvMsg, persistent);
            ivTimerService.createSingleActionTimer(5, config);
        } catch (Throwable ex) {
            // The test will fail because the timer is not created....
            // so eat the exception and log it for debug.
            ex.printStackTrace(System.out);
            svLogger.info("onMessage() failed: " + ex);
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".onMessage");
        }
    }
}
