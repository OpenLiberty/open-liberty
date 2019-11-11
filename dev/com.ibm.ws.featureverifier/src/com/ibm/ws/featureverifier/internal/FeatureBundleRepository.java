/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;

/**
 * A class to make our messages clearer..
 * 
 * Working inside the runtime, we see packages from bundles.
 * 
 * But developers will want to know why a given bundle is loaded. So this class
 * will track the associations of bundles to features.
 */
public class FeatureBundleRepository {

    private final Map<String, Map<VersionRange, Set<FeatureInfo>>> symbolicNameToVersionRangeToFeature = new HashMap<String, Map<VersionRange, Set<FeatureInfo>>>();

    public void addFeature(FeatureInfo f) {
        Map<String, Set<VersionRange>> contentBundles = f.getContentBundles();
        if (contentBundles != null) {
            for (Map.Entry<String, Set<VersionRange>> contentEntry : contentBundles.entrySet()) {
                String symbName = contentEntry.getKey();
                if (!symbolicNameToVersionRangeToFeature.containsKey(symbName)) {
                    symbolicNameToVersionRangeToFeature.put(symbName, new HashMap<VersionRange, Set<FeatureInfo>>());
                }
                for (VersionRange v : contentEntry.getValue()) {
                    if (!symbolicNameToVersionRangeToFeature.get(symbName).containsKey(v)) {
                        symbolicNameToVersionRangeToFeature.get(symbName).put(v, new HashSet<FeatureInfo>());
                    }
                    symbolicNameToVersionRangeToFeature.get(symbName).get(v).add(f);
                }
            }
        }
    }

    public Set<FeatureInfo> getFeaturesForBundle(String symbolicName, Version version) {
        Set<FeatureInfo> results = new HashSet<FeatureInfo>();
        //do we know this symb name at all ?
        if (symbolicNameToVersionRangeToFeature.containsKey(symbolicName)) {
            Map<VersionRange, Set<FeatureInfo>> versionsToFeatures = symbolicNameToVersionRangeToFeature.get(symbolicName);
            for (Map.Entry<VersionRange, Set<FeatureInfo>> versionToTest : versionsToFeatures.entrySet()) {
                if (versionToTest.getKey().includes(version)) {
                    results.addAll(versionToTest.getValue());
                }
            }
        }
        return results;
    }

}
