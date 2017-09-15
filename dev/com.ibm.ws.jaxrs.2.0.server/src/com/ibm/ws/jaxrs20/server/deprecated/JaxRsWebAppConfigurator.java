/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.server.deprecated;

import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 *
 */
@Deprecated
public interface JaxRsWebAppConfigurator {

    /**
     * Publish necessary info in jaxWsModuleInfo to WebAppConfig
     * 
     * @param jaxWsModuleInfo
     * @param servletContext
     */
    public void configure(JaxRsModuleInfo jaxWsModuleInfo, WebAppConfig webAppConfig);

}
