/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.web.OAuth20EndpointServlet;
import com.ibm.ws.security.openidconnect.server.TraceConstants;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import io.openliberty.security.openidconnect.web.OidcSupportedHttpMethodHandler;

@SuppressWarnings("restriction")
public class OidcEndpointServlet extends OAuth20EndpointServlet {
    private static TraceComponent tc = Tr.register(OidcEndpointServlet.class);

    private transient OidcEndpointServices oidcEndpointServices = null;
    private transient ServletContext servletContext = null;
    private transient BundleContext bundleContext = null;
    private transient ServiceReference<OidcEndpointServices> oidcEndPointServicesRef = null;

    private static final long serialVersionUID = 1L;

    public OidcEndpointServlet() {
        super();
    }

    @Override
    public void init() {
        servletContext = getServletContext();
        bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        oidcEndPointServicesRef = bundleContext.getServiceReference(OidcEndpointServices.class);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.GET)) {
            return;
        }
        handleRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.POST)) {
            return;
        }
        handleRequest(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.HEAD)) {
            return;
        }
        handleRequest(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.DELETE)) {
            return;
        }
        handleRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.PUT)) {
            return;
        }
        handleRequest(request, response);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        OidcSupportedHttpMethodHandler optionsRequestHandler = getOidcSupportedHttpMethodHandler(request, response);
        optionsRequestHandler.sendUnsupportedMethodResponse();
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        OidcSupportedHttpMethodHandler optionsRequestHandler = getOidcSupportedHttpMethodHandler(request, response);
        optionsRequestHandler.sendHttpOptionsResponse();
    }

    @Override
    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getOidcEndpointServices();
        oidcEndpointServices.handleOidcRequest(request, response, servletContext);
    }

    OidcSupportedHttpMethodHandler getOidcSupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        return new OidcSupportedHttpMethodHandler(request, response, getOidcEndpointServices());
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

    private boolean isValidHttpMethodForRequest(HttpServletRequest request, HttpServletResponse response, HttpMethod requestMethod) throws IOException, ServletException {
        OidcSupportedHttpMethodHandler optionsRequestHandler = getOidcSupportedHttpMethodHandler(request, response);
        if (!optionsRequestHandler.isValidHttpMethodForRequest(requestMethod)) {
            optionsRequestHandler.sendUnsupportedMethodResponse();
            return false;
        }
        return true;
    }

}
