/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 * This file is based on Apache CXF's org.apache.cxf.common.util.ProxyClassLoader.
 * https://github.com/apache/cxf/blob/main/core/src/main/java/org/apache/cxf/common/util/ProxyClassLoader.java
 *
 *******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.rest.client40.internal;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Utility class loader that can be used to create proxies in cases where
 * the the client classes are not visible to the loader of the
 * service class.
 */
public class LibertyProxyClassLoader extends ClassLoader {
    private final Class<?>[] classes;
    private final Set<ClassLoader> loaders = new HashSet<>();
    private boolean checkSystem;

    public LibertyProxyClassLoader(ClassLoader parent) {
        super(parent);
        classes = null;
    }

    public LibertyProxyClassLoader(ClassLoader parent, Class<?>[] cls) {
        super(parent);
        classes = cls;
    }

    public void addLoader(ClassLoader loader) {
        if (loader == null) {
            checkSystem = true;
        } else {
            // Liberty Change Start
            if (!loaders.contains(loader)) {
                loaders.add(loader);
            }
            // Liberty Change End
        }
    }

    @Override
    @FFDCIgnore({ ClassNotFoundException.class, NoClassDefFoundError.class}) // Liberty Change
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (classes != null) {
            for (Class<?> c : classes) {
                if (name.equals(c.getName())) {
                    return c;
                }
            }
        }
        for (ClassLoader loader : loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException | NoClassDefFoundError cnfe) {
                // Try next
            }
        }
        if (checkSystem) {
            try {
                return getSystemClassLoader().loadClass(name);
            } catch (ClassNotFoundException | NoClassDefFoundError cnfe) {
                // Try next
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL findResource(String name) {
        for (ClassLoader loader : loaders) {
            URL url = loader.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }
}