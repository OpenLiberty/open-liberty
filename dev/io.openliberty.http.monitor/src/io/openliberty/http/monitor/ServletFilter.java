/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import java.io.IOException;

import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
public class ServletFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        System.out.println("DC Context Path 1 " + servletRequest.getServletContext().getContextPath());

        System.out.println("DC: SERVLET:inallalala");
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (IOException ioe) {
            throw ioe;
        } catch (ServletException se) {
            throw se;
        } finally {
            System.out.println("DC: SERVLET:outalalala");

            String httpRoute = (String) servletRequest.getAttribute("RESTFUL.HTTP.ROUTE");
            System.out.println("DC RESTFUL.HTTP.ROUTE " + httpRoute);

            if (HttpServletRequest.class.isInstance(servletRequest)) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
                System.out.println("DC CONTEXT PATH2 " + httpServletRequest.getContextPath());

                /*
                 * Obtain the pattern. RULES:
                 *
                 * 1. For /metrics context-route = "/metrics" httpServletMapping-> pattern is /*
                 * httpServletMapping-> match is null
                 *
                 * This resolves to http.route of /metrics
                 *
                 *
                 * pattern is /* match is /asdf URI /metrics/asdf
                 *
                 * This resolvse to http.route of /metrics/*
                 *
                 *
                 * 2. Non-existent path (after context-root) - Either null HttpServletMapping
                 * will be null with _pathInfo = "/" - OR Match
                 */
                if (servletRequest instanceof SRTServletRequest) {
                    SRTServletRequest srtServletRequest = (SRTServletRequest) servletRequest;
                    try {

                        IWebAppDispatcherContext wadc = srtServletRequest.getWebAppDispatcherContext();

                        if (wadc instanceof WebAppDispatcherContext40) {
                            WebAppDispatcherContext40 wadc40 = (WebAppDispatcherContext40) wadc;

                            HttpServletMapping httpServletMapping = wadc40.getServletMapping();
                            if (httpServletMapping != null) {
                                String pattern = httpServletMapping.getPattern();
                                System.out.println("Servlet Mapping pattern: " + pattern);
                            } else {
                                // log
                                // static page probably
                                // only really happens if only servlet
                            }

                        }

                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }

                }

                System.out.println("ServletFilter method" + httpServletRequest.getMethod());
                System.out.println("ServletFilter scheme " + httpServletRequest.getScheme());
                System.out.println("ServletFilter protocol (name + version)" + httpServletRequest.getProtocol());
                System.out.println("ServletFilter server name/addr" + httpServletRequest.getServerName() + " "
                        + httpServletRequest.getLocalAddr());
                System.out.println("ServletFilter server port" + httpServletRequest.getServerPort() + " "
                        + httpServletRequest.getLocalPort());

            }

            if (servletResponse instanceof HttpServletResponse) {
                HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                System.out.println("ServletFilter response" + httpServletResponse.getStatus());
            }

            HttpMetricsMonitor inst = HttpMetricsMonitor.getInstance();
            if (inst != null) {
                System.out.println("not null");
                inst.mockUpdate("fakeroute");
            } else {
                System.out.println("boooo");
            }

        }

    }

}
