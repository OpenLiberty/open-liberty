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
package com.ibm.ws.ejbcontainer.async.fat.xml.ejb;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean implementation class for Enterprise Bean: BasicStatelessLocal
 **/
public class Style3XMLBean {
    public final static String CLASSNAME = Style3XMLBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variables for thread the method is executing on for comparison to caller thread **/
    public static volatile long syncMethThreadId = 0;
    public static volatile long syncMeth2ThreadId = 0;
    public static volatile long asyncMethThreadId = 0;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static volatile CountDownLatch svBeanLatch;

    public CountDownLatch getBeanLatch() {
        svBeanLatch = new CountDownLatch(1);
        return svBeanLatch;
    }

    public void test_xmlStyle3() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_xmlStyle3");
        }

        syncMethThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be synchronous it should run under the same thread as the test, beanThreadID = " + syncMethThreadId);

        svBeanLatch.countDown();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_xmlStyle3");
        }

        return;
    }

    public void test_xmlStyle3(String param) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_xmlStyle3(param)");
        }

        asyncMethThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be asynchronous it should run under a different thread, beanThreadID = " + asyncMethThreadId);

        svBeanLatch.countDown();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_xmlStyle3(param)");
        }

        return;
    }

    public void test_diffNameSameParam(String param) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "test_diffNameSameParam(param)");
        }

        syncMeth2ThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be synchronous it should run under the same thread as the test, beanThreadID = " + syncMeth2ThreadId);

        svBeanLatch.countDown();

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "test_diffNameSameParam(param)");
        }

        return;
    }
}