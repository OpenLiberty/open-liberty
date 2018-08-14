/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import java.util.List;

import com.ibm.ws.app.manager.module.internal.ExtendedModuleInfoImpl;
import com.ibm.ws.app.manager.module.internal.ModuleClassLoaderFactory;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class SpringBootModuleInfo extends ExtendedModuleInfoImpl {
    private final SpringBootApplicationImpl springBootApplication;
    private final ClassLoader threadContextClassLoader;

    public SpringBootModuleInfo(ApplicationInfo appInfo, String moduleName, String path,
                                Container moduleContainer, Entry altDDEntry, List<ContainerInfo> moduleClassesContainers,
                                ModuleClassLoaderFactory classLoaderFactory,
                                SpringBootApplicationImpl springBootApplication) throws UnableToAdaptException {
        super(appInfo, moduleName, path, moduleContainer, altDDEntry, moduleClassesContainers, classLoaderFactory, ContainerInfo.Type.WEB_MODULE, SpringBootModuleInfo.class);
        this.springBootApplication = springBootApplication;
        this.threadContextClassLoader = springBootApplication.getClassLoadingService().createThreadContextClassLoader(this.getClassLoader());        
    }

    SpringBootApplicationImpl getSpringBootApplication() {
        return springBootApplication;
    }

    ClassLoader getThreadContextClassLoader() {
        return threadContextClassLoader;
    }

    void destroyThreadContextClassLoader() {
        if (springBootApplication.getClassLoadingService() != null) {
        	springBootApplication.getClassLoadingService().destroyThreadContextClassLoader(threadContextClassLoader);
        }
    }
}