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
package testservlet40.listeners;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

/**
 * Test for the setSessionTimeout method from the servlet context API
 */
@WebListener
public class SessionTimeoutContextListenerImpl implements ServletContextListener {

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        // Setting up configurations for a servlet programmatically
        servletContext.setSessionTimeout(1); // Set the session timeout to 1 minute
        ServletRegistration servletRegistration = servletContext.addServlet("SessionTimeoutServlet", "servlets.SessionTimeoutServlet");
        if (servletRegistration != null) {
            servletRegistration.addMapping("/SessionTimeoutServlet");
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Do nothing.
    }

}
