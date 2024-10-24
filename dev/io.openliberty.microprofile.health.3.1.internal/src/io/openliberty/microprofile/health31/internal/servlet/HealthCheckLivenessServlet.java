/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.health31.internal.servlet;

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

import io.openliberty.microprofile.health.internal.common.HealthCheckConstants;
import io.openliberty.microprofile.health31.internal.HealthCheck31Service;

public class HealthCheckLivenessServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(HealthCheckLivenessServlet.class);
    private transient HealthCheck31Service healthService = null;
    private ServiceTracker<HealthCheck31Service, HealthCheck31Service> healthServiceTracker;

    private final static Logger logger = Logger.getLogger(HealthCheckLivenessServlet.class.getName(), "io.openliberty.microprofile.health.resources.Health");

    @Override
    public void init(ServletConfig config) throws ServletException {
        BundleContext bundleContext = (BundleContext) config.getServletContext().getAttribute("osgi-bundlecontext");
        healthServiceTracker = new ServiceTracker<>(bundleContext, HealthCheck31Service.class, null);
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
        healthService.performHealthCheck(request, response, HealthCheckConstants.HEALTH_CHECK_LIVE);
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
