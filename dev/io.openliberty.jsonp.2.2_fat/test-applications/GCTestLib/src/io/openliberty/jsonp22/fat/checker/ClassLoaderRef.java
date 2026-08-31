/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.jsonp22.fat.checker;

import java.lang.ref.WeakReference;

/**
 * Shared static holder for the classloader WeakReference used by the GC test.
 *
 * Loaded by the gcTestLib shared library classloader, which is the parent of both
 * CacheCheckerApp's and BundledProviderApp's classloaders. There is therefore
 * exactly one instance of this class in the JVM, and its static field is shared
 * across both WARs.
 *
 * ClassLoaderRegistrationServlet (inside BundledProviderApp) calls set() with
 * BundledProviderApp's TCCL. CacheCheckerServlet (inside CacheCheckerApp) calls
 * get() to observe whether that classloader has been collected after undeploy.
 */
public class ClassLoaderRef {

    private static volatile WeakReference<ClassLoader> ref = null;

    /** Store a weak reference to the given classloader. */
    public static void set(ClassLoader cl) {
        ref = new WeakReference<>(cl);
    }

    /** Return the stored weak reference, or null if never set. */
    public static WeakReference<ClassLoader> get() {
        return ref;
    }
}
