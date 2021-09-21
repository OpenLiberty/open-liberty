/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakartaee9.internal.tests;

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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EERepeatTests.EEVersion;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test validates that all Open Liberty features that are expected to work
 * with Jakarta EE 9 features resolve correctly compared to a Jakarta EE 9 feature.
 *
 * When a feature is expected to conflict with Jakarta EE 9 features, this test validates
 * that the com.ibm.websphere.appserver.eeCompatible feature is one of the conflicts. This is
 * important because there are messages that come up for eeCompatible conflicts to tell the
 * customer that features are not compatible with Jakarta EE 9.
 *
 * As new value add features are added to Open Liberty this test will assume that they
 * will support Jakarta EE 9. When Jakarta EE 10 is added there will need to be some additional
 * logic to handle Jakarta EE 10 features as being expected to not run with EE 9 features.
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class EE9FeatureCompatibilityTest extends FATServletClient {

    private static final Class<?> c = EE9FeatureCompatibilityTest.class;

    static final List<String> features = new ArrayList<>();

    static final Set<String> nonEE9JavaEEFeatures = new HashSet<>();

    static final Set<String> nonEE9MicroProfileFeatures = new HashSet<>();

    static final Set<String> incompatibleValueAddFeatures = new HashSet<>();

    static final String serverName = "jakartaee9.fat";
    static final FeatureResolver resolver = new FeatureResolverImpl();
    static FeatureRepository repository;

    @Server("jakartaee9.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        File featureDir = new File(server.getInstallRoot() + "/lib/features/");
        // If there was a problem building projects before this test runs, "lib/features" won't exist
        if (featureDir != null && featureDir.exists()) {
            for (File feature : featureDir.listFiles()) {
                if (feature.getName().startsWith("io.openliberty.") ||
                    feature.getName().startsWith("com.ibm.")) {
                    parseShortName(feature);
                }
            }
        }

        nonEE9JavaEEFeatures.addAll(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        nonEE9JavaEEFeatures.addAll(EE8FeatureReplacementAction.EE8_FEATURE_SET);

        // appSecurity-1.0 is superceded by appSecurity-2.0 so it isn't in one of the replacement
        // feature lists.  jaxb and jaxws 2.3 are EE related, but are noship features currently.
        // jsp-2.2 is a EE6 feature that is included with open liberty.
        // websocket-1.0 is a special case.  Part of EE7, but 1.1 is used by liberty.
        nonEE9JavaEEFeatures.add("appSecurity-1.0");
        nonEE9JavaEEFeatures.add("jaxb-2.3");
        nonEE9JavaEEFeatures.add("jaxws-2.3");
        nonEE9JavaEEFeatures.add("jsp-2.2");
        nonEE9JavaEEFeatures.add("websocket-1.0");

        // Remove test features that are in the FeatureReplacementActions
        nonEE9JavaEEFeatures.remove("componenttest-1.0");
        nonEE9JavaEEFeatures.remove("componenttest-2.0");
        nonEE9JavaEEFeatures.remove("txtest-1.0");
        nonEE9JavaEEFeatures.remove("txtest-2.0");
        nonEE9JavaEEFeatures.remove("ejbTest-1.0");
        nonEE9JavaEEFeatures.remove("ejbTest-2.0");

        for (FeatureSet mpFeatureSet : MicroProfileActions.ALL) {
            if (mpFeatureSet.getEEVersion() != EEVersion.EE9) {
                nonEE9MicroProfileFeatures.addAll(mpFeatureSet.getFeatures());
            }
        }

        // MP standalone features
        for (FeatureSet mpFeatureSet : MicroProfileActions.STANDALONE_ALL) {
            if (mpFeatureSet.getEEVersion() != EEVersion.EE9) {
                nonEE9MicroProfileFeatures.addAll(mpFeatureSet.getFeatures());
            }
        }

        incompatibleValueAddFeatures.add("jwtSso-1.0"); // depends on mpJWT
        incompatibleValueAddFeatures.add("openid-2.0"); // stabilized
        incompatibleValueAddFeatures.add("openapi-3.1"); // depends on mpOpenAPI
        incompatibleValueAddFeatures.add("opentracing-1.0"); // opentracing depends on mpConfig
        incompatibleValueAddFeatures.add("opentracing-1.1");
        incompatibleValueAddFeatures.add("opentracing-1.2");
        incompatibleValueAddFeatures.add("opentracing-1.3");
        incompatibleValueAddFeatures.add("opentracing-2.0");
        incompatibleValueAddFeatures.add("sipServlet-1.1"); // purposely not supporting EE 9
        incompatibleValueAddFeatures.add("springBoot-1.5"); // springBoot 3.0 will support EE 9
        incompatibleValueAddFeatures.add("springBoot-2.0");

        // The features set should contain all of the incompatible features.  If it doesn't
        // something was removed or there is a typo.
        for (String feature : nonEE9JavaEEFeatures) {
            Assert.assertTrue(feature + " was not in the all features list", features.contains(feature));
        }
        for (String feature : nonEE9MicroProfileFeatures) {
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

    private static void parseShortName(File feature) throws IOException {
        // Only scan *.mf files
        if (feature.isDirectory() || !feature.getName().endsWith(".mf"))
            return;

        Scanner scanner = new Scanner(feature);
        try {
            String shortName = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("IBM-ShortName:")) {
                    shortName = line.substring("IBM-ShortName:".length()).trim();
                } else if (line.contains("IBM-Test-Feature:") && line.contains("true")) {
                    Log.info(c, "parseShortName", "Skipping test feature: " + feature.getName());
                    return;
                } else if (line.startsWith("Subsystem-SymbolicName:") && !line.contains("visibility:=public")) {
                    Log.info(c, "parseShortName", "Skipping non-public feature: " + feature.getName());
                    return;
                } else if (line.startsWith("IBM-ProductID") && !line.contains("io.openliberty")) {
                    Log.info(c, "parseShortName", "Skipping non Open Liberty feature: " + feature.getName());
                    return;
                }
            }
            // some test feature files do not have a short name and do not have IBM-Test-Feature set.
            // We do not want those ones.
            if (shortName != null) {
                features.add(shortName);
            }
        } finally {
            scanner.close();
        }
    }

    @Test
    public void testEE9FeatureConflictsEE8() throws Exception {
        Set<String> ee8Features = new HashSet<>();
        ee8Features.addAll(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        // remove test features from the list
        ee8Features.remove("componenttest-1.0");
        ee8Features.remove("txtest-1.0");
        ee8Features.remove("ejbTest-1.0");

        // j2eeManagement-1.1 was removed in Jakarta EE 9 so there is no replacement
        ee8Features.remove("j2eeManagement-1.1");

        // A couple of special cases that we want to make sure work.
        ee8Features.add("jaxb-2.3");
        ee8Features.add("jaxws-2.3");

        // servlet long name is the same for EE9 so it will fail because the prefixes
        // match and it is marked as a singleton.
        ee8Features.remove("servlet-4.0");
        testEE9FeatureRenameConflicts(ee8Features);
    }

    @Test
    public void testEE9FeatureConflictsEE7() throws Exception {
        Set<String> ee7Features = new HashSet<>();
        ee7Features.addAll(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        // remove test features from the list
        ee7Features.remove("componenttest-1.0");
        ee7Features.remove("txtest-1.0");
        ee7Features.remove("ejbTest-1.0");

        // j2eeManagement-1.1 was removed in Jakarta EE 9 so there is no replacement
        ee7Features.remove("j2eeManagement-1.1");

        // A couple of special cases that we want to make sure work.
        ee7Features.add("appSecurity-1.0");
        ee7Features.add("websocket-1.0");

        // servlet long name is the same for EE9 so it will fail because the prefixes
        // match and it is marked as a singleton.
        ee7Features.remove("servlet-3.1");
        testEE9FeatureRenameConflicts(ee7Features);
    }

    private void testEE9FeatureRenameConflicts(Set<String> olderEEFeatureSet) throws Exception {

        List<String> featuresToTest = new ArrayList<>(2);
        featuresToTest.add("previous-ee-feature-name");
        featuresToTest.add("ee9-feature-name");

        List<String> errors = new ArrayList<>();
        for (String feature : olderEEFeatureSet) {
            String ee9FeatureName = FeatureReplacementAction.getReplacementFeature(feature, JakartaEE9Action.EE9_FEATURE_SET, Collections.<String> emptySet());
            if (ee9FeatureName == null) {
                errors.add("Did not find EE 9 replacement feature for " + feature + '\n');
                continue;
            }
            featuresToTest.set(0, feature);
            featuresToTest.set(1, ee9FeatureName);
            Result result = resolver.resolveFeatures(repository, Collections.<ProvisioningFeatureDefinition> emptySet(), featuresToTest, Collections.<String> emptySet(), false);
            Map<String, Collection<Chain>> conflicts = result.getConflicts();
            if (conflicts.isEmpty()) {
                errors.add("Did not get expected conflict for " + feature + '\n');
            } else if (!conflicts.containsKey("com.ibm.websphere.appserver.eeCompatible")) {
                errors.add("Expected a conflict for com.ibm.websphere.appserver.eeCompatible for " + feature + " " + conflicts.keySet() + '\n');
            }
        }

        if (!errors.isEmpty()) {
            Assert.fail("Found errors while checking EE9 features incompatibility with EE8 features:\n" + errors);
        }
    }

    @Test
    public void testJsonP20Feature() throws Exception {
        Map<String, String> specialEE9Conflicts = new HashMap<>();
        // jsonp-2.0 will conflict with itself
        specialEE9Conflicts.put("jsonp-2.0", "io.openliberty.jsonp");
        specialEE9Conflicts.put("jsonp-2.1", "io.openliberty.jsonp");
        testCompatibility("jsonp-2.0", specialEE9Conflicts);
    }

    @Test
    public void testServlet50Feature() throws Exception {
        Map<String, String> specialEE9Conflicts = new HashMap<>();
        specialEE9Conflicts.put("servlet-6.0", "com.ibm.websphere.appserver.servlet");
        specialEE9Conflicts.put("servlet-5.0", "com.ibm.websphere.appserver.servlet");
        specialEE9Conflicts.put("servlet-4.0", "com.ibm.websphere.appserver.servlet");
        specialEE9Conflicts.put("servlet-3.1", "com.ibm.websphere.appserver.servlet");

        testCompatibility("servlet-5.0", specialEE9Conflicts);
    }

    /**
     * This test is marked as FULL FAT because it can take 10 to 30 minutes to run depending on
     * the system. This puts it past the 5 minute expected limit for lite tests. The jsonp-2.0 test
     * is a quick test to show that the basics work with feature resolution.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testJakarta91ConvenienceFeature() throws Exception {
        Map<String, String> specialEE9Conflicts = new HashMap<>();
        // faces and facesContainer conflict with each other
        specialEE9Conflicts.put("facesContainer-3.0", "io.openliberty.facesProvider");
        // the jakartaee-9.1 convenience feature conflicts with itself and with 9.0
        specialEE9Conflicts.put("jakartaee-9.0", "io.openliberty.jakartaee");
        specialEE9Conflicts.put("jakartaee-9.1", "io.openliberty.jakartaee");
        // the convenience feature depends on jdbc-4.2 and tolerates 4.3
        specialEE9Conflicts.put("jdbc-4.0", "com.ibm.websphere.appserver.jdbc");
        specialEE9Conflicts.put("jdbc-4.1", "com.ibm.websphere.appserver.jdbc");
        // the webProfile-9.1 convenience feature conflicts with the 9.0 one
        specialEE9Conflicts.put("webProfile-9.0", "io.openliberty.webProfile");

        // Add EE10 features that are not part of EE9
        // They will conflict by their long name
        for (String feature : JakartaEE10Action.EE10_FEATURE_SET) {
            if (!JakartaEE9Action.EE9_FEATURE_SET.contains(feature)) {
                specialEE9Conflicts.put(feature,
                                        feature.startsWith("servlet-") ? "com.ibm.websphere.appserver.servlet" : "io.openliberty." + feature.substring(0, feature.indexOf('-')));
            }
        }

        testCompatibility("jakartaee-9.1", specialEE9Conflicts);
    }

    private void testCompatibility(String featureName, Map<String, String> specialConflicts) throws Exception {
        final List<String> errors = new CopyOnWriteArrayList<>();

        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        if (threadCount > 4) {
            threadCount = 4;
        }
        if (threadCount <= 1) {
            checkFeatures(featureName, new ArrayDeque<String>(features), specialConflicts, errors);
        } else {
            final ConcurrentLinkedQueue<String> featuresQueue = new ConcurrentLinkedQueue<>(features);
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
            Assert.fail("Found errors while checking EE9 features compatibility:\n" + errors);
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
            boolean expectToConflict = nonEE9JavaEEFeatures.contains(feature) || nonEE9MicroProfileFeatures.contains(feature) || incompatibleValueAddFeatures.contains(feature);
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
                        } else if (conflicts.containsKey("com.ibm.websphere.appserver.eeCompatible")) {
                            errors.add("Got eeCompatible conflict in additional to special conflict for " + feature + " " + conflicts.keySet() + '\n');
                        }
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
}