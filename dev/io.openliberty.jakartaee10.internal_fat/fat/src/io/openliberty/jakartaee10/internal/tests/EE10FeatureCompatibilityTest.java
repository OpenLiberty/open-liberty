/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee10.internal.tests;

import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

import componenttest.annotation.Server;
import componenttest.common.apiservices.Bootstrap;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.FeatureUtilities;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

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

    static Set<String> allFeatures = new HashSet<>();

    static Set<String> compatibleFeatures = new HashSet<>();

    static Set<String> incompatibleFeatures = new HashSet<>();

    static final String serverName = "jakartaee10.fat";
    static final FeatureResolver resolver = new FeatureResolverImpl();
    static FeatureRepository repository;

    @Server("jakartaee10.fat")
    public static LibertyServer server;

    static Set<String> getAllCompatibleFeatures(boolean openLibertyOnly) {
        Set<String> allFeatures = new HashSet<>();
        try {
            File installRoot = new File(Bootstrap.getInstance().getValue("libertyInstallPath"));
            allFeatures.addAll(FeatureUtilities.getFeaturesFromServer(installRoot, openLibertyOnly));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return getCompatibleFeatures(allFeatures, openLibertyOnly);
    }

    private static Set<String> getCompatibleFeatures(Set<String> allFeatures, boolean openLibertyOnly) {
        Set<String> compatibleFeatures = new HashSet<>();

        // By default, features are assumed to be compatible
        compatibleFeatures.addAll(allFeatures);

        // Non-ee10 features are not compatible
        compatibleFeatures.removeAll(FeatureUtilities.allEeFeatures(openLibertyOnly));
        compatibleFeatures.addAll(JakartaEE10Action.EE10_FEATURE_SET);

        // MP features are only compatible if they're in MP versions which work with EE10
        compatibleFeatures.removeAll(FeatureUtilities.allMpFeatures());
        compatibleFeatures.addAll(FeatureUtilities.compatibleMpFeatures(EEVersion.EE10));

        // Value-add features which aren't compatible
        compatibleFeatures.remove("openid-2.0"); // stabilized
        compatibleFeatures.remove("openapi-3.1"); // depends on mpOpenAPI
        compatibleFeatures.remove("opentracing-1.0"); // opentracing depends on mpConfig
        compatibleFeatures.remove("opentracing-1.1");
        compatibleFeatures.remove("opentracing-1.2");
        compatibleFeatures.remove("opentracing-1.3");
        compatibleFeatures.remove("opentracing-2.0");
        compatibleFeatures.remove("sipServlet-1.1"); // purposely not supporting EE 10
        compatibleFeatures.remove("springBoot-1.5"); // springBoot 3.0 only supports EE10
        compatibleFeatures.remove("springBoot-2.0");

        compatibleFeatures.remove("mpReactiveMessaging-3.0"); //still in development

        compatibleFeatures.remove("mpHealth"); //versionless features in development
        compatibleFeatures.remove("mpMetrics");

        if (!openLibertyOnly) {
            // stabilized features
            compatibleFeatures.remove("apiDiscovery-1.0");
            compatibleFeatures.remove("blueprint-1.0");
            compatibleFeatures.remove("httpWhiteboard-1.0");
            compatibleFeatures.remove("mqtt-3.1");
            compatibleFeatures.remove("openapi-3.0");
            compatibleFeatures.remove("osgiAppConsole-1.0");
            compatibleFeatures.remove("osgiAppIntegration-1.0");
            compatibleFeatures.remove("osgiBundle-1.0");
            compatibleFeatures.remove("osgi.jpa-1.0");
            compatibleFeatures.remove("restConnector-1.0");
            compatibleFeatures.remove("rtcomm-1.0");
            compatibleFeatures.remove("rtcommGateway-1.0");
            compatibleFeatures.remove("scim-1.0");
            compatibleFeatures.remove("wab-1.0");
            compatibleFeatures.remove("zosConnect-1.0");
            compatibleFeatures.remove("zosConnect-1.2");

            // depend on previous EE versions and now uses wmqMessagingClient-3.0 for EE9
            compatibleFeatures.remove("wmqJmsClient-1.1");
            compatibleFeatures.remove("wmqJmsClient-2.0");

            // heritage API features
            compatibleFeatures.remove("heritageAPIs-1.0");
            compatibleFeatures.remove("heritageAPIs-1.1");
        }

        // Test features may or may not be compatible, we don't want to assert either way
        compatibleFeatures.removeAll(FeatureUtilities.allTestFeatures());

        return compatibleFeatures;
    }

    public static Set<String> getIncompatibleFeatures(Set<String> allFeatures, Set<String> compatibleFeatures) {
        Set<String> incompatibleFeatures = new HashSet<>();

        // Logically, incompatible features are all those that aren't compatible...
        incompatibleFeatures.addAll(allFeatures);
        incompatibleFeatures.removeAll(compatibleFeatures);

        incompatibleFeatures.remove("mpReactiveMessaging-3.0"); //still in development

        // Test features may or may not be compatible, we don't want to assert either way
        incompatibleFeatures.removeAll(FeatureUtilities.allTestFeatures());

        return incompatibleFeatures;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        allFeatures = FeatureUtilities.getFeaturesFromServer(new File(server.getInstallRoot()), false);
        compatibleFeatures = getCompatibleFeatures(allFeatures, false);
        incompatibleFeatures = getIncompatibleFeatures(allFeatures, compatibleFeatures);

        // Check for typos, every feature we've declared as being compatible or non-compatible should exist
        Stream.concat(incompatibleFeatures.stream(), compatibleFeatures.stream())
                        .forEach(feature -> assertThat(allFeatures, Matchers.hasItem(feature)));

        File lib = new File(server.getInstallRoot(), "lib");

        Utils.setInstallDir(lib.getParentFile());
        KernelUtils.setBootStrapLibDir(lib);

        BundleRepositoryRegistry.initializeDefaults(serverName, true);

        repository = new FeatureRepository();
        repository.init();
    }

    @Test
    public void testEE10FeatureConflictsEE8() throws Exception {
        Set<String> ee8Features = new HashSet<>();
        ee8Features.addAll(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        // remove test features from the list
        ee8Features.removeAll(FeatureUtilities.allTestFeatures());

        // j2eeManagement-1.1 was removed in Jakarta EE 9 so there is no replacement
        ee8Features.remove("j2eeManagement-1.1");

        // jcaInboundSecurity-1.0 was removed in Jakarta EE 10 in favor of using an auto feature.
        ee8Features.remove("jcaInboundSecurity-1.0");

        // servlet long name is the same for EE10 so it will fail because the prefixes
        // match and it is marked as a singleton.
        ee8Features.remove("servlet-4.0");
        testEE10FeatureRenameConflicts(ee8Features);
    }

    @Test
    public void testEE10FeatureConflictsEE7() throws Exception {
        Set<String> ee7Features = new HashSet<>();
        ee7Features.addAll(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        // remove test features from the list
        ee7Features.removeAll(FeatureUtilities.allTestFeatures());

        // j2eeManagement-1.1 was removed in Jakarta EE 9 so there is no replacement
        ee7Features.remove("j2eeManagement-1.1");

        // jcaInboundSecurity-1.0 was removed in Jakarta EE 10 in favor of using an auto feature.
        ee7Features.remove("jcaInboundSecurity-1.0");

        // A couple of special cases that we want to make sure work.
        ee7Features.add("appSecurity-1.0");
        ee7Features.add("websocket-1.0");

        // servlet long name is the same for EE10 so it will fail because the prefixes
        // match and it is marked as a singleton.
        ee7Features.remove("servlet-3.1");
        testEE10FeatureRenameConflicts(ee7Features);
    }

    private void testEE10FeatureRenameConflicts(Set<String> olderEEFeatureSet) throws Exception {

        List<String> featuresToTest = new ArrayList<>(2);
        featuresToTest.add("previous-ee-feature-name");
        featuresToTest.add("ee10-feature-name");

        List<String> errors = new ArrayList<>();
        for (String feature : olderEEFeatureSet) {
            String ee10FeatureName = FeatureReplacementAction.getReplacementFeature(feature, JakartaEE10Action.EE10_FEATURE_SET, Collections.<String> emptySet());
            if (ee10FeatureName == null) {
                errors.add("Did not find EE 10 replacement feature for " + feature + '\n');
                continue;
            }
            featuresToTest.set(0, feature);
            featuresToTest.set(1, ee10FeatureName);
            Result result = resolver.resolveFeatures(repository, Collections.<ProvisioningFeatureDefinition> emptySet(), featuresToTest, Collections.<String> emptySet(), false);
            Map<String, Collection<Chain>> conflicts = result.getConflicts();
            if (conflicts.isEmpty()) {
                errors.add("Did not get expected conflict for " + feature + '\n');
            } else if (!conflicts.containsKey("com.ibm.websphere.appserver.eeCompatible")) {
                errors.add("Expected a conflict for com.ibm.websphere.appserver.eeCompatible for " + feature + " " + conflicts.keySet() + '\n');
            }
        }

        if (!errors.isEmpty()) {
            Assert.fail("Found errors while checking EE10 features incompatibility with previous EE features:\n" + errors);
        }
    }

    /**
     * Test expected compatibility of the cdi-4.0 feature (picked as an example of an EE10 feature)
     * <p>
     * For cdi-3.0 and cdi-4.0, check that it's incompatible and that io.openliberty.cdi is listed as a conflict
     * <p>
     * Otherwise:
     * <ul>
     * <li>Check that it's compatible with all features in {@link #compatibleFeatures}
     * <li>Check that it's incompatible with all features in {@link #incompatibleFeatures} and that eeCompatible is listed as a conflict
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testCdi40Feature() throws Exception {
        Set<String> featureSet = new HashSet<>(allFeatures);
        featureSet.remove("mpHealth");
        featureSet.remove("mpMetrics");
        Map<String, String> specialEE10Conflicts = new HashMap<>();
        specialEE10Conflicts.put("cdi-3.0", "io.openliberty.cdi");
        // cdi-4.0 will conflict with itself
        specialEE10Conflicts.put("cdi-4.0", "io.openliberty.cdi");
        specialEE10Conflicts.put("cdi-4.1", "io.openliberty.cdi");
        testCompatibility("cdi-4.0", featureSet, specialEE10Conflicts);
    }

    /**
     * Test expected compatibility of the servlet-6.0 feature
     * <p>
     * For servlet-x.x features, check that it's incompatible and that com.ibm.websphere.appserver.servlet is listed as a conflict
     * <p>
     * Otherwise:
     * <ul>
     * <li>Check that it's compatible with all features in {@link #compatibleFeatures}
     * <li>Check that it's incompatible with all features in {@link #incompatibleFeatures} and that eeCompatible is listed as a conflict
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testServlet60Feature() throws Exception {
        Set<String> featureSet = new HashSet<>(allFeatures);
        featureSet.remove("mpHealth");
        featureSet.remove("mpMetrics");
        Map<String, String> specialEE10Conflicts = new HashMap<>();
        specialEE10Conflicts.put("servlet-6.1", "com.ibm.websphere.appserver.servlet");
        specialEE10Conflicts.put("servlet-6.0", "com.ibm.websphere.appserver.servlet");
        specialEE10Conflicts.put("servlet-5.0", "com.ibm.websphere.appserver.servlet");
        specialEE10Conflicts.put("servlet-4.0", "com.ibm.websphere.appserver.servlet");
        specialEE10Conflicts.put("servlet-3.1", "com.ibm.websphere.appserver.servlet");
        specialEE10Conflicts.put("servlet-3.0", "com.ibm.websphere.appserver.servlet");

        testCompatibility("servlet-6.0", featureSet, specialEE10Conflicts);
    }

    /**
     * This test is marked as FULL FAT because it can take 10 to 30 minutes to run depending on
     * the system. This puts it past the 5 minute expected limit for lite tests. The cdi-4.0 test
     * is a quick test to show that the basics work with feature resolution.
     *
     * @throws Exception
     */
    // For now don't run this test until it can be refactored
    //@Test
    @Mode(TestMode.FULL)
    public void testJakarta10ConvenienceFeature() throws Exception {
        Set<String> featureSet = new HashSet<>(allFeatures);
        // opentracing-1.3 and jakartaee-10.0 take over an hour to run on power linux system.
        // For now excluding opentracing-1.3 in order to not go past the 3 hour limit for a
        // Full FAT to run.
        featureSet.remove("opentracing-1.3");

        Map<String, String> specialEE10Conflicts = new HashMap<>();
        // faces and facesContainer conflict with each other
        specialEE10Conflicts.put("facesContainer-3.0", "io.openliberty.facesProvider");
        specialEE10Conflicts.put("facesContainer-4.0", "io.openliberty.facesProvider");
        specialEE10Conflicts.put("facesContainer-4.1", "io.openliberty.facesProvider");
        // the jakartaee-10.0 convenience feature conflicts with itself
        specialEE10Conflicts.put("jakartaee-10.0", "io.openliberty.jakartaee");
        // the convenience feature depends on jdbc-4.2 and tolerates 4.3
        specialEE10Conflicts.put("jdbc-4.0", "com.ibm.websphere.appserver.jdbc");
        specialEE10Conflicts.put("jdbc-4.1", "com.ibm.websphere.appserver.jdbc");

        // Add EE9 features that are not part of EE10
        // They will conflict by their long name
        for (String feature : JakartaEE9Action.EE9_FEATURE_SET) {
            if (!JakartaEE10Action.EE10_FEATURE_SET.contains(feature)) {
                // The features below are not included in the convenience feature
                // so they will not conflict on the long name.
                if (!feature.startsWith("connectorsInboundSecurity-") &&
                    !feature.startsWith("jsonpContainer-") &&
                    !feature.startsWith("jsonbContainer-") &&
                    !feature.startsWith("facesContainer-") &&
                    !feature.startsWith("jakartaeeClient-")) {
                    specialEE10Conflicts.put(feature,
                                             feature.startsWith("servlet-") ? "com.ibm.websphere.appserver.servlet" : ("io.openliberty."
                                                                                                                       + feature.substring(0, feature.indexOf('-'))));
                }
            }
        }

        testCompatibility("jakartaee-10.0", featureSet, specialEE10Conflicts);
    }

    private void testCompatibility(String featureName, Set<String> featureSet, Map<String, String> specialConflicts) throws Exception {
        final List<String> errors = new CopyOnWriteArrayList<>();

        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        if (threadCount > 4) {
            threadCount = 4;
        } else if (threadCount == 1) {
            // default to 2 threads so that we don't go over 3 hours in a slow build machine with only two CPUs
            threadCount = 2;
        }
        // leaving <= instead of < in case we change back to only one thread in a two CPU environment.
        if (threadCount <= 1) {
            checkFeatures(featureName, new ArrayDeque<String>(featureSet), specialConflicts, errors);
        } else {
            final ConcurrentLinkedQueue<String> featuresQueue = new ConcurrentLinkedQueue<>(featureSet);
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; ++i) {
                threads[i] = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        checkFeatures(featureName, featuresQueue, specialConflicts, errors);
                    }
                });
            }
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        }

        if (!errors.isEmpty()) {
            Assert.fail("Found errors while checking EE10 features compatibility:\n" + errors);
        }
    }

    private void checkFeatures(String baseFeature, Queue<String> featureQueue, Map<String, String> specialConflicts,
                               List<String> errors) {
        List<String> featuresToTest = new ArrayList<>(2);
        featuresToTest.add(baseFeature);
        featuresToTest.add("tested-feature-name");

        String feature;
        while ((feature = featureQueue.poll()) != null) {
            Log.info(c, "checkFeatures", "start testing: " + feature);
            featuresToTest.set(1, feature);
            if (!compatibleFeatures.contains(feature) && !incompatibleFeatures.contains(feature)) {
                // Don't test this feature
                continue;
            }
            boolean expectToConflict = incompatibleFeatures.contains(feature);
            Result result = resolver.resolveFeatures(repository, Collections.<ProvisioningFeatureDefinition> emptySet(), featuresToTest, Collections.<String> emptySet(), false);
            Log.info(c, "checkFeatures", "finished testing: " + feature + ", conflict expected: " + expectToConflict + ", conflict found: " + !result.getConflicts().isEmpty());
            Map<String, Collection<Chain>> conflicts = result.getConflicts();
            if (expectToConflict) {
                if (conflicts.isEmpty()) {
                    errors.add("Did not get expected conflict for " + feature + '\n');
                } else {
                    String specialConflict = specialConflicts.get(feature);
                    if (specialConflict != null) {
                        if (!conflicts.containsKey(specialConflict)) {
                            errors.add("Got unexpected conflict for " + feature + " " + conflicts.keySet() + '\n');
                        }
                        // else if (conflicts.containsKey("com.ibm.websphere.appserver.eeCompatible")) {
                        //     errors.add("Got eeCompatible conflict in addition to special conflict for " + feature + " " + conflicts.keySet() + '\n');
                        // }
                    } else if (!conflicts.containsKey("com.ibm.websphere.appserver.eeCompatible")) {
                        errors.add("Expected a conflict for com.ibm.websphere.appserver.eeCompatible for " + feature + " " + conflicts.keySet() + '\n');
                    }
                }
            } else if (!conflicts.isEmpty()) {
                String specialConflict = specialConflicts.get(feature);
                if (specialConflict == null || !conflicts.containsKey(specialConflict)) {
                    errors.add("Got unexpected conflict for " + feature + " " + conflicts.keySet() + '\n');
                }
            }
        }
    }

    @Test
    public void transportSecurityUsed() {
        List<String> errors = new ArrayList<>();

        Set<String> ee10FeaturesThatEnableSsl = new HashSet<>();
        ee10FeaturesThatEnableSsl.add("appSecurity-5.0");
        ee10FeaturesThatEnableSsl.add("messagingSecurity-3.0");

        for (String feature : compatibleFeatures) {
            Set<String> featuresToTest;
            if (ee10FeaturesThatEnableSsl.contains(feature)) {
                featuresToTest = Collections.singleton(feature);
            } else {
                featuresToTest = new HashSet<>();
                featuresToTest.add(feature);
                Result result = resolver.resolveFeatures(repository, Collections.<ProvisioningFeatureDefinition> emptySet(), featuresToTest, Collections.<String> emptySet(),
                                                         false);
                if (!result.getResolvedFeatures().contains("ssl-1.0")) {
                    featuresToTest.add("ssl-1.0");
                }
                featuresToTest.add(!feature.equals("jsonp-2.1") ? "jsonp-2.1" : "jsonb-3.0");
            }
            Result result = resolver.resolveFeatures(repository, Collections.<ProvisioningFeatureDefinition> emptySet(), featuresToTest, Collections.<String> emptySet(), false);
            if (!result.getConflicts().isEmpty()) {
                errors.add("Got unexpected conflicts for feature " + feature + '\n');
            } else if (!result.getResolvedFeatures().contains("transportSecurity-1.0")) {
                errors.add("Did not enable transportSecurity for feature " + feature + '\n');
            }
        }

        if (!errors.isEmpty()) {
            Assert.fail("Found errors while checking EE10 features enable transportSecurity-1.0 when ssl-1.0 is used:\n" + errors);
        }
    }
}
