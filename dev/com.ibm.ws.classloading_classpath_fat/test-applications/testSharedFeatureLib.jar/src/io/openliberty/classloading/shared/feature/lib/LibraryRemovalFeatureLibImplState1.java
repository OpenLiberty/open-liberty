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
package io.openliberty.classloading.shared.feature.lib;

import io.openliberty.classloading.feature.api.TestFeatureApi;

/**
 * Test 3, State 1 impl class.
 *
 * <p>Used by {@code testLibraryJarRemovedFromSharedLib_LibraryPresent} to verify
 * that the shared-library classloader chain resolves correctly when both the
 * library JAR and the feature bundle are present.
 *
 * <p>A distinct class is used per lifecycle state so that
 * {@code ClassLoader.loadClass()} is forced to perform a genuine re-lookup through
 * the delegation chain rather than returning a result cached by
 * {@code findLoadedClass()} from a prior state.
 */
public class LibraryRemovalFeatureLibImplState1 implements TestFeatureApi {
    @Override
    public String doWork() {
        return "LibraryRemovalFeatureLibImplState1.doWork() called successfully — Test 3 State 1";
    }
}
