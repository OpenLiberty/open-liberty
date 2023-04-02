/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.servlet_40_fat.testprogrammaticlisteneraddition.jar.listeners;

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
     *
     * Test the following methods:
     *
     * getSessionTimeout
     * getRequestCharacterEncoding
     * getResponseCharacterEncoding
     *
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        boolean getSessionTimeoutExceptionThrown = false;
        boolean getRequestCharacterEncodingExceptionThrown = false;
        boolean getResponseCharacterEncodingExceptionThrown = false;

        ServletContext context = sce.getServletContext();

        try {
            context.log("getSessionTimeout: " + context.getSessionTimeout());
        } catch (UnsupportedOperationException uoe) {
            getSessionTimeoutExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        try {
            context.log("getRequestCharacterEncoding: " + context.getRequestCharacterEncoding());
        } catch (UnsupportedOperationException uoe) {
            getRequestCharacterEncodingExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        try {
            context.log("getResponseCharacterEncoding: " + context.getResponseCharacterEncoding());
        } catch (UnsupportedOperationException uoe) {
            getResponseCharacterEncodingExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        context.log("getSessionTimeoutExceptionThrown: " + getSessionTimeoutExceptionThrown + " getRequestCharacterEncodingExceptionThrown: "
                    + getRequestCharacterEncodingExceptionThrown +
                    " getResponseCharacterEncodingExceptionThrown: " + getResponseCharacterEncodingExceptionThrown);

        if (!getSessionTimeoutExceptionThrown && !getRequestCharacterEncodingExceptionThrown && !getResponseCharacterEncodingExceptionThrown) {
            context.log("UnsupportedOperationException was not thrown.");
        }
    }

}
