/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.health30.internal.servlet;

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

import io.openliberty.microprofile.health30.internal.HealthCheck30Service;
import io.openliberty.microprofile.health30.internal.HealthCheckConstants;

public class HealthCheckLivenessServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(HealthCheckLivenessServlet.class);
    private transient HealthCheck30Service healthService = null;

    private final static Logger logger = Logger.getLogger(HealthCheckLivenessServlet.class.getName(), "io.openliberty.microprofile.health30.resources.Health30");

    /** {@inheritDoc} */
    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        findHealthService(request);
        healthService.performHealthCheck(request, response, HealthCheckConstants.HEALTH_CHECK_LIVE);
    }

    /** {@inheritDoc} */
    private synchronized void findHealthService(final HttpServletRequest request) throws ServletException {

        if (healthService == null) {
            HttpSession session = request.getSession();
            ServletContext sc = session.getServletContext();
            BundleContext ctxt = (BundleContext) sc.getAttribute("osgi-bundlecontext");

            ServiceReference<HealthCheck30Service> ref = ctxt.getServiceReference(HealthCheck30Service.class);
            if (ref == null) {
                logger.log(Level.SEVERE, "OSGI_SERVICE_ERROR", "HealthCheckService");
                throw new ServletException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", "HealthCheckService"));
            } else {
                healthService = ctxt.getService(ref);
            }
        }
    }
}
