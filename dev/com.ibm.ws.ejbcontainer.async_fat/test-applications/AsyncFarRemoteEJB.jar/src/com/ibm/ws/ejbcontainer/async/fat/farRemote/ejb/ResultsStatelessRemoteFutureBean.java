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
package com.ibm.ws.ejbcontainer.async.fat.farRemote.ejb;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Enterprise Bean: ResultsStatelessLocal
 * Returning a Future<String>
 **/
@Stateless
@Remote(ResultsStatelessRemote.class)
@Asynchronous
public class ResultsStatelessRemoteFutureBean {
    public final static String CLASSNAME = ResultsStatelessRemoteFutureBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static boolean asyncWorkDone = false;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static long beanThreadId = 0;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static volatile CountDownLatch svBeanLatch = new CountDownLatch(1);
    public static volatile CountDownLatch svTestLatch = new CountDownLatch(1);

    public Future<String> test_fireAndReturnResults() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndReturnResults");
        }

        // save threadId value to static variable for verification method executed on different thread
        ResultsStatelessRemoteFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ResultsStatelessRemoteFutureBean.beanThreadId);

        // set static variable for work completed to true
        ResultsStatelessRemoteFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndReturnResults");
        }

        return new AsyncResult<String>(Boolean.toString(ResultsStatelessRemoteFutureBean.asyncWorkDone));
    }

    public Future<String> test_fireAndReturnResults_null() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndReturnResults_null");
        }

        // save threadId value to static variable for verification method executed on different thread
        ResultsStatelessRemoteFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ResultsStatelessRemoteFutureBean.beanThreadId);

        // set static variable for work completed to true
        ResultsStatelessRemoteFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndReturnResults_null");
        }

        return new AsyncResult<String>(null);
    }

    public Future<String> test_fireAndReturnResults_await() {
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
        ResultsStatelessRemoteFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ResultsStatelessRemoteFutureBean.beanThreadId);

        // set static variable for work completed to true
        ResultsStatelessRemoteFutureBean.asyncWorkDone = true;

        svBeanLatch.countDown();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndReturnResults_await");
        }

        return new AsyncResult<String>(Boolean.toString(ResultsStatelessRemoteFutureBean.asyncWorkDone));
    }

    public Future<Boolean> test_fireAndReturnResults_classloader() {
        final String meth = "test_fireAndReturnResults_classloader";

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, meth);
        }

        // save threadId value to static variable for verification method executed on different thread
        ResultsStatelessRemoteFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ResultsStatelessRemoteFutureBean.beanThreadId);

        // retrieve this classes classloader and contextclassloader and compare to ensure they are the same
        ClassLoader cl1 = this.getClass().getClassLoader();
        svLogger.info("Bean ClassLoader: " + cl1.toString());

        ClassLoader cl2 = Thread.currentThread().getContextClassLoader();
        svLogger.info("Context ClassLoader: " + cl2.toString());

        // in liberty, thread context classloader will not be application classloader, but will delegate
        // to the application classloader. Loading the class should return the currently running class.
        Class<?> thisFromTCCL = null;
        try {
            thisFromTCCL = cl2.loadClass(this.getClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // compare classes loaded by classloaders and store value in local variable to return to caller
        boolean classesLoadedEqual = (this.getClass() == thisFromTCCL);
        svLogger.info("Loaded Classes same = " + classesLoadedEqual);

        // set static variable for work completed to true
        ResultsStatelessRemoteFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, meth);
        }

        return new AsyncResult<Boolean>(Boolean.valueOf(classesLoadedEqual));
    }

    public ResultsStatelessRemoteFutureBean() {
    }
}