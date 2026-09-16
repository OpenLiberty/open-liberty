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
 * Shared-library implementation of {@link TestFeatureApi2} for State 2b
 * (fresh-interface probe).
 * <p>
 * Implements {@link TestFeatureApi2}, which was not previously loaded.
 */
public class LibraryFeatureApiImplState2b implements TestFeatureApi2 {

    @Override
    public String doWork() {
        return "LibraryFeatureApiImplState2b.doWork() called successfully (state 2b, fresh interface)";
    }
}
