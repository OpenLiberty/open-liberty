/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.tx.methodintf.ejb;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

@Stateless
public class MethodIntfSLSB {
    private static final Logger svLogger = Logger.getLogger(MethodIntfSLSB.class.getName());

    @Resource
    private TimerService ivTimerService;

    private static CountDownLatch svTimeoutCountDownLatch = new CountDownLatch(1);
    private static boolean svTimeoutTransactionGlobal;

    public boolean isTransactionGlobal() {
        return FATTransactionHelper.isTransactionGlobal();
    }

    public CountDownLatch setup() {
        svTimeoutCountDownLatch = new CountDownLatch(1);
        ivTimerService.createSingleActionTimer(0, new TimerConfig(null, false));
        return svTimeoutCountDownLatch;
    }

    public boolean isTimeoutTransactionGlobal() throws InterruptedException {
        return svTimeoutTransactionGlobal;
    }

    @Timeout
    public void timeout() {
        svTimeoutTransactionGlobal = FATTransactionHelper.isTransactionGlobal();
        svTimeoutCountDownLatch.countDown();
        svLogger.info("timeout: " + svTimeoutTransactionGlobal);
    }
}
