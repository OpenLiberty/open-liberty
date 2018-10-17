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
package com.ibm.ws.microprofile.appConfig.classLoaderCache1.test;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class ClassLoaderCache1TestServlet extends FATServlet {
    @Test
    public void testClassLoaderCache() throws Exception {
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        System.out.println("Resolver: " + resolver.getClass().getName());
        Method getConfigCacheSize = resolver.getClass().getMethod("getConfigCacheSize");
        int size = (int) getConfigCacheSize.invoke(resolver, null);
        System.out.println("Before: " + size);
        assertEquals("Wrong number of Configs in the cache", 0, size);
        Config configA = resolver.getConfig(); //using the classloader unique to the war
        Config configB = resolver.getConfig(getRootClassLoader()); //using the common root classloader
        size = (int) getConfigCacheSize.invoke(resolver, null);
        System.out.println("After: " + size);
        assertEquals("Wrong number of Configs in the cache", 2, size);
    }

    private ClassLoader getRootClassLoader() {
        ClassLoader rootCL = this.getClass().getClassLoader();
        ClassLoader parentCL = rootCL;
        while (parentCL != null) {
            rootCL = parentCL;
            parentCL = rootCL.getParent();
        }
        System.out.println("Root ClassLoader: " + rootCL);
        return rootCL;
    }
}