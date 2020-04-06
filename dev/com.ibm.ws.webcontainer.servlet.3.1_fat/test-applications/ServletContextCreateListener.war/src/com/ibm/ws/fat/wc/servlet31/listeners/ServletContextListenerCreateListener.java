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
package com.ibm.ws.fat.wc.servlet31.listeners;

import java.util.EventListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;

/**
 *
 */
@WebListener
public class ServletContextListenerCreateListener implements ServletContextListener {

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        try {
            // Try to create a listener that does not implement one of the allowed interfaces
            @SuppressWarnings("unused")
            EventListener badListener = context.createListener(ListenerDoesNotImplementInterface.class);
        } catch (IllegalArgumentException iae) {
            context.log(iae.getMessage());

            //Try to create a listener that does implement one of the allowed interfaces. Add the created listener
            // and ensure it works properly
            try {
                EventListener goodListener = context.createListener(ListenerDoesImplementInterface.class);
                context.addListener(goodListener);
            } catch (Exception ex) {
                context.log(ex.getMessage());
            }
        } catch (ServletException se) {
            context.log(se.getMessage());
        }
    }

}
