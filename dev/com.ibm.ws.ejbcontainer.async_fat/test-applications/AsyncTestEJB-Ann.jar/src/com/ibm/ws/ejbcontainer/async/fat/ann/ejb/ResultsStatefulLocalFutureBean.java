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
package com.ibm.ws.ejbcontainer.async.fat.ann.ejb;

import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionManagementType.CONTAINER;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;

/**
 * Bean implementation class for Enterprise Bean: ResultsStatefulLocal
 * Returning a boolean ( EJBContainer will package into Future<Boolean> for the return )
 **/
@Stateful(name = "ResultsStateful")
@Local(ResultsStatefulLocal.class)
@TransactionManagement(CONTAINER)
public class ResultsStatefulLocalFutureBean {
    public final static String CLASSNAME = ResultsStatefulLocalFutureBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static volatile boolean asyncWorkDone = false;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static volatile long beanThreadId = 0;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static volatile CountDownLatch svBeanLatch = new CountDownLatch(1);
    public static volatile CountDownLatch svTestLatch = new CountDownLatch(1);

    @Asynchronous
    @TransactionAttribute(REQUIRED)
    public Future<Boolean> test_fireAndReturnResults() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndReturnResults");
        }

        // save threadId value to static variable for verification method executed on different thread
        ResultsStatefulLocalFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ResultsStatefulLocalFutureBean.beanThreadId);

        // set static variable for work completed to true
        ResultsStatefulLocalFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndReturnResults");
        }

        return new AsyncResult<Boolean>(ResultsStatefulLocalFutureBean.asyncWorkDone);
    }

    @Asynchronous
    @TransactionAttribute(REQUIRED)
    public Future<Boolean> test_fireAndReturnResults_await() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndReturnResults_await");
        }

        try {
            // wait to ensure TimeoutException in calling test
            svTestLatch.await(MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            svLogger.warning("Exception while waiting: " + ie.toString());
        }

        // save threadId value to static variable for verification method executed on different thread
        ResultsStatefulLocalFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ResultsStatefulLocalFutureBean.beanThreadId);

        // set static variable for work completed to true
        ResultsStatefulLocalFutureBean.asyncWorkDone = true;

        svBeanLatch.countDown();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndReturnResults_await");
        }

        return new AsyncResult<Boolean>(ResultsStatefulLocalFutureBean.asyncWorkDone);
    }

    public ResultsStatefulLocalFutureBean() {
    }
}