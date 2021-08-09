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
 * Servlet Filter implementation class ReadParameterFilter
 */
@WebFilter(urlPatterns = "/ReadParameterNBReadFilter/*", asyncSupported = true)
public class ReadParameterNBReadFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(ReadParameterNBReadFilter.class.getName());

    private final String classname = "ReadPostDataFromNBInputStreamFilter";

    /**
     * Default constructor.
     */
    public ReadParameterNBReadFilter() {
    }

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String param = request.getParameter("F003449Test");
        System.out.println("param value ->" + param);
        if (param.indexOf("ReadParameterNBReadFilter_NoWorkServlet") != -1) {
            request.setAttribute("F003449Filter", "Read_Parameter_Success");
        } else {
            request.setAttribute("F003449Filter", "Read_Parameter_Fail");
        }

        ServletInputStream input = request.getInputStream();
        AsyncContext ac = this.setupAsyncListener(request, response);
        ReadListener readListener = new TestAsyncNBReadListener(input, (HttpServletRequest) request, (HttpServletResponse) response, ac, "ReadParameterNBReadFilter_NoWorkServlet");
        input.setReadListener(readListener);

        chain.doFilter(request, response);
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    @Override
    public void init(FilterConfig fConfig) throws ServletException {
    }

    private AsyncContext setupAsyncListener(ServletRequest req, ServletResponse res) {
        // set up ReadListener to read data for processing

        // start async
        AsyncContext ac = req.startAsync();
        // set up async listener
        ac.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                LOG.info(classname + " Filter Complete");

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
