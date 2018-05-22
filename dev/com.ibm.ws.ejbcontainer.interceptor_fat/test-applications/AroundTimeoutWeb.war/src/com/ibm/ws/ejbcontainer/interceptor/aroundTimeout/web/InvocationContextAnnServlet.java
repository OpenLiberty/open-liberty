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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.InvocationContextAbstractBean;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.InvocationContextNoParamBean;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.InvocationContextParamBean;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.InvocationContextSFBean;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.InvocationContextSFInterceptor;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.InvocationContextSFInterceptor.InterceptorType;

import componenttest.app.FATServlet;

/**
 * Tests the InvocationContext that is passed to AroundTimer interceptors.
 */
@SuppressWarnings("serial")
@WebServlet("/InvocationContextAnnServlet")
public class InvocationContextAnnServlet extends FATServlet {
    @EJB(beanName = "AroundTimeoutAnnEJB/InvocationContextParamBean")
    InvocationContextParamBean ivPBean;

    @EJB(beanName = "AroundTimeoutAnnEJB/InvocationContextNoParamBean")
    InvocationContextNoParamBean ivNPBean;

    @SuppressWarnings("unchecked")
    private <T> T lookupBean(Class<T> beanClass) throws NamingException {
        if (InvocationContextParamBean.class == beanClass) {
            return (T) ivPBean;
        }

        if (InvocationContextNoParamBean.class == beanClass) {
            return (T) ivNPBean;
        }

        throw new IllegalArgumentException(beanClass.getName());
    }

    private InvocationContextSFBean lookupSFBean() throws NamingException {
        return (InvocationContextSFBean) (new InitialContext()).lookup("java:app/AroundTimeoutAnnEJB/InvocationContextSFBean");
    }

    /**
     * This test verifies that the InvocationContext.getTarget, getMethod,
     * getParameters, and getTimer methods return valid values from an
     * AroundTimeout method of a timeout method.
     *
     * <p>A stateless bean is looked up, a method is invoked that creates a
     * 0-millisecond delay timer, the test sleeps waiting for the timer to fire,
     * the timer calls setParameters(new Object[1]), and then the invocation
     * context is checked.
     */
    @Test
    public void testInvocationContextAroundTimeoutAnn() throws Exception {
        CountDownLatch timerLatch = lookupBean(InvocationContextParamBean.class).createTimer();
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        assertEquals("interceptor target", InvocationContextParamBean.class, InvocationContextParamBean.svAroundTimeoutTarget.getClass());
        assertEquals("interceptor method", "timeout", InvocationContextParamBean.svAroundTimeoutMethod.getName());
        assertEquals("interceptor method params length", 1, InvocationContextParamBean.svAroundTimeoutParameters.length);
        assertSame("interceptor method param", InvocationContextParamBean.svAroundTimeoutTimer, InvocationContextParamBean.svAroundTimeoutParameters[0]);
        assertEquals("interceptor timer info", InvocationContextAbstractBean.INFO, InvocationContextParamBean.svAroundTimeoutTimerInfo);
        assertSame("interceptor timer", InvocationContextParamBean.svAroundTimeoutTimer, InvocationContextParamBean.svTimeoutTimer);
    }

    /**
     * This test verifies that the InvocationContext.getTarget, getMethod,
     * getParameters, and getTimer methods return valid values from an
     * AroundTimeout method of a no-parameter timeout method.
     *
     * <p>A stateless bean is looked up, a method is invoked that creates a
     * 0-millisecond delay timer, the test sleeps waiting for the timer to fire,
     * and then the invocation context is checked.
     */
    @Test
    public void testInvocationContextAroundNoParamTimeoutAnn() throws Exception {
        CountDownLatch timerLatch = lookupBean(InvocationContextNoParamBean.class).createTimer();
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        assertEquals("interceptor target", InvocationContextNoParamBean.class, InvocationContextNoParamBean.svAroundTimeoutTarget.getClass());
        assertEquals("interceptor method", "timeout", InvocationContextNoParamBean.svAroundTimeoutMethod.getName());
        assertEquals("interceptor method params length", 0, InvocationContextNoParamBean.svAroundTimeoutParameters.length);
        assertEquals("interceptor timer info", InvocationContextAbstractBean.INFO, InvocationContextNoParamBean.svAroundTimeoutTimerInfo);
    }

    private static void checkInterceptor(InterceptorType type) {
        assertTrue("interceptor fired: " + type, InvocationContextSFInterceptor.svFired.contains(type));

        Object timer = InvocationContextSFInterceptor.svTimers.get(type);
        assertNull("expected null timer: " + timer, timer);
    }

    /**
     * This test verifies that the InvocationContext.getTimer method returns
     * null from a PostConstruct interceptor.
     *
     * <p>A stateful bean is looked up, and then the interceptor data is checked
     * to ensure that the PostConstruct interceptor fired and that the timer
     * from the InvocationContext was null.
     */
    @Test
    public void testInvocationContextPostConstructGetTimerAnn() throws Exception {
        InvocationContextSFInterceptor.reset();
        lookupSFBean();
        checkInterceptor(InterceptorType.POST_CONSTRUCT);
    }

    /**
     * This test verifies that the InvocationContext.getTimer method returns
     * null from a PreDestroy interceptor.
     *
     * <p>A stateful bean is looked up, a Remove method is called, and then the
     * interceptor data is checked to ensure that the PreDestroy interceptor
     * fired and that the timer from the InvocationContext was null.
     */
    @Test
    public void testInvocationContextPreDestroyGetTimerAnn() throws Exception {
        InvocationContextSFInterceptor.reset();
        lookupSFBean().remove();
        checkInterceptor(InterceptorType.POST_CONSTRUCT);
    }

    /**
     * This test verifies that the InvocationContext.getTimer method returns
     * null from a PrePassivate interceptor.
     *
     * <p>A stateful bean with activation-policy="TRANSACTION" is looked up, a
     * method with default REQUIRED transaction attribute is called, and then
     * the interceptor data is checked to ensure that the PrePassivate
     * interceptor fired and that the timer from the InvocationContext was null.
     */
    @Test
    public void testInvocationContextPrePassivateGetTimerAnn() throws Exception {
        InvocationContextSFInterceptor.reset();
        lookupSFBean().test();
        checkInterceptor(InterceptorType.PRE_PASSIVATE);
    }

    /**
     * This test verifies that the InvocationContext.getTimer method returns
     * null from a PostActivate interceptor.
     *
     * <p>A stateful bean with activation-policy="TRANSACTION" is looked up, a
     * method with default REQUIRED transaction attribute is called, and then
     * the interceptor data is checked to ensure that the PostActivate
     * interceptor fired and that the timer from the InvocationContext was null.
     */
    @Test
    public void testInvocationContextPostActivateGetTimerAnn() throws Exception {
        InvocationContextSFInterceptor.reset();
        lookupSFBean().test();
        checkInterceptor(InterceptorType.POST_ACTIVATE);
    }

    /**
     * This test verifies that the InvocationContext.getTimer method returns
     * null from an AroundInvoke interceptor.
     *
     * <p>A stateful bean, a business method is called, and and then the
     * interceptor data is checked to ensure that the AroundInvoke interceptor
     * fired and that the timer from the InvocationContext was null.
     */
    @Test
    public void testInvocationContextAroundInvokeGetTimerAnn() throws Exception {
        InvocationContextSFInterceptor.reset();
        lookupSFBean().test();
        checkInterceptor(InterceptorType.AROUND_INVOKE);
    }
}
