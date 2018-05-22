/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web;

import static com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.TimerData.MAX_TIMER_WAIT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.AroundInvokeBean;

import componenttest.app.FATServlet;

/**
 * Tests that AroundInvoke and AroundTimeout interceptors are independent.
 */
@SuppressWarnings("serial")
@WebServlet("/AroundInvokeAnnServlet")
public class AroundInvokeAnnServlet extends FATServlet {
    @EJB(beanName = "AroundTimeoutAnnEJB/AroundInvokeBean")
    AroundInvokeBean ivBean;

    private AroundInvokeBean lookupBean() throws NamingException {
        return ivBean;
    }

    /**
     * This test verifies that AroundInvoke interceptors are called and
     * AroundTimeout interceptors are not called when a Timeout method is
     * invoked as a business method.
     */
    @Test
    public void testAroundInvokeAnn() throws Exception {
        AroundInvokeBean bean = lookupBean();
        bean.reset();
        bean.test();

        assertTrue("expected AroundInvoke called", bean.isAroundInvokeCalled());
        assertFalse("expected AroundTimeout not called", bean.isAroundTimeoutCalled());
    }

    /**
     * This test verifies that AroundTimeout interceptors are called and
     * AroundInvoke interceptors are not called when a timeout method is
     * invoked.
     */
    @Test
    public void testAroundTimerAnn() throws Exception {
        AroundInvokeBean bean = lookupBean();
        bean.reset();
        CountDownLatch timerLatch = bean.createTimer();
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        assertFalse("expected AroundInvoke not called", bean.isAroundInvokeCalled());
        assertTrue("expected AroundTimeout called", bean.isAroundTimeoutCalled());
    }
}
