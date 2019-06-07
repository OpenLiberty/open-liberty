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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.AsyncApplicationException;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.AsyncApplicationException2;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.ExceptionStatefulLocal;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.ExceptionStatefulLocalFutureBean;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.ExceptionStatelessLocal;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.ExceptionStatelessLocal2;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.ExceptionStatelessLocalFutureBean;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@WebServlet("/ExceptionServlet")
@SuppressWarnings("serial")
public class ExceptionServlet extends FATServlet {
    public final static String CLASSNAME = ExceptionServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB
    ExceptionStatelessLocal ivSLLBean;

    @EJB
    ExceptionStatelessLocal2 ivSLLBean2;

    private ExceptionStatelessLocal lookupSLLBean() throws Exception {
        return ivSLLBean;
    }

    private ExceptionStatelessLocal2 lookupSLLBean2() throws Exception {
        return ivSLLBean2;
    }

    private ExceptionStatefulLocal lookupSFLBean() throws Exception {
        return (ExceptionStatefulLocal) new InitialContext().lookup("java:app/AsyncTestEJB-Ann/ExceptionStatefulLocalFutureBean");
    }

    /**
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean that throws a system exception.
     * Verification will be done via the Future<V>.get() method to retrieve
     * the thrown exception. Future<V>.get() will block until completion.
     */
    @Test
    @ExpectedFFDC("java.lang.RuntimeException")
    public void testSLLFutureThrowSystemException() throws Exception {
        String results = "false";
        long currentThreadId = 0;

        ExceptionStatelessLocal bean = lookupSLLBean();

        // update results with bean creation
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndThrowSystemException();

        try {
            svLogger.info("Retrieving results");
            results = future.get();
            fail("Did not catch expected ExecutionException : " + results);
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            Throwable root1 = ee.getCause();
            assertTrue("Nested exception is an EJBException", root1 instanceof EJBException);
            assertTrue("Nested exception is a RuntimeException", root1.getCause() instanceof RuntimeException);

            // get current thread Id for comparison to bean method thread id
            currentThreadId = Thread.currentThread().getId();

            svLogger.info("Test threadId = " + currentThreadId);
            svLogger.info("Bean threadId = " + ExceptionStatelessLocalFutureBean.beanThreadId);

            // update results with current and bean method thread id comparison
            assertThat("Async Stateless Bean method completed on separate thread", ExceptionStatelessLocalFutureBean.beanThreadId, is(not(currentThreadId)));
        }
    }

    /**
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateful Session Bean that throws a system exception.
     * Verification will be done via the Future<V>.get() method to retrieve
     * the thrown exception. Future<V>.get() will block until completion.
     */
    @Test
    @ExpectedFFDC("java.lang.RuntimeException")
    public void testSFLFutureThrowSystemException() throws Exception {
        String results = "false";
        long currentThreadId = 0;

        ExceptionStatefulLocal bean = lookupSFLBean();

        // update results with bean creation
        assertNotNull("Async Stateful Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndThrowSystemException();

        try {
            svLogger.info("Retrieving results");
            results = future.get();
            fail("Did not catch expected ExecutionException : " + results);
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            Throwable root1 = ee.getCause();
            assertTrue("Nested exception is an EJBException", root1 instanceof EJBException);
            assertTrue("Nested exception is a RuntimeException", root1.getCause() instanceof RuntimeException);

            // get current thread Id for comparison to bean method thread id
            currentThreadId = Thread.currentThread().getId();

            svLogger.info("Test threadId = " + currentThreadId);
            svLogger.info("Bean threadId = " + ExceptionStatefulLocalFutureBean.beanThreadId);

            // update results with current and bean method thread id comparison
            assertThat("Async Stateful Bean method completed on separate thread", ExceptionStatefulLocalFutureBean.beanThreadId, is(not(currentThreadId)));
        }
    }

    /**
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean that throws an application exception.
     * Verification will be done via the Future<V>.get() method to retrieve
     * the thrown exception. Future<V>.get() will block until completion.
     */
    @Test
    public void testSLLFutureThrowApplicationException() throws Exception {
        String results = "false";
        long currentThreadId = 0;

        ExceptionStatelessLocal bean = lookupSLLBean();

        // update results with bean creation
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndThrowApplicationException();

        try {
            svLogger.info("Retrieving results");
            results = future.get();
            fail("Did not catch expected ExecutionException : " + results);
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            assertTrue("Nested exception is an AsyncApplicationException", ee.getCause() instanceof AsyncApplicationException);

            // get current thread Id for comparison to bean method thread id
            currentThreadId = Thread.currentThread().getId();

            svLogger.info("Test threadId = " + currentThreadId);
            svLogger.info("Bean threadId = " + ExceptionStatelessLocalFutureBean.beanThreadId);

            // update results with current and bean method thread id comparison
            assertThat("Async Stateless Bean method completed on separate thread", ExceptionStatelessLocalFutureBean.beanThreadId, is(not(currentThreadId)));
        }
    }

    /**
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateful Session Bean that throws an application exception.
     * Verification will be done via the Future<V>.get() method to retrieve
     * the thrown exception. Future<V>.get() will block until completion.
     */
    @Test
    public void testSFLFutureThrowApplicationException() throws Exception {
        String results = "false";
        long currentThreadId = 0;

        ExceptionStatefulLocal bean = lookupSFLBean();

        // update results with bean creation
        assertNotNull("Async Stateful Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        Future<String> future = bean.test_fireAndThrowApplicationException();

        try {
            svLogger.info("Retrieving results");
            results = future.get();
            fail("Did not catch expected ExecutionException : " + results);
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            assertTrue("Nested exception is an AsyncApplicationException", ee.getCause() instanceof AsyncApplicationException);

            // get current thread Id for comparison to bean method thread id
            currentThreadId = Thread.currentThread().getId();

            svLogger.info("Test threadId = " + currentThreadId);
            svLogger.info("Bean threadId = " + ExceptionStatefulLocalFutureBean.beanThreadId);

            // update results with current and bean method thread id comparison
            assertThat("Async Stateless Bean method completed on separate thread", ExceptionStatefulLocalFutureBean.beanThreadId, is(not(currentThreadId)));
        }
    }

    /**
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean and an AroundInvoke interceptor that
     * throws an application exception. Verification will be done via the
     * Future<V>.get() method to retrieve the thrown exception. Future<V>.get()
     * will block until completion.
     */
    @Test
    public void testSLLFutureWithAroundInvokeThrowApplicationException() throws Exception {
        String results = "false";
        long currentThreadId = 0;

        ExceptionStatelessLocal bean = lookupSLLBean();

        // update results with bean creation
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method using Future<V> object to receive results
        List<String> value = new ArrayList<String>();
        Future<String> future = bean.test_fireAroundInvokeAndThrowApplicationException(value);

        try {
            svLogger.info("Retrieving results");
            results = future.get();
            fail("Did not catch expected ExecutionException : " + results);
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            assertTrue("Nested exception is an AsyncApplicationException", ee.getCause() instanceof AsyncApplicationException);

            // get current thread Id for comparison to bean method thread id
            currentThreadId = Thread.currentThread().getId();

            svLogger.info("Test threadId = " + currentThreadId);
            svLogger.info("Bean threadId = " + ExceptionStatelessLocalFutureBean.beanThreadId);

            // update results with current and bean method thread id comparison
            assertThat("Async Stateless Bean method completed on separate thread", ExceptionStatelessLocalFutureBean.beanThreadId, is(not(currentThreadId)));
            assertEquals(Arrays.asList("interceptor", "method"), value);
        }
    }

    /**
     * This test ensures that the declared application exceptions for an
     * asynchronous method are determined by the throws clause of the interface
     * being invoked and that they are not determined by the bean class or by
     * an arbitrary interface.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.ejbcontainer.async.fat.ann.ejb.AsyncApplicationException", "com.ibm.ws.ejbcontainer.async.fat.ann.ejb.AsyncApplicationException2" })
    public void testSLLTwoInterfaces() throws Exception {
        ExceptionStatelessLocal bean = lookupSLLBean();
        Future<Void> fFuture = bean.test_exception(false);
        try {
            fFuture.get();
            fail("Did not catch expected ExecutionException");
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            assertTrue("Nested exception is an AsyncApplicationException", ee.getCause() instanceof AsyncApplicationException);
        }

        Future<Void> tFuture = bean.test_exception(true);
        try {
            tFuture.get();
            fail("Did not catch expected ExecutionException");
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            assertTrue("Nested exception is an EJBException", ee.getCause() instanceof EJBException);
        }

        ExceptionStatelessLocal2 bean2 = lookupSLLBean2();
        Future<Void> fFuture2 = bean2.test_exception(false);
        try {
            fFuture2.get();
            fail("Did not catch expected ExecutionException");
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            assertTrue("Nested exception is an EJBException", ee.getCause() instanceof EJBException);
        }

        Future<Void> tFuture2 = bean2.test_exception(true);
        try {
            tFuture2.get();
            fail("Did not catch expected ExecutionException");
        } catch (ExecutionException ee) {
            // exception received as expected
            svLogger.info("caught expected ExecutionException");
            assertTrue("Nested exception is an AsyncApplicationException2", ee.getCause() instanceof AsyncApplicationException2);
        }
    }
}