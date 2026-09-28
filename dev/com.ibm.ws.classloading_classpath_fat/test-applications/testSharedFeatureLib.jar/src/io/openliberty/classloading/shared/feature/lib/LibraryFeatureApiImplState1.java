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
 * Shared-library implementation of {@link TestFeatureApi} for State 1.
 * <p>
 * Lives inside the shared library JAR and is loaded by the shared-library
 * {@code AppClassLoader}.
 */
public class LibraryFeatureApiImplState1 implements TestFeatureApi {

    @Override
    public String doWork() {
        return "LibraryFeatureApiImplState1.doWork() called successfully from shared library";
    }
}
