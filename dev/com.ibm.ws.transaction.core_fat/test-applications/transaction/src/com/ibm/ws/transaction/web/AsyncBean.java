/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

/**
 *
 */
@RequestScoped
public class AsyncBean {

    @Resource
    private UserTransaction ut;

    @Asynchronous
    @Fallback(fallbackMethod = "fallbackMethod")
    public Future<Integer> getInt() throws Exception {
        System.out.println("AsyncBean.getInt(): About to begin tran on thread: " + String.format("%08X", Thread.currentThread().getId()));

        ut.begin();

        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        final Transaction tx = tm.getTransaction();

        tx.enlistResource(new XAResourceImpl());

        Integer i = getResult();

        ut.commit();

        return CompletableFuture.completedFuture(i);
    }

    public Future<Integer> fallbackMethod() throws Exception {
        System.out.println("AsyncBean.fallbackMethod(): Returning 17");
        return CompletableFuture.completedFuture(Integer.valueOf(17));
    }

    public Integer getResult() throws Exception {
        throw new Exception();
    }
}