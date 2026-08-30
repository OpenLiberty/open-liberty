/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package test.delegation.bundled;

import java.io.IOException;
import java.net.URL;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.apache.commons.io.FileUtils;
import org.slf4j.MDC;

public class TestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        logClassResourceURL(FileUtils.class, "org/apache/commons/io/FileUtils.class");
        logClassResourceURL(MDC.class, "org/slf4j/MDC.class");
        chain.doFilter(servletRequest, servletResponse);
    }

    private void logClassResourceURL(Class<?> cls, String path) {
        URL url = cls.getClassLoader().getResource(path);
        System.out.println("BundledLibsTest " + cls.getName() + " => " + url.toExternalForm());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
