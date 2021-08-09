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
package com.ibm.ws.jaxws.webcontainer;

import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 *
 */
public interface JaxWsWebAppConfigurator {

    /**
     * Publish necessary info in jaxWsModuleInfo to WebAppConfig
     * 
     * @param jaxWsModuleInfo
     * @param servletContext
     */
    public void configure(JaxWsModuleInfo jaxWsModuleInfo, WebAppConfig webAppConfig);

}
