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
package com.ibm.ws.ejbcontainer.async.fat.farRemote.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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

import com.ibm.ws.ejbcontainer.async.fat.farRemote.ejb.ResultsStatelessRemote;
import com.ibm.ws.ejbcontainer.async.fat.farRemote.ejb.ResultsStatelessRemoteFutureBean;

import componenttest.app.FATServlet;

/**
 * Tests client container and within server process (ie. junitee) remote calls to EJB 3.1 Stateless Session ivSLRBean
 * with Asynchronous annotation at the class level where results are returned in a Future<V> object.
 * <p>
 *
 * <b>Test Matrix:</b>
 * <ul>
 * <li>testFutureIsDone - Asynchronous annotation at the class level on an EJB 3.1 Stateless Session ivSLRBean where
 * the called method returns results in a Future<V> object.
 * Verification will be done via checking the Future<V>.isDone() method prior to
 * Future<V>.get() method is called to retrieve returned results.
 * <li>testFutureGetBlocks - Asynchronous annotation at the class level on an EJB 3.1 Stateless Session ivSLRBean where
 * the called method returns results in a Future<V> object.
 * Verification will be done via the Future<V>.get() method to retrieve returned results,
 * this is done prior to the called method completion, Future<V>.get() will block
 * until completion.
 * <li>testFutureGetNoTimeout - Asynchronous annotation at the class level on an EJB 3.1 Stateless Session ivSLRBean where
 * the called method returns results in a Future<V> object.
 * Verification will be done via the Future<V>.get(timeout, unit) method to retrieve
 * returned results, this is done prior to the called method completion,
 * Future<V>.get(timeout, unit) will block until completion or timeout value
 * exceeded, timeout value will be greater than method completion time needed.
 * <li>testFutureGetTimesOut - Asynchronous annotation at the class level on an EJB 3.1 Stateless Session ivSLRBean where
 * the called method returns results in a Future<V> object.
 * Verification will be done via the Future<V>.get(timeout, unit) method to retrieve
 * returned results, this is done prior to the called method completion,
 * Future<V>.get() will block until completion or timeout value exceeded,
 * timeout value will be shorter than method completion time needed.
 * TimeoutException is expected to be signaled.
 * <li>testFutureGetNull - Asynchronous annotation at the class level on an EJB 3.1 Stateless Session ivSLRBean where
 * the called method returns null for results in a Future<V> object.
 * Verification will be done via the Future<V>.get() method to retrieve returned null,
 * this is done prior to the called method completion, Future<V>.get() will block
 * until completion.
 * <li>testFutureCompareClassloaders - Asynchronous annotation at the class level on an EJB 3.1 Stateless Session
 * ivSLRBean where the called method compares the ivSLRBean classloader to the contextclassloader and returns
 * results in a Future<V> object.
 * Verification will be done via the Future<V>.get() method to retrieve returned null,
 * this is done prior to the called method completion, Future<V>.get() will block
 * until completion.
 * </ul>
 */
@SuppressWarnings("serial")
@WebServlet("/ResultsStatelessRemoteServlet")
public class ResultsStatelessRemoteServlet extends FATServlet {
    private final static String CLASSNAME = ResultsStatelessRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // wait time used for future.isDone polling
    public static int waitTime = 400;

    @EJB
    ResultsStatelessRemote ivSLRBean;

    private ResultsStatelessRemote lookupSLRBean() throws Exception {
        return ivSLRBean;
    }

    /**
     * testFutureIsDone ()
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session ivSLRBean that returns results in a Future<String> object.
     * Verification will be done via checking the Future<V>.isDone() method prior to
     * Future<V>.get() method is called to retrieve returned results.
     */
    @Test
    public void testFutureIsDone() throws Exception {
        long currentThreadId = 0;
        int i = 0;

        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless ivSLRBean created successfully", bean);

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
        // update results for asynchronous work done
        assertEquals("Async Stateless bean method completed", "true", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessRemoteFutureBean.beanThreadId);

        // update results with current and bean method thread id comparison
        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessRemoteFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * testFutureGetBlocks ()
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean that returns results in a Future<String> object.
     * Verification will be done via the Future<V>.get() method to retrieve returned results,
     * this is done prior to the called method completion, Future<V>.get() will block
     * until completion.
     */
    @Test
    public void testFutureGetBlocks() throws Exception {
        long currentThreadId = 0;

        ResultsStatelessRemote bean = lookupSLRBean();
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
        svLogger.info("Bean threadId = " + ResultsStatelessRemoteFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessRemoteFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * testFutureGetNoTimeout ()
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean that returns results in a Future<String> object.
     * Verification will be done via the Future<V>.get(timeout, unit) method to retrieve
     * returned results, this is done prior to the called method completion,
     * Future<V>.get(timeout, unit) will block until completion or timeout value
     * exceeded, timeout value will be greater than method completion time needed.
     */
    @Test
    public void testFutureGetNoTimeout() throws Exception {
        long currentThreadId = 0;

        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults();

        // Wait for up to 10 secs
        svLogger.info("Retrieving results");
        String results = future.get(ResultsStatelessRemoteFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + results);
        assertEquals("Async Stateless Bean method completed", "true", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessRemoteFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessRemoteFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * testFutureGetTimesOut ()
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean that returns results in a Future<String> object.
     * Verification will be done via the Future<V>.get(timeout, unit) method to retrieve
     * returned results, this is done prior to the called method completion,
     * Future<V>.get(timeout, unit) will block until completion or timeout value
     * exceeded, timeout value will be shorter than method completion time needed.
     * TimeoutException is expected to be signaled.
     */
    @Test
    public void testFutureGetTimesOut() throws Exception {
        long currentThreadId = 0;

        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // initialize latches
        ResultsStatelessRemoteFutureBean.svBeanLatch = new CountDownLatch(1);
        ResultsStatelessRemoteFutureBean.svTestLatch = new CountDownLatch(1);

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
            ResultsStatelessRemoteFutureBean.svTestLatch.countDown();

            // wait to ensure async method has completed
            ResultsStatelessRemoteFutureBean.svBeanLatch.await(ResultsStatelessRemoteFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);
        }

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessRemoteFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method executed on separate thread", (ResultsStatelessRemoteFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * testFutureGetNull ()
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean that returns results in a Future<String> object.
     * Verification will be done via the Future<V>.get(timeout, unit) method to retrieve
     * returned results, this is done prior to the called method completion,
     * Future<V>.get(timeout, unit) will block until completion or timeout value
     * exceeded, timeout value will be greater than method completion time needed.
     */
    @Test
    public void testFutureGetNull() throws Exception {
        String results = "";
        long currentThreadId = 0;

        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndReturnResults_null();

        // Wait for up to 10 secs
        svLogger.info("Retrieving results");
        results = future.get(ResultsStatelessRemoteFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + results);
        assertNull("Async Stateless Bean method returned null", results);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessRemoteFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessRemoteFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * testFutureCompareClassloaders ()
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean that compares the bean classloader to the
     * contextclassloader and returns results in a Future<String> object. The classloaders
     * will not be equal, but loading the bean class from either will return the same class.
     * Verification will be done via the Future<V>.get(timeout, unit) method to retrieve
     * returned results, this is done prior to the called method completion,
     * Future<V>.get(timeout, unit) will block until completion or timeout value
     * exceeded, timeout value will be greater than method completion time needed.
     */
    @Test
    public void testFutureCompareClassloaders() throws Exception {
        Boolean results = Boolean.FALSE;
        long currentThreadId = 0;

        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Asynch Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<Boolean> future = bean.test_fireAndReturnResults_classloader();

        // Wait for up to 10 secs
        svLogger.info("Retrieving results");
        results = future.get(ResultsStatelessRemoteFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + results.toString());
        assertTrue("Async Stateless Bean method returned true", results.booleanValue());

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + ResultsStatelessRemoteFutureBean.beanThreadId);

        assertFalse("Async Stateless Bean method completed on separate thread", (ResultsStatelessRemoteFutureBean.beanThreadId == currentThreadId));
    }

    /**
     * This test verifies that the call to get() returns immediately after it
     * has been previously successfully called.
     *
     * <p>An asynchronous method is called, and then get() is called twice on
     * the Future.
     *
     * <p>The expected result is that the first call to get() will block until
     * the results are available, but the second call will return immediately.
     */
    @Test
    public void testFutureReGet() throws Exception {
        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        Future<String> future = bean.test_fireAndReturnResults();
        Object result1 = future.get();

        long before = System.currentTimeMillis();
        Object result2 = future.get();
        long after = System.currentTimeMillis();

        assertTrue("second get is immediate (before=" + before + ", after=" + after + ")", after - before < ResultsStatelessRemoteFutureBean.MAX_ASYNC_WAIT);
        assertSame("result objects are the same", result1, result2);
    }

    /**
     * This test verifies that the call to get(long, TimeUnit) returns
     * immediately after it has been previously successfully called.
     *
     * <p>An asynchronous method is called, and then get(long, TimeUnit) is
     * called twice on the Future.
     *
     * <p>The expected result is that the first call to get(long, TimeUnit)
     * will block until the results are available, but the second call will
     * return immediately.
     */
    @Test
    public void testFutureReGetNoTimeout() throws Exception {
        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        Future<String> future = bean.test_fireAndReturnResults();
        Object result1 = future.get(ResultsStatelessRemoteFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        long before = System.currentTimeMillis();
        Object result2 = future.get(0, TimeUnit.MILLISECONDS);
        long after = System.currentTimeMillis();

        assertTrue("second get is immediate (before=" + before + ", after=" + after + ")", after - before < ResultsStatelessRemoteFutureBean.MAX_ASYNC_WAIT);
        assertSame("result objects are the same", result1, result2);
    }

    /**
     * This test verifies that a call to isDone() returns true when called after
     * get() has been previously successfully called.
     *
     * <p>An asynchronous method is called, then get() is called, then isDone()
     * is called twice, then get() is called again, then isDone() is called
     * twice.
     *
     * <p>The expected result is that isDone() should always return true after
     * get() has been called successfully.
     */
    @Test
    public void testFutureDoneAfterGet() throws Exception {
        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        Future<String> future = bean.test_fireAndReturnResults();
        future.get();

        assertTrue("done after get", future.isDone());
        assertTrue("still done after get", future.isDone());

        future.get();
        assertTrue("done after reget", future.isDone());
        assertTrue("still done after reget", future.isDone());
    }

    /**
     * This test verifies that a call to isDone() returns true when called after
     * get(long, TimeUnit) has been previously successfully called.
     *
     * <p>An asynchronous method is called, then get(long, TimeUnit) is called,
     * then isDone() is called twice, then get(long, TimeUnit) is called again,
     * then isDone() is called
     * twice.
     *
     * <p>The expected result is that isDone() should always return true after
     * get(long, TimeUnit) has been called successfully.
     */
    @Test
    public void testFutureDoneAfterGetNoTimeout() throws Exception {
        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        Future<String> future = bean.test_fireAndReturnResults();
        future.get(ResultsStatelessRemoteFutureBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        assertTrue("done after get", future.isDone());
        assertTrue("still done after get", future.isDone());

        future.get(0, TimeUnit.MILLISECONDS);
        assertTrue("done after reget", future.isDone());
        assertTrue("still done after reget", future.isDone());
    }

    /**
     * This tests verifies that calling isDone() returns false until get()
     * returns successfully.
     *
     * <p>An asynchronous method is called, get(0, TimeUnit) is called,
     * isDone() is called, get() is called, and isDone() is called.
     *
     * <p>The expected result is that the call to get(0, TimeUnit) will throw a
     * TimeoutException immediately, the call to isDone() will return false,
     * the call to get() will block until the result is available, and then
     * isDone() will return true.
     */
    @Test
    public void testFutureNotDoneAfterGetTimesOut() throws Exception {
        ResultsStatelessRemote bean = lookupSLRBean();
        assertNotNull("Async Stateless Bean created successfully", bean);

        // initialize latch
        ResultsStatelessRemoteFutureBean.svTestLatch = new CountDownLatch(1);

        Future<String> future = bean.test_fireAndReturnResults_await();

        try {
            future.get(0, TimeUnit.MILLISECONDS);
            fail("Expected TimeoutException did not occur");
        } catch (TimeoutException te) {
            // timeout exceeded as expected
            svLogger.info("received expected exception: " + te);

            assertFalse("not done after get", future.isDone());
        } finally {
            // allow async method to run
            ResultsStatelessRemoteFutureBean.svTestLatch.countDown();
        }

        future.get();
        assertTrue("done after reget", future.isDone());
        assertTrue("still done after reget", future.isDone());
    }
}