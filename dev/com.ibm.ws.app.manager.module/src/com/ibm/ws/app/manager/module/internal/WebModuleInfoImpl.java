/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedWebModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * A simple immutable implementation of the ModuleInfo interface.
 */
public class WebModuleInfoImpl extends ExtendedModuleInfoImpl implements ExtendedWebModuleInfo {

    /** The context root for this web module */
    private final String contextRoot;

    /**
     * Creates a new instance of a web module with the class loader set to <code>null</code>
     *
     * @param modulePath
     * @param contextRoot
     * @throws UnableToAdaptException
     */
    public WebModuleInfoImpl(ApplicationInfo appInfo, String moduleName, String path, String contextRoot,
                             Container moduleContainer, Entry altDDEntry, List<ContainerInfo> moduleClassesContainers,
                             ModuleClassLoaderFactory classLoaderFactory) throws UnableToAdaptException {
        super(appInfo, moduleName, path, moduleContainer, altDDEntry, moduleClassesContainers, classLoaderFactory,
              ContainerInfo.Type.WEB_MODULE, WebModuleInfo.class);
        this.contextRoot = contextRoot;
    }

    /** {@inheritDoc} */
    @Override
    public String getContextRoot() {
        return this.contextRoot;
    }

}
