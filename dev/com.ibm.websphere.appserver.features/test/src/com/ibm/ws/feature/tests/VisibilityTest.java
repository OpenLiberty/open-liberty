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

package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureInfo;
import com.ibm.ws.feature.utils.FeatureInfo.ExternalPackageInfo;
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
     * Tests that all features with the same base name have the same visibility
     */
    @Test
    public void testMatchingVisibilitySingletonFeatures() {
        StringBuilder errorMessage = new StringBuilder();

        Map<String, Set<FeatureInfo>> baseFeatureNameMap = new HashMap<>();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            FeatureInfo featureInfo = entry.getValue();
            if (!featureInfo.isSingleton()) {
                continue;
            }
            String feature = entry.getKey();
            int lastIndex = feature.indexOf('-');
            if (lastIndex == -1) {
                continue;
            }
            String baseFeatureName = feature.substring(0, lastIndex + 1);
            Set<FeatureInfo> featureInfos = baseFeatureNameMap.get(baseFeatureName);
            if (featureInfos == null) {
                featureInfos = new HashSet<>();
                baseFeatureNameMap.put(baseFeatureName, featureInfos);
            }
            featureInfos.add(featureInfo);
        }

        for (Entry<String, Set<FeatureInfo>> featureInfosEntry : baseFeatureNameMap.entrySet()) {
            String baseFeatureName = featureInfosEntry.getKey();
            Set<FeatureInfo> featureInfos = featureInfosEntry.getValue();
            // Have fixed this one, but for now leaving the bad feature files in until can update a test
            // that will think that it is a breaking change.
            if (featureInfos.size() == 1 || baseFeatureName.equals("io.openliberty.connectors-")) {
                continue;
            }

            String visibility = null;
            for (FeatureInfo featureInfo : featureInfos) {
                if (visibility == null) {
                    visibility = featureInfo.getVisibility();
                } else if (!visibility.equals(featureInfo.getVisibility())) {
                    errorMessage.append("Mismatched visibility ").append(featureInfos).append("\n\n");
                    break;
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features with the same base name with errors: " + '\n' + errorMessage.toString());
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
                    errorMessage.append("     OR this should be turned into a private feature that " + filterFeatures[0] + " depends on.\n");
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
        // appSecurity features are special because they have dependencies on each other.
        Set<String> nonSingletonToleratedFeatures = new HashSet<>();
        nonSingletonToleratedFeatures.add("com.ibm.websphere.appserver.appSecurity-");
        Map<String, String> visibilityMap = new HashMap<>();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            FeatureInfo featureInfo = entry.getValue();
            if (featureInfo.isAutoFeature()) {
                continue;
            }
            String feature = entry.getKey();
            int lastIndex = feature.indexOf('-');
            if (lastIndex == -1) {
                continue;
            }
            String baseFeatureName = feature.substring(0, lastIndex + 1);
            visibilityMap.put(baseFeatureName, featureInfo.getVisibility());
        }

        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();

            if (featureName.contains("versionless")) {
                continue;
            }

            FeatureInfo featureInfo = entry.getValue();
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
                        boolean isTolerates = depEntry2.getValue().containsKey("ibm.tolerates:");
                        if (!isTolerates && processedFeatures.contains(depEntry2.getKey())) {
                            continue;
                        }
                        Map<String, Set<String>> tolFeatures = processIncludedFeature(featureName, rootDepFeatureWithoutTolerates,
                                                                                      depEntry2.getKey(), featureName + " -> " + depFeatureName, featureErrors, processedFeatures,
                                                                                      isTolerates,
                                                                                      depFeature.getValue().containsKey("ibm.tolerates:"), false);
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
            Assert.fail("Found features missing feature dependency due to tolerates not being transitive for public and protected features: " + '\n' + errorMessage.toString());
        }
    }

    /**
     * This test validates that jakarta and value-add features do not have public or protected tolerated features so that versionless
     * features and product extension / user features that have tolerates in their features for all ee levels can do so and avoid
     * having to do an auto feature.
     */
    @Test
    public void testTolerates() {
        StringBuilder errorMessage = new StringBuilder();
        // appSecurity features are special because they have dependencies on each other and are not singleton.
        // The other features in this list are public or protected features that did not change their feature base
        // name for the EE 8 to EE 9 transition.
        Set<String> allowedToleratedFeatures = new HashSet<>();
        allowedToleratedFeatures.add("com.ibm.websphere.appserver.servlet-");
        allowedToleratedFeatures.add("com.ibm.websphere.appserver.transaction-");
        allowedToleratedFeatures.add("com.ibm.websphere.appserver.appSecurity-");
        allowedToleratedFeatures.add("com.ibm.websphere.appserver.jdbc-");

        // data-1.0 will be updated to not tolerate EE 10 features when it ships with EE 11.0 and
        // can depend on EE 11 features because they are also in beta.
        // restfulWSLogging-3.0 hopefully never will see the light of day and will be done differently.
        Set<String> expectedFailingFeatures = new HashSet<>();
        expectedFailingFeatures.add("io.openliberty.data-1.0");
        expectedFailingFeatures.add("io.openliberty.restfulWSLogging-3.0");
        Map<String, String> visibilityMap = new HashMap<>();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            FeatureInfo featureInfo = entry.getValue();
            if (featureInfo.isAutoFeature()) {
                continue;
            }
            String feature = entry.getKey();
            int lastIndex = feature.indexOf('-');
            if (lastIndex == -1) {
                continue;
            }
            String baseFeatureName = feature.substring(0, lastIndex + 1);
            String oldVisibility = visibilityMap.put(baseFeatureName, featureInfo.getVisibility());
            if (oldVisibility != null && !oldVisibility.equals(featureInfo.getVisibility())) {
                if (oldVisibility.equals("public")) {
                    visibilityMap.put(baseFeatureName, "public");
                }
            }
        }

        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();

            FeatureInfo featureInfo = entry.getValue();
            if (featureInfo.isAutoFeature()) {
                continue;
            }

            // MicroProfile features do not currently follow this convention.  They may need to in the future.
            // openapi features are stabilized and were not updated to support EE 9.
            // opentracing is also now stabilized and does not support running with EE 10+
            if (featureInfo.getVisibility().equals("public") && (featureName.startsWith("io.openliberty.mp") || featureName.startsWith("com.ibm.websphere.appserver.mp")
                                                                 || featureName.startsWith("com.ibm.websphere.appserver.opentracing")
                                                                 || featureName.startsWith("com.ibm.websphere.appserver.openapi")
                                                                 || featureName.startsWith("com.ibm.websphere.appserver.microProfile-1."))) {
                continue;
            }

            Map<String, Attrs> depFeatures = featureInfo.getDependentFeatures();
            for (Map.Entry<String, Attrs> depEntry : depFeatures.entrySet()) {
                Attrs attrs = depEntry.getValue();
                if (attrs.containsKey("ibm.tolerates:")) {
                    String featureBaseName = depEntry.getKey().substring(0, depEntry.getKey().lastIndexOf('-') + 1);
                    String visibility = visibilityMap.get(featureBaseName);
                    if (!"private".equals(visibility) && !expectedFailingFeatures.remove(featureName) && !allowedToleratedFeatures.contains(featureBaseName)) {
                        errorMessage.append(featureName).append(" tolerates a feature with base name of ").append(featureBaseName).append("\n\n");
                    }
                }
            }
        }

        if (!expectedFailingFeatures.isEmpty()) {
            errorMessage.append("This test needs to be updated to remove features that no longer fail:\n");
            for (String featureName : expectedFailingFeatures) {
                errorMessage.append("    ").append(featureName);
            }
            errorMessage.append("\n\n");
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found public or protected features with tolerated public or protected features.  Update to use private feature toleration: " + '\n'
                        + errorMessage.toString());
        }
    }

    /**
     * Finds private and protected dependent features that are redundant because other dependent features already bring them in.
     * Public features are not included in this test since those features may be explicitly included just to show
     * which public features are enabled by a feature.
     */
    @Test
    public void testFeatureDependenciesRedundancy() {
        StringBuilder errorMessage = new StringBuilder();
        Map<String, String> visibilityMap = new HashMap<>();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            FeatureInfo featureInfo = entry.getValue();
            if (featureInfo.isAutoFeature()) {
                continue;
            }
            String feature = entry.getKey();
            int lastIndex = feature.indexOf('-');
            if (lastIndex == -1) {
                continue;
            }
            String baseFeatureName = feature.substring(0, lastIndex + 1);
            visibilityMap.put(baseFeatureName, featureInfo.getVisibility());

        }
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            String featureName = entry.getKey();

            FeatureInfo featureInfo = entry.getValue();
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
                        boolean isApiJarFalse = "false".equals(depFeature.getValue().get("apiJar")) || "false".equals(depEntry2.getValue().get("apiJar"));
                        Map<String, Set<String>> tolFeatures = processIncludedFeatureAndChildren(featureName, rootDepFeatureWithoutTolerates,
                                                                                                 depEntry2.getKey(), featureName + " -> " + depFeatureName, featureErrors,
                                                                                                 processedFeatures,
                                                                                                 depEntry2.getValue().containsKey("ibm.tolerates:"),
                                                                                                 depFeature.getValue().containsKey("ibm.tolerates:"), isApiJarFalse);
                        if (tolFeatures != null) {
                            toleratedFeatures.addAll(tolFeatures.keySet());
                        }
                    }
                }
            }
            for (Map.Entry<String, Set<String>> errorEntry : featureErrors.entrySet()) {
                String depFeature = errorEntry.getKey();
                String baseFeatureName = depFeature.substring(0, depFeature.lastIndexOf('-') + 1);
                if (toleratedFeatures.contains(baseFeatureName) || visibilityMap.get(baseFeatureName).equals("public")) {
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
                                                                       boolean isTolerates, boolean hasToleratesAncestor, boolean isApiJarFalse) {
        Map<String, Set<String>> toleratedFeatures = processIncludedFeature(rootFeature, rootDepFeatures, feature, parentFeature, featureErrors,
                                                                            processedFeatures, isTolerates, hasToleratesAncestor, isApiJarFalse);
        FeatureInfo featureInfo = features.get(feature);
        if (featureInfo != null) {
            for (Map.Entry<String, Attrs> depEntry : featureInfo.getDependentFeatures().entrySet()) {
                boolean depApiJarFalse = "false".equals(depEntry.getValue().get("apiJar"));
                Map<String, Set<String>> includeTolerates = processIncludedFeatureAndChildren(rootFeature, rootDepFeatures, depEntry.getKey(),
                                                                                              parentFeature + " -> " + feature, featureErrors, processedFeatures,
                                                                                              depEntry.getValue().containsKey("ibm.tolerates:"),
                                                                                              isTolerates || hasToleratesAncestor, isApiJarFalse || depApiJarFalse);
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
        return toleratedFeatures;
    }

    private Map<String, Set<String>> processIncludedFeature(String rootFeature, Set<String> rootDepFeatures, String feature,
                                                            String parentFeature, Map<String, Set<String>> featureErrors, Set<String> processedFeatures,
                                                            boolean isTolerates, boolean hasToleratesAncestor, boolean isApiJarFalse) {
        Map<String, Set<String>> toleratedFeatures = null;
        if (isTolerates) {
            toleratedFeatures = new HashMap<>();
            HashSet<String> depFeatureWithTolerate = new HashSet<>();
            depFeatureWithTolerate.add(parentFeature);
            toleratedFeatures.put(feature.substring(0, feature.lastIndexOf('-') + 1), depFeatureWithTolerate);
            processedFeatures.add(feature);
        } else if (!hasToleratesAncestor && rootDepFeatures.contains(feature) && !feature.startsWith("com.ibm.websphere.appserver.eeCompatible-")
                   && !feature.startsWith("io.openliberty.mpCompatible-") && !feature.startsWith("io.openliberty.servlet.internal-")) {
            if (!isApiJarFalse) {
                Set<String> errors = featureErrors.get(feature);
                if (errors == null) {
                    errors = new HashSet<String>();
                    featureErrors.put(feature, errors);
                }
                errors.add(parentFeature);
            }
        } else {
            processedFeatures.add(feature);
        }
        return toleratedFeatures;
    }

    enum ExternalPackageType {
        IBM_API("ibm-api", false), SPEC("spec", false), STABLE("stable", false), THIRD_PARTY("third-party", false), IBM_SPI("ibm-spi", true);

        final String type;
        final boolean isSPI;

        ExternalPackageType(String type, boolean isSPI) {
            this.type = type;
            this.isSPI = isSPI;
        }
    }

    /**
     * For documentation features APIs and SPIs need to be in a public feature for them to be exposed in the documentation for that public feature.
     * If the APIs and SPIs are listed in a private feature the documentation auto gen logic will not add them to the public feature even when
     * the public feature depends on the private feature. This test makes sure that APIs and SPIs listed in private features are also included
     * in the public features that depend on those private features.
     *
     * To help keep ibm-api, spec, stable, third-party, and spi separated, there is a test for each one of them.
     */
    @Test
    public void testIBMAPIsInPublicFeatures() {
        Map<String, Set<MissingExternalPackageInfo>> missingExternalPackages = testAPIandSPIsInPublicFeatures(ExternalPackageType.IBM_API);
        assertMissingExternalPackages(missingExternalPackages);
    }

    @Test
    public void testSpecAPIsInPublicFeatures() {
        Map<String, Set<MissingExternalPackageInfo>> missingExternalPackages = testAPIandSPIsInPublicFeatures(ExternalPackageType.SPEC);
        assertMissingExternalPackages(missingExternalPackages);
    }

    @Test
    public void testStableAPIsInPublicFeatures() {
        Map<String, Set<MissingExternalPackageInfo>> missingExternalPackages = testAPIandSPIsInPublicFeatures(ExternalPackageType.STABLE);
        assertMissingExternalPackages(missingExternalPackages);
    }

    @Test
    public void testThirdPartyAPIsInPublicFeatures() {
        Map<String, Set<MissingExternalPackageInfo>> missingExternalPackages = testAPIandSPIsInPublicFeatures(ExternalPackageType.THIRD_PARTY);
        assertMissingExternalPackages(missingExternalPackages);
    }

    @Test
    public void testSPIsInPublicFeatures() {
        Map<String, Set<MissingExternalPackageInfo>> missingExternalPackages = testAPIandSPIsInPublicFeatures(ExternalPackageType.IBM_SPI);

        // For now javax.cache and io.openliberty.jcache SPI packages are filtered out so that the test works.  This logic should be
        // removed once the security features are correctly updated.
        for (Iterator<Entry<String, Set<MissingExternalPackageInfo>>> it = missingExternalPackages.entrySet().iterator(); it.hasNext();) {
            Entry<String, Set<MissingExternalPackageInfo>> entry = it.next();
            Set<MissingExternalPackageInfo> value = entry.getValue();
            for (Iterator<MissingExternalPackageInfo> infoIter = value.iterator(); infoIter.hasNext();) {
                MissingExternalPackageInfo info = infoIter.next();
                if (info.packageName.startsWith("javax.cache") || info.packageName.equals("io.openliberty.jcache")) {
                    infoIter.remove();
                }
            }
            if (value.size() == 0) {
                it.remove();
            }
        }
        assertMissingExternalPackages(missingExternalPackages);
    }

    private Map<String, Set<MissingExternalPackageInfo>> testAPIandSPIsInPublicFeatures(ExternalPackageType type) {
        // Create a map of dependent feature to the list of features that depend on that feature or tolerated list of features.
        // A non tolerate feature dependency is just a Set size of 1.
        Map<Set<FeatureInfo>, Set<FeatureInfo>> ancestors = new HashMap<>();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            FeatureInfo featureInfo = entry.getValue();
            if (featureInfo.isAutoFeature()) {
                continue;
            }

            Map<String, Attrs> dependentFeatures = featureInfo.getDependentFeatures();

            for (Entry<String, Attrs> depFeatureEntry : dependentFeatures.entrySet()) {
                String depFeatureName = depFeatureEntry.getKey();
                Attrs depFeatureAttr = depFeatureEntry.getValue();
                String tolerates = depFeatureAttr.get("ibm.tolerates:");
                Set<FeatureInfo> depFeatureInfoSet = null;
                if (tolerates == null) {
                    FeatureInfo depFeatureInfo = features.get(depFeatureName);
                    if (depFeatureInfo != null) {
                        depFeatureInfoSet = Collections.singleton(depFeatureInfo);
                    }
                } else {
                    depFeatureInfoSet = new LinkedHashSet<>();
                    FeatureInfo depFeatureInfo = features.get(depFeatureName);
                    if (depFeatureInfo != null) {
                        depFeatureInfoSet.add(depFeatureInfo);
                    }
                    String[] tolerateArr = tolerates.split(",");
                    String baseDepFeatureName = depFeatureName.substring(0, depFeatureName.lastIndexOf('-') + 1);
                    for (String tolerateVer : tolerateArr) {
                        String tolerateCandate = baseDepFeatureName + (tolerateVer.trim());
                        depFeatureInfo = features.get(tolerateCandate);
                        if (depFeatureInfo != null) {
                            depFeatureInfoSet.add(depFeatureInfo);
                        }
                    }
                }

                if (depFeatureInfoSet != null && depFeatureInfoSet.size() > 0) {
                    Set<FeatureInfo> ancestorSet = ancestors.get(depFeatureInfoSet);
                    if (ancestorSet == null) {
                        ancestorSet = new HashSet<>();
                        ancestors.put(depFeatureInfoSet, ancestorSet);
                    }
                    ancestorSet.add(featureInfo);
                }
            }
        }

        // Get all of the external packages that a feature inherits from its dependencies.
        Map<Set<FeatureInfo>, Set<ExternalPackageInfo>> cumulativeExtPackageInfo = new HashMap<>();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            FeatureInfo featureInfo = entry.getValue();
            if (!"private".equals(featureInfo.getVisibility())) {
                continue;
            }

            Set<ExternalPackageInfo> featureExternalPackages = null;
            if (type.isSPI) {
                List<ExternalPackageInfo> featureSPIs = featureInfo.getSPIs();
                if (featureSPIs != null) {
                    featureExternalPackages = new HashSet<>(featureSPIs);
                }
            } else {
                List<ExternalPackageInfo> featureAPIs = featureInfo.getAPIs();

                if (featureAPIs != null) {
                    featureExternalPackages = new LinkedHashSet<>();
                    for (ExternalPackageInfo featureAPI : featureAPIs) {
                        if (type.type.equals(featureAPI.getType())) {
                            featureExternalPackages.add(featureAPI);
                        }
                    }
                }
            }
            if (featureExternalPackages == null || featureExternalPackages.size() == 0) {
                continue;
            }

            Set<ExternalPackageInfo> extPackages = cumulativeExtPackageInfo.get(Collections.singleton(featureInfo));
            if (extPackages == null) {
                extPackages = new HashSet<>();
                cumulativeExtPackageInfo.put(Collections.singleton(featureInfo), extPackages);
            }
            extPackages.addAll(featureExternalPackages);

            Set<FeatureInfo> featureAncestors = getFeatureAncestors(Collections.singleton(featureInfo), ancestors);
            for (FeatureInfo featureAncestor : featureAncestors) {
                extPackages = cumulativeExtPackageInfo.get(Collections.singleton(featureAncestor));
                if (extPackages == null) {
                    extPackages = new HashSet<>();
                    cumulativeExtPackageInfo.put(Collections.singleton(featureAncestor), extPackages);
                }
                extPackages.addAll(featureExternalPackages);
            }
        }

        // Now update the tolerates dependencies to have the right list of cumulative features.
        // As features are updated, we may need to re-evaluate the tolerate dependencies so we do this until we stop making updates.
        while (true) {
            boolean updatesMade = false;
            for (Entry<Set<FeatureInfo>, Set<FeatureInfo>> entry : ancestors.entrySet()) {
                Set<FeatureInfo> key = entry.getKey();
                if (key.size() == 1) {
                    continue;
                }
                if (!"private".equals(key.iterator().next().getVisibility())) {
                    continue;
                }
                Set<ExternalPackageInfo> featureExternalPackages = null;
                /*
                 * For the feature dependences with tolerates, only include the APIs that are common in all of
                 * the tolerated features. If one of the tolerated features doesn't include an API or SPI do not include
                 * it in the list of missing features.
                 */
                for (FeatureInfo tolerateFeature : key) {
                    Set<ExternalPackageInfo> tolerateFeatureExtPackages = cumulativeExtPackageInfo.get(Collections.singleton(tolerateFeature));
                    if (tolerateFeatureExtPackages == null) {
                        if (featureExternalPackages == null) {
                            featureExternalPackages = new HashSet<>();
                        } else {
                            featureExternalPackages.clear();
                        }
                    } else {
                        if (featureExternalPackages == null) {
                            featureExternalPackages = new HashSet<>();
                            for (ExternalPackageInfo featureAPI : tolerateFeatureExtPackages) {
                                if (type.type.equals(featureAPI.getType())) {
                                    featureExternalPackages.add(featureAPI);
                                }
                            }
                        } else if (featureExternalPackages.size() != 0) {
                            featureExternalPackages.retainAll(tolerateFeatureExtPackages);
                        }
                    }
                }
                if (featureExternalPackages == null || featureExternalPackages.size() == 0) {
                    continue;
                }

                Set<ExternalPackageInfo> extPackages = cumulativeExtPackageInfo.get(key);
                if (extPackages == null) {
                    extPackages = new HashSet<>();
                    cumulativeExtPackageInfo.put(key, extPackages);
                }
                if (extPackages.addAll(featureExternalPackages)) {
                    updatesMade = true;
                }

                Set<FeatureInfo> featureAncestors = getFeatureAncestors(key, ancestors);
                for (FeatureInfo featureAncestor : featureAncestors) {
                    extPackages = cumulativeExtPackageInfo.get(Collections.singleton(featureAncestor));
                    if (extPackages == null) {
                        extPackages = new HashSet<>();
                        cumulativeExtPackageInfo.put(Collections.singleton(featureAncestor), extPackages);
                    }
                    if (extPackages.addAll(featureExternalPackages)) {
                        updatesMade = true;
                    }
                }
            }
            if (!updatesMade) {
                break;
            }
        }

        Map<String, Set<MissingExternalPackageInfo>> missingExternalPackages = new HashMap<>();
        for (Entry<Set<FeatureInfo>, Set<FeatureInfo>> entry : ancestors.entrySet()) {
            Set<FeatureInfo> key = entry.getKey();
            Set<ExternalPackageInfo> featureExternalPackages = null;
            String privateFeatureName = null;

            if (!"private".equals(key.iterator().next().getVisibility())) {
                continue;
            }
            if (key.size() == 1) {
                FeatureInfo featureInfo = key.iterator().next();

                if (type.isSPI) {
                    List<ExternalPackageInfo> featureSPIs = featureInfo.getSPIs();
                    if (featureSPIs != null) {
                        featureExternalPackages = new HashSet<>(featureSPIs);
                    }
                } else {
                    List<ExternalPackageInfo> featureAPIs = featureInfo.getAPIs();

                    if (featureAPIs != null) {
                        featureExternalPackages = new LinkedHashSet<>();
                        for (ExternalPackageInfo featureAPI : featureAPIs) {
                            if (type.type.equals(featureAPI.getType())) {
                                featureExternalPackages.add(featureAPI);
                            }
                        }
                    }
                }
                privateFeatureName = featureInfo.getName();
            } else {
                featureExternalPackages = cumulativeExtPackageInfo.get(key);

                StringBuilder sb = new StringBuilder();
                for (FeatureInfo toleratedFeature : key) {
                    String toleratedFeatureName = toleratedFeature.getName();
                    if (sb.length() == 0) {
                        sb.append(toleratedFeatureName);
                    } else {
                        sb.append(',');
                        sb.append(toleratedFeatureName.substring(toleratedFeatureName.lastIndexOf('-') + 1));
                    }
                }
                privateFeatureName = sb.toString();
            }

            if (featureExternalPackages != null && featureExternalPackages.size() > 0) {
                Set<FeatureInfo> featureAncestors = entry.getValue();
                findMissingPackages(ancestors, missingExternalPackages, type, featureExternalPackages, featureAncestors, privateFeatureName);
            }
        }

        return missingExternalPackages;
    }

    private void assertMissingExternalPackages(Map<String, Set<MissingExternalPackageInfo>> missingExternalPackages) {
        if (missingExternalPackages.size() > 0) {
            StringBuilder errorMessage = new StringBuilder();
            int packageCount = 0;
            for (Entry<String, Set<MissingExternalPackageInfo>> entry : missingExternalPackages.entrySet()) {
                errorMessage.append(entry.getKey()).append(" is missing the following packages:\n");
                for (MissingExternalPackageInfo missingInfo : entry.getValue()) {
                    errorMessage.append("    ").append(missingInfo).append('\n');
                    packageCount++;
                }
            }
            Assert.fail("Found " + packageCount + " external packages in private features that are not also included in " +
                        missingExternalPackages.size() + " public features: " + '\n' + errorMessage.toString());
        }
    }

    private static class MissingExternalPackageInfo {

        String privateFeatureName;
        final String packageName;

        MissingExternalPackageInfo(String privateFeatureName, String packageName) {
            this.privateFeatureName = privateFeatureName;
            this.packageName = packageName;
        }

        @Override
        public int hashCode() {
            return packageName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MissingExternalPackageInfo other = (MissingExternalPackageInfo) obj;
            return packageName.equals(other.packageName);
        }

        public void updatePrivateFeature(String newPrivateFeature) {
            privateFeatureName = privateFeatureName.replace(" and ", ", ");
            privateFeatureName += (" and " + newPrivateFeature);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return packageName + " from private feature " + privateFeatureName;
        }
    }

    private void findMissingPackages(Map<Set<FeatureInfo>, Set<FeatureInfo>> ancestors, Map<String, Set<MissingExternalPackageInfo>> missingExternalPackages,
                                     ExternalPackageType type, Set<ExternalPackageInfo> featureExternalPackages,
                                     Set<FeatureInfo> featureAncestors, String privateFeatureName) {
        Set<FeatureInfo> publicAncestors = getPublicFeatureAncestors(featureAncestors, ancestors);
        for (FeatureInfo publicAncestor : publicAncestors) {
            Set<MissingExternalPackageInfo> missingExtPackages = new LinkedHashSet<>();
            List<ExternalPackageInfo> ancestorExternalPackages = type.isSPI ? publicAncestor.getSPIs() : publicAncestor.getAPIs();
            for (ExternalPackageInfo featureExternalPackage : featureExternalPackages) {
                if (ancestorExternalPackages == null || !ancestorExternalPackages.contains(featureExternalPackage)) {
                    MissingExternalPackageInfo missingPackage = new MissingExternalPackageInfo(privateFeatureName, featureExternalPackage.getPackageName());
                    missingExtPackages.add(missingPackage);
                }
            }
            if (missingExtPackages.size() > 0) {
                Set<MissingExternalPackageInfo> missingPackages = missingExternalPackages.get(publicAncestor.getName());
                if (missingPackages == null) {
                    missingPackages = new LinkedHashSet<>();
                    missingExternalPackages.put(publicAncestor.getName(), missingPackages);
                }
                for (MissingExternalPackageInfo missingPackage : missingExtPackages) {
                    if (!missingPackages.add(missingPackage)) {
                        for (MissingExternalPackageInfo missing : missingPackages) {
                            if (missing.equals(missingPackage)) {
                                missing.updatePrivateFeature(missingPackage.privateFeatureName);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the public features in the dependency chain that depend on the Set of feature supplied.
     * If the dependency chain stops at a protected or public feature, no other ancestors are gotten past that feature.
     *
     * @param ancestors
     * @param ancestorsMap
     * @return
     */
    private Set<FeatureInfo> getPublicFeatureAncestors(Set<FeatureInfo> ancestors, Map<Set<FeatureInfo>, Set<FeatureInfo>> ancestorsMap) {
        Set<FeatureInfo> publicFeatures = new HashSet<>();
        if (ancestors != null) {
            for (FeatureInfo ancestor : ancestors) {
                if (ancestor.getVisibility().equals("public")) {
                    publicFeatures.add(ancestor);
                } else if (ancestor.getVisibility().equals("private")) {
                    publicFeatures.addAll(getPublicFeatureAncestors(ancestorsMap.get(Collections.singleton(ancestor)), ancestorsMap));
                }
            }
        }
        return publicFeatures;
    }

    /**
     * Get the features in the dependency chain that depend on the Set of features supplied.
     * If the dependency chain stops at a protected or public feature, no other ancestors are gotten past that feature.
     *
     * @param ancestors
     * @param ancestorsMap
     * @return
     */
    private Set<FeatureInfo> getFeatureAncestors(Set<FeatureInfo> feature, Map<Set<FeatureInfo>, Set<FeatureInfo>> ancestorsMap) {
        Set<FeatureInfo> featureAncestors = new HashSet<>();
        Set<FeatureInfo> ancestors = ancestorsMap.get(feature);
        if (ancestors != null) {
            for (FeatureInfo ancestor : ancestors) {
                featureAncestors.add(ancestor);
                if (ancestor.getVisibility().equals("private")) {
                    featureAncestors.addAll(getFeatureAncestors(Collections.singleton(ancestor), ancestorsMap));
                }
            }
        }

        return featureAncestors;
    }

    @Test
    public void testValidAPITypes() {
        HashSet<String> validAPITypes = new HashSet<>();
        for (ExternalPackageType validType : ExternalPackageType.values()) {
            if (!validType.isSPI) {
                validAPITypes.add(validType.type);
            }
        }
        validAPITypes.add("internal");
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            FeatureInfo featureInfo = entry.getValue();
            List<ExternalPackageInfo> APIs = featureInfo.getAPIs();
            if (APIs != null) {
                for (ExternalPackageInfo packageInfo : APIs) {
                    String type = packageInfo.getType();
                    if (!validAPITypes.contains(type)) {
                        errorMessage.append(packageInfo.getPackageName()).append(" in feature ").append(entry.getKey()).append(" has an invalid type ").append(type).append('\n');
                    }
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features with APIs with invalid types: \n" + errorMessage.toString());
        }
    }

    @Test
    public void testValidSPITypes() {
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            FeatureInfo featureInfo = entry.getValue();
            List<ExternalPackageInfo> SPIs = featureInfo.getSPIs();
            if (SPIs != null) {
                for (ExternalPackageInfo packageInfo : SPIs) {
                    String type = packageInfo.getType();

                    // Since there is only really 1 type for SPIs, usually the type is null which we set to ibm-spi in the parse
                    // logic in the feature utility test function.
                    if (type != null && !"ibm-spi".equals(type)) {
                        errorMessage.append(packageInfo.getPackageName()).append(" in feature ").append(entry.getKey()).append(" has an invalid type ").append(type).append('\n');
                    }
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features with SPIs with invalid types: \n" + errorMessage.toString());
        }
    }

    @Test
    public void testExternalPackageDuplication() {
        StringBuilder errorMessage = new StringBuilder();
        for (Entry<String, FeatureInfo> entry : features.entrySet()) {
            Map<String, String> packageToTypeMap = new HashMap<>();
            FeatureInfo featureInfo = entry.getValue();
            List<ExternalPackageInfo> APIs = featureInfo.getAPIs();
            if (APIs != null) {
                for (ExternalPackageInfo packageInfo : APIs) {
                    String type = packageInfo.getType();
                    if (packageToTypeMap.putIfAbsent(packageInfo.getPackageName(), type) != null) {
                        String existingType = packageToTypeMap.get(packageInfo.getPackageName());
                        // Java / Jakarta Persistence externalizes APIs as both internal and third-party
                        if ((!type.equals("internal") && !existingType.equals("third-party")) && (!type.equals("third-party") && !existingType.equals("internal"))
                            && !entry.getKey().startsWith(".com.ibm.websphere.appserver.jpa-") && !entry.getKey().startsWith("io.openliberty.persistence-")) {
                            errorMessage.append(packageInfo.getPackageName()).append(" in feature ").append(entry.getKey()).append(" is externalized twice").append('\n');
                            if (!existingType.equals(type)) {
                                errorMessage.append(packageInfo.getPackageName()).append(" in feature ").append(entry.getKey()).append(" is externalized as two different types ")
                                            .append(type).append(" and ").append(existingType).append('\n');
                            }
                        }
                    }
                }
            }
            List<ExternalPackageInfo> SPIs = featureInfo.getSPIs();
            if (SPIs != null) {
                for (ExternalPackageInfo packageInfo : SPIs) {
                    if (packageToTypeMap.putIfAbsent(packageInfo.getPackageName(), "ibm-spi") != null) {
                        // Servlet externalizes APIs as both internal and also as an SPI
                        if (!packageToTypeMap.get(packageInfo.getPackageName()).equals("internal") && !entry.getKey().startsWith("com.ibm.websphere.appserver.servlet-")) {
                            errorMessage.append(packageInfo.getPackageName()).append(" in feature ").append(entry.getKey()).append(" is externalized twice").append('\n');
                            if (!packageToTypeMap.get(packageInfo.getPackageName()).equals("ibm-spi")) {
                                errorMessage.append(packageInfo.getPackageName()).append(" in feature ").append(entry.getKey()).append(" is externalized as two different types ")
                                            .append(" ibm-spi and ").append(packageToTypeMap.get(packageInfo.getPackageName())).append('\n');
                            }
                        }
                    }
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features with duplicate APIs / SPIs: \n" + errorMessage.toString());
        }
    }
}
