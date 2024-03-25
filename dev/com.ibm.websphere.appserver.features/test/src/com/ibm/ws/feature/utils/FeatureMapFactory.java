/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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

package com.ibm.ws.feature.utils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class FeatureMapFactory {

    public static Map<String, FeatureInfo> getFeatureMapFromFile(String... fileLoc) {
        Map<String, FeatureInfo> features = new LinkedHashMap<String, FeatureInfo>();

        for (File featureFile : new FeatureFileList(fileLoc)) {
            FeatureInfo feature = new FeatureInfo(featureFile);
            features.put(feature.getName(), feature);

        }

        for (FeatureInfo feature : features.values()) {
            for (String autoFeature : feature.getAutoFeatures()) {
                if (features.containsKey(autoFeature)) {
                    features.get(autoFeature).addActivatingAutoFeature(feature.getName());
                }
            }
        }

        for (FeatureInfo feature : features.values()) {
            feature.lockActivatingAutoFeatures();
        }

        return features;
    }

}
