/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.classloading;

import java.io.File;
import java.util.List;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.library.Library;

/**
 * This interface constrains the {@link ClassLoadingService} to use the {@link LibertyClassLoader}.
 */
public interface LibertyClassLoadingService<CL extends ClassLoader & LibertyClassLoader> extends ClassLoadingService {
    @Override
    CL createTopLevelClassLoader(List<Container> classPath, GatewayConfiguration gwConfig, ClassLoaderConfiguration config);

    @Override
    CL createBundleAddOnClassLoader(List<File> classPath, ClassLoader gwClassLoader, ClassLoaderConfiguration config);

    @Override
    CL createChildClassLoader(List<Container> classpath, ClassLoaderConfiguration config);

    @Override
    CL getShadowClassLoader(ClassLoader loader);

    @Override
    CL getSharedLibraryClassLoader(Library lib);

    CL getSharedLibrarySpiClassLoader(Library lib, String ownerId);

    @Override
    CL createThreadContextClassLoader(ClassLoader applicationClassLoader);
    
    /**
     * This method returns whether or not the provided ClassLoader is a ThreadContextClassLoader
     * and if the second provided ClassLoader is an AppClassLoader. And if the TCCL is for the AppClassLoader 
     *
     * @param tccl The thread context class loader object to analyze.
     * @param appClassLoader The app class loader object to analyze.
     * @return true if tccl is a ThreadContextClassLoader for an app classloader appClassLoader
     */
    boolean isThreadContextClassLoaderForAppClassLoader(ClassLoader tccl, ClassLoader appClassLoader);
}
