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
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;

@Singleton
public class AroundInvokeBean {
    private static final Logger svLogger = Logger.getLogger(AroundInvokeBean.class.getName());

    private static CountDownLatch svTimerLatch;

    @Resource
    private TimerService ivTimerService;

    private boolean ivAroundInvokeCalled;
    private boolean ivAroundTimeoutCalled;

    public boolean isAroundInvokeCalled() {
        return ivAroundInvokeCalled;
    }

    public boolean isAroundTimeoutCalled() {
        return ivAroundTimeoutCalled;
    }

    public void reset() {
        ivAroundInvokeCalled = ivAroundTimeoutCalled = false;
    }

    public CountDownLatch createTimer() {
        svTimerLatch = new CountDownLatch(1);
        ivTimerService.createSingleActionTimer(0, new TimerConfig(null, false));
        return svTimerLatch;
    }

    @Timeout
    public void test() {
        svLogger.info("test method called");
        if (svTimerLatch != null) { // null if not called as timer
            svTimerLatch.countDown();
        }
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ic) throws Exception {
        svLogger.info("aroundInvoke: " + ic.getMethod());

        if (ic.getMethod().getName().equals("test")) {
            ivAroundInvokeCalled = true;
        }

        return ic.proceed();
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext ic) throws Exception {
        svLogger.info("aroundTimeout: " + ic.getMethod());

        ivAroundTimeoutCalled = true;
        return ic.proceed();
    }
}
