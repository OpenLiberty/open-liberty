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
 * Shared-library implementation of {@link TestFeatureApi}.
 * <p>
 * This class lives inside the shared library JAR (not the WAR), so it is loaded
 * by the shared-library {@code AppClassLoader} that is cached in
 * {@code ClassLoadingServiceImpl.aclStore}. The WAR classloader delegates to
 * the shared-library loader to obtain this class.
 * <p>
 * The critical difference from the Test 1 scenario is that {@code aclStore} now
 * caches the <em>library's</em> {@code AppClassLoader} (keyed by the library
 * identity), not the WAR's. When the feature bundle is removed and re-added, the
 * library's cached loader retains the old bundle revision's type bindings for
 * {@code TestFeatureApi}. If Liberty produces a new bundle revision on re-add,
 * the cast at the call site will fail with {@code ClassCastException} — this is
 * the <em>Failure Mode B</em> described in the background document.
 */
public class SharedFeatureLibImpl implements TestFeatureApi {

    @Override
    public String doWork() {
        return "SharedFeatureLibImpl.doWork() called successfully from shared library";
    }
}
