/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.internal;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.util.WSUtil;

/**
 * Simple Servlet filter that will return 404 for any request to /OSGI-INF/* or /OSGI-OPT/*
 * <br>
 * Alternate approach of using a filter that always returned 404, mapped to /OSGI-INF/* and /OSGI-OPT/*
 * failed as WebSphere doesnt always resolve the url before invoking a servlet filter.
 * <br>
 * This leads to missed invocations to paths like /./OSGI-INF/*
 * <br>
 * This Servlet filter is registered against a mapping of /* for every WAB we place in the webcontainer.
 */
public class OsgiDirectoryProtectionFilter implements Filter {

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        //We have no specific init/destroy behavior
    }

    @Override
    public void destroy() {
        //We have no specific init/destroy behavior
    }

    @Override
    public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
                    throws IOException, ServletException {
        //note that this method gets invoked for EVERY request to any of our deployed WABs
        //be very careful when adding trace here!

        // Only if the request is a HTTP request could we potentially deny access
        if (arg0 instanceof HttpServletRequest) {
            HttpServletRequest hsrq = (HttpServletRequest) arg0;
            HttpServletResponse hsrp = (HttpServletResponse) arg1;

            String ruri = hsrq.getServletPath(); //returns the servlet mapping part of the uri or "" if no servlet
            String pathinfo = hsrq.getPathInfo(); //returns the path part of the uri, or null if none.
            if (pathinfo != null) {
                ruri += pathinfo;
            }

            //Have WebSphere "resolve" the uri, thus /./OSGI-OPT will become /OSGI-OPT etc.
            //Also uppercase it to simplify the comparison test below.
            String uri = WSUtil.resolveURI(ruri);

            //Seems that under certain conditions the servlet path (in ruri) can be null in which
            //case WSUtil.resolveURI will be null.  Since we only need this value to test if it 
            //starts "/OSGI..." we can safely map the null to an empty string here.
            uri = (uri == null) ? "" : uri.toUpperCase();

            //prevent directory access attempts by appending /
            if (!uri.endsWith("/")) {
                uri += "/";
            }

            //Alternatively here, we could test startsWith("/OSGI-" to guard against this 1st block,
            //which would reduce the overhead for every request to a single test, at the cost of having
            //potentially 3 tests when actually matching a deny case.
            if (uri.startsWith("/OSGI-INF/") || uri.startsWith("/OSGI-OPT/")) {
                //Using FNFException to provide any message for the 404. 
                //The message is just informational, the 404 code conveys all that is required.
                FileNotFoundException fnf = new FileNotFoundException(hsrq.getRequestURI());
                hsrp.sendError(404, fnf.getMessage());
            } else {
                //Not a denied request, execute it normally
                arg2.doFilter(arg0, arg1);
            }
        } else {
            // Not a HTTPRequest so let it go through
            arg2.doFilter(arg0, arg1);
        }

    }
}