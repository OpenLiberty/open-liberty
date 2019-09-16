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

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Singleton;

/**
 * Bean implementation class for Enterprise Bean: BasicSingletonLocal
 **/
@Singleton
@Local(BasicSingletonLocal.class)
@Asynchronous
public class BasicSingletonLocalBean {
    public final static String CLASSNAME = BasicSingletonLocalBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static volatile boolean asyncWorkDone = false;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static volatile long beanThreadId = 0;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static final CountDownLatch svBeanLatch = new CountDownLatch(1);

    public void test_fireAndForget() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_fireAndForget");
        }

        // save threadId value to static variable for verification method executed on different thread
        BasicSingletonLocalBean.beanThreadId = Thread.currentThread().getId();

        svLogger.info("threadId: " + BasicSingletonLocalBean.beanThreadId);

        // set static variable for work completed to true
        BasicSingletonLocalBean.asyncWorkDone = true;

        svBeanLatch.countDown();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_fireAndForget");
        }

        return;
    }

    public BasicSingletonLocalBean() {
    }
}