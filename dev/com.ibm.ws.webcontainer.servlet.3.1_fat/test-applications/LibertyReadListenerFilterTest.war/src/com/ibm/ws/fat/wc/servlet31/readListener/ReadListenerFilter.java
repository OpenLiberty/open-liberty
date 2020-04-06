/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.servlet31.readListener;

import java.io.IOException;
import java.util.Date;
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

/**
 *
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class ReadListenerFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(ReadListenerFilter.class.getName());

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain fc) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        //Get the IP address of client machine.
        String ipAddress = request.getRemoteAddr();

        //Log the IP address and current timestamp.
        LOG.info("ReadListenerFilter In filter IP " + ipAddress + ", Time "
                 + new Date().toString());

        // set up ReadListener to read data for processing
        // start async
        AsyncContext ac = req.startAsync();
        // set up async listener
        ac.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                LOG.info("ReadListenerFilter Filter Complete");

            }

            @Override
            public void onError(AsyncEvent event) {
                LOG.info("ReadListenerFilter" + event.getThrowable().toString());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                LOG.info(" ReadListenerFilter my asyncListener.onStartAsync");
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                LOG.info("ReadListenerFilter Filter my asyncListener.onTimeout");
            }
        }, req, res);

        ServletInputStream input = req.getInputStream();

        ReadListener readListener = new TestAsyncFilterReadListener(input, (HttpServletResponse) res, ac);
        input.setReadListener(readListener);

        fc.doFilter(req, res);

    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
        //Get init parameter
        String testParam = fc.getInitParameter("test-param");

        //Print the init parameter
        LOG.info("ReadListenerFilter In Filter,Test Param: " + testParam);

    }

}
