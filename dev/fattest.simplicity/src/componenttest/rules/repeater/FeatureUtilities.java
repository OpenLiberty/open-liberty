/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.rules.repeater;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.LocalMachine;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.topology.impl.JavaInfo;

/**
 * Utility methods for obtaining features associated with
 * a JavaEE or JakartaEE version, or, for obtaining features
 * from a liberty installation.
 *
 * The results are sensitive to whether the current operating
 * system is Z/OS. See {@link #isZOS()}.
 */
public class FeatureUtilities {
    private static final Class<?> CLASS = FeatureUtilities.class;

    private static void error(String method, Throwable th, String msg) {
        Log.error(CLASS, method, th, msg);
    }

    private static void info(String method, String msg) {
        Log.info(CLASS, method, msg);
    }

    //

    private static final boolean isZOS;

    static {
        boolean zos;
        try {
            zos = LocalMachine.getInstance().getOperatingSystem() == OperatingSystem.ZOS;
        } catch (Exception e) {
            zos = false;
            error("<clinit>", e, "Failed to detect OS: Defaulting to NOT Z/OS.");
        }
        isZOS = zos;
    }

    /**
     * Tell if the current operating system is Z/OS.
     *
     * @return True or false telling if the current operating
     *         system is Z/OS.
     */
    public boolean isZOS() {
        return isZOS;
    }

    /**
     * All short names of all public JavaEE or JakartaEE features.
     *
     * Restricting the results to only open liberty features removes
     * all JavaEE6 features from the result.
     *
     * @param  openLibertyOnly Control parameter: Whether to restrict the result
     *                             to open liberty features.
     *
     * @return                 All short names of all public JavaEE or JakartaEE features.
     */
    public static Set<String> allEeFeatures(boolean openLibertyOnly) {
        Set<String> features = new HashSet<>();

        if (!openLibertyOnly) {
            features.addAll(EE6FeatureReplacementAction.EE6_FEATURE_SET);
        }
        features.addAll(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        features.addAll(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        features.addAll(JakartaEE9Action.EE9_FEATURE_SET);
        features.addAll(JakartaEE10Action.EE10_FEATURE_SET);
        features.addAll(JakartaEE11Action.EE11_FEATURE_SET);

        // EE-related features which aren't in one of the feature sets
        features.add("appSecurity-1.0");
        features.add("data-1.1");
        features.add("jsp-2.2");
        features.add("websocket-1.0");

        return features;
    }

    /**
     * Answer the short names of all public micro-profile features.
     *
     * See {@link MicroProfileActions#ALL} and
     * {@link MicroProfileActions#STANDALONE_ALL}.
     *
     * @return The short names of all public micro-profile features.
     *
     * @return the set of short names
     */
    public static Set<String> allMpFeatures() {
        return MicroProfileActions.ALL.stream()
                        .flatMap(s -> s.getFeatures().stream())
                        .collect(Collectors.toSet());
    }

    /**
     * Answer the short names of all public micro-profile features
     * which are compatible with a JavaEE or JakartaEE version.
     *
     * See {@link MicroProfileActions#ALL} and
     * {@link MicroProfileActions#STANDALONE_ALL}.
     *
     * @param  eeVersion the EE version
     * @return           the set of compatible MP feature short names
     */
    public static Set<String> compatibleMpFeatures(EEVersion eeVersion) {
        Set<String> features = MicroProfileActions.ALL.stream()
                        .filter(s -> s.getEEVersion() == eeVersion)
                        .flatMap(s -> s.getFeatures().stream())
                        .collect(Collectors.toCollection(HashSet::new));

        //MP RM is compatible with EE9+ but only with Java11+
        if (eeVersion.compareTo(EEVersion.EE9) >= 0 && JavaInfo.JAVA_VERSION >= 11) {
            features.add("mpReactiveStreams-3.0");
            features.add("mpReactiveMessaging-3.0");
        }

        // MP Telemetry 1.1 is compatible with EE7 - EE10 but only with Java11+
        if (eeVersion.compareTo(EEVersion.EE7) >= 0 && eeVersion.compareTo(EEVersion.EE10) <= 0 && JavaInfo.JAVA_VERSION >= 11) {
            features.add("mpTelemetry-1.1");
        }

        // MP Telemetry 2.0 is compatible with EE7 - EE11 but only with Java11+
        if (eeVersion.compareTo(EEVersion.EE7) >= 0 && eeVersion.compareTo(EEVersion.EE11) <= 0 && JavaInfo.JAVA_VERSION >= 11) {
            features.add("mpTelemetry-2.0");
        }

        return features;
    }

    /**
     * Answer the short names of public test features.
     *
     * This is a specific, hard-coded list.
     *
     * @return The short names of all public test features.
     */
    public static Set<String> allTestFeatures() {
        Set<String> testFeatures = new HashSet<>();
        testFeatures.add("componenttest-1.0");
        testFeatures.add("componenttest-2.0");
        testFeatures.add("txtest-1.0");
        testFeatures.add("txtest-2.0");
        testFeatures.add("ejbTest-1.0");
        testFeatures.add("ejbTest-2.0");
        testFeatures.add("enterpriseBeansTest-2.0");
        return testFeatures;
    }

    /**
     * Tell if a feature a public, non-test feature.
     *
     * @param  featureFile A feature file.
     * @return             the list of public short names
     */
    public static boolean isPublicFeature(File featureFile) {
        String name = featureFile.getName();
        return (name.startsWith("io.openliberty.") || name.startsWith("com.ibm."));
    }

    /**
     * Tell if a feature is a versionless feature.
     *
     * Versionless feature names do not have a '-' character.
     *
     * @param  feature A feature name or short name.
     *
     * @return         True or false telling if the feature is a versionless
     *                 feature.
     */
    public static boolean isVersionless(String feature) {
        return (feature.indexOf('-') == -1);
    }

    public static final boolean OPEN_LIBERTY_ONLY = true;

    public static Set<String> getFeaturesFromServer(String serverRoot, boolean openLibertyOnly) {
        return getFeaturesFromServer(new File(serverRoot), openLibertyOnly);
    }

    /**
     * Answer the short names of all public features of a server installation.
     *
     * @param  installRoot     The root (wlp) folder of a liberty installation.
     * @param  openLibertyOnly Control parameter: Whether to restrict the result
     *                             to open liberty features.
     *
     * @return                 The short names of public features of the server.
     */
    public static Set<String> getFeaturesFromServer(File installRoot, boolean openLibertyOnly) {
        Set<String> features = new HashSet<>();

        // If there was a problem building projects before this test runs,
        // "lib/features" may not exist.

        File featureDir = new File(installRoot, "lib/features");
        if (!featureDir.exists()) {
            return features;
        }

        try {
            for (File featureFile : featureDir.listFiles()) {
                if (!isPublicFeature(featureFile)) {
                    continue;
                }
                String shortName = parseShortName(featureFile, openLibertyOnly);
                if (shortName == null) {
                    continue;
                }
                features.add(shortName);
            }
            return features;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reject features which are versionless. These
     * are features which do not a version number.
     *
     * @param  features Features from which to obtain non-versionless
     *                      features.
     *
     * @return          The non-versionless features of the specified set of
     *                  features.
     */
    public static Set<String> rejectVersionless(Set<String> features) {
        Set<String> filtered = new HashSet<>(features.size());
        for (String feature : features) {
            if (!isVersionless(feature)) {
                filtered.add(feature);
            }
        }
        return filtered;
    }

    /**
     * Select features which are versionless features. These
     * are features which do not a version number.
     *
     * @param  features Features from which to obtain versionless
     *                      features.
     *
     * @return          The versionless features of the specified set of
     *                  features.
     */
    public static Set<String> selectVersionless(Set<String> features) {
        Set<String> filtered = new HashSet<>(features.size());
        for (String feature : features) {
            if (isVersionless(feature)) {
                filtered.add(feature);
            }
        }
        return filtered;
    }

    /**
     * Read the feature short name from a feature manifest file.
     *
     * Skip non-manifest files. Feature manifest files have the
     * extension ".mf".
     *
     * Answer null for features which are test features, which are
     * non-public features, or (conditionally) which are non-open
     * liberty features.
     *
     * @param  featureFile     A feature manifest.
     * @param  onlyLibertyOnly Control parameter: When true, answer
     *                             null for features which do not start with "io.openliberty.".
     *
     * @return                 The feature short name. Null if the target file is a directory,
     *                         is not a feature manifest, or if the feature is a test feature, a
     *                         non-public feature, or, conditionally, not an open liberty feature.
     *
     * @throws IOException     Thrown if there was a failure reading the feature file.
     */
    private static String parseShortName(File featureFile, boolean openLibertyOnly) throws IOException {
        String methodName = "parseShortName";

        String fileName = featureFile.getName();
        if (featureFile.isDirectory()) {
            // info(methodName, "Skipping directory: " + fileName);
            return null;
        } else if (!fileName.endsWith(".mf")) {
            // info(methodName, "Skipping non-manifest: " + fileName);
            return null;
        }

        String shortName = null;

        try (Scanner scanner = new Scanner(featureFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("IBM-ShortName:")) {
                    shortName = line.substring("IBM-ShortName:".length()).trim();

                } else if (line.contains("IBM-Test-Feature:") && line.contains("true")) {
                    info(methodName, "Skipping test feature: " + fileName);
                    return null;

                } else if (line.startsWith("Subsystem-SymbolicName:") && !line.contains("visibility:=public")) {
                    info(methodName, "Skipping non-public feature: " + fileName);
                    return null;

                } else if (openLibertyOnly && line.startsWith("IBM-ProductID") && !line.contains("io.openliberty")) {
                    info(methodName, "Skipping non-open liberty feature: " + fileName);
                    return null;
                }
            }
        }

        if (shortName == null) {
            info(methodName, "Skipping feature: No short name: " + fileName);
            return null;
        }

        if (!isZOS && (shortName.startsWith("zos") || shortName.startsWith("batchSMFLogging"))) {
            info(methodName, "Skipping zos feature: " + fileName);
            return null;
        }

        return shortName;
    }

    private static boolean isTestAutoFeature(File featureFile) throws IOException {

        String fileName = featureFile.getName();
        if (featureFile.isDirectory()) {
            // info(methodName, "Skipping directory: " + fileName);
            return false;
        } else if (!fileName.endsWith(".mf")) {
            // info(methodName, "Skipping non-manifest: " + fileName);
            return false;
        }

        boolean isAutoFeature = false;
        boolean isTestFeature = false;

        try (Scanner scanner = new Scanner(featureFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.contains("IBM-Provision-Capability:")) {
                    isAutoFeature = true;
                } else if (line.contains("IBM-Test-Feature:") && line.contains("true")) {
                    isTestFeature = true;
                }
            }
        }

        return isAutoFeature && isTestFeature;
    }

    public static void removeTestAutoFeatures(File installRoot) {

        // If there was a problem building projects before this test runs,
        // "lib/features" may not exist.

        File featureDir = new File(installRoot, "lib/features");
        if (!featureDir.exists()) {
            return;
        }

        try {
            for (File featureFile : featureDir.listFiles()) {
                if (isTestAutoFeature(featureFile)) {
                    featureFile.delete();
                    continue;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
