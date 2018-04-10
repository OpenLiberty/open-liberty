/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.feature.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FeatureVerifier {

	private Map<String, FeatureInfo> omni;

	//Send in the omnibus of features.
	public FeatureVerifier(Map<String, FeatureInfo> omni) {
		this.omni = omni;
	}

	public Map<String, Set<String>> verifyAllFeatures() {

		Set<String> lessVisible = null;

		Map<String, Set<String>> violations = new HashMap<String, Set<String>>();

		for (String feature : omni.keySet()) {
			lessVisible = findViolatingFeatures(new HashSet<String>(), feature, getLevelFromKind(omni.get(feature).getKind()), "KIND");
			lessVisible.addAll(findViolatingFeatures(new HashSet<String>(), feature, getLevelFromEdition(omni.get(feature).getEdition()), "EDITION"));

			violations.put(feature, lessVisible);
		}

		return violations;


	}

	public String printFeature(String feature) {
		return "Feature: [" + feature + "]   Edition: [" + omni.get(feature).getEdition() + "]   Kind: [" + omni.get(feature).getKind() + "]";
	}

	private Set<String> EMPTY_SET = new HashSet<String>();

	private Set<String> findViolatingFeatures(Set<String> seen, String featureName, int featureVisibility, String classification) {

		//short circuit if we start go go down a path we've already been down.
		if (!omni.containsKey(featureName) || (seen.contains(featureName)))
			return EMPTY_SET;

		seen.add(featureName);

		Set<String> featuresLessVisible = new HashSet<String>();

		String featureClass = (classification.contentEquals("KIND") ? omni.get(featureName).getKind() : omni.get(featureName).getEdition());
		int featureLevel = (classification.contentEquals("KIND") ? getLevelFromKind(featureClass) : getLevelFromEdition(featureClass));

		if (featureLevel < featureVisibility ) {
			featuresLessVisible.add(featureName);
		}

		for (String depFeature : omni.get(featureName).getDependentFeatures()) {
				featuresLessVisible.addAll(findViolatingFeatures(seen, depFeature, featureVisibility, classification));
		}

		return featuresLessVisible;

	}

	private int getLevelFromEdition(String edition) {
		switch(edition.toLowerCase()) {
		    case "full": return 0;
		    case "unsupported": return 1;
		    case "zos": return 2;
			case "nd": return 3;
			case "base": return 4;
			case "core": return 5;
			default: return 99;
		}
	}

	private int getLevelFromKind(String kind) {
		switch(kind.toLowerCase()) {
			case "noship": return 0;
			case "beta": return 1;
			case "ga": return 2;
			default: return 99;
		}
	}



}
