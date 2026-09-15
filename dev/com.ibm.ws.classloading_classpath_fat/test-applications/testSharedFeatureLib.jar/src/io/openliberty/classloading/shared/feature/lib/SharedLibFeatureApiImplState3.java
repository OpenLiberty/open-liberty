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

import io.openliberty.classloading.feature.api.TestFeatureApi3;

/**
 * Third shared-library implementation, used by the State 3 probe of Test 2
 * (shared-library classloader test).
 * <p>
 * This class intentionally implements {@link TestFeatureApi3} — an interface that is
 * <em>never</em> referenced in States 1 or 2 and therefore never cached. Loading this
 * class in State 3 (feature re-added) forces a genuine cold delegation-chain walk.
 * A successful load confirms the re-added bundle is properly wired back into the
 * shared-library classloader chain.
 */
public class SharedLibFeatureApiImplState3 implements TestFeatureApi3 {

    @Override
    public String doWork() {
        return "SharedLibFeatureApiImplState3.doWork() called successfully from shared library (state 3)";
    }
}
