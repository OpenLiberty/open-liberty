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
public interface LibertyClassLoadingService extends ClassLoadingService {
    @Override
    LibertyClassLoader createTopLevelClassLoader(List<Container> classPath, GatewayConfiguration gwConfig, ClassLoaderConfiguration config);

    @Override
    LibertyClassLoader createBundleAddOnClassLoader(List<File> classPath, ClassLoader gwClassLoader, ClassLoaderConfiguration config);

    @Override
    LibertyClassLoader createChildClassLoader(List<Container> classpath, ClassLoaderConfiguration config);

    @Override
    LibertyClassLoader getShadowClassLoader(ClassLoader loader);

    @Override
    LibertyClassLoader getSharedLibraryClassLoader(Library lib);

    @Override
    LibertyClassLoader createThreadContextClassLoader(ClassLoader applicationClassLoader);
}
