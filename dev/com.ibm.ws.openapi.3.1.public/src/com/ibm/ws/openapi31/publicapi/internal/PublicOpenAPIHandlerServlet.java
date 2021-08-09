/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31.publicapi.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.openapi31.OpenAPIAggregator;
import com.ibm.ws.openapi31.OpenAPIUtils;


public class PublicOpenAPIHandlerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final TraceComponent tc = Tr.register(PublicOpenAPIHandlerServlet.class);

    private transient OpenAPIAggregator oasProviderAggregator;
    
    private static final String ACCEPT_HEADER = "Accept";
    private static final String QUERY_COMPACT = "compact";
    private static final String MIME_JSON = "application/json";
    private static final String MIME_TEXT_PLAIN = "text/plain";

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException, ServletException {
        if ("GET".equals(request.getMethod())) {
            //Process input
            boolean compact = Boolean.valueOf(request.getParameter(QUERY_COMPACT));
            boolean yaml = true;
            response.setContentType(MIME_TEXT_PLAIN);
            
            String acceptHeader = "";
            acceptHeader = request.getHeader(ACCEPT_HEADER);
            if (acceptHeader != null && acceptHeader.equals(MIME_JSON)) {
            	response.setContentType(MIME_JSON);
                yaml = false;
            }
            String formatParam = request.getParameter("format");
            if (formatParam != null && formatParam.equals("json")) {
            	response.setContentType(MIME_JSON);
            	yaml = false;
            }

            boolean isSuccess = getOASProviderAggregator(false).getPublicDocumentation(request, compact,yaml,response);
            if(!isSuccess) {
                //OASProviderAggregator was deactivated, grab the new OASProviderAggregator (if available) and try again
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Couldn't get public API documentation. Retry after resetting the aggregator.");
                }
                getOASProviderAggregator(true).getPublicDocumentation(request, compact,yaml,response);
            }
        }
    }
    
    private synchronized OpenAPIAggregator getOASProviderAggregator(boolean resetAggregator) throws ServletException {
        if (oasProviderAggregator == null || resetAggregator) {
            setOASProviderAggregator();
        }
        return oasProviderAggregator;
    }
     
    private void setOASProviderAggregator() throws ServletException {
        BundleContext bundleContext = (BundleContext) getServletContext().getAttribute("osgi-bundlecontext");
        ServiceReference<OpenAPIAggregator> oasProviderAggregatorRef = bundleContext.getServiceReference(OpenAPIAggregator.class);

        if (oasProviderAggregatorRef == null) {
            // Couldn't find service, so throw the error.
            throw new ServletException(OpenAPIUtils.getOsgiServiceErrorMessage(this.getClass(), "OASProviderAggregator"));
        }

        oasProviderAggregator = bundleContext.getService(oasProviderAggregatorRef);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "oasProviderAggregator=" + oasProviderAggregator);
        }

        if (oasProviderAggregator == null) {
            // Couldn't find service, so throw the error.
            throw new ServletException(OpenAPIUtils.getOsgiServiceErrorMessage(this.getClass(), "OASProviderAggregator"));
        }
    }

}
