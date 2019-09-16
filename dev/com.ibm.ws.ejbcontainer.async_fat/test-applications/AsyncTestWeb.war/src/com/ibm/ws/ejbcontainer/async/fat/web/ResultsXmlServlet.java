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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.ResultsSingletonLocal;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.ResultsSingletonLocalFutureBean;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.ResultsStatelessLocal;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.ResultsStatelessLocalFutureBean;

import componenttest.app.FATServlet;

/**
 * Tests async methods that return results for singleton and stateless session beans. <p>
 *
 * This test has been derived from 2 different suites in the r80-asynch FAT
 * bucket originally implemented for traditional WAS : sing_xml, slx.
 */
@WebServlet("/ResultsXmlServlet")
@SuppressWarnings("serial")
public class ResultsXmlServlet extends FATServlet {
    public final static String CLASSNAME = ResultsXmlServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // wait time used for future.isDone polling
    public static int waitTime = 400;

    @EJB
    ResultsStatelessLocal ivSLLBean;

    @EJB
    ResultsSingletonLocal ivSGLBean;

    private ResultsStatelessLocal lookupSLLBean() throws Exception {
        return ivSLLBean;
    }

    private ResultsSingletonLocal lookupSGLBean() throws Exception {
        return ivSGLBean;
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Stateless
     * Session Bean that returns results in a Future<String> object. Verification will be
     * done via checking the Future<V>.isDone() method prior to Future<V>.get() method is
     * called to retrieve returned results.
     */
    @Test
    public void testSLLFutureIsDoneXml() throws Exception {
        long currentThreadId = 0;
        int i = 0;

        ResultsStatelessLocal bean = lookupSLLBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults();

        // wait for async work to complete using isDone()
        while (i < 450 && !future.isDone()) {
            i++;
            Thread.sleep(waitTime);
        }

        assertTrue("future.isDone() didn't return true", future.isDone());
        String results = future.get();

        svLogger.info("Asynchronous method work completed: " + results);
        assertEquals("Async Stateless Bean method completed", "true", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessLocalFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Stateless
     * Session Bean that returns results in a Future<String> object. Verification will
     * be done via the Future<V>.get() method to retrieve returned results, this is done
     * prior to the called method completion, Future<V>.get() will block until completion.
     */
    @Test
    public void testSLLFutureGetBlocksXml() throws Exception {
        long currentThreadId = 0;

        ResultsStatelessLocal bean = lookupSLLBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults();

        svLogger.info("Retrieving results");
        String results = future.get();

        svLogger.info("Asynchronous method work completed: " + results);
        assertEquals("Async Stateless Bean method completed", "true", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessLocalFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Stateless
     * Session Bean that returns results in a Future<String> object. Verification will be
     * done via the Future<V>.get(timeout, unit) method to retrieve returned results, this
     * is done prior to the called method completion, Future<V>.get(timeout, unit) will
     * block until completion or timeout value exceeded, timeout value will be greater
     * than method completion time needed.
     */
    @Test
    public void testSLLFutureGetNoTimeoutXml() throws Exception {
        long currentThreadId = 0;

        ResultsStatelessLocal bean = lookupSLLBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults();

        svLogger.info("Retrieving results");
        // Wait for up to 10 secs
        String results = future.get(ResultsStatelessLocalFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + results);
        assertEquals("Async Stateless Bean method completed", "true", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessLocalFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Stateless
     * Session Bean that returns results in a Future<String> object. Verification will be
     * done via the Future<V>.get(timeout, unit) method to retrieve returned results, this
     * is done prior to the called method completion, Future<V>.get(timeout, unit) will
     * block until completion or timeout value exceeded, timeout value will be shorter
     * than method completion time needed. TimeoutException is expected to be signaled.
     */
    @Test
    public void testSLLFutureGetTimesOutXml() throws Exception {
        long currentThreadId = 0;

        ResultsStatelessLocal bean = lookupSLLBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // initialize latches
        ResultsStatelessLocalFutureBean.svBeanLatch = new CountDownLatch(1);
        ResultsStatelessLocalFutureBean.svTestLatch = new CountDownLatch(1);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults_await();

        svLogger.info("Retrieving results");

        try {
            // get without wait
            future.get(0, TimeUnit.MILLISECONDS);
            fail("Expected TimeoutException did not occur");
        } catch (TimeoutException te) {
            // timeout exceeded as expected
            svLogger.info("caught expected TimeoutException");
        } finally {
            // allow async method to run
            ResultsStatelessLocalFutureBean.svTestLatch.countDown();

            // wait to ensure async method has completed
            ResultsStatelessLocalFutureBean.svBeanLatch.await(ResultsStatelessLocalFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);
        }

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessLocalFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method executed on separate thread", (ResultsStatelessLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Stateless
     * Session Bean that returns results in a Future<String> object. Verification will be
     * done via the Future<V>.get(timeout, unit) method to retrieve returned results, this
     * is done prior to the called method completion, Future<V>.get(timeout, unit) will
     * block until completion or timeout value exceeded, timeout value will be greater
     * than method completion time needed.
     */
    @Test
    public void testSLLFutureGetNullXml() throws Exception {
        String results = "";
        long currentThreadId = 0;

        ResultsStatelessLocal bean = lookupSLLBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults_null();

        // Wait for up to 10 secs
        svLogger.info("Retrieving results");
        results = future.get(ResultsStatelessLocalFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + results);
        assertNull("Async Stateless Bean method returned null", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessLocalFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Singleton Session Bean
     * that returns results in a Future<String> object. Verification will be done via checking the
     * Future<V>.isDone() method prior to Future<V>.get() method is called to retrieve returned results.
     */
    @Test
    public void testSGLFutureIsDoneXml() throws Exception {
        long currentThreadId = 0;
        int i = 0;

        ResultsSingletonLocal bean = lookupSGLBean();
        assertNotNull("Async Singleton Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults();

        // wait for async work to complete using isDone()
        while (i < 450 && !future.isDone()) {
            i++;
            Thread.sleep(waitTime);
        }

        assertTrue("future.isDone() didn't return true", future.isDone());

        String results = future.get();

        svLogger.info("Asynchronous method work completed: " + results);
        assertEquals("Async Singleton Bean method completed", "true", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsSingletonLocalFutureBean.beanThreadId);

        assertFalse("Async Singleton Bean method completed on separate thread", (ResultsSingletonLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Singleton Session Bean
     * that returns results in a Future<String> object. Verification will be done via the Future<V>.get()
     * method to retrieve returned results, this is done prior to the called method completion,
     * Future<V>.get() will block until completion.
     */
    @Test
    public void testSGLFutureGetBlocksXml() throws Exception {
        long currentThreadId = 0;

        ResultsSingletonLocal bean = lookupSGLBean();
        assertNotNull("Async Singleton Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults();

        svLogger.info("Retrieving results");
        String results = future.get();

        svLogger.info("Asynchronous method work completed: " + results);
        assertEquals("Async Singleton Bean method completed", "true", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsSingletonLocalFutureBean.beanThreadId);

        assertFalse("Async Singleton Bean method completed on separate thread", (ResultsSingletonLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Singleton Session Bean
     * that returns results in a Future<String> object. Verification will be done via the
     * Future<V>.get(timeout, unit) method to retrieve returned results, this is done prior to the
     * called method completion, Future<V>.get(timeout, unit) will block until completion or timeout
     * value exceeded, timeout value will be greater than method completion time needed.
     */
    @Test
    public void testSGLFutureGetNoTimeoutXml() throws Exception {
        long currentThreadId = 0;

        ResultsSingletonLocal bean = lookupSGLBean();
        assertNotNull("Async Singleton Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults();

        svLogger.info("Retrieving results");
        String results = future.get(ResultsSingletonLocalFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + results);
        assertEquals("Async Singleton Bean method completed", "true", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsSingletonLocalFutureBean.beanThreadId);

        assertFalse("Async Singleton Bean method completed on separate thread", (ResultsSingletonLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Singleton Session Bean
     * that returns results in a Future<String> object. Verification will be done via the
     * Future<V>.get(timeout, unit) method to retrieve returned results, this is done prior to the
     * called method completion, Future<V>.get(timeout, unit) will block until completion or timeout
     * value exceeded, timeout value will be shorter than method completion time needed.
     * TimeoutException is expected to be signaled.
     */
    @Test
    public void testSGLFutureGetTimesOutXml() throws Exception {
        long currentThreadId = 0;

        ResultsSingletonLocal bean = lookupSGLBean();
        assertNotNull("Async Singleton Bean created successfully", bean);

        // initialize latches
        ResultsSingletonLocalFutureBean.svBeanLatch = new CountDownLatch(1);
        ResultsSingletonLocalFutureBean.svTestLatch = new CountDownLatch(1);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults_await();

        svLogger.info("Retrieving results");

        try {
            // get without wait
            future.get(0, TimeUnit.MILLISECONDS);
            fail("Expected TimeoutException did not occur");
        } catch (TimeoutException te) {
            // timeout exceeded as expected
            svLogger.info("caught expected TimeoutException");
        } finally {
            // allow async method to run
            ResultsSingletonLocalFutureBean.svTestLatch.countDown();

            // wait to ensure async method has completed
            ResultsSingletonLocalFutureBean.svBeanLatch.await(ResultsSingletonLocalFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);
        }

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsSingletonLocalFutureBean.beanThreadId);

        assertFalse("Async Singleton Bean method executed on separate thread", (ResultsSingletonLocalFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method, defined in XML to be Asynchronous, on an EJB 3.1 Singleton Session Bean
     * that returns results in a Future<String> object. Verification will be done via the
     * Future<V>.get(timeout, unit) method to retrieve returned results, this is done prior to the
     * called method completion, Future<V>.get(timeout, unit) will block until completion or timeout
     * value exceeded, timeout value will be greater than method completion time needed.
     */
    @Test
    public void testSGLFutureGetNullXml() throws Exception {
        String results = "";
        long currentThreadId = 0;

        ResultsSingletonLocal bean = lookupSGLBean();
        assertNotNull("Async Singleton Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults_null();

        // Wait for up to 10 secs
        svLogger.info("Retrieving results");
        results = future.get(ResultsSingletonLocalFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + results);
        assertNull("Async Singleton Bean method returned null", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsSingletonLocalFutureBean.beanThreadId);

        assertFalse("Async Singleton Bean method completed on separate thread", (ResultsSingletonLocalFutureBean.beanThreadId == currentThreadId));
    }
}