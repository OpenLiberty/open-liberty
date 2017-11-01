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
    private String contextRoot;

    /** Field to check whether Default Context Root is being used */
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
    }

    /** {@inheritDoc} */
    @Override
    public String getContextRoot() {
        //tWAS doesn't check the ibm-web-ext when deployed in an ear
        //however, there is a liberty test, testContextRootWarInEar_Ext, which verifies this behavior
        //since this could be considered a config related change, we can be different than tWAS and we don't want to break backward compatibility with Liberty 85
        // Note that the context root setup needs to happen here on the first call to getContextRoot(). Otherwise config overrides from server.xml will
        // not be available.
        if (this.contextRoot == null) {
            this.contextRoot = ContextRootUtil.getContextRoot(getContainer());

            if (contextRoot == null) {
                contextRoot = ContextRootUtil.getContextRoot(defaultContextRoot);
            }

        }
        return this.contextRoot;
    }

    /**
     * Sets the default context root name
     */
    public void setDefaultContextRoot(String defaultContextRoot) {
        this.defaultContextRoot = defaultContextRoot;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefaultContextRootUsed() {
        /**
         * If the module name is equal to the default context root,
         * it means that the default context root is being used.
         */
        return getName().equals(defaultContextRoot);
    }

}
