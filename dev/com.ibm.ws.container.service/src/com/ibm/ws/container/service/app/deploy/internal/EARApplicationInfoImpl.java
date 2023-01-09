/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.container.service.app.deploy.internal;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.AppClassLoaderFactory;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedEARApplicationInfo;
import com.ibm.wsspi.adaptable.module.Container;

/**
 *
 */
final class EARApplicationInfoImpl extends ApplicationInfoImpl implements ExtendedEARApplicationInfo {

    private final Container libDirContainer;
    private final AppClassLoaderFactory appClassLoaderFactory;
    private volatile ClassLoader appClassLoader;

    EARApplicationInfoImpl(String appName, J2EEName j2eeName, Container appContainer, NestedConfigHelper configHelper,
                           Container libDirContainer, AppClassLoaderFactory appClassLoaderFactory) {
        super(appName, j2eeName, appContainer, configHelper);
        this.libDirContainer = libDirContainer;
        this.appClassLoaderFactory = appClassLoaderFactory;
    }

    @Override
    public Container getLibraryDirectoryContainer() {
        return libDirContainer;
    }

    @Override
    public synchronized ClassLoader getApplicationClassLoader() {
        if (appClassLoader == null && appClassLoaderFactory != null) {
            appClassLoader = appClassLoaderFactory.createAppClassLoader();
        }
        return appClassLoader;
    }
}
