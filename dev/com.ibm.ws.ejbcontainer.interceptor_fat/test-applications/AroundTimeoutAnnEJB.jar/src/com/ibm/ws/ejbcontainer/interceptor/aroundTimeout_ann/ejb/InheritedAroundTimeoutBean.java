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
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Singleton()
@Local(LocalInterface.class)
public class InheritedAroundTimeoutBean extends SuperAroundTimeout {
    private static final String CLASS_NAME = InheritedAroundTimeoutBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

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

    @Timeout
    public void timeout(Timer t) {
        svLogger.info("--> Entered " + CLASS_NAME + ".timeout");
        try {
            svLogger.info("--> Timer t = " + t);
            String infoKey = (String) t.getInfo();
            svLogger.info("--> infoKey = " + infoKey);
            TimerData td = TimerData.svIntEventMap.get(infoKey);
            svLogger.info("--> svIntEventMap = " + TimerData.svIntEventMap);
            svLogger.info("--> td = " + td);

            String eventTag = "::" + this + ".timeout:" + t.getInfo();
            svLogger.info("--> eventTag = " + eventTag);
            TimerData.addIntEvent(t, eventTag);

            td.setIsFired(true);
            svTimerLatch.countDown();
        } finally {
            svLogger.info("<-- Exiting " + CLASS_NAME + ".timeout");
        }
    }
}
