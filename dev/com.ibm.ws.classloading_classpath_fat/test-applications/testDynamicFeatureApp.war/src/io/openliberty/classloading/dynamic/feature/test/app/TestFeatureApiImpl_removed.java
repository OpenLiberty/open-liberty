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
package io.openliberty.classloading.dynamic.feature.test.app;

import io.openliberty.classloading.feature.api.TestFeatureApi;

/**
 * In-WAR implementation of {@link TestFeatureApi} used by the State 2 "removed"
 * probe of Test 1 (direct application dependency test).
 * <p>
 * This class implements the <em>already-cached</em> {@link TestFeatureApi} interface.
 * Because {@code TestFeatureApi} was successfully loaded in State 1 it remains in the
 * WAR classloader's internal cache even after the feature bundle is removed. Loading
 * this class in State 2 therefore still succeeds — the JVM resolves the supertype from
 * the cache without walking the delegation chain to the (now-absent) bundle.
 * <p>
 * This is the expected <em>success</em> probe for State 2: it documents that the WAR
 * {@code AppClassLoader} is not recycled on feature removal and that previously-loaded
 * types remain accessible. Compare with {@link TestFeatureApiImpl2}, which implements
 * the never-before-seen {@code TestFeatureApi2} and is expected to produce a
 * {@link NoClassDefFoundError}.
 */
public class TestFeatureApiImpl_removed implements TestFeatureApi {

    @Override
    public String doWork() {
        return "TestFeatureApiImpl_removed.doWork() called successfully (state 2, cached interface)";
    }
}
