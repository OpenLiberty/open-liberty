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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.BasicSingletonLocal;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.BasicSingletonLocalBean;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.BasicStatefulLocal;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.BasicStatefulLocalBean;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.BasicStatelessLocal;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.BasicStatelessLocalBean;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.Style2XMLBean;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.Style2XMLLocal;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.Style3XMLBean;
import com.ibm.ws.ejbcontainer.async.fat.xml.ejb.Style3XMLLocal;

import componenttest.app.FATServlet;

/**
 * Tests the basic fire-and-forget async method behavior for singleton,
 * stateful and stateless session beans defined in XML. <p>
 *
 * This test has been derived from 3 different suites in the r80-asynch FAT
 * bucket originally implemented for traditional WAS : singleton, sla, sfa.
 */
@WebServlet("/BasicXmlServlet")
@SuppressWarnings("serial")
public class BasicXmlServlet extends FATServlet {
    public final static String CLASS_NAME = BasicXmlServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    BasicStatelessLocal ivSLLBean;

    @EJB
    Style2XMLLocal ivSLL2Bean;

    @EJB
    Style3XMLLocal ivSLL3Bean;

    @EJB
    BasicSingletonLocal ivSGLBean;

    private BasicStatelessLocal lookupSLLBean() throws Exception {
        return ivSLLBean;
    }

    private Style2XMLLocal lookupSLL2Bean() throws Exception {
        return ivSLL2Bean;
    }

    private Style3XMLLocal lookupSLL3Bean() throws Exception {
        return ivSLL3Bean;
    }

    private BasicStatefulLocal lookupSFLBean() throws Exception {
        return (BasicStatefulLocal) new InitialContext().lookup("java:app/AsyncTestEJB-Xml/BasicStatefulLocalBean");
    }

    private BasicSingletonLocal lookupSGLBean() throws Exception {
        return ivSGLBean;
    }

    /**
     * Test calling a method where the bean's xml uses style1 (i.e. the wildcard * for the
     * method name) to define all business methods to be asynchronous.
     */
    @Test
    public void testSLLStyle1Xml() throws Exception {
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
     * Verify that by using Style2 XML to mark all methods named test_xmlStyle2
     * to be asynchronous that those methods really are asynchronous regardless
     * of their method signature. Also verify that methods not named test_xmlStyle2
     * are synchronous.
     *
     * Local interface version.
     *
     * @throws Exception
     */
    @Test
    public void testSLLStyle2Xml() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        Style2XMLLocal bean = lookupSLL2Bean();
        assertNotNull("Bean created successfully", bean);

        CountDownLatch beanLatch = bean.getBeanLatch();

        // call the method that should be synchronous
        bean.test_notCoveredByStyle2();

        // wait for work to complete in case it ran asynchronously
        beanLatch.await(Style2XMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> Style2XMLBean.syncMethThreadId = " + Style2XMLBean.syncMethThreadId + "  currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId,
        // they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test:" +
                     " syncMethThreadId = " + Style2XMLBean.syncMethThreadId +
                     ", currentThreadId = " + currentThreadId, Style2XMLBean.syncMethThreadId, currentThreadId);

        beanLatch = bean.getBeanLatch();

        // call the first method that should be asynchronous
        bean.test_xmlStyle2();

        // wait for asynchronous work to complete
        beanLatch.await(Style2XMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> Style2XMLBean.asyncMeth1ThreadId = " + Style2XMLBean.asyncMeth1ThreadId + "  currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread,
        // they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test:" +
                   " asyncMeth1ThreadId = " + Style2XMLBean.asyncMeth1ThreadId +
                   ", currentThreadId = " + currentThreadId, (Style2XMLBean.asyncMeth1ThreadId != currentThreadId));

        beanLatch = bean.getBeanLatch();

        // call the second method that should be asynchronous
        bean.test_xmlStyle2("style2");

        // wait for asynchronous work to complete
        beanLatch.await(Style2XMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> Style2XMLBean.asyncMeth2ThreadId = " + Style2XMLBean.asyncMeth2ThreadId + "  currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread,
        // they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test:" +
                   " asyncMeth2ThreadId = " + Style2XMLBean.asyncMeth2ThreadId +
                   ", currentThreadId = " + currentThreadId, (Style2XMLBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify that by using Style3 XML to mark a method named test_xmlStyle3 which
     * takes a String as a parameter it is treated as an asynchronous method. Also
     * tests that a method with the same name but different method signature is treated
     * as a synchronous method as well as a method with a different name but the
     * same method signature will be synchronous.
     *
     * Local interface version.
     *
     * @throws Exception
     */
    @Test
    public void testSLLStyle3Xml() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        Style3XMLLocal bean = lookupSLL3Bean();
        assertNotNull("Bean created successfully", bean);

        CountDownLatch beanLatch = bean.getBeanLatch();

        // call the method that should be synchronous
        bean.test_xmlStyle3();

        // wait for work to complete in case it ran asynchronously
        beanLatch.await(Style2XMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> Style3XMLBean.syncMethThreadId = " + Style3XMLBean.syncMethThreadId + "  currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId,
        // they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test:" +
                     " syncMethThreadId = " + Style3XMLBean.syncMethThreadId +
                     ", currentThreadId = " + currentThreadId, Style3XMLBean.syncMethThreadId, currentThreadId);

        beanLatch = bean.getBeanLatch();

        // call the method that should be synchronous
        bean.test_diffNameSameParam("synchronous");

        // wait for work to complete in case it ran asynchronously
        beanLatch.await(Style2XMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> Style3XMLBean.syncMeth2ThreadId = " + Style3XMLBean.syncMeth2ThreadId + "  currentThreadId = " + currentThreadId);

        // compare the threadId the method was run in and the current threadId,
        // they should match since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test:" +
                     " syncMeth2ThreadId = " + Style3XMLBean.syncMeth2ThreadId +
                     ", currentThreadId = " + currentThreadId, Style3XMLBean.syncMeth2ThreadId, currentThreadId);

        beanLatch = bean.getBeanLatch();

        // call the method which has a parameter and should be asynchronous
        bean.test_xmlStyle3("style3");

        // wait for asynchronous work to complete
        beanLatch.await(Style2XMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> Style3XMLBean.asyncMethThreadId = " + Style3XMLBean.asyncMethThreadId + "  currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread,
        // they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread as the test:" +
                   " asyncMethThreadId = " + Style3XMLBean.asyncMethThreadId +
                   ", currentThreadId = " + currentThreadId, (Style3XMLBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Test calling a method, defined via Style 1 XML to be Asynchronous,
     * on an EJB 3.1 Singleton Session Bean.
     */
    @Test
    public void testSGLStyle1Xml() throws Exception {
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
     * Test calling a method that is defined in XML to be Asynchronous at the method level
     * on an EJB 3.1 Stateful Session Bean.
     */
    @Test
    public void testSFLMethXml() throws Exception {
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