/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.isolation;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to drive the annotated method on {@link IsolationBean} so that its metrics get reported
 */
@WebServlet("/isolationtest")
public class IsolationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    IsolationBean bean;

    @Inject
    InMemoryMetricReader reader;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String resultOne = reader.areThereNoMetrics() ? "Test one passed" : "There were metrics before calling doWorkWithRetry";
        bean.doWorkWithRetry();
        String resultTwo = reader.areThereNoMetrics() ? "There were no metrics after calling doWorkWithRetry" : "Test two passed";
        resp.getWriter().println(resultOne + " " + resultTwo);
    }

}
