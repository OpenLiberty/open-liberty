/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.timeout;

import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertReturns;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.ExecutionAssert.assertThrowsEjbWrapped;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Assert;
import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/TimeOutOnEJBServlet")
public class TimeOutOnEJBServlet extends FATServlet {

    @EJB
    private TimeoutOnEJB ejb;

    // EJB will create FFDC for non-application exceptions
    @ExpectedFFDC("org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException")
    @Test
    public void testTimeoutOnEJB(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        ResultsRecord record = new ResultsRecord();
        assertThrowsEjbWrapped(TimeoutException.class, () -> ejb.testMethodThatTimesOut(record));

        Assert.assertTrue(record.testMethodCalled);
        Assert.assertTrue(record.testMethodRecievedInteruptException);
        Assert.assertFalse(record.testMethodContinuedPastInterruptException);
    }

    @Test
    public void testNoTimeoutOnEJB(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        assertReturns(() -> ejb.testMethodThatWontTimeOut());
    }

}
