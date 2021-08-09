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
package com.ibm.ws.classloading.internal;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.GatewayConfiguration;

/**
 * Same as ShadowClassLoaderTest except that the wiring of the child ClassLoaders is done through
 * ClassLoadingService.createTopLevelClassLoader(List<URL>, ClassLoader, ClassLoaderConfiguration).
 */
public class BundleAddOnClassLoaderTest extends ShadowClassLoaderTest {

    private AppClassLoader topLevelClassLoader;

    @Override
    protected AppClassLoader createTopLevelClassLoader(List<Container> classPath, GatewayConfiguration gwConfig, ClassLoaderConfiguration clConfig) {
        topLevelClassLoader = super.createTopLevelClassLoader(classPath, gwConfig, clConfig);
        return topLevelClassLoader;
    }

    @Override
    protected AppClassLoader createChildClassLoader(List<Container> classpath, ClassLoaderConfiguration config) {
        config.setParentId(null);

        //messy... this is really shared library glue!
        //we can do this, because we know that the containers will all actually be jar files.. 
        //this assumption holds out for the tests in this project, but wouldn't for runtime containers..
        List<URL> urls = new ArrayList<URL>();
        for (Container c : classpath) {
            urls.addAll(c.getURLs());
        }
        List<File> files = new ArrayList<File>();
        for (URL u : urls) {
            if (u.toString().endsWith(".jar"))
                try {
                    files.add(new File(u.toURI()));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
        }

        return createBundleAddOnClassLoader(files, topLevelClassLoader, config);
    }

    @Override
    @Ignore
    @Test
    public void testNonSystemClasses() {
        // Not relevant for add-on classloader now that the add-on classloader's parent is hidden behind a gateway classloader.
        // This puts an early end to the recursion used to build the shadow classloader parent chain.
        // Rather than fixing shadowing to work on a GatwayClassLoader (no-one would use it) we disable this test for this specialisation.
    }

}
