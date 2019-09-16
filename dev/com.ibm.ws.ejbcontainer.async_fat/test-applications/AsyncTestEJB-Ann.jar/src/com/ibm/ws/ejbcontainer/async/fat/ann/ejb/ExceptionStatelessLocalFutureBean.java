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

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

/**
 * Bean implementation class for Enterprise Bean: ExceptionStatelessLocal
 * Returning a Future<String>
 **/
@Stateless
@Local({ ExceptionStatelessLocal.class, ExceptionStatelessLocal2.class })
@Asynchronous
public class ExceptionStatelessLocalFutureBean {
    public final static String CLASSNAME = ExceptionStatelessLocalFutureBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static volatile boolean asyncWorkDone = false;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static volatile long beanThreadId = 0;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public Future<String> test_fireAndThrowSystemException() {
        String method = "test_fireAndThrowSystemException";

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        // save threadId value to static variable for verification method executed on different thread
        ExceptionStatelessLocalFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ExceptionStatelessLocalFutureBean.beanThreadId);

        // set static variable for work completed to true
        ExceptionStatelessLocalFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, method);
        }

        throw new RuntimeException("This exception was intentionally thrown to test error path behavior.");
    }

    public Future<String> test_fireAndThrowApplicationException() throws AsyncApplicationException {
        String method = "test_fireAndThrowApplicationException";

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        // save threadId value to static variable for verification method executed on different thread
        ExceptionStatelessLocalFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ExceptionStatelessLocalFutureBean.beanThreadId);

        // set static variable for work completed to true
        ExceptionStatelessLocalFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, method);
        }

        throw new AsyncApplicationException("This exception was intentionally thrown to test error path behavior.");
    }

    @Interceptors(BasicStatelessLocalInterceptor.class)
    public Future<String> test_fireAroundInvokeAndThrowApplicationException(List<String> value) throws AsyncApplicationException {
        String method = "test_fireAroundInvokeAndThrowApplicationException";

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, method);
        }

        value.add("method");

        // save threadId value to static variable for verification method executed on different thread
        ExceptionStatelessLocalFutureBean.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + ExceptionStatelessLocalFutureBean.beanThreadId);

        // set static variable for work completed to true
        ExceptionStatelessLocalFutureBean.asyncWorkDone = true;

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, method);
        }

        throw new AsyncApplicationException("This exception was intentionally thrown to test error path behavior.");
    }

    public Future<Void> test_exception(boolean throwException2) throws AsyncApplicationException, AsyncApplicationException2 {
        if (throwException2) {
            throw new AsyncApplicationException2("");
        }

        throw new AsyncApplicationException("");
    }
}