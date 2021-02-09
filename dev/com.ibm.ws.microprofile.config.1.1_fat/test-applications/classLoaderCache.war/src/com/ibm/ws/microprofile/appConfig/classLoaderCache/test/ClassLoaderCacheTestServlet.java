/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

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

        int size = getConfigCacheSize(resolver);
        System.out.println("Before: " + resolver);
        assertEquals("Wrong number of Configs in the cache - " + resolver, before, size);
        System.out.println("After: " + resolver);

        Config configA = resolver.getConfig(); //using the classloader unique to the war
        String testA = configA.getValue("TEST", String.class);
        assertEquals("Incorrect config value", "OK", testA);

        Config configB = resolver.getConfig(getRootClassLoader()); //using the common root classloader
        String testB = configB.getValue("TEST", String.class);
        assertEquals("Incorrect config value", "OK", testB);

        size = getConfigCacheSize(resolver);
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

    private int getConfigCacheSize(ConfigProviderResolver resolver) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
        int size = -1;
        try {
            //Our original ConfigProviderResolver implementations (v 1.1 - 1.4) have a getConfigCacheSize method just for test purposes
            Method getConfigCacheSizeMethod = resolver.getClass().getMethod("getConfigCacheSize");
            size = (int) getConfigCacheSizeMethod.invoke(resolver);
        } catch (NoSuchMethodException e) {
            //if that method was not found then we go looking for the private Map that the SmallRye impl uses for caching
            Field configsForClassLoaderField = resolver.getClass().getSuperclass().getDeclaredField("configsForClassLoader");
            configsForClassLoaderField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ClassLoader, Config> configsForClassLoader = (Map<ClassLoader, Config>) configsForClassLoaderField.get(resolver);
            size = configsForClassLoader.size();
        }
        return size;

    }
}