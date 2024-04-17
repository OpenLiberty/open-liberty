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
package com.ibm.ws.feature.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.ProcessType;
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

import componenttest.common.apiservices.Bootstrap;

/**
 * Feature resolution testing.
 */
public class FeatureResolutionUnitTestBase {

    public static void largeDashes(PrintStream stream) {
        stream.println("============================================================");
    }

    public static void smallDashes(PrintStream stream) {
        stream.println("------------------------------------------------------------");
    }

    //

    private static final List<FailureSummary> failures = new ArrayList<>(0);

    public static final class FailureSummary {
        public final String description;
        public final List<String> features;
        public final List<String> extra;
        public final List<String> missing;

        FailureSummary(String description, List<String> features, List<String> extra, List<String> missing) {
            this.description = description;
            this.features = new ArrayList<>(features);
            this.extra = (((extra == null) || extra.isEmpty()) ? null : new ArrayList<>(extra));
            this.missing = (((missing == null) || missing.isEmpty()) ? null : new ArrayList<>(missing));
        }

        public void print(PrintStream output) {
            output.println("Test [ " + description + " ]:");
            output.println("  Features [ " + features + " ]");
            if (extra != null) {
                output.println("  Extra [ " + extra + " ]");
            }
            if (missing != null) {
                output.println("  Missing [ " + missing + " ]");
            }
        }
    }

    public static void addFailure(String description, List<String> features, List<String> extra, List<String> missing) {
        failures.add(new FailureSummary(description, features, extra, missing));

        System.out.println("Failures [ " + failures.size() + " ]");
    }

    public static void printFailures(PrintStream output) {
        if (!failures.isEmpty()) {
            largeDashes(output);
            output.println("Failures [ " + failures.size() + " ]:");
            smallDashes(output);
            for (FailureSummary summary : failures) {
                summary.print(output);
            }
            largeDashes(output);
        }
    }

    public static void clearFailures() {
        failures.clear();
    }

    //

    public static final String SERVER_NAME = "FeatureResolverTest";

    public static String getServerName() {
        return SERVER_NAME;
    }

    //

    public static void doSetupClass(String serverName) throws Exception {
        setupLocations();
        setupRepo(serverName);
        setupBeta();
    }

    //

    public static final String INSTALL_PATH_PROPERTY_NAME = "libertyInstallPath";

    public static String IMAGE_PATH;
    public static String BOOTSTRAP_LIB_PATH;

    public static File IMAGE_DIR;
    public static File BOOTSTRAP_LIB_DIR;

    public static File getImageDir() {
        return IMAGE_DIR;
    }

    public static File getBootstrapLibDir() {
        return BOOTSTRAP_LIB_DIR;
    }

    public static void setupLocations() throws Exception {
        Bootstrap bootstrap = Bootstrap.getInstance(); // throws Exception

        IMAGE_PATH = bootstrap.getValue(INSTALL_PATH_PROPERTY_NAME);
        BOOTSTRAP_LIB_PATH = IMAGE_PATH + "/lib";

        IMAGE_DIR = new File(IMAGE_PATH);
        BOOTSTRAP_LIB_DIR = new File(BOOTSTRAP_LIB_PATH);
    }

    //

    public static void setupRepo(String serverName) throws Exception {
        System.out.println("Image   [ " + IMAGE_DIR.getAbsolutePath() + " ]");
        System.out.println("BootLib [ " + BOOTSTRAP_LIB_DIR.getAbsolutePath() + " ]");
        System.out.println("Server  [ " + serverName + " ]");

        Utils.setInstallDir(IMAGE_DIR);
        KernelUtils.setBootStrapLibDir(BOOTSTRAP_LIB_DIR);
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
    }

    public static void setupBeta() {
        System.setProperty("com.ibm.ws.beta.edition", "true");
    }

    //

    public static void doTearDownClass() throws Exception {
        printFailures(System.out);
        clearFailures();

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

    private static List<String> publicFeatures;

    public static List<String> getPublicFeatures() {
        if (publicFeatures == null) {
            List<String> features = new ArrayList<>();
            for (ProvisioningFeatureDefinition featureDef : getRepository().getFeatures()) {
                if (isPublic(featureDef)) {
                    features.add(featureDef.getSymbolicName());
                }
            }
            publicFeatures = features;
        }
        return publicFeatures;
    }

    private static List<String> versionlessFeatures;

    public static List<String> getVersionlessFeatures() {
        if (versionlessFeatures == null) {
            List<String> features = new ArrayList<>();
            for (String publicFeature : getPublicFeatures()) {
                if (publicFeature.startsWith("io.openliberty.versionless")) {
                    features.add(publicFeature);
                }
            }
            versionlessFeatures = features;
        }
        return versionlessFeatures;
    }

    private static List<String> servletFeatures;

    public static List<String> getServletFeatures() {
        if (servletFeatures == null) {
            List<String> features = new ArrayList<>();
            for (String publicFeature : getPublicFeatures()) {
                if (publicFeature.startsWith("servlet-")) {
                    features.add(publicFeature);
                }
            }
            servletFeatures = features;
        }
        return servletFeatures;
    }

    public static List<ProvisioningFeatureDefinition> ignoreFeatures(String tag, List<String> featureNames) {
        System.out.println("Ignore [ " + tag + " ] features:");
        for (String featureName : featureNames) {
            System.out.println("  " + featureName);
        }

        return Collections.emptyList();
    }

    public static List<ProvisioningFeatureDefinition> getFeatures(List<String> featureNames) {
        List<ProvisioningFeatureDefinition> featureDefs = new ArrayList<>(featureNames.size());
        for (String featureName : featureNames) {
            ProvisioningFeatureDefinition featureDef = getFeature(featureName);
            if (featureDef == null) {
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
        for (VerifyCase aCase : cases) {
            params.add(new Object[] { aCase.name, aCase });
        }
        return params;
    }

    public static VerifyData readData(File verifyDataFile) throws Exception {
        return VerifyXML.read(verifyDataFile);
    }

    //

    public FeatureResolutionUnitTestBase(String name, VerifyCase testCase) {
        this.name = name;
        this.testCase = testCase;
    }

    private final String name;
    private final VerifyCase testCase;

    public String getName() {
        return name;
    }

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
        if (preferredVersions != null) {
            resolver.setPreferredVersion(preferredVersions);
        }
    }

    public void doClearResolver() throws Exception {
        resolver = null;
    }

    public static final Collection<ProvisioningFeatureDefinition> emptyFeatureDefs = Collections.<ProvisioningFeatureDefinition> emptySet();

    public Result resolveFeatures(VerifyCase verifyCase) throws Exception {
        return resolver.resolveFeatures(getRepository(),
                                        ignoreFeatures("Kernel", verifyCase.input.kernel),
                                        verifyCase.input.roots,
                                        Collections.<String> emptySet(), // pre-resolved feature names
                                        verifyCase.input.isMultiple,
                                        getProcessTypes(verifyCase));
    }

    public static EnumSet<ProcessType> getProcessTypes(VerifyCase verifyCase) {
        if (verifyCase.input.isClient) {
            if (verifyCase.input.isServer) {
                return EnumSet.of(ProcessType.CLIENT, ProcessType.SERVER);
            } else {
                return EnumSet.of(ProcessType.CLIENT);
            }
        } else {
            if (verifyCase.input.isServer) {
                return EnumSet.of(ProcessType.SERVER);
            } else {
                return EnumSet.noneOf(ProcessType.class);
            }
        }
    }

    public void doTestResolve() throws Exception {
        VerifyCase inputCase = getTestCase();

        largeDashes(System.out);

        System.out.println("Verifying [ " + inputCase.name + " ] [ " + inputCase.description + " ]");

        long startNs = System.nanoTime();
        Result result = resolveFeatures(inputCase);
        long endNs = System.nanoTime();
        long durationNs = endNs - startNs;

        Set<String> resultFeatures = result.getResolvedFeatures();
        List<String> resolvedFeatures = new ArrayList<>(resultFeatures.size());
        resolvedFeatures.addAll(resultFeatures);

        VerifyCase outputCase = new VerifyCase(inputCase, resolvedFeatures, durationNs);

        List<String> warnings = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> extra = new ArrayList<>();

        List<String> errors = VerifyDelta.compare(new RepoVisibilitySupplier(getRepository()),
                                                  null, warnings,
                                                  inputCase, outputCase,
                                                  !VerifyDelta.UPDATED_USED_KERNEL,
                                                  extra, missing);

        if ((errors == null) || errors.isEmpty()) {
            System.out.println("Verified");
        } else {
            System.out.println("Verification errors [ " + errors.size() + " ]:");
            for (String error : errors) {
                System.out.println("  [ " + error + " ]");
            }
            write("Revised Case:", outputCase, System.out);
        }

        if (!warnings.isEmpty()) {
            System.out.println("Verification warnings [ " + warnings.size() + " ]:");
            for (String warning : warnings) {
                System.out.println("  [ " + warning + " ]");
            }
        }

        if ((errors != null) && !errors.isEmpty()) {
            addFailure(inputCase.description, inputCase.input.roots, extra, missing);
            fail("Verification failed with [ " + errors.size() + " ] errors");
        }
    }

    private class RepoVisibilitySupplier implements VerifyDelta.VisibilitySupplier {
        public RepoVisibilitySupplier(FeatureResolver.Repository repo) {
            this.repo = repo;
        }

        private final FeatureResolver.Repository repo;

        @Override
        public String getVisibility(String featureName) {
            ProvisioningFeatureDefinition featureDef = repo.getFeature(featureName);
            return ((featureDef == null) ? "MISSING" : featureDef.getVisibility().toString());
        }
    }

    protected void write(String tag, VerifyCase verifyCase, PrintStream output) {
        smallDashes(output);
        System.out.println(tag);
        smallDashes(output);
        PrintWriter writer = new PrintWriter(output);

        // Do not close the writer: 'output' is often System.out, which
        // must not be closed.
        @SuppressWarnings("resource")
        VerifyXML.VerifyXMLWriter xmlWriter = new VerifyXML.VerifyXMLWriter(writer);

        try {
            writer.println(tag);
            xmlWriter.write(verifyCase);
        } finally {
            xmlWriter.flush();
        }

        smallDashes(output);
    }
}
