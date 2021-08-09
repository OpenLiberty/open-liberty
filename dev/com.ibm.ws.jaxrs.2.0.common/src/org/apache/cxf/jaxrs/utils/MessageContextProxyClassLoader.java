/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.cxf.jaxrs.utils;

public class MessageContextProxyClassLoader extends ClassLoader {
    private ClassLoader applicationClassLoader;
    private ClassLoader bundleClassLoader;

    private final Class<?> classes[];
    private boolean checkSystem;

    public MessageContextProxyClassLoader(ClassLoader parent, ClassLoader applicationClassLoader, ClassLoader bundleClassLoader) {
        super(parent);
        this.applicationClassLoader = applicationClassLoader;
        this.bundleClassLoader = bundleClassLoader;
        classes = null;
    }

    public MessageContextProxyClassLoader(ClassLoader parent, Class<?>[] cls) {
        super(parent);
        classes = cls;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (classes != null) {
            for (Class<?> c : classes) {
                if (name.equals(c.getName())) {
                    return c;
                }
            }
        }
        ClassLoader[] loaders = new ClassLoader[2];

        if (name.equals("org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy")) {
            loaders[0] = this.bundleClassLoader;
            loaders[1] = this.applicationClassLoader;
        } else {
            loaders[0] = this.applicationClassLoader;
            loaders[1] = this.bundleClassLoader;

        }
        for (ClassLoader loader : loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException cnfe) {
                // Try next
            } catch (NoClassDefFoundError cnfe) {
                // Try next
            }
        }
        if (checkSystem) {
            try {
                return getSystemClassLoader().loadClass(name);
            } catch (ClassNotFoundException cnfe) {
                // Try next
            } catch (NoClassDefFoundError cnfe) {
                // Try next
            }
        }
        throw new ClassNotFoundException(name);
    }

}
//Liberty Change for CXF End