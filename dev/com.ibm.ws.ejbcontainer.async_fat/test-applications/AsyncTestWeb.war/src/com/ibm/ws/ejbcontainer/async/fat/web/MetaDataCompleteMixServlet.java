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
package com.ibm.ws.ejbcontainer.async.fat.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.async.fat.mix.mdcomp.ejb.BasicSingletonBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.mdcomp.ejb.BasicStatefulBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.mdcomp.ejb.BasicStatelessBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.shared.BasicMetadataLocal;
import com.ibm.ws.ejbcontainer.async.fat.mix.shared.TestData;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/MetaDataCompleteMixServlet")
public class MetaDataCompleteMixServlet extends FATServlet {
    private final static String CLASSNAME = MetaDataCompleteMixServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB(beanName = "BasicSingletonBean")
    BasicMetadataLocal basicSingletonLocalBean;

    @EJB(beanName = "BasicStatefulBean")
    BasicMetadataLocal basicStatefulLocalBean;

    @EJB(beanName = "BasicStatelessBean")
    BasicMetadataLocal basicStatelessLocalBean;

    private BasicMetadataLocal lookupBasicSingletonLocalBean() throws Exception {
        return basicSingletonLocalBean;
    }

    private BasicMetadataLocal lookupBasicStatefulLocalBean() throws Exception {
        return basicStatefulLocalBean;
    }

    private BasicMetadataLocal lookupBasicStatelessLocalBean() throws Exception {
        return basicStatelessLocalBean;
    }

    @Test
    public void testStatefulMetaDataComplete() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMetadataLocal bean = lookupBasicStatefulLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        BasicStatefulBean.svMDCLatch = new CountDownLatch(1);

        // call the method that should be synchronous
        bean.test_metaDataComplete();

        // Wait for test_metaDataComplete() to finish
        BasicStatefulBean.svMDCLatch.await(BasicStatefulBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatefulBean.syncMethThreadId = " + TestData.getBsb_syncMethThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMethThreadId = " + TestData.getBsb_syncMethThreadId() + ", currentThreadId = "
                     + currentThreadId, TestData.getBsb_syncMethThreadId(), currentThreadId);

        // Reset the latch
        BasicStatefulBean.svMDCLatch = new CountDownLatch(1);

        // This method has an Asynchronous annotation that should be ignored
        bean.test_metaDataCompleteSync("Method should be synchronous.");

        // Wait for test_metaDataCompleteSync() to finish
        BasicStatefulBean.svMDCLatch.await(BasicStatefulBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatefulBean.syncMeth2ThreadId = " + TestData.getBsb_syncMeth2ThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMeth2ThreadId = " + TestData.getBsb_syncMeth2ThreadId() + ", currentThreadId = "
                     + currentThreadId, TestData.getBsb_syncMeth2ThreadId(), currentThreadId);

        // Reset the latch
        BasicStatefulBean.svMDCLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous as defined in xml
        bean.test_metaDataComplete("Method should be asynchronous.");

        // Wait for test_metaDataComplete() to finish
        BasicStatefulBean.svMDCLatch.await(BasicStatefulBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatefulBean.asyncMethThreadId = " + TestData.getBsb_asyncMethThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + TestData.getBsb_asyncMethThreadId() + ", currentThreadId = "
                   + currentThreadId, (TestData.getBsb_asyncMethThreadId() != currentThreadId));
    }

    @Test
    public void testSingletonMetaDataComplete() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMetadataLocal bean = lookupBasicSingletonLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        BasicSingletonBean.svMDCLatch = new CountDownLatch(1);

        // call the method that should be synchronous
        bean.test_metaDataComplete();

        // Wait for test_metaDataComplete() to finish
        BasicSingletonBean.svMDCLatch.await(BasicSingletonBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicSingletonBean.syncMethThreadId = " + TestData.getBsb_syncMethThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMethThreadId = " + TestData.getBsb_syncMethThreadId() + ", currentThreadId = "
                     + currentThreadId, TestData.getBsb_syncMethThreadId(), currentThreadId);

        // Reset the latch
        BasicSingletonBean.svMDCLatch = new CountDownLatch(1);

        // This method has an Asynchronous annotation that should be ignored
        bean.test_metaDataCompleteSync("Method should be synchronous.");

        // Wait for test_metaDataCompleteSync() to finish
        BasicSingletonBean.svMDCLatch.await(BasicSingletonBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicSingletonBean.syncMeth2ThreadId = " + TestData.getBsb_syncMeth2ThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMeth2ThreadId = " + TestData.getBsb_syncMeth2ThreadId() + ", currentThreadId = "
                     + currentThreadId, TestData.getBsb_syncMeth2ThreadId(), currentThreadId);

        // Reset the latch
        BasicSingletonBean.svMDCLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous as defined in xml
        bean.test_metaDataComplete("Method should be asynchronous.");

        // Wait for test_metaDataComplete() to finish
        BasicSingletonBean.svMDCLatch.await(BasicSingletonBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicSingletonBean.asyncMethThreadId = " + TestData.getBsb_asyncMethThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + TestData.getBsb_asyncMethThreadId() + ", currentThreadId = "
                   + currentThreadId, (TestData.getBsb_asyncMethThreadId() != currentThreadId));
    }

    @Test
    public void testStatelessMetaDataComplete() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMetadataLocal bean = lookupBasicStatelessLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        BasicStatelessBean.svMDCLatch = new CountDownLatch(1);

        // call the method that should be synchronous
        bean.test_metaDataComplete();

        // Wait for test_metaDataComplete() to finish
        BasicStatelessBean.svMDCLatch.await(BasicStatelessBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatelessBean.syncMethThreadId = " + TestData.getBsb_syncMethThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMethThreadId = " + TestData.getBsb_syncMethThreadId() + ", currentThreadId = "
                     + currentThreadId, TestData.getBsb_syncMethThreadId(), currentThreadId);

        // Reset the latch
        BasicStatelessBean.svMDCLatch = new CountDownLatch(1);

        // This method has an Asynchronous annotation that should be ignored
        bean.test_metaDataCompleteSync("Method should be synchronous.");

        // Wait for test_metaDataCompleteSync() to finish
        BasicStatelessBean.svMDCLatch.await(BasicStatelessBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatelessBean.syncMeth2ThreadId = " + TestData.getBsb_syncMeth2ThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMeth2ThreadId = " + TestData.getBsb_syncMeth2ThreadId() + ", currentThreadId = "
                     + currentThreadId, TestData.getBsb_syncMeth2ThreadId(), currentThreadId);

        // Reset the latch
        BasicStatelessBean.svMDCLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous as defined in xml
        bean.test_metaDataComplete("Method should be asynchronous.");

        // Wait for test_metaDataComplete() to finish
        BasicStatelessBean.svMDCLatch.await(BasicStatelessBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatelessBean.asyncMethThreadId = " + TestData.getBsb_asyncMethThreadId() + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + TestData.getBsb_asyncMethThreadId() + ", currentThreadId = "
                   + currentThreadId, (TestData.getBsb_asyncMethThreadId() != currentThreadId));
    }
}