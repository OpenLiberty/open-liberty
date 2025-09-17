/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee9.internal.tests;

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
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jakartaee9.internal.tests.util.FATFeatureResolver;
import io.openliberty.jakartaee9.internal.tests.util.FATFeatureTester;
import io.openliberty.jakartaee9.internal.tests.util.FATLogger;

/**
 * Verify that features which are compatible with EE9 do resolve successfully
 * with EE9 features.
 *
 * Verify that incompatible features generate a conflict with an EE compatibility
 * feature. This is important as conflict messages rely on the EE compatibility
 * conflict being present.
 */
@RunWith(FATRunner.class)
public class EE9FeatureCompatibilityTest extends FATServletClient {
    private static final Class<?> c = EE9FeatureCompatibilityTest.class;

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

    //

    public static EE9Features ee9Features;

    public static void setFeatures() throws Exception {
        ee9Features = new EE9Features(FATFeatureResolver.getInstallRoot());
    }

    public static EE9Features getFeatures() {
        return ee9Features;
    }

    public Set<String> getVersionedFeatures() {
        return ee9Features.getVersionedFeatures(openLibertyOnly());
    }

    public Set<String> getVersionlessFeatures() {
        return ee9Features.getVersionlessFeatures(openLibertyOnly());
    }

    public Set<String> getCompatibleFeatures() {
        return ee9Features.getCompatibleFeatures(openLibertyOnly());
    }

    public Set<String> getIncompatibleFeatures() {
        return ee9Features.getIncompatibleFeatures(openLibertyOnly());
    }

    //

    @Test
    public void testEE9FeatureConflictsEE7() throws Exception {
        String method = "testEE9FeatureConflictsEE7";

        FATFeatureTester.FeatureReplacer ee9Replacer = new FATFeatureTester.FeatureReplacer() {
            @Override
            public String getReplacement(String feature) {
                return EE9Features.getEE9Replacement(feature);
            }
        };

        List<String> errors = FATFeatureTester.testRenameConflicts(EE9Features.getEE7ConflictFeatures(), ee9Replacer);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE9: Feature incompatibility errors:", errors);
            Assert.fail("Features incompatibility errors. " + errors);
        }
    }

    @Test
    public void testEE9FeatureConflictsEE8() throws Exception {
        String method = "testEE9FeatureConflictsEE8";

        FATFeatureTester.FeatureReplacer ee9Replacer = new FATFeatureTester.FeatureReplacer() {
            @Override
            public String getReplacement(String feature) {
                return EE9Features.getEE9Replacement(feature);
            }
        };

        List<String> errors = FATFeatureTester.testRenameConflicts(EE9Features.getEE8ConflictFeatures(),
                                                                   ee9Replacer);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE9: Feature incompatibility errors:", errors);
            Assert.fail("Features incompatibility errors. " + errors);
        }
    }

    /**
     * EE9 sample feature test: Verify the feature resolution results for "cdi-3.0",
     * selected as a sample EE9 feature.
     *
     * In addition, look for conflicts with other CDI versions.
     */
    @Test
    public void testCDIConflicts() throws Exception {
        String method = "testCDIConflicts";

        Set<String> versionedFeatures = getVersionedFeatures();
        Set<String> compatibleFeatures = getCompatibleFeatures();
        Set<String> incompatibleFeatures = getIncompatibleFeatures();

        String cdiFeatureEE9 = "cdi-3.0";

        Map<String, String> cdiSpecialConflicts = EE9Features.getCDIConflicts();

        List<String> errors = FATFeatureTester.testCompatibility(cdiFeatureEE9, versionedFeatures,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 cdiSpecialConflicts);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE9: Feature compatibility errors:", errors);
            Assert.fail("EE9 compatibility errors. " + errors);
        }
    }

    /**
     * EE9 sample feature test: Verify the feature resolution results for "servlet-5.0",
     * selected as a sample EE9 feature.
     *
     * In addition, look for conflicts with other servlet versions.
     */
    @Test
    public void testServletConflicts() throws Exception {
        String method = "testServletConflicts";

        Set<String> versionedFeatures = getVersionedFeatures();
        Set<String> compatibleFeatures = getCompatibleFeatures();
        Set<String> incompatibleFeatures = getIncompatibleFeatures();

        String servletFeatureEE9 = "servlet-5.0";
        Map<String, String> servletSpecialConflicts = EE9Features.getServletConflicts();

        List<String> errors = FATFeatureTester.testCompatibility(servletFeatureEE9, versionedFeatures,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 servletSpecialConflicts);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE9: Feature compatibility errors:", errors);
            Assert.fail("EE9 compatibility errors. " + errors);
        }
    }

    /**
     * This test is marked as FULL FAT because it can take 10 to 30 minutes to run depending on
     * the system. This puts it past the 5 minute expected limit for lite tests. The cdi-3.0 test
     * is a quick test to show that the basics work with feature resolution.
     */
    // For now don't run this test until work is done in feature resolution to get conflict errors faster
    // On power linux build systems this test ends up causing timeout because this test takes so long.
    // On other platforms it also takes a long time, but doesn't cause timeout on the build systems.
    //@Test
    @Mode(TestMode.FULL)
    public void testJakarta91ConvenienceFeature() throws Exception {
        String method = "testJakarta91ConvenienceFeature";

        Set<String> features = new HashSet<>(getVersionedFeatures());

        // opentracing-1.3 and jakartaee-9.1 take over an hour to run on power linux system.
        // For now excluding opentracing-1.3 in order to not go past the 3 hour limit for a
        // Full FAT to run.
        features.remove("opentracing-1.3");

        Map<String, String> specialConflicts = new HashMap<>();

        // faces and facesContainer conflict with each other
        specialConflicts.put("facesContainer-3.0", "io.openliberty.facesProvider");
        specialConflicts.put("facesContainer-4.0", "io.openliberty.facesProvider");
        specialConflicts.put("facesContainer-4.1", "io.openliberty.facesProvider");

        // the jakartaee-9.1 convenience feature conflicts with itself
        specialConflicts.put("jakartaee-9.1", "io.openliberty.jakartaee");

        // the convenience feature depends on jdbc-4.2 and tolerates 4.3
        specialConflicts.put("jdbc-4.0", "com.ibm.websphere.appserver.jdbc");
        specialConflicts.put("jdbc-4.1", "com.ibm.websphere.appserver.jdbc");

        // Add EE10 features that are not part of EE9
        // They will conflict by their long name
        for (String feature : JakartaEE10Action.EE10_FEATURE_SET) {
            if (!JakartaEE9Action.EE9_FEATURE_SET.contains(feature)) {
                // The features below are not included in the convenience feature
                // so they will not conflict on the long name.
                if (feature.startsWith("nosql-") ||
                    feature.startsWith("data-") ||
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

        List<String> errors = FATFeatureTester.testCompatibility("jakartaee-9.1", features,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 specialConflicts);
        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE9: Feature compatibility errors:", errors);
            Assert.fail("EE9 compatibility errors. " + errors);
        }
    }

    @Test
    public void testVersionlessFeatures() {
        Set<String> versionlessFeatures = getVersionlessFeatures();
        List<String> errors = FATFeatureTester.testVersionlessFeatures(versionlessFeatures, ee9Features.getIncompatibleVersionlessFeatures());

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, "testVersionlessFeatures", "EE9: Versionless feature errors:", errors);
            Assert.fail("EE9 versionless feature errors. " + errors);
        }
    }

    /**
     * Verify that SSL transport (ssl-1.0) resolves correctly for EE9 compatible
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
        sslEnablingFeatures.add("appSecurity-4.0");
        sslEnablingFeatures.add("connectorsInboundSecurity-2.0");
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

                String jsonFeature = rootFeature.equals("jsonp-2.0") ? "jsonb-2.0" : "jsonp-2.0";
                rootFeatures.add(jsonFeature);
            }

            Result sslResult = FATFeatureResolver.resolve(rootFeatures);
            Map<String, Collection<Chain>> conflicts = sslResult.getConflicts();

            if (!conflicts.isEmpty()) {
                errors.add("EE9: Unexpected conflicts resolving [ " + rootFeatures + " ]: [ " + conflicts.keySet() + " ]");
            } else if (!sslResult.getResolvedFeatures().contains("transportSecurity-1.0")) {
                errors.add("EE9: Resolving [ " + rootFeatures + " ] did not resolve [ transportSecurity-1.0 ]: " + sslResult.getResolvedFeatures());
            } else {
                // OK: No conflicts and transportSecurity-1.0 resolved.
            }
        }

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE9: Transport resolution errors", errors);
            Assert.fail("EE9: Transport resolution errors. " + errors);
        }
    }
}
