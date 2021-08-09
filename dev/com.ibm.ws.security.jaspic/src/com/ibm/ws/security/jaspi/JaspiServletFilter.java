/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspi;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * This class is the JASPIC servlet filter that is added for each web app
 * that starts when the jaspic-1.1 feature is enabled. The filter is required
 * to enable the servlet request/response wrapper function in the JSR 196 spec.
 */

public class JaspiServletFilter implements Filter {

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {}

    /*
     * If a JASPI provider returned request/response wrappers in the MessageInfo object then
     * use those wrappers instead of the original request/response objects for web request.
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequestWrapper reqestWrapper =
                        (HttpServletRequestWrapper) request.getAttribute("com.ibm.ws.security.jaspi.servlet.request.wrapper");
        HttpServletResponseWrapper responseWrapper =
                        (HttpServletResponseWrapper) request.getAttribute("com.ibm.ws.security.jaspi.servlet.response.wrapper");
        ServletRequest req = reqestWrapper != null ? reqestWrapper : request;
        ServletResponse res = responseWrapper != null ? responseWrapper : response;
        chain.doFilter(req, res);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig arg0) throws ServletException {}
}
