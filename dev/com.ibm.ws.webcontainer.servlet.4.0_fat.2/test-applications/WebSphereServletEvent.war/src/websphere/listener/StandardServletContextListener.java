/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package websphere.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/*
 * Standard Servlet API as usage example.
 *
 * Register via either:
 *      (1) @WebListener
 *      or
 *      (2) web.xml <listener> element, for example:
 *              <listener>
 *                <listener-class>com.webtier.listener.StandardServletContextListener</listener-class>
 *              </listener>
 *
 * NOTE: this register only works for standard servlet listeners!
 */

@WebListener
public class StandardServletContextListener implements ServletContextListener {
    private static final String CLASS_NAME = StandardServletContextListener.class.getName();
    public static final String STANDARD_ATT = "STANDARD_API";
    private final String STANDARD_ATT_VALUE = "STANDARD Servlet API using ServletContextListener.";

    ServletContext context = null;

    public StandardServletContextListener() {
        log("StandardServletContextListener constructor");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log("STANDARD: contextDestroyed, ServletContext using standard ServletContextListener [" + context + "]");

        context.setAttribute(STANDARD_ATT, null);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        context = sce.getServletContext();
        log("STANDARD: contextInitialized, ServletContext using standard ServletContextListener [" + context + "]");
        context.setAttribute(STANDARD_ATT, STANDARD_ATT_VALUE);
    }

    private void log(String s) {
        System.out.println("\t" + CLASS_NAME + ": " + s);
    }
}
