/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package websphere.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/*
 * Cause a ServletException to trigger the FilterErrorListener event
 *
 * map /FilterErrorEvent
 */
public class EventErrorFilter implements Filter {
    private static final String CLASS_NAME = EventErrorFilter.class.getName();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log(">>> doFilter ENTER");

        chain.doFilter(request, response);

        log("<<< doFilter after chain.doFilter. Throwing a ServletException");
        throw new ServletException(CLASS_NAME + "throws ServletException ");
    }

    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
}
