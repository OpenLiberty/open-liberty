/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.nested.ejb;

import static javax.ejb.LockType.READ;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.Stateless;

/**
 *
 */
@Stateless
public class SleepTimeoutBean {
    private static final String CLASSNAME = SleepTimeoutBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    public static volatile CountDownLatch svTestLatch = new CountDownLatch(1);

    @Lock(READ)
    @Asynchronous
    public Future<String> testFutureTimeoutInnerHangedThread() {
        svLogger.info("SleepBean sleeping");
        try {
            svTestLatch.await(60 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        svLogger.info("SleepBean done");
        return new AsyncResult<String>("done sleeping");
    }
}
