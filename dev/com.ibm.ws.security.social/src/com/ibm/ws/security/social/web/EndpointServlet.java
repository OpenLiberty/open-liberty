/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.ErrorHandlerImpl;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;

public class EndpointServlet extends HttpServlet {
    private static TraceComponent tc = Tr.register(EndpointServlet.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);
    private transient EndpointServices endpointServices = null;
    private transient ServletContext servletContext = null;
    private transient BundleContext bundleContext = null;
    private transient ServiceReference<EndpointServices> endpointServicesRef = null;
    private transient ReferrerURLCookieHandler referrerURLCookieHandler = null;

    private static final long serialVersionUID = 1L;

    public EndpointServlet() {
        super();
    }

    @Override
    public void init() {
        servletContext = getServletContext();
        bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        endpointServicesRef = bundleContext.getServiceReference(EndpointServices.class);
        referrerURLCookieHandler = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig().createReferrerURLCookieHandler();
        EndpointServices.setReferrerURLCookieHandler(referrerURLCookieHandler);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "init:" + servletContext + "   " + bundleContext + "  " + endpointServicesRef);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        this.doPost(request, response);
    }

    @Override
    @FFDCIgnore({ SocialLoginException.class })
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            EndpointServices socialLoginEndpointServices = getSocialLoginEndpointServices();
            // this won't be null, otherwise it throws Exception already
            socialLoginEndpointServices.handleSocialLoginRequest(request, response);
        } catch (SocialLoginException e) {
            e.logErrorMessage();
            ErrorHandlerImpl.getInstance().handleErrorResponse(response, HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private EndpointServices getSocialLoginEndpointServices() throws ServletException {
        if (endpointServicesRef != null) {
            endpointServices = bundleContext.getService(endpointServicesRef);
            if (endpointServices == null) {
                init(); // we need to init the bundleContext once more
                endpointServices = bundleContext.getService(endpointServicesRef);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getSocialLoginEndpointServices:" + servletContext + "   " + bundleContext + " " + endpointServices);
        }
        if (endpointServices == null) {
            // This should not happen
            throw new ServletException(TraceNLS.getFormattedMessage(this.getClass(),
                    TraceConstants.MESSAGE_BUNDLE,
                    "SOCIAL_LOGIN_OSGI_ENDPOINT_SERVICE_ERROR",
                    null,
                    "CWWKS5408E: A Social Login request cannot be processed because there is no Social Login feature available."));
        }
        return endpointServices;
    }

}
