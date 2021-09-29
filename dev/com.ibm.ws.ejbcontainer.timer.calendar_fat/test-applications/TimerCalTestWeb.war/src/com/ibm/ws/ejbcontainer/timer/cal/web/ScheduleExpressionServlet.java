/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.cal.web;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.cal.ejb.ScheduleExpressionIntf;

@WebServlet("/ScheduleExpressionServlet")
@SuppressWarnings("serial")
public class ScheduleExpressionServlet extends AbstractServlet {

    @EJB
    protected ScheduleExpressionIntf ivBean;

    @Override
    public void cleanup() throws Exception {
        if (ivBean != null) {
            ivBean.clearAllTimers();
        }
    }

    @Test
    public void testYear() {

        try {
            ivBean.testYear();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testMonth() {

        try {
            ivBean.testMonth();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testDayOfMonth() {

        try {
            ivBean.testDayOfMonth();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testDayOfWeek() {

        try {
            ivBean.testDayOfWeek();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testHour() {

        try {
            ivBean.testHour();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testMinute() {

        try {
            ivBean.testMinute();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testSecond() {

        try {
            ivBean.testSecond();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testGetScheduleForNpTimer() {

        try {
            ivBean.testGetScheduleForNpTimer();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    // Called directly from NextTimeoutPersistentTest
    public void testGetScheduleForPersistentTimer() {

        try {
            ivBean.testGetScheduleForPersistentTimer();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    @Test
    public void testScheduleExpressionSubclassing() {

        try {
            ivBean.testScheduleExpressionSubclassing();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

}
