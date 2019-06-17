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

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingConfigurationException;
import com.ibm.wsspi.kernel.service.utils.CompositeEnumeration;

@Trivial
final class Util {
    static final TraceComponent tc = Tr.register(Util.class);

    private Util() {
        throw null;
    }

    /**
     * Check that the parameter is not null.
     * 
     * @param msg the exception message to use if the parameter is null
     * @return the parameter if it isn't null
     * @throws ClassLoadingConfigurationException if the parameter is null
     */
    static <T> T ensureNotNull(String msg, T t) throws ClassLoadingConfigurationException {
        ensure(msg, t != null);
        return t;
    }

    /**
     * Check the given condition
     * 
     * @param msg the message to use if the condition is false
     * @throws ClassLoadingConfigurationException if the condition is false
     */
    static void ensure(String msg, boolean condition) throws ClassLoadingConfigurationException {
        if (!condition)
            throw new ClassLoadingConfigurationException(msg);
    }

    /**
     * Create a list
     */
    static <T> List<T> list(T... elems) {
        return Arrays.asList(elems);
    }

    /**
     * Ensure a collection is unmodifiable, but cope with <code>null</code> too
     */
    static <T> Collection<T> freeze(Collection<T> coll) {
        return coll == null ? null : Collections.unmodifiableCollection(coll);
    }

    /**
     * Ensure a list is unmodifiable, but cope with <code>null</code> too
     */
    static <T> List<T> freeze(List<T> list) {
        return list == null ? null : Collections.unmodifiableList(list);
    }

    /**
     * Ensure a set is unmodifiable, but cope with <code>null</code> too
     */
    static <T> Set<T> freeze(Set<T> set) {
        return set == null ? null : Collections.unmodifiableSet(set);
    }

    /**
     * Join several objects into a string
     */
    static <T> String join(Iterable<T> elems, String delim) {
        if (elems == null)
            return "";
        StringBuilder result = new StringBuilder();
        for (T elem : elems)
            result.append(elem).append(delim);
        if (result.length() > 0)
            result.setLength(result.length() - delim.length());
        return result.toString();
    }

    /**
     * Convert this resource name to a standard representation
     */
    static String normalizeResourceName(String resourceName) {
        if (resourceName.startsWith("/"))
            return resourceName.substring(1);
        return resourceName;
    }

    /**
     * When a class is to be looked up as a resource,
     * the class name needs to be transformed into a
     * resource name. That's what this method does.
     */
    static String convertClassNameToResourceName(String className) {
        return className.replace('.', '/') + ".class";
    }

    /**
     * Attempt to close a resource, disregarding any {@link IOException}s
     * that arise as a result.
     */
    @Trivial
    @FFDCIgnore(IOException.class)
    public static void tryToClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Compose two enumerations into one.
     * 
     * @param e1 an enumeration
     * @param e2 another enumeration
     * @return an enumeration containing every element from <code>e1</code> and <code>e2</code>
     */
    static <T> Enumeration<T> compose(Enumeration<T> e1, Enumeration<T> e2) {
        // return the composite of e1 and e2, or whichever is non-empty 
        return isEmpty(e1) ? e2
                        : isEmpty(e2) ? e1
                                        : new CompositeEnumeration<T>(e1).add(e2);
    }

    private static boolean isEmpty(Enumeration<?> e) {
        return !!!e.hasMoreElements();
    }

    static boolean isGlobalSharedLibraryLoader(AppClassLoader loader) {
        ClassLoaderIdentity id = loader.getKey();
        return ClassLoadingConstants.SHARED_LIBRARY_DOMAIN.equals(id.getDomain()) &&
               ClassLoadingConstants.GLOBAL_SHARED_LIBRARY_ID.equals(id.getId());
    }
}
