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
 * First in-WAR implementation of {@link TestFeatureApi}, used by the State 1
 * probe of Test 1 (direct application dependency test).
 * <p>
 * Lives in the WAR classloader. A distinct class is used per lifecycle state so
 * that each probe forces {@code ClassLoader.loadClass()} to perform a genuine
 * re-lookup rather than returning a cached result from {@code findLoadedClass()}.
 */
public class TestFeatureApiImpl implements TestFeatureApi {

    @Override
    public String doWork() {
        return "TestFeatureApiImpl.doWork() called successfully (state 1)";
    }
}
