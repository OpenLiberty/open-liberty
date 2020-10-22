/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.noparam.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.auto.noparam.ejb.Intf;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public abstract class NoParamAbstractServlet extends FATServlet {
    static final long MAX_TIMER_WAIT_TIME = 60 * 1000; // 1 minute

    public abstract void setup() throws Exception;

    /**
     * Starts the timer for the bean and waits for it to execute.
     */
    protected abstract void startTimer(Intf bean);

    /**
     * Returns true if the timer method with the specified number of parameters
     * was executed.
     */
    protected abstract boolean isTimerMethodExecuted(Intf bean, int methodId);

    protected final Intf lookupBean(String name) throws NamingException {
        Intf bean = (Intf) (new InitialContext()).lookup("java:global/NoParamTimerApp/NoParamTimerEJB/" + name);
        startTimer(bean);
        return bean;
    }

    public void cleanup() throws Exception {
        lookupBean("AnnotationBean").clearAllTimers();
        lookupBean("PrivateAnnotationBean").clearAllTimers();
        lookupBean("NoParamsZeroParamsXMLBean").clearAllTimers();
        lookupBean("NoParamsOneParamXMLBean").clearAllTimers();
        lookupBean("NoParamsHierarchyXMLBean").clearAllTimers();
        lookupBean("EmptyParamsZeroParamsXMLBean").clearAllTimers();
        lookupBean("EmptyParamsHierarchyXMLBean").clearAllTimers();
        lookupBean("NoParamsMixedBean").clearAllTimers();
        lookupBean("EmptyParamsMixedBean").clearAllTimers();
    }

    /**
     * This test verifies that the Timeout annotation works on a method with no
     * parameters. A bean is looked up, a 0-second timer is started, and then
     * the test waits for it to fire. The expected result is that the timer
     * fires.
     */
    @Test
    public void testBasicAnnotation() throws Exception {
        assertTrue(isTimerMethodExecuted(lookupBean("AnnotationBean"), 0));
    }

    /**
     * This test verifies that the Timeout annotation works on a private method
     * with no parameters.
     */
    @Test
    public void testPrivateAnnotation() throws Exception {
        assertTrue(isTimerMethodExecuted(lookupBean("PrivateAnnotationBean"), 0));
    }

    /**
     * This test verifies that a timeout-method with no method-params selects
     * a method with no parameters.
     */
    @Test
    public void testNoParamsZeroParamsXML() throws Exception {
        assertTrue(isTimerMethodExecuted(lookupBean("NoParamsZeroParamsXMLBean"), 0));
    }

    /**
     * This test verifies that a timeout-method with no method-params selects
     * a method with a Timer parameter.
     */
    @Test
    public void testNoParamsOneParamXML() throws Exception {
        assertTrue(isTimerMethodExecuted(lookupBean("NoParamsOneParamXMLBean"), 0));
    }

    /**
     * This test verifies that a timeout-method with an empty method-params
     * selects a private method on the bean class not a private method on a
     * super-class.
     */
    @Test
    public void testHierarchyXML() throws Exception {
        Intf bean = lookupBean("NoParamsHierarchyXMLBean");
        assertTrue(isTimerMethodExecuted(bean, 0));
        assertFalse(isTimerMethodExecuted(bean, 1));
    }

    /**
     * This test verifies that a timeout-method with an empty method-params
     * unambiguously selects the method with no parameters, even if an
     * overloaded method with a parameter exists.
     */
    @Test
    public void testEmptyParamsZeroParamsXML() throws Exception {
        Intf bean = lookupBean("EmptyParamsZeroParamsXMLBean");
        assertTrue(isTimerMethodExecuted(bean, 0));
        assertFalse(isTimerMethodExecuted(bean, 1));
    }

    /**
     * This test verifies that a timeout-method with an empty method-params
     * selects a private method on the bean class not a private method on a
     * super-class.
     */
    @Test
    public void testEmptyParamsHierarchyXML() throws Exception {
        Intf bean = lookupBean("EmptyParamsHierarchyXMLBean");
        assertTrue(isTimerMethodExecuted(bean, 0));
        assertFalse(isTimerMethodExecuted(bean, 1));
    }

    /**
     * This test verifies that the Timeout annotation and a timeout-method with
     * no timeout-params can be specified for the same method with no
     * parameters.
     */
    @Test
    public void testNoParamsMixed() throws Exception {
        assertTrue(isTimerMethodExecuted(lookupBean("NoParamsMixedBean"), 0));
    }

    /**
     * This test verifies that the Timeout annotation and a timeout-method with
     * an empty method-params can be specified for the same method with no
     * parameters.
     */
    @Test
    public void testEmptyParamsMixed() throws Exception {
        assertTrue(isTimerMethodExecuted(lookupBean("EmptyParamsMixedBean"), 0));
    }
}
