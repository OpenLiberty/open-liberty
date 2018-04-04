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

package com.ibm.ws.feature.tests;

import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureInfo;
import com.ibm.ws.feature.utils.FeatureMapFactory;
import com.ibm.ws.feature.utils.FeatureVerifier;

import org.junit.Assert;

public class VisibilityTest {

  private static FeatureVerifier verifier = null;

	@BeforeClass
	public static void setUpClass() {
		Map<String, FeatureInfo> features = FeatureMapFactory.getFeatureMapFromFile("./visibility/");
		verifier = new FeatureVerifier(features);
	}

	@Test
	public void testVisibility() {
		Map<String, Set<String>> violations = verifier.verifyAllFeatures();

		boolean failed = false;
		StringBuilder errorMessage = new StringBuilder();


		for (String feature : violations.keySet()) {
			if (!violations.get(feature).isEmpty()) {
				failed = true;

				errorMessage.append("Found issues with " + verifier.printFeature(feature) + '\n');
				errorMessage.append("     It provisions less visible features: " + '\n');

				for (String featureKindViolators : violations.get(feature)) {
					errorMessage.append("     " + verifier.printFeature(featureKindViolators) + '\n');
				}
			}
		}

		Assert.assertFalse("Found features violating visibility conditions: " + '\n' + errorMessage.toString(), failed);

	}


}
