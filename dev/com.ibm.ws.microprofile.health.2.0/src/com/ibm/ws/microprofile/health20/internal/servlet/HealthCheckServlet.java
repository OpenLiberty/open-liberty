/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.internal.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.health20.internal.HealthCheck20Service;

public class HealthCheckServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(HealthCheckServlet.class);
    private transient HealthCheck20Service healthService = null;

    private final static Logger logger = Logger.getLogger(HealthCheckServlet.class.getName(), "com.ibm.ws.microprofile.health20.resources.Health20");

    /** {@inheritDoc} */
    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        findHealthService(request);
        healthService.performHealthCheck(request, response);
    }

    /** {@inheritDoc} */
    private synchronized void findHealthService(final HttpServletRequest request) throws ServletException {

        if (healthService == null) {
            HttpSession session = request.getSession();
            ServletContext sc = session.getServletContext();
            BundleContext ctxt = (BundleContext) sc.getAttribute("osgi-bundlecontext");

            ServiceReference<HealthCheck20Service> ref = ctxt.getServiceReference(HealthCheck20Service.class);
            if (ref == null) {
                logger.log(Level.SEVERE, "healthcheck.CWMH0000E", "HealthCheckService");
                throw new ServletException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", "HealthCheckService"));
            } else {
                healthService = ctxt.getService(ref);
            }
        }
    }
}
