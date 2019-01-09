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
package com.ibm.ws.kernel.boot.classloader;

import java.lang.reflect.Method;
import java.net.URL;

final class SharedClassCacheHook implements ClassLoaderHook {

    private static final Object sharedClassHelperFactoryClass;

    private static final Method getURLHelperMethod;

    private static final Method findSharedClassMethod;

    private static final Method storeSharedClassMethod;

    static {
        Object sharedClassHelperFactory = null;
        Method getURLHelper = null;
        Method findSharedClass = null;
        Method storeSharedClass = null;
        try {
            Class<?> sharedClass = Class.forName("com.ibm.oti.shared.Shared");
            Method m = sharedClass.getDeclaredMethod("getSharedClassHelperFactory");
            sharedClassHelperFactory = m.invoke(null);
            getURLHelper = m.getReturnType().getDeclaredMethod("getURLHelper", ClassLoader.class);
            Class<?> sharedClassURLHelperClass = getURLHelper.getReturnType();
            findSharedClass = sharedClassURLHelperClass.getDeclaredMethod("findSharedClass", URL.class, String.class);
            storeSharedClass = sharedClassURLHelperClass.getDeclaredMethod("storeSharedClass", URL.class, Class.class);
        } catch (Exception e) {
            // If things fail, assume we aren't using shared classes.
        }
        sharedClassHelperFactoryClass = sharedClassHelperFactory;
        getURLHelperMethod = getURLHelper;
        findSharedClassMethod = findSharedClass;
        storeSharedClassMethod = storeSharedClass;
    }

    private final Object sharedClassURLHelper;

    static ClassLoaderHook newInstance(ClassLoader loader) {
        Object helper = null;
        if (getURLHelperMethod != null && sharedClassHelperFactoryClass != null) {
            try {
                helper = getURLHelperMethod.invoke(sharedClassHelperFactoryClass, loader);
            } catch (Exception e) {
                // We should never get here.
                // If we do, we simply won't share for this ClassLoader
            }
        }
        return helper == null ? null : new SharedClassCacheHook(helper);
    }

    private SharedClassCacheHook(Object sharedClassHelper) {
        sharedClassURLHelper = sharedClassHelper;
    }

    @Override
    public byte[] loadClass(URL path, String name) {
        if (findSharedClassMethod != null) {
            // file protocol doesn't appear to work right now.
            if (path != null && "file".equals(path.getProtocol())) {
                return null;
            }
            try {
                return (byte[]) findSharedClassMethod.invoke(sharedClassURLHelper, path, name);
            } catch (Exception e) {
                // reflection failed.
            }
        }
        return null;
    }

    @Override
    public void storeClass(URL path, Class<?> clazz) {
        if (storeSharedClassMethod != null) {
            // file protocol doesn't appear to work right now.
            if (path != null && "file".equals(path.getProtocol())) {
                return;
            }
            try {
                storeSharedClassMethod.invoke(sharedClassURLHelper, path, clazz);
            } catch (Exception e) {
                // reflection failed.
            }
        }
    }

}
