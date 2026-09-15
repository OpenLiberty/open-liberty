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
package io.openliberty.classloading.feature.api;

/**
 * Secondary API interface exported by the testFeatureApi OSGi bundle.
 * <p>
 * This interface is intentionally <em>never</em> referenced during State 1
 * (feature present), so it is never cached by any classloader before the
 * feature is removed. Implementations that {@code implements TestFeatureApi2}
 * therefore force a genuine, cold delegation-chain walk when their class is
 * first loaded in State 2 (feature removed). Because the bundle has been
 * evicted, that walk fails with {@link NoClassDefFoundError} — the expected
 * signal that the classloader correctly sees the feature as absent.
 */
public interface TestFeatureApi2 {
    /** Returns a non-null, non-empty string token confirming the feature is live. */
    String doWork();
}
