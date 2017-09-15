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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

class TestUtilClassLoader extends URLClassLoader {
    static final ClassLoader NULL_LOADER = new ClassLoader() {
        @Override
        protected synchronized Class<?> loadClass(String className, boolean resolveClass) throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }
    };

    final Set<String> classNamesNotToLoad = new HashSet<String>();

    TestUtilClassLoader(URL[] urls) {
        // disable delegation by passing in a parent that loads nothing
        super(urls, NULL_LOADER);
    }

    TestUtilClassLoader doNotLoad(String... names) {
        for (String name : names)
            classNamesNotToLoad.add(name);
        return this;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (classNamesNotToLoad.contains(name))
            throw new ClassNotFoundException(name);
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException normal) {
            return findSystemClass(name);
        }
    }
}