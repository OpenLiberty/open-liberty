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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureInfo;
import com.ibm.ws.feature.utils.FeatureRepo;
import com.ibm.ws.feature.utils.VersionlessFeatureDefinition;
import com.ibm.ws.feature.utils.VersionlessFeatureCreator;



public class VersionlessTest {

    public static FeatureRepo getRepository() {
        return FeatureTest.getRepository();
    }

    public static FeatureInfo getFeature(String featureName) {
        return getRepository().getFeature(featureName);
    }

    @Test
    public void listCohorts() {
        Map<String, List<String>> cohorts = getRepository().getCohorts();

        List<String> baseNames = new ArrayList<>(cohorts.keySet());
        baseNames.sort(Comparator.comparing(String::toString));

        System.out.println("Cohorts:");
        for (String baseName : baseNames) {
            List<String> cohort = cohorts.get(baseName);
            System.out.println("  [ " + baseName + " ] [ " + cohort + " ]");
        }
    }

    @Test
    public void listSelectorCohorts() {
        Map<String, List<String>> cohorts = getRepository().getCohorts();

        System.out.println("Selectors:");
        getSelectorCohorts().forEach((String baseName, List<String> featureNames) -> {
            System.out.println("  [ " + baseName + " ]");
            for (String featureName : featureNames) {
                System.out.println("    [ " + featureName + " ] [ " + cohorts.get(featureName) + " ]");
            }
        });
    }

    @Test
    public void listSelectorDetails() {
        Map<String, List<String>> cohorts = getRepository().getCohorts();
        Map<String, VersionlessFeatureDefinition> versionlessFeatures = new HashMap<String, VersionlessFeatureDefinition>();

        System.out.println("Selectors:");
        getSelectorCohorts().forEach((String baseName, List<String> featureBaseNames) -> {
            System.out.println("  [ " + baseName + " ]");
            for (String featureBaseName : featureBaseNames) {
                List<String> cohort = cohorts.get(featureBaseName);
                System.out.println("    [ " + featureBaseName + " ] [ " + cohort + " ]");

                for (String version : cohort) {
                    String featureName = featureBaseName + "-" + version;
                    FeatureInfo featureInfo = getFeature(featureName);
                    if (featureInfo == null) {
                        System.out.println("      [ " + featureName + " ** NOT FOUND ** ]");
                        continue;
                    } else {
                        System.out.println("      [ " + featureInfo.getName() + " ]");
                    }

                    featureInfo.forEachSortedDepName((String depName) -> {
                        FeatureInfo depInfo = getFeature(depName);
                        if (depInfo == null) {
                            System.out.println("        [ " + depName + " ** NOT FOUND ** ]");
                        } else if (depInfo.isPublic()) {
                            System.out.println("        [ " + depInfo.getBaseName() + " - " + depInfo.getVersion() + " ]");
                            
                            String featureTitle = depInfo.getShortName().split("-")[0]; //Just the name not the version
                            if(versionlessFeatures.containsKey(featureTitle)){
                                versionlessFeatures.get(featureTitle).addFeaturePlatform(new String[] { depInfo.getShortName(), baseName + "-" + version, depInfo.getName()});
                            }
                            else {
                                versionlessFeatures.put(featureTitle, new VersionlessFeatureDefinition(featureTitle, featureTitle, new String[] { depInfo.getShortName(), baseName + "-" + version, depInfo.getName()}));
                            }
                        } else {
                            // Ignore
                        }
                    });
                }
            }
        });

        VersionlessFeatureCreator creator = new VersionlessFeatureCreator();

        for(String title : versionlessFeatures.keySet()) {
            VersionlessFeatureDefinition feat = versionlessFeatures.get(title);
			try {
				creator.createPublicVersionlessFeature(feat);
				creator.createPrivateFeatures(feat);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
        put(useCohorts, "jakartaeeClient",
            "io.openliberty.jakartaeeClient");

        put(useCohorts, "javaee",
            "com.ibm.websphere.appserver.javaee");
        put(useCohorts, "javaeeClient",
            "com.ibm.websphere.appserver.javaeeClient");

        put(useCohorts, "microProfile",
            "com.ibm.websphere.appserver.microProfile", "io.openliberty.microProfile");

        put(useCohorts, "webProfile",
            "com.ibm.websphere.appserver.webProfile", "io.openliberty.webProfile");

        selectorCohorts = useCohorts;
    }

    public Map<String, List<String>> getSelectorCohorts() {
        return selectorCohorts;
    }

}
