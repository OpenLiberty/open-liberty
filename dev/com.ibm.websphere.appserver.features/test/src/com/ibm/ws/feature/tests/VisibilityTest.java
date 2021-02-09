/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureInfo;
import com.ibm.ws.feature.utils.FeatureMapFactory;
import com.ibm.ws.feature.utils.FeatureVerifier;

public class VisibilityTest {

  private static Map<String, FeatureInfo> features = null; 

	@BeforeClass
	public static void setUpClass() {
		features = FeatureMapFactory.getFeatureMapFromFile("./visibility/");
	}

    /**
     * Tests to see if a feature has a dependency on an auto feature.
     */
	@Test
	public void testDependingOnAutoFeature() {
	    StringBuilder errorMessage = new StringBuilder();
	    for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();
	        FeatureInfo featureInfo = entry.getValue();

	        Set<String> dependentAutoFeatures = null;
	        for (String dependentFeature : featureInfo.getDependentFeatures()) {
	            FeatureInfo depFeatureInfo = features.get(dependentFeature);
	            if (depFeatureInfo != null && depFeatureInfo.isAutoFeature()) {
	                if (dependentAutoFeatures == null) {
	                    dependentAutoFeatures = new HashSet<>();
	                }
	                dependentAutoFeatures.add(dependentFeature);
	            }
	        }

            if (dependentAutoFeatures != null) {
                errorMessage.append("Found issues with " + featureName + '\n');
                errorMessage.append("     It depends on an auto feature: " + '\n');

                for (String autoFeature : dependentAutoFeatures) {
                    errorMessage.append("     " + autoFeature + '\n');
                }
            }
	    }

	    if (errorMessage.length() != 0) {
	        Assert.fail("Found features that depend on auto features: " + '\n' + errorMessage.toString());
	    }
	}
	
	// commented out for now.  Security function doesn't work well with parallel activation
	// currently, but can re-enable at times to see how things are looking and find places
	// where new features were added and parallel activation should match.
    //@Test
    public void testParallelActivation() {
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();
            FeatureInfo featureInfo = entry.getValue();
            if (!featureInfo.isParallelActivationEnabled()) {
                continue;
            }

            Set<String> dependentActivationMismatch = null;
            for (String dependentFeature : featureInfo.getDependentFeatures()) {
                FeatureInfo depFeatureInfo = features.get(dependentFeature);
                if (depFeatureInfo != null && !depFeatureInfo.isParallelActivationEnabled()) {
                    if (dependentActivationMismatch == null) {
                        dependentActivationMismatch = new HashSet<>();
                    }
                    dependentActivationMismatch.add(dependentFeature);
                }
            }

            if (dependentActivationMismatch != null) {
                errorMessage.append("Found issues with " + featureName + '\n');
                errorMessage.append("     It has parallel activation enabled but depends on features that do not: " + '\n');

                for (String activationMismatch : dependentActivationMismatch) {
                    errorMessage.append("     " + activationMismatch + '\n');
                }
            }
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found features that have parallel activation mismatch: " + '\n' + errorMessage.toString());
        }
    }

    /**
     * Confirms if disable all feature on conflict is not enabled, dependent 
     * features also have the same settings.
     */
    @Test
    public void testDisableAllFeaturesOnConflict() {
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();
            FeatureInfo featureInfo = entry.getValue();
            if (featureInfo.isDisableOnConflictEnabled()) {
                continue;
            }

            Set<String> dependentDisableOnConflictMismatch = null;
            for (String dependentFeature : featureInfo.getDependentFeatures()) {
                FeatureInfo depFeatureInfo = features.get(dependentFeature);
                if (depFeatureInfo != null && depFeatureInfo.isDisableOnConflictEnabled()) {
                    if (dependentDisableOnConflictMismatch == null) {
                        dependentDisableOnConflictMismatch = new HashSet<>();
                    }
                    dependentDisableOnConflictMismatch.add(dependentFeature);
                }
            }

            if (dependentDisableOnConflictMismatch != null) {
                errorMessage.append("Found issues with " + featureName + '\n');
                errorMessage.append("     It has disable on conflict disabled but depends on features that have it enabled: " + '\n');

                for (String disableOnConflictMismatch : dependentDisableOnConflictMismatch) {
                    errorMessage.append("     " + disableOnConflictMismatch + '\n');
                }
            }
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found features that have disable on conflict mismatch: " + '\n' + errorMessage.toString());
        }
    }

    /**
     * Features that are marked ga or beta should be in core or base edition in open liberty.
     * Features that are marked noship should be in full edition.  This test validates
     * that the edition is marked correctly.
     */
    @Test
    public void testEdition() {
        Set<String> possibleEditions = new HashSet<>();
        possibleEditions.add("base");
        possibleEditions.add("core");
        possibleEditions.add("full");

        Set<String> possibleKinds = new HashSet<>();
        possibleKinds.add("beta");
        possibleKinds.add("ga");
        possibleKinds.add("noship");
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();
            FeatureInfo featureInfo = entry.getValue();
            String kind = featureInfo.getKind();

            boolean errorFound = false;
            if (kind == null || "".equals(kind)) {
                errorMessage.append("Found issues with " + featureName + '\n');
                errorMessage.append("     It does not have a kind set\n");
                errorFound = true;
            }

            String edition = featureInfo.getEdition();
            if (edition == null || "".equals(edition)) {
                if (!errorFound) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorFound = true;
                }
                errorMessage.append("     It does not have an edition set\n");
            }

            if (!possibleEditions.contains(edition)) {
                if (!errorFound) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorFound = true;
                }
                errorMessage.append("     Edition is not a recognized value " + edition + '\n');
            }
            
            if (!possibleKinds.contains(kind)) {
                if (!errorFound) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorFound = true;
                }
                errorMessage.append("     Kind is not a recognized value " + kind + '\n');
            }
            
            if (!errorFound) {
                if ("full".equals(edition)) {
                    if (!"noship".equals(kind)) {
                        errorMessage.append("Found issues with " + featureName + '\n');
                        errorMessage.append("     Only noship features should have an edition of full\n");
                    }
                } else {
                    if ("noship".equals(kind)) {
                        errorMessage.append("Found issues with " + featureName + '\n');
                        errorMessage.append("     noship features should have an edition of full instead of " + edition + '\n');
                    }
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features that have edition set incorrectly: " + '\n' + errorMessage.toString());
        }
    }
    
    @Test
	public void testVisibility() {
        FeatureVerifier verifier = new FeatureVerifier(features);
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

    /**
     * Validates that features are in the right directory, public features in public
     * directory, protected in protected, auto in auto and others in private.
     */
    @Test
    public void testVisibility2() {
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();
            FeatureInfo featureInfo = entry.getValue();
            String visibility = featureInfo.getVisibility();
            if (visibility == null || "".equals(visibility)) {
                visibility = "private";
            }

            if (featureInfo.isAutoFeature()) {
                if (!"private".equals(visibility)) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorMessage.append("     Auto feature should have a visibility of private\n");
                }
                visibility = "auto";
            }
            File featureFile = featureInfo.getFeatureFile();
            String fileName = featureFile.getAbsolutePath();
            fileName = fileName.replace('\\', '/');
            String expectedPathName = "/visibility/" + visibility + "/";
            if ("public".equals(visibility)) {
                expectedPathName += (featureInfo.getShortName() + '/');
            }
            expectedPathName += (featureName + ".feature");
            if (!fileName.endsWith(expectedPathName)) {
                errorMessage.append("Found issues with " + featureName + '\n');
                errorMessage.append("     Feature is not in the expected directory " + expectedPathName + ".\n");
                errorMessage.append("     The feature's visibility " + visibility + " may not match the directory.\n");
                if ("public".equals(visibility)) {
                    errorMessage.append("     AND/OR the feature short name " + featureInfo.getShortName() + " may not match the directory.\n");
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features that appear to be in the wrong directory based off of their settings: " + '\n' + errorMessage.toString());
        }
    }

    /**
     * Tests that an auto feature has more than one feature in its filter.
     */
    @Test
    public void testAutoFeatures() {
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();
            FeatureInfo featureInfo = entry.getValue();

            if (!featureInfo.isAutoFeature()) {
                continue;
            }

            String[] filterFeatures = featureInfo.getAutoFeatures();
            if (filterFeatures.length <= 1) {
                errorMessage.append("Found issues with " + featureName + '\n');
                if (filterFeatures.length == 0) {
                    errorMessage.append("     Auto feature filter doesn't have any features listed.\n");
                } else {
                    errorMessage.append("     Auto feature filter only depends on one feature " + filterFeatures[0] + ".\n");
                    errorMessage.append("     The feature and/or bundle dependencies in this auto feature should just be a dependency of that feature\n");
                    errorMessage.append("     OR this should be turned into a private feature that " + filterFeatures[0] + " depends on.");
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found auto features who only depend on one feature: " + '\n' + errorMessage.toString());
        }
    }

    /**
     * This test makes sure that public features have properties files that match the long
     * feature name.  When moving features to the io.openliberty prefix from com.ibm.websphere.appserver
     * and vice versa, the properties file renames were missed a few times.  This unit test
     * makes sure that it is found in the build instead of having to be detected by hand.
     */
    @Test
    public void testLocalizationResources() {
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();
            FeatureInfo featureInfo = entry.getValue();

            if (!"public".equals(featureInfo.getVisibility())) {
                continue;
            }

            String shortName = featureInfo.getShortName();
            File featureFile = featureInfo.getFeatureFile();

            String fileName = featureFile.getAbsolutePath();
            fileName = fileName.replace('\\', '/');

            int lastSlash = fileName.lastIndexOf('/'); 
            String expectedFileName = fileName.substring(0, lastSlash + 1) + "/resources/l10n/" + featureName + ".properties";
            
            if (!new File(expectedFileName).exists()) {
                String expectedPropertiesFileRelPath = shortName + "/resources/l10n/" + featureName + ".properties"; 
                errorMessage.append("Found issues with " + featureName + '\n');
                errorMessage.append("     Expected to find file " + expectedPropertiesFileRelPath + '\n');
            }
        }
        
        if (errorMessage.length() != 0) {
            Assert.fail("Found features whose localization files are missing or in the wrong package: " + '\n' + errorMessage.toString());
        }
    }
}
