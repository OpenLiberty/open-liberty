/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.web;

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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.error.ErrorHandlerImpl;
import com.ibm.ws.security.saml.error.SamlException;

public class EndpointServlet extends HttpServlet {
    private static TraceComponent tc = Tr.register(EndpointServlet.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    private transient EndpointServices endpointServices = null;
    private transient ServletContext servletContext = null;
    private transient BundleContext bundleContext = null;
    private transient ServiceReference<EndpointServices> endpointServicesRef = null;

    private static final long serialVersionUID = 1L;

    public EndpointServlet() {
        super();
    }

    @Override
    public void init() {
        servletContext = getServletContext();
        bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        endpointServicesRef = bundleContext.getServiceReference(EndpointServices.class);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "init:" + servletContext + "   " + bundleContext + "  " + endpointServicesRef);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        SsoRequest samlRequest = (SsoRequest) request.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
        if (Constants.EndpointType.SAMLMETADATA.equals(samlRequest.getType()) ||
            Constants.EndpointType.SLO.equals(samlRequest.getType()) ||
            Constants.EndpointType.LOGOUT.equals(samlRequest.getType())) {
            this.doPost(request, response);
        } else {

            if (tc.isDebugEnabled()) {
                String requestType = (samlRequest != null && samlRequest.getType() != null) ? samlRequest.getType().toString() : null;
                Tr.debug(tc, "doGet is not allowed for samlrequest of type: " + requestType + ", sending http500");
            }

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL ERROR");
            //throw new ServletException("The GET method is not allowed in this SAMLResponse");  // shows stack, bad.
        }
    }

    @Override
    @FFDCIgnore({ SamlException.class })
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EndpointServices samlEndpointServices = getSamlEndpointServices(response); // this won't be null, otherwise it throws Exception already
        try {
            if (samlEndpointServices != null) {
                samlEndpointServices.handleSamlRequest(request, response);
            }
        } catch (SamlException e) {
            // error handling.
            ErrorHandlerImpl.getInstance().handleException(request, response, e);
        }

    }

    private EndpointServices getSamlEndpointServices(HttpServletResponse response) throws ServletException {
        if (endpointServicesRef != null) {
            endpointServices = bundleContext.getService(endpointServicesRef);
            if (endpointServices == null) {
                init(); // we need to init the bundleContext once more
                endpointServices = bundleContext.getService(endpointServicesRef);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getSamlEndpointServices:" + servletContext + "   " + bundleContext + " " + endpointServices);
        }
        if (endpointServices == null) {
            // This should not happen
            try {
                Tr.error(tc, "SAML20_OSGI_ENDPOINT_SERVICE_ERROR", new Object[] {});
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL ERROR");
            } catch (IOException e) {
                // ffdc gets emitted if we ever go here
            }
            //throw new ServletException(Tr.formatMessage(tc, "SAML20_OSGI_ENDPOINT_SERVICE_ERROR"));  //bad
        }
        return endpointServices;
    }
}
