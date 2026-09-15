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
package io.openliberty.classloading.feature.api;

/**
 * Minimal API interface exported by the testFeatureApi OSGi bundle.
 * Used by dynamic feature lifecycle tests to verify classloader behaviour
 * across feature install / remove / re-add cycles.
 */
public interface TestFeatureApi {
    /** Returns a non-null, non-empty string token confirming the feature is live. */
    String doWork();
}
