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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.SingletonSetId;

public class FeatureJSONCreator {
    public static String buildJsonString(List<FeatureInfo> allInstalledFeatures,
                                         Map<String, FeatureInfo> nameToFeatureMap) {
        String json = "";
        ArrayList<SingletonSetId> singletonSets = new ArrayList<SingletonSetId>();
        singletonSets.add(new SingletonSetId("public", false));
        singletonSets.add(new SingletonSetId("protected", false));
        singletonSets.add(new SingletonSetId("private", false));
        ArrayList<String> features = new ArrayList<String>();
        json += "{\"nodes\":[\n";
        boolean first = true;
        for (FeatureInfo f : allInstalledFeatures) {
            if (!first)
                json += ",";
            int groupId = 0;
            if (f.getSingleton()) {
                SingletonSetId id = new SingletonSetId(f);
                if (!singletonSets.contains(id)) {
                    singletonSets.add(id);
                }
                groupId = singletonSets.indexOf(id);
            } else {
                groupId = singletonSets.indexOf(new SingletonSetId(f.getVisibility(), false));
            }
            features.add(f.getName());

            json += "{\"name\":\"" + f.getName() + "\",\"group\":" + groupId + "}\n";
            first = false;
        }
        json += "],\"links\":[\n";
        first = true;
        int sourceIndex;
        for (FeatureInfo f : allInstalledFeatures) {
            sourceIndex = features.indexOf(f.getName());
            Map<String, String> nesteds = f.getContentFeatures();
            int weight = nesteds.size() + 1;
            for (Map.Entry<String, String> nestedFeature : nesteds.entrySet()) {
                FeatureInfo nfi = nameToFeatureMap.get(nestedFeature.getKey());
                if (nfi == null) {
                    System.out.println("Error.. missing feature " + nestedFeature.getKey() + " referenced by " + f.getName());
                    throw new IllegalStateException();
                }
                int targetIndex = features.indexOf(nfi.getName());
                if (!first)
                    json += ",";
                json += "{\"source\":" + sourceIndex + ",\"target\":" + targetIndex + ",\"value\":" + weight + "}\n";
                first = false;

                if (nfi.getSingleton()) {
                    if (nestedFeature.getValue() != null) {
                        //singleton with tolerates.. 
                        String tolerate[] = nestedFeature.getValue().split(",");
                        for (String t : tolerate) {
                            SingletonSetId setId = new SingletonSetId(nfi);
                            String newFeature = setId.getSetFeatureNamePrefix() + "-" + t;
                            FeatureInfo tfi = nameToFeatureMap.get(newFeature);
                            System.out.println("looking for " + newFeature + " in map, owning feature " + f.getName() + " found? " + (tfi != null));
                            if (tfi != null) {
                                targetIndex = features.indexOf(tfi.getName());
                                if (!first)
                                    json += ",";
                                json += "{\"source\":" + sourceIndex + ",\"target\":" + targetIndex + ",\"value\":" + weight + "}\n";
                                first = false;
                            } else {
                                System.out.println("unable to process nested tolerated option " + newFeature + " as it is not known to the current set of features");
                            }
                        }
                    }
                }
            }
        }
        json += "]}\n";
        return json;
    }
}
