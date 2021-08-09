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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.interceptor.InvocationContext;

public class OverrideBase implements OverrideIntf {
    private final String CLASS_NAME = getClass().getName();
    private final Logger ivLogger = Logger.getLogger(CLASS_NAME);

    private static CountDownLatch svTimerLatch;

    @Resource
    private TimerService ivTimerService;

    private boolean ivAroundTimeout;
    private boolean ivParentAroundTimeout;

    @Override
    public CountDownLatch createTimer() {
        ivAroundTimeout = false;
        ivParentAroundTimeout = false;
        svTimerLatch = new CountDownLatch(1);
        ivTimerService.createSingleActionTimer(0, new TimerConfig(null, false));
        return svTimerLatch;
    }

    @Timeout
    private void timeout() {
        ivLogger.logp(Level.INFO, CLASS_NAME, "timeout", "invoked");
        svTimerLatch.countDown();
    }

    @Override
    public boolean isAroundTimeoutInvoked() {
        return ivAroundTimeout;
    }

    @Override
    public boolean isParentAroundTimeoutInvoked() {
        return ivParentAroundTimeout;
    }

    protected final Object doAroundTimeout(InvocationContext ic) throws Exception {
        ivLogger.logp(Level.INFO, CLASS_NAME, "doAroundTimeout", "invoked: " + ic);
        try {
            ivAroundTimeout = true;
            return ic.proceed();
        } finally {
            ivLogger.logp(Level.INFO, CLASS_NAME, "doAroundTimeout", "exit");
        }
    }

    protected final Object doParentAroundTimeout(InvocationContext ic) throws Exception {
        ivLogger.logp(Level.INFO, CLASS_NAME, "doParentAroundTimeout", "invoked: " + ic);
        ivParentAroundTimeout = true;
        return ic.proceed();
    }
}
