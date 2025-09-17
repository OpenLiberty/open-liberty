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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.fallback;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/FallbackOnEJBServlet")
public class FallbackOnEJBServlet extends FATServlet {

    @EJB
    private FallbackOnEJB ejb;

    @Test
    //The fault tolerance CDI Extension does not fire events for methods on an EJB on these versions
    public void testFallbackOnEJB(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int result = ejb.test();

        //Assert that an exception caused us to get a result from the fallback method
        Assert.assertEquals(FallbackOnEJB.FROM_FALL_BACK_METHOD, result);
    }

}
