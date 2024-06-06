/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureInfo;
import com.ibm.ws.feature.utils.FeatureMapFactory;
import com.ibm.ws.feature.utils.VersionlessFeatureCreator;
import com.ibm.ws.feature.utils.VersionlessFeatureDefinition;

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
        featureInfo.forEachSortedDepName((String depName) -> {
            FeatureInfo depInfo = getFeature(depName);
            if (depInfo == null) {
                System.out.println("        [ " + depName + " ** NOT FOUND ** ]");
            } else {
                if (depInfo.isPublic()) {
                    publicDepFeatures.add(depInfo);
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

    /**
     * Generate versionless features
     * 1. Create a map of each feature (ex. servlet) and data on each of its versions
     * 2. Validate the features using the VersionlessFeatureCreator
     * 3. Create the features that need updating/creating in the build folder
     */
    @Test
    public void listSelectorDetails() {
        StringBuilder errorMessage = new StringBuilder();
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
                        if (depInfo.getKind().equals("noship")){
                            continue;
                        }
                        System.out.println("        [ " + depInfo.getBaseName() + " - " + depInfo.getVersion() + " ]");
                        if (depInfo.isAlsoKnownAsSet()) {
                            System.out.println("            [ AKA: " + depInfo.getAlsoKnownAs() + " ]");
                        }

                        //
                        String featureTitle = depInfo.getShortName().split("-")[0]; //Just the name not the version

                        if (!skipFeatures.contains(featureTitle) && depInfo.getPlatforms().isEmpty()) {
                            errorMessage.append(depInfo.getName()).append('\n');
                        }

                        //add features to our map and add data on its platform-version link
                        if (versionlessFeatures.containsKey(featureTitle)) {
                            versionlessFeatures.get(featureTitle)
                                               .addFeaturePlatform(new String[] { depInfo.getShortName(), baseName.replace("javaee", "jakartaee") + "-" + version,
                                                                                  depInfo.getName() });
                        } else {
                            versionlessFeatures.put(featureTitle,
                                                    new VersionlessFeatureDefinition(featureTitle, featureTitle,
                                                                                     new String[] { depInfo.getShortName(),
                                                                                                    baseName.replace("javaee", "jakartaee") + "-" + version,
                                                                                                    depInfo.getName() }));
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

        VersionlessFeatureCreator creator = new VersionlessFeatureCreator();

        String createdFeatures = "";

        for (String title : versionlessFeatures.keySet()) {
            //We don't want to have versionless features of other convenience features, so skip them
            System.out.println(title);
            if (skipFeatures.contains(title)) {
                System.out.println("true");
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
                             "Keep in mind the templates are not guarenteed to be correct and are solely meant to " +
                             "act as a helpful starting point for creating a versionless feature.\n" +
                             "Verify the data inside the feature files are correct, then copy the features from 'build/versionless' into 'visibility'.\n" + createdFeatures;

        Assert.assertEquals(errorOutput, "", createdFeatures);

        if (errorMessage.length() != 0) {
            Assert.fail("Found features that are missing WLP-Platform settings:\n" + errorMessage.toString());
        }
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

    /**
     * Presently the Jakarta Container features (jsonbContainer, jsonpContainer, [jsf|faces]Container and [jpa|persistence]Container
     * are not created as versionless features. To be determined if they should.
     */
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
                                                                                  "jpaContainer", // jpa depends on jpaContainer
                                                                                  "persistenceContainer" // persistence depends on persistenceContainer.
    ));

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