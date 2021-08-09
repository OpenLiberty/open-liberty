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
package com.ibm.ws.app.manager.module.internal;

import java.util.List;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class ClientModuleInfoImpl extends ExtendedModuleInfoImpl implements ClientModuleInfo {

    private final String mainClassName;

    public ClientModuleInfoImpl(ApplicationInfo appInfo, String moduleName, String path,
                                Container moduleContainer, Entry altDDEntry, List<ContainerInfo> moduleClassesContainers,
                                String mainClassName, ModuleClassLoaderFactory classLoaderFactory) throws UnableToAdaptException {
        super(appInfo, moduleName, path, moduleContainer, altDDEntry, moduleClassesContainers, classLoaderFactory,
              ContainerInfo.Type.CLIENT_MODULE, ClientModuleInfo.class);

        this.mainClassName = mainClassName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.app.deploy.ClientModuleInfo#getMainClassName()
     */
    @Override
    public String getMainClassName() {
        return mainClassName;
    }
}
