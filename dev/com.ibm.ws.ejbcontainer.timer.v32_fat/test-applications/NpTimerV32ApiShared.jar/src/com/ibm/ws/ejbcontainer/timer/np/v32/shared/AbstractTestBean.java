/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.v32.shared;

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.junit.Assert;

/**
 * Abstract implementation for a session bean with methods to test the TimerService getAllTimers API.
 **/
public abstract class AbstractTestBean implements TestBean {
    private static final Logger logger = Logger.getLogger(AbstractTestBean.class.getName());

    @Resource
    protected SessionContext context;

    @Resource
    protected TimerService timerService;

    @EJB(beanName = "SingletonTimerBean")
    TestBean singletonTestBean;

    @Override
    public int getAllExpectedAutomaticTimerCount() {
        return singletonTestBean.getAllExpectedAutomaticTimerCount();
    }

    @Override
    public int adjustExpectedAutomaticTimerCount(int delta) {
        return singletonTestBean.adjustExpectedAutomaticTimerCount(delta);
    }

    @Override
    public void verifyGetAllTimers(int expected) {

        String module = (String) context.lookup("java:module/ModuleName");
        logger.info("   --> Calling getAllTimers() from module " + module);
        Collection<Timer> timers = timerService.getAllTimers();
        for (Timer timer : timers) {
            String timerInfo = (String) timer.getInfo();
            if (timerInfo == null || !timerInfo.contains(module)) {
                Assert.fail("getAllTimers found a timer for a different module; expected : " + module + ", found : " + timerInfo);
            }
        }
        Assert.assertEquals("getAllTimers() returned incorrect number of Timers", expected, timers.size());
    }

    @Override
    public void clearAllProgrammaticTimers() {
        Collection<Timer> timers = timerService.getAllTimers();
        for (Timer timer : timers) {
            try {
                String timerInfo = (String) timer.getInfo();
                if (!timerInfo.startsWith("Automatic :")) {
                    timer.cancel();
                }
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
            }
        }
    }
}
