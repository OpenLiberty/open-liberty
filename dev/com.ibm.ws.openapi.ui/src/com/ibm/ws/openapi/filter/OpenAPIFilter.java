/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @version 1.0
 */
package com.ibm.ws.openapi.filter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;  

public class OpenAPIFilter implements Filter {
        /**
     * Filters out specific requests and takes the appropriate action for each
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        if (resp instanceof HttpServletResponse) {
            HttpServletResponse httpServletResp = (HttpServletResponse) resp;
            httpServletResp.setHeader("X-Frame-Options", "DENY");
            httpServletResp.setHeader("X-Content-Type-Options", "nosniff");
            httpServletResp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; connect-src *; style-src 'self' 'unsafe-inline'; img-src 'self' data:");
            chain.doFilter(req, resp);
        } else {
            chain.doFilter(req, resp);
        }
    }

    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig arg0) throws ServletException {}  
}
