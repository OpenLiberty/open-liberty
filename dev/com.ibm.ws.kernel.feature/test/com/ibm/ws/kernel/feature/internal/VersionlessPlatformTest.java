/*******************************************************************************
 * Copyright (c) 2010, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.kernel.feature.FeatureDefinition;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

import junit.framework.Assert;
import test.common.SharedOutputManager;
import test.utils.TestUtils;
import test.utils.VersionlessTestCase;

/**
 *
 */
@RunWith(Parameterized.class)
public class VersionlessPlatformTest {
    static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    public static final String RESOLVER_DATA_DIR = testBuildDir + "/test/resolverData";
    //public static final String RESOLVER_DATA_DIR = "../build.image";
    static final File RESOLVER_DATA_FILE = new File(RESOLVER_DATA_DIR);
    static final String serverName = "FeatureResolverTest";
    static final AtomicBoolean returnAutoFeatures = new AtomicBoolean(false);
    static final FeatureResolverImpl resolver = new FeatureResolverImpl();
    static final Collection<ProvisioningFeatureDefinition> noKernelFeatures = Collections.<ProvisioningFeatureDefinition> emptySet();
    static FeatureResolver.Repository repository;
    static Map<String, List<String>> overrideTolerates = new HashMap<String, List<String>>();

    private VersionlessTestCase testCase;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception{
        List<VersionlessTestCase> testCases = new ArrayList<VersionlessTestCase>();

        // loop through data set of versionless feature server configurations and resolved outputs
        // add to our versionless test case array to iteratively test every test case
        // for(int i = 0; i<dataset size; i++){
        //     testCases.add(new VersionlessTestCase(
        //         Arrays.asList( // feature inputs
        //             data set inputs
        //         ), 
        //         Arrays.asList( // feature outputs
        //             data set outputs
        //         )));
        // }
        //
        // user str.split("\\s*,\\s*") to separate string of all features to list
        // ex Arrays.asList("String, of, features".split("\\s*,\\s*"))

        File root = RESOLVER_DATA_FILE.getCanonicalFile();
        System.out.println(root.getAbsolutePath());
        File lib = new File(root, "lib");
        System.out.println(lib.getAbsolutePath());

        TestUtils.setUtilsInstallDir(root);
        TestUtils.setKernelUtilsBootstrapLibDir(lib);

        BundleRepositoryRegistry.initializeDefaults(serverName, true);

        FeatureRepository tempRepo = new FeatureRepository();
        tempRepo.init();

        Map<String, SubsystemFeatureDefinitionImpl> publicDefs = tempRepo.getAllFeatures();

        for(String s : publicDefs.keySet()){
            if(s.startsWith("io.openliberty.versionless")){
                String featureName = tempRepo.getFeature(s).getFeatureName();


                if(featureName.startsWith("mp")){
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "MicroProfile-1.3"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "MicroProfile-2.2"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "MicroProfile-3.0"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "MicroProfile-3.3"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "MicroProfile-4.0"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "MicroProfile-5.0"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "MicroProfile-6.0"));
                }
                else{
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "jakartaee-7.0"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "jakartaee-8.0"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "jakartaee-9.0"));
                    testCases.add(new VersionlessTestCase(
                        Arrays.asList(
                            (featureName).split("\\s*,\\s*")
                        ), 
                        Collections.<String> emptyList(),
                        "jakartaee-10.0"));
                }
            }
        }

        System.out.println("TestCases: " + testCases.toString());
        System.out.println("As List: " + Arrays.asList(testCases));
        List<Object[]> output = new ArrayList<Object[]>();
        for(int i = 0; i < testCases.size(); i++){
            output.add(new Object[] {testCases.get(i)});
        }

        System.out.println(output);
        return output;
    }

    public VersionlessPlatformTest(VersionlessTestCase testCase) {
        this.testCase = testCase;
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.setProperty("com.ibm.ws.beta.edition", "true");
        //resolver.setPrefferedVersion("mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpMetrics-3.0,mpMetrics-2.3,mpMetrics-2.2,mpMetrics-2.0,mpMetrics-1.1,mpMetrics-1.0,mpHealth-4.0,mpHealth-3.1,mpHealth-3.0,mpHealth-2.2,mpHealth-2.1,mpHealth-2.0,mpHealth-1.0");

        File root = RESOLVER_DATA_FILE.getCanonicalFile();
        System.out.println(root.getAbsolutePath());
        File lib = new File(root, "lib");
        System.out.println(lib.getAbsolutePath());

        TestUtils.setUtilsInstallDir(root);
        TestUtils.setKernelUtilsBootstrapLibDir(lib);

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
        returnAutoFeatures.set(false);
        overrideTolerates.clear();
    }

    @Test
    public void testVersionlessFeatureResolve() {
        System.out.println("TestCase: " + this.testCase.getFeatureInputs() + " - " + this.testCase.getPlatforms() + ": ");
        if(this.testCase.getPlatforms() != null){
            resolver.setPreferredPlatforms(this.testCase.getPlatforms());
        }
        Result result = resolver.resolveFeatures(repository, noKernelFeatures, this.testCase.getFeatureInputs(), Collections.<String> emptySet(), false);
        Set<String> expected = new HashSet<String>(this.testCase.getFeatureOutputs());
        Set<String> output = new HashSet<String>();

        for(String feature : result.getResolvedFeatures()){
            if(repository.getFeature(feature).getVisibility() == Visibility.PUBLIC){
                output.add(feature);
            }
        }

        System.out.println("          " + output + " - Conflicts: " + result.getConflicts() + " - Missing: " + result.getMissing());

        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(output));
        Assert.assertEquals("Unexpected Conflicts.", Collections.emptyMap(), result.getConflicts());
        Assert.assertEquals("Unexpected missing.", Collections.emptySet(), result.getMissing());
    }
}