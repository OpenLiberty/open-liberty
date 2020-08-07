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
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.removal;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

/**
 * Lists the metrics currently registered in the application registry
 */
@WebServlet("metriclist")
public class MetricListServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    MetricRegistry reg;

    @Inject
    @RegistryType(type = Type.BASE)
    MetricRegistry baseReg;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        for (String metricName : reg.getNames()) {
            resp.getWriter().println(metricName);
        }
        for (String metricName : baseReg.getNames()) {
            resp.getWriter().println(metricName);
        }
    }

}
