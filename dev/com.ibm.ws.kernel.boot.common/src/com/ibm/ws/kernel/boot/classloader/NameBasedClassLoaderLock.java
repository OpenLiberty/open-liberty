/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot.classloader;

import java.util.function.Supplier;

public class NameBasedClassLoaderLock {

    public static final Supplier<NameBasedClassLoaderLock> LOCK_SUPPLIER = new Supplier<NameBasedClassLoaderLock>() {
        @Override
        public NameBasedClassLoaderLock get() {
            return new NameBasedClassLoaderLock();
        }
    };

    private NameBasedClassLoaderLock() {
    }
}
