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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.retry;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/RetryOnEJBServlet")
public class RetryOnEJBServlet extends FATServlet {

    @EJB
    private RetryOnEJB ejb;

    @Test
    public void testRetryEventuallyPassesOnEJB(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        //This method will fail until it has failed the Maximum number of times it was configured to
        //do so. Fault Tolerence will make it retry each time.

        //When it has reached the maximum it will successfully return the number of times it has run.
        int result = ejb.retryEventuallyPass();
        Assert.assertEquals(RetryOnEJB.MAX, result);
    }

    @Test
    @AllowedFFDC("org.jboss.weld.exceptions.WeldException") //This will wrap the Test Exception, but only on FT 1.x
    public void testRetryNeverPassesOnEJB(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        boolean caughtTestException = false;
        try {
            ejb.retryNeverPass();
        } catch (TestException e) {
            caughtTestException = true;
        }
        Assert.assertEquals(RetryOnEJB.NEVER_PASS_MAX + 1, RetryOnEJB.getNeverPassCounter());//+1 for fencepost
        Assert.assertTrue(caughtTestException);
    }

}
