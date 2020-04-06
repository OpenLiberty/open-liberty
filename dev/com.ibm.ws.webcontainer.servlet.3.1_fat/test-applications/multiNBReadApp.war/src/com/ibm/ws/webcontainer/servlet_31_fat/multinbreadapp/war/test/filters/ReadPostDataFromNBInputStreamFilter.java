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
package com.ibm.ws.webcontainer.servlet_31_fat.multinbreadapp.war.test.filters;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.servlet_31_fat.multinbreadapp.war.test.listeners.TestAsyncNBReadListener;

/**
 * Servlet Filter implementation class ReadPostDataFromNBInputStreamFilter
 */
@WebFilter(urlPatterns = "/ReadPostDataFromNBInputStreamFilter/*", asyncSupported = true)
public class ReadPostDataFromNBInputStreamFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(ReadPostDataFromNBInputStreamFilter.class.getName());

    private final String classname = "ReadPostDataFromNBInputStreamFilter";

    /**
     * Default constructor.
     */
    public ReadPostDataFromNBInputStreamFilter() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        AsyncContext ac = this.setupAsyncListener(request, response);

        java.io.InputStream input = ((HttpServletRequest) request).getInputStream();
        LOG.info(classname + " input ->" + input);

        ReadListener readListener = new TestAsyncNBReadListener((ServletInputStream) input, (HttpServletRequest) request, (HttpServletResponse) response, ac, "ReadPostDataFromNBInputStreamFilter_NoWorkServlet");
        ((ServletInputStream) input).setReadListener(readListener);
        // pass the request along the filter chain
        chain.doFilter(request, response);
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        // TODO Auto-generated method stub
    }

    private AsyncContext setupAsyncListener(ServletRequest req, ServletResponse res) {
        // set up ReadListener to read data for processing

        // start async
        AsyncContext ac = req.startAsync();
        // set up async listener
        ac.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                LOG.info(classname + " my asyncListener in Filter Complete");

            }

            @Override
            public void onError(AsyncEvent event) {
                LOG.info(classname + event.getThrowable().toString());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                LOG.info(classname + " my asyncListener.onStartAsync");
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                LOG.info(classname + " Filter my asyncListener.onTimeout");
            }
        }, req, res);

        return ac;
    }

}
