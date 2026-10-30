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
package io.openliberty.classloading.shared.feature.lib2;

import io.openliberty.classloading.feature.api.TestFeatureApi3;

/**
 * Shared-library 2 implementation of {@link TestFeatureApi3} for State 3
 * (feature re-added).
 */
public class LibraryFeatureApi2ImplState3 implements TestFeatureApi3 {

    @Override
    public String doWork() {
        return "LibraryFeatureApi2ImplState3.doWork() called successfully (state 3, lib2 re-added)";
    }
}
