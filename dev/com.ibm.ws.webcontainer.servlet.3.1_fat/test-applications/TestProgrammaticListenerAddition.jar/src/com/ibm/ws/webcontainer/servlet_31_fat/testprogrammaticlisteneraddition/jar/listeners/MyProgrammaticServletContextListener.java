/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners;

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
     * getEffectiveMajorVersion
     * getEffectiveMinorVersion
     * getDefaultSessionTrackingModes
     * getEffectiveSessionTrackingModes
     * getJspConfigDescriptor
     * getClassLoader
     * getVirtualServerName
     *
     * There are four methods that now throw an UnsupportedOperationException in servlet-3.1 that
     * did not previously:
     *
     * getEffectiveMajorVersion
     * getEffectiveMinorVersion
     * getClassLoader
     * getVirtualServerName
     *
     * The other methods we are testing just to be complete!
     *
     * The following methods are new to servlet-4.0 and will be tested in the Servlet 4.0 FAT bucket:
     * getSessionTimeout
     * getRequestCharacterEncoding
     * getResponseCharacterEncoding
     *
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        boolean getEffectiveMajorVersionExceptionThrown = false;
        boolean getEffectiveMinorVersionExceptionThrown = false;
        boolean getDefaultSessionTrackingModesExceptionThrown = false;
        boolean getEffectiveSessionTrackingModesExceptionThrown = false;
        boolean getJspConfigDescriptorExceptionThrown = false;
        boolean getClassLoaderExceptionThrown = false;
        boolean getVirtualServerNameExceptionThrown = false;

        ServletContext context = sce.getServletContext();

        try {
            context.log("effectiveMajorVersion: " + context.getEffectiveMajorVersion());
        } catch (UnsupportedOperationException uoe) {
            getEffectiveMajorVersionExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        try {
            context.log("effectiveMinorVersion: " + context.getEffectiveMinorVersion());
        } catch (UnsupportedOperationException uoe) {
            getEffectiveMinorVersionExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        try {
            context.log("defaultSessionTrackingModes: " + context.getDefaultSessionTrackingModes());
        } catch (UnsupportedOperationException uoe) {
            getDefaultSessionTrackingModesExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        try {
            context.log("effectiveSessionTrackingModes: " + context.getEffectiveSessionTrackingModes());
        } catch (UnsupportedOperationException uoe) {
            getEffectiveSessionTrackingModesExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        try {
            context.log("jspConfigDescriptor: " + context.getJspConfigDescriptor());
        } catch (UnsupportedOperationException uoe) {
            getJspConfigDescriptorExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        try {
            context.log("getClassLoader: " + context.getClassLoader());
        } catch (UnsupportedOperationException uoe) {
            getClassLoaderExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        try {
            context.log("getVirtualServerName: " + context.getVirtualServerName());
        } catch (UnsupportedOperationException uoe) {
            getVirtualServerNameExceptionThrown = true;
            context.log(uoe.getMessage());
        }

        context.log("getEffectiveMajorVersionExceptionThrown: " + getEffectiveMajorVersionExceptionThrown + " getEffectiveMinorVersionExceptionThrown: "
                    + getEffectiveMinorVersionExceptionThrown +
                    " getDefaultSessionTrackingModesExceptionThrown: " + getDefaultSessionTrackingModesExceptionThrown + " getEffectiveSessionTrackingModesExceptionThrown: "
                    + getEffectiveSessionTrackingModesExceptionThrown +
                    " getJspConfigDescriptorExceptionThrown: " + getJspConfigDescriptorExceptionThrown + " getClassLoaderExceptionThrown: " + getClassLoaderExceptionThrown
                    + " getVirtualServerNameExceptionThrown: " + getVirtualServerNameExceptionThrown);

        if (!getEffectiveMajorVersionExceptionThrown && !getEffectiveMinorVersionExceptionThrown && !getDefaultSessionTrackingModesExceptionThrown &&
            !getEffectiveSessionTrackingModesExceptionThrown && !getJspConfigDescriptorExceptionThrown && !getClassLoaderExceptionThrown &&
            !getVirtualServerNameExceptionThrown) {
            context.log("UnsupportedOperationException was not thrown.");
        }
    }
}
