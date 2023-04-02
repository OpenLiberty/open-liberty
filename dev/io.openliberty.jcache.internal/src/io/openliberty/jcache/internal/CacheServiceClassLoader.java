/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.jcache.internal;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ClassLoader that exists for the sole purpose of ensuring that the JCache features
 * use a different CachingProvider instance than applications.
 *
 * This might not be entirely necessary since we send a unified ClassLoader into the
 * CachingProvider, but it is best to be safe.
 */
@Trivial
public class CacheServiceClassLoader extends ClassLoader {
    private ClassLoader parent;

    /**
     * Create a new {@link CacheServiceClassLoader} instance with the parent
     * classloader.
     *
     * @param parent The parent {@link ClassLoader}.
     */
    public CacheServiceClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public String toString() {
        return new StringBuilder("CacheServiceClassLoader@")
                        .append(Integer.toHexString(System.identityHashCode(this)))
                        .append(" for ")
                        .append(parent)
                        .toString();
    }
}