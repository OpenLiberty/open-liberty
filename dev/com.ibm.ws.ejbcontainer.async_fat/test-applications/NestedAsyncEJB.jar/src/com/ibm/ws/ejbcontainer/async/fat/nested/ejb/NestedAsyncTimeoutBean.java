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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.Stateless;

import org.junit.Assert;

/**
 *
 */
@Stateless
public class NestedAsyncTimeoutBean {

    private static final String CLASSNAME = NestedAsyncTimeoutBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB
    private NestedAsyncTimeoutBean selfBean;

    @EJB(beanName = "SleepTimeoutBean")
    private SleepTimeoutBean sleepBean;

    @Lock(READ)
    @Asynchronous
    public Future<String> testFutureTimeout() {

        svLogger.info("testFutureTimeout: enter");
        Future<String> futureMsg = selfBean.invoke();

        try {
            svLogger.info("Calling 2nd level future.get()");
            futureMsg.get(3, TimeUnit.SECONDS);
            Assert.fail("2nd level future.get() returned, it should have timed out.");
        } catch (TimeoutException e) {
            //expected timeout
            svLogger.info("testFutureTimeout: hit expected timeout");
        } catch (Exception e) {
            //unexpected exception;
            Assert.fail("testFutureTimeout unexpected exception: " + e.getMessage());
        }

        svLogger.info("testFutureTimeout: done");
        return new AsyncResult<String>("testFutureTimeout done");
    }

    @Lock(READ)
    @Asynchronous
    public Future<String> invoke() {
        svLogger.info("Injected selfBean invoke enter");
        Future<String> beanAsync = sleepBean.testFutureTimeoutInnerHangedThread();
        svLogger.info("Invoke exit");
        return beanAsync;
    }
}
