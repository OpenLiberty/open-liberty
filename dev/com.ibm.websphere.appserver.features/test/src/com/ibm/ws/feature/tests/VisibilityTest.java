/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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

package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureInfo;
import com.ibm.ws.feature.utils.FeatureMapFactory;
import com.ibm.ws.feature.utils.FeatureVerifier;

import aQute.bnd.header.Attrs;

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
            for (String dependentFeature : featureInfo.getDependentFeatures().keySet()) {
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
            for (String dependentFeature : featureInfo.getDependentFeatures().keySet()) {
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
            for (String dependentFeature : featureInfo.getDependentFeatures().keySet()) {
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
     * Features that are marked noship should be in full edition. This test validates
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
     *
     * Also validates that public features are in their short name directory and that non public features do not
     * have a "short name" or "also known as" property set and that auto features do not set "disable on conflict".
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
                expectedPathName += (featureInfo.getShortName() + '/') + (featureName + ".feature");

                if (!fileName.endsWith(expectedPathName)) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorMessage.append("     Feature is not in the expected directory " + expectedPathName + ".\n");
                    errorMessage.append("     The feature's visibility " + visibility + " may not match the directory.\n");
                    errorMessage.append("     AND/OR the feature short name " + featureInfo.getShortName() + " may not match the directory.\n");
                }
            } else {
                // private features that start with com.ibm.websphere.appserver.adminCenter.tool use IBM-ShortName for logic in the admin center
                if (featureInfo.getShortName() != null && !featureName.startsWith("com.ibm.websphere.appserver.adminCenter.tool")) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorMessage.append("     Feature contains IBM-ShortName, but it not a public feature.\n");
                }
                if (featureInfo.isDisableOnConflictSet() && featureInfo.isAutoFeature()) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorMessage.append("     Feature contains WLP-DisableAllFeatures-OnConflict, but it is an auto feature.\n");
                }
                if (featureInfo.isAlsoKnownAsSet()) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorMessage.append("     Feature contains WLP-AlsoKnownAs, but it not a public feature.\n");
                }
                String withoutFeatureDir = expectedPathName + (featureName + ".feature");
                String withFeatureShortNameDir = expectedPathName + featureInfo.getShortName() + "/" + (featureName + ".feature");
                if (!fileName.endsWith(withoutFeatureDir) && !fileName.endsWith(withFeatureShortNameDir)) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorMessage.append("     Feature is not in the expected directory " + withoutFeatureDir + " or " + withFeatureShortNameDir + ".\n");
                    errorMessage.append("     The feature's visibility " + visibility + " may not match the directory.\n");
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
     * feature name. When moving features to the io.openliberty prefix from com.ibm.websphere.appserver
     * and vice versa, the properties file renames were missed a few times. This unit test
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

    @Test
    public void testMissingBetaFeatures() {
        Set<String> expectedFailures = new HashSet<>();
        // The following features are marked no ship, but are not ready for beta yet.
        // If they get marked beta, they should be removed from this list.
        expectedFailures.add("io.openliberty.persistentExecutor.internal.ee-10.0"); // the persistentExecutor feature is no ship

        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();
            FeatureInfo featureInfo = entry.getValue();
            if (!"noship".equals(featureInfo.getKind())) {
                if (expectedFailures.contains(featureName)) {
                    errorMessage.append("Found issues with " + featureName + '\n');
                    errorMessage.append("     The feature is no longer marked noship, it should be removed from the expected failure set\n");
                }
            } else {
                if (!featureInfo.isAutoFeature()) {
                    boolean containsNoShipFeature = false;
                    boolean containsBetaFeature = false;
                    for (String depFeatureName : featureInfo.getDependentFeatures().keySet()) {
                        FeatureInfo depFeature = features.get(depFeatureName);
                        if (depFeature == null) {
                            continue;
                        }
                        if ("noship".equals(depFeature.getKind())) {
                            containsNoShipFeature = true;
                            break;
                        }
                        if ("beta".equals(depFeature.getKind())) {
                            containsBetaFeature = true;
                        }
                    }

                    if (!containsNoShipFeature && containsBetaFeature) {
                        if (!expectedFailures.contains(featureName)) {
                            errorMessage.append("Found issues with " + featureName + '\n');
                            errorMessage.append("     The feature is marked noship, but all dependencies are beta or ga\n");
                        }
                    }
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features that are marked noship, but contain only beta/ga features without a noship feature dependency: " + '\n' +
                        "If you recently marked a feature beta, you may need to update the feature to depend on noShip-1.0 feature, add or remove from the expected failures list in this test, or have something to fix.\n"
                        +
                        errorMessage.toString());
        }
    }

    /**
     * Tests to make sure that public and protected features are correctly referenced in a feature
     * when a dependent feature includes a public or protected feature with a tolerates attribute.
     */
    @Test
    public void testNonTransitiveTolerates() {
        StringBuilder errorMessage = new StringBuilder();
        // javaeePlatform and appSecurity are special because they have dependencies on each other.
        Set<String> nonSingletonToleratedFeatures = new HashSet<>();
        nonSingletonToleratedFeatures.add("io.openliberty.jakartaeePlatform-");
        nonSingletonToleratedFeatures.add("com.ibm.websphere.appserver.javaeePlatform-");
        nonSingletonToleratedFeatures.add("com.ibm.websphere.appserver.appSecurity-");
        Map<String, String> visibilityMap = new HashMap<>();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String feature = entry.getKey();
            int lastIndex = feature.indexOf('-');
            if (lastIndex == -1) {
                continue;
            }
            String baseFeatureName = feature.substring(0, lastIndex + 1);
            visibilityMap.put(baseFeatureName, entry.getValue().getVisibility());

        }
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();

            FeatureInfo featureInfo = entry.getValue();
            if (featureInfo.isAutoFeature()) {
                continue;
            }
            Set<String> processedFeatures = new HashSet<>();
            Map<String, Attrs> depFeatures = featureInfo.getDependentFeatures();
            Set<String> rootDepFeatureWithoutTolerates = new HashSet<>();
            for (Map.Entry<String, Attrs> depEntry : depFeatures.entrySet()) {
                Attrs attrs = depEntry.getValue();
                if (!attrs.containsKey("ibm.tolerates:")) {
                    rootDepFeatureWithoutTolerates.add(depEntry.getKey());
                }
            }

            Map<String, Set<String>> featureErrors = new HashMap<>();
            Map<String, Set<String>> toleratedFeatures = new HashMap<>();
            for (Map.Entry<String, Attrs> depFeature : depFeatures.entrySet()) {
                String depFeatureName = depFeature.getKey();
                FeatureInfo depFeatureInfo = features.get(depFeatureName);
                if (depFeatureInfo != null) {
                    for (Map.Entry<String, Attrs> depEntry2 : depFeatureInfo.getDependentFeatures().entrySet()) {
                        Map<String, Set<String>> tolFeatures = processIncludedFeature(featureName, rootDepFeatureWithoutTolerates,
                                                                                      depEntry2.getKey(), featureName + " -> " + depFeatureName, featureErrors, processedFeatures,
                                                                                      depEntry2.getValue().containsKey("ibm.tolerates:"),
                                                                                      depFeature.getValue().containsKey("ibm.tolerates:"));
                        if (tolFeatures != null) {
                            for (Entry<String, Set<String>> entry2 : tolFeatures.entrySet()) {
                                String key = entry2.getKey();
                                Set<String> existing = toleratedFeatures.get(key);
                                if (existing == null) {
                                    toleratedFeatures.put(key, entry2.getValue());
                                } else {
                                    existing.addAll(entry2.getValue());
                                }
                            }
                        }
                    }
                }
            }

            if (!toleratedFeatures.isEmpty()) {
                for (String depFeature : depFeatures.keySet()) {
                    String baseFeatureName = depFeature.substring(0, depFeature.lastIndexOf('-') + 1);
                    toleratedFeatures.remove(baseFeatureName);
                }
                if (!toleratedFeatures.isEmpty()) {
                    for (Iterator<String> i = toleratedFeatures.keySet().iterator(); i.hasNext();) {
                        String featureBase = i.next();
                        if (nonSingletonToleratedFeatures.contains(featureBase) || "private".equals(visibilityMap.get(featureBase))) {
                            i.remove();
                        }
                    }
                    if (!toleratedFeatures.isEmpty()) {
                        for (Entry<String, Set<String>> tolEntry : toleratedFeatures.entrySet()) {
                            errorMessage.append(featureName)
                                        .append(" must have a dependency on tolerated feature that start with ").append(tolEntry.getKey()).append(" in features ")
                                        .append(tolEntry.getValue()).append("\n\n");
                        }
                    }
                }
            }
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found features missing feature due to tolerates not being transitive for public and protected features: " + '\n' + errorMessage.toString());
        }
    }

    /**
     * Finds dependent features that are redundant because other dependent features already bring in those features.
     */
    //@Test
    public void testFeatureDependenciesRedundancy() {
        StringBuilder errorMessage = new StringBuilder();
        // javaeePlatform and appSecurity are special because they have dependencies on each other.
        Set<String> nonSingletonToleratedFeatures = new HashSet<>();
        nonSingletonToleratedFeatures.add("io.openliberty.jakartaeePlatform-");
        nonSingletonToleratedFeatures.add("com.ibm.websphere.appserver.javaeePlatform-");
        nonSingletonToleratedFeatures.add("com.ibm.websphere.appserver.appSecurity-");
        Map<String, String> visibilityMap = new HashMap<>();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String feature = entry.getKey();
            int lastIndex = feature.indexOf('-');
            if (lastIndex == -1) {
                continue;
            }
            String baseFeatureName = feature.substring(0, lastIndex + 1);
            visibilityMap.put(baseFeatureName, entry.getValue().getVisibility());

        }
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();

            FeatureInfo featureInfo = entry.getValue();
            if (featureInfo.isAutoFeature()) {
                continue;
            }
            Set<String> processedFeatures = new HashSet<>();
            Map<String, Attrs> depFeatures = featureInfo.getDependentFeatures();
            Set<String> rootDepFeatureWithoutTolerates = new HashSet<>();
            for (Map.Entry<String, Attrs> depEntry : depFeatures.entrySet()) {
                Attrs attrs = depEntry.getValue();
                if (!attrs.containsKey("ibm.tolerates:")) {
                    rootDepFeatureWithoutTolerates.add(depEntry.getKey());
                }
            }

            Map<String, Set<String>> featureErrors = new HashMap<>();
            Set<String> toleratedFeatures = new HashSet<>();
            for (Map.Entry<String, Attrs> depFeature : depFeatures.entrySet()) {
                String depFeatureName = depFeature.getKey();
                FeatureInfo depFeatureInfo = features.get(depFeatureName);
                if (depFeatureInfo != null) {
                    for (Map.Entry<String, Attrs> depEntry2 : depFeatureInfo.getDependentFeatures().entrySet()) {
                        Map<String, Set<String>> tolFeatures = processIncludedFeatureAndChildren(featureName, rootDepFeatureWithoutTolerates,
                                                                                                 depEntry2.getKey(), featureName + " -> " + depFeatureName, featureErrors,
                                                                                                 processedFeatures,
                                                                                                 depEntry2.getValue().containsKey("ibm.tolerates:"),
                                                                                                 depFeature.getValue().containsKey("ibm.tolerates:"));
                        if (tolFeatures != null) {
                            toleratedFeatures.addAll(tolFeatures.keySet());
                        }
                    }
                }
            }
            for (Map.Entry<String, Set<String>> errorEntry : featureErrors.entrySet()) {
                String depFeature = errorEntry.getKey();
                String baseFeatureName = depFeature.substring(0, depFeature.lastIndexOf('-') + 1);
                if (toleratedFeatures.contains(baseFeatureName)) {
                    continue;
                }
                errorMessage.append(featureName).append(" contains redundant feature ").append(depFeature)
                            .append(" because it is already in an included feature(s):\n");
                for (String errorPath : errorEntry.getValue()) {
                    errorMessage.append("    ").append(errorPath).append('\n');
                }
                errorMessage.append('\n');
            }
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found features contains redundant included features: " + '\n' + errorMessage.toString());
        }
    }

    private Map<String, Set<String>> processIncludedFeatureAndChildren(String rootFeature, Set<String> rootDepFeatures, String feature,
                                                                       String parentFeature, Map<String, Set<String>> featureErrors, Set<String> processedFeatures,
                                                                       boolean isTolerates, boolean hasToleratesAncestor) {
        Map<String, Set<String>> toleratedFeatures = processIncludedFeature(rootFeature, rootDepFeatures, feature, parentFeature, featureErrors,
                                                                            processedFeatures, isTolerates, hasToleratesAncestor);
        if (toleratedFeatures != null) {
            FeatureInfo featureInfo = features.get(feature);
            if (featureInfo != null) {
                for (Map.Entry<String, Attrs> depEntry : featureInfo.getDependentFeatures().entrySet()) {
                    Map<String, Set<String>> includeTolerates = processIncludedFeatureAndChildren(rootFeature, rootDepFeatures, depEntry.getKey(),
                                                                                                  parentFeature + " -> " + feature, featureErrors, processedFeatures,
                                                                                                  depEntry.getValue().containsKey("ibm.tolerates:"),
                                                                                                  isTolerates || hasToleratesAncestor);
                    if (includeTolerates != null) {
                        if (toleratedFeatures == null) {
                            toleratedFeatures = new HashMap<>(includeTolerates);
                        } else {
                            for (Entry<String, Set<String>> entry : includeTolerates.entrySet()) {
                                String key = entry.getKey();
                                Set<String> existing = toleratedFeatures.get(key);
                                if (existing == null) {
                                    toleratedFeatures.put(key, entry.getValue());
                                } else {
                                    existing.addAll(entry.getValue());
                                }
                            }
                        }
                    }
                }
            }
        }
        return toleratedFeatures;
    }

    private Map<String, Set<String>> processIncludedFeature(String rootFeature, Set<String> rootDepFeatures, String feature,
                                                            String parentFeature, Map<String, Set<String>> featureErrors, Set<String> processedFeatures,
                                                            boolean isTolerates, boolean hasToleratesAncestor) {
        if (!isTolerates && processedFeatures.contains(feature)) {
            return null;
        }
        Map<String, Set<String>> toleratedFeatures = null;
        if (isTolerates) {
            toleratedFeatures = new HashMap<>();
            HashSet<String> depFeatureWithTolerate = new HashSet<>();
            depFeatureWithTolerate.add(parentFeature);
            toleratedFeatures.put(feature.substring(0, feature.lastIndexOf('-') + 1), depFeatureWithTolerate);
            processedFeatures.add(feature);
        } else if (!hasToleratesAncestor && rootDepFeatures.contains(feature) && !feature.startsWith("com.ibm.websphere.appserver.eeCompatible-")
                   && !feature.startsWith("io.openliberty.mpCompatible-")) {
            Set<String> errors = featureErrors.get(feature);
            if (errors == null) {
                errors = new HashSet<String>();
                featureErrors.put(feature, errors);
            }
            errors.add(parentFeature);
        } else {
            processedFeatures.add(feature);
        }
        return toleratedFeatures;
    }
}
