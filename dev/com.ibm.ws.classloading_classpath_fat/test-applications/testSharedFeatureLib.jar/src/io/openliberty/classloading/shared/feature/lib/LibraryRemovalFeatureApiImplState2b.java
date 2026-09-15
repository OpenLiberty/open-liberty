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

import io.openliberty.classloading.feature.api.TestFeatureApi2;

/**
 * Test 3, State 2b fresh-interface probe.
 * <p>
 * This class intentionally implements {@link TestFeatureApi2} — an interface that is
 * <em>never</em> referenced in State 1 and therefore never cached by any classloader.
 * Loading it in State 2b (after the library JAR is removed from the fileset) forces
 * a genuine cold delegation-chain walk. Because the library's {@code AppClassLoader}
 * should have been evicted by {@code SharedLibraryImpl.delete()}, the expected outcome
 * is {@link NoClassDefFoundError} (or {@link ClassNotFoundException}).
 * <p>
 * Compare with {@link LibraryRemovalFeatureApiImplState2a}, which implements the
 * already-cached {@code TestFeatureApi} and whose load outcome reveals whether the
 * loader was evicted at all.
 */
public class LibraryRemovalFeatureApiImplState2b implements TestFeatureApi2 {
    @Override
    public String doWork() {
        return "LibraryRemovalFeatureApiImplState2b.doWork() called successfully — Test 3 State 2b (fresh interface)";
    }
}
