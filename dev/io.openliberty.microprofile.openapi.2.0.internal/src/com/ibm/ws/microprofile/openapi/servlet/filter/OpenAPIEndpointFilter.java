/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.openapi.servlet.filter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIEndpointProvider;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Filter for altering the openapi path defined in the OpenAPI UI index.html
 *
 *
 */
public class OpenAPIEndpointFilter implements Filter {

    private static final TraceComponent tc = Tr.register(OpenAPIEndpointFilter.class);

    private ServiceTracker<OpenAPIEndpointProvider, OpenAPIEndpointProvider> openAPIEndpointTracker;

    @Override
    public void init(FilterConfig config) throws ServletException {
        BundleContext bundleContext = (BundleContext) config.getServletContext().getAttribute("osgi-bundlecontext");
        openAPIEndpointTracker = new ServiceTracker<>(bundleContext, OpenAPIEndpointProvider.class, null);
        openAPIEndpointTracker.open();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        if (resp instanceof HttpServletResponse) {
            HttpServletResponse httpServletResp = (HttpServletResponse) resp;
            //ensure browsers don't store the result of any requests that go via the filter
            //Filter url mapping should ensure only HTML pages go via the fitler
            httpServletResp.setHeader("cache-control", "no-store");
            //Wrap request so that we can update the response content when it comes back
            HtmlResponseWrapper wrapper = new HtmlResponseWrapper(httpServletResp);

            chain.doFilter(req, wrapper);

            //Modify Response
            if (resp.getContentType() != null && resp.getContentType().contains("text/html")) {
                String content = wrapper.getContentAsString();
                //replace the default URL for the document endpoint with the value that is being used
                content = content.replaceAll("/openapi", openAPIEndpointTracker.getService().getOpenAPIDocUrl());

                resp.getWriter().write(content);
            }
        }

    }

    @Override
    public void destroy() {
        openAPIEndpointTracker.close();
    }
}
