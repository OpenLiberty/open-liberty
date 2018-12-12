/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.test.utils;

/**
 * Feature definition.  Used when preparing liberty servers.
 */
public class FATFeatureDef {
    public final String sourceFeatureManifestPath;
    public final String sourceFeatureJarPath;
    public final boolean isUserFeature;

    public static final boolean IS_USER_FEATURE = true;
    public static final boolean IS_SERVER_FEATURE = false;

    public FATFeatureDef(
        String featureManifestPath,
        String featureJarPath,
        boolean isUserFeature) {

        this.sourceFeatureManifestPath = featureManifestPath;
        this.sourceFeatureJarPath = featureJarPath;
        this.isUserFeature = isUserFeature;
    }
}
