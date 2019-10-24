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
package com.ibm.aries.buildtasks.semantic.versioning.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for feature that represents a feature, and the singleton choices possible for the feature.
 */
public final class UnresolvedConstrainedFeature {
    public FeatureInfo feature;
    public Map<SingletonSetId, Set<SingletonChoice>> choices = new HashMap<SingletonSetId, Set<SingletonChoice>>();

    //copy constructor for when the maps are already built outside this class.. note the arg order is reversed
    //to avoid a clash with the other constructor after type erasure.
    public UnresolvedConstrainedFeature(Map<SingletonSetId, Set<SingletonChoice>> choices, FeatureInfo base) {
        this.feature = base;
        this.choices = choices;
    }

    //processes the choices from this feature only, not any nesteds, and represents them as the options for this feature.
    public UnresolvedConstrainedFeature(FeatureInfo base, Map<String, FeatureInfo> nameToFeatureMap) {
        if (base == null) {
            throw new IllegalStateException("passed null feature?!!");
        }
        if (base.getContentFeatures() == null) {
            throw new IllegalStateException("feature had null content feature map?" + base.getName());
        }
        this.feature = base;
        for (Map.Entry<String, String> featureToAttribs : base.getContentFeatures().entrySet()) {
            FeatureInfo part = nameToFeatureMap.get(featureToAttribs.getKey());
            if (part == null) {
                System.out.println("** Unable to locate feature info for key " + featureToAttribs.getKey() + " referenced by feature " + base.getName());
                throw new IllegalStateException();
            }
            if (part.getSingleton()) {
                SingletonSetId setId = new SingletonSetId(part);

                if (choices.get(setId) == null) {
                    choices.put(setId, new HashSet<SingletonChoice>());
                }

                //at least the requested version is included
                SingletonChoice choice = new SingletonChoice(part.getName(), true);
                choices.get(setId).add(choice);

                //retrieve the original include attributes, 
                String tolerates = featureToAttribs.getValue();
                if (tolerates != null) {
                    //for each additional version add it as a choice.
                    String[] versions = tolerates.split(",");
                    for (String version : versions) {
                        SingletonChoice tolerated = new SingletonChoice(setId.getSetFeatureNamePrefix() + "-" + version, false);
                        choices.get(setId).add(tolerated);
                    }
                }
            }
        }

        //if this feature IS a singleton, then we'll give it a dependency on itself, 
        //which will allow it to satisfy its own dependency when being added to a set.
        if (base.getSingleton()) {
            SingletonSetId setId = new SingletonSetId(base);
            if (choices.get(setId) == null) {
                choices.put(setId, new HashSet<SingletonChoice>());
            }
            SingletonChoice itself = new SingletonChoice(base.getName(), true);
            choices.get(setId).add(itself);
        }
    }

    //delegate hashcode/equals to the underlying feature.
    @Override
    public int hashCode() {
        return feature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UnresolvedConstrainedFeature) {
            UnresolvedConstrainedFeature other = (UnresolvedConstrainedFeature) obj;
            return feature.equals(other.feature);
        }
        return false;
    }

    @Override
    public String toString() {
        String v = feature.getName() + " " + feature.getVisibility() + " { ";
        for (Map.Entry<SingletonSetId, Set<SingletonChoice>> e : choices.entrySet()) {
            v += "{" + e.getKey() + " " + e.getValue() + "} ";
        }
        v += "}";
        return v;
    }

    public boolean noChoicesRemainInThisUCF() {
        boolean noChoices = true;
        if (choices != null) {
            for (Map.Entry<SingletonSetId, Set<SingletonChoice>> choice : choices.entrySet()) {
                if (choice.getValue().size() != 1) {
                    noChoices = false;
                    break;
                }
            }
        }
        return noChoices;
    }
}