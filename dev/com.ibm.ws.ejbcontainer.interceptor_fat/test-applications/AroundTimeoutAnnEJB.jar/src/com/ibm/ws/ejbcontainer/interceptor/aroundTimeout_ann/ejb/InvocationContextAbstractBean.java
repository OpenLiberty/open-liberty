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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Resource;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;

public class InvocationContextAbstractBean {
    public static final String INFO = "InvocationContextTest";

    static CountDownLatch svTimerLatch;

    public static Object svAroundTimeoutTarget;
    public static Method svAroundTimeoutMethod;
    public static Object[] svAroundTimeoutParameters;
    public static Timer svAroundTimeoutTimer;
    public static Object svAroundTimeoutTimerInfo;

    @Resource
    private TimerService ivTimerService;

    public CountDownLatch createTimer() {
        svTimerLatch = new CountDownLatch(1);
        ivTimerService.createSingleActionTimer(0, new TimerConfig(INFO, false));
        return svTimerLatch;
    }

    @AroundTimeout
    private Object aroundTimeout(InvocationContext ic) throws Exception {
        svAroundTimeoutTarget = ic.getTarget();
        svAroundTimeoutMethod = ic.getMethod();
        svAroundTimeoutParameters = ic.getParameters();

        // Temporarily set then reset the parameters to ensure that getTimer
        // does not rely on the parameters being passed to the method.
        if (svAroundTimeoutParameters.length == 1) {
            ic.setParameters(new Object[1]);
        }

        svAroundTimeoutTimer = (Timer) ic.getTimer();
        ic.setParameters(svAroundTimeoutParameters);
        svAroundTimeoutTimerInfo = svAroundTimeoutTimer.getInfo();

        return ic.proceed();
    }
}
