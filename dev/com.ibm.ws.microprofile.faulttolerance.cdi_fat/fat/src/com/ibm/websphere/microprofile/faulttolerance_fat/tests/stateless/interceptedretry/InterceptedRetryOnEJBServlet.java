/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.interceptedretry;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet("/InterceptedRetryOnEJBServlet")
public class InterceptedRetryOnEJBServlet extends FATServlet {

    public static int logMethodCalledCounter = 0;

    public static void log() {
        logMethodCalledCounter++;
    }

    @EJB
    private InterceptedRetryOnEJB ejb;

    @Test
    @Mode(TestMode.EXPERIMENTAL) //Currently the EJB/Interceptor integration cannot handle calling an intercepter more than once due to FT Retries.
    public void testInterceptorOnRetryIsCalledEveryRetry(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        ejb.retryEventuallyPass();
        Assert.assertEquals(InterceptedRetryOnEJB.FAILURES_PLUS_FENCEPOST, logMethodCalledCounter); //The interceptor should fire once every time the method is run, including retries.
    }

}
