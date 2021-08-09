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
import javax.ejb.Schedule;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.Interceptors;

@Interceptors({ CL1Interceptor.class, CL2Interceptor.class })
public class SuperTimerCallback extends SuperDuperTimerCallback {
    private static final String CLASS_NAME = SuperTimerCallback.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final CountDownLatch svAutoTimerLatch = new CountDownLatch(1);

    public static final String SUPER_AUTO_TIMER_INFO = "superTimerCallbackTimer";

    private static CountDownLatch svTimerLatch;

    public static CountDownLatch getSuperAutoTimerLatch() {
        return svAutoTimerLatch;
    }

    @Resource
    private TimerService ivTimerService;

    public CountDownLatch createSingleActionTimer(String info) {
        svTimerLatch = new CountDownLatch(1);
        TimerConfig config = new TimerConfig(info, false);
        Timer singleAction = ivTimerService.createSingleActionTimer(5, config);
        svLogger.info("Created single action timer with info + " + info + " : " + singleAction);

        return svTimerLatch;
    }

    @Timeout
    public void superTimeoutTimerCallbackMethod(Timer t) {
        svLogger.info("--> Entered " + CLASS_NAME + ".superTimeoutTimerCallbackMethod");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);
            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".superTimeoutTimerCallbackMethod:" + t.getInfo();
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            td.setIsFired(true);
            svTimerLatch.countDown();
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".superTimeoutTimerCallbackMethod");
        }
    }

    @Schedule(second = "*", minute = "*", hour = "*", info = SUPER_AUTO_TIMER_INFO, persistent = false)
    public void superTimerCallbackMethod(Timer t) {
        svLogger.info("--> Entered " + CLASS_NAME + ".superTimerCallbackMethod");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);

            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".superTimerCallbackMethod:" + t.getInfo();
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
