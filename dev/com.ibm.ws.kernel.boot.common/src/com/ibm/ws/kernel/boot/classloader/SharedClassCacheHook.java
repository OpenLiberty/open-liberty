/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.classloader;

import java.lang.reflect.Method;
import java.net.URL;

import com.ibm.oti.shared.HelperAlreadyDefinedException;
import com.ibm.oti.shared.Shared;
import com.ibm.oti.shared.SharedClassHelperFactory;
import com.ibm.oti.shared.SharedClassURLHelper;

final class SharedClassCacheHook implements ClassLoaderHook {

    private static final SharedClassHelperFactory helperFactory = Shared.getSharedClassHelperFactory();

    private final SharedClassURLHelper sharedClassURLHelper;

    static ClassLoaderHook newInstance(ClassLoader loader) {
        if (helperFactory == null) {
            return null;
        }
        try {
            return new SharedClassCacheHook(helperFactory.getURLHelper(loader));
        } catch (HelperAlreadyDefinedException e) {
            // continue on as if not sharing
        }
        return null;
    }

    private SharedClassCacheHook(SharedClassURLHelper sharedClassHelper) {
        sharedClassURLHelper = sharedClassHelper;
    }

    @Override
    public byte[] loadClass(URL path, String name) {
        return sharedClassURLHelper.findSharedClass(path, name);
    }

    @Override
    public void storeClass(URL path, Class<?> clazz) {
        sharedClassURLHelper.storeSharedClass(path, clazz);
    }

}
