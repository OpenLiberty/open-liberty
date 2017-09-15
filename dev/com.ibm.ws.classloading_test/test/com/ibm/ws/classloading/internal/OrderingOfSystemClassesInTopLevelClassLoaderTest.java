/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.getClassLoadingService;

import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.GatewayConfiguration;

/**
 * Test the search order is as expected for a top-level application classloader
 */
public class OrderingOfSystemClassesInTopLevelClassLoaderTest extends GatewayClassLoaderTest {

    @Override
    ClassLoader createGatewayToParent(ClassLoader parentLoader) throws Exception {
        ClassLoadingServiceImpl classLoadingService = getClassLoadingService(parentLoader);
        GatewayConfiguration gwCfg = classLoadingService.createGatewayConfiguration();
        ClassLoaderConfiguration clCfg = classLoadingService.createClassLoaderConfiguration();
        clCfg.setDelegateToParentAfterCheckingLocalClasspath(true);
        ClassLoaderIdentity id = classLoadingService.createIdentity("UnitTest", "DirectGatewayClassLoaderTest");
        clCfg.setId(id);
        return classLoadingService.createTopLevelClassLoader(null, gwCfg, clCfg);
    }

}
