/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.tx;

import java.io.IOException;
import java.util.HashSet;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.faulttolerance_fat.tx.beans.RetryBeanB;
import com.ibm.ws.microprofile.faulttolerance_fat.tx.beans.RetryBeanB2;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/retry")
public class RetryServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    RetryBeanB beanB;

    @Inject
    RetryBeanB2 beanB2;

    public void testRetryMultiTran(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HashSet<Long> txns = new HashSet<Long>();
        //should be retried 3 times as per default
        try {
            beanB.connectB(txns);
            throw new AssertionError("Exception not thrown");
        } catch (ConnectException e) {
            String expected = "ConnectException: RetryBeanB Connect: 4";
            String actual = e.getMessage();
            if (!expected.equals(actual)) {
                throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
            }
        }

        // Each invocation should be in its own transaction
        if (txns.size() != 4) {
            throw new AssertionError("Expected txns: 4, Actual: " + txns.size());
        }
    }

    public void testRetrySingleTran(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HashSet<Long> txns = new HashSet<Long>();
        //should be retried 3 times as per default
        try {
            beanB2.connectB(txns);
            throw new AssertionError("Exception not thrown");
        } catch (ConnectException e) {
            String expected = "ConnectException: RetryBeanB2 Connect: 4";
            String actual = e.getMessage();
            if (!expected.equals(actual)) {
                throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
            }
        }

        // All invocations should be within the same transaction
        if (txns.size() != 1) {
            throw new AssertionError("Expected txns: 1, Actual: " + txns.size());
        }
    }
}