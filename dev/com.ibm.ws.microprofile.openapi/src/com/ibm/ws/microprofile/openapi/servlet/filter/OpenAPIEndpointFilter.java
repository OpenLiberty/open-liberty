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
import java.nio.charset.StandardCharsets;


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
        initializeTracker(config.getServletContext());
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        initializeTracker(req.getServletContext());

        if (resp instanceof HttpServletResponse) {
            HttpServletResponse httpServletResp = (HttpServletResponse) resp;
            //ensure browsers don't store the result of any requests that go via the filter
            //Filter url mapping should ensure only HTML pages go via the fitler
            httpServletResp.setHeader("cache-control", "no-store");
            //Wrap request so that we can update the response content when it comes back
            HtmlResponseWrapper wrapper = new HtmlResponseWrapper(httpServletResp);

            chain.doFilter(req, wrapper);

            //Modify Response
            if (httpServletResp.getContentType() != null && httpServletResp.getContentType().contains("text/html")) {
                String content = new String(wrapper.getContentAsBytes(), StandardCharsets.UTF_8);
                // in case we have had a issue creating the tracker earlier - if so leave content as-is
                if(openAPIEndpointTracker != null) {
                    // replace the default URL for the document endpoint with the value that is being used
                    content = content.replaceAll("/openapi", openAPIEndpointTracker.getService().getOpenAPIDocUrl());
                }
                httpServletResp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                byte[] cbytes = content.getBytes(StandardCharsets.UTF_8);
                httpServletResp.setContentLength(cbytes.length);
                try (ServletOutputStream sos = httpServletResp.getOutputStream()) {
                    sos.write(cbytes);
                }
            }
        }
    }

    @Override
    public void destroy() {
        if(openAPIEndpointTracker != null) {
            openAPIEndpointTracker.close();
        }
    }

    /**
     * Initialize the ServiceTracker for the OpenAPIEndpointProvider
     *
     * @param context the Servlet Context
     */
    private void initializeTracker(ServletContext context){
        // If we have not previously configured the tracker, create one
        if(openAPIEndpointTracker == null){
            BundleContext bundleContext = (BundleContext) context.getAttribute("osgi-bundlecontext");
            // make sure we have a context before trying to use it to create the tracker for the OpenAPI doc endpoint
            if(bundleContext != null) {
                openAPIEndpointTracker = new ServiceTracker<>(bundleContext, OpenAPIEndpointProvider.class, null);
                openAPIEndpointTracker.open();
            }
        }
    }
}
