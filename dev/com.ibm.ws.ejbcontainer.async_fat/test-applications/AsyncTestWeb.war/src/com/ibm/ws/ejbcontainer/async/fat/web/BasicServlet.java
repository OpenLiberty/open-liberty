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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.BasicSingletonLocal;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.BasicSingletonLocalBean;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.BasicStatefulLocal;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.BasicStatefulLocalBean;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.BasicStatelessLocal;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.BasicStatelessLocal2;
import com.ibm.ws.ejbcontainer.async.fat.ann.ejb.BasicStatelessLocalBean;

import componenttest.app.FATServlet;

/**
 * Tests the basic fire-and-forget async method behavior for singleton,
 * stateful and stateless session beans. <p>
 *
 * This test has been derived from 3 different suites in the r80-asynch FAT
 * bucket originally implemented for traditional WAS : singleton, sla, sfa.
 */
@WebServlet("/BasicServlet")
@SuppressWarnings("serial")
public class BasicServlet extends FATServlet {
    public final static String CLASS_NAME = BasicServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    BasicStatelessLocal ivSLLBean;

    @EJB
    BasicStatelessLocal2 ivSLLBean2;

    @EJB
    BasicSingletonLocal ivSGLBean;

    private BasicStatelessLocal lookupSLLBean() throws Exception {
        return ivSLLBean;
    }

    private BasicStatelessLocal2 lookupSLLBean2() throws Exception {
        return ivSLLBean2;
    }

    private BasicStatefulLocal lookupSFLBean() throws Exception {
        return (BasicStatefulLocal) new InitialContext().lookup("java:app/AsyncTestEJB-Ann/BasicStatefulLocalBean");
    }

    private BasicSingletonLocal lookupSGLBean() throws Exception {
        return ivSGLBean;
    }

    /**
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Stateless Session Bean.
     */
    @Test
    public void testSLLClassAnn() throws Exception {
        long currentThreadId = 0;

        BasicStatelessLocal bean = lookupSLLBean();

        // update results with bean creation
        assertNotNull("Async Stateless Bean created successfully", bean);

        // call bean asynchronous method
        bean.test_fireAndForget();

        // wait for async work to complete
        BasicStatelessLocalBean.svBeanLatch.await(BasicStatelessLocalBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + BasicStatelessLocalBean.asyncWorkDone);
        // update results for asynchronous work done
        assertTrue("Async Stateless Bean method completed", BasicStatelessLocalBean.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + BasicStatelessLocalBean.beanThreadId);

        // update results with current and bean method thread id comparison
        assertFalse("Async Stateless Bean method completed on separate thread", (BasicStatelessLocalBean.beanThreadId == currentThreadId));
    }

    /**
     * Test calling a method with an Asynchronous annotation at the class level
     * on an EJB 3.1 Singleton Session Bean.
     */
    @Test
    public void testSGLClassAnn() throws Exception {
        long currentThreadId = 0;

        BasicSingletonLocal bean = lookupSGLBean();

        // update results with bean creation
        assertNotNull("Async Singleton Bean created successfully", bean);

        // call bean asynchronous method
        bean.test_fireAndForget();

        // wait for async work to complete
        BasicSingletonLocalBean.svBeanLatch.await(BasicSingletonLocalBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + BasicSingletonLocalBean.asyncWorkDone);
        // update results for asynchronous work done
        assertTrue("Async Singleton Bean method completed", BasicSingletonLocalBean.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + BasicSingletonLocalBean.beanThreadId);

        // update results with current and bean method thread id comparison
        assertFalse("Async Singleton Bean method completed on separate thread", (BasicSingletonLocalBean.beanThreadId == currentThreadId));
    }

    /**
     * This test ensures that interceptors are called when they are declared
     * for an asynchronous method.
     */
    @Test
    public void testSLLInterceptor() throws Exception {
        BasicStatelessLocal bean = lookupSLLBean();
        List<String> value = new ArrayList<String>();
        bean.test_interceptor(value).get();
        assertEquals(Arrays.asList("interceptor", "bean", "method"), value);
    }

    /**
     * This test ensures that getInvokedBusinessInterface returns the proper
     * value in an asynchronous method when the method exists on multiple
     * local interfaces defined by the same bean.
     */
    @Test
    public void testSLLGetInvokedBusinessInterface() throws Exception {
        BasicStatelessLocal bean = lookupSLLBean();
        assertEquals(BasicStatelessLocal.class, bean.test_getInvokedBusinessInterface().get());

        BasicStatelessLocal2 bean2 = lookupSLLBean2();
        assertEquals(BasicStatelessLocal2.class, bean2.test_getInvokedBusinessInterface().get());
    }

    /**
     * Test calling a method with an Asynchronous annotation at the method level
     * on an EJB 3.1 Stateful Session Bean.
     */
    @Test
    public void testSFLMethAnn() throws Exception {
        long currentThreadId = 0;

        BasicStatefulLocal bean = lookupSFLBean();

        // update results with bean creation
        assertNotNull("Async Stateful Bean created successfully", bean);

        // call bean asynchronous method
        bean.test_fireAndForget();

        // wait for async work to complete
        BasicStatefulLocalBean.svBeanLatch.await(BasicStatefulLocalBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + BasicStatefulLocalBean.asyncWorkDone);
        // update results for asynchronous work done
        assertTrue("Async Stateful Bean method completed", BasicStatefulLocalBean.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + BasicStatefulLocalBean.beanThreadId);

        // update results with current and bean method thread id comparison
        assertFalse("Async Stateful Bean method completed on separate thread", (BasicStatefulLocalBean.beanThreadId == currentThreadId));
    }
}