/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.logging.osgi.fat.wsloghandler;

import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.rest.handler.helper.ServletRESTRequestImpl;
import com.ibm.ws.rest.handler.helper.ServletRESTResponseImpl;
import com.ibm.wsspi.rest.handler.RESTHandlerContainer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Proxy servlet that bridges HTTP requests into the Liberty RESTHandlerContainer,
 * allowing the WsLogHandlerEndpoint RESTHandler DS component to receive HTTP traffic.
 */
public class WsLogHandlerProxyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient RESTHandlerContainer restHandlerContainer = null;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getAndSetRESTHandlerContainer(request);
        handleWithDelegate(request, response);
    }

    private void handleWithDelegate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean foundHandler = restHandlerContainer.handleRequest(
                new ServletRESTRequestImpl(request),
                new ServletRESTResponseImpl(response));

        if (!foundHandler) {
            String errorMsg = "There are no registered handlers that match the requested URL " + request.getRequestURI();
            response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg);
        }
    }

    private synchronized void getAndSetRESTHandlerContainer(HttpServletRequest request) throws ServletException {
        if (restHandlerContainer == null) {
            ServletContext sc = request.getServletContext();
            BundleContext ctxt = (BundleContext) sc.getAttribute("osgi-bundlecontext");

            ServiceReference<RESTHandlerContainer> ref = ctxt.getServiceReference(RESTHandlerContainer.class);
            if (ref == null) {
                throw new ServletException("OSGi service RESTHandlerContainer is not available.");
            }
            restHandlerContainer = ctxt.getService(ref);
        }
    }
}
