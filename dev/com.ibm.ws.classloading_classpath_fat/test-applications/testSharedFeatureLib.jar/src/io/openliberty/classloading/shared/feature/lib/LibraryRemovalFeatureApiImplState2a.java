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
 * Test 3, State 2a cached-interface probe.
 * <p>
 * This class implements the <em>already-cached</em> {@link TestFeatureApi} interface.
 * Because {@code TestFeatureApi} was successfully resolved via the shared-library
 * classloader in State 1, it remains in that loader's internal cache even after the
 * library JAR is removed from the fileset. If the library {@code AppClassLoader} was
 * NOT evicted by {@code SharedLibraryImpl.delete()}, loading this class still succeeds.
 * <p>
 * Compare with {@link LibraryRemovalFeatureApiImplState2b}, which implements the never-before-seen
 * {@code TestFeatureApi2} and is expected to produce a {@link NoClassDefFoundError}
 * if the loader was properly evicted.
 */
public class LibraryRemovalFeatureApiImplState2a implements TestFeatureApi {

    @Override
    public String doWork() {
        return "LibraryRemovalFeatureApiImplState2a.doWork() called successfully — Test 3 State 2a (cached interface)";
    }
}
