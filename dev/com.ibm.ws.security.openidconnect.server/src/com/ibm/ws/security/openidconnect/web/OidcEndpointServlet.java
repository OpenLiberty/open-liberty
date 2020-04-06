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
package com.ibm.ws.security.openidconnect.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.security.openidconnect.server.TraceConstants;

public class OidcEndpointServlet extends OAuth20EndpointServlet
{
    private transient OidcEndpointServices oidcEndpointServices = null;
    private transient ServletContext servletContext = null;
    private transient BundleContext bundleContext = null;
    private transient ServiceReference<OidcEndpointServices> oidcEndPointServicesRef = null;

    private static final long serialVersionUID = 1L;

    public OidcEndpointServlet() {
        super();
    }

    public void init() {
        servletContext = getServletContext();
        bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        oidcEndPointServicesRef = bundleContext.getServiceReference(OidcEndpointServices.class);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
        this.doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
        getOidcEndpointServices();
        oidcEndpointServices.handleOidcRequest(request, response, servletContext);
    }

    private OidcEndpointServices getOidcEndpointServices() throws ServletException {
        if (oidcEndPointServicesRef == null) {
            throw new ServletException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                    TraceConstants.MESSAGE_BUNDLE,
                                                                    "OIDC_OSGI_ENDPOINT_SERVICE_ERROR",
                                                                    null,
                                                                    "CWWKS1616E: A configuration error has occurred. No OpenID Connect endpoint service is available. Ensure that you have the openidConnectServer-1.0 feature configured."));
        } else {
            oidcEndpointServices = bundleContext.getService(oidcEndPointServicesRef);
        }
        return oidcEndpointServices;
    }
}
