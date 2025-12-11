/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee10.internal.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jarkartaee10.internal.tests.util.FATFeatureResolver;
import io.openliberty.jarkartaee10.internal.tests.util.FATFeatureTester;
import io.openliberty.jarkartaee10.internal.tests.util.FATLogger;

/**
 * This test validates that all Open Liberty features that are expected to work
 * with Jakarta EE 10 features resolve correctly compared to a Jakarta EE 10 feature.
 *
 * When a feature is expected to conflict with Jakarta EE 10 features, this test validates
 * that the com.ibm.websphere.appserver.eeCompatible feature is one of the conflicts. This is
 * important because there are messages that come up for eeCompatible conflicts to tell the
 * customer that features are not compatible with Jakarta EE 10.
 *
 * As new value add features are added to Open Liberty this test will assume that they
 * will support Jakarta EE 10. When Jakarta EE 11 is added there will need to be some additional
 * logic to handle Jakarta EE 11 features as being expected to not run with EE 10 features.
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class EE10FeatureCompatibilityTest extends FATServletClient {
    private static final Class<?> c = EE10FeatureCompatibilityTest.class;

    //

    @BeforeClass
    public static void setUp() throws Exception {
        setFeatures();
        FATFeatureResolver.setup();
    }

    //

    /**
     * Control setting: Should tested features be restricted to
     * open liberty features.
     */
    public static boolean openLibertyOnly() {
        return false;
    }

    public static EE10Features ee10Features;

    public static void setFeatures() throws Exception {
        ee10Features = new EE10Features(FATFeatureResolver.getInstallRoot());
    }

    public static EE10Features getFeatures() {
        return ee10Features;
    }

    public Set<String> getVersionedFeatures() {
        return ee10Features.getVersionedFeatures(openLibertyOnly());
    }

    public Set<String> getVersionlessFeatures() {
        return ee10Features.getVersionlessFeatures(openLibertyOnly());
    }

    public Set<String> getCompatibleFeatures() {
        return ee10Features.getCompatibleFeatures(openLibertyOnly());
    }

    public Set<String> getIncompatibleFeatures() {
        return ee10Features.getIncompatibleFeatures(openLibertyOnly());
    }

    //

    @Test
    public void testEE10FeatureConflictsEE7() throws Exception {
        String method = "testEE10FeatureConflictsEE7";

        FATFeatureTester.FeatureReplacer ee10Replacer = new FATFeatureTester.FeatureReplacer() {
            @Override
            public String getReplacement(String feature) {
                return FeatureReplacementAction.getReplacementFeature(feature, JakartaEE10Action.EE10_FEATURE_SET, Collections.<String> emptySet());
            }
        };

        List<String> errors = FATFeatureTester.testRenameConflicts(EE10Features.getEE7Conflicts(), ee10Replacer);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE10: Feature incompatibility errors:", errors);
            Assert.fail("Features incompatibility errors");
        }
    }

    @Test
    public void testEE10FeatureConflictsEE8() throws Exception {
        String method = "testEE10FeatureConflictsEE8";

        FATFeatureTester.FeatureReplacer ee10Replacer = new FATFeatureTester.FeatureReplacer() {
            @Override
            public String getReplacement(String feature) {
                return FeatureReplacementAction.getReplacementFeature(feature, JakartaEE10Action.EE10_FEATURE_SET, Collections.<String> emptySet());
            }
        };

        List<String> errors = FATFeatureTester.testRenameConflicts(EE10Features.getEE8Conflicts(), ee10Replacer);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE10: Feature incompatibility errors:", errors);
            Assert.fail("Features incompatibility errors");
        }
    }

    /**
     * EE10 sample feature test: Verify the feature resolution results for "cdi-4.0",
     * selected as a sample EE10 feature.
     *
     * In addition, look for conflicts with other CDI versions.
     */
    @Test
    public void testCDIConflicts() throws Exception {
        String method = "testCDIConflicts";

        Set<String> versionedFeatures = getVersionedFeatures();
        Set<String> compatibleFeatures = getCompatibleFeatures();
        Set<String> incompatibleFeatures = getIncompatibleFeatures();

        String cdiFeatureEE10 = "cdi-4.0";

        Map<String, String> cdiSpecialConflicts = EE10Features.getCDIConflicts();

        List<String> errors = FATFeatureTester.testCompatibility(cdiFeatureEE10, versionedFeatures,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 cdiSpecialConflicts);
        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE10: Feature compatibility errors:", errors);
            Assert.fail("EE10 compatibility errors");
        }
    }

    /**
     * EE10 sample feature test: Verify the feature resolution results for "servlet-5.0",
     * selected as a sample EE10 feature.
     *
     * In addition, look for conflicts with other servlet versions.
     */
    @Test
    public void testServletConflicts() throws Exception {
        String method = "testServletConflicts";

        Set<String> versionedFeatures = getVersionedFeatures();
        Set<String> compatibleFeatures = getCompatibleFeatures();
        Set<String> incompatibleFeatures = getIncompatibleFeatures();

        String servletFeatureEE10 = "servlet-6.0";
        Map<String, String> servletSpecialConflicts = EE10Features.getServletConflicts();

        List<String> errors = FATFeatureTester.testCompatibility(servletFeatureEE10, versionedFeatures,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 servletSpecialConflicts);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE10: Feature compatibility errors:", errors);
            Assert.fail("EE10 compatibility errors");
        }
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJakarta10ConvenienceFeature() throws Exception {
        String method = "testJakarta10ConvenienceFeature";

        Set<String> features = new HashSet<>(getVersionedFeatures());

        // opentracing-1.3 and jakartaee-10.0 take over an hour to run on power linux system.
        // For now excluding opentracing-1.3 in order to not go past the 3 hour limit for a
        // Full FAT to run.
        features.remove("opentracing-1.3");

        Map<String, String> specialConflicts = new HashMap<>();
        // faces and facesContainer conflict with each other
        specialConflicts.put("facesContainer-3.0", "io.openliberty.facesProvider");
        specialConflicts.put("facesContainer-4.0", "io.openliberty.facesProvider");
        specialConflicts.put("facesContainer-4.1", "io.openliberty.facesProvider");
        // the jakartaee-10.0 convenience feature conflicts with itself
        specialConflicts.put("jakartaee-10.0", "io.openliberty.jakartaee");
        // the convenience feature depends on jdbc-4.2 and tolerates 4.3
        specialConflicts.put("jdbc-4.0", "com.ibm.websphere.appserver.jdbc");
        specialConflicts.put("jdbc-4.1", "com.ibm.websphere.appserver.jdbc");

        // Add EE9 features that are not part of EE10
        // They will conflict by their long name
        for (String feature : JakartaEE9Action.EE9_FEATURE_SET) {
            if (!JakartaEE10Action.EE10_FEATURE_SET.contains(feature)) {
                // The features below are not included in the convenience feature
                // so they will not conflict on the long name.
                if (feature.startsWith("connectorsInboundSecurity-") ||
                    feature.startsWith("jsonpContainer-") ||
                    feature.startsWith("jsonbContainer-") ||
                    feature.startsWith("facesContainer-") ||
                    feature.startsWith("jakartaeeClient-")) {
                    continue;
                }

                String conflictFeature;
                if (feature.startsWith("servlet-")) {
                    conflictFeature = "com.ibm.websphere.appserver.servlet";
                } else {
                    conflictFeature = "io.openliberty." + feature.substring(0, feature.indexOf('-'));
                }

                specialConflicts.put(feature, conflictFeature);
            }
        }

        Set<String> compatibleFeatures = getCompatibleFeatures();
        Set<String> incompatibleFeatures = getIncompatibleFeatures();

        List<String> errors = FATFeatureTester.testCompatibility("jakartaee-10.0", features,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 specialConflicts);
        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE10: Feature compatibility errors:", errors);
            Assert.fail("EE10 compatibility errors");
        }
    }

    @Test
    public void testVersionlessFeaturesMP60() {
        testVersionlessFeatures("microProfile-6.0");
    }

    @Test
    public void testVersionlessFeaturesMP61() {
        testVersionlessFeatures("microProfile-6.1");
    }

    @Test
    public void testVersionlessFeaturesMP70() {
        testVersionlessFeatures("microProfile-7.0");
    }

    @Test
    public void testVersionlessFeaturesMP71() {
        testVersionlessFeatures("microProfile-7.1");
    }

    private void testVersionlessFeatures(String mpPlatform) {
        Set<String> versionlessFeatures = getVersionlessFeatures();
        List<String> errors = FATFeatureTester.testVersionlessFeatures(versionlessFeatures, ee10Features.getIncompatibleVersionlessFeatures(), mpPlatform);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, "testVersionlessFeatures", "EE10: Versionless feature errors:", errors);
            Assert.fail("EE10 versionless feature errors. " + errors);
        }
    }

    /**
     * Verify that SSL transport (ssl-1.0) resolves correctly for EE10 compatible
     * features.
     *
     * Verify that compatible features which enable SSL resolve as singletons
     * without conflicts.
     *
     * Verify that compatible features which do not enable SSL resolve successfully
     * with SSL added to the root features.
     *
     * Verify that compatible features which do not enable SSL resolve successfully
     * when either jsonp-2.0 or jsonb-2.0 is added to the root features.
     *
     * Successful resolution means that no conflicts occur, and SSL security
     * (transport-security-1.0) is a resolved feature.
     */
    @Test
    public void testTransportResolution() {
        String method = "testTransportResolution";

        Set<String> sslEnablingFeatures = new HashSet<>(); // These enable SSL.
        sslEnablingFeatures.add("appSecurity-5.0");
        sslEnablingFeatures.add("messagingSecurity-3.0");

        List<String> errors = new ArrayList<>();

        for (String rootFeature : getCompatibleFeatures()) {
            Set<String> rootFeatures;
            if (sslEnablingFeatures.contains(rootFeature)) {
                // Only do one resolution of features which are known
                // to enable SSL.
                rootFeatures = Collections.singleton(rootFeature);

            } else {
                // Do an initial resolution of other features.
                // Add ssl-1.0 to the root features if necessary.
                // Add either of jsonb-2.0 or jsonp-2.0 to the
                // root features.
                //
                // The additional of ssl-1.0 and of a json feature should
                // force transportSecurity-1.0 to be resolved.

                rootFeatures = new HashSet<>(3);
                rootFeatures.add(rootFeature);

                Result nonSSLResult = FATFeatureResolver.resolve(rootFeatures);

                if (!nonSSLResult.getResolvedFeatures().contains("ssl-1.0")) {
                    rootFeatures.add("ssl-1.0");
                }

                String jsonFeature = rootFeature.equals("jsonp-2.1") ? "jsonb-3.0" : "jsonp-2.1";
                rootFeatures.add(jsonFeature);
            }

            Result sslResult = FATFeatureResolver.resolve(rootFeatures);
            Map<String, Collection<Chain>> conflicts = sslResult.getConflicts();

            if (!conflicts.isEmpty()) {
                errors.add("EE10: Unexpected conflicts resolving [ " + rootFeatures + " ]: [ " + conflicts.keySet() + " ]");
            } else if (!sslResult.getResolvedFeatures().contains("transportSecurity-1.0")) {
                errors.add("EE10: Resolving [ " + rootFeatures + " ] did not resolve [ transportSecurity-1.0 ]: " + sslResult.getResolvedFeatures());
            } else {
                // OK: No conflicts and transportSecurity-1.0 resolved.
            }
        }

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE10: Transport resolution errors", errors);
            Assert.fail("EE10: Transport resolution errors");
        }
    }
}
