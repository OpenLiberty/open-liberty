/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.health40.internal.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.health40.internal.HealthCheck40Service;

public class HealthCheckServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(HealthCheckServlet.class);
    private transient HealthCheck40Service healthService = null;
    private ServiceTracker<HealthCheck40Service, HealthCheck40Service> healthServiceTracker;

    private final static Logger logger = Logger.getLogger(HealthCheckServlet.class.getName(), "io.openliberty.microprofile.health.resources.Health");

    @Override
    public void init(ServletConfig config) throws ServletException {
        BundleContext bundleContext = (BundleContext) config.getServletContext().getAttribute("osgi-bundlecontext");
        healthServiceTracker = new ServiceTracker<>(bundleContext, HealthCheck40Service.class, null);
        healthServiceTracker.open();
        super.init(config);
    }

    @Override
    public void destroy() {
        super.destroy();
        healthServiceTracker.close();
    }

    /** {@inheritDoc} */
    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        findHealthService();
        healthService.performHealthCheck(request, response);
    }

    /** {@inheritDoc} */
    private synchronized void findHealthService() throws ServletException {
        if (healthService == null) {
            if (healthServiceTracker == null) {
                logger.log(Level.SEVERE, "OSGI_SERVICE_ERROR", "HealthCheckService");
                throw new ServletException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", "HealthCheckService"));
            } else {
                healthService = healthServiceTracker.getService();
            }
        }
    }
}
