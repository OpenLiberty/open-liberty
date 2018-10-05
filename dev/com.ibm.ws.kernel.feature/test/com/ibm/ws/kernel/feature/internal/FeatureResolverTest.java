/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

import junit.framework.Assert;
import test.common.SharedOutputManager;
import test.utils.TestUtils;

/**
 *
 */
public class FeatureResolverTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=audit=enabled:featureManager=all=enabled");
    static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    public static final String RESOLVER_DATA_DIR = testBuildDir + "/test/resolverData";

    static final File RESOLVER_DATA_FILE = new File(RESOLVER_DATA_DIR);
    static final String serverName = "FeatureResolverTest";
    static final AtomicBoolean returnAutoFeatures = new AtomicBoolean(false);
    static final FeatureResolver resolver = new FeatureResolverImpl();
    static final Collection<ProvisioningFeatureDefinition> noKernelFeatures = Collections.<ProvisioningFeatureDefinition> emptySet();
    static FeatureResolver.Repository repository;
    static Map<String, List<String>> overrideTolerates = new HashMap<String, List<String>>();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();

        File root = RESOLVER_DATA_FILE.getCanonicalFile();
        File lib = new File(root, "lib");

        TestUtils.setUtilsInstallDir(root);
        TestUtils.setKernelUtilsBootstrapLibDir(lib);
        TestUtils.clearBundleRepositoryRegistry();

        BundleRepositoryRegistry.initializeDefaults(serverName, true);

        final FeatureRepository repoImpl = new FeatureRepository();
        repoImpl.init();
        repository = new FeatureResolver.Repository() {

            @Override
            public ProvisioningFeatureDefinition getFeature(String featureName) {
                return repoImpl.getFeature(featureName);
            }

            @Override
            public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
                if (returnAutoFeatures.get()) {
                    return repoImpl.getAutoFeatures();
                }
                return Collections.emptyList();
            }

            @Override
            public List<String> getConfiguredTolerates(String baseSymbolicName) {
                List<String> testOverrides = overrideTolerates.get(baseSymbolicName);
                if (testOverrides != null) {
                    return testOverrides;
                }
                return repoImpl.getConfiguredTolerates(baseSymbolicName);
            }
        };
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();

        TestUtils.setUtilsInstallDir(null);
        TestUtils.setKernelUtilsBootstrapLibDir(null);
        TestUtils.clearBundleRepositoryRegistry();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();
        returnAutoFeatures.set(false);
        overrideTolerates.clear();
    }

    @Test
    public void testSingleFeatureResolve() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.a-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testSingleFeatureTwoResolve() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.b-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.0", "t1.b-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testSimplePreferLatestResolve() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.b-1.1"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.1", "t1.b-1.1"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testPreferLowestExplicitHighestResolve() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t2.x-1.0", "t2.y-1.1"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t2.x-1.0", "t2.y-1.1", "t2.z-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testSimpleAutoFeature() {
        returnAutoFeatures.set(true);
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.c-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.0", "t1.c-1.0", "t1.auto.t1.c-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testSimpleRequireLowest() {
        returnAutoFeatures.set(true);
        doSimpleRequireLowest(new HashSet<String>(Arrays.asList("t1.b-1.1", "t1.c-1.0")));
        // reverse the resolution order
        doSimpleRequireLowest(new HashSet<String>(Arrays.asList("t1.c-1.0", "t1.b-1.1")));
    }

    private void doSimpleRequireLowest(Set<String> toResolve) {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, toResolve, Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.0", "t1.b-1.1", "t1.c-1.0", "t1.auto.t1.c-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testSimpleRootConflict() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.a-1.0", "t1.a-1.1"), Collections.<String> emptySet(), false);
        Assert.assertEquals("Wrong number of results: " + result, 0, result.getResolvedFeatures().size());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t1.a");
    }

    @Test
    public void testSimpleRootConflictAllowMultiple() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.a-1.0", "t1.a-1.1"), Collections.<String> emptySet(), true);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.0", "t1.a-1.1"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testAllowMultipleWithMissingPreferred() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.b-0.9"), Collections.<String> emptySet(), true);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.0", "t1.b-0.9"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testAllowMultipleWithMissingPreferredConflict() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.b-0.9", "t1.c-1.1"), Collections.<String> emptySet(), true);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.0", "t1.a-1.1", "t1.b-0.9", "t1.c-1.1"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testRootConflictWithOtherFeature() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.a-1.0", "t1.a-1.1", "t1.b-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.b-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t1.a");
    }

    @Test
    public void testSimplePreresolve1() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.a-1.0"), new HashSet<String>(Arrays.asList("t1.a-1.1")), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.1"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testSimplePreresolve2() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.a-1.1"), new HashSet<String>(Arrays.asList("t1.a-1.1")), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.1"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testSimplePreresolve3() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.a-1.1"), new HashSet<String>(Arrays.asList("t1.a-1.1", "missing-1.0")), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.1"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testSimplePreresolve4() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t1.a-1.0"), new HashSet<String>(Arrays.asList("t1.a-1.1", "missing-1.0")), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t1.a-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testPostponedDandE() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t3.a-1.0", "t3.b-1.0", "t3.c-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t3.a-1.0", "t3.b-1.0", "t3.c-1.0", "t3.d-2.0", "t3.e-2.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testPostponedDandEWithDirectConflictF() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t3.a-1.0", "t3.b-1.0", "t3.c-1.0", "t3.f-1.0"), Collections.<String> emptySet(),
                                                 false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t3.a-1.0", "t3.b-1.0", "t3.c-1.0", "t3.f-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        Map<String, Collection<Chain>> conflicts = result.getConflicts();
        checkConflict(conflicts, "t3.d", "t3.b-1.0", "t3.f-1.0");
        checkConflict(conflicts, "t3.e", "t3.b-1.0", "t3.f-1.0");
    }

    @Test
    public void testPostponedDandEWithConflictG() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t3.a-1.0", "t3.b-1.0", "t3.c-1.0", "t3.g-1.0"), Collections.<String> emptySet(),
                                                 false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t3.a-1.0", "t3.b-1.0", "t3.c-1.0", "t3.g-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        Map<String, Collection<Chain>> conflicts = result.getConflicts();
        checkConflict(conflicts, "t3.d", "t3.c-1.0", "t3.g-1.0");
        checkConflict(conflicts, "t3.e", "t3.c-1.0", "t3.g-1.0");
    }

    @Test
    public void testConflictingPreferences1() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t4.c-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected1 = new TreeSet<String>(Arrays.asList("t4.c-1.0", "t4.a-1.0", "t4.b-1.0"));
        Set<String> expected2 = new TreeSet<String>(Arrays.asList("t4.c-1.0", "t4.a-2.0", "t4.b-2.0"));
        assertOneOf(new TreeSet<String>(result.getResolvedFeatures()), expected1, expected2);
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testConflictingPreferences2() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t4.f-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected1 = new TreeSet<String>(Arrays.asList("t4.f-1.0", "t4.d-2.0", "t4.e-2.0"));
        Set<String> expected2 = new TreeSet<String>(Arrays.asList("t4.f-1.0", "t4.d-1.0", "t4.e-1.0"));
        assertOneOf(new TreeSet<String>(result.getResolvedFeatures()), expected1, expected2);
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    private void assertOneOf(Set<String> actual, Set<String> expected1, Set<String> expected2) {
        if (actual.equals(expected1) || actual.equals(expected2)) {
            return;
        }
        Assert.fail("Actual: " + actual + " was none of the expected: " + expected1 + " -- " + expected2);
    }

    @Test
    public void testSimpleCycle() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t5.a-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t5.a-1.0", "t5.b-1.0", "t5.c-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testMissing() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t6.a-1.0", "missing.d-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t6.a-1.0", "t6.b-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", new HashSet<String>(Arrays.asList("missing.a-1.0", "missing.b-1.0", "missing.c-1.0", "missing.d-1.0")), result.getMissing());
    }

    @Test
    public void testComplicatedPreference() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.a-1.0", "t7.b-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.c-2.0", "t7.d-2.0", "t7.f-1.0", "t7.g-1.0", "t7.h-2.0", "t7.i-2.0", "t7.j-2.0",
                                                                 "t7.k-2.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testComplicatedPreferenceAllowMultiple() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.a-1.0", "t7.b-1.0"), Collections.<String> emptySet(), true);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.c-1.0", "t7.d-1.0", "t7.f-1.0", "t7.g-1.0",
                                                                 "t7.h-1.0", "t7.h-2.0", "t7.i-1.0", "t7.i-2.0", "t7.j-1.0", "t7.j-2.0", "t7.k-1.0", "t7.k-2.0",
                                                                 "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testComplicatedPreferenceWithAutoFeature() {
        returnAutoFeatures.set(true);
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.a-1.0", "t7.b-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.auto.1-1.0", "t7.auto.3-1.0", "t7.auto.6-1.0", "t7.a-1.0", "t7.b-1.0", "t7.c-2.0", "t7.d-2.0", "t7.f-1.0", "t7.g-1.0",
                                                                 "t7.h-2.0", "t7.i-2.0", "t7.j-2.0", "t7.k-2.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testComplicatedPreferenceWithAutoFeatureAllowMultiple() {
        returnAutoFeatures.set(true);
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.p-1.0", "t7.q-1.0"), Collections.<String> emptySet(),
                                                 true);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.auto.1-1.0", "t7.auto.2-1.0", "t7.auto.3-1.0", "t7.auto.4-1.0", "t7.auto.5-1.0", "t7.auto.6-1.0", "t7.a-1.0", "t7.b-1.0",
                                                                 "t7.c-1.0", "t7.d-1.0", "t7.f-1.0", "t7.g-1.0",
                                                                 "t7.h-1.0", "t7.h-2.0", "t7.i-1.0", "t7.i-2.0", "t7.j-1.0", "t7.j-2.0", "t7.k-1.0", "t7.k-2.0",
                                                                 "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0", "t7.p-1.0", "t7.q-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testComplicatedPreferenceWithHardConflict() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.h-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.c-1.0", "t7.d-2.0", "t7.f-1.0", "t7.g-1.0",
                                                                 "t7.j-2.0", "t7.k-2.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t7.h", ROOT, "t7.l-1.0");
        checkConflict(result.getConflicts(), "t7.i", "t7.c-1.0", "t7.m-1.0");
    }

    @Test
    public void testComplicatedPreferenceWithHardConflictAndAutoFeatures() {
        returnAutoFeatures.set(true);
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.h-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.auto.6-1.0", "t7.a-1.0", "t7.b-1.0", "t7.c-1.0", "t7.d-2.0", "t7.f-1.0", "t7.g-1.0",
                                                                 "t7.j-2.0", "t7.k-2.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t7.h", ROOT, "t7.l-1.0");
        checkConflict(result.getConflicts(), "t7.i", "t7.c-1.0", "t7.m-1.0");
    }

    @Test
    public void TestComplicatedPreferenceWithAutoFeatureConflict() {
        returnAutoFeatures.set(true);
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.a-1.0", "t7.p-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.auto.2-1.0", "t7.auto.3-1.0", "t7.auto.4-1.0", "t7.auto.6-1.0", "t7.a-1.0", "t7.b-1.0", "t7.c-1.0", "t7.d-1.0", "t7.f-1.0",
                                                                 "t7.g-1.0", "t7.h-1.0", "t7.i-1.0", "t7.j-1.0", "t7.k-1.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0",
                                                                 "t7.p-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t7.h", "t7.c-1.0", "t7.l-1.0");
        checkConflict(result.getConflicts(), "t7.i", "t7.c-1.0", "t7.m-1.0");
        checkConflict(result.getConflicts(), "t7.j", "t7.d-1.0", "t7.n-1.0");
        checkConflict(result.getConflicts(), "t7.k", "t7.d-1.0", "t7.o-1.0");
    }

    @Test
    public void TestComplicatedPreferenceWithAutoFeatureLooping() {
        returnAutoFeatures.set(true);
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.p-1.0", "t7.q-1.0"), Collections.<String> emptySet(),
                                                 false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.auto.1-1.0", "t7.auto.3-1.0", "t7.auto.4-1.0", "t7.auto.5-1.0", "t7.auto.6-1.0", "t7.a-1.0", "t7.b-1.0", "t7.c-2.0",
                                                                 "t7.d-2.0", "t7.f-1.0", "t7.g-1.0", "t7.h-2.0", "t7.i-2.0", "t7.j-2.0", "t7.k-2.0", "t7.l-1.0", "t7.m-1.0",
                                                                 "t7.n-1.0", "t7.o-1.0", "t7.p-1.0", "t7.q-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testComplicatedPreferenceWithTransitiveChecks1() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.r-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.c-2.0", "t7.d-2.0", "t7.f-1.0", "t7.g-1.0", "t7.h-2.0", "t7.i-2.0", "t7.j-2.0",
                                                                 "t7.k-2.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0", "t7.r-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testComplicatedPreferenceWithTransitiveChecks2() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.s-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.c-2.0", "t7.d-2.0", "t7.f-1.0", "t7.g-1.0", "t7.h-2.0", "t7.i-2.0", "t7.j-2.0",
                                                                 "t7.k-2.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0", "t7.s-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testComplicatedPreferenceWithTransitiveChecks3() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.t-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.c-1.0", "t7.d-1.0", "t7.f-1.0", "t7.g-1.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0",
                                                                 "t7.o-1.0", "t7.t-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t7.h", "t7.c-1.0", "t7.l-1.0");
        checkConflict(result.getConflicts(), "t7.i", "t7.c-1.0", "t7.m-1.0");
        checkConflict(result.getConflicts(), "t7.j", "t7.d-1.0", "t7.n-1.0");
        checkConflict(result.getConflicts(), "t7.k", "t7.d-1.0", "t7.o-1.0");
    }

    @Test
    public void testComplicatedPreferenceWithTransitiveChecks4() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.u-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.c-2.0", "t7.d-2.0", "t7.f-1.0", "t7.g-1.0", "t7.h-2.0", "t7.i-2.0", "t7.j-2.0",
                                                                 "t7.k-2.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0", "t7.o-1.0", "t7.s-1.0", "t7.u-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testComplicatedPreferenceWithTransitiveChecks5() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t7.v-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t7.a-1.0", "t7.b-1.0", "t7.c-1.0", "t7.d-1.0", "t7.f-1.0", "t7.g-1.0", "t7.l-1.0", "t7.m-1.0", "t7.n-1.0",
                                                                 "t7.o-1.0", "t7.s-1.0", "t7.v-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t7.h", "t7.c-1.0", "t7.l-1.0");
        checkConflict(result.getConflicts(), "t7.i", "t7.c-1.0", "t7.m-1.0");
        checkConflict(result.getConflicts(), "t7.j", "t7.d-1.0", "t7.n-1.0");
        checkConflict(result.getConflicts(), "t7.k", "t7.d-1.0", "t7.o-1.0");
    }

    @Test
    public void testServletExample1() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.jsp-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample2() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.jsp-1.0", "t8.portlet-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-1.0", "t8.portlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample3() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.jsp-1.0", "t8.websockets-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0", "t8.websockets-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample4() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.portlet-1.0", "t8.jsp-1.0", "t8.websockets-1.0"), Collections.<String> emptySet(),
                                                 false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.websockets-1.0", "t8.portlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t8.servlet", "t8.websockets-1.0", "t8.jsp-1.0");
    }

    @Test
    public void testServletExample5() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample6() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0", "t8.portlet-1.0"), Collections.<String> emptySet(),
                                                 false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.portlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t8.servlet", ROOT, "t8.jsp-1.0");
    }

    @Test
    public void testServletExample6OverrideTolerates() {
        overrideTolerates.put("t8.servlet", Collections.singletonList("2.0"));
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0", "t8.portlet-1.0"), Collections.<String> emptySet(),
                                                 false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0", "t8.portlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample7() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.jsp-1.0", "t8.portlet-2.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-1.0", "t8.portlet-2.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample8() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.jsp-1.0", "t8.portlet-2.0", "t8.websockets-1.0"), Collections.<String> emptySet(),
                                                 false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0", "t8.portlet-2.0", "t8.websockets-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample9() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.portlet-2.0", "t8.websockets-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0", "t8.portlet-2.0", "t8.websockets-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample10() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.portlet-1.0", "t8.websockets-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.websockets-1.0", "t8.portlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t8.servlet", "t8.websockets-1.0", "t8.jsp-1.0");
    }

    @Test
    public void testServletExample10OverrideTolerates() {
        overrideTolerates.put("t8.servlet", Collections.singletonList("2.0"));
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.portlet-1.0", "t8.websockets-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.jsp-1.0", "t8.servlet-2.0", "t8.websockets-1.0", "t8.portlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample11() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.x-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.x-1.0", "t8.jsp-1.0", "t8.servlet-1.0", "t8.portlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample12() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.websockets-1.0", "t8.x-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.x-1.0", "t8.jsp-1.0", "t8.websockets-1.0", "t8.portlet-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        checkConflict(result.getConflicts(), "t8.servlet", "t8.websockets-1.0", "t8.jsp-1.0");
    }

    @Test
    public void testServletExample12OverrideTolerates() {
        overrideTolerates.put("t8.servlet", Collections.singletonList("2.0"));
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.websockets-1.0", "t8.x-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.x-1.0", "t8.jsp-1.0", "t8.websockets-1.0", "t8.portlet-1.0", "t8.servlet-2.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testServletExample13() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t8.x-2.0", "t8.websockets-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t8.x-2.0", "t8.websockets-1.0", "t8.jsp-1.0", "t8.servlet-2.0", "t8.portlet-2.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testProcessTypeClient1() {
        doTestContainerType1("client", ProcessType.CLIENT, true);
    }

    @Test
    public void testProcessTypeClient2() {
        doTestContainerType2("client", ProcessType.CLIENT, true);
    }

    @Test
    public void testProcessTypeServer1() {
        doTestContainerType1("server", ProcessType.SERVER, true);
    }

    @Test
    public void testProcessTypeServer2() {
        doTestContainerType2("server", ProcessType.SERVER, true);
    }

    @Test
    public void testProcessTypeClientServer1() {
        doTestContainerType1("client.server", ProcessType.SERVER, false);
    }

    @Test
    public void testProcessTypeClientServer2() {
        doTestContainerType2("client.server", ProcessType.SERVER, false);
    }

    @Test
    public void testProcessTypeClientServerWithAutoServer1() {
        doTestContainerTypeAuto("client.server", ProcessType.SERVER);
    }

    @Test
    public void testIgnoreProcessTypeClient() {
        String serverFeatureA = "t9.a.server-1.0";
        String clientFeatureA = "t9.a.client-1.0";
        Collection<String> rootFeatures = Arrays.asList(serverFeatureA, clientFeatureA);
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, rootFeatures, Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(rootFeatures);
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertFalse("Unexpected failures.", result.hasErrors());
    }

    private void doTestContainerType1(String featureName, ProcessType processType, boolean expectFailure) {
        EnumSet<ProcessType> types1 = EnumSet.of(processType);
        EnumSet<ProcessType> types2 = EnumSet.complementOf(types1);

        String featureA = "t9.a." + featureName + "-1.0";

        verifySuccessProcessType(Arrays.asList(featureA), types1);
        if (expectFailure) {
            verifyFailProcessType(Arrays.asList(featureA), types2);
        } else {
            verifySuccessProcessType(Arrays.asList(featureA), types2);
        }
    }

    private void doTestContainerType2(String featureName, ProcessType processType, boolean expectFailure) {
        EnumSet<ProcessType> types1 = EnumSet.of(processType);
        EnumSet<ProcessType> types2 = EnumSet.complementOf(types1);

        String featureA = "t9.a." + featureName + "-1.0";
        String featureB = "t9.b." + featureName + "-1.0";
        String featureC = "t9.c." + featureName + "-1.0";

        verifySuccessProcessType(Arrays.asList(featureC, featureB, featureA), types1);
        if (expectFailure) {
            verifyFailProcessType(Arrays.asList(featureC, featureB, featureA), types2);
        } else {
            verifySuccessProcessType(Arrays.asList(featureC, featureB, featureA), types2);
        }
    }

    private void doTestContainerTypeAuto(String featureName, ProcessType processType) {
        returnAutoFeatures.set(true);
        EnumSet<ProcessType> types1 = EnumSet.of(processType);
        EnumSet<ProcessType> types2 = EnumSet.complementOf(types1);

        String featureA = "t9.a." + featureName + "-1.0";
        String featureA_Auto = "t9.a." + featureName + ".auto." + processType.toString().toLowerCase() + "-1.0";

        verifySuccessProcessType(Arrays.asList(featureA, featureA_Auto), types1);
        // with the complement process type we expect the auto-feature to not load
        verifySuccessProcessType(Arrays.asList(featureA), types2);
    }

    private void verifyFailProcessType(List<String> features, EnumSet<ProcessType> unsupportedTypes) {
        Collection<String> rootFeatures = Arrays.asList(features.get(0));
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, rootFeatures, Collections.<String> emptySet(), false, unsupportedTypes);

        Set<String> expected = new HashSet<String>(features);
        expected.remove(features.get(features.size() - 1));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertTrue("Expected failures.", result.hasErrors());
        Map<String, Chain> wrongContainerTypes = result.getWrongProcessTypes();
        Assert.assertEquals("Wrong number of wrong container types.", 1, wrongContainerTypes.size());
        Assert.assertEquals("Wrong bad feature.", features.get(features.size() - 1), wrongContainerTypes.keySet().iterator().next());
    }

    private void verifySuccessProcessType(List<String> features, EnumSet<ProcessType> supportedTypes) {
        Collection<String> rootFeatures = Arrays.asList(features.get(0));
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, rootFeatures, Collections.<String> emptySet(), false, supportedTypes);
        Set<String> expected = new HashSet<String>(features);
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertFalse("Unexpected failures.", result.hasErrors());
    }

    @Test
    public void testToleratesTransitively01() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t9.a-1.0"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t9.a-1.0", "t9.b-1.1", "t9.c-1.0", "t9.d-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    @Test
    public void testToleratesTransitively02() {
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, Arrays.asList("t9.a-1.1"), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(Arrays.asList("t9.a-1.1", "t9.c-1.0", "t9.d-1.0"));
        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(result.getResolvedFeatures()));
        checkConflict(result.getConflicts(), "t9.b", "t9.d-1.0");
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }

    private static String ROOT = "ROOT";

    private static void checkConflict(Map<String, Collection<Chain>> conflicts, String baseConflict, String... leaves) {
        Collection<Chain> chains = conflicts.get(baseConflict);
        Assert.assertNotNull("Conflicts chains is null: " + baseConflict, conflicts.get(baseConflict));
        if (leaves.length == 0) {
            // check that this is a root only change; empty
            for (Chain chain : chains) {
                Assert.assertEquals("Unexpected change.", Collections.emptyList(), chain.getChain());
            }
        } else {
            for (String leaf : leaves) {
                boolean foundLeaf = false;
                for (Chain chain : chains) {
                    List<String> features = chain.getChain();
                    if (features.isEmpty()) {
                        foundLeaf = leaf.equals(ROOT);
                    } else {
                        foundLeaf = leaf.equals(features.get(features.size() - 1));
                    }
                    if (foundLeaf) {
                        break;
                    }
                }
                Assert.assertTrue("Did not found source of conflict: " + leaf, foundLeaf);
            }
        }
    }
}
