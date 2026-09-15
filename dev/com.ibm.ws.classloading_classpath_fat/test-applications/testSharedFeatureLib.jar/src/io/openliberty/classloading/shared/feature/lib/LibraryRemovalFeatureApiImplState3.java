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
 * Test 3, State 3 probe.
 * <p>
 * This class intentionally implements {@link TestFeatureApi3} — an interface that is
 * <em>never</em> referenced in States 1 or 2 and therefore never cached. Loading it
 * in State 3 (after the original {@code server.xml} is restored with the library JAR
 * back in the fileset) forces a genuine cold delegation-chain walk. A successful load
 * confirms that Liberty recreated the shared-library {@code AppClassLoader} and that
 * the feature bundle is properly wired back into the classloader chain.
 */
public class LibraryRemovalFeatureApiImplState3 implements TestFeatureApi3 {
    @Override
    public String doWork() {
        return "LibraryRemovalFeatureApiImplState3.doWork() called successfully — Test 3 State 3";
    }
}
