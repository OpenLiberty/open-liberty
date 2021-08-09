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

import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceAnnLocal;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceSingletonAnnBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceSingletonXMLBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceStatefulAnnBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceStatefulXMLBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceStatelessAnnBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceStatelessXMLBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceXMLLocal;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/AsyncInheritanceMixServlet")
public class AsyncInheritanceMixServlet extends FATServlet {
    private final static String CLASSNAME = AsyncInheritanceMixServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB(beanName = "InheritanceSingletonAnnBean")
    InheritanceAnnLocal inheritanceSingletonAnnBean;

    @EJB(beanName = "InheritanceSingletonXMLBean")
    InheritanceXMLLocal inheritanceSingletonXMLBean;

    @EJB(beanName = "InheritanceStatefulAnnBean")
    InheritanceAnnLocal inheritanceStatefulAnnBean;

    @EJB(beanName = "InheritanceStatefulXMLBean")
    InheritanceXMLLocal inheritanceStatefulXMLBean;

    @EJB(beanName = "InheritanceStatelessAnnBean")
    InheritanceAnnLocal inheritanceStatelessAnnBean;

    @EJB(beanName = "InheritanceStatelessXMLBean")
    InheritanceXMLLocal inheritanceStatelessXMLBean;

    private InheritanceAnnLocal lookupInheritanceSingletonAnnBean() throws Exception {
        return inheritanceSingletonAnnBean;
    }

    private InheritanceXMLLocal lookupInheritanceSingletonXMLBean() throws Exception {
        return inheritanceSingletonXMLBean;
    }

    private InheritanceAnnLocal lookupInheritanceStatefulAnnBean() throws Exception {
        return inheritanceStatefulAnnBean;
    }

    private InheritanceXMLLocal lookupInheritanceStatefulXMLBean() throws Exception {
        return inheritanceStatefulXMLBean;
    }

    private InheritanceAnnLocal lookupInheritanceStatelessAnnBean() throws Exception {
        return inheritanceStatelessAnnBean;
    }

    private InheritanceXMLLocal lookupInheritanceStatelessXMLBean() throws Exception {
        return inheritanceStatelessXMLBean;
    }

    /**
     * Scenario:
     * Class C has a method m(). There are no asynchronous annotations
     * in class C.
     * Bean class A extends class C. Bean A does NOT override m() but
     * does use Style 1 XML (i.e. * for method-name) to define all methods
     * of A to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatefulInheritanceXML() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceXMLLocal bean = lookupInheritanceStatefulXMLBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceStatefulXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the super class method that should be asynchronous due to style1 XML
        bean.test_inheritance();

        // Wait for test_inheritance() to complete
        InheritanceStatefulXMLBean.svInheritanceLatch.await(InheritanceStatefulXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatefulXMLBean.superMethThreadId = " + InheritanceStatefulXMLBean.superMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: superMethThreadId = " + InheritanceStatefulXMLBean.superMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatefulXMLBean.superMethThreadId != currentThreadId));

        // Reset the latch
        InheritanceStatefulXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous
        bean.test_beanMethodAsync("Method should be asynchronous.");

        // Wait for test_beanMethodAsync() to complete
        InheritanceStatefulXMLBean.svInheritanceLatch.await(InheritanceStatefulXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatefulXMLBean.asyncMethThreadId = " + InheritanceStatefulXMLBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + InheritanceStatefulXMLBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatefulXMLBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Scenario:
     * Class C has a method m(). There are no asynchronous annotations
     * in class C.
     * Bean class B extends class C. Bean B does NOT override m() but
     * does have a class level Asynchronous annotation to define all methods
     * of B to be asynchronous. This class level annotation should not
     * pertain to methods in the super class.
     *
     * Method m() should be synchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatefulInheritanceAnn() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceAnnLocal bean = lookupInheritanceStatefulAnnBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceStatefulAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the super class method that should be synchronous
        bean.test_inheritance();

        // Wait for test_inheritance() to complete
        InheritanceStatefulAnnBean.svInheritanceLatch.await(InheritanceStatefulAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatefulAnnBean.superMethThreadId = " + InheritanceStatefulAnnBean.superMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: superMethThreadId = " + InheritanceStatefulAnnBean.superMethThreadId
                     + ", currentThreadId = " + currentThreadId, InheritanceStatefulAnnBean.superMethThreadId, currentThreadId);

        // Reset the latch
        InheritanceStatefulAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous
        bean.test_beanMethodAsync("Method should be asynchronous.");

        // Wait for test_beanMethodAsync() to complete
        InheritanceStatefulAnnBean.svInheritanceLatch.await(InheritanceStatefulAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatefulAnnBean.asyncMethThreadId = " + InheritanceStatefulAnnBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + InheritanceStatefulAnnBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatefulAnnBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Scenario:
     * Class C has a method m(). There are no asynchronous annotations
     * in class C.
     * Bean class A extends class C. Bean A does NOT override m() but
     * does use Style 1 XML (i.e. * for method-name) to define all methods
     * of A to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testSingletonInheritanceXML() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceXMLLocal bean = lookupInheritanceSingletonXMLBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceSingletonXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the super class method that should be asynchronous due to style1 XML
        bean.test_inheritance();

        // Wait for test_inheritance() to complete
        InheritanceSingletonXMLBean.svInheritanceLatch.await(InheritanceSingletonXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceSingletonXMLBean.superMethThreadId = " + InheritanceSingletonXMLBean.superMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: superMethThreadId = " + InheritanceSingletonXMLBean.superMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceSingletonXMLBean.superMethThreadId != currentThreadId));

        // Reset the latch
        InheritanceSingletonXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous
        bean.test_beanMethodAsync("Method should be asynchronous.");

        // Wait for test_beanMethodAsync() to complete
        InheritanceSingletonXMLBean.svInheritanceLatch.await(InheritanceSingletonXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceSingletonXMLBean.asyncMethThreadId = " + InheritanceSingletonXMLBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + InheritanceSingletonXMLBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceSingletonXMLBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Scenario:
     * Class C has a method m(). There are no asynchronous annotations
     * in class C.
     * Bean class B extends class C. Bean B does NOT override m() but
     * does have a class level Asynchronous annotation to define all methods
     * of B to be asynchronous. This class level annotation should not
     * pertain to methods in the super class.
     *
     * Method m() should be synchronous.
     *
     * @throws Exception
     */
    @Test
    public void testSingletonInheritanceAnn() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceAnnLocal bean = lookupInheritanceSingletonAnnBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceSingletonAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the super class method that should be synchronous
        bean.test_inheritance();

        // Wait for test_inheritance() to complete
        InheritanceSingletonAnnBean.svInheritanceLatch.await(InheritanceSingletonAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceSingletonAnnBean.superMethThreadId = " + InheritanceSingletonAnnBean.superMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: superMethThreadId = " + InheritanceSingletonAnnBean.superMethThreadId
                     + ", currentThreadId = " + currentThreadId, InheritanceSingletonAnnBean.superMethThreadId, currentThreadId);

        // Reset the latch
        InheritanceSingletonAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous
        bean.test_beanMethodAsync("Method should be asynchronous.");

        // Wait for test_beanMethodAsync() to complete
        InheritanceSingletonAnnBean.svInheritanceLatch.await(InheritanceSingletonAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceSingletonAnnBean.asyncMethThreadId = " + InheritanceSingletonAnnBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + InheritanceSingletonAnnBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceSingletonAnnBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Scenario:
     * Class C has a method m(). There are no asynchronous annotations
     * in class C.
     * Bean class A extends class C. Bean A does NOT override m() but
     * does use Style 1 XML (i.e. * for method-name) to define all methods
     * of A to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatelessInheritanceXML() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceXMLLocal bean = lookupInheritanceStatelessXMLBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceStatelessXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the super class method that should be asynchronous due to style1 XML
        bean.test_inheritance();

        // Wait for test_inheritance() to complete
        InheritanceStatelessXMLBean.svInheritanceLatch.await(InheritanceStatelessXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatelessXMLBean.superMethThreadId = " + InheritanceStatelessXMLBean.superMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: superMethThreadId = " + InheritanceStatelessXMLBean.superMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatelessXMLBean.superMethThreadId != currentThreadId));

        // Reset the latch
        InheritanceStatelessXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous
        bean.test_beanMethodAsync("Method should be asynchronous.");

        // Wait for test_beanMethodAsync() to complete
        InheritanceStatelessXMLBean.svInheritanceLatch.await(InheritanceStatelessXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatelessXMLBean.asyncMethThreadId = " + InheritanceStatelessXMLBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + InheritanceStatelessXMLBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatelessXMLBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Scenario:
     * Class C has a method m(). There are no asynchronous annotations
     * in class C.
     * Bean class B extends class C. Bean B does NOT override m() but
     * does have a class level Asynchronous annotation to define all methods
     * of B to be asynchronous. This class level annotation should not
     * pertain to methods in the super class.
     *
     * Method m() should be synchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatelessInheritanceAnn() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceAnnLocal bean = lookupInheritanceStatelessAnnBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceStatelessAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the super class method that should be synchronous
        bean.test_inheritance();

        // Wait for test_inheritance() to complete
        InheritanceStatelessAnnBean.svInheritanceLatch.await(InheritanceStatelessAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatelessAnnBean.superMethThreadId = " + InheritanceStatelessAnnBean.superMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId; they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: superMethThreadId = " + InheritanceStatelessAnnBean.superMethThreadId
                     + ", currentThreadId = " + currentThreadId, InheritanceStatelessAnnBean.superMethThreadId, currentThreadId);

        // Reset the latch
        InheritanceStatelessAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method which has a parameter and should be asynchronous
        bean.test_beanMethodAsync("Method should be asynchronous.");

        // Wait for test_beanMethodAsync() to complete
        InheritanceStatelessAnnBean.svInheritanceLatch.await(InheritanceStatelessAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceAnnBean.asyncMethThreadId = " + InheritanceStatelessAnnBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test: asyncMethThreadId = " + InheritanceStatelessAnnBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatelessAnnBean.asyncMethThreadId != currentThreadId));
    }
}