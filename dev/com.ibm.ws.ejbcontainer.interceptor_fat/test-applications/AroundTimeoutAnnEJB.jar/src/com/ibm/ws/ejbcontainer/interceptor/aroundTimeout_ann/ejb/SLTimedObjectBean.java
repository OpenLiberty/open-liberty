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
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TimedObject;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

@Stateless()
@Local(LocalInterface.class)
@Interceptors(ATOInterceptor.class)
public class SLTimedObjectBean implements TimedObject {
    private static final Logger svLogger = Logger.getLogger(SLTimedObjectBean.class.getName());

    private static CountDownLatch svTimerLatch;

    @Resource
    private TimerService ivTimerService;

    public CountDownLatch createSingleActionTimer(String info) {
        svTimerLatch = new CountDownLatch(1);
        TimerConfig config = new TimerConfig(info, false);
        Timer singleAction = ivTimerService.createSingleActionTimer(5, config);
        svLogger.info("Created single action timer with info + " + info + " : " + singleAction);

        return svTimerLatch;
    }

    @AroundTimeout
    protected Object aroundTimeout(InvocationContext c) throws Exception {
        svLogger.info("--> Entered SLTimedObjectBean.aroundTimeout");
        try {
            Timer t = (Timer) c.getTimer();
            svLogger.info("--> Timer t = " + t);
            String eventTag = "::" + this + ".aroundTimeout:" + c.getMethod() + "," + t.getInfo();

            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            return c.proceed();
        } finally {
            svLogger.info("<-- Exiting SLTimedObjectBean.aroundTimeout");
        }
    }

    @Override
    public void ejbTimeout(Timer t) {
        svLogger.info("--> Entered SLTimedObjectBean.ejbTimeout()");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);
            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".ejbTimeout:" + t.getInfo();
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            td.setIsFired(true);
            svTimerLatch.countDown();
        } finally {
            svLogger.info("<-- Exiting SLTimedObjectBean.ejbTimeout()");
        }
    }
}
