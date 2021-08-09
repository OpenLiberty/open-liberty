/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.basic;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.SessionContext;

@Remote(BusinessRMI.class)
public abstract class AbstractBusinessRemoteBean implements BusinessRMI, BusinessRemote {
    private static long asyncVoidThreadId;
    private static CountDownLatch asyncVoidLatch;
    private static CountDownLatch asyncFutureLatch;

    @Resource
    private SessionContext context;

    @Override
    public void test() {
    }

    @Override
    public void testAppException() throws TestAppException {
        throw new TestAppException();
    }

    @Override
    public void testSystemException() {
        throw new TestSystemException();
    }

    @Override
    public void testTransactionException() {
        ((RollbackBean) context.lookup("java:module/RollbackBean")).enlist();
    }

    @Override
    public List<?> testWriteValue(List<?> list) {
        return list;
    }

    @Override
    public void setupAsyncVoid() {
        asyncVoidThreadId = -1;
        asyncVoidLatch = new CountDownLatch(1);
    }

    @Asynchronous
    @Override
    public void testAsyncVoid() {
        asyncVoidThreadId = Thread.currentThread().getId();
        asyncVoidLatch.countDown();
    }

    @Override
    public long awaitAsyncVoidThreadId() {
        try {
            asyncVoidLatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new EJBException(e);
        }
        return asyncVoidThreadId;
    }

    @Override
    public void setupAsyncFuture(int asyncCount) {
        asyncFutureLatch = new CountDownLatch(asyncCount);
    }

    @Asynchronous
    @Override
    public Future<Long> testAsyncFuture() {
        asyncFutureLatch.countDown();
        return new AsyncResult<Long>(Thread.currentThread().getId());
    }

    @Override
    public void awaitAsyncFuture() {
        try {
            asyncFutureLatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new EJBException(e);
        }
    }
}
