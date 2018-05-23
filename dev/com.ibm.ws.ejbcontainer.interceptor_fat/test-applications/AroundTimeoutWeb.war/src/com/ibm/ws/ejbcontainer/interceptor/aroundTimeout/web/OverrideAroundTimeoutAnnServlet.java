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
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.OverrideIntf;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.OverridePackagePrivateBean;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.OverridePrivateBean;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.OverrideProtectedBean;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.OverridePublicBean;

import componenttest.app.FATServlet;

/**
 * Tests that AroundTimeout interceptor methods can be properly overridden.
 */
@SuppressWarnings("serial")
@WebServlet("/OverrideAroundTimeoutAnnServlet")
public class OverrideAroundTimeoutAnnServlet extends FATServlet {
    @EJB(beanName = "AroundTimeoutAnnEJB/OverridePublicBean")
    OverrideIntf ivPublicBean;

    @EJB(beanName = "AroundTimeoutAnnEJB/OverrideProtectedBean")
    OverrideIntf ivProtectedBean;

    @EJB(beanName = "AroundTimeoutAnnEJB/OverridePackagePrivateBean")
    OverrideIntf ivBean;

    @EJB(beanName = "AroundTimeoutAnnEJB/OverridePrivateBean")
    OverrideIntf ivPrivateBean;

    private OverrideIntf lookupBean(Class<?> beanClass) throws NamingException {
        if (OverridePublicBean.class == beanClass) {
            return ivPublicBean;
        }

        if (OverrideProtectedBean.class == beanClass) {
            return ivProtectedBean;
        }

        if (OverridePackagePrivateBean.class == beanClass) {
            return ivBean;
        }

        if (OverridePrivateBean.class == beanClass) {
            return ivPrivateBean;
        }

        throw new IllegalArgumentException(beanClass.getName());
    }

    private void runTest(Class<?> beanClass, boolean expectParent, boolean expectBean) throws NamingException, InterruptedException {
        OverrideIntf bean = lookupBean(beanClass);
        CountDownLatch timerLatch = bean.createTimer();
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        assertEquals("checking parent interceptor", expectParent, bean.isParentAroundTimeoutInvoked());
        assertEquals("checking bean interceptor", expectBean, bean.isAroundTimeoutInvoked());
    }

    /**
     * This test verifies that a public AroundTimeout method can be overridden in
     * a child class.
     *
     * <p>A stateless bean is looked up, a timer is created, the timer fires,
     * and the AroundTimeout method on the bean class overrides the method on
     * the parent class.
     */
    @Test
    public void testOverridePublicAnn() throws Exception {
        runTest(OverridePublicBean.class, false, true);
    }

    /**
     * This test verifies that a protected AroundTimeout method can be overridden
     * in a child class.
     *
     * <p>A stateless bean is looked up, a timer is created, the timer fires,
     * and the AroundTimeout method on the bean class overrides the method on
     * the parent class.
     */
    @Test
    public void testOverrideProtectedAnn() throws Exception {
        runTest(OverrideProtectedBean.class, false, true);
    }

    /**
     * This test verifies that a package private AroundTimeout method can be
     * overridden in a child class.
     *
     * <p>A stateless bean is looked up, a timer is created, the timer fires,
     * and the AroundTimeout method on the bean class overrides the method on
     * the parent class.
     */
    @Test
    public void testOverridePackagePrivateAnn() throws Exception {
        runTest(OverridePackagePrivateBean.class, false, true);
    }

    /**
     * This test verifies that a private AroundTimeout method cannot be
     * overridden by a child class.
     *
     * <p>A stateless bean is looked up, a timer is created, the timer fires,
     * the AroundTimeout method on the super class is invoked, and then the
     * AroundTimeout method on the bean class is invoked.
     */
    @Test
    public void testOverridePrivateAnn() throws Exception {
        runTest(OverridePrivateBean.class, true, true);
    }
}
