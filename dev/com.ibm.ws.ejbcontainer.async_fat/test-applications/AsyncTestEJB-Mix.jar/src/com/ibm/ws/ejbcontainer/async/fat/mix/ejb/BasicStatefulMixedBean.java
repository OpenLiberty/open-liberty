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
package com.ibm.ws.ejbcontainer.async.fat.mix.ejb;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateful;

/**
 * Bean implementation class for Enterprise Bean
 **/
@Stateful
@Local(BasicMixedLocal.class)
public class BasicStatefulMixedBean {
    public final static String CLASSNAME = BasicStatefulMixedBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static volatile long asyncMethThreadId;
    public static volatile long asyncMeth2ThreadId;
    public static volatile long asyncMeth3ThreadId;
    public static volatile long syncMethThreadId;

    public static volatile CountDownLatch svBasicLatch = new CountDownLatch(1);

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    @Asynchronous
    public void test_asyncMethAnnWithStyle2XML(String param) {
        svLogger.info("--> Entering method, test_MethAnnWithStyle2XML, that has a String parameter: " + param);

        asyncMethThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be asynchronous it should run under a different thread, asyncMethThreadId = " + asyncMethThreadId);
        svBasicLatch.countDown();

        svLogger.info("--> Exiting method, test_MethAnnWithStyle2XML, that has a String parameter.");
        return;
    }

    @Asynchronous
    public void test_asyncMethAnnOnly(String param) {
        svLogger.info("--> Entering method, test_asyncMethAnnOnly, that has a String parameter: " + param);

        asyncMeth2ThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be asynchronous it should run under a different thread, asyncMeth2ThreadId = " + asyncMeth2ThreadId);
        svBasicLatch.countDown();

        svLogger.info("--> Exiting method, test_asyncMethAnnOnly, that has a String parameter.");
        return;
    }

    public void test_asyncMethStyle2XMLOnly(String param) {
        svLogger.info("--> Entering method, test_asyncMethStyle2XMLOnly, that has a String parameter: " + param);

        asyncMeth3ThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be asynchronous it should run under a different thread, asyncMeth3ThreadId = " + asyncMeth3ThreadId);
        svBasicLatch.countDown();

        svLogger.info("--> Exiting method, test_asyncMethStyle2XMLOnly, that has a String parameter.");
        return;
    }

    public void test_syncMethod(String param) {
        svLogger.info("--> Entering method, test_syncMethod, that has a String parameter: " + param);

        syncMethThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be synchronous it should run under the same thread as the test, syncMethThreadId = " + syncMethThreadId);
        svBasicLatch.countDown();

        svLogger.info("--> Exiting method, test_syncMethod, that has a String parameter.");
        return;
    }
}