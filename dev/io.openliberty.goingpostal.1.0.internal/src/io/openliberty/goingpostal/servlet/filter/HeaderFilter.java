/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.goingpostal.servlet.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        System.out.println("Header Filter is running.");
        if (req instanceof HttpServletRequest && resp instanceof HttpServletResponse) {
            System.out.println("HttpServletResponse.");
            HttpServletResponse httpServletResp = (HttpServletResponse) resp;
            httpServletResp.setHeader("X-Clacks-Overhead", "GNU Terry Pratchett");
            System.out.println("Headers set.");
            chain.doFilter(req, resp);
        }
        chain.doFilter(req, resp);

    }

    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig arg0) throws ServletException {}
}
