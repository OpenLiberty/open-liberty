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
import javax.ejb.MessageDriven;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

@MessageDriven
public class MethodIntfMDB implements MessageListener {
    private static final Logger svLogger = Logger.getLogger(MethodIntfMDB.class.getName());

    @Resource
    private TimerService ivTimerService;

    public static CountDownLatch svCountDownLatch;
    public static boolean svMessageTransactionGlobal;
    public static boolean svTimeoutTransactionGlobal;

    public static void setup() {
        svCountDownLatch = new CountDownLatch(1);
        svMessageTransactionGlobal = false;
        svTimeoutTransactionGlobal = false;
    }

    @Override
    public void onMessage(Message arg) {
        svMessageTransactionGlobal = FATTransactionHelper.isTransactionGlobal();
        svLogger.info("onMessage: " + svMessageTransactionGlobal);
        ivTimerService.createSingleActionTimer(0, new TimerConfig(null, false));
    }

    @Timeout
    public void timeout() {
        svTimeoutTransactionGlobal = FATTransactionHelper.isTransactionGlobal();
        svLogger.info("timeout: " + svMessageTransactionGlobal);
        svCountDownLatch.countDown();
    }
}
