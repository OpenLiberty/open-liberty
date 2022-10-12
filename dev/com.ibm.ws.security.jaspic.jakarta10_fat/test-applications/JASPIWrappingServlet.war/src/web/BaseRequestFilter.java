/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base test filter that prints out to the response if the request and the response were wrapped by the JASPI AuthModule.
 */
public class BaseRequestFilter implements Filter {

    public String filterName;

    @Override
    public void init(FilterConfig arg0) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (((HttpServletRequest) request).getHeader("hasWrapper").equalsIgnoreCase("true")) {
            response.getWriter().println("The httpServletRequest in the " + filterName + " filter has been wrapped by httpServletRequestWrapper.");
        }
        if (((HttpServletResponse) response).getHeader("hasWrapper").equalsIgnoreCase("true")) {
            response.getWriter().println("The httpServletRestponse in the " + filterName + " filter has been wrapped by httpServletResponseWrapper.");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}

}