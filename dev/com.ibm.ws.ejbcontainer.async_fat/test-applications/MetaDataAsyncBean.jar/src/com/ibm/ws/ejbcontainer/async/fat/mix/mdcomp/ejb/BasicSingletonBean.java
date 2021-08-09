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
package com.ibm.ws.ejbcontainer.async.fat.mix.mdcomp.ejb;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Singleton;

import com.ibm.ws.ejbcontainer.async.fat.mix.shared.BasicMetadataLocal;
import com.ibm.ws.ejbcontainer.async.fat.mix.shared.TestData;

/**
 * Bean implementation class for Enterprise Bean.
 * The ejb-jar.xml for this Bean will have the metadata-complete attribute
 * set to true and therefore all of the annotations should be ignored.
 **/
@Singleton
@Local(BasicMetadataLocal.class)
@Asynchronous
public class BasicSingletonBean {
    public final static String CLASSNAME = BasicSingletonBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    public static CountDownLatch svMDCLatch = null;

    public void test_metaDataComplete() {
        svLogger.info("--> Entering method, test_metaDataComplete, that has no parameters.");

        TestData.setBsb_syncMethThreadId(Thread.currentThread().getId());
        svLogger.info("--> Since the method should be synchronous it should run under the same thread as the test, beanThreadID = " + TestData.getBsb_syncMethThreadId());
        svMDCLatch.countDown();

        svLogger.info("--> Exiting method, test_metaDataComplete, that has no parameters.");
        return;
    }

    public void test_metaDataComplete(String param) {
        svLogger.info("--> Entering method, test_metaDataComplete, that has a String parameter: " + param);

        TestData.setBsb_asyncMethThreadId(Thread.currentThread().getId());
        svLogger.info("--> Since the method should be asynchronous it should run under a different thread, beanThreadID = " + TestData.getBsb_asyncMethThreadId());
        svMDCLatch.countDown();

        svLogger.info("--> Exiting method, test_metaDataComplete, that has a String parameter.");
        return;
    }

    @Asynchronous
    public void test_metaDataCompleteSync(String param) {
        svLogger.info("--> Entering method, test_metaDataCompleteSync, that has a different name than the method marked as asynchronous in the XML and has a String parameter: "
                      + param);

        TestData.setBsb_syncMeth2ThreadId(Thread.currentThread().getId());
        svLogger.info("--> Since the method should be synchronous it should run under the same thread as the test, beanThreadID = " + TestData.getBsb_syncMeth2ThreadId());
        svMDCLatch.countDown();

        svLogger.info("--> Exiting method, test_metaDataCompleteSync, that has a different name than the method marked as asynchronous in the XML and has a String parameter.");
        return;
    }
}