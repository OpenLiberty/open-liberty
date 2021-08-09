/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Servlet implementation class JwtEndpointServlet
 */
public class JwtEndpointServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static TraceComponent tc = Tr.register(JwtEndpointServlet.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private transient ServletContext servletContext = null;
    private transient BundleContext bundleContext = null;
    private transient JwtEndpointServices jwtEndpointServices = null;
    private transient ServiceReference<JwtEndpointServices> jwtEndpointServicesRef = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public JwtEndpointServlet() {
        super();
    }

    @Override
    public void init() {
        servletContext = getServletContext();
        bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        jwtEndpointServicesRef = bundleContext.getServiceReference(JwtEndpointServices.class);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    /**
     * @see HttpServlet#doHead(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    /**
     * @see HttpServlet#doDelete(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    /**
     * @see HttpServlet#doPut(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getJwtEndpointServices();
        jwtEndpointServices.handleEndpointRequest(request, response, servletContext);
    }

    private synchronized JwtEndpointServices getJwtEndpointServices() throws ServletException {
        if (jwtEndpointServicesRef == null) {
            throw new ServletException(Tr.formatMessage(tc, "JWT_OSGI_ENDPOINT_SERVICE_ERROR"));
        }

        jwtEndpointServices = bundleContext.getService(jwtEndpointServicesRef);
        return jwtEndpointServices;
    }
}