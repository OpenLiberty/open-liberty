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

import io.openliberty.classloading.feature.api.TestFeatureApi2;

/**
 * In-WAR implementation used by the State 2b NCDFE probe of Test 1
 * (direct application dependency test).
 * <p>
 * This class intentionally implements {@link TestFeatureApi2} — an interface that
 * is <em>never</em> referenced in State 1 and therefore never cached by any
 * classloader before the feature is removed. When the JVM attempts to resolve
 * {@code TestFeatureApi2} while loading this class in State 2b, it must walk the
 * full delegation chain: WAR classloader → GatewayClassLoader →
 * EquinoxClassLoader. Because the feature bundle has been evicted, that walk
 * fails with {@link NoClassDefFoundError} — the expected outcome confirming that
 * the classloader correctly observes the feature as absent.
 */
public class AppFeatureApiImplState2b implements TestFeatureApi2 {

    @Override
    public String doWork() {
        return "AppFeatureApiImplState2b.doWork() called successfully (state 2b, fresh interface)";
    }
}
