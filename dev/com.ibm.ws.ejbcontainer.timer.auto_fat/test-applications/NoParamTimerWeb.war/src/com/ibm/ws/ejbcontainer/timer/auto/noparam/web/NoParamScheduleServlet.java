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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.auto.noparam.ejb.AbstractBean;
import com.ibm.ws.ejbcontainer.timer.auto.noparam.ejb.Intf;

/**
 * This test is equivalent to TimeoutTest, except that the Schedule annotation
 * is used instead of the Timeout annotation, and a timer/timeout-method is
 * used in XML instead of a timeout-method. In all cases, the scheduled timers
 * are non-persistent and are scheduled to fire every second.
 */
@SuppressWarnings("serial")
@WebServlet("/NoParamScheduleServlet")
public class NoParamScheduleServlet extends NoParamAbstractServlet {
    private static final String CLASS_NAME = NoParamScheduleServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Override
    public void setup() throws Exception {
        svLogger.info("> setup : waiting for scheduled timers to expire : " + MAX_TIMER_WAIT_TIME);
        AbstractBean.svScheduleLatch.await(MAX_TIMER_WAIT_TIME, TimeUnit.MILLISECONDS);
        svLogger.info("< setup : expired or max timeout reached");
    }

    @Override
    public void cleanup() throws Exception {
        svLogger.info("> cleanup : clearing all timers");
        super.cleanup();
        lookupBean("NoParamsScheduleMixedBean").clearAllTimers();
        lookupBean("EmptyParamsScheduleMixedBean").clearAllTimers();
        svLogger.info("< cleanup : timers cleared");
    }

    // Automatic timers should have been started.. automatically.
    @Override
    protected void startTimer(Intf bean) {
    }

    @Override
    protected boolean isTimerMethodExecuted(Intf bean, int methodId) {
        return bean.isScheduleExecuted(methodId);
    }

    /**
     * This test verifies that a timer/timeout-method with no method-params
     * overrides the Schedule annotation.
     */
    @Test
    public void testOverrideScheduleMixed() throws Exception {
        assertTrue(isTimerMethodExecuted(lookupBean("NoParamsScheduleMixedBean"), 0));
    }

    /**
     * This test verifies that a timer/timeout-method with a method-params with
     * an empty method-param overrides the Schedule annotation.
     */
    @Test
    public void testOverrideEmptyParamsScheduleMixed() throws Exception {
        assertTrue(isTimerMethodExecuted(lookupBean("EmptyParamsScheduleMixedBean"), 0));
    }
}
