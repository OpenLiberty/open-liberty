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
package com.ibm.ws.cdi.web.interfaces;

import javax.enterprise.inject.spi.BeanManager;

import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 *
 */
public interface CDIWebRuntime {

    /**
     * Indicate whether cdi is enabled for the current web module
     */
    public static final String CDI_ENABLED_ATTR = "com.ibm.ws.cdi.cdiEnabledApp";
    public static final String SESSION_NEEDS_PERSISTING = "com.ibm.ws.cdi.web.WeldServletRequestListener.SESSION_NEEDS_PERSISTING";

    /**
     * @param moduleMetaData
     * @return
     */
    BeanManager getModuleBeanManager(ModuleMetaData moduleMetaData);

    /**
     * @param isc
     * @return
     */
    boolean isCdiEnabled(IServletContext isc);

    /**
     * @return
     */
    BeanManager getCurrentBeanManager();

}
