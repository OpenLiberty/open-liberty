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
package com.ibm.ws.security.acme.internal.web;

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
 * Servlet Filter implementation class filterImpl
 */
//@WebFilter(dispatcherTypes = {
//				DispatcherType.REQUEST,
//				DispatcherType.FORWARD
//		}
//					, urlPatterns = { "/*" })
public class AcmeFilterImpl implements javax.servlet.Filter {

    /**
     * Default constructor.
     */
    public AcmeFilterImpl() {
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
        System.out.println("******* AcmeFilterImpl: Entered ACME servlet filter.");

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            /******************************************************************
             * Intercept LetsEncrypt challenge and route to servlet to handle
             ******************************************************************/
            String requestURI = httpRequest.getRequestURI();
            System.out.println("URI request length " + requestURI.length());

            if (requestURI.indexOf("/.well-known/acme-challenge") >= 0) {
                if (requestURI.indexOf("/.well-known/acme-challenge/AcmeAuthorization") >= 0) {
                    System.out.println("******* AcmeFilterImpl: Entered for ACME challenge forward Authorization -bypass!");
                    chain.doFilter(request, response);
                } else {
                    System.out.println("******* AcmeFilterImpl: Matched .well-known/acme-challenge/!");
                    String keydata = requestURI.substring(28, requestURI.length());
                    System.out.println("******* AcmeFilterImpl: Key data parsed: " + keydata);
                    String toReplace = requestURI.substring(requestURI.indexOf("/.well-known/acme-challenge"), requestURI.length());

                    System.out.println("******* AcmeFilterImpl: displaying toReplace: " + toReplace);
                    String newURI = requestURI.replaceAll(toReplace, "AcmeAuthorization?" + keydata);
                    System.out.println("******* AcmeFilterImpl: new URI: " + newURI);
                    httpRequest.getRequestDispatcher(newURI).forward(httpRequest, httpResponse);
                }
            } else {
                System.out.println("******* AcmeFilterImpl: No match on /.well-known/acme-challenge/ leave original URI in place");
                // Leave the original request URI in place
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        // TODO Auto-generated method stub
    }

}
