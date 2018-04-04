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
package com.ibm.ws.session.store.cache;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Class loader that exists for the sole purpose of ensuring that the session cache feature
 * uses a different CachingProvider instance than applications.
 */
@Trivial
class CachingProviderClassLoader extends ClassLoader {
    private ClassLoader parent;

    CachingProviderClassLoader(ClassLoader parent) {
        super(parent);
        this.parent = parent;
    }

    public String toString() {
        return new StringBuilder("CachingProviderClassLoader@")
                        .append(Integer.toHexString(System.identityHashCode(this)))
                        .append(" for ")
                        .append(parent)
                        .toString();
    }
}
