/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.internal.servlet;

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
import com.ibm.ws.microprofile.health.internal.HealthCheckService;

public class HealthCheckServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(HealthCheckServlet.class);
    private transient HealthCheckService healthService = null;

    private final static Logger logger = Logger.getLogger(HealthCheckServlet.class.getName(), "com.ibm.ws.microprofile.health.resources.Health");

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

            ServiceReference<HealthCheckService> ref = ctxt.getServiceReference(HealthCheckService.class);
            if (ref == null) {
                logger.log(Level.SEVERE, "healthcheck.CWMH0000E", "HealthCheckService");
                throw new ServletException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", "HealthCheckService"));
            } else {
                healthService = ctxt.getService(ref);
            }
        }
    }
}
