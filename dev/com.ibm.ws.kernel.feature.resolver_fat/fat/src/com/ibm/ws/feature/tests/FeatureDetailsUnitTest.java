/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.tests.util.FeatureInfo;
import com.ibm.ws.feature.tests.util.FeatureUtil;
import com.ibm.ws.feature.tests.util.RepositoryUtil;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

import junit.framework.Assert;

public class FeatureDetailsUnitTest {
    public static final String SERVER_NAME = "FeatureResolverTest";

    //

    private static boolean didSetup;

    @BeforeClass
    public static void setupClass() throws Exception {
        if (didSetup) {
            return;
        } else {
            didSetup = true;
        }

        RepositoryUtil.setupFeatures();
        RepositoryUtil.setupRepo(SERVER_NAME);

        setupMaps();
    }

    private static Map<String, FeatureInfo> featureMap;

    public static Map<String, ? extends FeatureInfo> getFeatureMap() {
        return featureMap;
    }

    public static FeatureInfo getFeature(String symName) {
        return featureMap.get(symName);
    }

    private static Map<String, ProvisioningFeatureDefinition> featureDefMap;

    public static Map<String, ? extends ProvisioningFeatureDefinition> getFeatureDefMap() {
        return featureDefMap;
    }

    public static ProvisioningFeatureDefinition getFeatureDef(String symName) {
        return featureDefMap.get(symName);
    }

    // Feature definition [ txtest-1.0 ]
    // Feature definition [ txtest-2.0 ]
    // Feature definition [ com.ibm.websphere.appserver.componenttest-1.0 ]
    // Feature definition [ com.ibm.websphere.appserver.componenttest-2.0 ]

    // Feature definition [ io.openliberty.arquillian.arquillian-support-1.0 ]
    // Feature definition [ io.openliberty.arquillian.arquillian-support-jakarta-2.1 ]

    // Feature definition [ com.ibm.websphere.appserver.http2clienttest-1.0 ]

    // Feature definition [ com.ibm.websphere.appserver.timedexit-1.0 ]

    // Feature definition [ test.InterimFixManagerTest-1.0 ]
    // Feature definition [ test.TestFixManagerTest-1.0 ]
    // Feature definition [ test.featurefixmanager-1.0 ]
    // Feature definition [ test.InterimFixesManagerTest-1.0 ]

    protected static final Set<String> ignoreFeatures;

    static {
        Set<String> ignored = new HashSet<>(8);
        ignored.add("txtest-1.0");
        ignored.add("txtest-2.0");
        ignored.add("com.ibm.websphere.appserver.componenttest-1.0");
        ignored.add("com.ibm.websphere.appserver.componenttest-2.0");
        ignored.add("io.openliberty.arquillian.arquillian-support-1.0");
        ignored.add("io.openliberty.arquillian.arquillian-support-jakarta-2.1");
        ignored.add("com.ibm.websphere.appserver.http2clienttest-1.0");
        ignored.add("com.ibm.websphere.appserver.timedexit-1.0");

        ignored.add("test.InterimFixManagerTest-1.0");
        ignored.add("test.TestFixManagerTest-1.0");
        ignored.add("test.featurefixmanager-1.0");
        ignored.add("test.InterimFixesManagerTest-1.0");

        ignoreFeatures = ignored;
    }

    public static boolean ignore(String symName) {
        return ignoreFeatures.contains(symName);
    }

    protected static void setupMaps() {
        List<FeatureInfo> features = RepositoryUtil.getFeaturesList();
        Map<String, FeatureInfo> useFeatureMap = new HashMap<String, FeatureInfo>(features.size());
        for (FeatureInfo feature : features) {
            String symName = feature.getName();
            if (!ignore(symName)) {
                useFeatureMap.put(symName, feature);
            }
        }
        featureMap = useFeatureMap;

        List<ProvisioningFeatureDefinition> featureDefs = RepositoryUtil.getFeatureDefs();
        Map<String, ProvisioningFeatureDefinition> useFeatureDefMap = new HashMap<String, ProvisioningFeatureDefinition>(featureDefs.size());
        for (ProvisioningFeatureDefinition featureDef : featureDefs) {
            String symName = featureDef.getSymbolicName();
            if (!ignore(symName)) {
                useFeatureDefMap.put(symName, featureDef);
            }
        }
        featureDefMap = useFeatureDefMap;
    }

    @Test
    public void features_validateMapsTest() throws Exception {
        Map<String, ? extends FeatureInfo> useFeatureMap = getFeatureMap();
        int numFeatures = useFeatureMap.size();
        Map<String, ? extends ProvisioningFeatureDefinition> useFeatureDefMap = getFeatureDefMap();
        int numFeatureDefs = useFeatureDefMap.size();

        System.out.println("Validating [ " + numFeatures + " ] features");
        System.out.println("Validating [ " + numFeatureDefs + " ] feature definitions");

        boolean unequalMaps = (numFeatures != numFeatureDefs);
        if (unequalMaps) {
            System.out.println("Failed: Have [ " + numFeatures + " ] features and [ " + numFeatureDefs + " ] feature definitions");
        }

        boolean missingFeatureDef = false;
        for (String featureSymName : useFeatureMap.keySet()) {
            if (useFeatureDefMap.get(featureSymName) == null) {
                missingFeatureDef = true;
                System.out.println("Feature [ " + featureSymName + " ] not found in defs");
            }
        }

        boolean missingFeature = false;
        for (String featureSymName : useFeatureDefMap.keySet()) {
            if (useFeatureMap.get(featureSymName) == null) {
                missingFeature = true;
                System.out.println("Feature definition [ " + featureSymName + " ] not found in features");
            }
        }

        if (unequalMaps || missingFeatureDef || missingFeature) {
            Assert.fail("Features do not match the feature definitions");
        }
    }

    protected void validate(String banner, FeatureTester tester) {
        banner += " ...";
        System.out.println(banner);

        Map<String, ? extends FeatureInfo> useFeatureMap = getFeatureMap();
        Map<String, ? extends ProvisioningFeatureDefinition> useFeatureDefMap = getFeatureDefMap();

        boolean failed = false;

        for (Map.Entry<String, ? extends ProvisioningFeatureDefinition> defEntry : useFeatureDefMap.entrySet()) {
            String symName = defEntry.getKey();
            ProvisioningFeatureDefinition featureDef = defEntry.getValue();

            FeatureInfo feature = useFeatureMap.get(symName);
            if (feature == null) {
                continue;
            }

            if (!tester.validate(symName, featureDef, feature)) {
                failed = true;
            }
        }

        if (failed) {
            Assert.fail(banner + " failed");
        } else {
            System.out.println(banner + " succeeded");
        }
    }

    public static interface FeatureTester {
        boolean validate(String symName, ProvisioningFeatureDefinition featureDef, FeatureInfo featureInfo);
    }

    @Test
    public void features_validateVisibilityTest() throws Exception {
        FeatureTester visibilityTester = new FeatureTester() {
            @Override
            public boolean validate(String symName,
                                    ProvisioningFeatureDefinition featureDef,
                                    FeatureInfo featureInfo) {

                String defVisibility = featureDef.getVisibility().toString();
                String infoVisibility = featureInfo.getVisibility();

                if (!defVisibility.equalsIgnoreCase(infoVisibility)) {
                    System.out.println("Feature visibility error [ " + symName + " ]" +
                                       " Definition visibility [ " + defVisibility + " ] " +
                                       " Info visibility [ " + infoVisibility + " ]");
                    return false;
                } else {
                    return true;
                }
            }
        };

        validate("Validate feature visibility", visibilityTester);
    }

    @Test
    public void features_validateIsVersionlessTest() throws Exception {
        FeatureTester isVersionlessTester = new FeatureTester() {
            @Override
            public boolean validate(String symName,
                                    ProvisioningFeatureDefinition featureDef,
                                    FeatureInfo featureInfo) {

                boolean defIsVersionless = featureDef.isVersionless();

                boolean utilIsVersionless = FeatureUtil.isVersionless(featureDef);

                if (defIsVersionless != utilIsVersionless) {
                    System.out.println("Feature versionless error [ " + symName + " ]");
                    System.out.println("   Definition is-versionless [ " + defIsVersionless + " ] ");
                    System.out.println("   Utility is-versionless [ " + utilIsVersionless + " ]");
                    return false;
                } else {
                    return true;
                }
            }
        };

        validate("Validate feature versionless", isVersionlessTester);
    }

    @Test
    public void features_validatePlatformsTest() throws Exception {
        FeatureTester platformsTester = new FeatureTester() {
            @Override
            public boolean validate(String symName,
                                    ProvisioningFeatureDefinition featureDef,
                                    FeatureInfo featureInfo) {

                List<String> defPlatforms = featureDef.getPlatformNames();
                String defPlatform = featureDef.getPlatformName();

                List<String> infoPlatforms = featureInfo.getPlatforms();
                if (infoPlatforms == null) {
                    infoPlatforms = Collections.emptyList();
                }

                if (!sameAs(defPlatforms, infoPlatforms)) {
                    System.out.println("Feature platforms error:");
                    System.out.println("   Definition plaforms [ " + defPlatforms + " ] ");
                    System.out.println("   Info platforms [ " + infoPlatforms + " ]");

                    return false;
                }

                boolean valueError;
                if (!defPlatforms.isEmpty()) {
                    if (defPlatform == null) {
                        valueError = true;
                    } else if (!defPlatform.equals(defPlatforms.get(0))) {
                        valueError = true;
                    } else {
                        valueError = false;
                    }
                } else {
                    if (defPlatform != null) {
                        valueError = true;
                    } else {
                        valueError = false;
                    }
                }

                if (valueError) {
                    System.out.println("Feature platforms error:");
                    System.out.println("   Definition plaforms [ " + defPlatforms + " ] ");
                    System.out.println("   Platform value [ " + defPlatform + " ]");

                    return false;
                }

                return true;
            }
        };

        validate("Validate feature platforms", platformsTester);
    }

    protected boolean sameAs(List<String> l0, List<String> l1) {
        if (l0.size() != l1.size()) {
            return false;
        }

        Set<String> s0 = new HashSet<String>(l0);
        Set<String> s1 = new HashSet<String>(l1);
        if (s0.size() != s1.size()) {
            return false;
        }

        for (String e0 : s0) {
            if (!s1.contains(e0)) {
                return false;
            }
        }

        for (String e1 : s1) {
            if (!s0.contains(e1)) {
                return false;
            }
        }

        return true;
    }

    @Test
    public void features_validateIsConvenienceTest() throws Exception {
        FeatureTester isConvenienceTester = new FeatureTester() {
            @Override
            public boolean validate(String symName,
                                    ProvisioningFeatureDefinition featureDef,
                                    FeatureInfo featureInfo) {

                @SuppressWarnings("unused")
                boolean utilIsConvenience = FeatureUtil.isConvenience(featureDef);
                return true;
            }
        };

        validate("Validate feature is-convenience", isConvenienceTester);
    }

    @Test
    public void features_validateIsCompatibilityTest() throws Exception {
        FeatureTester isCompatibilityTester = new FeatureTester() {
            @Override
            public boolean validate(String symName,
                                    ProvisioningFeatureDefinition featureDef,
                                    FeatureInfo featureInfo) {

                boolean defIsCompatibility = featureDef.isCompatibility();

                @SuppressWarnings("unused")
                boolean utilIsCompatibility = FeatureUtil.isCompatibility(featureDef);

                if (defIsCompatibility != utilIsCompatibility) {
                    System.out.println("Feature is-compatibility error [ " + symName + " ]:");
                    System.out.println("   Definition is-compatibility [ " + defIsCompatibility + " ] ");
                    System.out.println("   Info is-compatibility [ " + utilIsCompatibility + " ]");

                    return false;
                }

                return true;
            }
        };

        validate("Validate feature is-compatibility", isCompatibilityTester);
    }

}
