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

import io.openliberty.classloading.feature.api.TestFeatureApi3;

/**
 * Third in-WAR implementation, used by the State 3 probe of Test 1
 * (direct application dependency test).
 * <p>
 * This class intentionally implements {@link TestFeatureApi3} — an interface that
 * is <em>never</em> referenced in States 1 or 2 and therefore never cached. When
 * loaded in State 3 (feature re-added), it forces a genuine cold delegation-chain
 * walk. A successful load confirms the re-added bundle is properly wired back into
 * the classloader chain.
 */
public class TestFeatureApiImpl3 implements TestFeatureApi3 {

    @Override
    public String doWork() {
        return "TestFeatureApiImpl3.doWork() called successfully (state 3)";
    }
}
