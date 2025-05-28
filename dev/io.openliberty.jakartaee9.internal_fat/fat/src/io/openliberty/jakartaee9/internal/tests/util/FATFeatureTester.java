/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee9.internal.tests.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;

public class FATFeatureTester {
    private static final Class<? extends FATFeatureTester> c = FATFeatureTester.class;

    public static interface FeatureReplacer {
        String getReplacement(String feature);
    }

    public static List<String> testRenameConflicts(Set<String> features, FeatureReplacer replacer) throws Exception {
        List<String> errors = new ArrayList<>();

        List<String> rootFeatures = new ArrayList<>(2);
        rootFeatures.add("feature");
        rootFeatures.add("updated-feature");

        for (String feature : features) {
            String updatedFeature = replacer.getReplacement(feature);
            if (updatedFeature == null) {
                errors.add("Missing replacement feature for [ " + feature + " ]");
                continue;
            }

            rootFeatures.set(0, feature);
            rootFeatures.set(1, updatedFeature);

            Result result = FATFeatureResolver.resolve(rootFeatures);

            Map<String, Collection<Chain>> conflicts = result.getConflicts();
            if (conflicts.isEmpty()) {
                errors.add("Missing conflict of [ " + feature + " ] with [ " + updatedFeature + " ]");

            } else if (!conflicts.containsKey("com.ibm.websphere.appserver.eeCompatible")) {
                errors.add("Missing conflict of [ " + feature + " ] with [ " + "com.ibm.websphere.appserver.eeCompatible" + " ]");

            } else {
                // OK
            }
        }

        return errors;
    }

    /**
     * Validate the compatibility of a feature against other features.
     *
     * This is done in parallel: Process the features using up to four
     * threads.
     *
     * Test errors are placed into a single errors list. The placement
     * of errors in this list will be unpredictable.
     *
     * @param feature          A feature which is to be tested.
     * @param againstFeatures  The features against which to test the feature.
     * @param specialConflicts Expected conflicts.
     *
     * @return Test errors.
     *
     * @throws InterruptedException Thrown if a test thread fails unexpectedly.
     */
    public static List<String> testCompatibility(String feature,
                                                 Set<String> againstFeatures,
                                                 Set<String> compatibleFeatures,
                                                 Set<String> incompatibleFeatures,
                                                 Map<String, String> specialConflicts) throws InterruptedException {

        FATExecutor.FATAction testAction = new FATExecutor.FATAction() {
            @Override
            public void run(Queue<String> parms, List<String> errors) {
                checkFeatures(feature, parms,
                              compatibleFeatures, incompatibleFeatures, specialConflicts,
                              errors);
            }
        };

        return FATExecutor.run(testAction, againstFeatures); // throws InterruptedException
    }

    /**
     * Verify feature resolution conflicts.
     *
     * Verify the given feature against specified feature. Poll and test
     * the specified features one at a time. Other threads are expected
     * to be polling from the specified features.
     *
     * Skip any features which are listed as neither compatible nor
     * incompatible with the base feature.
     *
     * Verify the given feature against the keys of the special conflicts
     * table. Each value of that table indicates a feature conflict which
     * should occur.
     *
     * Store incorrect resolutions to the errors.
     *
     * @param baseFeature      A base feature which is to be resolved.
     * @param againstFeatures  A queue of features which are to be tested against.
     * @param specialConflicts Table of special conflicts which are to be verified.
     *
     * @param errors           Storage for resolution errors.
     */
    private static void checkFeatures(String baseFeature,
                                      Queue<String> againstFeatures,
                                      Set<String> compatibleFeatures,
                                      Set<String> incompatibleFeatures,
                                      Map<String, String> specialConflicts,
                                      List<String> errors) {

        String method = "checkFeatures";

        List<String> rootFeatures = new ArrayList<>(2);
        rootFeatures.add(baseFeature);
        rootFeatures.add("against-feature");

        String againstFeature;
        while ((againstFeature = againstFeatures.poll()) != null) {
            if (!compatibleFeatures.contains(againstFeature) &&
                !incompatibleFeatures.contains(againstFeature)) {
                FATLogger.info(c, method, "Test [ " + baseFeature + " ] against [ " + againstFeature + " ]: Disincluded");
                continue;
            }

            boolean isIncompatible = incompatibleFeatures.contains(againstFeature);

            String prefix = "resolving [ " + baseFeature + " ] against [ " + againstFeature + " ] ...";
            FATLogger.info(c, method, prefix);

            rootFeatures.set(1, againstFeature);
            Result result = FATFeatureResolver.resolve(rootFeatures);
            Map<String, Collection<Chain>> conflicts = result.getConflicts();

            String error = diagnoseResult(baseFeature, againstFeature,
                                          isIncompatible, specialConflicts,
                                          conflicts);

            String resultText;
            if (error != null) {
                resultText = " failed";
                errors.add(error);
            } else {
                resultText = " passed";
            }
            FATLogger.info(c, method, prefix + resultText);
        }
    }

    public static List<String> testVersionlessFeatures(Set<String> versionlessFeatures, Set<String> incompatibleVersionlessFeatures) {
        List<String> errors = new ArrayList<>();
        String method = "testVersionlessFeature";

        List<String> platforms = new ArrayList<>(2);
        platforms.add("jakartaee-9.1");
        platforms.add("microProfile-5.0");

        for (String feature : versionlessFeatures) {
            String prefix = "resolving [ " + feature + " ] ...";
            FATLogger.info(c, method, prefix);

            Result result = FATFeatureResolver.resolve(Collections.singleton(feature), platforms);

            boolean hasErrors = result.hasErrors();
            boolean expectsIncompatible = incompatibleVersionlessFeatures.contains(feature);

            StringBuilder sb = new StringBuilder();
            // If this versionless feature is suppose to not be valid for EE 9, but we didn't get
            // any errors when calling resolve, that is a test failure
            if (!hasErrors && expectsIncompatible) {
                sb.append(feature);
                sb.append(" expected to be incompatible with EE 9 / MP 5, but resolve did not find any errors");
            } else if (hasErrors && !expectsIncompatible) {
                Set<String> missingPlatforms = result.getMissingPlatforms();
                Map<String, Set<String>> missingBasePlatforms = result.getNoPlatformVersionless();
                Set<String> missingFeatures = result.getMissing();
                Map<String, Collection<Chain>> conflicts = result.getConflicts();
                String resolvedFeature = result.getVersionlessFeatures().get(feature);
                if (resolvedFeature == null) {
                    sb.append(feature);
                    sb.append(" did not resolve to a versioned feature");
                }
                if (!conflicts.isEmpty()) {
                    if (sb.length() == 0) {
                        sb.append(feature);
                        sb.append(" failed due to");
                    } else {
                        sb.append(',');
                    }
                    sb.append(" conflicts ").append(conflicts);
                }
                if (!missingPlatforms.isEmpty()) {
                    if (sb.length() == 0) {
                        sb.append(feature);
                        sb.append(" failed due to");
                    } else {
                        sb.append(',');
                    }
                    sb.append(" missing platforms ").append(missingPlatforms);
                }
                if (!missingBasePlatforms.isEmpty()) {
                    if (sb.length() == 0) {
                        sb.append(feature);
                        sb.append(" failed due to");
                    } else {
                        sb.append(',');
                    }
                    sb.append(" missing base platforms ").append(missingBasePlatforms);
                }
                if (!missingFeatures.isEmpty()) {
                    if (sb.length() == 0) {
                        sb.append(feature);
                        sb.append(" failed due to");
                    } else {
                        sb.append(',');
                    }
                    sb.append(" missing features ").append(missingFeatures);
                }
            }

            String resultText;
            if (sb.length() != 0) {
                resultText = " failed";
                errors.add(sb.toString());
            } else {
                resultText = " passed";
            }
            FATLogger.info(c, method, prefix + resultText);
        }

        return errors;
    }

    /**
     * Diagnose a resolution result.
     *
     * There are three general cases:
     *
     * <ul>
     * <li>A special conflict is expected.</li>
     * <li>Conflicts are expected.</li>
     * <li>No conflicts are expected.</li>
     * </ul>
     *
     * The actual conflicts are examined and verified to contain
     * the expected conflicts.
     *
     * @param baseFeature      An initial root feature.
     * @param againstFeature   An additional root feature.
     * @param isIncompatible   True or false telling if the additional feature
     *                             is known to be incompatible with the base feature.
     * @param specialConflicts Table of special conflicts which may occur.
     * @param actualConflicts  The actual conflicts which occurred.
     *
     * @return Null if the actual conflicts are as expected. Otherwise, an
     *         error message describing an unexpected conflict result. This includes
     *         cases where a conflict was expected but none occurred.
     */
    public static String diagnoseResult(String baseFeature, String againstFeature,
                                        boolean isIncompatible, Map<String, String> specialConflicts,
                                        Map<String, Collection<Chain>> actualConflicts) {

        String prefix = "Resolve [ " + baseFeature + " ] against [ " + againstFeature + " ]: ";

        boolean haveActualConflict = !actualConflicts.isEmpty();

        String specialConflict = specialConflicts.get(againstFeature);
        if (specialConflict != null) {
            // Special conflict case:
            //
            // The actual conflicts are required to contain the indicated
            // special conflict.

            if (!haveActualConflict) {
                return prefix + "Special: No conflicts expecting [ " + specialConflict + " ]";
            } else if (!actualConflicts.containsKey(specialConflict)) {
                return prefix + "Special: Missing conflict [ " + specialConflict + " ]; actual [ " + actualConflicts.keySet() + " ]";
            } else {
                return null; // Expected special conflict did occur.
            }

        } else if (isIncompatible) {
            // Normal conflict case:
            //
            // At least one conflict must occur.

            if (!haveActualConflict) {
                return prefix + "Incompatible: Missing conflicts";
            } else {
                return null; // OK: Conflicts were expected and occurred.
            }

        } else {
            if (haveActualConflict) {
                return prefix + "Compatible: Unexpected conflicts [ " + actualConflicts.keySet() + " ]";
            } else {
                return null; // OK: No conflicts were expected and none occurred.
            }
        }
    }
}
