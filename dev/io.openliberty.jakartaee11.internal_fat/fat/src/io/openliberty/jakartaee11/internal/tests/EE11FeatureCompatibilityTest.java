/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee11.internal.tests;

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
import componenttest.rules.repeater.JakartaEE11Action;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jakartaee11.internal.tests.util.FATFeatureResolver;
import io.openliberty.jakartaee11.internal.tests.util.FATFeatureTester;
import io.openliberty.jakartaee11.internal.tests.util.FATLogger;

/**
 * This test validates that all Open Liberty features that are expected to work
 * with Jakarta EE 11 features resolve correctly compared to a Jakarta EE 11 feature.
 *
 * When a feature is expected to conflict with Jakarta EE 11 features, this test validates
 * that the com.ibm.websphere.appserver.eeCompatible feature is one of the conflicts. This is
 * important because there are messages that come up for eeCompatible conflicts to tell the
 * customer that features are not compatible with Jakarta EE 11.
 *
 * As new value add features are added to Open Liberty this test will assume that they
 * will support Jakarta EE 11. When Jakarta EE 12 is added there will need to be some additional
 * logic to handle Jakarta EE 12 features as being expected to not run with EE 11 features.
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class EE11FeatureCompatibilityTest extends FATServletClient {
    private static final Class<?> c = EE11FeatureCompatibilityTest.class;

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

    public static EE11Features ee11Features;

    public static void setFeatures() throws Exception {
        ee11Features = new EE11Features(FATFeatureResolver.getInstallRoot());
    }

    public static EE11Features getFeatures() {
        return ee11Features;
    }

    public Set<String> getVersionedFeatures() {
        return ee11Features.getVersionedFeatures(openLibertyOnly());
    }

    public Set<String> getVersionlessFeatures() {
        return ee11Features.getVersionlessFeatures(openLibertyOnly());
    }

    public Set<String> getCompatibleFeatures() {
        return ee11Features.getCompatibleFeatures(openLibertyOnly());
    }

    public Set<String> getIncompatibleFeatures() {
        return ee11Features.getIncompatibleFeatures(openLibertyOnly());
    }

    //

    @Test
    public void testEE11FeatureConflictsEE7() throws Exception {
        String method = "testEE11FeatureConflictsEE7";

        FATFeatureTester.FeatureReplacer ee11Replacer = new FATFeatureTester.FeatureReplacer() {
            @Override
            public String getReplacement(String feature) {
                return EE11Features.getEE11Replacement(feature);
            }
        };

        List<String> errors = FATFeatureTester.testRenameConflicts(EE11Features.getEE7ConflictFeatures(), ee11Replacer);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE11: Feature incompatibility errors:", errors);
            Assert.fail("Features incompatibility errors");
        }
    }

    @Test
    public void testEE11FeatureConflictsEE8() throws Exception {
        String method = "testEE11FeatureConflictsEE8";

        FATFeatureTester.FeatureReplacer ee11Replacer = new FATFeatureTester.FeatureReplacer() {
            @Override
            public String getReplacement(String feature) {
                return EE11Features.getEE11Replacement(feature);
            }
        };

        List<String> errors = FATFeatureTester.testRenameConflicts(EE11Features.getEE8ConflictFeatures(),
                                                                   ee11Replacer);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE11: Feature incompatibility errors:", errors);
            Assert.fail("Features incompatibility errors");
        }
    }

    @Test
    public void testCDIConflicts() throws Exception {
        String method = "testCDIConflicts";

        Set<String> versionedFeatures = getVersionedFeatures();
        Set<String> compatibleFeatures = getCompatibleFeatures();
        Set<String> incompatibleFeatures = getIncompatibleFeatures();

//        FATLogger.info(c, method, "Versioned    [ data-1.0 ]: " + versionedFeatures.contains("data-1.0"));
//        FATLogger.info(c, method, "Compatible   [ data-1.0 ]: " + compatibleFeatures.contains("data-1.0"));
//        FATLogger.info(c, method, "Incompatible [ data-1.0 ]: " + incompatibleFeatures.contains("data-1.0"));
//
//        FATLogger.info(c, method, "Versioned    [ data-1.1 ]: " + versionedFeatures.contains("data-1.1"));
//        FATLogger.info(c, method, "Compatible   [ data-1.1 ]: " + compatibleFeatures.contains("data-1.1"));
//        FATLogger.info(c, method, "Incompatible [ data-1.1 ]: " + incompatibleFeatures.contains("data-1.1"));

        String cdiFeatureEE11 = "cdi-4.1";

        Map<String, String> cdiSpecialConflicts = EE11Features.getCDIConflicts();

        List<String> errors = FATFeatureTester.testCompatibility(cdiFeatureEE11, versionedFeatures,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 cdiSpecialConflicts);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE11: CDI compatibility errors:", errors);
            Assert.fail("EE11: CDI compatibility errors");
        }
    }

    @Test
    public void testServletConflicts() throws Exception {
        String method = "testServletConflicts";

        Set<String> versionedFeatures = getVersionedFeatures();
        Set<String> compatibleFeatures = getCompatibleFeatures();
        Set<String> incompatibleFeatures = getIncompatibleFeatures();

        String servletFeatureEE11 = "servlet-6.1";
        Map<String, String> servletSpecialConflicts = EE11Features.getServletConflicts();

        List<String> errors = FATFeatureTester.testCompatibility(servletFeatureEE11, versionedFeatures,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 servletSpecialConflicts);
        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE11: Servlet compatibility errors:", errors);
            Assert.fail("EE11: Servlet compatibility errors");
        }
    }

    @Test
    @Mode(TestMode.FULL)
    public void testJakarta11ConvenienceFeature() throws Exception {
        String method = "testJakarta11ConvenienceFeature";

        Set<String> features = new HashSet<>(getVersionedFeatures());

        Map<String, String> specialConflicts = new HashMap<>();
        // faces and facesContainer conflict with each other
        specialConflicts.put("facesContainer-3.0", "io.openliberty.facesProvider");
        specialConflicts.put("facesContainer-4.0", "io.openliberty.facesProvider");
        specialConflicts.put("facesContainer-4.1", "io.openliberty.facesProvider");
        // the jakartaee-11.0 convenience feature conflicts with itself
        specialConflicts.put("jakartaee-11.0", "io.openliberty.jakartaee");
        // the convenience feature depends on jdbc-4.2 and tolerates 4.3
        specialConflicts.put("jdbc-4.0", "com.ibm.websphere.appserver.jdbc");
        specialConflicts.put("jdbc-4.1", "com.ibm.websphere.appserver.jdbc");
        specialConflicts.put("data-1.1", "io.openliberty.data");

        // opentracing-1.3 and jakartaee-9.1 take over an hour to run on power linux system.
        // For now excluding opentracing-1.3 in order to not go past the 3 hour limit for a
        // Full FAT to run.
        features.remove("opentracing-1.3");

        // Add EE11 features that are not part of EE9
        // They will conflict by their long name
        for (String feature : JakartaEE10Action.EE10_FEATURE_SET) {
            if (!JakartaEE11Action.EE11_FEATURE_SET.contains(feature) &&
                JakartaEE10Action.EE10_FEATURE_SET.contains(feature)) {

                // The features below are not included in the convenience feature
                // so they will not conflict on the long name.
                if (feature.startsWith("connectorsInboundSecurity-") ||
                    feature.startsWith("jsonpContainer-") ||
                    !feature.startsWith("jsonbContainer-") ||
                    !feature.startsWith("facesContainer-") ||
                    !feature.startsWith("jakartaeeClient-")) {
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

        List<String> errors = FATFeatureTester.testCompatibility("jakartaee-11.0", features,
                                                                 compatibleFeatures, incompatibleFeatures,
                                                                 specialConflicts);
        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE11: Feature compatibility errors:", errors);
            Assert.fail("EE11 compatibility errors");
        }
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
        List<String> errors = FATFeatureTester.testVersionlessFeatures(versionlessFeatures, ee11Features.getIncompatibleVersionlessFeatures(), mpPlatform);

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, "testVersionlessFeatures", "EE11: Versionless feature errors:", errors);
            Assert.fail("EE11 versionless feature errors. " + errors);
        }
    }

    /**
     * Verify that SSL transport (ssl-1.0) resolves correctly for EE11 compatible
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

        Set<String> sslEnablingFeatures = new HashSet<>();
        sslEnablingFeatures.add("appSecurity-6.0");
        sslEnablingFeatures.add("messagingSecurity-3.0");

        List<String> errors = new ArrayList<>();

        for (String rootFeature : getCompatibleFeatures()) {
            Set<String> rootFeatures;
            if (sslEnablingFeatures.contains(rootFeature)) {
                rootFeatures = Collections.singleton(rootFeature);

            } else {
                rootFeatures = new HashSet<>(3);
                rootFeatures.add(rootFeature);

                Result nonSSLResult = FATFeatureResolver.resolve(rootFeatures);
                if (!nonSSLResult.getResolvedFeatures().contains("ssl-1.0")) {
                    rootFeatures.add("ssl-1.0");
                }

                String addFeature;
                if (!rootFeature.equals("cdi-4.1")) {
                    addFeature = "cdi-4.1";
                } else {
                    addFeature = "expressionLanguage-6.0";
                }
                rootFeatures.add(addFeature);
            }

            Result sslResult = FATFeatureResolver.resolve(rootFeatures);
            Map<String, Collection<Chain>> conflicts = sslResult.getConflicts();

            if (!conflicts.isEmpty()) {
                errors.add("EE11: Unexpected conflicts resolving [ " + rootFeatures + " ]: [ " + conflicts.keySet() + " ]");
            } else if (!sslResult.getResolvedFeatures().contains("transportSecurity-1.0")) {
                errors.add("EE11: Resolving [ " + rootFeatures + " ] did not resolve [ transportSecurity-1.0 ]: " + sslResult.getResolvedFeatures());
            } else {
                // OK: No conflicts and transportSecurity-1.0 resolved.
            }
        }

        if (!errors.isEmpty()) {
            FATLogger.dumpErrors(c, method, "EE11: Transport resolution errors", errors);
            Assert.fail("EE11: Transport resolution errors");
        }
    }
}
