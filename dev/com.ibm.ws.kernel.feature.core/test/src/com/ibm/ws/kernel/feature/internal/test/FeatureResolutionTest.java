/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.internal.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.internal.util.VerifyDelta;
import com.ibm.ws.kernel.feature.internal.util.VerifyXML;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Selector;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

/**
 * Feature resolution testing.
 */
public class FeatureResolutionTest {
    public static final String UT_IMAGE_PATH = "/data/wlp";
    public static final String FAT_IMAGE_PATH = "../build.image/wlp";

    public static final String UT_BOOTSTRAP_LIB_PATH = "/data/wlp/lib";
    public static final String FAT_BOOTSTRAP_LIB_PATH = "../build.image/wlp/lib";

    public static final boolean IS_COMPLETE_BUILD = (new File(FAT_IMAGE_PATH)).exists();

    public static final String IMAGE_PATH = (IS_COMPLETE_BUILD ? FAT_IMAGE_PATH : UT_IMAGE_PATH);
    public static final String BOOTSTRAP_LIB_PATH = (IS_COMPLETE_BUILD ? FAT_BOOTSTRAP_LIB_PATH : UT_BOOTSTRAP_LIB_PATH);

    public static File IMAGE_DIR = new File(IMAGE_PATH);
    public static File BOOTSTRAP_LIB_DIR = new File(BOOTSTRAP_LIB_PATH);

    public static final String SERVER_NAME = "FeatureResolverTest";

    public static File getImageDir() {
        return IMAGE_DIR;
    }

    public static File getBootstrapLibDir() {
        return BOOTSTRAP_LIB_DIR;
    }

    public static String getServerName() {
        return SERVER_NAME;
    }

    public static void doSetupClass(File imageDir, File bootstrapLibDir, String serverName) throws Exception {
        System.out.println("Build type [ " + (IS_COMPLETE_BUILD ? "FAT" : "UnitTest") + " ]");
        System.out.println("  Image directory [ " + imageDir.getAbsolutePath() + " ]");
        System.out.println("  Bootstrap directory [ " + bootstrapLibDir.getAbsolutePath() + " ]");
        System.out.println("Server [ " + serverName + " ]");

        Utils.setInstallDir(imageDir);
        KernelUtils.setBootStrapLibDir(bootstrapLibDir);
        BundleRepositoryRegistry.initializeDefaults(serverName, true);

        FeatureRepository repoImpl = new FeatureRepository();
        repoImpl.init();
        System.out.println("Features [ " + repoImpl.getFeatures().size() + " ]");

        repository = new FeatureResolver.Repository() {
            private final FeatureRepository baseRepo = repoImpl;

            @Override
            public List<ProvisioningFeatureDefinition> getFeatures() {
                return baseRepo.getFeatures();
            }

            @Override
            public List<ProvisioningFeatureDefinition> select(Selector<ProvisioningFeatureDefinition> selector) {
                return baseRepo.select(selector);
            }

            @Override
            public ProvisioningFeatureDefinition getFeature(String featureName) {
                return baseRepo.getFeature(featureName);
            }

            @Override
            public List<String> getConfiguredTolerates(String baseSymbolicName) {
                return baseRepo.getConfiguredTolerates(baseSymbolicName);
            }

            @Override
            public Collection<ProvisioningFeatureDefinition> getAutoFeatures() {
                return baseRepo.getAutoFeatures();
            }
        };

        System.setProperty("com.ibm.ws.beta.edition", "true");
    }

    @AfterClass
    public static void doTearDownClass() throws Exception {
        Utils.setInstallDir(null);
        KernelUtils.setBootStrapLibDir(null);
    }

    //

    public static boolean isPublic(ProvisioningFeatureDefinition featureDef) {
        return (featureDef.getVisibility() == Visibility.PUBLIC);
    }

    public static FeatureResolver.Repository repository;

    public static FeatureResolver.Repository getRepository() {
        return repository;
    }

    public static ProvisioningFeatureDefinition getFeature(String featureName) {
        return getRepository().getFeature(featureName);
    }

    public static List<ProvisioningFeatureDefinition> getFeatures(List<String> featureNames) {
        List<ProvisioningFeatureDefinition> featureDefs = new ArrayList<>(featureNames.size());
        for ( String featureName : featureNames ) {
            ProvisioningFeatureDefinition featureDef = getFeature(featureName);
            if ( featureDef == null ) {
                throw new IllegalArgumentException("Feature not found [ " + featureName + " ]");
            }
            featureDefs.add(featureDef);
        }
        return featureDefs;
    }

    //

    public static Collection<Object[]> readCases(File verifyDataFile) throws Exception {
        VerifyData verifyData = readData(verifyDataFile);
        List<? extends VerifyCase> cases = verifyData.getCases();

        System.out.println("Read [ " + cases.size() + " ]" +
                           " test cases from [ " + verifyDataFile.getAbsolutePath() + " ]");

        List<Object[]> params = new ArrayList<>(cases.size());
        for ( VerifyCase aCase : cases ) {
            params.add(new Object[] { aCase });
        }
        return params;
    }

    public static VerifyData readData(File verifyDataFile) throws Exception {
        return VerifyXML.read(verifyDataFile);
    }

    //

    public FeatureResolutionTest(VerifyCase testCase) {
        this.testCase = testCase;
    }

    private final VerifyCase testCase;

    public VerifyCase getTestCase() {
        return testCase;
    }

    //

    public String getPreferredVersions() {
        return null;
    }

    public FeatureResolverImpl resolver;

    public void doSetupResolver() throws Exception {
        resolver = new FeatureResolverImpl();

        String preferredVersions = getPreferredVersions();
        if ( preferredVersions != null ) {
            resolver.setPreferredVersion(preferredVersions);
        }
    }

    public void doClearResolver() throws Exception {
        resolver = null;
    }

    public static final Collection<ProvisioningFeatureDefinition> emptyFeatureDefs =
        Collections.<ProvisioningFeatureDefinition> emptySet();

    public Result resolveFeatures(VerifyCase verifyCase) throws Exception {
        return resolver.resolveFeatures( getRepository(),
                                         getFeatures(verifyCase.input.kernel),
                                         verifyCase.input.roots,
                                         Collections.<String> emptySet(), // pre-resolved feature names
                                         verifyCase.input.isMultiple,
                                         verifyCase.input.getProcessTypes() );
    }

    public void doTestResolve() throws Exception {
        VerifyCase inputCase = getTestCase();

        System.out.println("Verifying [ " + inputCase.name + " ] [ " + inputCase.description + " ]");

        long startNs = System.nanoTime();
        Result result = resolveFeatures(inputCase);
        long endNs = System.nanoTime();
        long durationNs = endNs - startNs;

        Set<String> resultFeatures = result.getResolvedFeatures();
        List<String> resolvedFeatures = new ArrayList<>(resultFeatures.size());
        resolvedFeatures.addAll(resultFeatures);

        VerifyCase outputCase = new VerifyCase(inputCase, resolvedFeatures, durationNs);

        List<String> errors = VerifyDelta.compare(null, inputCase, outputCase);

        if ( (errors == null) || errors.isEmpty() ) {
            System.out.println("Verified");
        } else {
            System.out.println("Verification failure:");
            for ( String error : errors ) {
                System.out.println("  [ " + error + " ]");
            }
            fail("Verification failures [ " + errors.size() + " ]");
        }
    }
}
