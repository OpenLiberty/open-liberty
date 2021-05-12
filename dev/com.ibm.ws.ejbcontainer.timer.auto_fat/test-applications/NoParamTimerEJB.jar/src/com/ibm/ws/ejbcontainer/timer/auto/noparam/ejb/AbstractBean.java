/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.noparam.ejb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

public abstract class AbstractBean implements Intf {
    private static final String CLASS_NAME = AbstractBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final Set<String> svScheduleBeans = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    public static CountDownLatch svScheduleLatch = new CountDownLatch(11); // 11 scheduled timers

    private CountDownLatch ivTimeoutLatch;

    @Resource(name = "beanName")
    String ivBeanName = getClass().getSimpleName();

    @Resource
    private TimerService ivTimerService;

    private boolean[] ivTimeoutExecuted = new boolean[2];
    private boolean[] ivScheduleExecuted = new boolean[2];

    @Override
    public CountDownLatch startTimer() {
        Arrays.fill(ivTimeoutExecuted, false);
        ivTimeoutLatch = new CountDownLatch(1);
        Timer timer = ivTimerService.createSingleActionTimer(0, new TimerConfig(null, false));
        svLogger.info(ivBeanName + ": Started Timer: " + timer);
        return ivTimeoutLatch;
    }

    @Override
    public void clearAllTimers() {
        Collection<Timer> timers = ivTimerService.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

    protected void setTimeoutExecuted(int index) {
        svLogger.info(ivBeanName + ": setTimeoutExecuted: " + index);
        ivTimeoutExecuted[index] = true;
        ivTimeoutLatch.countDown();
    }

    protected void setTimeoutExecuted(int index, Timer timer) {
        svLogger.info(ivBeanName + ": setTimeoutExecuted: " + index + ", " + timer);
        ivTimeoutExecuted[index] = timer != null;
        ivTimeoutLatch.countDown();
    }

    protected void setScheduleExecuted(int index) {
        if (!ivScheduleExecuted[index]) {
            svLogger.info(ivBeanName + ": setScheduleExecuted: " + index);
            ivScheduleExecuted[index] = true;
        }
        if (svScheduleBeans.add(ivBeanName)) {
            svScheduleLatch.countDown();
        }
    }

    protected void setScheduleExecuted(int index, Timer timer) {
        if (!ivScheduleExecuted[index]) {
            svLogger.info(ivBeanName + ": setScheduleExecuted: " + index + ", " + timer);
            ivScheduleExecuted[index] = timer != null;
        }
        if (svScheduleBeans.add(ivBeanName)) {
            svScheduleLatch.countDown();
        }
    }

    @Override
    public boolean isTimeoutExecuted(int index) {
        return ivTimeoutExecuted[index];
    }

    @Override
    public boolean isScheduleExecuted(int index) {
        return ivScheduleExecuted[index];
    }
}
