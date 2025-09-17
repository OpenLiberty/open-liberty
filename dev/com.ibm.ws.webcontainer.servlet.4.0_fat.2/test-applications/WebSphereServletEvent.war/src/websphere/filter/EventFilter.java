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

//map to "/*"
public class EventFilter implements Filter {
    private static final String CLASS_NAME = EventFilter.class.getName();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            log(">>> doFilter ENTER");
            chain.doFilter(request, response);
        } catch (Exception e) {
            log(CLASS_NAME + ". Exception [ " + e + "]");
        } finally {
            log("<<< doFilter EXIT");
        }
    }

    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
}
