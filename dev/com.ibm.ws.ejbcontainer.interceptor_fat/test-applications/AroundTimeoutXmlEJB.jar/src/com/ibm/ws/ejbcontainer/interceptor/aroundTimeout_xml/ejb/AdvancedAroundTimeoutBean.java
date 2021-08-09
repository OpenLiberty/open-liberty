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

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.InvocationContext;

public class AdvancedAroundTimeoutBean extends SuperAdvancedAroundTimeout {
    private static final String CLASS_NAME = AdvancedAroundTimeoutBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static CountDownLatch svTimerLatch;
    private static final CountDownLatch svAutoTimerLatch = new CountDownLatch(1);

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
