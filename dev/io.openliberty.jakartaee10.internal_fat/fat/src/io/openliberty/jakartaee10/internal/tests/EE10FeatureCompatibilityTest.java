/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakartaee10.internal.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

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
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.MicroProfileActions;
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

    static final Set<String> features = new HashSet<>();

    static final Set<String> nonEE10JavaEEFeatures = new HashSet<>();

    static final Set<String> nonEE10MicroProfileFeatures = new HashSet<>();

    static final Set<String> incompatibleValueAddFeatures = new HashSet<>();

    static final String serverName = "jakartaee10.fat";
    static final FeatureResolver resolver = new FeatureResolverImpl();
    static FeatureRepository repository;

    @Server("jakartaee10.fat")
    public static LibertyServer server;

    static {
        nonEE10JavaEEFeatures.addAll(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        nonEE10JavaEEFeatures.addAll(EE8FeatureReplacementAction.EE8_FEATURE_SET);

        // appSecurity-1.0 is superceded by appSecurity-2.0 so it isn't in one of the replacement
        // feature lists.  jaxb and jaxws 2.3 are EE related, but are noship features currently.
        // jsp-2.2 is a EE6 feature that is included with open liberty.
        // websocket-1.0 is a special case.  Part of EE7, but 1.1 is used by liberty.
        nonEE10JavaEEFeatures.add("appSecurity-1.0");
        nonEE10JavaEEFeatures.add("jaxb-2.3");
        nonEE10JavaEEFeatures.add("jaxws-2.3");
        nonEE10JavaEEFeatures.add("jsp-2.2");
        nonEE10JavaEEFeatures.add("websocket-1.0");

        // Remove test features that are in the FeatureReplacementActions
        nonEE10JavaEEFeatures.remove("componenttest-1.0");
        nonEE10JavaEEFeatures.remove("componenttest-2.0");
        nonEE10JavaEEFeatures.remove("txtest-1.0");
        nonEE10JavaEEFeatures.remove("txtest-2.0");
        nonEE10JavaEEFeatures.remove("ejbTest-1.0");
        nonEE10JavaEEFeatures.remove("ejbTest-2.0");

        for (FeatureSet mpFeatureSet : MicroProfileActions.ALL) {
            if (mpFeatureSet.getEEVersion() != EEVersion.EE10) {
                nonEE10MicroProfileFeatures.addAll(mpFeatureSet.getFeatures());
            }
        }

        // MP standalone features
        for (FeatureSet mpFeatureSet : MicroProfileActions.STANDALONE_ALL) {
            if (mpFeatureSet.getEEVersion() != EEVersion.EE10) {
                nonEE10MicroProfileFeatures.addAll(mpFeatureSet.getFeatures());
            }
        }

        //These MP6 features are now EE10 based (even if MP 6.0 is still EE9), they should not conflict
        nonEE10MicroProfileFeatures.remove("mpOpenAPI-3.1");
        nonEE10MicroProfileFeatures.remove("mpMetrics-5.0");
        nonEE10MicroProfileFeatures.remove("mpJwt-2.1");

        //These MP5 features are also in MP6 and have had EE10 toleration added, they should not conflict
        nonEE10MicroProfileFeatures.remove("mpFaultTolerance-4.0");
        //nonEE10MicroProfileFeatures.remove("mpRestClient-3.0"); //EE10 toleration does not work yet
        nonEE10MicroProfileFeatures.remove("mpConfig-3.0");
        nonEE10MicroProfileFeatures.remove("mpHealth-4.0");
        nonEE10MicroProfileFeatures.remove("mpContextPropagation-1.3");

        // Add EE9 features that are not part of EE10
        for (String feature : JakartaEE9Action.EE9_FEATURE_SET) {
            if (!JakartaEE10Action.EE10_FEATURE_SET.contains(feature)) {
                nonEE10JavaEEFeatures.add(feature);
            }
        }

        incompatibleValueAddFeatures.add("openid-2.0"); // stabilized
        incompatibleValueAddFeatures.add("openapi-3.1"); // depends on mpOpenAPI
        incompatibleValueAddFeatures.add("opentracing-1.0"); // opentracing depends on mpConfig
        incompatibleValueAddFeatures.add("opentracing-1.1");
        incompatibleValueAddFeatures.add("opentracing-1.2");
        incompatibleValueAddFeatures.add("opentracing-1.3");
        incompatibleValueAddFeatures.add("opentracing-2.0");
        incompatibleValueAddFeatures.add("sipServlet-1.1"); // purposely not supporting EE 10
        incompatibleValueAddFeatures.add("springBoot-1.5"); // springBoot 3.0 will support EE 9 and possibly 10
        incompatibleValueAddFeatures.add("springBoot-2.0");

        // temporarily add jwtSso-1.0 until mpJWT 2.1 is added or mpJWT 2.0 is designated as compatible with EE10.
        incompatibleValueAddFeatures.add("jwtSso-1.0");
    }

    static Set<String> getAllCompatibleFeatures() {
        Set<String> compatFeatures = new HashSet<>();
        try {
            File featureDir = new File(Bootstrap.getInstance().getValue("libertyInstallPath") + "/lib/features/");
            // If there was a problem building projects before this test runs, "lib/features" won't exist
            if (featureDir != null && featureDir.exists()) {
                for (File feature : featureDir.listFiles()) {
                    if (feature.getName().startsWith("io.openliberty.") ||
                        feature.getName().startsWith("com.ibm.")) {
                        String shortName = EE10FeatureCompatibilityTest.parseShortName(feature);
                        if (shortName != null) {
                            compatFeatures.add(shortName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        compatFeatures.removeAll(EE10FeatureCompatibilityTest.nonEE10JavaEEFeatures);
        compatFeatures.removeAll(EE10FeatureCompatibilityTest.nonEE10MicroProfileFeatures);
        compatFeatures.removeAll(EE10FeatureCompatibilityTest.incompatibleValueAddFeatures);

        return compatFeatures;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        File featureDir = new File(server.getInstallRoot() + "/lib/features/");
        // If there was a problem building projects before this test runs, "lib/features" won't exist
        if (featureDir != null && featureDir.exists()) {
            for (File feature : featureDir.listFiles()) {
                if (feature.getName().startsWith("io.openliberty.") ||
                    feature.getName().startsWith("com.ibm.")) {
                    String shortName = parseShortName(feature);
                    if (shortName != null) {
                        features.add(shortName);
                    }
                }
            }
        }

        // The features set should contain all of the incompatible features.  If it doesn't
        // something was removed or there is a typo.
        for (String feature : nonEE10JavaEEFeatures) {
            Assert.assertTrue(feature + " was not in the all features list", features.contains(feature));
        }
        for (String feature : nonEE10MicroProfileFeatures) {
            Assert.assertTrue(feature + " was not in the all features list", features.contains(feature));
        }
        for (String feature : incompatibleValueAddFeatures) {
            Assert.assertTrue(feature + " was not in the all features list", features.contains(feature));
        }

        File lib = new File(server.getInstallRoot(), "lib");

        Utils.setInstallDir(lib.getParentFile());
        KernelUtils.setBootStrapLibDir(lib);

        BundleRepositoryRegistry.initializeDefaults(serverName, true);

        repository = new FeatureRepository();
        repository.init();
    }

    static String parseShortName(File feature) throws IOException {
        // Only scan *.mf files
        if (feature.isDirectory() || !feature.getName().endsWith(".mf"))
            return null;

        Scanner scanner = new Scanner(feature);
        try {
            String shortName = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("IBM-ShortName:")) {
                    shortName = line.substring("IBM-ShortName:".length()).trim();
                } else if (line.contains("IBM-Test-Feature:") && line.contains("true")) {
                    Log.info(c, "parseShortName", "Skipping test feature: " + feature.getName());
                    return null;
                } else if (line.startsWith("Subsystem-SymbolicName:") && !line.contains("visibility:=public")) {
                    Log.info(c, "parseShortName", "Skipping non-public feature: " + feature.getName());
                    return null;
                } else if (line.startsWith("IBM-ProductID") && !line.contains("io.openliberty")) {
                    Log.info(c, "parseShortName", "Skipping non Open Liberty feature: " + feature.getName());
                    return null;
                }
            }
            // some test feature files do not have a short name and do not have IBM-Test-Feature set.
            // We do not want those ones.
            if (shortName != null) {
                return shortName;
            }
        } finally {
            scanner.close();
        }
        return null;
    }

    @Test
    public void testEE10FeatureConflictsEE8() throws Exception {
        Set<String> ee8Features = new HashSet<>();
        ee8Features.addAll(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        // remove test features from the list
        ee8Features.remove("componenttest-1.0");
        ee8Features.remove("txtest-1.0");
        ee8Features.remove("ejbTest-1.0");

        // j2eeManagement-1.1 was removed in Jakarta EE 9 so there is no replacement
        ee8Features.remove("j2eeManagement-1.1");

        // jcaInboundSecurity-1.0 was removed in Jakarta EE 10 in favor of using an auto feature.
        ee8Features.remove("jcaInboundSecurity-1.0");

        // A couple of special cases that we want to make sure work.
        ee8Features.add("jaxb-2.3");
        ee8Features.add("jaxws-2.3");

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
        ee7Features.remove("componenttest-1.0");
        ee7Features.remove("txtest-1.0");
        ee7Features.remove("ejbTest-1.0");

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

    @Test
    public void testJsonP21Feature() throws Exception {
        Map<String, String> specialEE10Conflicts = new HashMap<>();
        specialEE10Conflicts.put("jsonp-2.0", "io.openliberty.jsonp");
        // jsonp-2.1 will conflict with itself
        specialEE10Conflicts.put("jsonp-2.1", "io.openliberty.jsonp");
        testCompatibility("jsonp-2.1", features, specialEE10Conflicts);
    }

    @Test
    public void testServlet60Feature() throws Exception {
        Map<String, String> specialEE10Conflicts = new HashMap<>();
        specialEE10Conflicts.put("servlet-6.0", "com.ibm.websphere.appserver.servlet");
        specialEE10Conflicts.put("servlet-5.0", "com.ibm.websphere.appserver.servlet");
        specialEE10Conflicts.put("servlet-4.0", "com.ibm.websphere.appserver.servlet");
        specialEE10Conflicts.put("servlet-3.1", "com.ibm.websphere.appserver.servlet");

        testCompatibility("servlet-6.0", features, specialEE10Conflicts);
    }

    /**
     * This test is marked as FULL FAT because it can take 10 to 30 minutes to run depending on
     * the system. This puts it past the 5 minute expected limit for lite tests. The jsonp-2.0 test
     * is a quick test to show that the basics work with feature resolution.
     *
     * @throws Exception
     */
    // For now don't run this test until it can be refactored
    //@Test
    @Mode(TestMode.FULL)
    public void testJakarta10ConvenienceFeature() throws Exception {
        Set<String> featureSet = new HashSet<>(features);
        // opentracing-1.3 and jakartaee-10.0 take over an hour to run on power linux system.
        // For now excluding opentracing-1.3 in order to not go past the 3 hour limit for a
        // Full FAT to run.
        featureSet.remove("opentracing-1.3");

        Map<String, String> specialEE10Conflicts = new HashMap<>();
        // faces and facesContainer conflict with each other
        specialEE10Conflicts.put("facesContainer-3.0", "io.openliberty.facesProvider");
        specialEE10Conflicts.put("facesContainer-4.0", "io.openliberty.facesProvider");
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
                if (!feature.startsWith("jsonpContainer-") &&
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
            boolean expectToConflict = nonEE10JavaEEFeatures.contains(feature) || nonEE10MicroProfileFeatures.contains(feature) || incompatibleValueAddFeatures.contains(feature);
            Result result = resolver.resolveFeatures(repository, Collections.<ProvisioningFeatureDefinition> emptySet(), featuresToTest, Collections.<String> emptySet(), false);
            Log.info(c, "checkFeatures", "finished testing: " + feature);
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
        ee10FeaturesThatEnableSsl.add("connectorsInboundSecurity-2.0");
        ee10FeaturesThatEnableSsl.add("messagingSecurity-3.0");

        for (String feature : features) {
            if (nonEE10JavaEEFeatures.contains(feature) || nonEE10MicroProfileFeatures.contains(feature) || incompatibleValueAddFeatures.contains(feature)) {
                continue;
            }
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