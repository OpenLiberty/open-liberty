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

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

/**
 * Bean implementation class for Enterprise Bean: ResultsSingletonLocal
 * Returning a Future<String>
 **/
@Singleton
@Remote(ResultsSingletonRemote.class)
public class ResultsSingletonRemoteFutureBean {
    public final static String CLASSNAME = ResultsSingletonRemoteFutureBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static boolean asyncWorkDone = false;

    /** Static variable for timeout value for performing work asynchronously **/
    public static int asyncTimeout = 1000;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static long beanThreadId = 0;

    public void test_SynchTimeout(int waitTime) {
        final String method = "test_SynchTimeout";
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        svLogger.info("threadId: " + Thread.currentThread().getId());

        // sleep for specified time to force client request to timeout
        svLogger.info("Start sleep for " + waitTime + " seconds");
        FATHelper.sleep(waitTime);
        svLogger.info("End sleep for " + waitTime + " seconds");

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, method);
        }
    }

    @Asynchronous
    public Future<String> test_fireAndReturnResults() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndReturnResults");
        }

        // save threadId value to static variable for verification method executed on different thread
        ResultsSingletonRemoteFutureBean.beanThreadId = Thread.currentThread().getId();

        svLogger.info("threadId: " + ResultsSingletonRemoteFutureBean.beanThreadId);

        // sleep for specified time for opportunity to perform other work in calling test
        svLogger.info("Start sleep for " + ResultsSingletonRemoteFutureBean.asyncTimeout / 1000 + " seconds");
        FATHelper.sleep(ResultsSingletonRemoteFutureBean.asyncTimeout);
        svLogger.info("End sleep for " + ResultsSingletonRemoteFutureBean.asyncTimeout / 1000 + " seconds");

        // set static variable for work completed to true
        ResultsSingletonRemoteFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndReturnResults");
        }

        return new AsyncResult<String>(Boolean.toString(ResultsSingletonRemoteFutureBean.asyncWorkDone));
    }

    /*
     * Method to accept wait time value to provide the ability to extend the runtime of the method
     * in order to exceed the ORB request timeout.
     *
     * @waitTime - parameter to specify the time to wait during the method, specified in milliseconds.
     */
    @Asynchronous
    public Future<String> test_fireAndReturnResults(int waitTime) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndReturnResults");
        }

        // save threadId value to static variable for verification method executed on different thread
        ResultsSingletonRemoteFutureBean.beanThreadId = Thread.currentThread().getId();

        svLogger.info("threadId: " + ResultsSingletonRemoteFutureBean.beanThreadId);

        // sleep for specified time for opportunity to perform other work in calling test
        svLogger.info("Start sleep for " + waitTime + " seconds");
        FATHelper.sleep(waitTime);
        svLogger.info("End sleep for " + waitTime + " seconds");

        // set static variable for work completed to true
        ResultsSingletonRemoteFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndReturnResults");
        }

        return new AsyncResult<String>(Boolean.toString(ResultsSingletonRemoteFutureBean.asyncWorkDone));
    }

    @Asynchronous
    public Future<String> test_fireAndReturnResults_null() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndReturnResults_null");
        }

        // save threadId value to static variable for verification method executed on different thread
        ResultsSingletonRemoteFutureBean.beanThreadId = Thread.currentThread().getId();

        svLogger.info("threadId: " + ResultsSingletonRemoteFutureBean.beanThreadId);

        // sleep for specified time for opportunity to perform other work in calling test
        svLogger.info("Start sleep for " + ResultsSingletonRemoteFutureBean.asyncTimeout / 1000 + " seconds");
        FATHelper.sleep(ResultsSingletonRemoteFutureBean.asyncTimeout);
        svLogger.info("End sleep for " + ResultsSingletonRemoteFutureBean.asyncTimeout / 1000 + " seconds");

        // set static variable for work completed to true
        ResultsSingletonRemoteFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndReturnResults_null");
        }

        return new AsyncResult<String>(null);
    }

    public ResultsSingletonRemoteFutureBean() {
    }
}