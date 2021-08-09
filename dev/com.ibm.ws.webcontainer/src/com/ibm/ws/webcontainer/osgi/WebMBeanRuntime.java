/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi;

import java.util.Iterator;

import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * Web module JSR77 registration interface.
 */
public interface WebMBeanRuntime {
    /**
     * Register a web module mbean.
     *
     * @param appName The name of the application enclosing the web module.  May be null.
     * @param moduleName The name of the web module which is to be registered.
     * @param container The container of the web module.
     * @param ddPath The path to the descriptor of the web module.
     * @param servletConfigs Metadata for the servlets of the web module.
     *
     * @return The service registration of the web module.
     */
    ServiceRegistration<?> registerModuleMBean(String appName, String moduleName, Container container, String ddPath, Iterator<IServletConfig> servletConfigs);

    /**
     * Register a servlet mbean.
     *
     * @param appName The name of the application enclosing the web module.  May be null.
     * @param moduleName The name of the web module which is to be registered.
     * @param servletName The name of the servlet.
     *
     * @return The service registration of the servlet.
     */
    ServiceRegistration<?> registerServletMBean(String appName, String moduleName, String servletName);
}
