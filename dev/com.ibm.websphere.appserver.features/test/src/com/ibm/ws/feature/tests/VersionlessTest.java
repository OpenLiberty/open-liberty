/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureInfo;
import com.ibm.ws.feature.utils.FeatureMapFactory;
import com.ibm.ws.feature.utils.VersionlessFeatureCreator;
import com.ibm.ws.feature.utils.VersionlessFeatureDefinition;

import aQute.bnd.header.Attrs;

//Generating reports on EE and MP features as well as creating versionless features
public class VersionlessTest {

    private static Map<String, FeatureInfo> featureRepo = null;
    private static Map<String, List<String>> cohorts = null;

    public static FeatureInfo getFeature(String featureName) {
        return featureRepo.get(featureName);
    }

    @BeforeClass
    public static void setUpClass() {
        featureRepo = FeatureMapFactory.getFeatureMapFromFile("./visibility/");
        cohorts = computeCohorts();
    }

    /**
     * features and a list of their versions
     *
     * @return
     */
    private static Map<String, List<String>> computeCohorts() {
        Map<String, List<String>> useCohorts = new LinkedHashMap<>();

        for (FeatureInfo featureInfo : featureRepo.values()) {
            if (!featureInfo.isPublic()) {
                continue;
            }

            String version = featureInfo.getVersion();
            if (version == null) {
                continue;
            }

            String baseName = featureInfo.getBaseName();
            List<String> cohort = useCohorts.computeIfAbsent(baseName,
                                                             (String useBaseName) -> new ArrayList<>());
            cohort.add(version);
        }

        useCohorts.forEach((String baseName, List<String> useVersions) -> {
            System.out.println(baseName);
            useVersions.sort(VersionlessTest::compareVersions);
        });

        return useCohorts;
    }

    private static Set<FeatureInfo> getAllPublicDependentFeatures(FeatureInfo featureInfo) {
        Set<FeatureInfo> publicDepFeatures = new HashSet<>();
        Set<String> processedDepFeatures = new HashSet<>();
        processFeatureInfo(featureInfo, publicDepFeatures, processedDepFeatures);
        return publicDepFeatures;
    }

    private static void processFeatureInfo(FeatureInfo featureInfo, Set<FeatureInfo> publicDepFeatures, Set<String> processedDepFeatures) {
        // if already processed just return
        if (!processedDepFeatures.add(featureInfo.getName())) {
            return;
        }
        featureInfo.getDependentFeatures().forEach((String depName, Attrs attr) -> {
            FeatureInfo depInfo = getFeature(depName);
            if (depInfo == null) {
                System.out.println("        [ " + depName + " ** NOT FOUND ** ]");
                // Only include dependent features that do not have tolerates.  If a feature depends on particular version it will have been
                // included in the convenience feature due to how tolerates works to not be transitive.
            } else if (!attr.containsKey("ibm.tolerates:") || depName.startsWith("com.ibm.websphere.appserver.jdbc-")) {
                if (depInfo.isPublic()) {
                    publicDepFeatures.add(depInfo);
                    // Check for a Container feature and add it to the list of features
                    String possibleContainerFeature = depInfo.getBaseName() + "Container-" + depInfo.getVersion();
                    FeatureInfo containerFeatureInfo = getFeature(possibleContainerFeature);
                    if (containerFeatureInfo != null) {
                        if (processedDepFeatures.add(containerFeatureInfo.getName())) {
                            publicDepFeatures.add(containerFeatureInfo);
                        }
                    }
                }
                processFeatureInfo(depInfo, publicDepFeatures, processedDepFeatures);
            }
        });
    }

    /**
     * List all features and their versions
     */
    @Test
    public void listCohorts() {

        List<String> baseNames = new ArrayList<>(cohorts.keySet());
        baseNames.sort(Comparator.comparing(String::toString));

        System.out.println("Cohorts:");
        for (String baseName : baseNames) {
            List<String> cohort = cohorts.get(baseName);
            System.out.println("  [ " + baseName + " ] [ " + cohort + " ]");
        }
    }

    /**
     * List all EE and MP platforms and their versions
     */
    @Test
    public void listSelectorCohorts() {

        System.out.println("Selectors:");
        getSelectorCohorts().forEach((String baseName, List<String> featureNames) -> {
            System.out.println("  [ " + baseName + " ]");
            for (String featureName : featureNames) {
                System.out.println("    [ " + featureName + " ] [ " + cohorts.get(featureName) + " ]");
            }
        });
    }

    /**
     * Generate report of all EE features and their platforms
     */
    @Test
    public void listEECohorts() {
        System.out.println("EE Cohorts!!");
        Set<String> allCohortsSet = new HashSet<String>();
        Map<String, List<String>> featuresMPDependencies = new HashMap<>();
        getSelectorCohorts().forEach((String baseName, List<String> featureBaseNames) -> {
            if (baseName.equals("javaee") || baseName.equals("jakartaee")) {
                for (String featureBaseName : featureBaseNames) {
                    List<String> cohort = cohorts.get(featureBaseName);
                    allCohortsSet.addAll(cohort);
                    System.out.println("    [ " + featureBaseName + " ] [ " + cohort + " ]");

                    //loop over each version of a platform to get data on its features
                    for (String version : cohort) {
                        String featureName = featureBaseName + "-" + version;
                        FeatureInfo featureInfo = getFeature(featureName);
                        if (featureInfo == null) {
                            continue;
                        } else {
                            System.out.println("      [ " + featureInfo.getName() + " ]");
                        }

                        Set<FeatureInfo> publicDepFeatures = getAllPublicDependentFeatures(featureInfo);
                        for (FeatureInfo depInfo : publicDepFeatures) {
                            System.out.println("        [ " + depInfo.getBaseName() + " - " + depInfo.getVersion() + " ]");
                            if (depInfo.getShortName() != null) {
                                if (featuresMPDependencies.containsKey(depInfo.getShortName())) {
                                    featuresMPDependencies.get(depInfo.getShortName()).add(version);
                                } else {
                                    List<String> mpVersions = new ArrayList<String>();
                                    mpVersions.add(version);
                                    featuresMPDependencies.put(depInfo.getShortName(), mpVersions);
                                }
                            }
                        }
                    }
                }
            }
        });
        System.out.println(allCohortsSet);
        ArrayList<String> allCohorts = new ArrayList<>(allCohortsSet);

        System.out.println("Features EE Deps:");

        List<String> sortedFeatures = new ArrayList<>(featuresMPDependencies.keySet());
        Collections.sort(sortedFeatures);

        ArrayList<String> missingCohorts = (ArrayList<String>) allCohorts.clone();
        String current = "";

        for (String feature : sortedFeatures) {
            if (!current.equals("") && !current.equals(feature.split("-")[0])) {
                System.out.println("        " + current + " missing cohorts: " + missingCohorts);
                missingCohorts = (ArrayList<String>) allCohorts.clone();
            }
            current = feature.split("-")[0];
            System.out.println("    [ " + feature + " ]   " + featuresMPDependencies.get(feature));
            missingCohorts.removeAll(featuresMPDependencies.get(feature));
        }

    }

    /**
     * Generate report on all MP features and its platforms
     */
    @Test
    public void listMicroProfileCohorts() {
        System.out.println("MP Cohorts!!");
        ArrayList<String> allCohorts = new ArrayList<>();
        Map<String, List<String>> featuresMPDependencies = new HashMap<>();
        getSelectorCohorts().forEach((String baseName, List<String> featureBaseNames) -> {
            if (baseName.equals("microProfile")) {
                for (String featureBaseName : featureBaseNames) {
                    List<String> cohort = cohorts.get(featureBaseName);
                    allCohorts.addAll(cohort);
                    System.out.println("    [ " + featureBaseName + " ] [ " + cohort + " ]");

                    for (String version : cohort) {
                        String featureName = featureBaseName + "-" + version;
                        FeatureInfo featureInfo = getFeature(featureName);
                        if (featureInfo == null) {
                            continue;
                        } else {
                            System.out.println("      [ " + featureInfo.getName() + " ]");
                        }

                        Set<FeatureInfo> publicDepFeatures = getAllPublicDependentFeatures(featureInfo);
                        for (FeatureInfo depInfo : publicDepFeatures) {
                            System.out.println("        [ " + depInfo.getBaseName() + " - " + depInfo.getVersion() + " ]");
                            if (depInfo.getShortName() != null) {
                                if (depInfo.getShortName().startsWith("mp")) {
                                    if (featuresMPDependencies.containsKey(depInfo.getShortName())) {
                                        featuresMPDependencies.get(depInfo.getShortName()).add(version);
                                    } else {
                                        List<String> mpVersions = new ArrayList<String>();
                                        mpVersions.add(version);
                                        featuresMPDependencies.put(depInfo.getShortName(), mpVersions);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        System.out.println("Features MP Deps:");

        List<String> sortedFeatures = new ArrayList<>(featuresMPDependencies.keySet());
        Collections.sort(sortedFeatures);

        ArrayList<String> missingCohorts = (ArrayList<String>) allCohorts.clone();
        String current = "";

        for (String feature : sortedFeatures) {
            if (!current.equals("") && !current.equals(feature.split("-")[0])) {
                System.out.println("        " + current + " missing cohorts: " + missingCohorts);
                missingCohorts = (ArrayList<String>) allCohorts.clone();
            }
            current = feature.split("-")[0];
            System.out.println("    [ " + feature + " ]   " + featuresMPDependencies.get(feature));
            missingCohorts.removeAll(featuresMPDependencies.get(feature));
        }
    }

    @Test
    public void validatePlatformSettings() {
        StringBuilder errorMessage = new StringBuilder();
        Map<String, Set<String>> featureToPlatformMapping = new HashMap<>();
        Set<String> platformFeatureBaseNames = new HashSet<>();
        getSelectorCohorts().forEach((String baseName, List<String> featureBaseNames) -> {

            for (String featureBaseName : featureBaseNames) {
                List<String> cohort = cohorts.get(featureBaseName);
                //loops through each version of a platform
                for (String version : cohort) {
                    boolean isMP = baseName.equals("microProfile");
                    String featureName = featureBaseName + "-" + version;
                    String featureShortName = baseName + "-" + version;
                    FeatureInfo featureInfo = getFeature(featureName);
                    if (featureInfo != null) {
                        //each feature dependency of the platform
                        Set<FeatureInfo> publicDepFeatures = getAllPublicDependentFeatures(featureInfo);
                        for (FeatureInfo depInfo : publicDepFeatures) {
                            String featureTitle = depInfo.getShortName().split("-")[0]; //Just the name not the version

                            if (skipFeatures.contains(featureTitle)) {
                                continue;
                            }

                            // Only MicroProfile features (short names that start with "mp") include the microProfile platforms.
                            if (isMP && !depInfo.getShortName().startsWith("mp")) {
                                continue;
                            }

                            platformFeatureBaseNames.add(depInfo.getBaseName());

                            Set<String> platforms = featureToPlatformMapping.get(depInfo.getName());
                            if (platforms == null) {
                                platforms = new HashSet<>();
                                featureToPlatformMapping.put(depInfo.getName(), platforms);
                            }
                            platforms.add(featureShortName);
                        }
                    }
                }
            }
        });

        // Special cases of features that were removed from the platform and from the convenience feature, but are still compatible with later MP / Jakarta
        // versions
        featureToPlatformMapping.get("io.openliberty.enterpriseBeans-4.0").add("jakartaee-11.0");
        featureToPlatformMapping.get("io.openliberty.enterpriseBeansHome-4.0").add("jakartaee-11.0");
        featureToPlatformMapping.get("io.openliberty.xmlBinding-4.0").add("jakartaee-11.0");
        featureToPlatformMapping.get("io.openliberty.xmlWS-4.0").add("jakartaee-11.0");
        featureToPlatformMapping.get("io.openliberty.mpMetrics-5.1").add("microProfile-7.0");

        // jdbc is special
        featureToPlatformMapping.get("com.ibm.websphere.appserver.jdbc-4.1").remove("javaee-8.0");
        featureToPlatformMapping.get("com.ibm.websphere.appserver.jdbc-4.2").add("javaee-8.0");
        featureToPlatformMapping.get("com.ibm.websphere.appserver.jdbc-4.1").remove("jakartaee-8.0");
        featureToPlatformMapping.get("com.ibm.websphere.appserver.jdbc-4.2").add("jakartaee-8.0");
        featureToPlatformMapping.get("com.ibm.websphere.appserver.jdbc-4.2").remove("jakartaee-11.0");
        Set<String> jdbc43Platform = new HashSet<>();
        jdbc43Platform.add("jakartaee-11.0");
        featureToPlatformMapping.put("com.ibm.websphere.appserver.jdbc-4.3", jdbc43Platform);

        // Now that we have a mapping of features to the platform features that they belong to validates that the platform settings in the features
        // include all of the convenience features that enable those features.

        for (Entry<String, Set<String>> entry : featureToPlatformMapping.entrySet()) {
            String featureName = entry.getKey();
            Set<String> platforms = entry.getValue();
            FeatureInfo feature = getFeature(featureName);
            Set<String> featurePlatforms = feature.getPlatforms();

            int matches = 0;
            for (String featurePlatform : featurePlatforms) {
                // Skip javaee-6.0 since javaee 6 convenience feature isn't in Open Liberty
                if (featurePlatform.equals("javaee-6.0")) {
                    continue;
                }
                if (platforms.contains(featurePlatform)) {
                    matches++;
                } else {
                    errorMessage.append(featureName).append(" contains a platform ").append(featurePlatform)
                                .append(" even though it isn't enabled by that platform convenience feature\n");
                }
            }
            if (matches != platforms.size()) {
                for (String platform : platforms) {
                    if (!featurePlatforms.contains(platform)) {
                        errorMessage.append(featureName).append(" is missing platform ").append(platform).append("\n");
                    }
                }
            }
        }

        // Special private features that also should have platforms listed in them
        platformFeatureBaseNames.add("io.openliberty.internal.mpVersion");
        platformFeatureBaseNames.add("com.ibm.websphere.appserver.eeCompatible");

        // MicroProfile standalone features that we want to be versionless as well
        platformFeatureBaseNames.add("com.ibm.websphere.appserver.mpContextPropagation");
        platformFeatureBaseNames.add("com.ibm.websphere.appserver.mpGraphQL");
        platformFeatureBaseNames.add("com.ibm.websphere.appserver.mpReactiveMessaging");
        platformFeatureBaseNames.add("com.ibm.websphere.appserver.mpReactiveStreams");
        platformFeatureBaseNames.add("io.openliberty.mpContextPropagation");
        platformFeatureBaseNames.add("io.openliberty.mpGraphQL");
        platformFeatureBaseNames.add("io.openliberty.mpReactiveMessaging");
        platformFeatureBaseNames.add("io.openliberty.mpReactiveStreams");

        // Special versions of features that are not part of a convenience feature even though
        // they share the base feature name as feature we expect to have platforms listed
        Set<String> exceptionCases = new HashSet<>();
        exceptionCases.add("com.ibm.websphere.appserver.appSecurity-1.0"); // We use appSecurity-2.0 for EE 6 and EE 7
        exceptionCases.add("com.ibm.websphere.appserver.websocket-1.0"); // We use websocket-1.1 for EE 7 instead of 1.0
        exceptionCases.add("com.ibm.websphere.appserver.servlet-servletSpi1.0"); // Private feature that has a public feature base name

        Map<String, Set<String>> featurePlatforms = new HashMap<>();
        for (FeatureInfo featureInfo : featureRepo.values()) {
            String featureName = featureInfo.getName();
            boolean expectsPlatforms = platformFeatureBaseNames.contains(featureInfo.getBaseName());

            // If it is a feature that we expect to have platforms listed in it and we haven't already reported it and it isn't a special
            // feature that we don't expect a Platform to be associated with it.
            if (!featureInfo.isAutoFeature() && expectsPlatforms && !exceptionCases.contains(featureName)) {
                if (featureInfo.getPlatforms().isEmpty() && !featureToPlatformMapping.containsKey(featureName)) {
                    errorMessage.append(featureName).append(" is a feature that expects Platforms to be listed\n");
                }
                String featureBaseNameToUse = featureInfo.getShortName();
                if (featureBaseNameToUse != null) {
                    featureBaseNameToUse = FeatureInfo.getBaseName(featureBaseNameToUse);
                } else {
                    featureBaseNameToUse = featureInfo.getBaseName();
                }
                Set<String> platforms = featurePlatforms.get(featureBaseNameToUse);
                if (platforms == null) {
                    platforms = new HashSet<>();
                    featurePlatforms.put(featureBaseNameToUse, platforms);
                }
                Set<String> featureInfoPlatforms = featureInfo.getPlatforms();
                for (String platform : featureInfoPlatforms) {
                    if (!platforms.add(platform)) {
                        errorMessage.append(featureName).append(" contains Platform ").append(platform)
                                    .append(" that is also contained in another feature with the same base feature name ").append(featureBaseNameToUse).append("\n");
                    }
                }
            }

            if (!expectsPlatforms && !featureInfo.getPlatforms().isEmpty()) {
                errorMessage.append(featureName).append(" is not part of a versionless feature so it shouldn't have any Platforms listed\n");
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features that have incorrect WLP-Platform settings:\n" + errorMessage.toString());
        }
    }

    /**
     * Generate versionless features
     * 1. Create a map of each feature (ex. servlet) and data on each of its versions
     * 2. Validate the features using the VersionlessFeatureCreator
     * 3. Create the features that need updating/creating in the build folder
     */
    @Test
    public void listSelectorDetails() {
        Map<String, VersionlessFeatureDefinition> versionlessFeatures = new HashMap<String, VersionlessFeatureDefinition>();

        System.out.println("Selectors:");
        getSelectorCohorts().forEach((String baseName, List<String> featureBaseNames) -> {
            System.out.println("  [ " + baseName + " ]");
            for (String featureBaseName : featureBaseNames) {
                List<String> cohort = cohorts.get(featureBaseName);
                System.out.println("    [ " + featureBaseName + " ] [ " + cohort + " ]");

                //loops through each version of a platform
                for (String version : cohort) {

                    String featureName = featureBaseName + "-" + version;
                    FeatureInfo featureInfo = getFeature(featureName);
                    if (featureInfo == null) {
                        System.out.println("      [ " + featureName + " ** NOT FOUND ** ]");
                        continue;
                    } else {
                        System.out.println("      [ " + featureInfo.getName() + " ]");
                    }

                    //each feature dependency of the platform
                    Set<FeatureInfo> publicDepFeatures = getAllPublicDependentFeatures(featureInfo);
                    for (FeatureInfo depInfo : publicDepFeatures) {
                        System.out.println("        [ " + depInfo.getBaseName() + " - " + depInfo.getVersion() + " ]");

                        if (depInfo.isAlsoKnownAsSet()) {
                            System.out.println("            [ AKA: " + depInfo.getAlsoKnownAs() + " ]");
                        }

                        //
                        String featureTitle = depInfo.getShortName().split("-")[0]; //Just the name not the version

                        //add features to our map and add data on its platform-version link
                        if (versionlessFeatures.containsKey(featureTitle)) {
                            versionlessFeatures.get(featureTitle)
                                               .addFeaturePlatformAndKind(new String[] { depInfo.getShortName(), baseName.replace("javaee", "jakartaee") + "-" + version,
                                                                                         depInfo.getName(), depInfo.getKind() });
                        } else {
                            versionlessFeatures.put(featureTitle,
                                                    new VersionlessFeatureDefinition(featureTitle, featureTitle,
                                                                                     new String[] { depInfo.getShortName(),
                                                                                                    baseName.replace("javaee", "jakartaee") + "-" + version,
                                                                                                    depInfo.getName(),
                                                                                                    depInfo.getKind() },
                                                                                     depInfo.getEdition()));
                        }

                        //Keep track of features with updated names via the alsoknownas metadata
                        if (depInfo.isAlsoKnownAsSet()) {
                            String aka = depInfo.getAlsoKnownAs().split("-")[0];
                            if (!aka.equals(featureTitle)) {
                                if (versionlessFeatures.get(featureTitle).getAlsoKnownAs() == null) {
                                    versionlessFeatures.get(featureTitle).setAlsoKnownAs(aka);
                                }
                                if (versionlessFeatures.containsKey(aka)) {
                                    versionlessFeatures.get(aka).setAKAFutureFeature(featureTitle);
                                }
                            }
                        }
                    }
                }
            }
        });

        versionlessFeatures.get("mpMetrics").addFeaturePlatformAndKind("mpMetrics-5.1", "microProfile-7.0", "io.openliberty.mpMetrics-5.1", "ga");

        VersionlessFeatureCreator creator = new VersionlessFeatureCreator(featureRepo);

        String createdFeatures = "";

        for (String title : versionlessFeatures.keySet()) {
            //We don't want to have versionless features of other convenience features, so skip them
            if (skipFeatures.contains(title)) {
                continue;
            }
            boolean createdFile = false;
            VersionlessFeatureDefinition feat = versionlessFeatures.get(title);
            try {
                if (feat.getAlsoKnownAs() != null) {
                    createdFile = creator.createFeatureFiles(feat, versionlessFeatures.get(feat.getAlsoKnownAs()));
                } else if (feat.getAKAFutureFeature() != null) {
                    createdFile = creator.createFeatureFiles(feat, versionlessFeatures.get(feat.getAKAFutureFeature()));
                } else {
                    createdFile = creator.createFeatureFiles(feat, null);
                }
                if (createdFile) {
                    if (createdFeatures.isEmpty()) {
                        createdFeatures += "Versionless feature files were created for feature(s): " + title;
                    } else {
                        createdFeatures += ", " + title;
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        String errorOutput = "There are versionless features that need to be created. " +
                             "Templates for the needed features have been created in the 'build/versionless' directory.\n" +
                             "Keep in mind the templates are not guaranteed to be correct and are solely meant to " +
                             "act as a helpful starting point for creating a versionless feature.\n" +
                             "Verify the data inside the feature files are correct, then copy the features from 'build/versionless' into 'visibility'.\n" + createdFeatures;

        Assert.assertEquals(errorOutput, "", createdFeatures);
    }

    private static final Map<String, List<String>> selectorCohorts;

    private static void put(Map<String, List<String>> storage, String... data) {
        String key = data[0];

        List<String> values = new ArrayList<>(data.length - 1);
        for (int datumNo = 1; datumNo < data.length; datumNo++) {
            values.add(data[datumNo]);
        }

        storage.put(key, values);
    }

    static {
        Map<String, List<String>> useCohorts = new LinkedHashMap<>();

        put(useCohorts, "jakartaee",
            "com.ibm.websphere.appserver.jakartaee", "io.openliberty.jakartaee");

        put(useCohorts, "javaee",
            "com.ibm.websphere.appserver.javaee");

        put(useCohorts, "microProfile",
            "com.ibm.websphere.appserver.microProfile", "io.openliberty.microProfile");

        selectorCohorts = useCohorts;
    }

    public Map<String, List<String>> getSelectorCohorts() {
        return selectorCohorts;
    }

    public static List<String> skipFeatures = new ArrayList<String>(Arrays.asList("webProfile",
                                                                                  "noShip",
                                                                                  "monitor",
                                                                                  "json",
                                                                                  "distributedMap",
                                                                                  "ssl",
                                                                                  "jwt",
                                                                                  "opentracing",
                                                                                  "jndi",
                                                                                  "restConnector", // restConnector-2.0 was erroneously added to jakartaee-9.1
                                                                                  "bells"));

    private static Map<String, int[]> versions = new HashMap<>();

    public static int[] parse(String version) {
        return versions.computeIfAbsent(version, (String useVersion) -> {
            int vOffset = useVersion.indexOf('.');
            if (vOffset == -1) {
                return new int[] { Integer.parseInt(useVersion) };
            } else {
                return new int[] { Integer.parseInt(useVersion.substring(0, vOffset)),
                                   Integer.parseInt(useVersion.substring(vOffset + 1, useVersion.length())) };
            }
        });
    }

    public static int compareVersions(String v0, String v1) {
        int[] fields0 = parse(v0);
        int len0 = fields0.length;

        int[] fields1 = parse(v1);
        int len1 = fields1.length;

        int fNo = 0;
        while ((fNo < len0) && (fNo < len1)) {
            int f0 = fields0[fNo];
            int f1 = fields1[fNo];
            if (f0 < f1) {
                return -1; // 8.x < 9.x
            } else if (f0 > f1) {
                return +1; // 9.x > 8.x
            } else {
                fNo++;
                // loop    // 8.x ? 8.y
            }
        }

        if (fNo == len0) {
            if (fNo == len1) {
                return 0; // 10.x == 10.x
            } else {
                return -1; // 10 < 10.x
            }
        } else if (fNo == len1) {
            return +1; // 8.x > 8
        } else {
            throw new IllegalStateException("Strange comparison of [ " + v0 + " ] with [ " + v0 + " ]");
        }
    }

}