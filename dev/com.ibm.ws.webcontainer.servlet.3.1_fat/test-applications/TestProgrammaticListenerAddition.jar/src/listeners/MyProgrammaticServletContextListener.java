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
package listeners;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This ServletContextListener was added by MyServletContainerInitializer. The point of this ServletContextListener
 * is to call a method on the ServletContext and ensure that we get an UnsupportedOperationException when expected.
 *
 * There are some methods on the ServletContext that throw the UnsupportedOperationException when a listener is added
 * in a programmatic way.
 */
public class MyProgrammaticServletContextListener implements ServletContextListener {

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // do nothing

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        // Since this test is running in the context for servlet-3.1 + we expect an UnsupportedOperationException
        // to be thrown for the follow method call. The exception is caught and logged.
        try {
            String virtualServerName = context.getVirtualServerName();
            context.log("VirtualServerName: " + virtualServerName);
        } catch (Exception e) {
            context.log(e.getMessage());
        }

    }

}
