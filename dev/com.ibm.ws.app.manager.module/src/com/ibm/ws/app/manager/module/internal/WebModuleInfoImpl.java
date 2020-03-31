/*******************************************************************************
 * Copyright (c) 2011-2020 IBM Corporation and others.
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
    private String contextRoot;

    /** Field to check whether Default Context Root is being used */
    private boolean isDefaultContextRootUsed;

    private String defaultContextRoot;

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
        super(appInfo, moduleName, path, moduleContainer, altDDEntry, moduleClassesContainers, classLoaderFactory, ContainerInfo.Type.WEB_MODULE, WebModuleInfo.class);

        this.contextRoot = contextRoot;
        this.isDefaultContextRootUsed = false;
    }

    /** {@inheritDoc} */
    @Override
    public String getContextRoot() {

        if (this.contextRoot == null) {

            this.contextRoot = ContextRootUtil.getContextRoot(getContainer());

            if (this.contextRoot == null) {
                if (defaultContextRoot != null && getName().equals(defaultContextRoot)) {
                    isDefaultContextRootUsed = true;
                }
                this.contextRoot = ContextRootUtil.getContextRoot(defaultContextRoot);
            }
        }

        return this.contextRoot;
    }

    /**
     * Sets the value of the default context root
     */
    public void setDefaultContextRoot(String root) {
        this.defaultContextRoot = root;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefaultContextRootUsed() {
        return isDefaultContextRootUsed;
    }

}
