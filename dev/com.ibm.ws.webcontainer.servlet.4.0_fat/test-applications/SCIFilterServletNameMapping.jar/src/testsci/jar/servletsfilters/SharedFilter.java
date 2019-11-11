/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testsci.jar.servletsfilters;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SharedFilter implements javax.servlet.Filter {
    private ServletContext servletContext;
    public static final String OUTPUT_TEXT_ATTRIBUTE = "OutputText";

    @Override
    public void destroy() {
        this.servletContext.log("***********< SharedFilter destroy invoked >****************");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        this.servletContext.log("***********< SharedFilter doFilter invoked >****************");
        request.setAttribute(SharedFilter.OUTPUT_TEXT_ATTRIBUTE, "SharedFilter.doFilter");
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.servletContext = filterConfig.getServletContext();
        this.servletContext.log("***********< SharedFilter init invoked >****************");
    }
}
