/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jaxrs.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.jaxrs20.api.JaxRsProviderFactoryService;
import com.ibm.ws.jaxrs20.endpoint.JaxRsWebEndpoint;
import com.ibm.ws.jaxrs20.server.JaxRsHttpServletRequestAdapter;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;

/**
 *
 */
public class IBMRestServlet extends HttpServlet {

    private static final long serialVersionUID = -6835560282014155024L;

    private static final List<String> KNOWN_HTTP_VERBS = Arrays.asList(new String[] { "POST", "GET", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE" });

    private static final String HTML_CONTENT_TYPE = "text/html";

    private transient JaxRsWebEndpoint endpoint;

    private final transient IWebAppNameSpaceCollaborator collaborator;

    private final transient JaxRsProviderFactoryService providerFactoryService;

    public IBMRestServlet(JaxRsWebEndpoint endpoint, JaxRsProviderFactoryService jaxRsProviderFactoryServicer) {
        this(endpoint, null, jaxRsProviderFactoryServicer);
    }

    public IBMRestServlet(JaxRsWebEndpoint endpoint, IWebAppNameSpaceCollaborator collaborator, JaxRsProviderFactoryService jaxRsProviderFactoryService) {
        this.endpoint = endpoint;
        this.collaborator = collaborator;
        providerFactoryService = jaxRsProviderFactoryService;

    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        if (endpoint != null) {
            endpoint.init(servletConfig, providerFactoryService);
        } else {
            // TODO: Get endpoint from some place and then init
            if (endpoint == null) {
                throw new ServletException("Coult not find endpoint information.");
            }
        }
    }

    /**
     * As AbstractHTTPServlet in CXF, with this, it will make sure that, all the request methods
     * will be routed to handleRequest method.
     */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

        HttpServletRequest request;
        HttpServletResponse response;

        try {
            request = (HttpServletRequest) req;
            if (collaborator != null) {
                ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
                request = new JaxRsHttpServletRequestAdapter(request, collaborator, componentMetaData);
            }
            response = (HttpServletResponse) res;
        } catch (ClassCastException e) {
            throw new ServletException("Unrecognized HTTP request or response object", e);
        }

        String method = request.getMethod();
        if (KNOWN_HTTP_VERBS.contains(method)) {
            super.service(request, response);
        } else {
            handleRequest(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        if (null == request.getQueryString()) {
//            reportAvailableService(request, response);
//        } else {
        handleRequest(request, response);
//        }
    }

//    /**
//     * Prints a welcome message for the endpoint
//     *
//     * @param resquest
//     * @param response
//     */
//    private void reportAvailableService(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        PrintWriter writer = response.getWriter();
//        response.setContentType(HTML_CONTENT_TYPE);
//        writer.println("<h2>" + request.getServletPath() + "</h2>");
//        writer.println("<h3>Hello! This is a CXF Web Service!</h3>");
//    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        if (endpoint != null) {
            endpoint.invoke(request, response);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        if (endpoint != null) {
            endpoint.destroy();
        }
        endpoint = null;
        super.destroy();
    }
}
