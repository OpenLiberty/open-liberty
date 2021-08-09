/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.cancel.web;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Stateless
@Local(FutureCancelDriver.class)
public class FutureCancelDriverLocalBean {
    private static final String CLASSNAME = FutureCancelDriverLocalBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    private static CountDownLatch methodRunningLatch;
    private static final Exchanger<Object> exchanger = new Exchanger<Object>();

    @Resource
    SessionContext ctx;

    @Asynchronous
    public Future<String> asyncCancelled() throws Exception {
        svLogger.info("asyncCancelled");
        return new AsyncResult<String>("In asyncCancelled");
    }

    public void initializeAsyncNotCancelled() {
        methodRunningLatch = new CountDownLatch(1);
    }

    @Asynchronous
    public Future<String> asyncNotCancelled(int numAwaitWasCancelCalled) throws Exception {
        svLogger.info("asyncNotCancelled - " + numAwaitWasCancelCalled);
        methodRunningLatch.countDown();
        for (int i = 0; i < numAwaitWasCancelCalled; i++) {
            if (!"unblock".equals(exchange("unblocked"))) {
                throw new IllegalStateException();
            }
            exchange(ctx.wasCancelCalled());
        }
        return new AsyncResult<String>("asyncNotCancelled - " + numAwaitWasCancelCalled);
    }

    public void awaitAsyncNotCancelled() {
        try {
            if (!methodRunningLatch.await(30, TimeUnit.SECONDS)) {
                throw new EJBException("timeout");
            }
        } catch (InterruptedException e) {
            throw new EJBException(e);
        }
    }

    public boolean awaitWasCancelCalled() {
        if (!"unblocked".equals(exchange("unblock"))) {
            throw new IllegalStateException();
        }
        return (Boolean) exchange(null);
    }

    public boolean sessionContextWasCancelCalledInSync() throws Exception {
        svLogger.info("sessionContextwasCancelCalledInSync");
        try {
            ctx.wasCancelCalled();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }

    @Asynchronous
    public void sessionContextWasCancelCalledInVoid() throws Exception {
        svLogger.info("sessionContextwasCancelCalledInSync");
        boolean result;
        try {
            ctx.wasCancelCalled();
            result = false;
        } catch (IllegalStateException e) {
            result = true;
        }
        exchange(result);
    }

    public boolean awaitWasCancelCalledInVoid() {
        return (Boolean) exchange(null);
    }

    private static Object exchange(Object value) {
        try {
            return exchanger.exchange(value, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new EJBException(e);
        } catch (TimeoutException e) {
            throw new EJBException(e);
        }
    }
}