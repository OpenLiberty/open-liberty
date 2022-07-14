/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.support;

import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedWebModuleInfo;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public interface JaxWsWebContainerManager {

    /**
     * Create Web Router module based on the ModuleInfo parameter, it is used for EJB based web services
     *
     * @param moduleInfo
     * @return
     * @throws UnableToAdaptException
     */
    public ExtendedWebModuleInfo createWebModuleInfo(ExtendedModuleInfo moduleInfo, String contextPath) throws UnableToAdaptException;

}