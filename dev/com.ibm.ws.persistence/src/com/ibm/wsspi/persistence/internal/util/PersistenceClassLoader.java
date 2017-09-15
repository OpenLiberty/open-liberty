/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence.internal.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.persistence.InMemoryMappingFile;

/**
 * TODO -- 151916
 * <p>
 * A ClassLoader that :
 * <li>Delegates findClass/getResource calls to other [n] ClassLoaders
 * <li>Users can register named resources with this ClassLoader that can be resolved via
 * ClassLoader.getResource(s).
 * <p>
 * 
 */
public class PersistenceClassLoader extends ClassLoader {
    final ClassLoader[] _delegates;
    private final Map<String, URL> _resourceMap;

    public PersistenceClassLoader(ClassLoader appLoader, ClassLoader... delegates) {
        super(appLoader);
        _delegates = delegates;
        _resourceMap = new ConcurrentHashMap<String, URL>();
    }

    @Override
    public URL getResource(final String resourceName) {
        if (System.getSecurityManager() == null) {
            return getResourceInternal(resourceName);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<URL>() {
                @Override
                public URL run() {
                    return getResourceInternal(resourceName);
                }
            });
        }
    }

    private URL getResourceInternal(String resourceName) {
        URL res = super.getResource(resourceName);
        if (res == null) {
            for (ClassLoader del : _delegates) {
                res = del.getResource(resourceName);
                if (res != null) {
                    return res;
                }
            }
            res = _resourceMap.get(resourceName);
        }
        return res;
    }

    @Override
    public Enumeration<URL> getResources(String resourceName) throws IOException {
        List<URL> res = new ArrayList<URL>();

        res.addAll(Collections.list(super.getResources(resourceName)));
        for (ClassLoader del : _delegates) {
            res.addAll(Collections.list(del.getResources(resourceName)));
        }
        URL url = _resourceMap.get(resourceName);
        if (url != null) {
            res.add(url);
        }

        return Collections.enumeration(res);
    }

    /**
     * Registers the provided File with our ClassLoader. This allows this resource to be looked up
     * via ClassLoader.getResource(fileName).
     */
    public void registerFileResource(final String file) {
        try {
            _resourceMap.put(file, new File(file).toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers the provided InMemoryMappingFile with our ClassLoader. This allows this resource
     * to be looked up via ClassLoader.getResource(fileName).
     */
    public URL registerInMemoryResource(final InMemoryMappingFile file) {
        URL url = DoPrivHelper.newInMemoryMappingFileURL(file);
        _resourceMap.put(file.getName(), url);

        return url;
    }

    @Trivial
    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        ClassNotFoundException cnfe = null;
        for (ClassLoader del : _delegates) {
            try {
                return del.loadClass(className);
            } catch (ClassNotFoundException e) {
                cnfe = e;
            }

        }
        throw cnfe;
    }

}
