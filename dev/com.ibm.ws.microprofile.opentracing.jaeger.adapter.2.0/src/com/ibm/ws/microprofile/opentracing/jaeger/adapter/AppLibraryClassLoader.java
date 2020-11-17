/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.opentracing.jaeger.adapter;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Allows a library to be loaded from the app classloader
 * <p>
 * This class should be used when you want to access a library provided by the
 * user in their application.
 * <p>
 * Users need to provide three things:
 * <p>
 * Firstly a set of interfaces which wrap the parts of the library which the
 * user wishes to use. These may not reference any classes from the library.
 * <p>
 * Secondly an implementation of those interfaces, which may reference the
 * classes from the library.
 * <p>
 * Thirdly the application classloader, from which the library classes may be
 * loaded.
 * <p>
 * The user should create an instance of this classloader, reflectively load
 * classes from their implementation, cast them to their interface and then use
 * them as normal.
 */
public class AppLibraryClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> interfaces;

    /**
     * @param implementationUrls URLs to jars which contain the implementation classes
     * @param interfaces a List of interface classes which should be shared with the caller
     * @param parent the parent classloader (usually the application classloader)
     */
    public AppLibraryClassLoader(URL[] implementationUrls, List<Class<?>> interfaces, ClassLoader parent) {
        super(implementationUrls, parent);
        this.interfaces = interfaces.stream().collect(Collectors.toMap(c -> c.getCanonicalName(),
                                                                       c -> c));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Replace $ from inner class to dot
        String nname= name.replace('$', '.');
        // First check whether the requested class is an interface shared with the caller
        Class<?> result = this.interfaces.get(nname);
        // Otherwise attempt to load from the implementation URLs
        if (result == null) {
            result = super.findClass(name);
        }
        return result;
    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        // These permissions are used by classes loaded directly by this classloader.
        // These are the implementations of the adaptor interfaces which are liberty classes so we grant all permissions (as we would if they were loaded from a bundle).
        AllPermission allPermission = new AllPermission();
        PermissionCollection collection = allPermission.newPermissionCollection();
        collection.add(allPermission);
        return collection;
    }

}