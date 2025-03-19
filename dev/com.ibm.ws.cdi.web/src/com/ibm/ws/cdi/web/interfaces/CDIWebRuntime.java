/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.web.interfaces;

import javax.enterprise.inject.spi.BeanManager;

import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * A set of CDI runtime methods used by the web container integrations
 */
public interface CDIWebRuntime {

    /**
     * Indicate whether cdi is enabled for the current web module
     */
    public static final String CDI_ENABLED_ATTR = "com.ibm.ws.cdi.cdiEnabledApp";
    public static final String SESSION_NEEDS_PERSISTING = "com.ibm.ws.cdi.web.WeldServletRequestListener.SESSION_NEEDS_PERSISTING";

    /**
     * @see CDIRuntime.getModuleBeanManager(ModuleMetaData)
     */
    public BeanManager getModuleBeanManager(ModuleMetaData moduleMetaData);

    /**
     * Check if CDI is enabled for this module
     *
     * @param isc the IServletContext
     * @return true if CDI is enabled
     */
    public boolean isCdiEnabled(IServletContext isc);

    /**
     * @see CDIRuntime.getCurrentBeanManager()
     */
    public BeanManager getCurrentBeanManager();

}
