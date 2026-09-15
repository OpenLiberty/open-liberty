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
 * Second shared-library implementation, used by the State 2 NCDFE probe of Test 2
 * (shared-library classloader test).
 * <p>
 * This class intentionally implements {@link TestFeatureApi2} — an interface that is
 * <em>never</em> referenced in State 1 and therefore never cached by any classloader
 * before the feature is removed. Loading this class in State 2 forces a genuine cold
 * delegation-chain walk: library loader → GatewayClassLoader → EquinoxClassLoader.
 * Because the feature bundle has been evicted, that walk fails with
 * {@link NoClassDefFoundError} — the expected signal that the classloader correctly
 * observes the feature as absent.
 */
public class SharedFeatureLibImpl2 implements TestFeatureApi2 {

    @Override
    public String doWork() {
        return "SharedFeatureLibImpl2.doWork() called successfully from shared library (state 2, fresh interface)";
    }
}
