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
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ejs.ras.TraceNLS;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import io.openliberty.security.oauth20.web.OAuthSupportedHttpMethodHandler;

/**
 * Servlet implementation class OAuth20EndpointServlet
 */
@SuppressWarnings("restriction")
public class OAuth20EndpointServlet extends HttpServlet {
    private static final String MESSAGE_BUNDLE = "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages";

    private static final long serialVersionUID = 1L;

    private transient OAuth20EndpointServices oauthEndpointServices = null;
    private transient ServletContext servletContext = null;
    private transient BundleContext bundleContext = null;
    private transient ServiceReference<OAuth20EndpointServices> oauth20EndPointServicesRef = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public OAuth20EndpointServlet() {
        super();
    }

    @Override
    public void init() {
        servletContext = getServletContext();
        bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        oauth20EndPointServicesRef = bundleContext.getServiceReference(OAuth20EndpointServices.class);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.GET)) {
            return;
        }
        handleRequest(request, response);
    }

    /**
     * @see HttpServlet#doHead(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.HEAD)) {
            return;
        }
        handleRequest(request, response);
    }

    /**
     * @see HttpServlet#doDelete(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.DELETE)) {
            return;
        }
        handleRequest(request, response);
    }

    /**
     * @see HttpServlet#doPut(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.PUT)) {
            return;
        }
        handleRequest(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isValidHttpMethodForRequest(request, response, HttpMethod.POST)) {
            return;
        }
        handleRequest(request, response);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        OAuthSupportedHttpMethodHandler optionsRequestHandler = getOAuthSupportedHttpMethodHandler(request, response);
        optionsRequestHandler.sendUnsupportedMethodResponse();
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        OAuthSupportedHttpMethodHandler optionsRequestHandler = getOAuthSupportedHttpMethodHandler(request, response);
        optionsRequestHandler.sendHttpOptionsResponse();
    }

    OAuthSupportedHttpMethodHandler getOAuthSupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response) {
        return new OAuthSupportedHttpMethodHandler(request, response);
    }

    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getOAuthEndpointServices();
        oauthEndpointServices.handleOAuthRequest(request, response, servletContext);
    }

    private synchronized OAuth20EndpointServices getOAuthEndpointServices() throws ServletException {
        // TODO: do we have to do this for every call?
        if (oauth20EndPointServicesRef == null) {
            throw new ServletException(TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_OSGI_ENDPOINT_SERVICE_ERROR",
                    null,
                    "CWWKS1616E: A configuration error has occurred. No endpoint service is available. Ensure that you have the openidConnectServer-1.0 feature configured."));
        } else {
            oauthEndpointServices = bundleContext.getService(oauth20EndPointServicesRef);
        }
        return oauthEndpointServices;
    }

    private boolean isValidHttpMethodForRequest(HttpServletRequest request, HttpServletResponse response, HttpMethod requestMethod) throws IOException {
        OAuthSupportedHttpMethodHandler optionsRequestHandler = getOAuthSupportedHttpMethodHandler(request, response);
        if (!optionsRequestHandler.isValidHttpMethodForRequest(requestMethod)) {
            optionsRequestHandler.sendUnsupportedMethodResponse();
            return false;
        }
        return true;
    }

}
