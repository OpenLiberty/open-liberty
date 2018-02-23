/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.app.deploy.extended;

import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.wsspi.adaptable.module.Container;

/**
 *
 */
public interface ApplicationInfoFactory {

    @Deprecated
    public ExtendedApplicationInfo createApplicationInfo(String appName, String preferredName,
                                                         Container container,
                                                         ApplicationClassesContainerInfo appClassesContainerInfo,
                                                         NestedConfigHelper configHelper);

    public ExtendedApplicationInfo createApplicationInfo(String appName, String preferredName, Container container,
                                                         ApplicationClassesContainerInfo appClassesContainerInfo,
                                                         NestedConfigHelper configHelper,
                                                         boolean useJandex);

    //public ExtendedApplicationInfo createApplicationInfo(String appMgrName, String preferredName, Container container,
    //                                                     ApplicationClassesContainerInfo appClassesContainerInfo,
    //                                                     NestedConfigHelper configHelper);

    public ExtendedEARApplicationInfo createEARApplicationInfo(String appName, String preferredName, Container container,
                                                               ApplicationClassesContainerInfo appClassesContainerInfo,
                                                               NestedConfigHelper configHelper,
                                                               boolean useJandex,
                                                               Container libDirContainer, AppClassLoaderFactory classLoaderFactory);

    //public ExtendedEARApplicationInfo createEARApplicationInfo(String appMgrName, String preferredName, Container container,
    //                                                           ApplicationClassesContainerInfo appClassesContainerInfo,
    //                                                           NestedConfigHelper configHelper,
    //                                                           Container libDirContainer, AppClassLoaderFactory classLoaderFactory);

    public void destroyApplicationInfo(ApplicationInfo appInfo);
}
