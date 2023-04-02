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
package com.ibm.ws.classloading.internal;

import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

import com.ibm.ws.classloading.LibertyClassLoader;

public abstract class LibertyLoader extends SecureClassLoader implements NoClassNotFoundLoader, LibertyClassLoader, DeclaredApiAccess {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    final ClassLoader parent;

    public LibertyLoader(ClassLoader parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public final Class<?> loadClassNoException(String name) {
        try {
            return loadClass(name, false, false, true);
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    @Override
    protected final Class<?> findClass(String className) throws ClassNotFoundException {
        return findClass(className, false);
    }

    protected abstract Class<?> loadClass(String className, boolean resolve, boolean onlySearchSelf, boolean returnNull) throws ClassNotFoundException;

    protected abstract Class<?> findClass(String className, boolean returnNull) throws ClassNotFoundException;
    
    @Override
    protected URL findResource(String resName) {
        return super.findResource(resName);
    }

    @Override
    protected Enumeration<URL> findResources(String resName) throws IOException {
        return super.findResources(resName);
    }

    @Override
    protected final Package definePackage(String packageName, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor,
                                    URL sealBase) throws IllegalArgumentException {
        return super.definePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    public abstract Bundle getBundle();
}
