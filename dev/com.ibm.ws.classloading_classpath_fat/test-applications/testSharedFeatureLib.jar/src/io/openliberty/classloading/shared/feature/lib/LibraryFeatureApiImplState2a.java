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
 * Shared-library implementation of {@link TestFeatureApi} for State 2a
 * (cached-interface probe).
 * <p>
 * Implements {@link TestFeatureApi}, which was cached during State 1.
 */
public class LibraryFeatureApiImplState2a implements TestFeatureApi {

    @Override
    public String doWork() {
        return "LibraryFeatureApiImplState2a.doWork() called successfully (state 2a, cached interface)";
    }
}
