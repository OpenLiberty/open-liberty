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
 * Shared-library implementation of {@link TestFeatureApi} used by the State 2a
 * cached-interface probe of Test 2 (shared-library classloader test).
 * <p>
 * This class implements the <em>already-cached</em> {@link TestFeatureApi} interface.
 * Because {@code TestFeatureApi} was successfully resolved via the shared-library
 * classloader in State 1, it remains in that loader's internal cache even after the
 * feature bundle is removed. Loading this class in State 2a therefore still succeeds
 * — the JVM resolves the supertype from the cache without walking to the
 * (now-absent) bundle.
 * <p>
 * This is the expected <em>success</em> probe for State 2a: it documents that the
 * shared-library {@code AppClassLoader} is not recycled on feature removal and that
 * previously-loaded types remain accessible. Compare with {@link SharedLibFeatureApiImplState2b},
 * which implements the never-before-seen {@code TestFeatureApi2} and is expected to
 * produce a {@link NoClassDefFoundError}.
 */
public class SharedLibFeatureApiImplState2a implements TestFeatureApi {

    @Override
    public String doWork() {
        return "SharedLibFeatureApiImplState2a.doWork() called successfully from shared library (state 2a, cached interface)";
    }
}
