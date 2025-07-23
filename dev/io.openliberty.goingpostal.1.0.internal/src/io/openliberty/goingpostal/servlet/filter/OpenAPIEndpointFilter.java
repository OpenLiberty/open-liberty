/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.goingpostal.servlet.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Filter for altering the openapi path defined in the OpenAPI UI index.html
 *
 *
 */
public class OpenAPIEndpointFilter implements Filter {

    private static final TraceComponent tc = Tr.register(OpenAPIEndpointFilter.class);

    @Override
    public void init(FilterConfig config) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        // In case during filter Init, that the bundlecontext was irretrievable, attempt to initialize the tracker again

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

    }
}
