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
package com.ibm.ws.microprofile.appConfig.classLoaderCache.test;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class ClassLoaderCacheTestServlet extends FATServlet {

    public static final String BEFORE = "BEFORE";
    public static final String AFTER = "AFTER";

    public void testClassLoaderCache(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String beforeStr = request.getParameter(BEFORE);
        String afterStr = request.getParameter(AFTER);
        int before = Integer.parseInt(beforeStr);
        int after = Integer.parseInt(afterStr);

        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        System.out.println("Resolver: " + resolver.getClass().getName());
        Method getConfigCacheSize = resolver.getClass().getMethod("getConfigCacheSize");

        int size = (int) getConfigCacheSize.invoke(resolver, null);
        System.out.println("Before: " + size);
        assertEquals("Wrong number of Configs in the cache", before, size);

        Config configA = resolver.getConfig(); //using the classloader unique to the war
        String testA = configA.getValue("TEST", String.class);
        assertEquals("Incorrect config value", "OK", testA);

        Config configB = resolver.getConfig(getRootClassLoader()); //using the common root classloader
        String testB = configB.getValue("TEST", String.class);
        assertEquals("Incorrect config value", "OK", testB);

        size = (int) getConfigCacheSize.invoke(resolver, null);
        System.out.println("After: " + size);
        assertEquals("Wrong number of Configs in the cache", after, size);
    }

    private static ClassLoader getRootClassLoader() {
        ClassLoader rootCL = ClassLoaderCacheTestServlet.class.getClassLoader();
        ClassLoader parentCL = rootCL;
        while (parentCL != null) {
            rootCL = parentCL;
            parentCL = rootCL.getParent();
        }
        System.out.println("Root ClassLoader: " + rootCL);
        return rootCL;
    }
}